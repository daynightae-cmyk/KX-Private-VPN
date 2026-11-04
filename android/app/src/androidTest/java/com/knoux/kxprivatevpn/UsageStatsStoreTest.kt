package com.knoux.kxprivatevpn

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.data.stats.UsageStatsStore
import com.knoux.kxprivatevpn.platform.usage.UsageRecord
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The persisted history must survive restarts, validate every value on read, and clear both the
 * records and an unfinished checkpoint atomically.
 */
class UsageStatsStoreTest {
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val store get() = UsageStatsStore(context)

    private fun record(
        started: Long = 1_760_000_000_000,
        duration: Long = 60_000,
        rx: Long = 1_000,
        tx: Long = 500,
        endpoint: String = "vpn.example.com:51820",
        recovered: Boolean = false,
    ) = UsageRecord(
        profileId = "profile-1",
        profileName = "Home",
        endpoint = endpoint,
        startedAtMillis = started,
        durationMillis = duration,
        rxBytes = rx,
        txBytes = tx,
        recovered = recovered,
    )

    @Before
    fun clearBefore() = runBlocking { store.clear() }

    @After
    fun clearAfter() = runBlocking { store.clear() }

    @Test fun emptyStoreHasNoRecordsOrCheckpoint() = runBlocking {
        val stored = store.load()
        assertTrue(stored.records.isEmpty())
        assertNull(stored.checkpoint)
    }

    @Test fun committedRecordsAreReadBackFromANewStoreInstance() = runBlocking {
        store.commit(record(endpoint = "a", rx = 100))
        store.commit(record(started = 1_760_000_100_000, endpoint = "b", rx = 200))

        val stored = UsageStatsStore(context).load()
        assertEquals(2, stored.records.size)
        assertEquals(listOf("a", "b"), stored.records.map { it.endpoint })
        assertEquals(100L, stored.records[0].rxBytes)
        assertNull(stored.checkpoint)
    }

    @Test fun recordsStaySortedByStartEvenIfCommittedOutOfOrder() = runBlocking {
        store.commit(record(started = 1_760_000_200_000, endpoint = "later"))
        store.commit(record(started = 1_760_000_000_000, endpoint = "earlier"))

        assertEquals(
            listOf("earlier", "later"),
            store.load().records.map { it.endpoint },
        )
    }

    @Test fun checkpointRoundTripsAndIsClearedByACommittedRecord() = runBlocking {
        store.saveCheckpoint(record(duration = 15_000, rx = 700))
        val checkpoint = store.load().checkpoint
        assertNotNull(checkpoint)
        assertEquals(15_000L, checkpoint?.durationMillis)
        assertEquals(700L, checkpoint?.rxBytes)

        store.commit(record(started = 1_760_000_100_000, duration = 90_000, rx = 9_000))
        val stored = store.load()
        assertEquals(1, stored.records.size)
        assertNull("A committed session must never be recoverable twice", stored.checkpoint)
    }

    @Test fun clearRemovesRecordsAndAnOpenCheckpoint() = runBlocking {
        store.commit(record())
        store.saveCheckpoint(record(duration = 5_000))

        store.clear()

        val stored = store.load()
        assertTrue(stored.records.isEmpty())
        assertNull(stored.checkpoint)
    }

    @Test fun invalidRecordIsRejectedAndTheStoreIsUnchanged() = runBlocking {
        store.commit(record(endpoint = "valid"))
        try {
            store.commit(record(rx = -1))
            org.junit.Assert.fail("Negative counters are corruption, not a measurement")
        } catch (_: IllegalArgumentException) {
        }
        assertEquals(listOf("valid"), store.load().records.map { it.endpoint })
    }

    @Test fun recoveredFlagSurvivesPersistence() = runBlocking {
        store.commit(record(recovered = true))
        assertTrue(store.load().records.single().recovered)
    }
}
