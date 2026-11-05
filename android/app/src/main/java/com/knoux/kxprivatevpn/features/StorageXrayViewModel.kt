package com.knoux.kxprivatevpn.features

import android.app.Application
import android.net.Uri
import android.os.StatFs
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.knoux.kxprivatevpn.platform.storage.AndroidDocumentSource
import com.knoux.kxprivatevpn.platform.storage.MediaInventory
import com.knoux.kxprivatevpn.platform.storage.MediaStoreInventory
import com.knoux.kxprivatevpn.platform.storage.SafStorageScanner
import com.knoux.kxprivatevpn.platform.storage.StorageScan
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
import java.nio.file.Files
import kotlin.coroutines.coroutineContext

data class VolumeReading(val totalBytes: Long?, val availableBytes: Long?, val atMillis: Long)

data class StorageXrayUiState(
    val loading: Boolean = true,
    val measuring: Boolean = false,
    val volume: VolumeReading? = null,
    val kxBytes: Long? = null,
    val kxFiles: Int = 0,
    val kxLimited: Boolean = false,
    val media: MediaInventory? = null,
    val mediaLoading: Boolean = false,
    val mediaDenied: Boolean = false,
    val scan: StorageScan? = null,
    val scanning: Boolean = false,
    val scanProgress: Int = 0,
    val scanCancelled: Boolean = false,
    val scanFailed: Boolean = false,
    val treeName: String? = null,
    val treeUri: String? = null,
    val pathStack: List<String> = emptyList(),
    val measurementFailed: Boolean = false,
)

class StorageXrayViewModel(app: Application) : AndroidViewModel(app) {
    private val store = StorageXrayStore(app)
    private val media = MediaStoreInventory(app)
    private val mutable = MutableStateFlow(StorageXrayUiState())
    private var scanner: SafStorageScanner? = null
    private var scanJob: Job? = null
    private var measureJob: Job? = null
    private var mediaJob: Job? = null
    val state: StateFlow<StorageXrayUiState> = mutable.asStateFlow()

    init {
        val tree = restoredTree()
        mutable.update { it.copy(treeName = tree?.second, treeUri = tree?.first) }
        refresh()
    }

    /** Only a permission the process still holds is restored; a stale URI is discarded. */
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

    fun refresh() {
        if (measureJob?.isActive == true) return
        mutable.update { it.copy(measuring = true, measurementFailed = false) }
        measureJob = viewModelScope.launch {
            try {
                val volume = withContext(Dispatchers.IO) { measureVolume() }
                val kx = withContext(Dispatchers.IO) { measureKx() }
                mutable.update {
                    it.copy(
                        loading = false, measuring = false, volume = volume,
                        kxBytes = kx.bytes, kxFiles = kx.files, kxLimited = kx.limited,
                        measurementFailed = volume.totalBytes == null,
                    )
                }
                refreshMedia()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutable.update { it.copy(loading = false, measuring = false, measurementFailed = true) }
            }
        }
    }

    fun refreshMedia() {
        if (mediaJob?.isActive == true) return
        mutable.update { it.copy(mediaLoading = true) }
        mediaJob = viewModelScope.launch {
            try {
                val value = withContext(Dispatchers.IO) { media.scan() }
                mutable.update { it.copy(media = value, mediaLoading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutable.update { it.copy(mediaLoading = false) }
            }
        }
    }

    fun onMediaPermissions(result: Map<String, Boolean>) {
        mutable.update { it.copy(mediaDenied = result.values.none { granted -> granted }) }
        refreshMedia()
    }

    fun requiredMediaPermissions(): Array<String> = media.requiredPermissions().toTypedArray()

    fun mediaGranted(): Boolean = media.grantedPermissions().isNotEmpty()

    /** The Activity result already granted the tree URI; this stores and scans it. */
    fun attachTree(uri: Uri) {
        val source = AndroidDocumentSource(getApplication(), uri)
        viewModelScope.launch {
            try {
                source.takePersistablePermission()
            } catch (_: Exception) {
                // The grant is still usable for this session; a failed persist is surfaced by
                // the next launch showing no stored tree instead of a fake saved state.
            }
            val name = try { source.treeName() } catch (_: Exception) { null }
                ?: uri.lastPathSegment ?: "Folder"
            store.treeUri = uri.toString()
            store.treeName = name
            mutable.update { it.copy(treeUri = uri.toString(), treeName = name) }
            scanTree()
        }
    }

    fun scanTree() {
        val uri = mutable.value.treeUri ?: return
        if (scanJob?.isActive == true) return
        mutable.update {
            it.copy(scanning = true, scanProgress = 0, scanCancelled = false,
                scanFailed = false, pathStack = emptyList())
        }
        val source = AndroidDocumentSource(getApplication(), Uri.parse(uri))
        val active = SafStorageScanner(source)
        scanner = active
        scanJob = viewModelScope.launch {
            try {
                val value = withContext(Dispatchers.IO) {
                    active.scan(android.provider.DocumentsContract.getTreeDocumentId(Uri.parse(uri)),
                        System.currentTimeMillis()) { count ->
                        mutable.update { it.copy(scanProgress = count) }
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

    fun forgetTree() {
        val uri = mutable.value.treeUri
        if (uri != null) {
            try {
                getApplication<Application>().contentResolver.releasePersistableUriPermission(
                    Uri.parse(uri), android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {
                // The grant may already be gone; local state is cleared regardless.
            }
        }
        store.clear()
        mutable.update {
            it.copy(treeUri = null, treeName = null, scan = null, scanCancelled = false,
                scanFailed = false, pathStack = emptyList(), scanProgress = 0)
        }
    }

    fun enterFolder(path: String) = mutable.update { it.copy(pathStack = it.pathStack + path) }

    fun upFolder() = mutable.update {
        it.copy(pathStack = if (it.pathStack.isEmpty()) it.pathStack else it.pathStack.dropLast(1))
    }

    private data class KxMeasurement(val bytes: Long?, val files: Int, val limited: Boolean)

    private fun measureVolume(): VolumeReading {
        val now = System.currentTimeMillis()
        return try {
            val stat = StatFs(getApplication<Application>().filesDir.absolutePath)
            VolumeReading(stat.totalBytes, stat.availableBytes, now)
        } catch (_: Exception) {
            VolumeReading(null, null, now)
        }
    }

    private suspend fun measureKx(): KxMeasurement {
        val app = getApplication<Application>()
        var bytes = 0L
        var files = 0
        var limited = false
        var any = false
        listOf(app.filesDir, app.cacheDir, app.noBackupFilesDir).forEach { directory ->
            try {
                val root = directory.canonicalFile
                for (file in root.walkTopDown().onEnter {
                    !Files.isSymbolicLink(it.toPath()) && it.canonicalFile.toPath().startsWith(root.toPath())
                }) {
                    coroutineContext.ensureActive()
                    any = true
                    if (!file.isFile) continue
                    if (files >= MAX_KX_FILES) { limited = true; break }
                    files++
                    bytes += file.length()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Keep the other directories' real measurements.
            }
        }
        return KxMeasurement(if (any) bytes else null, files, limited)
    }

    companion object {
        const val MAX_KX_FILES = 20_000
    }
}
