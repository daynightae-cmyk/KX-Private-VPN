package com.knoux.kxprivatevpn.platform.repair

import android.os.StatFs
import com.knoux.kxprivatevpn.platform.storage.CacheMaintenance
import com.knoux.kxprivatevpn.platform.storage.CachePreview
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import kotlin.coroutines.coroutineContext

/**
 * What the Repair Lab is actually allowed to see: the internal data volume through StatFs and
 * KX's own cache directory. A null field means the measurement failed; it is never replaced by
 * zero, an estimate or a fabricated value.
 */
data class RepairMeasurement(
    val totalBytes: Long?,
    val availableBytes: Long?,
    val cacheBytes: Long?,
    val cacheFileCount: Int,
    val cacheWalkLimited: Boolean,
    val preview: CachePreview?,
    val previewFailed: Boolean,
    val atMillis: Long,
) {
    val volumeMeasured: Boolean get() = totalBytes != null && availableBytes != null
    val cacheMeasured: Boolean get() = cacheBytes != null
}

/** Real measurement only. No permission is requested and no other application data is read. */
class AndroidRepairMeasurer(
    private val filesDir: File,
    private val cacheDir: File,
    private val now: () -> Long = System::currentTimeMillis,
) {
    suspend fun measure(): RepairMeasurement = withContext(Dispatchers.IO) {
        var total: Long? = null
        var available: Long? = null
        try {
            val stat = StatFs(filesDir.absolutePath)
            total = stat.totalBytes
            available = stat.availableBytes
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Leave null: the UI must say "unavailable", not zero.
        }

        var cacheBytes: Long? = null
        var cacheFiles = 0
        var limited = false
        try {
            val root = cacheDir.canonicalFile
            var bytes = 0L
            if (root.exists()) {
                for (file in root.walkTopDown().onEnter {
                    !Files.isSymbolicLink(it.toPath()) && it.canonicalFile.toPath().startsWith(root.toPath())
                }) {
                    coroutineContext.ensureActive()
                    if (!file.isFile) continue
                    if (cacheFiles >= MAX_CACHE_FILES) { limited = true; break }
                    cacheFiles++
                    bytes += file.length()
                }
            }
            cacheBytes = bytes
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            cacheFiles = 0
        }

        var preview: CachePreview? = null
        var previewFailed = false
        try {
            preview = CacheMaintenance(cacheDir).preview()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            previewFailed = true
        }

        RepairMeasurement(
            totalBytes = total,
            availableBytes = available,
            cacheBytes = cacheBytes,
            cacheFileCount = cacheFiles,
            cacheWalkLimited = limited,
            preview = preview,
            previewFailed = previewFailed,
            atMillis = now(),
        )
    }

    companion object {
        const val MAX_CACHE_FILES = 20_000
    }
}
