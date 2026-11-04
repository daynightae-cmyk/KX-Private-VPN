package com.knoux.kxprivatevpn.platform.usage

import java.util.Base64

/**
 * One completed tunnel session, measured by the app itself.
 *
 * [durationMillis] is monotonic elapsed time, never wall-clock subtraction. [startedAtMillis] is
 * wall time and is used only to place the session on a calendar day. Counters are the WireGuard
 * backend's cumulative RX/TX delta across this session; no value here is estimated or sampled
 * from anywhere else.
 */
data class UsageRecord(
    val profileId: String,
    val profileName: String,
    val endpoint: String,
    val startedAtMillis: Long,
    val durationMillis: Long,
    val rxBytes: Long,
    val txBytes: Long,
    /** True when the process died mid-session and the last checkpoint was recovered. */
    val recovered: Boolean = false,
) {
    val totalBytes: Long get() = rxBytes.coerceAtLeast(0) + txBytes.coerceAtLeast(0)
    val endedAtMillis: Long get() = startedAtMillis + durationMillis
}

/** Maximum plausible stored duration: one year. Longer values are corruption, not measurements. */
const val MAX_USAGE_DURATION_MS = 365L * 24 * 60 * 60 * 1000

private const val MAX_BYTES = 10_000_000_000_000_000L // 10 PB
private const val MAX_TEXT = 200

/** Drops corrupt records; keeps genuinely measured ones. Never repairs numbers by guessing. */
fun UsageRecord.sanitized(): UsageRecord? {
    val id = profileId.trim().take(MAX_TEXT)
    val name = profileName.trim().take(MAX_TEXT)
    val host = endpoint.trim().take(MAX_TEXT * 2)
    if (id.isEmpty() && name.isEmpty() && host.isEmpty()) return null
    if (startedAtMillis <= 0) return null
    if (durationMillis < 0 || durationMillis > MAX_USAGE_DURATION_MS) return null
    if (rxBytes < 0 || txBytes < 0 || rxBytes > MAX_BYTES || txBytes > MAX_BYTES) return null
    return copy(profileId = id, profileName = name, endpoint = host)
}

/**
 * Private, versioned, line-oriented codec. Base64 keeps arbitrary profile names and endpoints
 * (tabs/newlines included) from breaking the record boundary. The format stays testable on the
 * JVM without Android's org.json.
 */
object UsageCodec {
    const val HEADER = "kx-usage-v1"
    private const val FIELD_SEPARATOR = '\t'
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()

    fun encode(records: List<UsageRecord>): String =
        (listOf(HEADER) + records.mapNotNull { it.sanitized()?.let(::encodeRecord) })
            .joinToString("\n")

    fun encodeRecord(record: UsageRecord): String = listOf(
        "r",
        text(record.profileId),
        text(record.profileName),
        text(record.endpoint),
        record.startedAtMillis.toString(),
        record.durationMillis.toString(),
        record.rxBytes.toString(),
        record.txBytes.toString(),
        if (record.recovered) "1" else "0",
    ).joinToString(FIELD_SEPARATOR.toString())

    fun decode(raw: String?): List<UsageRecord> {
        if (raw.isNullOrBlank()) return emptyList()
        val lines = raw.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        if (lines.firstOrNull() != HEADER) return emptyList()
        return lines.drop(1).mapNotNull(::decodeRecord).mapNotNull { it.sanitized() }
    }

    fun decodeCheckpoint(raw: String?): UsageRecord? =
        if (raw.isNullOrBlank()) null else decodeRecord(raw.trim())?.sanitized()

    private fun decodeRecord(line: String): UsageRecord? {
        val parts = line.split(FIELD_SEPARATOR)
        if (parts.size != 9 || parts[0] != "r") return null
        return try {
            UsageRecord(
                profileId = untext(parts[1]),
                profileName = untext(parts[2]),
                endpoint = untext(parts[3]),
                startedAtMillis = parts[4].toLong(),
                durationMillis = parts[5].toLong(),
                rxBytes = parts[6].toLong(),
                txBytes = parts[7].toLong(),
                recovered = parts[8] == "1",
            )
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun text(value: String): String = encoder.encodeToString(value.toByteArray(Charsets.UTF_8))

    private fun untext(value: String): String =
        String(decoder.decode(value), Charsets.UTF_8)
}
