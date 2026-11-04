package com.knoux.kxprivatevpn.platform.usage

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

enum class StatsRange(val days: Int) {
    WEEK(7), MONTH(30), QUARTER(90), YEAR(365);

    companion object {
        fun fromDays(days: Int): StatsRange = entries.firstOrNull { it.days == days } ?: WEEK
    }
}

data class DailyUsage(
    val epochDay: Long,
    val bytes: Long,
    val durationMillis: Long,
    val sessions: Int,
)

/** One column of the chart. Short ranges use days; 3M uses weeks; 1Y uses calendar months. */
data class ChartBucket(
    val startEpochDay: Long,
    val endEpochDay: Long,
    val bytes: Long,
    val durationMillis: Long,
    val sessions: Int,
) {
    val days: Int get() = (endEpochDay - startEpochDay + 1).toInt()
}

data class ServerUsage(
    val key: String,
    val label: String,
    val endpoint: String,
    val bytes: Long,
    val durationMillis: Long,
    val sessions: Int,
    val lastStartedAtMillis: Long,
    val lastDurationMillis: Long,
    /** Bytes per day for the last seven calendar days, oldest first. */
    val trend: List<Long>,
    val shareOfDuration: Double,
)

data class UsageSummary(
    val range: StatsRange,
    val rangeStartEpochDay: Long,
    val rangeEndEpochDay: Long,
    val totalBytes: Long,
    val rxBytes: Long,
    val txBytes: Long,
    val durationMillis: Long,
    val sessions: Int,
    val averageBytesPerSecond: Long?,
    val averageRxBytesPerSecond: Long?,
    val averageTxBytesPerSecond: Long?,
    val servers: List<ServerUsage>,
    val mostUsed: ServerUsage?,
    val daily: List<DailyUsage>,
    val buckets: List<ChartBucket>,
    /** Null when the previous equally-sized window has no measured sessions at all. */
    val previousTotalBytes: Long?,
    val previousDurationMillis: Long?,
    val previousSessions: Int,
    val hasAnyRecords: Boolean,
) {
    val hasRangeData: Boolean get() = sessions > 0

    companion object {
        fun empty(range: StatsRange, nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): UsageSummary =
            UsageAggregator.summarize(emptyList(), range, nowMillis, zone)
    }
}

/**
 * Pure aggregation over completed [UsageRecord]s. Every output is derived from measured counters
 * and monotonic durations; days without measurements stay zero and are never interpolated.
 *
 * A session that crosses midnight is attributed to the calendar day it started on. That is stated
 * in the UI copy and keeps the arithmetic exact rather than inventing a split.
 */
object UsageAggregator {
    fun summarize(
        records: List<UsageRecord>,
        range: StatsRange,
        nowMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): UsageSummary {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val start = today.minusDays((range.days - 1).toLong())
        val clean = records.mapNotNull { it.sanitized() }

        val days = LinkedHashMap<Long, MutableDay>()
        var cursor = start
        while (!cursor.isAfter(today)) {
            days[cursor.toEpochDay()] = MutableDay()
            cursor = cursor.plusDays(1)
        }

        val previousStart = start.minusDays(range.days.toLong())
        val previousEnd = start.minusDays(1)
        var previousBytes = 0L
        var previousDuration = 0L
        var previousSessions = 0
        var inRange = 0

        for (record in clean) {
            val day = Instant.ofEpochMilli(record.startedAtMillis).atZone(zone).toLocalDate()
            when {
                day.isBefore(previousStart) -> Unit
                day.isBefore(start) -> {
                    previousBytes += record.totalBytes
                    previousDuration += record.durationMillis
                    previousSessions++
                }
                !day.isAfter(today) -> {
                    val bucket = days.getValue(day.toEpochDay())
                    bucket.bytes += record.totalBytes
                    bucket.duration += record.durationMillis
                    bucket.sessions++
                    inRange++
                }
                // A clock set into the future cannot be placed honestly; leave it out of buckets.
            }
        }

        val daily = days.map { (epochDay, value) ->
            DailyUsage(epochDay, value.bytes, value.duration, value.sessions)
        }

        val rangeRecords = clean.filter { record ->
            val day = Instant.ofEpochMilli(record.startedAtMillis).atZone(zone).toLocalDate()
            !day.isBefore(start) && !day.isAfter(today)
        }
        val totalBytes = rangeRecords.sumOf { it.totalBytes }
        val rxBytes = rangeRecords.sumOf { it.rxBytes }
        val txBytes = rangeRecords.sumOf { it.txBytes }
        val durationMillis = rangeRecords.sumOf { it.durationMillis }

        val servers = serverUsage(rangeRecords, today, zone, durationMillis)

        return UsageSummary(
            range = range,
            rangeStartEpochDay = start.toEpochDay(),
            rangeEndEpochDay = today.toEpochDay(),
            totalBytes = totalBytes,
            rxBytes = rxBytes,
            txBytes = txBytes,
            durationMillis = durationMillis,
            sessions = inRange,
            averageBytesPerSecond = rate(totalBytes, durationMillis),
            averageRxBytesPerSecond = rate(rxBytes, durationMillis),
            averageTxBytesPerSecond = rate(txBytes, durationMillis),
            servers = servers,
            mostUsed = servers.firstOrNull(),
            daily = daily,
            buckets = chartBuckets(daily, range),
            previousTotalBytes = previousSessions.takeIf { it > 0 }?.let { previousBytes },
            previousDurationMillis = previousSessions.takeIf { it > 0 }?.let { previousDuration },
            previousSessions = previousSessions,
            hasAnyRecords = clean.isNotEmpty(),
        )
    }

    private class MutableDay {
        var bytes = 0L
        var duration = 0L
        var sessions = 0
    }

    private class MutableServer {
        var label = ""
        var endpoint = ""
        var bytes = 0L
        var duration = 0L
        var sessions = 0
        var lastStartedAt = 0L
        var lastDuration = 0L
        var lastSeenAt = 0L
    }

    private fun serverUsage(
        records: List<UsageRecord>,
        today: LocalDate,
        zone: ZoneId,
        totalDuration: Long,
    ): List<ServerUsage> {
        val trendStart = today.minusDays(6).toEpochDay()
        val names = LinkedHashMap<String, MutableServer>()
        val trends = LinkedHashMap<String, LongArray>()

        for (record in records) {
            val key = record.endpoint.ifBlank { record.profileId }.ifBlank { "unknown" }
            val server = names.getOrPut(key) { MutableServer() }
            if (record.startedAtMillis >= server.lastSeenAt) {
                server.lastSeenAt = record.startedAtMillis
                server.label = record.profileName.ifBlank { record.endpoint }.ifBlank { key }
                server.endpoint = record.endpoint
            }
            server.bytes += record.totalBytes
            server.duration += record.durationMillis
            server.sessions++
            if (record.startedAtMillis >= server.lastStartedAt) {
                server.lastStartedAt = record.startedAtMillis
                server.lastDuration = record.durationMillis
            }

            val day = Instant.ofEpochMilli(record.startedAtMillis).atZone(zone).toLocalDate().toEpochDay()
            if (day >= trendStart && day <= today.toEpochDay()) {
                val series = trends.getOrPut(key) { LongArray(7) }
                series[(day - trendStart).toInt()] += record.totalBytes
            }
        }

        return names.map { (key, value) ->
            ServerUsage(
                key = key,
                label = value.label.ifBlank { key },
                endpoint = value.endpoint,
                bytes = value.bytes,
                durationMillis = value.duration,
                sessions = value.sessions,
                lastStartedAtMillis = value.lastStartedAt,
                lastDurationMillis = value.lastDuration,
                trend = trends[key]?.toList().orEmpty().ifEmpty { List(7) { 0L } },
                shareOfDuration = if (totalDuration <= 0) 0.0
                else value.duration.toDouble() / totalDuration.toDouble(),
            )
        }.sortedWith(compareByDescending<ServerUsage> { it.durationMillis }
            .thenByDescending { it.bytes }
            .thenBy { it.label })
    }

    private fun chartBuckets(daily: List<DailyUsage>, range: StatsRange): List<ChartBucket> =
        when (range) {
            StatsRange.WEEK, StatsRange.MONTH -> daily.map { day ->
                ChartBucket(day.epochDay, day.epochDay, day.bytes, day.durationMillis, day.sessions)
            }
            StatsRange.QUARTER -> chunkDays(daily, 7)
            StatsRange.YEAR -> chunkMonths(daily)
        }

    private fun chunkDays(daily: List<DailyUsage>, size: Int): List<ChartBucket> =
        daily.chunked(size).map { chunk ->
            ChartBucket(
                startEpochDay = chunk.first().epochDay,
                endEpochDay = chunk.last().epochDay,
                bytes = chunk.sumOf { it.bytes },
                durationMillis = chunk.sumOf { it.durationMillis },
                sessions = chunk.sumOf { it.sessions },
            )
        }

    private fun chunkMonths(daily: List<DailyUsage>): List<ChartBucket> {
        val groups = LinkedHashMap<YearMonth, MutableList<DailyUsage>>()
        for (day in daily) {
            groups.getOrPut(YearMonth.from(LocalDate.ofEpochDay(day.epochDay))) { mutableListOf() } += day
        }
        return groups.values.map { chunk ->
            ChartBucket(
                startEpochDay = chunk.first().epochDay,
                endEpochDay = chunk.last().epochDay,
                bytes = chunk.sumOf { it.bytes },
                durationMillis = chunk.sumOf { it.durationMillis },
                sessions = chunk.sumOf { it.sessions },
            )
        }
    }

    private fun rate(bytes: Long, durationMillis: Long): Long? =
        if (durationMillis <= 0) null
        else (bytes.toDouble() * 1000.0 / durationMillis.toDouble()).toLong().coerceAtLeast(0)
}
