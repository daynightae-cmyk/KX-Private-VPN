package com.knoux.kxprivatevpn.features

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.knoux.kxprivatevpn.platform.apps.InstalledApps
import com.knoux.kxprivatevpn.platform.network.NetworkDoctor
import com.knoux.kxprivatevpn.platform.network.NetworkReading
import com.knoux.kxprivatevpn.platform.repair.AndroidRepairMeasurer
import com.knoux.kxprivatevpn.platform.repair.RepairMeasurement
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Independent observation timestamps prevent a network event from freshening stale storage data. */
data class GuardianUiState(
    val scanning: Boolean = false,
    val scanned: Boolean = false,
    val network: NetworkReading? = null,
    val storage: RepairMeasurement? = null,
    val visibleApps: Int? = null,
    val networkFailed: Boolean = false,
    val storageFailed: Boolean = false,
    val appsFailed: Boolean = false,
    val measuredAtMillis: Long? = null,
    val networkReadAtMillis: Long? = null,
    val storageReadAtMillis: Long? = null,
    val appsReadAtMillis: Long? = null,
)

/**
 * Local-only, read-only Guardian: no silent file actions, permissions, Cloud calls, or
 * VPN mutations. Every recommendation opens a separately confirmed KX service.
 */
class GuardianViewModel(app: Application) : AndroidViewModel(app) {
    private val networkContext = app.applicationContext
    private val measurer = AndroidRepairMeasurer(app.filesDir, app.cacheDir)
    private val apps = InstalledApps(app.applicationContext)
    private val mutable = MutableStateFlow(GuardianUiState())
    val state: StateFlow<GuardianUiState> = mutable.asStateFlow()
    private var scanJob: Job? = null
    private var networkJob: Job? = null
    private var scanGeneration = 0

    fun scan() {
        if (scanJob?.isActive == true) return
        val generation = ++scanGeneration
        mutable.update { it.copy(scanning = true) }
        scanJob = viewModelScope.launch {
            try {
                val network = try {
                    withContext(Dispatchers.IO) { NetworkDoctor.read(networkContext) }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    null
                }
                val networkAt = if (network == null) null else System.currentTimeMillis()
                val storage = try {
                    measurer.measure()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    null
                }
                val storageAt = if (storage == null) null else System.currentTimeMillis()
                val visibleApps = try {
                    apps.inventory().size
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    null
                }
                val appsAt = if (visibleApps == null) null else System.currentTimeMillis()
                if (generation == scanGeneration) {
                    mutable.update {
                        it.copy(
                            scanning = false,
                            scanned = true,
                            network = network,
                            storage = storage,
                            visibleApps = visibleApps,
                            networkFailed = network == null,
                            storageFailed = storage == null,
                            appsFailed = visibleApps == null,
                            measuredAtMillis = System.currentTimeMillis(),
                            networkReadAtMillis = networkAt,
                            storageReadAtMillis = storageAt,
                            appsReadAtMillis = appsAt,
                        )
                    }
                }
            } finally {
                if (generation == scanGeneration) mutable.update { it.copy(scanning = false) }
            }
        }
    }

    /** Network callbacks refresh only network facts, not storage/app inventories. */
    fun refreshNetwork() {
        if (!mutable.value.scanned || mutable.value.scanning || networkJob?.isActive == true) return
        val generation = scanGeneration
        networkJob = viewModelScope.launch {
            val network = try {
                withContext(Dispatchers.IO) { NetworkDoctor.read(networkContext) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
            if (generation != scanGeneration || mutable.value.scanning) return@launch
            mutable.update {
                it.copy(
                    network = network,
                    networkFailed = network == null,
                    networkReadAtMillis = if (network == null) null else System.currentTimeMillis(),
                    measuredAtMillis = System.currentTimeMillis(),
                )
            }
        }
    }
}
