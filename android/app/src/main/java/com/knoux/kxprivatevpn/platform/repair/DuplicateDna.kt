package com.knoux.kxprivatevpn.platform.repair

import com.knoux.kxprivatevpn.platform.storage.DocumentEntry
import com.knoux.kxprivatevpn.platform.storage.MutableDocumentSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

/**
 * A file that is a confirmed exact duplicate of at least one other file in the same scan.
 *
 * [sha256] is hex-encoded and computed by streaming the full file content through the SAF
 * document URI; it is never guessed from metadata alone.
 * [keep] is true on the single copy the user chose to keep (or auto-selected by earliest
 * lastModified). Only duplicates with [keep]=false are offered for removal.
 */
data class DuplicateEntry(
    val documentId: String,
    val relativePath: String,
    val bytes: Long,
    val lastModified: Long,
    val sha256: String,
    val keep: Boolean,
)

/**
 * One group of files that share the same byte count and SHA-256 digest.
 *
 * The group is guaranteed to have at least 2 members. Exactly one entry has [DuplicateEntry.keep]
 * set to true; the rest are offered for confirmed deletion.
 */
data class DuplicateGroup(
    val sha256: String,
    val bytes: Long,
    val entries: List<DuplicateEntry>,
) {
    val duplicateCount: Int get() = entries.count { !it.keep }
    val reclaimableBytes: Long get() = duplicateCount * bytes
}

data class DuplicateScan(
    val groups: List<DuplicateGroup>,
    /** Files examined; includes any file that passed the size-group pre-filter. */
    val scannedFiles: Int,
    /** Files hashed (subset of scanned; only those sharing a size with another file). */
    val hashedFiles: Int,
    val truncated: Boolean = false,
    val cancelled: Boolean = false,
    val failed: Boolean = false,
    val atMillis: Long = 0,
) {
    val duplicateGroupCount: Int get() = groups.size
    val totalReclaimableBytes: Long get() = groups.sumOf { it.reclaimableBytes }
}

data class DuplicateCleanupReceipt(
    val deleted: Int,
    val skipped: List<String>,
    val failed: List<String>,
    val verifiedAbsent: Int,
    val bytesReclaimed: Long,
    val complete: Boolean,
)

/** Test seam so the scanner can hash via any byte-source without touching AndroidDocumentSource. */
interface DuplicateContentSource {
    /** Open the file identified by [documentId] and stream all bytes into the digest. */
    suspend fun hash(documentId: String): ByteArray
    /** Re-read the current byte count for a file (for pre-delete re-verification). */
    suspend fun currentBytes(documentId: String): Long?
}

/**
 * Two-pass duplicate scanner.
 *
 * Pass 1 — size grouping: walk the tree using [source], collecting (documentId, path, bytes,
 * lastModified) for every non-directory entry. Files whose byte count is unique in the whole scan
 * cannot be duplicates; they are discarded immediately (no SHA-256 work).
 *
 * Pass 2 — content hashing: for each size bucket with ≥ 2 files, hash every member via
 * [contentSource]. Files with identical digests form a [DuplicateGroup]. Within each group the
 * copy with the earliest [lastModified] is designated the keeper; ties are broken by
 * lexicographic path order (deterministic, no random choices).
 *
 * The scanner is bounded by [maxEntries] and [maxDepth]. Cancellation is checked at each entry
 * during both passes.
 */
class DuplicateScanner(
    private val source: com.knoux.kxprivatevpn.platform.storage.DocumentSource,
    private val contentSource: DuplicateContentSource,
    private val maxEntries: Int = MAX_ENTRIES,
    private val maxDepth: Int = MAX_DEPTH,
) {
    @Volatile private var cancelRequested = false
    fun requestCancel() { cancelRequested = true }

    suspend fun scan(
        rootDocumentId: String,
        atMillis: Long,
        onProgress: (scanned: Int, hashed: Int) -> Unit = { _, _ -> },
    ): DuplicateScan {
        cancelRequested = false

        // --- Pass 1: walk and collect ---
        data class RawEntry(
            val documentId: String,
            val relativePath: String,
            val bytes: Long,
            val lastModified: Long,
        )

        val all = mutableListOf<RawEntry>()
        var truncated = false
        var failed = false

        val stack = ArrayDeque<Pair<String, String>>()
        stack.addLast(rootDocumentId to "")
        var walkedDirs = 0

        while (stack.isNotEmpty()) {
            coroutineContext.ensureActive()
            if (cancelRequested) {
                return DuplicateScan(emptyList(), all.size, 0, truncated, true, failed, atMillis)
            }
            if (all.size >= maxEntries) {
                truncated = true
                break
            }
            val (docId, prefix) = stack.removeLast()
            walkedDirs++
            val children: List<DocumentEntry> = try {
                source.list(docId)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                failed = true
                truncated = true
                onProgress(all.size, 0)
                continue
            }
            for (child in children) {
                if (all.size >= maxEntries) {
                    truncated = true
                    break
                }
                val path = if (prefix.isEmpty()) child.name else "$prefix/${child.name}"
                if (child.mime == MIME_DIRECTORY) {
                    val depth = if (prefix.isEmpty()) 0 else prefix.count { it == '/' } + 1
                    if (depth < maxDepth) stack.addLast(child.documentId to path)
                    else truncated = true
                } else if (child.bytes > 0) {
                    // Zero-byte files are structurally identical but handled by Ghost Hunter.
                    all += RawEntry(child.documentId, path, child.bytes, child.lastModified)
                }
            }
            onProgress(all.size, 0)
        }

        val scannedFiles = all.size

        // --- Pass 2: hash size-bucket candidates ---
        val sizeBuckets = all.groupBy { it.bytes }
        val candidates = sizeBuckets.values.filter { it.size >= 2 }.flatten()

        if (candidates.isEmpty()) {
            return DuplicateScan(emptyList(), scannedFiles, 0, truncated, false, failed, atMillis)
        }

        val hashMap = mutableMapOf<String, MutableList<RawEntry>>()
        var hashed = 0

        for (entry in candidates) {
            coroutineContext.ensureActive()
            if (cancelRequested) {
                return DuplicateScan(emptyList(), scannedFiles, hashed, truncated, true, failed, atMillis)
            }
            val digest = try {
                contentSource.hash(entry.documentId)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                failed = true
                hashed++
                onProgress(scannedFiles, hashed)
                continue
            }
            val hex = digest.toHex()
            hashMap.getOrPut(hex) { mutableListOf() } += entry
            hashed++
            onProgress(scannedFiles, hashed)
        }

        // --- Build groups ---
        val groups = hashMap.entries
            .filter { it.value.size >= 2 }
            .map { (sha, bucket) ->
                val sorted = bucket.sortedWith(
                    compareBy({ it.lastModified }, { it.relativePath })
                )
                val entries = sorted.mapIndexed { idx, raw ->
                    DuplicateEntry(
                        documentId = raw.documentId,
                        relativePath = raw.relativePath,
                        bytes = raw.bytes,
                        lastModified = raw.lastModified,
                        sha256 = sha,
                        keep = idx == 0,
                    )
                }
                DuplicateGroup(sha, bucket.first().bytes, entries)
            }
            .sortedByDescending { it.reclaimableBytes }

        return DuplicateScan(groups, scannedFiles, hashed, truncated, false, failed, atMillis)
    }

    companion object {
        const val MAX_ENTRIES = 8_000
        const val MAX_DEPTH = 12
        private const val MIME_DIRECTORY = "vnd.android.document/directory"
    }
}

/**
 * Confirmed duplicate cleanup.
 *
 * Every group is re-verified immediately before deletion:
 *  1. the keeper still exists, has the scanned byte count, and re-hashes to the group digest;
 *  2. each selected duplicate still exists, has the scanned byte count, and re-hashes to the
 *     same digest.
 * A same-size content change is therefore skipped, not deleted. A successful provider delete is
 * followed by an absence check before the receipt calls it deleted.
 */
class DuplicateCleanup(
    private val source: MutableDocumentSource,
    private val contentSource: DuplicateContentSource,
) {
    @Volatile private var cancelRequested = false
    fun requestCancel() { cancelRequested = true }

    suspend fun remove(
        groups: List<DuplicateGroup>,
        selectedDocumentIds: Set<String>,
        onProgress: (Int) -> Unit = {},
    ): DuplicateCleanupReceipt {
        cancelRequested = false
        var deleted = 0
        var verified = 0
        var bytesReclaimed = 0L
        val skipped = mutableListOf<String>()
        val failed = mutableListOf<String>()
        var processed = 0

        for (group in groups) {
            coroutineContext.ensureActive()
            if (cancelRequested) break
            val selected = group.entries.filter {
                !it.keep && it.documentId in selectedDocumentIds
            }
            if (selected.isEmpty()) continue

            // The keeper must still be the same file; otherwise removing a duplicate could
            // destroy the only remaining copy of this content.
            val keeper = group.entries.firstOrNull { it.keep }
            val keeperValid = keeper != null && verify(group, keeper)
            if (!keeperValid) {
                skipped += selected.map { it.relativePath }
                processed += selected.size
                onProgress(processed)
                continue
            }

            for (entry in selected) {
                coroutineContext.ensureActive()
                if (cancelRequested) break
                processed++
                if (!verify(group, entry)) {
                    skipped += entry.relativePath
                    onProgress(processed)
                    continue
                }

                val removed = try {
                    source.delete(entry.documentId)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    false
                }
                if (!removed) {
                    failed += entry.relativePath
                    onProgress(processed)
                    continue
                }

                val absent = try {
                    source.document(entry.documentId) == null
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    false
                }
                if (absent) {
                    deleted++
                    verified++
                    bytesReclaimed += entry.bytes
                } else {
                    failed += entry.relativePath
                }
                onProgress(processed)
            }
        }

        return DuplicateCleanupReceipt(
            deleted, skipped, failed, verified, bytesReclaimed, !cancelRequested
        )
    }

    private suspend fun verify(group: DuplicateGroup, entry: DuplicateEntry): Boolean {
        val current = try {
            source.document(entry.documentId)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return false
        }
        if (current == null || current.bytes != group.bytes || current.bytes != entry.bytes) {
            return false
        }
        val digest = try {
            contentSource.hash(entry.documentId)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return false
        }
        return digest.toHex() == group.sha256
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

fun sha256OfBytes(bytes: ByteArray): ByteArray =
    MessageDigest.getInstance("SHA-256").digest(bytes)
