package com.knoux.kxprivatevpn

import com.knoux.kxprivatevpn.platform.repair.AutopilotKind
import com.knoux.kxprivatevpn.platform.repair.AutopilotPlanner
import com.knoux.kxprivatevpn.platform.repair.RepairJournalEntry
import com.knoux.kxprivatevpn.platform.repair.RepairOutcome
import com.knoux.kxprivatevpn.platform.security.ShieldRetention
import com.knoux.kxprivatevpn.platform.storage.CacheCandidate
import com.knoux.kxprivatevpn.platform.storage.CachePreview
import com.knoux.kxprivatevpn.platform.usage.UsageRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The autopilot proposal is a pure function of real measurements. These tests pin determinism,
 * the aged-history partition, honest gaps and byte accounting so the plan can never invent a
 * finding or mis-order a target.
 */
class AutopilotTest {
    private val now = 1_760_000_000_000L
    private val day = ShieldRetention.DAY_MILLIS
    private val aged = now - 30 * day
    private val fresh = now - day

    private fun candidate(path: String, bytes: Long) = CacheCandidate(path, bytes, now, "sha-$path")

    private fun journal(at: Long) = RepairJournalEntry(at, "cache", "cache_remove", RepairOutcome.COMPLETE, 1, 10)

    private fun usage(at: Long, id: String) = UsageRecord(id, "name", "endpoint", at, 1000, 10, 20)

    @Test fun emptyMeasurementsProposeNothing() {
        val plan = AutopilotPlanner.build(CachePreview(emptyList(), false), emptyList(), emptyList(), now)
        assertTrue(plan.isEmpty)
        assertTrue(plan.cacheMeasured)
        assertEquals(0L, plan.totalBytes)
        assertNull(plan.historyTarget)
    }

    @Test fun cacheFilesBecomeTargetsLargestFirst() {
        val cache = CachePreview(
            listOf(candidate("small.tmp", 100), candidate("big.tmp", 900), candidate("mid.tmp", 500)),
            false,
        )
        val plan = AutopilotPlanner.build(cache, emptyList(), emptyList(), now)
        assertEquals(listOf("big.tmp", "mid.tmp", "small.tmp"), plan.cacheTargets.map { it.title })
        assertEquals(listOf("cache:big.tmp", "cache:mid.tmp", "cache:small.tmp"), plan.cacheTargets.map { it.id })
        assertTrue(plan.cacheTargets.all { it.kind == AutopilotKind.CACHE_FILE && it.destructive })
        assertEquals(1500L, plan.totalBytes)
    }

    @Test fun onlyAgedHistoryBecomesASingleTarget() {
        val plan = AutopilotPlanner.build(
            CachePreview(emptyList(), false),
            listOf(journal(aged), journal(fresh)),
            listOf(usage(aged, "old"), usage(fresh, "new")),
            now,
        )
        assertEquals(1, plan.agedJournal)
        assertEquals(1, plan.agedUsage)
        val history = plan.historyTarget
        assertEquals(AutopilotPlanner.HISTORY_ID, history?.id)
        assertEquals(AutopilotKind.HISTORY, history?.kind)
        assertEquals("1+1", history?.detail)
        assertEquals(0L, history?.bytes)
        assertEquals(1, plan.targets.count { it.kind == AutopilotKind.HISTORY })
    }

    @Test fun freshHistoryProposesNoHistoryTarget() {
        val plan = AutopilotPlanner.build(
            CachePreview(emptyList(), false), listOf(journal(fresh)), listOf(usage(fresh, "new")), now,
        )
        assertNull(plan.historyTarget)
        assertEquals(0, plan.agedJournal)
        assertEquals(0, plan.agedUsage)
    }

    @Test fun unmeasuredCacheIsAnHonestGapNotAnEmptyClaim() {
        val plan = AutopilotPlanner.build(null, listOf(journal(aged)), emptyList(), now)
        assertFalse(plan.cacheMeasured)
        assertTrue(plan.cacheTargets.isEmpty())
        // The aged history is still proposed even though the cache could not be read.
        assertEquals(AutopilotPlanner.HISTORY_ID, plan.historyTarget?.id)
    }

    @Test fun theSameMeasurementAlwaysProposesTheSamePlan() {
        val cache = CachePreview(listOf(candidate("b.tmp", 20), candidate("a.tmp", 20)), false)
        val journal = listOf(journal(aged))
        val usage = listOf(usage(aged, "old"))
        val first = AutopilotPlanner.build(cache, journal, usage, now)
        val second = AutopilotPlanner.build(cache, journal, usage, now)
        assertEquals(first, second)
        // Equal byte counts tie-break on path so the order is stable.
        assertEquals(listOf("a.tmp", "b.tmp"), first.cacheTargets.map { it.title })
    }

    @Test fun selectedBytesCountOnlyChosenTargets() {
        val cache = CachePreview(listOf(candidate("a.tmp", 100), candidate("b.tmp", 200)), false)
        val plan = AutopilotPlanner.build(cache, emptyList(), emptyList(), now)
        assertEquals(300L, plan.selectedBytes(setOf("cache:a.tmp", "cache:b.tmp")))
        assertEquals(100L, plan.selectedBytes(setOf("cache:a.tmp")))
        assertEquals(0L, plan.selectedBytes(emptySet()))
    }

    @Test fun theLimitedFlagAndRetentionWindowAreCarriedThrough() {
        val limited = AutopilotPlanner.build(CachePreview(listOf(candidate("a.tmp", 1)), true),
            emptyList(), emptyList(), now, retentionDays = 0)
        assertTrue(limited.cacheLimited)
        assertEquals(1, limited.retentionDays)
        val windowed = AutopilotPlanner.build(CachePreview(emptyList(), false),
            listOf(journal(now - 10 * day)), emptyList(), now, retentionDays = 7)
        assertEquals(1, windowed.agedJournal)
    }

    @Test fun cacheIdsAreStableAndNamespaced() {
        assertEquals("cache:sub/dir/file.tmp", AutopilotPlanner.cacheId("sub/dir/file.tmp"))
        assertEquals("history", AutopilotPlanner.HISTORY_ID)
    }
}