package com.knoux.kxprivatevpn.features

import android.app.Application
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.knoux.kxprivatevpn.platform.repair.HeavyweightFilter
import com.knoux.kxprivatevpn.platform.repair.HeavyweightScan
import com.knoux.kxprivatevpn.platform.repair.HeavyweightScanner
import com.knoux.kxprivatevpn.platform.repair.LargeFile
import com.knoux.kxprivatevpn.platform.repair.LargeFileCategory
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

data class HeavyweightUiState(
    val treeName: String? = null,
    val treeUri: String? = null,
    val scanning: Boolean = false,
    val progress: Int = 0,
    val scan: HeavyweightScan? = null,
    val scanCancelled: Boolean = false,
    val scanFailed: Boolean = false,
    val minimumBytes: Long = 0L,
    val category: LargeFileCategory? = null,
    val selectedDocumentId: String? = null,
    val openFailed: Boolean = false,
) {
    /** Filtered ranking. Both values are local view state; no file is read again. */
    val visibleFiles: List<LargeFile> get() =
        scan?.files?.let { HeavyweightFilter(minimumBytes, category).apply(it) }.orEmpty()

    val selectedFile: LargeFile? get() =
        scan?.files?.firstOrNull { it.documentId == selectedDocumentId }
}

/**
 * Large-file ranking over the same single user-granted SAF tree used by Storage X-Ray, Ghost
 * Hunter and Duplicate DNA. The view model only ranks and previews; it never deletes or renames.
 */
class HeavyweightViewModel(app: Application) : AndroidViewModel(app) {
    private val store = StorageXrayStore(app)
    private val mutable = MutableStateFlow(HeavyweightUiState())
    private var scanner: HeavyweightScanner? = null
    private var scanJob: Job? = null
    val state: StateFlow<HeavyweightUiState> = mutable.asStateFlow()

    init {
        val tree = restoredTree()
        mutable.update {
            it.copy(treeName = tree?.second, treeUri = tree?.first)
        }
    }

    private fun restoredTree(): Pair<String, String>? {
        val uri = store.treeUri ?: return null
        val name = store.treeName ?: return null
        val held = getApplication<Application>().contentResolver.persistedUriPermissions.any {
            it.uri.toString() == uri && it.isReadPermission
        }
        if (!held) {
            store.clear()
            return null
        }
        return uri to name
    }

    fun attachTree(uri: Uri) {
        viewModelScope.launch {
            try {
                AndroidDocumentSource(getApplication(), uri)
                    .takePersistablePermission(readWrite = false)
            } catch (_: Exception) {
                // The provider may not persist grants; reading still works for this session.
            }
            val name = try {
                AndroidDocumentSource(getApplication(), uri).treeName()
            } catch (_: Exception) {
                null
            } ?: uri.lastPathSegment ?: "Folder"
            store.treeUri = uri.toString()
            store.treeName = name
            mutable.update {
                it.copy(treeUri = uri.toString(), treeName = name, scan = null,
                    selectedDocumentId = null, openFailed = false, scanCancelled = false,
                    scanFailed = false)
            }
            scan()
        }
    }

    fun scan() {
        val uri = mutable.value.treeUri ?: return
        if (scanJob?.isActive == true) return
        mutable.update {
            it.copy(scanning = true, progress = 0, scanCancelled = false, scanFailed = false,
                openFailed = false)
        }
        val source = AndroidDocumentSource(getApplication(), uri.toUri())
        val active = HeavyweightScanner(source)
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

    fun setMinimumBytes(value: Long) {
        mutable.update { it.copy(minimumBytes = value.coerceAtLeast(0L), openFailed = false) }
    }

    fun setCategory(value: LargeFileCategory?) {
        mutable.update { it.copy(category = value, openFailed = false) }
    }

    fun select(file: LargeFile) {
        mutable.update {
            it.copy(
                selectedDocumentId = if (it.selectedDocumentId == file.documentId) null
                else file.documentId,
                openFailed = false,
            )
        }
    }

    /** Open always focuses the row first so an unsupported viewer is reported next to it. */
    fun focus(file: LargeFile) {
        mutable.update { it.copy(selectedDocumentId = file.documentId, openFailed = false) }
    }

    /** Real content URI for the viewer intent; null when no tree or an invalid document id. */
    fun documentUri(documentId: String): Uri? {
        val tree = mutable.value.treeUri ?: return null
        return try {
            DocumentsContract.buildDocumentUriUsingTree(tree.toUri(), documentId)
        } catch (_: Exception) {
            null
        }
    }

    fun reportOpenResult(opened: Boolean) {
        mutable.update { it.copy(openFailed = !opened) }
    }

    companion object {
        const val MAX_DISPLAYED = 100
    }
}
