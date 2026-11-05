package com.knoux.kxprivatevpn.features

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.knoux.kxprivatevpn.platform.repair.GhostCandidate
import com.knoux.kxprivatevpn.platform.repair.GhostCleanup
import com.knoux.kxprivatevpn.platform.repair.GhostCleanupReceipt
import com.knoux.kxprivatevpn.platform.repair.GhostScan
import com.knoux.kxprivatevpn.platform.repair.GhostScanner
import com.knoux.kxprivatevpn.platform.repair.RepairJournal
import com.knoux.kxprivatevpn.platform.repair.RepairJournalEntry
import com.knoux.kxprivatevpn.platform.repair.RepairOutcome
import com.knoux.kxprivatevpn.platform.repair.RepairServiceId
import com.knoux.kxprivatevpn.platform.storage.AndroidDocumentSource
import com.knoux.kxprivatevpn.platform.storage.StorageXrayStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class GhostUiState(
    val treeName: String? = null,
    val treeUri: String? = null,
    val writable: Boolean = false,
    val scanning: Boolean = false,
    val progress: Int = 0,
    val scan: GhostScan? = null,
    val scanCancelled: Boolean = false,
    val scanFailed: Boolean = false,
    val selected: Set<String> = emptySet(),
    val confirming: Boolean = false,
    val cleaning: Boolean = false,
    val receipt: GhostCleanupReceipt? = null,
    val cleanupFailed: Boolean = false,
    val journalFailed: Boolean = false,
)

class GhostHunterViewModel(app: Application) : AndroidViewModel(app) {
    private val store = StorageXrayStore(app)
    private val journal = RepairJournal(File(app.noBackupFilesDir, "repair-journal.json"))
    private val mutable = MutableStateFlow(GhostUiState())
    private var scanner: GhostScanner? = null
    private var scanJob: Job? = null
    private var cleanupJob: Job? = null
    private var cleanup: GhostCleanup? = null
    val state: StateFlow<GhostUiState> = mutable.asStateFlow()

    init {
        val tree = restoredTree()
        mutable.update { it.copy(treeName = tree?.second, treeUri = tree?.first,
            writable = tree?.third ?: false) }
    }

    private fun restoredTree(): Triple<String, String, Boolean>? {
        val uri = store.treeUri ?: return null
        val name = store.treeName ?: return null
        val source = AndroidDocumentSource(getApplication(), Uri.parse(uri))
        val held = getApplication<Application>().contentResolver.persistedUriPermissions.any {
            it.uri.toString() == uri && it.isReadPermission
        }
        if (!held) {
            store.clear()
            return null
        }
        if (!source.hasWritePermission()) {
            try { source.takePersistablePermission(true) } catch (_: Exception) { }
        }
        return Triple(uri, name, source.hasWritePermission())
    }

    fun attachTree(uri: Uri) {
        val source = AndroidDocumentSource(getApplication(), uri)
        viewModelScope.launch {
            try { source.takePersistablePermission(true) } catch (_: Exception) { }
            val name = try { source.treeName() } catch (_: Exception) { null }
                ?: uri.lastPathSegment ?: "Folder"
            store.treeUri = uri.toString()
            store.treeName = name
            mutable.update {
                it.copy(treeUri = uri.toString(), treeName = name,
                    writable = source.hasWritePermission(), receipt = null,
                    scan = null, selected = emptySet(), cleanupFailed = false)
            }
            scan()
        }
    }

    fun scan() {
        val uri = mutable.value.treeUri ?: return
        if (scanJob?.isActive == true) return
        mutable.update {
            it.copy(scanning = true, progress = 0, scanCancelled = false, scanFailed = false,
                selected = emptySet(), cleanupFailed = false)
        }
        val source = AndroidDocumentSource(getApplication(), Uri.parse(uri))
        val active = GhostScanner(source)
        scanner = active
        scanJob = viewModelScope.launch {
            try {
                val value = withContext(Dispatchers.IO) {
                    active.scan(android.provider.DocumentsContract.getTreeDocumentId(Uri.parse(uri)),
                        System.currentTimeMillis()) { count ->
                        mutable.update { it.copy(progress = count) }
                    }
                }
                mutable.update {
                    it.copy(scanning = false, scan = value, scanCancelled = value.cancelled,
                        scanFailed = value.failed)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutable.update { it.copy(scanning = false, scanFailed = true) }
            } finally {
                scanner = null
            }
        }
    }

    fun cancelScan() { scanner?.requestCancel() }

    fun toggle(candidate: GhostCandidate) {
        mutable.update {
            val selected = if (candidate.documentId in it.selected) it.selected - candidate.documentId
            else it.selected + candidate.documentId
            it.copy(selected = selected)
        }
    }

    fun selectAll() {
        mutable.update { state ->
            state.copy(selected = state.scan?.candidates?.map { it.documentId }?.toSet().orEmpty())
        }
    }

    fun clearSelection() = mutable.update { it.copy(selected = emptySet()) }

    fun requestCleanup() {
        val state = mutable.value
        if (state.selected.isEmpty() || !state.writable || state.scan == null) return
        mutable.update { it.copy(confirming = true) }
    }

    fun dismissCleanup() = mutable.update { it.copy(confirming = false) }

    fun cancelCleanup() { cleanup?.requestCancel() }

    fun confirmCleanup() {
        val state = mutable.value
        val scan = state.scan ?: return
        if (cleanupJob?.isActive == true) return
        val selected = scan.candidates.filter { it.documentId in state.selected }
        if (selected.isEmpty()) return
        val uri = state.treeUri
        if (uri == null || !state.writable) return
        mutable.update {
            it.copy(confirming = false, cleaning = true, cleanupFailed = false,
                journalFailed = false, receipt = null)
        }
        val source = AndroidDocumentSource(getApplication(), Uri.parse(uri))
        val active = GhostCleanup(source)
        cleanup = active
        cleanupJob = viewModelScope.launch {
            try {
                val receipt = withContext(Dispatchers.IO) { active.remove(selected) }
                val recordFailed = try {
                    journal.append(RepairJournalEntry(
                        atMillis = System.currentTimeMillis(),
                        serviceKey = RepairServiceId.GHOST.key,
                        action = "ghost_cleanup",
                        outcome = when {
                            receipt.deleted == 0 -> RepairOutcome.FAILED
                            receipt.complete && receipt.skipped.isEmpty() && receipt.failed.isEmpty() ->
                                RepairOutcome.COMPLETE
                            else -> RepairOutcome.PARTIAL
                        },
                        items = receipt.deleted,
                        bytes = 0,
                    ))
                    false
                } catch (_: Exception) {
                    true
                }
                mutable.update {
                    it.copy(cleaning = false, receipt = receipt, journalFailed = recordFailed,
                        selected = emptySet())
                }
                // Re-scan for real post-action verification; the receipt stays visible.
                scan()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutable.update { it.copy(cleaning = false, cleanupFailed = true) }
            } finally {
                cleanup = null
            }
        }
    }

    companion object {
        const val MAX_DISPLAYED = 100
    }
}
