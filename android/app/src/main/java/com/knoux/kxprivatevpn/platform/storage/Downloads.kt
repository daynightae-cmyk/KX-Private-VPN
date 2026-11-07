package com.knoux.kxprivatevpn.platform.storage

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.os.Build
import android.provider.MediaStore
import com.knoux.kxprivatevpn.platform.repair.DownloadAction
import com.knoux.kxprivatevpn.platform.repair.DownloadActionResult
import com.knoux.kxprivatevpn.platform.repair.DownloadCandidate
import com.knoux.kxprivatevpn.platform.repair.DownloadFormat
import com.knoux.kxprivatevpn.platform.repair.DownloadHealth
import com.knoux.kxprivatevpn.platform.repair.DownloadValidator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.coroutineContext

/**
 * Android version rules for the shared download collection, kept pure so the matrix is
 * JVM-testable. Android before 10 has no shared download collection at all.
 */
object DownloadCollections {
    const val MEDIA_STORE_SINCE = 29

    fun mediaStoreSupported(sdk: Int): Boolean = sdk >= MEDIA_STORE_SINCE
}

/** One file the provider reported, before its bytes were inspected. */
data class DownloadRow(
    val documentId: String,
    val displayName: String,
    val mime: String,
    val bytes: Long,
    val lastModified: Long,
    val uri: String,
)

data class DownloadListing(
    val rows: List<DownloadRow>,
    val truncated: Boolean = false,
    val failed: Boolean = false,
    /** The shared download collection does not exist on this Android version. */
    val unavailable: Boolean = false,
)

/**
 * Reads and acts on files the user owns.
 *
 * Every operation works on a URI the user granted. Nothing here reaches into another
 * application's private directory, and a destructive action is only ever performed on the exact
 * document the user selected.
 */
class DownloadCatalog(private val context: Context) {

    /** Persists the folder grant so the same authorised scope survives a restart. */
    fun takePersistableGrant(treeUri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            treeUri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
    }

    /** The document URI of a row inside the granted tree. */
    fun documentUri(treeUri: Uri, documentId: String): Uri =
        DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)

    /** Lists the files of a user-granted tree. Only the granted tree is read. */
    suspend fun listTree(treeUri: Uri, limit: Int = MAX_ROWS): DownloadListing =
        withContext(Dispatchers.IO) {
            try {
                val root = DocumentsContract.getTreeDocumentId(treeUri)
                val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, root)
                val projection = arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_SIZE,
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                )
                val rows = mutableListOf<DownloadRow>()
                var truncated = false
                context.contentResolver.query(children, projection, null, null, null)?.use { cursor ->
                    val idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    val sizeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                    val modifiedIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                    while (cursor.moveToNext()) {
                        coroutineContext.ensureActive()
                        if (rows.size >= limit) { truncated = true; break }
                        val id = if (idIndex >= 0) cursor.getString(idIndex) else null ?: continue
                        val name = if (nameIndex >= 0) cursor.getString(nameIndex) else null ?: continue
                        val mime = if (mimeIndex >= 0) cursor.getString(mimeIndex) else null ?: ""
                        val size = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else -1L
                        val modified = if (modifiedIndex >= 0 && !cursor.isNull(modifiedIndex)) cursor.getLong(modifiedIndex) else 0L
                        rows += DownloadRow(
                            documentId = id,
                            displayName = name,
                            mime = mime,
                            bytes = size,
                            lastModified = modified,
                            uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id).toString(),
                        )
                    }
                }
                DownloadListing(rows, truncated = truncated)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                DownloadListing(emptyList(), failed = true)
            }
        }

    /**
     * Lists the shared download collection through MediaStore. Android only exposes the rows it
     * is allowed to share, so an empty result is reported as an empty result and never padded.
     */
    suspend fun listMediaStore(limit: Int = MAX_ROWS): DownloadListing = withContext(Dispatchers.IO) {
        // The guard is written the way lint reads it: the shared collection exists from Android
        // 10. Below that the authorised SAF folder stays the only source and the collection is
        // reported as unavailable instead of being faked with a count of zero.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val projection = arrayOf(
                    MediaStore.Downloads._ID,
                    MediaStore.Downloads.DISPLAY_NAME,
                    MediaStore.Downloads.MIME_TYPE,
                    MediaStore.Downloads.SIZE,
                    MediaStore.Downloads.DATE_MODIFIED,
                )
                val rows = mutableListOf<DownloadRow>()
                var truncated = false
                context.contentResolver.query(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI, projection, null, null,
                    MediaStore.Downloads.DATE_MODIFIED + " DESC",
                )?.use { cursor ->
                val idIndex = cursor.getColumnIndex(MediaStore.Downloads._ID)
                val nameIndex = cursor.getColumnIndex(MediaStore.Downloads.DISPLAY_NAME)
                val mimeIndex = cursor.getColumnIndex(MediaStore.Downloads.MIME_TYPE)
                val sizeIndex = cursor.getColumnIndex(MediaStore.Downloads.SIZE)
                val modifiedIndex = cursor.getColumnIndex(MediaStore.Downloads.DATE_MODIFIED)
                while (cursor.moveToNext()) {
                    coroutineContext.ensureActive()
                    if (rows.size >= limit) { truncated = true; break }
                    val id = if (idIndex >= 0) cursor.getLong(idIndex) else -1L
                    val name = if (nameIndex >= 0) cursor.getString(nameIndex) else null ?: continue
                    val mime = if (mimeIndex >= 0) cursor.getString(mimeIndex) else null ?: ""
                    val size = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else -1L
                    val modified = if (modifiedIndex >= 0 && !cursor.isNull(modifiedIndex)) cursor.getLong(modifiedIndex) * 1000L else 0L
                    rows += DownloadRow(
                        documentId = id.toString(),
                        displayName = name,
                        mime = mime,
                        bytes = size,
                        lastModified = modified,
                        uri = Uri.withAppendedPath(
                            MediaStore.Downloads.EXTERNAL_CONTENT_URI, id.toString(),
                        ).toString(),
                    )
                }
                }
                DownloadListing(rows, truncated = truncated)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                DownloadListing(emptyList(), failed = true)
            }
        } else {
            DownloadListing(emptyList(), unavailable = true)
        }
    }

    /**
     * Reads the real head and tail bytes of a document so the validator can judge it. The tail
     * window follows the format: a ZIP end of central directory record is 22 bytes plus an
     * optional comment, so a fixed small window would miss a perfectly intact archive.
     */
    suspend fun readEdges(
        documentUri: Uri,
        bytes: Long,
        tailWindow: Int,
    ): Pair<ByteArray?, ByteArray?> =
        withContext(Dispatchers.IO) {
            val head = try {
                context.contentResolver.openInputStream(documentUri)?.use { stream ->
                    val buffer = ByteArray(DownloadValidator.WINDOW)
                    val read = stream.read(buffer)
                    if (read <= 0) null else buffer.copyOf(read)
                }
            } catch (_: Exception) {
                null
            }
            val tail = try {
                if (bytes <= 0L || tailWindow <= 0) null else context.contentResolver.openInputStream(documentUri)?.use { stream ->
                    val length = bytes.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                    val window = tailWindow.coerceAtMost(length)
                    val start = (length - window).toLong()
                    stream.skip(start)
                    val buffer = ByteArray(window)
                    var offset = 0
                    while (offset < window) {
                        val read = stream.read(buffer, offset, window - offset)
                        if (read <= 0) break
                        offset += read
                    }
                    if (offset <= 0) null else buffer.copyOf(offset)
                }
            } catch (_: Exception) {
                null
            }
            head to tail
        }

    /** Classifies one row by reading its real bytes. */
    suspend fun classify(row: DownloadRow): DownloadCandidate {
        val format = DownloadFormat.fromMime(row.mime, row.displayName)?.first
        val window = DownloadValidator.tailWindow(format, row.bytes)
        val (head, tail) = readEdges(Uri.parse(row.uri), row.bytes, window)
        val candidate = DownloadValidator.classify(row.mime, row.displayName, row.bytes, head, tail)
        return candidate.copy(
            documentId = row.documentId,
            lastModified = row.lastModified,
        )
    }

    /**
     * Copies the document to a location the user picked and verifies the copied byte count.
     * The source is never modified.
     */
    suspend fun export(documentUri: Uri, target: Uri, expected: Long): DownloadActionResult =
        withContext(Dispatchers.IO) {
            var copied = 0L
            try {
                context.contentResolver.openInputStream(documentUri)?.use { input ->
                    context.contentResolver.openOutputStream(target, "wt")?.use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            coroutineContext.ensureActive()
                            val read = input.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            copied += read
                        }
                        output.flush()
                    } ?: throw IOException("target not writable")
                } ?: throw IOException("source not readable")
                DownloadActionResult(
                    documentId = documentUri.toString(),
                    displayName = target.lastPathSegment ?: "",
                    action = DownloadAction.EXPORT,
                    succeeded = copied == expected,
                    bytesCopied = copied,
                    bytesExpected = expected,
                    message = if (copied == expected) "copied" else "size_mismatch",
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                DownloadActionResult(
                    documentId = documentUri.toString(),
                    displayName = "",
                    action = DownloadAction.EXPORT,
                    succeeded = false,
                    bytesCopied = copied,
                    bytesExpected = expected,
                    message = "export_failed",
                )
            }
        }

    /** Renames a document inside the granted tree. The bytes are untouched. */
    suspend fun rename(documentUri: Uri, newName: String): DownloadActionResult =
        withContext(Dispatchers.IO) {
            try {
                val renamed = DocumentsContract.renameDocument(context.contentResolver, documentUri, newName)
                DownloadActionResult(
                    documentId = documentUri.toString(),
                    displayName = newName,
                    action = DownloadAction.RENAME,
                    succeeded = renamed != null,
                    message = if (renamed != null) "renamed" else "rename_rejected",
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                DownloadActionResult(
                    documentId = documentUri.toString(),
                    displayName = newName,
                    action = DownloadAction.RENAME,
                    succeeded = false,
                    message = "rename_failed",
                )
            }
        }

    /** Removes exactly the document the user approved. */
    suspend fun remove(documentUri: Uri, displayName: String): DownloadActionResult =
        withContext(Dispatchers.IO) {
            try {
                val deleted = DocumentsContract.deleteDocument(context.contentResolver, documentUri)
                DownloadActionResult(
                    documentId = documentUri.toString(),
                    displayName = displayName,
                    action = DownloadAction.REMOVE,
                    succeeded = deleted,
                    message = if (deleted) "removed" else "delete_rejected",
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                DownloadActionResult(
                    documentId = documentUri.toString(),
                    displayName = displayName,
                    action = DownloadAction.REMOVE,
                    succeeded = false,
                    message = "remove_failed",
                )
            }
        }

    companion object {
        const val MAX_ROWS = 500
    }
}

/** Formats a byte count for the result cards. */
fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    bytes < 1024L * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
    else -> "${bytes / (1024L * 1024 * 1024)} GB"
}

/** The health a file is reported with when no validator applies. */
val UNSUPPORTED_HEALTH = DownloadHealth.UNSUPPORTED

/** Convenience for the UI: the label key of a format. */
val DownloadFormat?.label: String
    get() = when (this) {
        DownloadFormat.PDF -> "PDF"
        DownloadFormat.PNG -> "PNG"
        DownloadFormat.JPEG -> "JPEG"
        DownloadFormat.MP4 -> "MP4"
        DownloadFormat.ZIP -> "ZIP"
        null -> "unknown"
    }
