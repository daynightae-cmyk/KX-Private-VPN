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
        require(limit > 0) { "Invalid journal limit" }
        RepairJournalCodec.validate(entry)
        val merged = (listOf(entry) + loadLocked().sortedByDescending { it.atMillis }).take(limit)
        writeLocked(merged)
        merged
    }

    /** Compatibility replacement; retention callers must use [removeApproved] instead. */
    @Throws(Exception::class)
    fun replace(entries: List<RepairJournalEntry>): List<RepairJournalEntry> = synchronized(lock) {
        loadLocked() // Never overwrite a corrupt source, even when replacing it with an empty list.
        entries.forEach(RepairJournalCodec::validate)
        val kept = entries.sortedByDescending { it.atMillis }.take(limit)
        writeLocked(kept)
        kept
    }

    /**
     * Removes only exact approved occurrences from the CURRENT journal under the append lock.
     * No display limit/retention trimming is applied here: all unapproved records must survive.
     * A count is returned only after AtomicFile's committed bytes have been read back.
     */
    @Throws(Exception::class)
    fun removeApproved(approved: List<RepairJournalEntry>): Int = synchronized(lock) {
        approved.forEach(RepairJournalCodec::validate)
        val current = loadLocked()
        val kept = survivorsAfterApproval(current, approved)
        val removed = current.size - kept.size
        if (removed > 0) writeLocked(kept)
        removed
    }

    /** Removes the whole valid journal atomically, with the same verified-count contract. */
    @Throws(Exception::class)
    fun clear(): Int = synchronized(lock) {
        val current = loadLocked()
        if (current.isNotEmpty()) writeLocked(emptyList())
        current.size
    }

    private fun loadLocked(): List<RepairJournalEntry> {
        if (!atomic.baseFile.exists() && !File(atomic.baseFile.path + ".bak").exists()) return emptyList()
        val bytes = atomic.openRead().use { it.readBytes() }
        val raw = Charsets.UTF_8.newDecoder()
            .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
            .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
            .decode(java.nio.ByteBuffer.wrap(bytes)).toString()
        return RepairJournalCodec.decodeForMutation(raw)
    }

    private fun writeLocked(entries: List<RepairJournalEntry>) {
        val bytes = encode(entries).toByteArray(Charsets.UTF_8)
        val output = atomic.startWrite()
        try {
            output.write(bytes)
            output.fd.sync()
            atomic.finishWrite(output)
        } catch (e: Exception) {
            atomic.failWrite(output)
            throw e
        }
        // AtomicFile may log rather than throw on a failed rename. Never count that as a commit.
        check(atomic.openRead().use { it.readBytes() }.contentEquals(bytes)) { "Journal write not verified" }
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
    fun validate(entry: RepairJournalEntry) {
        require(entry.atMillis > 0 && entry.serviceKey.isNotBlank() && entry.action.isNotBlank()) {
            "Invalid journal entry"
        }
        require(entry.items >= 0 && entry.bytes >= 0) { "Invalid journal measurement" }
    }

    /** Strict and lossless mutation input; the deliberately tolerant DISPLAY decoder stays below. */
    fun decodeForMutation(raw: String): List<RepairJournalEntry> {
        val reader = MutationReader(raw)
        val entries = mutableListOf<RepairJournalEntry>()
        reader.expect('[')
        if (!reader.consume(']')) {
            do {
                reader.expect('{')
                val fields = mutableMapOf<String, Any>()
                do {
                    val key = reader.string()
                    require(key !in fields) { "Duplicate journal field" }
                    reader.expect(':')
                    fields[key] = reader.value()
                } while (reader.consume(','))
                reader.expect('}')
                require(fields.keys == setOf("at", "service", "action", "outcome", "items", "bytes")) {
                    "Unknown or missing journal fields"
                }
                val items = fields["items"] as? Long ?: error("Invalid journal count")
                require(items in 0..Int.MAX_VALUE.toLong()) { "Invalid journal count" }
                val entry = RepairJournalEntry(
                    atMillis = fields["at"] as? Long ?: error("Invalid journal timestamp"),
                    serviceKey = fields["service"] as? String ?: error("Invalid journal service"),
                    action = fields["action"] as? String ?: error("Invalid journal action"),
                    outcome = RepairOutcome.valueOf(fields["outcome"] as? String ?: error("Invalid journal outcome")),
                    items = items.toInt(),
                    bytes = fields["bytes"] as? Long ?: error("Invalid journal bytes"),
                )
                validate(entry)
                entries += entry
            } while (reader.consume(','))
            reader.expect(']')
        }
        reader.expectEnd()
        return entries
    }

    /** The stored schema is an array of flat string/integer objects, not lenient JavaScript. */
    private class MutationReader(private val raw: String) {
        private var index = 0
        private fun whitespace() {
            while (index < raw.length && raw[index] in " \t\r\n") index++
        }
        fun consume(char: Char): Boolean {
            whitespace()
            if (raw.getOrNull(index) != char) return false
            index++
            return true
        }
        fun expect(char: Char) { require(consume(char)) { "Malformed journal JSON" } }
        fun expectEnd() {
            whitespace()
            require(index == raw.length) { "Trailing journal data" }
        }
        fun string(): String {
            whitespace()
            val start = index
            expect('"')
            while (index < raw.length) {
                val char = raw[index++]
                require(char >= ' ') { "Invalid journal string" }
                if (char == '"') {
                    return org.json.JSONTokener(raw.substring(start, index)).nextValue() as String
                }
                if (char == '\\') {
                    val escape = raw.getOrNull(index++) ?: error("Truncated journal escape")
                    require(escape in "\"\\/bfnrtu") { "Invalid journal escape" }
                    if (escape == 'u') repeat(4) {
                        require(raw.getOrNull(index++)?.digitToIntOrNull(16) != null) { "Invalid Unicode escape" }
                    }
                }
            }
            error("Unterminated journal string")
        }
        fun value(): Any {
            whitespace()
            if (raw.getOrNull(index) == '"') return string()
            val start = index
            if (raw.getOrNull(index) == '-') index++
            if (raw.getOrNull(index) == '0') index++ else {
                require(raw.getOrNull(index) in '1'..'9') { "Invalid journal integer" }
                while (raw.getOrNull(index) in '0'..'9') index++
            }
            return raw.substring(start, index).toLongOrNull() ?: error("Journal integer overflow")
        }
    }

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
