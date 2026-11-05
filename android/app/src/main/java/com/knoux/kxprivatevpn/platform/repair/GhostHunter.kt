package com.knoux.kxprivatevpn.platform.repair

import com.knoux.kxprivatevpn.platform.storage.DocumentSource
import com.knoux.kxprivatevpn.platform.storage.MutableDocumentSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

enum class GhostKind { ZERO_BYTE_FILE, EMPTY_DIRECTORY }
enum class GhostConfidence { HIGH, MEDIUM }
enum class GhostReason { REPORTED_ZERO_BYTES, NO_CHILDREN }

/**
 * A cleanup candidate with the evidence that produced it. Nothing is inferred from age or an
 * unknown extension: only a reported 0-byte size or a directory that returned no children.
 */
data class GhostCandidate(
    val documentId: String,
    val relativePath: String,
    val kind: GhostKind,
    val mime: String,
    val confidence: GhostConfidence,
    val reason: GhostReason,
)

data class GhostScan(
    val candidates: List<GhostCandidate>,
    val scannedFiles: Int,
    val scannedDirectories: Int,
    val excludedSentinels: Int,
    val truncated: Boolean = false,
    val cancelled: Boolean = false,
    val failed: Boolean = false,
    val atMillis: Long = 0,
) {
    val zeroByteCount: Int get() = candidates.count { it.kind == GhostKind.ZERO_BYTE_FILE }
    val emptyDirectoryCount: Int get() = candidates.count { it.kind == GhostKind.EMPTY_DIRECTORY }
}

/**
 * Read-only SAF walk that finds zero-byte files and directories with no children. Known sentinels
 * (`.nomedia`, `.gitkeep`, `.gitignore`) are never offered for cleanup: deleting them changes the
 * meaning of a folder instead of reclaiming junk.
 */
class GhostScanner(
    private val source: DocumentSource,
    private val maxEntries: Int = MAX_ENTRIES,
    private val maxDepth: Int = MAX_DEPTH,
) {
    @Volatile private var cancelRequested = false
    fun requestCancel() { cancelRequested = true }

    suspend fun scan(
        rootDocumentId: String,
        atMillis: Long,
        onProgress: (Int) -> Unit = {},
    ): GhostScan {
        cancelRequested = false
        val candidates = mutableListOf<GhostCandidate>()
        val stack = ArrayDeque<Pair<String, String>>()
        stack.addLast(rootDocumentId to "")
        var scannedFiles = 0
        var scannedDirectories = 0
        var excluded = 0
        var truncated = false
        var failed = false

        while (stack.isNotEmpty()) {
            coroutineContext.ensureActive()
            if (cancelRequested) {
                return GhostScan(candidates.toList(), scannedFiles, scannedDirectories, excluded,
                    truncated, true, failed, atMillis)
            }
            if (scannedFiles + scannedDirectories >= maxEntries) {
                truncated = true
                break
            }
            val (documentId, prefix) = stack.removeLast()
            scannedDirectories++
            val children = try {
                source.list(documentId)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                failed = true
                truncated = true
                onProgress(scannedFiles + scannedDirectories)
                continue
            }
            if (prefix.isNotEmpty() && children.isEmpty()) {
                candidates += GhostCandidate(
                    documentId, prefix, GhostKind.EMPTY_DIRECTORY, MIME_DIRECTORY,
                    GhostConfidence.MEDIUM, GhostReason.NO_CHILDREN,
                )
            }
            for (child in children) {
                if (scannedFiles + scannedDirectories >= maxEntries) {
                    truncated = true
                    break
                }
                val path = if (prefix.isEmpty()) child.name else "$prefix/${child.name}"
                if (child.mime == MIME_DIRECTORY) {
                    val depth = if (prefix.isEmpty()) 0 else prefix.count { it == '/' } + 1
                    if (depth < maxDepth) stack.addLast(child.documentId to path)
                    else truncated = true
                } else {
                    scannedFiles++
                    if (child.bytes == 0L) {
                        if (child.name.lowercase() in SENTINELS) excluded++
                        else candidates += GhostCandidate(
                            child.documentId, path, GhostKind.ZERO_BYTE_FILE, child.mime,
                            GhostConfidence.HIGH, GhostReason.REPORTED_ZERO_BYTES,
                        )
                    }
                }
            }
            onProgress(scannedFiles + scannedDirectories)
        }
        return GhostScan(candidates.toList(), scannedFiles, scannedDirectories, excluded,
            truncated, cancelRequested, failed, atMillis)
    }

    companion object {
        const val MAX_ENTRIES = 10_000
        const val MAX_DEPTH = 12
        private const val MIME_DIRECTORY = "vnd.android.document/directory"
        val SENTINELS = setOf(".nomedia", ".gitkeep", ".gitignore")
    }
}

data class GhostCleanupReceipt(
    val deleted: Int,
    val skipped: List<String>,
    val failed: List<String>,
    val verifiedAbsent: Int,
    val complete: Boolean,
)

/**
 * Confirmed cleanup. Each candidate is re-read immediately before deletion; a file that gained
 * bytes or a directory that gained children is skipped, not deleted. A successful provider call
 * is followed by a real absence check before the receipt calls it deleted.
 */
class GhostCleanup(private val source: MutableDocumentSource) {
    @Volatile private var cancelRequested = false
    fun requestCancel() { cancelRequested = true }

    suspend fun remove(
        selected: List<GhostCandidate>,
        onProgress: (Int) -> Unit = {},
    ): GhostCleanupReceipt {
        cancelRequested = false
        var deleted = 0
        var verified = 0
        val skipped = mutableListOf<String>()
        val failed = mutableListOf<String>()
        var processed = 0
        for (candidate in selected) {
            coroutineContext.ensureActive()
            if (cancelRequested) break
            processed++
            val stillValid = when (candidate.kind) {
                GhostKind.ZERO_BYTE_FILE -> {
                    val current = try { source.document(candidate.documentId) } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        null
                    }
                    current != null && current.bytes == 0L &&
                        current.name.lowercase() !in GhostScanner.SENTINELS
                }
                GhostKind.EMPTY_DIRECTORY -> {
                    try {
                        source.list(candidate.documentId).isEmpty()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        false
                    }
                }
            }
            if (!stillValid) {
                skipped += candidate.relativePath
                onProgress(processed)
                continue
            }
            val removed = try {
                source.delete(candidate.documentId)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                false
            }
            if (!removed) {
                failed += candidate.relativePath
                onProgress(processed)
                continue
            }
            val absent = try {
                source.document(candidate.documentId) == null
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                false
            }
            if (absent) {
                deleted++
                verified++
            } else {
                failed += candidate.relativePath
            }
            onProgress(processed)
        }
        return GhostCleanupReceipt(deleted, skipped, failed, verified, !cancelRequested)
    }
}
