package com.knoux.kxprivatevpn.data.stats

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.knoux.kxprivatevpn.platform.usage.StoredUsage
import com.knoux.kxprivatevpn.platform.usage.UsageCodec
import com.knoux.kxprivatevpn.platform.usage.UsageRecord
import com.knoux.kxprivatevpn.platform.usage.UsageSink
import com.knoux.kxprivatevpn.platform.usage.appendMeasured
import com.knoux.kxprivatevpn.platform.usage.sanitized
import kotlinx.coroutines.flow.first

private val Context.usageDataStore by preferencesDataStore(name = "usage-stats")

/**
 * App-private usage history in DataStore. One record per measured session; no identifiers beyond
 * the profile name/endpoint the user already saved, and no traffic content.
 *
 * [commit] appends a record and clears its checkpoint in one atomic DataStore edit, so recovery
 * can never duplicate a session. The oldest records are pruned beyond [MAX_RECORDS]; all values
 * are re-validated on every read.
 */
class UsageStatsStore(context: Context) : UsageSink {
    private val context = context.applicationContext
    private val recordsKey = stringPreferencesKey("records_v1")
    private val checkpointKey = stringPreferencesKey("checkpoint_v1")

    override suspend fun load(): StoredUsage {
        val prefs = context.usageDataStore.data.first()
        return StoredUsage(
            records = UsageCodec.decode(prefs[recordsKey]).sortedBy { it.startedAtMillis },
            checkpoint = UsageCodec.decodeCheckpoint(prefs[checkpointKey]),
        )
    }

    override suspend fun commit(record: UsageRecord) {
        val safe = requireNotNull(record.sanitized()) { "Invalid usage record" }
        context.usageDataStore.edit { prefs ->
            val next = appendMeasured(UsageCodec.decode(prefs[recordsKey]), safe, MAX_RECORDS)
            prefs[recordsKey] = UsageCodec.encode(next)
            prefs.remove(checkpointKey)
        }
    }

    override suspend fun saveCheckpoint(record: UsageRecord) {
        val safe = requireNotNull(record.sanitized()) { "Invalid usage checkpoint" }
        context.usageDataStore.edit { prefs -> prefs[checkpointKey] = UsageCodec.encodeRecord(safe) }
    }

    override suspend fun clearCheckpoint() {
        context.usageDataStore.edit { prefs -> prefs.remove(checkpointKey) }
    }

    suspend fun clear() {
        context.usageDataStore.edit { prefs ->
            prefs.remove(recordsKey)
            prefs.remove(checkpointKey)
        }
    }

    /**
     * Keeps exactly [records] and drops the rest. Used by the retention rule of the session
     * shield, which prunes the history that fell out of its window. The checkpoint is left
     * alone: an interrupted session is not history and must still be recoverable.
     */
    suspend fun saveSurvivors(records: List<UsageRecord>) {
        val safe = records.mapNotNull { it.sanitized() }
        context.usageDataStore.edit { prefs ->
            prefs[recordsKey] = UsageCodec.encode(safe.sortedBy { it.startedAtMillis })
        }
    }

    companion object {
        const val MAX_RECORDS = 2_000
    }
}
