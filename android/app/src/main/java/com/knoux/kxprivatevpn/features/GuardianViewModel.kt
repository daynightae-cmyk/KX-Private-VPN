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

/** Every field is a genuine Android observation, nullable on failure, never an estimate. */
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
)

/**
 * Read-only, on-device Guardian. Never contacts an external server, silently cleans files,
 * grants permissions, sends package lists or controls the VPN.
 * All mutations remain inside existing user-confirmed KX service screens.
 */
class GuardianViewModel(app: Application) : AndroidViewModel(app) {
    private val networkContext = app.applicationContext
    private val measurer = AndroidRepairMeasurer(app.filesDir, app.cacheDir)
    private val apps = InstalledApps(app.applicationContext)
    private val mutable = MutableStateFlow(GuardianUiState())
    val state: StateFlow<GuardianUiState> = mutable.asStateFlow()
    private var scanJob: Job? = null

    fun scan() {
        if (scanJob?.isActive == true) return
        mutable.update { it.copy(scanning = true) }
        scanJob = viewModelScope.launch {
            val network = try {
                withContext(Dispatchers.IO) { NetworkDoctor.read(networkContext) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
            val storage = try {
                measurer.measure()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
            val visibleApps = try {
                apps.inventory().size
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
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
                )
            }
        }
    }

    /** Connectivity callbacks update only the network reading, not an expensive disk scan. */
    fun refreshNetwork() {
        if (!mutable.value.scanned || mutable.value.scanning) return
        viewModelScope.launch {
            val network = try {
                withContext(Dispatchers.IO) { NetworkDoctor.read(networkContext) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
            mutable.update {
                it.copy(
                    network = network,
                    networkFailed = network == null,
                    measuredAtMillis = System.currentTimeMillis(),
                )
            }
        }
    }
}
