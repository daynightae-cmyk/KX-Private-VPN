package com.knoux.kxprivatevpn.features

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.knoux.kxprivatevpn.platform.apps.InstalledApp
import com.knoux.kxprivatevpn.platform.apps.canRequestUninstall
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

internal enum class AppScanPhase { READY, SCANNING, RESULTS, CANCELLED, FAILED }
internal enum class AppFilter { ALL, USER, SYSTEM }
internal enum class AppSort { NAME, PACKAGE }
internal enum class AppManagementAction { DETAILS, UNINSTALL, SETTINGS }
internal enum class AppManagementOutcome { OPENED, RETURNED, UNAVAILABLE, BLOCKED }

/** A handoff receipt, NOT evidence that Android removed an app or freed any bytes. */
internal data class AppManagementReceipt(
    val action: AppManagementAction,
    val target: InstalledApp?,
    val outcome: AppManagementOutcome,
)

internal data class AppInventoryState(
    val phase: AppScanPhase = AppScanPhase.READY,
    val apps: List<InstalledApp> = emptyList(),
    val query: String = "",
    val filter: AppFilter = AppFilter.ALL,
    val sort: AppSort = AppSort.NAME,
    val approval: InstalledApp? = null,
    val receipt: AppManagementReceipt? = null,
) {
    val externalPending: Boolean get() = receipt?.outcome == AppManagementOutcome.OPENED
    val canManage: Boolean get() = phase == AppScanPhase.RESULTS && !externalPending
    val shown: List<InstalledApp> get() {
        val term = query.trim()
        return apps.filter { app ->
            (filter == AppFilter.ALL || (filter == AppFilter.SYSTEM) == app.system) &&
                (term.isEmpty() || app.label.contains(term, ignoreCase = true) ||
                    app.packageName.contains(term, ignoreCase = true))
        }.sortedWith(when (sort) {
            AppSort.NAME -> compareBy({ it.label.lowercase(Locale.ROOT) }, { it.packageName })
            AppSort.PACKAGE -> compareBy { it.packageName }
        })
    }
}

/** UI state over the existing inventory, with no device mutations or persisted private inventory. */
internal class AppInventoryModel(
    private val ownPackage: String,
    private val readInventory: suspend () -> List<InstalledApp>,
) : ViewModel() {
    private val mutable = MutableStateFlow(AppInventoryState())
    val state = mutable.asStateFlow()
    private var scanJob: Job? = null
    private var generation = 0

    fun scan() {
        val current = mutable.value
        if (current.phase == AppScanPhase.SCANNING || current.approval != null || current.externalPending) return
        val run = ++generation
        mutable.update { it.copy(phase = AppScanPhase.SCANNING, apps = emptyList()) }
        scanJob = viewModelScope.launch {
            try {
                val apps = readInventory().distinctBy { it.packageName }.toList()
                ensureActive()
                if (run == generation) mutable.update { it.copy(phase = AppScanPhase.RESULTS, apps = apps) }
            } catch (cancelled: CancellationException) {
                // The reader may cancel independently of the Cancel button. Leave a retryable
                // state, but never let an older cancelled run replace a newer scan's state.
                if (run == generation) mutable.update {
                    it.copy(phase = AppScanPhase.CANCELLED, apps = emptyList())
                }
                throw cancelled
            } catch (_: Exception) {
                if (run == generation) mutable.update { it.copy(phase = AppScanPhase.FAILED) }
            }
        }
    }

    fun cancelScan() {
        if (mutable.value.phase != AppScanPhase.SCANNING) return
        generation++
        scanJob?.cancel()
        mutable.update { it.copy(phase = AppScanPhase.CANCELLED, apps = emptyList()) }
    }

    fun query(value: String) { mutable.update { it.copy(query = value.take(200)) } }
    fun filter(value: AppFilter) { mutable.update { it.copy(filter = value) } }
    fun sort(value: AppSort) { mutable.update { it.copy(sort = value) } }

    fun requestUninstall(packageName: String) {
        val current = mutable.value
        if (!current.canManage || current.approval != null) return
        val app = current.apps.find { it.packageName == packageName } ?: return
        if (canRequestUninstall(app, ownPackage)) mutable.update { it.copy(approval = app) }
    }

    fun dismissApproval() { mutable.update { it.copy(approval = null) } }

    fun confirmUninstall(launch: (InstalledApp) -> Boolean) {
        val current = mutable.value
        val approved = current.approval ?: return
        mutable.update { it.copy(approval = null) } // Consume once, before calling Android.
        if (!current.canManage || approved !in current.apps || !canRequestUninstall(approved, ownPackage)) {
            mutable.update { it.copy(receipt = AppManagementReceipt(
                AppManagementAction.UNINSTALL, approved, AppManagementOutcome.BLOCKED)) }
            return
        }
        handoff(AppManagementAction.UNINSTALL, approved) { launch(approved) }
    }

    fun openDetails(packageName: String, launch: (InstalledApp) -> Boolean) {
        val current = mutable.value
        if (!current.canManage || current.approval != null) return
        val app = current.apps.find { it.packageName == packageName } ?: return
        handoff(AppManagementAction.DETAILS, app) { launch(app) }
    }

    fun openSettings(launch: () -> Boolean) {
        val current = mutable.value
        if (current.externalPending || current.phase == AppScanPhase.SCANNING || current.approval != null) return
        handoff(AppManagementAction.SETTINGS, null, launch)
    }

    private fun handoff(action: AppManagementAction, target: InstalledApp?, launch: () -> Boolean) {
        val receipt = AppManagementReceipt(action, target, AppManagementOutcome.OPENED)
        mutable.update { it.copy(receipt = receipt) }
        val opened = try { launch() } catch (_: Exception) { false }
        if (!opened) mutable.update { it.copy(receipt = receipt.copy(outcome = AppManagementOutcome.UNAVAILABLE)) }
    }

    /** Activity result codes and launcher visibility cannot prove an uninstall. Refresh only. */
    fun returnedFromAndroid() {
        val current = mutable.value
        val receipt = current.receipt
        if (receipt == null) {
            // ActivityResultRegistry can restore a result after process recreation while this
            // private in-memory inventory/receipt is gone. Refresh, but invent no target/receipt.
            if (current.phase == AppScanPhase.READY) scan()
            return
        }
        if (receipt.outcome != AppManagementOutcome.OPENED) return
        mutable.update { it.copy(receipt = receipt.copy(outcome = AppManagementOutcome.RETURNED)) }
        scan()
    }
}
