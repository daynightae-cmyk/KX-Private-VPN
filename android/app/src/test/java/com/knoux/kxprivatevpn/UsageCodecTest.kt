package com.knoux.kxprivatevpn

import com.knoux.kxprivatevpn.platform.usage.UsageCodec
import com.knoux.kxprivatevpn.platform.usage.UsageRecord
import com.knoux.kxprivatevpn.platform.usage.appendMeasured
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageCodecTest {
    private fun record(
        name: String = "Home",
        endpoint: String = "vpn.example.com:51820",
        duration: Long = 60_000,
        reviewed: Boolean = false,
    ) = UsageRecord(
        profileId = "profile-1",
        profileName = name,
        endpoint = endpoint,
        startedAtMillis = 1_760_000_000_000,
        durationMillis = duration,
        rxBytes = 1_500,
        txBytes = 900,
        recovered = reviewed,
    )

    @Test fun roundTripPreservesUnicodeAndSeparators() {
        val original = listOf(record(name = "سيرفر\tالمنزل\nالرئيسي", endpoint = "2001:db8::1#tab\there"))
        val decoded = UsageCodec.decode(UsageCodec.encode(original))
        assertEquals(original, decoded)
    }

    @Test fun roundTripPreservesRecoveredFlag() {
        val decoded = UsageCodec.decode(UsageCodec.encode(listOf(record(reviewed = true))))
        assertEquals(true, decoded.single().recovered)
    }

    @Test fun missingOrUnknownHeaderDropsEverything() {
        assertTrue(UsageCodec.decode(null).isEmpty())
        assertTrue(UsageCodec.decode("").isEmpty())
        assertTrue(UsageCodec.decode("other-format\nr\t...").isEmpty())
    }

    @Test fun corruptLineDoesNotDiscardValidNeighbours() {
        val good = UsageCodec.encodeRecord(record())
        val raw = UsageCodec.HEADER + "\n" + good + "\nnot-a-record\n" + "r\tbroken"
        val decoded = UsageCodec.decode(raw)
        assertEquals(1, decoded.size)
        assertEquals("Home", decoded.single().profileName)
    }

    @Test fun checkpointRoundTripAndCorruption() {
        val raw = UsageCodec.encodeRecord(record(duration = 5_000))
        assertEquals(5_000L, UsageCodec.decodeCheckpoint(raw)?.durationMillis)
        assertNull(UsageCodec.decodeCheckpoint("garbage"))
        assertNull(UsageCodec.decodeCheckpoint(null))
    }

    @Test fun negativeAndAbsurdMeasurementsAreRejected() {
        val bad = listOf(
            record(duration = -1),
            record(duration = Long.MAX_VALUE),
            record().copy(rxBytes = -5),
            record().copy(txBytes = Long.MAX_VALUE),
            record().copy(startedAtMillis = 0),
        )
        assertTrue(UsageCodec.encode(bad).lineSequence().count() == 1) // header only
        assertTrue(bad.mapNotNull { it.sanitizedForTest() }.isEmpty())
    }

    private fun UsageRecord.sanitizedForTest(): UsageRecord? = UsageCodec.decodeCheckpoint(
        UsageCodec.encodeRecord(this),
    )

    @Test fun retentionKeepsTheNewestSessionsOnly() {
        val base = record().copy(profileId = "p")
        val existing = (1..3).map {
            base.copy(startedAtMillis = 1_760_000_000_000 + it * 1_000, endpoint = "old-$it")
        }
        val incoming = base.copy(startedAtMillis = 1_760_000_009_000, endpoint = "new")

        val kept = appendMeasured(existing, incoming, maxRecords = 3)

        assertEquals(listOf("old-2", "old-3", "new"), kept.map { it.endpoint })
    }

    @Test fun retentionSortsBeforeTrimmingSoTheOldestWallClockSessionGoes() {
        val base = record().copy(profileId = "p")
        val newer = base.copy(startedAtMillis = 1_760_000_009_000, endpoint = "newer")
        val oldest = base.copy(startedAtMillis = 1_760_000_001_000, endpoint = "oldest")

        val kept = appendMeasured(listOf(newer), oldest, maxRecords = 1)

        assertEquals("newer", kept.single().endpoint)
    }
}
