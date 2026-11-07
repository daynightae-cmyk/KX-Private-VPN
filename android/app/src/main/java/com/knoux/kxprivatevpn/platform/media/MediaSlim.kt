package com.knoux.kxprivatevpn.platform.media

import com.knoux.kxprivatevpn.platform.storage.DocumentEntry
import com.knoux.kxprivatevpn.platform.storage.DocumentSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

/** Media that KX may offer to slim. Video is measured but never silently re-encoded. */
enum class MediaKind { IMAGE, VIDEO }

/** One measured, user-authorized media document. No content is read during a scan. */
data class MediaCandidate(
    val documentId: String,
    val parentDocumentId: String,
    val relativePath: String,
    val name: String,
    val mime: String,
    val bytes: Long,
    val lastModified: Long,
) {
    val kind: MediaKind get() = if (mime.lowercase().startsWith("video/")) MediaKind.VIDEO
    else MediaKind.IMAGE

    /**
     * Only still images whose first frame is a complete picture are supported. GIF/AVIF and
     * every video stay visible but are never re-encoded by this slice.
     */
    val compressible: Boolean get() = mime.lowercase() in SUPPORTED_IMAGE_MIMES

    companion object {
        val SUPPORTED_IMAGE_MIMES = setOf(
            "image/jpeg", "image/png", "image/webp", "image/heic", "image/heif",
        )
    }
}

data class MediaSlimScan(
    val files: List<MediaCandidate>,
    val scannedFiles: Int,
    val skippedUnknownSize: Int,
    val truncated: Boolean = false,
    val cancelled: Boolean = false,
    val failed: Boolean = false,
    val atMillis: Long = 0,
) {
    val images: List<MediaCandidate> get() = files.filter { it.kind == MediaKind.IMAGE }
    val videos: List<MediaCandidate> get() = files.filter { it.kind == MediaKind.VIDEO }
    val imageBytes: Long get() = images.sumOf { it.bytes }
    val videoBytes: Long get() = videos.sumOf { it.bytes }
}

/** Bounded SAF walk for media. It keeps all supported candidates, not a sample. */
class MediaSlimScanner(
    private val source: DocumentSource,
    private val maxEntries: Int = MAX_ENTRIES,
    private val maxDepth: Int = MAX_DEPTH,
    private val minimumBytes: Long = MINIMUM_BYTES,
) {
    @Volatile private var cancelRequested = false
    fun requestCancel() { cancelRequested = true }

    suspend fun scan(
        rootDocumentId: String,
        atMillis: Long,
        onProgress: (Int) -> Unit = {},
    ): MediaSlimScan {
        cancelRequested = false
        val files = mutableListOf<MediaCandidate>()
        val stack = ArrayDeque<Pair<String, String>>()
        stack.addLast(rootDocumentId to "")
        var walked = 0
        var scannedFiles = 0
        var skippedUnknown = 0
        var truncated = false
        var failed = false

        while (stack.isNotEmpty()) {
            coroutineContext.ensureActive()
            if (cancelRequested) {
                return MediaSlimScan(files.toList(), scannedFiles, skippedUnknown,
                    truncated, cancelled = true, failed = failed, atMillis = atMillis)
            }
            if (walked >= maxEntries) { truncated = true; break }
            val (documentId, prefix) = stack.removeLast()
            val children: List<DocumentEntry> = try {
                source.list(documentId)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                failed = true
                truncated = true
                onProgress(walked)
                continue
            }
            for (child in children) {
                if (walked >= maxEntries) { truncated = true; break }
                walked++
                val path = if (prefix.isEmpty()) child.name else "$prefix/${child.name}"
                if (child.mime == MIME_DIRECTORY) {
                    val depth = if (prefix.isEmpty()) 0 else prefix.count { it == '/' } + 1
                    if (depth < maxDepth) stack.addLast(child.documentId to path)
                    else truncated = true
                    continue
                }
                if (!isMedia(child.mime)) continue
                scannedFiles++
                if (child.bytes < 0) {
                    skippedUnknown++
                } else if (child.bytes >= minimumBytes) {
                    files += MediaCandidate(
                        documentId = child.documentId,
                        parentDocumentId = documentId,
                        relativePath = path,
                        name = child.name,
                        mime = child.mime,
                        bytes = child.bytes,
                        lastModified = child.lastModified,
                    )
                }
            }
            onProgress(walked)
        }
        return MediaSlimScan(
            files = files.sortedByDescending { it.bytes },
            scannedFiles = scannedFiles,
            skippedUnknownSize = skippedUnknown,
            truncated = truncated,
            cancelled = cancelRequested,
            failed = failed,
            atMillis = atMillis,
        )
    }

    companion object {
        const val MAX_ENTRIES = 10_000
        const val MAX_DEPTH = 12
        const val MINIMUM_BYTES = 32L * 1024L
        private const val MIME_DIRECTORY = "vnd.android.document/directory"

        fun isMedia(mime: String): Boolean {
            val value = mime.lowercase()
            return value.startsWith("image/") || value.startsWith("video/")
        }
    }
}

/** Quality is an explicit user choice; there is no hidden default recompression. */
enum class MediaQuality(val percent: Int) {
    HIGH(85), BALANCED(65), SMALL(45);

    companion object {
        fun fromPercent(value: Int): MediaQuality = entries.firstOrNull { it.percent == value } ?: BALANCED
    }
}

data class MediaSelection(
    val ids: Set<String>,
    val quality: MediaQuality = MediaQuality.BALANCED,
) {
    fun candidates(scan: MediaSlimScan): List<MediaCandidate> =
        scan.files.filter { it.documentId in ids }

    fun supportedBytes(scan: MediaSlimScan): Long =
        candidates(scan).filter { it.compressible }.sumOf { it.bytes }

    fun unsupportedCount(scan: MediaSlimScan): Int =
        candidates(scan).count { !it.compressible }
}

/** Domain checks used by the ViewModel and by tests; never guesses an unsupported operation. */
object MediaSlimPolicy {
    fun canCompress(scan: MediaSlimScan?, selection: MediaSelection): Boolean =
        scan != null && selection.candidates(scan).any { it.compressible }

    fun confirmation(scan: MediaSlimScan, selection: MediaSelection): String? {
        val supported = selection.candidates(scan).count { it.compressible }
        if (supported == 0) return null
        return "$supported image(s), ${selection.supportedBytes(scan)} measured bytes"
    }
}
