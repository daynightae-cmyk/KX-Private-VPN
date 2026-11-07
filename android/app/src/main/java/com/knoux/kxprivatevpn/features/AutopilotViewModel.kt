package com.knoux.kxprivatevpn.features

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.knoux.kxprivatevpn.data.stats.UsageStatsStore
import com.knoux.kxprivatevpn.platform.repair.AutopilotPlan
import com.knoux.kxprivatevpn.platform.repair.AutopilotPlanner
import com.knoux.kxprivatevpn.platform.repair.AutopilotReceipt
import com.knoux.kxprivatevpn.platform.repair.RepairJournal
import com.knoux.kxprivatevpn.platform.repair.RepairJournalEntry
import com.knoux.kxprivatevpn.platform.repair.RepairOutcome
import com.knoux.kxprivatevpn.platform.security.ShieldRetention
import com.knoux.kxprivatevpn.platform.storage.CacheMaintenance
import com.knoux.kxprivatevpn.platform.storage.CachePreview
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
import java.io.File

data class AutopilotUiState(
    val loading: Boolean = true,
    val measuring: Boolean = false,
    val plan: AutopilotPlan? = null,
    val selected: Set<String> = emptySet(),
    val confirming: Boolean = false,
    val busy: Boolean = false,
    val receipt: AutopilotReceipt? = null,
    val failure: Boolean = false,
    val message: String? = null,
)

/**
 * KX Repair Autopilot.
 *
 * Measures the app's own cache and aged history, proposes a deterministic plan, and runs only the
 * approved targets. Cache deletion goes through [CacheMaintenance.removeApproved], which re-checks
 * size/mtime/SHA-256 and skips anything that changed; history pruning rewrites the journal and usage
 * store through the same retention rule the Session Shield uses. The receipt is built from the real
 * operation results and read back, partial failure and cancellation are reported honestly, and the
 * run is journaled. Nothing outside KX's own storage is read or written.
 */
class AutopilotViewModel(app: Application) : AndroidViewModel(app) {
    private val maintenance = CacheMaintenance(app.cacheDir)
    private val journal = RepairJournal(File(app.noBackupFilesDir, "repair-journal.json"))
    private val usage = UsageStatsStore(app)
    private val mutable = MutableStateFlow(AutopilotUiState())
    val state: StateFlow<AutopilotUiState> = mutable.asStateFlow()

    // The preview captured at measurement time. removeApproved re-verifies every candidate against
    // it, so a stale preview can only cause a skip, never a wrong deletion.
    private var lastCache: CachePreview? = null
    private var runJob: Job? = null

    init {
        measure()
    }

    /** Reads the real cache and history and builds the proposal. No value is invented on failure. */
    fun measure() {
        mutable.update { it.copy(measuring = true, failure = false, message = null) }
        viewModelScope.launch {
            try {
                val cache = withContext(Dispatchers.IO) {
                    try {
                        maintenance.preview()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        null
                    }
                }
                val entries = withContext(Dispatchers.IO) { journal.load() }
                val records = withContext(Dispatchers.IO) { usage.load().records }
                val plan = AutopilotPlanner.build(cache, entries, records, System.currentTimeMillis())
                lastCache = cache
                mutable.update {
                    it.copy(
                        loading = false,
                        measuring = false,
                        plan = plan,
                        selected = plan.targets.map { target -> target.id }.toSet(),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutable.update { it.copy(loading = false, measuring = false, failure = true) }
            }
        }
    }

    fun toggle(id: String) = mutable.update { current ->
        val next = if (id in current.selected) current.selected - id else current.selected + id
        current.copy(selected = next, message = null)
    }

    fun selectAll() = mutable.update { current ->
        current.copy(selected = current.plan?.targets?.map { it.id }?.toSet() ?: emptySet(), message = null)
    }

    fun clearSelection() = mutable.update { it.copy(selected = emptySet(), confirming = false, message = null) }

    /** Running deletes data, so it always opens an explicit approval first. */
    fun requestRun() = mutable.update { current ->
        if (current.selected.isEmpty()) current.copy(message = "no_selection")
        else current.copy(confirming = true, message = null)
    }

    fun dismissConfirmation() = mutable.update { it.copy(confirming = false) }

    fun cancel() {
        runJob?.cancel()
    }

    /**
     * Executes exactly the approved targets. Cache files are deleted through the verified maintenance
     * path; aged history is pruned through the retention rule. A checkpoint tracks partial progress so
     * a cancellation still reports what really happened. The receipt and journal entry are written in
     * a NonCancellable block, then the plan is re-measured.
     */
    fun confirmRun() {
        val snapshot = mutable.value
        val plan = snapshot.plan ?: return
        val selected = snapshot.selected
        if (selected.isEmpty()) return
        val preview = lastCache
        mutable.update { it.copy(confirming = false, busy = true, receipt = null, message = null) }
        runJob = viewModelScope.launch {
            var deleted = 0
            var bytes = 0L
            var pruned = 0
            val skipped = mutableListOf<String>()
            val failed = mutableListOf<String>()
            var cancelled = false
            var cacheFailed = false
            var historyFailed = false
            try {
                val cacheTargets = plan.cacheTargets.filter { it.id in selected }
                if (cacheTargets.isNotEmpty() && preview != null) {
                    try {
                        val paths = cacheTargets.map { it.title }.toSet()
                        val receipt = withContext(Dispatchers.IO) {
                            maintenance.removeApproved(preview, paths, approved = true) { partial, _ ->
                                deleted = partial.deleted
                                bytes = partial.removedFileBytes
                            }
                        }
                        deleted = receipt.deleted
                        bytes = receipt.removedFileBytes
                        skipped += receipt.skipped
                        failed += receipt.failed
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        cacheFailed = true
                    }
                }
                if (AutopilotPlanner.HISTORY_ID in selected) {
                    try {
                        withContext(Dispatchers.IO) {
                            val now = System.currentTimeMillis()
                            val days = plan.retentionDays
                            val entries = journal.load()
                            val kept = entries.filterNot { ShieldRetention.isExpired(it.atMillis, now, days) }
                            pruned += entries.size - kept.size
                            journal.replace(kept)
                            val stored = usage.load()
                            val survivors = stored.records.filterNot {
                                ShieldRetention.isExpired(it.startedAtMillis, now, days)
                            }
                            pruned += stored.records.size - survivors.size
                            if (stored.records.size != survivors.size) usage.saveSurvivors(survivors)
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        historyFailed = true
                    }
                }
            } catch (e: CancellationException) {
                cancelled = true
                throw e
            } finally {
                withContext(NonCancellable) {
                    val complete = !cancelled && !cacheFailed && !historyFailed &&
                        failed.isEmpty() && skipped.isEmpty()
                    val detail = when {
                        cancelled -> "cancelled"
                        complete -> "complete"
                        else -> "partial"
                    }
                    val receipt = AutopilotReceipt(
                        deleted = deleted,
                        pruned = pruned,
                        bytesFreed = bytes,
                        skipped = skipped.toList(),
                        failed = failed.toList(),
                        complete = complete,
                        cancelled = cancelled,
                        atMillis = System.currentTimeMillis(),
                        detail = detail,
                    )
                    val outcome = when {
                        complete -> RepairOutcome.COMPLETE
                        cancelled || deleted + pruned > 0 || skipped.isNotEmpty() -> RepairOutcome.PARTIAL
                        else -> RepairOutcome.FAILED
                    }
                    runCatching {
                        journal.append(
                            RepairJournalEntry(
                                atMillis = System.currentTimeMillis(),
                                serviceKey = "autopilot",
                                action = "autopilot_run",
                                outcome = outcome,
                                items = deleted + pruned,
                                bytes = bytes,
                            ),
                        )
                    }
                    mutable.update { it.copy(busy = false, receipt = receipt, selected = emptySet()) }
                    measure()
                }
            }
        }
    }
}