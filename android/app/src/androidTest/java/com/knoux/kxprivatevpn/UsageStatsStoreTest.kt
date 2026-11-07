package com.knoux.kxprivatevpn

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.data.stats.UsageStatsStore
import com.knoux.kxprivatevpn.platform.usage.UsageRecord
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
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
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var file: File
    private lateinit var job: kotlinx.coroutines.CompletableJob
    private val store get() = UsageStatsStore(dataStore)

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
    fun createTestOwnedStore() {
        // Instrumentation runs under the app UID; isolation comes from the injected UUID file,
        // not the inaccessible test-package sandbox. Never open the production DataStore.
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        file = File(context.cacheDir, "usage-test-${UUID.randomUUID()}.preferences_pb")
        job = SupervisorJob()
        dataStore = PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO)) { file }
    }

    @After
    fun closeTestOwnedStore() = runBlocking {
        job.cancelAndJoin()
        assertTrue(!file.exists() || file.delete())
    }

    @Test fun emptyStoreHasNoRecordsOrCheckpoint() = runBlocking {
        val stored = store.load()
        assertTrue(stored.records.isEmpty())
        assertNull(stored.checkpoint)
    }

    @Test fun committedRecordsAreReadBackFromANewStoreInstance() = runBlocking {
        store.commit(record(endpoint = "a", rx = 100))
        store.commit(record(started = 1_760_000_100_000, endpoint = "b", rx = 200))

        val stored = UsageStatsStore(dataStore).load()
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

    @Test fun approvedPrunePreservesConcurrentAppendAndCheckpoint() = runBlocking {
        val old = record(started = 1_600_000_000_000)
        val fresh = record(started = 1_760_000_000_000)
        store.commit(old)
        val pruning = async(Dispatchers.IO) { store.removeApproved(listOf(old)) }
        val appending = async(Dispatchers.IO) { store.commit(fresh) }
        assertEquals(1, pruning.await())
        appending.await()
        assertEquals(listOf(fresh), store.load().records)
        val checkpoint = record(duration = 42_000)
        store.saveCheckpoint(checkpoint)
        assertEquals(1, store.removeApproved(listOf(fresh)))
        assertEquals(checkpoint, store.load().checkpoint)
    }

    @Test fun approvedPruneHonorsDuplicateMultiplicityAndChangedRecords() = runBlocking {
        val old = record()
        store.commit(old); store.commit(old)
        assertEquals(1, store.removeApproved(listOf(old)))
        assertEquals(listOf(old), store.load().records)
        assertEquals(0, store.removeApproved(listOf(old.copy(rxBytes = 5))))
        assertEquals(listOf(old), store.load().records)
    }

    @Test fun corruptHistoryCannotBeSilentlyRewrittenByPruneOrCommit() = runBlocking {
        val key = stringPreferencesKey("records_v1")
        dataStore.edit { it[key] = "corrupt history" }
        for (operation in listOf<suspend () -> Unit>(
            { store.removeApproved(listOf(record())); Unit }, { store.commit(record()) },
        )) {
            try {
                operation()
                org.junit.Assert.fail("Corrupt input must be preserved")
            } catch (_: IllegalArgumentException) { }
            assertEquals("corrupt history", dataStore.data.first()[key])
        }
    }

    @Test fun recoveredFlagSurvivesPersistence() = runBlocking {
        store.commit(record(recovered = true))
        assertTrue(store.load().records.single().recovered)
    }
}
