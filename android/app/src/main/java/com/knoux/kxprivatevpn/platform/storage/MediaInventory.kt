package com.knoux.kxprivatevpn.platform.storage

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

data class MediaCategoryTotal(
    val category: StorageCategory,
    val bytes: Long?,
    val files: Int,
    val permitted: Boolean,
    val failed: Boolean = false,
    val partial: Boolean = false,
)

data class MediaInventory(
    val categories: List<MediaCategoryTotal>,
    val truncated: Boolean,
    val atMillis: Long,
    val partialAccess: Boolean = false,
)

/** Android version to permission mapping, kept pure so the matrix is JVM-testable. */
object StoragePermissions {
    fun required(sdk: Int): List<String> = when {
        sdk >= 34 -> listOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        )
        sdk >= 33 -> listOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO,
        )
        else -> listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun forCategory(category: StorageCategory, sdk: Int): String? = when {
        sdk >= 33 -> when (category) {
            StorageCategory.IMAGES -> Manifest.permission.READ_MEDIA_IMAGES
            StorageCategory.VIDEOS -> Manifest.permission.READ_MEDIA_VIDEO
            StorageCategory.AUDIO -> Manifest.permission.READ_MEDIA_AUDIO
            else -> null
        }
        else -> Manifest.permission.READ_EXTERNAL_STORAGE
    }

    /** Android 13+ media permissions never expose documents; SAF is the honest path there. */
    fun documentsAvailable(sdk: Int): Boolean = sdk in 29..32

    /** Android 14+ can grant only user-selected photos/videos instead of the whole library. */
    fun partialVisual(sdk: Int): Boolean = sdk >= 34
}

/**
 * Aggregates only rows the platform actually returns. A revoked permission or a failed cursor
 * yields a null byte total; nothing is replaced by zero.
 */
class MediaStoreInventory(
    private val context: Context,
    private val sdk: Int = Build.VERSION.SDK_INT,
    private val now: () -> Long = System::currentTimeMillis,
) {
    fun requiredPermissions(): List<String> = StoragePermissions.required(sdk)

    fun grantedPermissions(): Set<String> = requiredPermissions().filter {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }.toSet()

    suspend fun scan(): MediaInventory = withContext(Dispatchers.IO) {
        var truncated = false
        val values = mutableListOf<MediaCategoryTotal>()
        listOf(
            StorageCategory.IMAGES to MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            StorageCategory.VIDEOS to MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            StorageCategory.AUDIO to MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        ).forEach { (category, uri) ->
            val permission = StoragePermissions.forCategory(category, sdk)
            val full = permission != null && has(permission)
            val partial = !full && StoragePermissions.partialVisual(sdk) &&
                (category == StorageCategory.IMAGES || category == StorageCategory.VIDEOS) &&
                has(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
            if (!full && !partial) {
                values += MediaCategoryTotal(category, null, 0, permitted = false)
            } else {
                val result = query(uri)
                truncated = truncated || result.truncated
                values += MediaCategoryTotal(category, result.bytes, result.files, true, result.failed,
                    partial = partial)
            }
        }
        values += documents()
        MediaInventory(values, truncated, now(), values.any { it.partial })
    }

    private fun has(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private suspend fun documents(): MediaCategoryTotal {
        if (sdk >= 33) {
            return MediaCategoryTotal(StorageCategory.DOCUMENTS, null, 0, permitted = false)
        }
        if (!StoragePermissions.documentsAvailable(sdk)) {
            return MediaCategoryTotal(StorageCategory.DOCUMENTS, null, 0, permitted = false)
        }
        if (!has(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            return MediaCategoryTotal(StorageCategory.DOCUMENTS, null, 0, permitted = false)
        }
        val uri = MediaStore.Files.getContentUri("external")
        val result = query(uri, "${MediaStore.Files.FileColumns.MEDIA_TYPE}=?",
            arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_DOCUMENT.toString()))
        return MediaCategoryTotal(StorageCategory.DOCUMENTS, result.bytes, result.files, true,
            result.failed)
    }

    private data class QueryResult(val bytes: Long?, val files: Int, val truncated: Boolean,
                                   val failed: Boolean)

    private suspend fun query(
        uri: android.net.Uri,
        selection: String? = null,
        args: Array<String>? = null,
    ): QueryResult {
        val projection = arrayOf(MediaStore.MediaColumns.SIZE)
        return try {
            var bytes = 0L
            var files = 0
            var rows = 0
            context.contentResolver.query(uri, projection, selection, args, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                while (cursor.moveToNext()) {
                    coroutineContext.ensureActive()
                    if (rows++ >= MAX_ROWS) return@use
                    val size = if (cursor.isNull(sizeIndex)) 0L else cursor.getLong(sizeIndex)
                    if (size <= 0) continue
                    bytes += size
                    files++
                }
            } ?: return QueryResult(null, 0, false, true)
            QueryResult(bytes, files, rows > MAX_ROWS, false)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            QueryResult(null, 0, false, true)
        }
    }

    companion object {
        const val MAX_ROWS = 200_000
    }
}
