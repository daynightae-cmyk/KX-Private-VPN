package com.knoux.kxprivatevpn.features

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.knoux.kxprivatevpn.platform.repair.DownloadAction
import com.knoux.kxprivatevpn.platform.repair.DownloadActionResult
import com.knoux.kxprivatevpn.platform.repair.DownloadCandidate
import com.knoux.kxprivatevpn.platform.repair.DownloadHealth
import com.knoux.kxprivatevpn.platform.storage.DownloadCatalog
import com.knoux.kxprivatevpn.platform.storage.DownloadListing
import com.knoux.kxprivatevpn.platform.storage.DownloadRow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** What one approved operation really did. Every number here comes from the platform. */
data class DownloadReceipt(
    val action: DownloadAction,
    val attempted: Int,
    val succeeded: Int,
    val failed: Int,
    val bytesCopied: Long,
    val bytesRemoved: Long,
    val results: List<DownloadActionResult>,
    val atMillis: Long,
)

data class DownloadRescueUiState(
    val loading: Boolean = true,
    val treeName: String? = null,
    val treeUri: String? = null,
    val scanning: Boolean = false,
    val scanProgress: Int = 0,
    val candidates: List<DownloadCandidate> = emptyList(),
    val truncated: Boolean = false,
    val scanFailed: Boolean = false,
    val mediaStoreRows: Int = 0,
    val mediaStoreFailed: Boolean = false,
    val selected: Set<String> = emptySet(),
    val confirming: Boolean = false,
    val busy: Boolean = false,
    val receipt: DownloadReceipt? = null,
    val message: String? = null,
) {
    val inspected: Int get() = candidates.size
    val attention: Int get() = candidates.count { it.health.needsAttention }
    val selectedCandidates: List<DownloadCandidate>
        get() = candidates.filter { selected.contains(it.documentId) }
    val selectedBytes: Long get() = selectedCandidates.sumOf { it.bytes }
    val byHealth: Map<DownloadHealth, Int>
        get() = candidates.groupingBy { it.health }.eachCount()
}

/** Persisted grant of the folder the user authorised for Download Rescue. */
class DownloadRescueStore(context: Context) {
    private val preferences = context.getSharedPreferences("download_rescue", Context.MODE_PRIVATE)
    var treeUri: String?
        get() = preferences.getString("tree_uri", null)
        set(value) = preferences.edit().putString("tree_uri", value).apply()
    var treeName: String?
        get() = preferences.getString("tree_name", null)
        set(value) = preferences.edit().putString("tree_name", value).apply()
    fun clear() { preferences.edit().clear().apply() }
}

/**
 * Download Rescue.
 *
 * The screen only ever reports what the platform returned: bytes were read from the granted
 * document, a validator judged them, and an action is only performed on the documents the user
 * selected and approved.
 */
class DownloadRescueViewModel(app: Application) : AndroidViewModel(app) {
    private val store = DownloadRescueStore(app)
    private val catalog = DownloadCatalog(app)
    private val mutable = MutableStateFlow(DownloadRescueUiState())
    private var scanJob: Job? = null
    private var actionJob: Job? = null
    /** The real document URI of every inspected row, keyed by its provider id. */
    private val uriByDocument = mutableMapOf<String, String>()
    val state: StateFlow<DownloadRescueUiState> = mutable.asStateFlow()

    init {
        mutable.update { it.copy(loading = false, treeUri = store.treeUri, treeName = store.treeName) }
    }

    /** The Activity result already granted the folder; the grant is persisted when possible. */
    fun attachTree(uri: Uri) {
        val name = uri.lastPathSegment?.substringAfterLast(':')?.substringAfterLast('/') ?: "Downloads"
        viewModelScope.launch {
            try {
                catalog.takePersistableGrant(uri)
            } catch (_: Exception) {
                // A grant that cannot be persisted still works for this session; the next launch
                // shows no stored folder instead of a fake one.
            }
            store.treeUri = uri.toString()
            store.treeName = name
            mutable.update { it.copy(treeUri = uri.toString(), treeName = name) }
            scan()
        }
    }

    fun forgetTree() {
        val uri = mutable.value.treeUri
        if (uri != null) {
            try {
                getApplication<Application>().contentResolver.releasePersistableUriPermission(
                    Uri.parse(uri),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            } catch (_: Exception) {
                // The grant may already be gone; the local state is cleared regardless.
            }
        }
        store.clear()
        mutable.update {
            it.copy(treeUri = null, treeName = null, candidates = emptyList(), selected = emptySet(),
                truncated = false, scanFailed = false, mediaStoreRows = 0, mediaStoreFailed = false,
                receipt = null, message = null)
        }
    }

    /** Reads the authorised folder, judges every file by its real bytes and reports the result. */
    fun scan() {
        val uri = mutable.value.treeUri ?: return
        if (scanJob?.isActive == true) return
        mutable.update {
            it.copy(scanning = true, scanProgress = 0, scanFailed = false, truncated = false,
                receipt = null, message = null)
        }
        scanJob = viewModelScope.launch {
            try {
                val listing = catalog.listTree(Uri.parse(uri))
                val media = catalog.listMediaStore()
                val treeUris = listing.rows.map { it.uri }.toSet()
                val rows = listing.rows + media.rows.filter { row -> row.uri !in treeUris }
                uriByDocument.clear()
                rows.forEach { row -> uriByDocument[row.documentId] = row.uri }
                val judged = mutableListOf<DownloadCandidate>()
                rows.forEachIndexed { index, row ->
                    judged += catalog.classify(row)
                    mutable.update { it.copy(scanProgress = index + 1) }
                }
                mutable.update {
                    it.copy(
                        scanning = false,
                        candidates = judged,
                        truncated = listing.truncated || media.truncated,
                        scanFailed = listing.failed,
                        mediaStoreRows = media.rows.size,
                        mediaStoreFailed = media.failed,
                        selected = emptySet(),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutable.update { it.copy(scanning = false, scanFailed = true) }
            }
        }
    }

    fun toggle(documentId: String) = mutable.update { current ->
        val next = if (current.selected.contains(documentId)) current.selected - documentId
        else current.selected + documentId
        current.copy(selected = next)
    }

    fun selectAttention() = mutable.update { current ->
        val ids = current.candidates.filter { it.health.needsAttention }.map { it.documentId }
        current.copy(selected = ids.toSet())
    }

    fun clearSelection() = mutable.update { it.copy(selected = emptySet(), confirming = false) }

    /** Removal is a destructive action: it is only performed after the user confirms. */
    fun requestRemove() {
        if (mutable.value.selected.isEmpty()) return
        mutable.update { it.copy(confirming = true) }
    }

    fun dismissConfirmation() = mutable.update { it.copy(confirming = false) }

    /** Removes exactly the selected documents and reports what the platform really did. */
    fun confirmRemove() {
        val current = mutable.value
        val targets = current.selectedCandidates
        if (targets.isEmpty()) {
            mutable.update { it.copy(confirming = false) }
            return
        }
        mutable.update { it.copy(confirming = false, busy = true) }
        actionJob = viewModelScope.launch {
            val results = mutableListOf<DownloadActionResult>()
            var removedBytes = 0L
            targets.forEach { candidate ->
                val result = catalog.remove(uriOf(candidate), candidate.displayName)
                results += result
                if (result.succeeded) removedBytes += candidate.bytes
            }
            val receipt = DownloadReceipt(
                action = DownloadAction.REMOVE,
                attempted = targets.size,
                succeeded = results.count { it.succeeded },
                failed = results.count { !it.succeeded },
                bytesCopied = 0L,
                bytesRemoved = removedBytes,
                results = results,
                atMillis = System.currentTimeMillis(),
            )
            mutable.update { it.copy(busy = false, receipt = receipt, selected = emptySet()) }
            scan()
        }
    }

    /** Copies the selected documents to the destination the user picked and verifies the size. */
    fun exportTo(target: Uri) {
        val current = mutable.value
        val targets = current.selectedCandidates
        if (targets.isEmpty()) return
        mutable.update { it.copy(busy = true) }
        actionJob = viewModelScope.launch {
            val results = mutableListOf<DownloadActionResult>()
            var copied = 0L
            targets.forEach { candidate ->
                val result = catalog.export(uriOf(candidate), target, candidate.bytes)
                results += result
                if (result.succeeded) copied += result.bytesCopied
            }
            val receipt = DownloadReceipt(
                action = DownloadAction.EXPORT,
                attempted = targets.size,
                succeeded = results.count { it.succeeded },
                failed = results.count { !it.succeeded },
                bytesCopied = copied,
                bytesRemoved = 0L,
                results = results,
                atMillis = System.currentTimeMillis(),
            )
            mutable.update { it.copy(busy = false, receipt = receipt) }
        }
    }

    /** Renames the selected document inside the granted folder. The bytes are untouched. */
    /** The document URI that was really read for a candidate. */
    private fun uriOf(candidate: DownloadCandidate): Uri =
        uriByDocument[candidate.documentId]?.let { Uri.parse(it) } ?: Uri.EMPTY

    fun renameSelected(newName: String) {
        val current = mutable.value
        val target = current.selectedCandidates.firstOrNull() ?: return
        val name = newName.trim()
        if (name.isEmpty()) return
        mutable.update { it.copy(busy = true) }
        actionJob = viewModelScope.launch {
            val result = catalog.rename(uriOf(target), name)
            val receipt = DownloadReceipt(
                action = DownloadAction.RENAME,
                attempted = 1,
                succeeded = if (result.succeeded) 1 else 0,
                failed = if (result.succeeded) 0 else 1,
                bytesCopied = 0L,
                bytesRemoved = 0L,
                results = listOf(result),
                atMillis = System.currentTimeMillis(),
            )
            mutable.update { it.copy(busy = false, receipt = receipt) }
            scan()
        }
    }
}
