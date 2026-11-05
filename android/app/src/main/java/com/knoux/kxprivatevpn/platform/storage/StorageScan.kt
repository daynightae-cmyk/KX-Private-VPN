package com.knoux.kxprivatevpn.platform.storage

/**
 * Pure inventory model for the Storage X-Ray. Every byte here came from a real MediaStore row or
 * a user-authorized SAF document. Unknown MIME types are classified as OTHER, never silently
 * dropped, and a failed category keeps a null measurement instead of zero.
 */
enum class StorageCategory { IMAGES, VIDEOS, AUDIO, DOCUMENTS, OTHER }

data class StorageEntry(
    val relativePath: String,
    val bytes: Long,
    val mime: String,
    val lastModified: Long = 0,
)

data class CategoryTotal(val category: StorageCategory, val bytes: Long, val files: Int)

data class FolderSlice(val name: String, val path: String, val bytes: Long, val files: Int)
data class FileSlice(val name: String, val path: String, val bytes: Long)
data class FolderChildren(val folders: List<FolderSlice>, val files: List<FileSlice>)

data class StorageScan(
    val entries: List<StorageEntry>,
    val truncated: Boolean = false,
    val cancelled: Boolean = false,
    val atMillis: Long = 0,
    val failed: Boolean = false,
) {
    val totalBytes: Long get() = entries.sumOf { it.bytes }
    val totalFiles: Int get() = entries.size

    fun categories(): List<CategoryTotal> {
        val byCategory = entries.groupBy { categorize(it.mime, it.relativePath.substringAfterLast('/')) }
        return StorageCategory.entries.map { category ->
            val values = byCategory[category].orEmpty()
            CategoryTotal(category, values.sumOf { it.bytes }, values.size)
        }
    }

    fun topFiles(limit: Int = 20): List<FileSlice> {
        if (limit <= 0) return emptyList()
        return entries.sortedByDescending { it.bytes }.take(limit).map {
            FileSlice(it.relativePath.substringAfterLast('/'), it.relativePath, it.bytes)
        }
    }

    /** Direct children of [prefix] with descendants aggregated; paths are relative to the tree. */
    fun children(prefix: String = ""): FolderChildren {
        val clean = prefix.trim('/')
        val base = if (clean.isEmpty()) "" else "$clean/"
        val folders = mutableMapOf<String, Pair<Long, Int>>()
        val files = mutableListOf<FileSlice>()
        entries.forEach { entry ->
            val path = entry.relativePath.trim('/')
            if (base.isNotEmpty() && !path.startsWith(base)) return@forEach
            val remainder = path.removePrefix(base)
            if (remainder.isEmpty()) return@forEach
            val slash = remainder.indexOf('/')
            if (slash >= 0) {
                val name = remainder.substring(0, slash)
                val key = if (base.isEmpty()) name else base + name
                val current = folders[key] ?: (0L to 0)
                folders[key] = (current.first + entry.bytes) to (current.second + 1)
            } else {
                files += FileSlice(remainder, path, entry.bytes)
            }
        }
        return FolderChildren(
            folders = folders.map { (path, value) ->
                FolderSlice(path.substringAfterLast('/'), path, value.first, value.second)
            }.sortedByDescending { it.bytes },
            files = files.sortedByDescending { it.bytes },
        )
    }
}

data class StorageReconciliation(
    val usedBytes: Long,
    val freeBytes: Long,
    val accessibleBytes: Long,
    val unexplainedBytes: Long,
)

/** Clamps every real measurement; never invents a total that exceeds the volume. */
fun reconcileStorage(totalBytes: Long?, availableBytes: Long?, accessibleBytes: Long): StorageReconciliation? {
    if (totalBytes == null || availableBytes == null || totalBytes <= 0) return null
    val total = totalBytes.coerceAtLeast(0)
    val free = availableBytes.coerceIn(0, total)
    val used = total - free
    val accessible = accessibleBytes.coerceIn(0, used)
    return StorageReconciliation(used, free, accessible, used - accessible)
}

private val imageExtensions = setOf("jpg", "jpeg", "png", "gif", "webp", "heic", "heif", "bmp", "dng", "tif", "tiff")
private val videoExtensions = setOf("mp4", "mkv", "webm", "mov", "avi", "3gp", "m4v", "ts", "flv")
private val audioExtensions = setOf("mp3", "m4a", "aac", "wav", "flac", "ogg", "opus", "amr", "mid", "midi")
private val documentExtensions = setOf(
    "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md", "csv", "json", "xml",
    "zip", "rar", "7z", "tar", "gz", "epub", "rtf", "odt", "ods", "odp",
)

fun categorize(mime: String?, name: String): StorageCategory {
    val type = mime.orEmpty().lowercase()
    val extension = name.substringAfterLast('.', "").lowercase()
    return when {
        type.startsWith("image/") || extension in imageExtensions -> StorageCategory.IMAGES
        type.startsWith("video/") || extension in videoExtensions -> StorageCategory.VIDEOS
        type.startsWith("audio/") || extension in audioExtensions -> StorageCategory.AUDIO
        type.startsWith("text/") || type in documentMimes || extension in documentExtensions ->
            StorageCategory.DOCUMENTS
        else -> StorageCategory.OTHER
    }
}

private val documentMimes = setOf(
    "application/pdf", "application/msword", "application/rtf",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.ms-excel",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "application/vnd.ms-powerpoint",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    "application/zip", "application/x-rar-compressed", "application/x-7z-compressed",
    "application/json", "application/xml", "application/epub+zip",
)
