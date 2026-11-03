package com.knoux.kxprivatevpn.platform.storage

import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.coroutineContext

data class CacheCandidate(val path: String, val bytes: Long, val modified: Long, val sha256: String)
data class CachePreview(val files: List<CacheCandidate>, val limited: Boolean)
data class CacheReceipt(val deleted: Int, val removedFileBytes: Long, val skipped: List<String>, val failed: List<String>)

/** Production callers supply Context.cacheDir only. Never scans or deletes other app data. */
class CacheMaintenance(private val cacheDirectory: File) {
    private val root get() = cacheDirectory.canonicalFile
    private companion object { val mutex = Mutex() }
    private fun safe(relative: String): File? {
        if (relative.isBlank() || File(relative).isAbsolute) return null
        val candidate = File(root, relative)
        val normalized = candidate.toPath().normalize()
        if (!normalized.startsWith(root.toPath()) || normalized == root.toPath()) return null
        var step: File? = candidate
        while (step != null && step != root) {
            if (Files.isSymbolicLink(step.toPath())) return null
            step = step.parentFile
        }
        if (!candidate.canonicalFile.toPath().startsWith(root.toPath())) return null
        return candidate.takeIf { it.isFile }
    }
    private suspend fun digest(file: File): String {
        val hash = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(65536)
            while (true) {
                coroutineContext.ensureActive()
                val count = input.read(buffer)
                if (count < 0) break
                hash.update(buffer, 0, count)
            }
        }
        return hash.digest().joinToString("") { "%02x".format(it) }
    }
    suspend fun preview(): CachePreview = mutex.withLock {
        val values = mutableListOf<CacheCandidate>()
        var limited = false
        if (root.exists()) for (file in root.walkTopDown().onEnter {
            !Files.isSymbolicLink(it.toPath()) && it.canonicalFile.toPath().startsWith(root.toPath())
        }) {
            coroutineContext.ensureActive()
            if (!file.isFile) continue
            if (values.size >= 500) { limited = true; break }
            val relative = root.toPath().relativize(file.toPath()).toString()
            val permitted = safe(relative) ?: continue
            val beforeSize = permitted.length()
            val modified = permitted.lastModified()
            val hash = digest(permitted)
            if (beforeSize == permitted.length() && modified == permitted.lastModified())
                values += CacheCandidate(relative, beforeSize, modified, hash)
        }
        CachePreview(values.sortedByDescending { it.bytes }, limited)
    }
    suspend fun removeApproved(preview: CachePreview, selected: Set<String>, approved: Boolean,
        checkpoint: (CacheReceipt, Boolean) -> Unit = { _, _ -> }): CacheReceipt = mutex.withLock {
        require(approved) { "Explicit approval required" }
        require(selected.all { path -> preview.files.any { it.path == path } }) { "Unknown candidate" }
        var deleted = 0
        var bytes = 0L
        val skipped = mutableListOf<String>()
        val failed = mutableListOf<String>()
        fun receipt() = CacheReceipt(deleted, bytes, skipped.toList(), failed.toList())
        checkpoint(receipt(), false)
        for (item in preview.files.filter { it.path in selected }) {
            coroutineContext.ensureActive()
            val file = safe(item.path)
            if (file == null || file.length() != item.bytes || file.lastModified() != item.modified) {
                skipped += item.path; checkpoint(receipt(), false); continue
            }
            try {
                if (digest(file) != item.sha256) { skipped += item.path; checkpoint(receipt(), false); continue }
                if (file.delete() && !file.exists()) { deleted++; bytes += item.bytes }
                else failed += item.path
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (_: Exception) { failed += item.path }
            checkpoint(receipt(), false)
        }
        receipt().also { checkpoint(it, true) }
    }
}
