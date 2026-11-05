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
    private data class Volume(val total: Long?, val available: Long?)
    private data class CacheWalk(val bytes: Long?, val files: Int, val limited: Boolean)
    private data class PreviewResult(val value: CachePreview?, val failed: Boolean)

    suspend fun measure(): RepairMeasurement = withContext(Dispatchers.IO) {
        val volume = measureVolume()
        val cache = measureCache()
        val preview = measurePreview()
        RepairMeasurement(
            totalBytes = volume.total,
            availableBytes = volume.available,
            cacheBytes = cache.bytes,
            cacheFileCount = cache.files,
            cacheWalkLimited = cache.limited,
            preview = preview.value,
            previewFailed = preview.failed,
            atMillis = now(),
        )
    }

    private fun measureVolume(): Volume = try {
        val stat = StatFs(filesDir.absolutePath)
        Volume(stat.totalBytes, stat.availableBytes)
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        // Leave null: the UI must say "unavailable", not zero.
        Volume(null, null)
    }

    private suspend fun measureCache(): CacheWalk {
        var files = 0
        var limited = false
        return try {
            var bytes = 0L
            val root = cacheDir.canonicalFile
            if (root.exists()) {
                for (file in root.walkTopDown().onEnter {
                    !Files.isSymbolicLink(it.toPath()) && it.canonicalFile.toPath().startsWith(root.toPath())
                }) {
                    coroutineContext.ensureActive()
                    if (!file.isFile) continue
                    if (files >= MAX_CACHE_FILES) { limited = true; break }
                    files++
                    bytes += file.length()
                }
            }
            CacheWalk(bytes, files, limited)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            CacheWalk(null, 0, false)
        }
    }

    private suspend fun measurePreview(): PreviewResult = try {
        PreviewResult(CacheMaintenance(cacheDir).preview(), false)
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        PreviewResult(null, true)
    }

    companion object {
        const val MAX_CACHE_FILES = 20_000
    }
}
