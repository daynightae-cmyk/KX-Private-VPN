package com.knoux.kxprivatevpn.platform.repair

import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

enum class RepairOutcome { COMPLETE, PARTIAL, FAILED }

/**
 * One genuinely completed repair operation. Entries never contain file paths, profile names,
 * keys or any other sensitive content: only the service, the operation class, the outcome and
 * measured counts. The journal is app-private and excluded from backup by its location.
 */
data class RepairJournalEntry(
    val atMillis: Long,
    val serviceKey: String,
    val action: String,
    val outcome: RepairOutcome,
    val items: Int,
    val bytes: Long,
)

/**
 * Bounded, append-only repair journal. A corrupt file is preserved and reported as empty
 * rather than overwritten or silently "repaired" with invented entries.
 */
class RepairJournal(private val file: File, private val limit: Int = 50) {
    private val atomic = AtomicFile(file)

    fun load(): List<RepairJournalEntry> = synchronized(lock) {
        try {
            if (!atomic.baseFile.exists() && !File(atomic.baseFile.path + ".bak").exists()) {
                return emptyList()
            }
            val bytes = atomic.openRead().use { it.readBytes() }
            parse(bytes.toString(Charsets.UTF_8))
        } catch (_: Exception) {
            emptyList()
        }
    }

    @Throws(Exception::class)
    fun append(entry: RepairJournalEntry): List<RepairJournalEntry> = synchronized(lock) {
        require(entry.atMillis > 0) { "Invalid timestamp" }
        require(entry.serviceKey.isNotBlank()) { "Missing service" }
        require(entry.items >= 0 && entry.bytes >= 0) { "Invalid measurement" }
        val merged = (listOf(entry) + loadLocked()).take(limit)
        val output = atomic.startWrite()
        try {
            output.write(encode(merged).toByteArray(Charsets.UTF_8))
            atomic.finishWrite(output)
        } catch (e: Exception) {
            atomic.failWrite(output)
            throw e
        }
        merged
    }

    private fun loadLocked(): List<RepairJournalEntry> {
        if (!atomic.baseFile.exists() && !File(atomic.baseFile.path + ".bak").exists()) return emptyList()
        return try {
            val raw = atomic.openRead().use { it.readBytes() }.toString(Charsets.UTF_8)
            RepairJournalCodec.decode(raw, limit)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parse(raw: String): List<RepairJournalEntry> = RepairJournalCodec.decode(raw, limit)

    private fun encode(entries: List<RepairJournalEntry>): String = RepairJournalCodec.encode(entries)

    private companion object {
        val lock = Any()
    }
}

/**
 * Pure JSON codec, kept separate from AtomicFile so corrupt-input and ordering behavior can be
 * verified on the JVM without Android framework stubs.
 */
internal object RepairJournalCodec {
    fun decode(raw: String, limit: Int): List<RepairJournalEntry> {
        val array = JSONArray(raw)
        val values = mutableListOf<RepairJournalEntry>()
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val at = item.optLong("at", 0)
            val service = item.optString("service", "")
            val action = item.optString("action", "")
            val outcome = runCatching { RepairOutcome.valueOf(item.optString("outcome")) }.getOrNull()
                ?: continue
            val items = item.optInt("items", -1)
            val bytes = item.optLong("bytes", -1)
            if (at <= 0 || service.isBlank() || action.isBlank() || items < 0 || bytes < 0) continue
            values += RepairJournalEntry(at, service, action, outcome, items, bytes)
        }
        return values.sortedByDescending { it.atMillis }.take(limit)
    }

    fun encode(entries: List<RepairJournalEntry>): String {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(JSONObject()
                .put("at", entry.atMillis)
                .put("service", entry.serviceKey)
                .put("action", entry.action)
                .put("outcome", entry.outcome.name)
                .put("items", entry.items)
                .put("bytes", entry.bytes))
        }
        return array.toString()
    }
}
