package com.knoux.kxprivatevpn.platform.usage

import com.knoux.kxprivatevpn.platform.vpn.VpnPhase
import com.knoux.kxprivatevpn.platform.vpn.VpnState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class StoredUsage(
    val records: List<UsageRecord>,
    val checkpoint: UsageRecord?,
)

/** Persistence seam so the recorder's monotonic-time rules stay unit-testable. */
interface UsageSink {
    suspend fun load(): StoredUsage
    suspend fun commit(record: UsageRecord)
    suspend fun saveCheckpoint(record: UsageRecord)
    suspend fun clearCheckpoint()
}

/**
 * Converts the live [VpnState] into completed [UsageRecord]s.
 *
 * Rules that keep the numbers honest:
 * - duration always comes from the monotonic clock, so a wall-clock change cannot inflate it;
 * - only sessions whose start this process observed are recorded (a restored tunnel has no
 *   trustworthy start instant and is skipped rather than guessed);
 * - counters are the backend's cumulative RX/TX, captured on each 1 s controller tick;
 * - a checkpoint is written on start and every 15 s; if the process dies, the next launch
 *   recovers it once and marks the record `recovered`;
 * - clearing history while connected rebases the active measurement, so already-cleared time is
 *   not silently re-added by the final record.
 */
class UsageRecorder(
    private val sink: UsageSink,
    private val elapsedMillis: () -> Long,
    private val checkpointIntervalMillis: Long = 15_000L,
    private val onChanged: suspend () -> Unit = {},
) {
    private class Active(
        val key: String,
        val profileId: String,
        val profileName: String,
        val endpoint: String,
        val startedAtMillis: Long,
        var anchorElapsed: Long,
        var baseRx: Long,
        var baseTx: Long,
        var lastRx: Long,
        var lastTx: Long,
        var lastElapsed: Long,
        var lastCheckpointElapsed: Long,
    ) {
        fun record(): UsageRecord = UsageRecord(
            profileId = profileId.ifBlank { endpoint }.ifBlank { "unknown" },
            profileName = profileName.ifBlank { endpoint },
            endpoint = endpoint.ifBlank { profileId },
            startedAtMillis = startedAtMillis,
            durationMillis = (lastElapsed - anchorElapsed).coerceIn(0, MAX_USAGE_DURATION_MS),
            rxBytes = (lastRx - baseRx).coerceAtLeast(0),
            txBytes = (lastTx - baseTx).coerceAtLeast(0),
        )
    }

    private val mutableSaveFailed = MutableStateFlow(false)
    val saveFailed: StateFlow<Boolean> = mutableSaveFailed.asStateFlow()
    private var active: Active? = null

    /** Finalizes an interrupted checkpoint exactly once. Call before observing live state. */
    suspend fun recoverCheckpoint() {
        val checkpoint = try {
            sink.load().checkpoint
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            mutableSaveFailed.value = true
            return
        } ?: return
        if (checkpoint.durationMillis == 0L && checkpoint.totalBytes == 0L) {
            try {
                sink.clearCheckpoint()
                mutableSaveFailed.value = false
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutableSaveFailed.value = true
            }
            return
        }
        try {
            sink.commit(checkpoint.copy(recovered = true))
            mutableSaveFailed.value = false
            onChanged()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            mutableSaveFailed.value = true
        }
    }

    suspend fun onVpnState(state: VpnState) {
        val session = state.session
        val connected = state.phase == VpnPhase.CONNECTED
        if (connected && session != null && session.startedAtMillis != null) {
            val key = "${session.profileId}:${session.startedAtMillis}"
            val current = active
            if (current == null || current.key != key) {
                finishActive()
                val now = elapsedMillis()
                active = Active(
                    key = key,
                    profileId = session.profileId,
                    profileName = session.profileName,
                    endpoint = session.endpoint,
                    startedAtMillis = session.startedAtMillis,
                    anchorElapsed = now,
                    baseRx = 0,
                    baseTx = 0,
                    lastRx = session.rxBytes,
                    lastTx = session.txBytes,
                    lastElapsed = now,
                    lastCheckpointElapsed = now,
                )
                checkpoint()
            } else {
                current.lastRx = maxOf(current.lastRx, session.rxBytes)
                current.lastTx = maxOf(current.lastTx, session.txBytes)
                current.lastElapsed = elapsedMillis()
                if (current.lastElapsed - current.lastCheckpointElapsed >= checkpointIntervalMillis) {
                    checkpoint()
                }
            }
        } else if (!connected) {
            finishActive()
        }
        // CONNECTED with a null startedAtMillis is a restored tunnel: its start time is unknown,
        // so no record is invented for it.
    }

    /** Called after the user clears history; only time after the clear is measured again. */
    fun onHistoryCleared() {
        val current = active ?: return
        val now = elapsedMillis()
        current.anchorElapsed = now
        current.baseRx = current.lastRx
        current.baseTx = current.lastTx
        current.lastCheckpointElapsed = now
    }

    private suspend fun finishActive() {
        val current = active ?: return
        active = null
        val record = current.record()
        if (record.durationMillis == 0L && record.totalBytes == 0L) {
            try {
                sink.clearCheckpoint()
                mutableSaveFailed.value = false
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutableSaveFailed.value = true
            }
            return
        }
        try {
            sink.commit(record)
            mutableSaveFailed.value = false
            onChanged()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // The checkpoint stays in place, so the measured bytes can be recovered next launch.
            mutableSaveFailed.value = true
        }
    }

    private suspend fun checkpoint() {
        val current = active ?: return
        current.lastCheckpointElapsed = current.lastElapsed
        try {
            sink.saveCheckpoint(current.record())
            mutableSaveFailed.value = false
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            mutableSaveFailed.value = true
        }
    }
}
