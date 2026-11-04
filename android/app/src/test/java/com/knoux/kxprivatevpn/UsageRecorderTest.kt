package com.knoux.kxprivatevpn

import com.knoux.kxprivatevpn.platform.usage.StoredUsage
import com.knoux.kxprivatevpn.platform.usage.UsageRecord
import com.knoux.kxprivatevpn.platform.usage.UsageRecorder
import com.knoux.kxprivatevpn.platform.usage.UsageSink
import com.knoux.kxprivatevpn.platform.vpn.VpnPhase
import com.knoux.kxprivatevpn.platform.vpn.VpnSession
import com.knoux.kxprivatevpn.platform.vpn.VpnState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageRecorderTest {
    private class FakeSink : UsageSink {
        val committed = mutableListOf<UsageRecord>()
        var checkpoint: UsageRecord? = null
        var checkpointSaves = 0
        var failCommit = false
        var failCheckpoint = false
        var loadFailure: Exception? = null

        override suspend fun load(): StoredUsage {
            loadFailure?.let { throw it }
            return StoredUsage(committed.toList(), checkpoint)
        }

        override suspend fun commit(record: UsageRecord) {
            if (failCommit) error("disk full")
            committed += record
            checkpoint = null
        }

        override suspend fun saveCheckpoint(record: UsageRecord) {
            if (failCheckpoint) error("disk full")
            checkpointSaves++
            checkpoint = record
        }

        override suspend fun clearCheckpoint() {
            checkpoint = null
        }
    }

    private var elapsed = 0L
    private val sink = FakeSink()
    private val recorder = UsageRecorder(sink, elapsedMillis = { elapsed })

    private fun connected(rx: Long, tx: Long = 0, started: Long? = 1_760_000_000_000) = VpnState(
        phase = VpnPhase.CONNECTED,
        session = VpnSession(
            profileId = "profile-1",
            profileName = "Home",
            endpoint = "vpn.example.com:51820",
            startedAtMillis = started,
            rxBytes = rx,
            txBytes = tx,
        ),
    )

    private val stopped = VpnState(VpnPhase.IDLE)

    @Test fun sessionIsCommittedOnceWithMonotonicDuration() = runTest {
        recorder.onVpnState(connected(rx = 0))
        assertNotNull(sink.checkpoint) // checkpoint written at start

        elapsed = 65_000
        recorder.onVpnState(connected(rx = 5_000, tx = 1_000))
        elapsed = 66_000
        recorder.onVpnState(stopped)

        assertEquals(1, sink.committed.size)
        val record = sink.committed.single()
        assertEquals(65_000, record.durationMillis)
        assertEquals(5_000, record.rxBytes)
        assertEquals(1_000, record.txBytes)
        assertFalse(record.recovered)
        assertNull(sink.checkpoint)

        recorder.onVpnState(stopped)
        assertEquals("A stop state must not duplicate the record", 1, sink.committed.size)
    }

    @Test fun checkpointIsWrittenAtStartAndEveryFifteenSeconds() = runTest {
        recorder.onVpnState(connected(rx = 0))
        assertEquals(1, sink.checkpointSaves)

        elapsed = 10_000
        recorder.onVpnState(connected(rx = 100))
        assertEquals(1, sink.checkpointSaves)

        elapsed = 16_000
        recorder.onVpnState(connected(rx = 200))
        assertEquals(2, sink.checkpointSaves)

        elapsed = 20_000
        recorder.onVpnState(connected(rx = 300))
        assertEquals(2, sink.checkpointSaves)

        elapsed = 31_000
        recorder.onVpnState(connected(rx = 400))
        assertEquals(3, sink.checkpointSaves)
        assertEquals(400L, sink.checkpoint?.rxBytes)
    }

    @Test fun clearingHistoryWhileConnectedRebasesTheMeasurement() = runTest {
        recorder.onVpnState(connected(rx = 0))
        elapsed = 60_000
        recorder.onVpnState(connected(rx = 4_000))

        recorder.onHistoryCleared()
        elapsed = 70_000
        recorder.onVpnState(connected(rx = 5_000))
        recorder.onVpnState(stopped)

        val record = sink.committed.single()
        assertEquals("Only time after the clear is counted", 10_000, record.durationMillis)
        assertEquals("Only bytes after the clear are counted", 1_000, record.rxBytes)
    }

    @Test fun restoredTunnelWithoutStartTimeIsNeverInvented() = runTest {
        recorder.onVpnState(connected(rx = 9_000, started = null))
        assertNull(sink.checkpoint)
        elapsed = 10_000
        recorder.onVpnState(stopped)
        assertTrue(sink.committed.isEmpty())
    }

    @Test fun zeroLengthZeroByteSessionLeavesNoRecordOrCheckpoint() = runTest {
        recorder.onVpnState(connected(rx = 0))
        recorder.onVpnState(stopped)
        assertTrue(sink.committed.isEmpty())
        assertNull(sink.checkpoint)
    }

    @Test fun recoveredCheckpointIsCommittedExactlyOnce() = runTest {
        sink.checkpoint = UsageRecord(
            profileId = "p",
            profileName = "Home",
            endpoint = "vpn.example.com:51820",
            startedAtMillis = 1_760_000_000_000,
            durationMillis = 5_000,
            rxBytes = 1_000,
            txBytes = 500,
        )
        recorder.recoverCheckpoint()
        assertEquals(1, sink.committed.size)
        assertTrue(sink.committed.single().recovered)
        assertNull(sink.checkpoint)

        recorder.recoverCheckpoint()
        assertEquals(1, sink.committed.size)
    }

    @Test fun zeroLengthRecoveredCheckpointIsDiscarded() = runTest {
        sink.checkpoint = UsageRecord(
            profileId = "p",
            profileName = "Home",
            endpoint = "",
            startedAtMillis = 1_760_000_000_000,
            durationMillis = 0,
            rxBytes = 0,
            txBytes = 0,
        )
        recorder.recoverCheckpoint()
        assertTrue(sink.committed.isEmpty())
        assertNull(sink.checkpoint)
    }

    @Test fun saveFailureIsExposedAndRecoversOnNextSuccess() = runTest {
        sink.failCommit = true
        recorder.onVpnState(connected(rx = 0))
        elapsed = 5_000
        recorder.onVpnState(connected(rx = 1_000))
        recorder.onVpnState(stopped)
        assertTrue(recorder.saveFailed.value)
        assertTrue(sink.committed.isEmpty())

        sink.failCommit = false
        recorder.onVpnState(connected(rx = 0, started = 1_760_000_100_000))
        elapsed = 10_000
        recorder.onVpnState(connected(rx = 1_000, started = 1_760_000_100_000))
        recorder.onVpnState(stopped)
        assertFalse(recorder.saveFailed.value)
        assertEquals(1, sink.committed.size)
    }

    @Test fun countersNeverRegressWithinOneSession() = runTest {
        recorder.onVpnState(connected(rx = 0))
        elapsed = 5_000
        recorder.onVpnState(connected(rx = 1_000))
        elapsed = 10_000
        recorder.onVpnState(connected(rx = 500)) // a lower sample must not shrink the record
        recorder.onVpnState(stopped)
        assertEquals(1_000, sink.committed.single().rxBytes)
    }

    @Test fun checkpointFailureIsExposedWithoutLosingTheTunnel() = runTest {
        sink.failCheckpoint = true
        recorder.onVpnState(connected(rx = 0))
        assertTrue(recorder.saveFailed.value)

        sink.failCheckpoint = false
        elapsed = 20_000
        recorder.onVpnState(connected(rx = 2_000))
        assertFalse(recorder.saveFailed.value)
        recorder.onVpnState(stopped)
        assertEquals(1, sink.committed.size)
    }
}
