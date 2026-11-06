package com.knoux.kxprivatevpn.platform.repair

import com.knoux.kxprivatevpn.platform.storage.DocumentEntry
import com.knoux.kxprivatevpn.platform.storage.DocumentSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import java.util.PriorityQueue
import kotlin.coroutines.coroutineContext

/**
 * Broad file families used only to label and filter a measured size ranking. The category is
 * derived from the provider-reported MIME type; it is never guessed from the file extension.
 */
enum class LargeFileCategory(val key: String) {
    IMAGE("image"),
    VIDEO("video"),
    AUDIO("audio"),
    DOCUMENT("document"),
    ARCHIVE("archive"),
    OTHER("other"),
    ;

    companion object {
        fun fromMime(mime: String): LargeFileCategory {
            val value = mime.lowercase()
            return when {
                value.startsWith("image/") -> IMAGE
                value.startsWith("video/") -> VIDEO
                value.startsWith("audio/") -> AUDIO
                value in ARCHIVE_MIMES -> ARCHIVE
                value.startsWith("text/") || value in DOCUMENT_MIMES ||
                    value.contains("word") || value.contains("document") ||
                    value.contains("sheet") || value.contains("presentation") ||
                    value.contains("opendocument") -> DOCUMENT
                else -> OTHER
            }
        }

        private val ARCHIVE_MIMES = setOf(
            "application/zip",
            "application/x-zip-compressed",
            "application/x-rar-compressed",
            "application/vnd.rar",
            "application/x-7z-compressed",
            "application/x-tar",
            "application/gzip",
            "application/x-gzip",
            "application/x-bzip2",
        )

        private val DOCUMENT_MIMES = setOf(
            "application/pdf",
            "application/rtf",
            "application/json",
            "application/xml",
        )
    }
}

/** One measured file. [bytes] is always the provider-reported size, never an estimate. */
data class LargeFile(
    val documentId: String,
    val relativePath: String,
    val name: String,
    val mime: String,
    val bytes: Long,
    val lastModified: Long,
) {
    val category: LargeFileCategory get() = LargeFileCategory.fromMime(mime)
}

/**
 * Result of one bounded walk. [files] holds the largest [limit] entries at or above
 * [minimumBytes]; [largeFileCount] and [totalLargeBytes] cover every measured file that passed
 * the same threshold, even when more files existed than the ranking keeps.
 */
data class HeavyweightScan(
    val files: List<LargeFile>,
    val scannedFiles: Int,
    val largeFileCount: Int,
    val totalLargeBytes: Long,
    val skippedUnknownSize: Int,
    val minimumBytes: Long,
    val truncated: Boolean = false,
    val cancelled: Boolean = false,
    val failed: Boolean = false,
    val atMillis: Long = 0,
)

/** Pure, testable view filter. The scanner already bounded the ranking; this never rescans. */
data class HeavyweightFilter(
    val minimumBytes: Long = 0L,
    val category: LargeFileCategory? = null,
) {
    fun apply(files: List<LargeFile>): List<LargeFile> = files.filter { file ->
        file.bytes >= minimumBytes && (category == null || file.category == category)
    }
}

/**
 * Read-only SAF walk that ranks the largest files in a user-granted tree.
 *
 * The walk is bounded by [maxEntries] and [maxDepth]. A provider entry without a reported size is
 * counted in [HeavyweightScan.skippedUnknownSize] instead of being ranked with a guessed value.
 * Only the largest [limit] entries are retained in a min-heap, so a huge folder cannot grow the
 * result set without bound.
 */
class HeavyweightScanner(
    private val source: DocumentSource,
    private val maxEntries: Int = MAX_ENTRIES,
    private val maxDepth: Int = MAX_DEPTH,
    private val minimumBytes: Long = MINIMUM_BYTES,
    private val limit: Int = LIMIT,
) {
    @Volatile private var cancelRequested = false
    fun requestCancel() { cancelRequested = true }

    suspend fun scan(
        rootDocumentId: String,
        atMillis: Long,
        onProgress: (Int) -> Unit = {},
    ): HeavyweightScan {
        cancelRequested = false
        val retained = PriorityQueue<LargeFile>(
            compareBy<LargeFile> { it.bytes }.thenBy { it.relativePath }
        )
        val stack = ArrayDeque<Pair<String, String>>()
        stack.addLast(rootDocumentId to "")
        var walkedEntries = 0
        var scannedFiles = 0
        var largeFileCount = 0
        var totalLargeBytes = 0L
        var skippedUnknownSize = 0
        var truncated = false
        var failed = false

        while (stack.isNotEmpty()) {
            coroutineContext.ensureActive()
            if (cancelRequested) {
                return snapshot(retained, scannedFiles, largeFileCount, totalLargeBytes,
                    skippedUnknownSize, truncated, cancelled = true, failed, atMillis)
            }
            if (walkedEntries >= maxEntries) {
                truncated = true
                break
            }
            val (documentId, prefix) = stack.removeLast()
            val children: List<DocumentEntry> = try {
                source.list(documentId)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                failed = true
                truncated = true
                onProgress(walkedEntries)
                continue
            }
            for (child in children) {
                if (walkedEntries >= maxEntries) {
                    truncated = true
                    break
                }
                walkedEntries++
                val path = if (prefix.isEmpty()) child.name else "$prefix/${child.name}"
                if (child.mime == MIME_DIRECTORY) {
                    val depth = if (prefix.isEmpty()) 0 else prefix.count { it == '/' } + 1
                    if (depth < maxDepth) stack.addLast(child.documentId to path)
                    else truncated = true
                    continue
                }
                scannedFiles++
                if (child.bytes < 0) {
                    // The provider did not report a size. Ranking it would invent a number.
                    skippedUnknownSize++
                } else if (child.bytes >= minimumBytes) {
                    largeFileCount++
                    totalLargeBytes += child.bytes
                    retained.add(LargeFile(
                        documentId = child.documentId,
                        relativePath = path,
                        name = child.name,
                        mime = child.mime,
                        bytes = child.bytes,
                        lastModified = child.lastModified,
                    ))
                    if (retained.size > limit) retained.poll()
                }
            }
            onProgress(walkedEntries)
        }
        return snapshot(retained, scannedFiles, largeFileCount, totalLargeBytes,
            skippedUnknownSize, truncated, cancelled = cancelRequested, failed, atMillis)
    }

    private fun snapshot(
        retained: PriorityQueue<LargeFile>,
        scannedFiles: Int,
        largeFileCount: Int,
        totalLargeBytes: Long,
        skippedUnknownSize: Int,
        truncated: Boolean,
        cancelled: Boolean,
        failed: Boolean,
        atMillis: Long,
    ): HeavyweightScan = HeavyweightScan(
        files = retained.sortedWith(
            compareByDescending<LargeFile> { it.bytes }.thenBy { it.relativePath }
        ),
        scannedFiles = scannedFiles,
        largeFileCount = largeFileCount,
        totalLargeBytes = totalLargeBytes,
        skippedUnknownSize = skippedUnknownSize,
        minimumBytes = minimumBytes,
        truncated = truncated,
        cancelled = cancelled,
        failed = failed,
        atMillis = atMillis,
    )

    companion object {
        const val MAX_ENTRIES = 10_000
        const val MAX_DEPTH = 12
        const val LIMIT = 200
        const val MIB = 1024L * 1024L
        const val MINIMUM_BYTES = 1L * MIB
        val FILTER_STEPS = listOf(0L, 10L * MIB, 50L * MIB, 100L * MIB, 500L * MIB)
        private const val MIME_DIRECTORY = "vnd.android.document/directory"
    }
}
