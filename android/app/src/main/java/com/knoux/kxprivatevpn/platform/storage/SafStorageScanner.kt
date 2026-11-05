package com.knoux.kxprivatevpn.platform.storage

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

data class DocumentEntry(
    val documentId: String,
    val name: String,
    val mime: String,
    val bytes: Long,
    val lastModified: Long,
)

/** Test seam for SAF traversal; the production implementation reads DocumentsContract. */
interface DocumentSource {
    suspend fun list(documentId: String): List<DocumentEntry>
}

/**
 * Depth-first SAF walk. Only user-granted tree documents are read, every result is a real
 * DocumentsContract row, and the walk stops at a bounded entry count with `truncated = true`.
 */
class SafStorageScanner(
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
    ): StorageScan {
        cancelRequested = false
        val entries = mutableListOf<StorageEntry>()
        var truncated = false
        var failed = false
        val stack = ArrayDeque<Pair<String, String>>() // documentId to relative path
        stack.addLast(rootDocumentId to "")
        while (stack.isNotEmpty()) {
            coroutineContext.ensureActive()
            if (cancelRequested) return StorageScan(entries.toList(), truncated, true, atMillis, failed)
            if (entries.size >= maxEntries) { truncated = true; break }
            val (documentId, prefix) = stack.removeLast()
            val children = try {
                source.list(documentId)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                truncated = true
                failed = true
                continue
            }
            children.forEach { child ->
                if (entries.size >= maxEntries) { truncated = true; return@forEach }
                val depth = if (prefix.isEmpty()) 0 else prefix.count { it == '/' } + 1
                val path = if (prefix.isEmpty()) child.name else "$prefix/${child.name}"
                if (child.mime == MIME_DIRECTORY) {
                    if (depth < maxDepth) stack.addLast(child.documentId to path)
                    else truncated = true
                } else if (child.bytes >= 0) {
                    entries += StorageEntry(path, child.bytes, child.mime, child.lastModified)
                    if (entries.size % 50 == 0) onProgress(entries.size)
                }
            }
            onProgress(entries.size)
        }
        if (cancelRequested && entries.size < maxEntries) {
            return StorageScan(entries.toList(), truncated, true, atMillis, failed)
        }
        return StorageScan(entries.toList(), truncated, false, atMillis, failed)
    }

    companion object {
        const val MAX_ENTRIES = 10_000
        const val MAX_DEPTH = 12
        private const val MIME_DIRECTORY = "vnd.android.document/directory"
    }
}

class AndroidDocumentSource(
    private val context: Context,
    private val treeUri: Uri,
) : DocumentSource {
    override suspend fun list(documentId: String): List<DocumentEntry> = withContext(Dispatchers.IO) {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )
        val values = mutableListOf<DocumentEntry>()
        context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                coroutineContext.ensureActive()
                values += cursor.toEntry()
            }
        }
        values
    }

    suspend fun treeName(): String? = withContext(Dispatchers.IO) {
        val documentId = DocumentsContract.getTreeDocumentId(treeUri)
        val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
        val projection = arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }

    fun takePersistablePermission() {
        context.contentResolver.takePersistableUriPermission(
            treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
    }

    private fun Cursor.toEntry(): DocumentEntry {
        fun column(name: String) = getColumnIndexOrThrow(name)
        val size = if (isNull(column(DocumentsContract.Document.COLUMN_SIZE))) -1L
        else getLong(column(DocumentsContract.Document.COLUMN_SIZE))
        val modified = if (isNull(column(DocumentsContract.Document.COLUMN_LAST_MODIFIED))) 0L
        else getLong(column(DocumentsContract.Document.COLUMN_LAST_MODIFIED))
        return DocumentEntry(
            documentId = getString(column(DocumentsContract.Document.COLUMN_DOCUMENT_ID)),
            name = getString(column(DocumentsContract.Document.COLUMN_DISPLAY_NAME)) ?: "?",
            mime = getString(column(DocumentsContract.Document.COLUMN_MIME_TYPE))
                ?: "application/octet-stream",
            bytes = size,
            lastModified = modified,
        )
    }
}

/** App-private memory of the one chosen SAF tree; no path or document content is stored. */
class StorageXrayStore(context: Context) {
    private val preferences = context.getSharedPreferences("storage_xray", Context.MODE_PRIVATE)

    var treeUri: String?
        get() = preferences.getString("tree_uri", null)
        set(value) = preferences.edit().putString("tree_uri", value).apply()

    var treeName: String?
        get() = preferences.getString("tree_name", null)
        set(value) = preferences.edit().putString("tree_name", value).apply()

    fun clear() { preferences.edit().clear().apply() }
}
