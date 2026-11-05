package com.knoux.kxprivatevpn.features

import android.app.Application
import android.net.Uri
import android.provider.DocumentsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.knoux.kxprivatevpn.platform.repair.DuplicateCleanup
import com.knoux.kxprivatevpn.platform.repair.DuplicateCleanupReceipt
import com.knoux.kxprivatevpn.platform.repair.DuplicateContentSource
import com.knoux.kxprivatevpn.platform.repair.DuplicateEntry
import com.knoux.kxprivatevpn.platform.repair.DuplicateGroup
import com.knoux.kxprivatevpn.platform.repair.DuplicateScan
import com.knoux.kxprivatevpn.platform.repair.DuplicateScanner
import com.knoux.kxprivatevpn.platform.repair.RepairJournal
import com.knoux.kxprivatevpn.platform.repair.RepairJournalEntry
import com.knoux.kxprivatevpn.platform.repair.RepairOutcome
import com.knoux.kxprivatevpn.platform.repair.RepairServiceId
import com.knoux.kxprivatevpn.platform.storage.AndroidDocumentSource
import com.knoux.kxprivatevpn.platform.storage.StorageXrayStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * UI state for the Duplicate DNA screen.
 *
 * [selectedDocumentIds] holds the set of [DuplicateEntry.documentId] values the user has
 * chosen for deletion. A user must select at least one, then confirm, before any file is touched.
 * Only entries with [DuplicateEntry.keep]=false are selectable; the keep copies are never in
 * this set.
 */
data class DuplicateDnaUiState(
    val treeName: String? = null,
    val treeUri: String? = null,
    val writable: Boolean = false,
    val scanning: Boolean = false,
    val scanned: Int = 0,
    val hashed: Int = 0,
    val scan: DuplicateScan? = null,
    val scanCancelled: Boolean = false,
    val scanFailed: Boolean = false,
    val selectedDocumentIds: Set<String> = emptySet(),
    val confirming: Boolean = false,
    val cleaning: Boolean = false,
    val receipt: DuplicateCleanupReceipt? = null,
    val cleanupFailed: Boolean = false,
    val journalFailed: Boolean = false,
)

class DuplicateDnaViewModel(app: Application) : AndroidViewModel(app) {
    // Re-uses the same tree URI store as Ghost Hunter and Storage X-Ray (SAF permission is shared).
    private val store = StorageXrayStore(app)
    private val journal = RepairJournal(File(app.noBackupFilesDir, "repair-journal.json"))

    private val mutable = MutableStateFlow(DuplicateDnaUiState())
    val state: StateFlow<DuplicateDnaUiState> = mutable.asStateFlow()

    private var scanJob: Job? = null
    private var cleanupJob: Job? = null
    private var activeScanner: DuplicateScanner? = null
    private var activeCleanup: DuplicateCleanup? = null

    init {
        val tree = restoredTree()
        mutable.update {
            it.copy(treeName = tree?.second, treeUri = tree?.first, writable = tree?.third ?: false)
        }
    }

    private fun restoredTree(): Triple<String, String, Boolean>? {
        val uri = store.treeUri ?: return null
        val name = store.treeName ?: return null
        val held = getApplication<Application>().contentResolver.persistedUriPermissions.any {
            it.uri.toString() == uri && it.isReadPermission
        }
        if (!held) { store.clear(); return null }
        val source = AndroidDocumentSource(getApplication(), Uri.parse(uri))
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
                it.copy(
                    treeUri = uri.toString(), treeName = name,
                    writable = source.hasWritePermission(),
                    scan = null, receipt = null,
                    selectedDocumentIds = emptySet(), cleanupFailed = false,
                )
            }
            scan()
        }
    }

    fun scan() {
        val uriString = mutable.value.treeUri ?: return
        if (scanJob?.isActive == true) return
        mutable.update {
            it.copy(
                scanning = true, scanned = 0, hashed = 0,
                scanCancelled = false, scanFailed = false,
                selectedDocumentIds = emptySet(), cleanupFailed = false,
            )
        }
        val treeUri = Uri.parse(uriString)
        val docSource = AndroidDocumentSource(getApplication(), treeUri)
        val contentSource = contentSource(treeUri, docSource)
        val scanner = DuplicateScanner(docSource, contentSource)
        activeScanner = scanner
        scanJob = viewModelScope.launch {
            try {
                val rootId = DocumentsContract.getTreeDocumentId(treeUri)
                val result = withContext(Dispatchers.IO) {
                    scanner.scan(rootId, System.currentTimeMillis()) { scanned, hashed ->
                        mutable.update { it.copy(scanned = scanned, hashed = hashed) }
                    }
                }
                mutable.update {
                    it.copy(
                        scanning = false, scan = result,
                        scanCancelled = result.cancelled,
                        scanFailed = result.failed,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutable.update { it.copy(scanning = false, scanFailed = true) }
            } finally {
                activeScanner = null
            }
        }
    }

    fun cancelScan() { activeScanner?.requestCancel() }

    // --- Selection ---

    /** Toggle a single duplicate (non-keep) entry. */
    fun toggleEntry(entry: DuplicateEntry) {
        if (entry.keep) return
        mutable.update {
            val ids = if (entry.documentId in it.selectedDocumentIds)
                it.selectedDocumentIds - entry.documentId
            else
                it.selectedDocumentIds + entry.documentId
            it.copy(selectedDocumentIds = ids)
        }
    }

    /** Select all non-keep entries across all groups. */
    fun selectAllDuplicates() {
        val ids = mutable.value.scan?.groups
            ?.flatMap { g -> g.entries.filter { !it.keep }.map { it.documentId } }
            ?.toSet()
            .orEmpty()
        mutable.update { it.copy(selectedDocumentIds = ids) }
    }

    /** Select all non-keep entries within a single group. */
    fun selectGroup(group: DuplicateGroup) {
        val newIds = group.entries.filter { !it.keep }.map { it.documentId }.toSet()
        mutable.update { it.copy(selectedDocumentIds = it.selectedDocumentIds + newIds) }
    }

    private fun contentSource(
        treeUri: Uri,
        docSource: AndroidDocumentSource,
    ): DuplicateContentSource = object : DuplicateContentSource {
        override suspend fun hash(documentId: String): ByteArray =
            withContext(Dispatchers.IO) {
                val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
                val digest = MessageDigest.getInstance("SHA-256")
                getApplication<Application>().contentResolver.openInputStream(fileUri)
                    ?.use { stream ->
                        val buf = ByteArray(HASH_BUFFER_SIZE)
                        var read = stream.read(buf)
                        while (read >= 0) {
                            ensureActive()
                            digest.update(buf, 0, read)
                            read = stream.read(buf)
                        }
                    } ?: error("Could not open $documentId")
                digest.digest()
            }

        override suspend fun currentBytes(documentId: String): Long? =
            withContext(Dispatchers.IO) {
                docSource.document(documentId)?.bytes
            }
    }

    fun clearSelection() = mutable.update { it.copy(selectedDocumentIds = emptySet()) }

    // --- Cleanup ---

    fun requestCleanup() {
        if (mutable.value.selectedDocumentIds.isEmpty() || !mutable.value.writable) return
        mutable.update { it.copy(confirming = true) }
    }

    fun dismissCleanup() = mutable.update { it.copy(confirming = false) }

    fun cancelCleanup() { activeCleanup?.requestCancel() }

    fun confirmCleanup() {
        val state = mutable.value
        val scan = state.scan ?: return
        if (cleanupJob?.isActive == true) return
        val uriString = state.treeUri
        if (uriString == null || !state.writable) return

        val selectedIds = state.selectedDocumentIds.toSet()
        val hasSelectedDuplicate = scan.groups.any { group ->
            group.entries.any { !it.keep && it.documentId in selectedIds }
        }
        if (!hasSelectedDuplicate) return
        mutable.update {
            it.copy(confirming = false, cleaning = true, cleanupFailed = false,
                journalFailed = false, receipt = null)
        }
        val treeUri = Uri.parse(uriString)
        val source = AndroidDocumentSource(getApplication(), treeUri)
        val cleanup = DuplicateCleanup(source, contentSource(treeUri, source))
        activeCleanup = cleanup
        cleanupJob = viewModelScope.launch {
            try {
                val receipt = withContext(Dispatchers.IO) {
                    cleanup.remove(scan.groups, state.selectedDocumentIds.toSet())
                }
                val recordFailed = try {
                    journal.append(
                        RepairJournalEntry(
                            atMillis = System.currentTimeMillis(),
                            serviceKey = RepairServiceId.DUPLICATE.key,
                            action = ACTION_DUPLICATE_CLEANUP,
                            outcome = when {
                                receipt.deleted == 0 -> RepairOutcome.FAILED
                                receipt.complete && receipt.skipped.isEmpty() &&
                                    receipt.failed.isEmpty() -> RepairOutcome.COMPLETE
                                else -> RepairOutcome.PARTIAL
                            },
                            items = receipt.deleted,
                            bytes = receipt.bytesReclaimed,
                        )
                    )
                    false
                } catch (_: Exception) {
                    true
                }
                mutable.update {
                    it.copy(
                        cleaning = false, receipt = receipt,
                        journalFailed = recordFailed,
                        selectedDocumentIds = emptySet(),
                    )
                }
                // Re-scan to show post-cleanup state.
                scan()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutable.update { it.copy(cleaning = false, cleanupFailed = true) }
            } finally {
                activeCleanup = null
            }
        }
    }

    companion object {
        const val MAX_DISPLAYED_GROUPS = 50
        private const val HASH_BUFFER_SIZE = 64 * 1024
        const val ACTION_DUPLICATE_CLEANUP = "duplicate_cleanup"
    }
}


