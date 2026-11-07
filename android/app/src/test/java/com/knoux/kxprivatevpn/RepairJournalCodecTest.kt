package com.knoux.kxprivatevpn

import com.knoux.kxprivatevpn.platform.repair.RepairJournalCodec
import com.knoux.kxprivatevpn.platform.repair.RepairJournalEntry
import com.knoux.kxprivatevpn.platform.repair.RepairOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.json.JSONArray
import org.json.JSONObject

/**
 * The repair journal must round-trip only measured operations, ignore corrupt entries instead
 * of inventing values, and never grow without bound.
 */
class RepairJournalCodecTest {
    private fun entry(at: Long, service: String = "cache", action: String = "cache_remove",
                      outcome: RepairOutcome = RepairOutcome.COMPLETE, items: Int = 2,
                      bytes: Long = 4096) =
        RepairJournalEntry(at, service, action, outcome, items, bytes)

    @Test fun roundTripPreservesMeasuredFields() {
        val original = listOf(entry(1_700_000_000_000, items = 7, bytes = 123_456),
            entry(1_700_000_100_000, outcome = RepairOutcome.PARTIAL, items = 0, bytes = 0))
        val decoded = RepairJournalCodec.decode(RepairJournalCodec.encode(original), 50)
        assertEquals(original.sortedByDescending { it.atMillis }, decoded)
    }

    @Test fun newestEntriesComeFirstAndLimitApplies() {
        val values = (1..60).map { entry(it.toLong() * 1_000) }
        val decoded = RepairJournalCodec.decode(RepairJournalCodec.encode(values), 50)
        assertEquals(50, decoded.size)
        assertEquals(60_000L, decoded.first().atMillis)
        assertTrue(decoded.zipWithNext().all { (a, b) -> a.atMillis >= b.atMillis })
    }

    @Test fun invalidEntriesAreSkippedWithoutThrowing() {
        val array = JSONArray()
        array.put(JSONObject().put("at", 10).put("service", "cache").put("action", "cache_remove")
            .put("outcome", "COMPLETE").put("items", 1).put("bytes", 10))
        array.put(JSONObject().put("at", 0).put("service", "cache").put("action", "cache_remove")
            .put("outcome", "COMPLETE").put("items", 1).put("bytes", 10))
        array.put(JSONObject().put("at", 20).put("service", ""))
        array.put(JSONObject().put("at", 30).put("service", "cache").put("action", "cache_remove")
            .put("outcome", "UNKNOWN").put("items", 1).put("bytes", 10))
        array.put(JSONObject().put("at", 40).put("service", "cache").put("action", "cache_remove")
            .put("outcome", "COMPLETE").put("items", -1).put("bytes", 10))
        array.put("not an object")
        val decoded = RepairJournalCodec.decode(array.toString(), 50)
        assertEquals(1, decoded.size)
        assertEquals(10L, decoded.single().atMillis)
    }

    @Test(expected = org.json.JSONException::class)
    fun malformedJsonIsRejectedByTheCodec() {
        RepairJournalCodec.decode("not json", 50)
    }

    @Test fun mutationRejectsLossyOrAmbiguousInputButAcceptsExactRecords() {
        val valid = RepairJournalCodec.encode(listOf(entry(1_000)))
        assertEquals(listOf(entry(1_000)), RepairJournalCodec.decodeForMutation(valid))
        for (raw in listOf("not json", "[null]", "[{}]", valid + "junk",
            valid.replace("\"at\":1000", "\"at\":1000,\"at\":2000"),
            valid.replace("\"items\":2", "\"items\":2.5"),
            valid.replace("\"items\":2", "\"items\":2147483648"),
            valid.replace("\"items\":2", "\"unknown\":2"))) {
            try {
                RepairJournalCodec.decodeForMutation(raw)
                org.junit.Assert.fail("Invalid mutation input must not discard data")
            } catch (_: IllegalArgumentException) { }
            catch (_: IllegalStateException) { }
        }
    }

    @Test fun mutationRejectsUnpairedSurrogatesButPreservesSupplementaryCharacters() {
        val valid = entry(1_000, service = "service-\uD801\uDC00")
        assertEquals(listOf(valid), RepairJournalCodec.decodeForMutation(RepairJournalCodec.encode(listOf(valid))))
        val encoded = RepairJournalCodec.encode(listOf(entry(1_000)))
        for (escape in listOf("\\uD800", "\\uDC00", "\\uD800x", "x\\uDC00")) {
            try {
                RepairJournalCodec.decodeForMutation(encoded.replace("\"cache\"", "\"$escape\""))
                org.junit.Assert.fail("Unpaired surrogate must not become a lossy survivor")
            } catch (_: IllegalArgumentException) { }
        }
        try {
            RepairJournalCodec.validate(entry(1_000, action = "bad-\uD800"))
            org.junit.Assert.fail("Invalid new strings cannot be persisted")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun emptyArrayDecodesEmpty() {
        assertTrue(RepairJournalCodec.decode("[]", 50).isEmpty())
    }
}
