package com.knoux.kxprivatevpn

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
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
