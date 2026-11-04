package com.knoux.kxprivatevpn

import com.knoux.kxprivatevpn.platform.usage.StatsRange
import com.knoux.kxprivatevpn.platform.usage.UsageAggregator
import com.knoux.kxprivatevpn.platform.usage.UsageRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class UsageAggregatorTest {
    private val today = LocalDate.of(2026, 11, 5)
    private val now = today.atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()

    private fun at(daysAgo: Long, hour: Int = 12): Long =
        today.minusDays(daysAgo).atTime(hour, 0).toInstant(ZoneOffset.UTC).toEpochMilli()

    private fun record(
        daysAgo: Long = 0,
        duration: Long = 60_000,
        rx: Long = 1_000,
        tx: Long = 0,
        endpoint: String = "vpn.example.com:51820",
        name: String = "Home",
        hour: Int = 12,
    ) = UsageRecord(
        profileId = "profile-$endpoint",
        profileName = name,
        endpoint = endpoint,
        startedAtMillis = at(daysAgo, hour),
        durationMillis = duration,
        rxBytes = rx,
        txBytes = tx,
    )

    private fun summarize(records: List<UsageRecord>, range: StatsRange = StatsRange.WEEK) =
        UsageAggregator.summarize(records, range, now, ZoneOffset.UTC)

    @Test fun emptySummaryHasZeroesAndNoInventedRates() {
        val summary = summarize(emptyList())
        assertEquals(7, summary.daily.size)
        assertEquals(7, summary.buckets.size)
        assertEquals(0, summary.totalBytes)
        assertEquals(0, summary.sessions)
        assertNull(summary.averageBytesPerSecond)
        assertNull(summary.mostUsed)
        assertNull(summary.previousTotalBytes)
        assertFalse(summary.hasAnyRecords)
        assertFalse(summary.hasRangeData)
    }

    @Test fun totalsAndAverageUseMeasuredBytesAndMonotonicDuration() {
        val summary = summarize(
            listOf(
                record(rx = 2_000, tx = 1_000, duration = 60_000),
                record(rx = 1_000, tx = 0, duration = 60_000),
            ),
        )
        assertEquals(4_000, summary.totalBytes)
        assertEquals(3_000, summary.rxBytes)
        assertEquals(1_000, summary.txBytes)
        assertEquals(120_000, summary.durationMillis)
        assertEquals(2, summary.sessions)
        assertEquals(33L, summary.averageBytesPerSecond) // 4,000 B / 120 s, integer bytes
        assertEquals(25L, summary.averageRxBytesPerSecond)
    }

    @Test fun previousWindowIsSeparateAndReportedOnlyWhenMeasured() {
        val summary = summarize(
            listOf(
                record(daysAgo = 0, rx = 1_000),
                record(daysAgo = 10, rx = 7_000),
                record(daysAgo = 100, rx = 9_000),
            ),
        )
        assertEquals(1_000, summary.totalBytes)
        assertEquals(7_000L, summary.previousTotalBytes)
        assertEquals(1, summary.previousSessions)
        assertTrue(summary.hasAnyRecords)
    }

    @Test fun dailyBucketsStayZeroWithoutMeasurements() {
        val summary = summarize(listOf(record(daysAgo = 1, rx = 4_000, tx = 1_000)))
        assertEquals(7, summary.daily.size)
        assertEquals(5_000, summary.daily[5].bytes) // yesterday
        assertEquals(0, summary.daily[6].bytes) // today
        assertEquals(1, summary.daily[5].sessions)
    }

    @Test fun sessionsCrossingMidnightAreAttributedToTheirStartDay() {
        val summary = summarize(
            listOf(record(daysAgo = 1, hour = 23, duration = 20 * 60_000, rx = 2_000)),
        )
        assertEquals(2_000, summary.daily[5].bytes)
        assertEquals(0, summary.daily[6].bytes)
    }

    @Test fun rangesUseTheirDeclaredWindows() {
        val thirty = summarize(listOf(record(daysAgo = 20, rx = 5_000)), StatsRange.MONTH)
        assertEquals(30, thirty.daily.size)
        assertEquals(5_000, thirty.totalBytes)

        val quarter = summarize(listOf(record(daysAgo = 80, rx = 6_000)), StatsRange.QUARTER)
        assertEquals(90, quarter.daily.size)
        assertEquals(13, quarter.buckets.size)
        assertEquals(6_000, quarter.buckets.sumOf { it.bytes })

        val year = summarize(listOf(record(daysAgo = 300, rx = 7_000)), StatsRange.YEAR)
        assertEquals(365, year.daily.size)
        assertEquals(13, year.buckets.size) // 365 days spans 13 calendar months
        assertEquals(7_000, year.buckets.sumOf { it.bytes })
    }

    @Test fun futureClockRecordsAreNeverPlacedInTheChart() {
        val summary = summarize(listOf(record(daysAgo = -1, rx = 9_000)))
        assertEquals(0, summary.totalBytes)
        assertTrue(summary.hasAnyRecords)
        assertEquals(0, summary.daily.sumOf { it.bytes })
    }

    @Test fun mostUsedIsChosenByMeasuredDurationThenBytes() {
        val summary = summarize(
            listOf(
                record(endpoint = "a", name = "Alpha", duration = 600_000),
                record(endpoint = "a", name = "Alpha", duration = 120_000),
                record(endpoint = "b", name = "Beta", duration = 300_000, rx = 50_000),
            ),
        )
        val top = summary.mostUsed!!
        assertEquals("Alpha", top.label)
        assertEquals(720_000, top.durationMillis)
        assertEquals(3, summary.sessions)
        assertEquals(720_000.0 / 1_020_000.0, top.shareOfDuration, 1e-9)
    }

    @Test fun serverLabelComesFromItsLatestSession() {
        val summary = summarize(
            listOf(
                record(endpoint = "same", name = "Old name", daysAgo = 3),
                record(endpoint = "same", name = "New name", daysAgo = 1),
            ),
        )
        assertEquals("New name", summary.servers.single().label)
    }

    @Test fun serverHistoryTrendCoversLastSevenDays() {
        val summary = summarize(
            listOf(
                record(endpoint = "a", daysAgo = 6, rx = 100),
                record(endpoint = "a", daysAgo = 3, rx = 300),
                record(endpoint = "a", daysAgo = 0, rx = 500),
            ),
        )
        val trend = summary.servers.single().trend
        assertEquals(7, trend.size)
        assertEquals(100, trend[0])
        assertEquals(0, trend[1])
        assertEquals(300, trend[3])
        assertEquals(500, trend[6])
    }

    @Test fun zeroDurationHasNoSpeedInsteadOfZeroOrInfinity() {
        val summary = summarize(listOf(record(duration = 0, rx = 1_000)))
        assertNull(summary.averageBytesPerSecond)
        assertNull(summary.averageRxBytesPerSecond)
        assertEquals(1_000, summary.totalBytes)
    }

    @Test fun corruptRecordsAreDroppedBeforeAggregation() {
        val summary = summarize(
            listOf(
                record(rx = -1),
                record(duration = -5),
                record(endpoint = "ok", rx = 123),
            ),
        )
        assertEquals(123, summary.totalBytes)
        assertEquals(1, summary.sessions)
    }
}
