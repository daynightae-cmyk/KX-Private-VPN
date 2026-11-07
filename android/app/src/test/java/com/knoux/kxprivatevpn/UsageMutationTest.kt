package com.knoux.kxprivatevpn

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.knoux.kxprivatevpn.platform.repair.AutopilotReceipt
import com.knoux.kxprivatevpn.platform.repair.commitApprovedMutation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import java.nio.file.Files
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.knoux.kxprivatevpn.data.stats.UsageMutationCodec
import com.knoux.kxprivatevpn.data.stats.UsageStatsStore
import com.knoux.kxprivatevpn.platform.usage.UsageCodec
import com.knoux.kxprivatevpn.platform.usage.UsageRecord
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class UsageMutationTest {
    private val record = UsageRecord("fixture", "Fixture", "192.0.2.1:51820", 1_000, 100, 2, 3)

    @Test fun mutationCodecRejectsUnknownOrCorruptLines() {
        val valid = UsageCodec.encode(listOf(record))
        assertEquals(listOf(record), UsageMutationCodec.decode(valid))
        for (raw in listOf("", "not history", valid + "\ncorrupt", valid.replace("\t2\t3", "\t-2\t3"))) {
            try {
                UsageMutationCodec.decode(raw)
                fail("Corruption cannot silently drop a stored record")
            } catch (_: IllegalArgumentException) { }
        }
    }

    @Test fun actualCancellationDuringAndBetweenDurableCommitsKeepsExactCounts() = runTest {
        for (point in listOf("first", "second", "between")) {
            val directory = Files.createTempDirectory("kx-usage-cancellation-").toFile()
            val storeJob = SupervisorJob()
            val file = java.io.File(directory, "test.preferences_pb")
            val dataStore = PreferenceDataStoreFactory.create(
                scope = CoroutineScope(storeJob + Dispatchers.IO), produceFile = { file },
            )
            val store = UsageStatsStore(dataStore)
            try {
                val second = record.copy(startedAtMillis = 2_000)
                val fresh = record.copy(startedAtMillis = 3_000)
                val checkpoint = record.copy(startedAtMillis = 4_000)
                listOf(record, second, fresh).forEach { store.commit(it) }
                store.saveCheckpoint(checkpoint)
                val reached = CompletableDeferred<Unit>()
                val resume = CompletableDeferred<Unit>()
                var reported: AutopilotReceipt? = null
                val attempted = mutableListOf<Int>()
                val operation = launch {
                    val parent = currentCoroutineContext()[Job]!!
                    var count = 0
                    try {
                        listOf(record, second).forEachIndexed { index, approved ->
                            commitApprovedMutation(
                                dispatcher = StandardTestDispatcher(testScheduler),
                                mutate = {
                                    attempted += index
                                    val removed = store.removeApproved(listOf(approved))
                                    // Actual DataStore commit completed, but its count has not returned.
                                    if ((point == "first" && index == 0) || (point == "second" && index == 1)) {
                                        reached.complete(Unit)
                                        resume.await()
                                    }
                                    removed
                                },
                                onCommitted = {
                                    count += it
                                    if (point == "between" && index == 0) {
                                        parent.cancel()
                                        reached.complete(Unit)
                                    }
                                },
                            )
                        }
                    } finally {
                        val cancelled = !currentCoroutineContext().isActive
                        reported = AutopilotReceipt(0, count, 0, emptyList(), emptyList(),
                            complete = !cancelled, cancelled = cancelled, atMillis = 5_000,
                            detail = if (cancelled) "cancelled" else "complete")
                    }
                }
                reached.await()
                operation.cancel() // Cancel a real running Job, not an injected exception.
                resume.complete(Unit)
                operation.join()
                val removed = if (point == "second") 2 else 1
                assertEquals((0 until removed).toList(), attempted)
                assertEquals(removed, reported!!.pruned)
                assertTrue(reported!!.cancelled)
                assertFalse(reported!!.complete)
                val persisted = store.load()
                assertEquals(if (removed == 2) listOf(fresh) else listOf(second, fresh), persisted.records)
                assertEquals(checkpoint, persisted.checkpoint)
                assertTrue("Real preference bytes must exist", file.length() > 0)
            } finally {
                storeJob.cancelAndJoin()
                assertTrue(directory.deleteRecursively()) // Only this test-created temporary directory.
            }
        }
    }

    @Test fun failedOrCancelledCommitNeverReturnsARemovalCount() = runTest {
        val original = preferencesOf(stringPreferencesKey("records_v1") to UsageCodec.encode(listOf(record)))
        for (failure in listOf(IOException("test write failed"), CancellationException("test cancellation"))) {
            val persistence = object : DataStore<Preferences> {
                override val data = flowOf(original)
                override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
                    transform(original)
                    throw failure
                }
            }
            try {
                UsageStatsStore(persistence).removeApproved(listOf(record))
                fail("No result may be returned before a successful commit")
            } catch (caught: Exception) {
                assertSame(failure, caught)
            }
            assertEquals(listOf(record), UsageStatsStore(persistence).load().records)
        }
    }
}
