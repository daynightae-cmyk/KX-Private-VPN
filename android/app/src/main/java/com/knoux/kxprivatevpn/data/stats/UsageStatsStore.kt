package com.knoux.kxprivatevpn.data.stats

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.knoux.kxprivatevpn.platform.repair.survivorsAfterApproval
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
class UsageStatsStore(private val dataStore: DataStore<Preferences>) : UsageSink {
    constructor(context: Context) : this(context.applicationContext.usageDataStore)
    private val recordsKey = stringPreferencesKey("records_v1")
    private val checkpointKey = stringPreferencesKey("checkpoint_v1")

    override suspend fun load(): StoredUsage {
        val prefs = dataStore.data.first()
        return StoredUsage(
            records = UsageCodec.decode(prefs[recordsKey]).sortedBy { it.startedAtMillis },
            checkpoint = UsageCodec.decodeCheckpoint(prefs[checkpointKey]),
        )
    }

    override suspend fun commit(record: UsageRecord) {
        val safe = requireNotNull(record.sanitized()) { "Invalid usage record" }
        dataStore.edit { prefs ->
            val next = appendMeasured(UsageMutationCodec.decode(prefs[recordsKey]), safe, MAX_RECORDS)
            prefs[recordsKey] = UsageCodec.encode(next)
            prefs.remove(checkpointKey)
        }
    }

    override suspend fun saveCheckpoint(record: UsageRecord) {
        val safe = requireNotNull(record.sanitized()) { "Invalid usage checkpoint" }
        dataStore.edit { prefs -> prefs[checkpointKey] = UsageCodec.encodeRecord(safe) }
    }

    override suspend fun clearCheckpoint() {
        dataStore.edit { prefs -> prefs.remove(checkpointKey) }
    }

    suspend fun clear() {
        dataStore.edit { prefs ->
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
        dataStore.edit { prefs ->
            prefs[recordsKey] = UsageCodec.encode(safe.sortedBy { it.startedAtMillis })
        }
    }

    /**
     * Atomically subtracts the approved multiset from the latest records. A concurrent commit is
     * never replaced by a stale survivor list; changed records and the exact checkpoint survive.
     * edit returns the committed Preferences only after persistence succeeds.
     */
    suspend fun removeApproved(approved: List<UsageRecord>): Int {
        require(approved.all { it.sanitized() == it }) { "Invalid approved usage record" }
        var removed = 0
        var expectedRecords: String? = null
        var expectedCheckpoint: String? = null
        val committed = dataStore.edit { prefs ->
            val current = UsageMutationCodec.decode(prefs[recordsKey])
            val kept = survivorsAfterApproval(current, approved)
            removed = current.size - kept.size
            if (removed > 0) prefs[recordsKey] = UsageCodec.encode(kept)
            expectedRecords = prefs[recordsKey]
            expectedCheckpoint = prefs[checkpointKey]
        }
        check(committed[recordsKey] == expectedRecords && committed[checkpointKey] == expectedCheckpoint) {
            "Usage removal not verified"
        }
        return removed
    }

    companion object {
        const val MAX_RECORDS = 2_000
    }
}

/** Mutation must not silently discard lines/fields that the DISPLAY codec intentionally ignores. */
internal object UsageMutationCodec {
    fun decode(raw: String?): List<UsageRecord> {
        if (raw == null) return emptyList()
        val records = UsageCodec.decode(raw)
        require(UsageCodec.encode(records) == raw) { "Invalid usage history; original retained" }
        return records
    }
}
