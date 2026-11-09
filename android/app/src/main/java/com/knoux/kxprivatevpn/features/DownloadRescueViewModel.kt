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
import com.knoux.kxprivatevpn.platform.repair.DownloadExportCoordinator
import com.knoux.kxprivatevpn.platform.repair.DownloadHealth
import com.knoux.kxprivatevpn.platform.repair.DownloadTarget
import com.knoux.kxprivatevpn.platform.repair.freezeDownloadSelection
import com.knoux.kxprivatevpn.platform.repair.verifiedRemovalBytes
import com.knoux.kxprivatevpn.platform.storage.DownloadCatalog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    /** Rows inspected by the last completed scan; null until a scan actually succeeds. */
    val lastScanCount: Int? = null,
    val truncated: Boolean = false,
    val scanFailed: Boolean = false,
    val mediaStoreRows: Int = 0,
    val mediaStoreFailed: Boolean = false,
    /** Android before 10 has no shared download collection; the folder stays the only source. */
    val mediaStoreUnavailable: Boolean = false,
    val selected: Set<String> = emptySet(),
    val removalTargets: List<DownloadTarget> = emptyList(),
    val exportPending: Boolean = false,
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
    val confirming: Boolean get() = removalTargets.isNotEmpty()
    val actionsBlocked: Boolean get() = loading || busy || scanning || exportPending || confirming
    val canExport: Boolean get() = !actionsBlocked && selected.size == 1 && selectedCandidates.size == 1
    val canConfirmRemove: Boolean get() = confirming && !loading && !busy && !scanning && !exportPending

    /** Automatic post-action refreshes retain receipts; a deliberate new scan clears them. */
    fun startingScan(clearReceipt: Boolean): DownloadRescueUiState = copy(
        scanning = true, scanProgress = 0, scanFailed = false, truncated = false,
        receipt = if (clearReceipt) null else receipt, message = null,
    )
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
    private val exports = DownloadExportCoordinator()
    /** The real document URI of every inspected row, keyed by its provider id. */
    private val uriByDocument = mutableMapOf<String, String>()
    val state: StateFlow<DownloadRescueUiState> = mutable.asStateFlow()

    init {
        mutable.update { it.copy(loading = false, treeUri = store.treeUri, treeName = store.treeName) }
    }

    /** The Activity result already granted the folder; the grant is persisted when possible. */
    fun attachTree(uri: Uri) {
        if (mutable.value.actionsBlocked) return
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
        if (mutable.value.actionsBlocked) return
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
        uriByDocument.clear()
        mutable.update {
            it.copy(treeUri = null, treeName = null, candidates = emptyList(), selected = emptySet(),
                lastScanCount = null, truncated = false, scanFailed = false,
                mediaStoreRows = 0, mediaStoreFailed = false,
                receipt = null, message = null)
        }
    }

    /** A deliberate new scan clears the previous receipt; automatic refreshes do not. */
    fun scan() = scan(clearReceipt = true)

    private fun scan(clearReceipt: Boolean) {
        val current = mutable.value
        val uri = current.treeUri ?: return
        if (current.actionsBlocked || scanJob?.isActive == true) return
        mutable.update { it.startingScan(clearReceipt) }
        scanJob = viewModelScope.launch {
            try {
                val listing = catalog.listTree(Uri.parse(uri))
                val media = catalog.listMediaStore()
                val treeUris = listing.rows.map { it.uri }.toSet()
                val rows = listing.rows + media.rows.filter { row -> row.uri !in treeUris }
                val judged = mutableListOf<DownloadCandidate>()
                rows.forEachIndexed { index, row ->
                    judged += catalog.classify(row)
                    mutable.update { it.copy(scanProgress = index + 1) }
                }
                uriByDocument.clear()
                rows.forEach { row -> uriByDocument[row.documentId] = row.uri }
                mutable.update {
                    it.copy(
                        scanning = false,
                        candidates = judged,
                        lastScanCount = judged.size,
                        truncated = listing.truncated || media.truncated,
                        scanFailed = listing.failed,
                        mediaStoreRows = media.rows.size,
                        mediaStoreFailed = media.failed,
                        mediaStoreUnavailable = media.unavailable,
                        selected = emptySet(),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutable.update { it.copy(scanFailed = true) }
            } finally {
                mutable.update { it.copy(scanning = false) }
            }
        }
    }

    fun toggle(documentId: String) = mutable.update { current ->
        if (current.actionsBlocked || current.candidates.none { it.documentId == documentId }) return@update current
        val next = if (current.selected.contains(documentId)) current.selected - documentId
        else current.selected + documentId
        current.copy(selected = next)
    }

    fun selectAttention() = mutable.update { current ->
        if (current.actionsBlocked) return@update current
        val ids = current.candidates.filter { it.health.needsAttention }.map { it.documentId }
        current.copy(selected = ids.toSet())
    }

    fun clearSelection() = mutable.update {
        if (it.actionsBlocked) it else it.copy(selected = emptySet())
    }

    private fun frozenSelection(current: DownloadRescueUiState): List<DownloadTarget> =
        freezeDownloadSelection(current.candidates, current.selected, uriByDocument)

    /** Freeze the exact files and URIs shown by the destructive confirmation. */
    fun requestRemove() {
        val current = mutable.value
        if (current.actionsBlocked) return
        val targets = frozenSelection(current)
        if (targets.isEmpty()) return
        mutable.update { it.copy(removalTargets = targets) }
    }

    fun dismissConfirmation() = mutable.update { it.copy(removalTargets = emptyList()) }

    /** Consume the approval once, never substituting the current selection or a remapped URI. */
    fun confirmRemove() {
        val current = mutable.value
        if (!current.canConfirmRemove) return
        val targets = current.removalTargets
        if (targets.isEmpty()) return
        mutable.update { it.copy(removalTargets = emptyList(), busy = true) }
        viewModelScope.launch {
            try {
                val results = mutableListOf<DownloadActionResult>()
                targets.forEach { target ->
                    val result = catalog.remove(Uri.parse(target.uri), target.candidate.displayName)
                    results += result
                }
                val receipt = DownloadReceipt(
                    action = DownloadAction.REMOVE,
                    attempted = targets.size,
                    succeeded = results.count { it.succeeded },
                    failed = results.count { !it.succeeded },
                    bytesCopied = 0L,
                    bytesRemoved = verifiedRemovalBytes(results),
                    results = results,
                    atMillis = System.currentTimeMillis(),
                )
                mutable.update { it.copy(receipt = receipt, selected = emptySet()) }
            } finally {
                mutable.update { it.copy(busy = false) }
            }
            scan(clearReceipt = false)
        }
    }

    /** Called before launching CreateDocument, not when its result arrives. */
    fun prepareExport(): Boolean {
        val current = mutable.value
        if (!current.canExport) return false
        if (!exports.prepare(frozenSelection(current), current.busy, current.scanning)) return false
        mutable.update { it.copy(exportPending = true) }
        return true
    }

    /** A null picker result (including launch failure) cancels the frozen, one-shot request. */
    fun exportTo(target: Uri?) {
        val current = mutable.value
        val request = exports.complete(
            target?.toString(),
            busy = current.busy || current.loading || current.confirming,
            scanning = current.scanning,
        )
        mutable.update { it.copy(exportPending = false) }
        if (request == null) return
        mutable.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                val result = catalog.export(
                    Uri.parse(request.source.uri), Uri.parse(request.targetUri), request.source.candidate.bytes,
                ).copy(displayName = request.source.candidate.displayName)
                val receipt = DownloadReceipt(
                    action = DownloadAction.EXPORT,
                    attempted = 1,
                    succeeded = if (result.succeeded) 1 else 0,
                    failed = if (result.succeeded) 0 else 1,
                    bytesCopied = if (result.succeeded) result.bytesCopied else 0L,
                    bytesRemoved = 0L,
                    results = listOf(result),
                    atMillis = System.currentTimeMillis(),
                )
                mutable.update { it.copy(receipt = receipt) }
            } finally {
                mutable.update { it.copy(busy = false) }
            }
        }
    }

    /** Renames a single frozen document; no action may overlap a scan or another submission. */
    fun renameSelected(newName: String) {
        val current = mutable.value
        if (current.actionsBlocked) return
        val target = frozenSelection(current).singleOrNull() ?: return
        val name = newName.trim()
        if (name.isEmpty()) return
        mutable.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                val result = catalog.rename(Uri.parse(target.uri), name)
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
                mutable.update { it.copy(receipt = receipt) }
            } finally {
                mutable.update { it.copy(busy = false) }
            }
            scan(clearReceipt = false)
        }
    }
}
