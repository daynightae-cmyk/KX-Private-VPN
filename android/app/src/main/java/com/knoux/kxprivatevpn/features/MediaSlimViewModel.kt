package com.knoux.kxprivatevpn.features

import android.app.Application
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.knoux.kxprivatevpn.platform.media.AndroidMediaGateway
import com.knoux.kxprivatevpn.platform.media.CompressionOutcome
import com.knoux.kxprivatevpn.platform.media.ImageCompressor
import com.knoux.kxprivatevpn.platform.media.MediaCandidate
import com.knoux.kxprivatevpn.platform.media.MediaQuality
import com.knoux.kxprivatevpn.platform.media.MediaSelection
import com.knoux.kxprivatevpn.platform.media.MediaSlimScan
import com.knoux.kxprivatevpn.platform.media.MediaSlimScanner
import com.knoux.kxprivatevpn.platform.media.MediaSlimStore
import com.knoux.kxprivatevpn.platform.storage.AndroidDocumentSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MediaSlimUiState(
    val treeName: String? = null,
    val treeUri: String? = null,
    val scanning: Boolean = false,
    val progress: Int = 0,
    val scan: MediaSlimScan? = null,
    val scanCancelled: Boolean = false,
    val scanFailed: Boolean = false,
    val selected: Set<String> = emptySet(),
    val quality: MediaQuality = MediaQuality.BALANCED,
    val confirming: Boolean = false,
    val compressing: Boolean = false,
    val currentIndex: Int = 0,
    val total: Int = 0,
    val results: List<CompressionOutcome> = emptyList(),
    val completed: Boolean = false,
    val cancelled: Boolean = false,
    val selectionError: Boolean = false,
    val permissionError: Boolean = false,
) {
    val selection: MediaSelection get() = MediaSelection(selected, quality)
    val selectedCandidates: List<MediaCandidate>
        get() = scan?.files?.filter { it.documentId in selected }.orEmpty()
    val selectedSupported: List<MediaCandidate>
        get() = selectedCandidates.filter { it.compressible }
    val selectedBytes: Long get() = selectedSupported.sumOf { it.bytes }
    val verified: List<CompressionOutcome.Verified>
        get() = results.filterIsInstance<CompressionOutcome.Verified>()
    val failed: List<CompressionOutcome.Failed>
        get() = results.filterIsInstance<CompressionOutcome.Failed>()
    val savedBytes: Long get() = verified.sumOf { it.value.savedBytes }
    val visibleFiles: List<MediaCandidate>
        get() = scan?.files?.take(MAX_DISPLAYED).orEmpty()

    companion object { const val MAX_DISPLAYED = 200 }
}

class MediaSlimViewModel(app: Application) : AndroidViewModel(app) {
    private val store = MediaSlimStore(app)
    private val mutable = MutableStateFlow(MediaSlimUiState())
    private var scanner: MediaSlimScanner? = null
    private var scanJob: Job? = null
    private var compressJob: Job? = null
    val state: StateFlow<MediaSlimUiState> = mutable.asStateFlow()

    init {
        val tree = restoredTree()
        if (tree != null) mutable.update { it.copy(treeName = tree.second, treeUri = tree.first) }
    }

    private fun restoredTree(): Pair<String, String>? {
        val uri = store.treeUri ?: return null
        val name = store.treeName ?: return null
        val held = getApplication<Application>().contentResolver.persistedUriPermissions.any {
            it.uri.toString() == uri && it.isReadPermission && it.isWritePermission
        }
        if (!held) { store.clear(); return null }
        return uri to name
    }

    fun attachTree(uri: Uri) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val source = AndroidDocumentSource(app, uri)
            try {
                source.takePersistablePermission(readWrite = true)
            } catch (_: Exception) {
                // Providers may keep the grant for this process only; the write check below
                // decides whether compression can honestly be offered.
            }
            val writable = app.contentResolver.persistedUriPermissions.any {
                it.uri == uri && it.isWritePermission
            }
            if (!writable) {
                store.clear()
                mutable.update {
                    it.copy(treeUri = null, treeName = null, scan = null,
                        selected = emptySet(), permissionError = true, scanning = false)
                }
                return@launch
            }
            val name = try {
                source.treeName()
            } catch (_: Exception) { null } ?: uri.lastPathSegment ?: "Folder"
            store.treeUri = uri.toString()
            store.treeName = name
            mutable.update {
                it.copy(treeUri = uri.toString(), treeName = name, scan = null,
                    selected = emptySet(), scanCancelled = false, scanFailed = false,
                    results = emptyList(), completed = false, cancelled = false,
                    selectionError = false, confirming = false, permissionError = false)
            }
            scan()
        }
    }

    fun scan() {
        val uri = mutable.value.treeUri ?: return
        if (scanJob?.isActive == true) return
        mutable.update {
            it.copy(scanning = true, progress = 0, scanCancelled = false, scanFailed = false,
                selected = emptySet(), confirming = false, results = emptyList(),
                completed = false, cancelled = false, selectionError = false,
                permissionError = false)
        }
        val source = AndroidDocumentSource(getApplication(), uri.toUri())
        val active = MediaSlimScanner(source)
        scanner = active
        scanJob = viewModelScope.launch {
            try {
                val value = withContext(Dispatchers.IO) {
                    active.scan(DocumentsContract.getTreeDocumentId(uri.toUri()),
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

    fun toggle(file: MediaCandidate) {
        if (mutable.value.compressing) return
        mutable.update {
            val next = if (file.documentId in it.selected) it.selected - file.documentId
            else it.selected + file.documentId
            it.copy(selected = next, selectionError = false, completed = false, results = emptyList())
        }
    }

    fun selectAllSupported() {
        if (mutable.value.compressing) return
        mutable.update { state ->
            state.copy(
                selected = state.scan?.files?.filter { it.compressible }?.map { it.documentId }?.toSet()
                    .orEmpty(),
                selectionError = false, completed = false, results = emptyList(),
            )
        }
    }

    fun clearSelection() {
        if (mutable.value.compressing) return
        mutable.update { it.copy(selected = emptySet(), selectionError = false) }
    }

    fun chooseQuality(value: MediaQuality) {
        if (mutable.value.compressing) return
        mutable.update { it.copy(quality = value) }
    }

    fun requestConfirmation() {
        val state = mutable.value
        if (state.compressing) return
        if (state.selectedSupported.isEmpty()) {
            mutable.update { it.copy(selectionError = true) }
            return
        }
        mutable.update { it.copy(confirming = true) }
    }

    fun dismissConfirmation() { mutable.update { it.copy(confirming = false) } }

    fun compressConfirmed() {
        val state = mutable.value
        if (state.compressing) return
        val pending = state.selectedSupported
        if (pending.isEmpty()) {
            mutable.update { it.copy(confirming = false, selectionError = true) }
            return
        }
        val uri = state.treeUri ?: run {
            mutable.update { it.copy(confirming = false, scanFailed = true) }
            return
        }
        val compressor = ImageCompressor(AndroidMediaGateway(getApplication(), uri.toUri()))
        mutable.update {
            it.copy(confirming = false, compressing = true, currentIndex = 0,
                total = pending.size, results = emptyList(), completed = false,
                cancelled = false, selectionError = false)
        }
        compressJob = viewModelScope.launch {
            val outcomes = mutableListOf<CompressionOutcome>()
            var finished = false
            try {
                pending.forEachIndexed { index, candidate ->
                    mutable.update { it.copy(currentIndex = index + 1) }
                    val result = compressor.compress(candidate, state.quality)
                    outcomes += result
                    mutable.update { it.copy(results = outcomes.toList()) }
                }
                finished = true
            } catch (e: CancellationException) {
                throw e
            } finally {
                withContext(NonCancellable) {
                    mutable.update {
                        it.copy(compressing = false,
                            cancelled = it.cancelled || !finished,
                            completed = outcomes.isNotEmpty())
                    }
                }
            }
        }
    }

    fun cancelCompression() {
        mutable.update { it.copy(cancelled = true) }
        compressJob?.cancel()
    }

    fun resetReceipt() {
        mutable.update {
            it.copy(results = emptyList(), completed = false, cancelled = false,
                currentIndex = 0, total = 0)
        }
    }

    override fun onCleared() {
        super.onCleared()
        scanner?.requestCancel()
        compressJob?.cancel()
    }
}
