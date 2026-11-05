package com.knoux.kxprivatevpn.features

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.knoux.kxprivatevpn.platform.repair.AndroidRepairMeasurer
import com.knoux.kxprivatevpn.platform.repair.RepairJournal
import com.knoux.kxprivatevpn.platform.repair.RepairJournalEntry
import com.knoux.kxprivatevpn.platform.repair.RepairMeasurement
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class RepairUiState(
    val loading: Boolean = true,
    val measuring: Boolean = false,
    val failure: Boolean = false,
    val measurement: RepairMeasurement? = null,
    val journal: List<RepairJournalEntry> = emptyList(),
)

class RepairLabViewModel(app: Application) : AndroidViewModel(app) {
    private val journal = RepairJournal(File(app.noBackupFilesDir, "repair-journal.json"))
    private val measurer = AndroidRepairMeasurer(app.filesDir, app.cacheDir)
    private val mutable = MutableStateFlow(RepairUiState())
    private var measurementJob: Job? = null
    val state: StateFlow<RepairUiState> = mutable.asStateFlow()

    init {
        refreshJournal()
        measure()
    }

    /** Re-reads completed operations; the dashboard calls this whenever it becomes visible. */
    fun refreshJournal() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val entries = journal.load()
                mutable.update { it.copy(journal = entries) }
            } catch (_: Exception) {
                // A journal read failure must not erase the last known list or claim success.
            }
        }
    }

    fun measure() {
        if (measurementJob?.isActive == true) return
        mutable.update { it.copy(measuring = true, failure = false) }
        measurementJob = viewModelScope.launch {
            try {
                val measurement = measurer.measure()
                mutable.update {
                    it.copy(loading = false, measuring = false, failure = false, measurement = measurement)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutable.update {
                    it.copy(loading = false, measuring = false, failure = it.measurement == null)
                }
            }
        }
    }
}
