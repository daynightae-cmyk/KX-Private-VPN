package com.knoux.kxprivatevpn.features

import android.app.Application
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.knoux.kxprivatevpn.data.config.ProfileStore
import com.knoux.kxprivatevpn.data.stats.UsageStatsStore
import com.knoux.kxprivatevpn.launch.LaunchPreferences
import com.knoux.kxprivatevpn.platform.repair.RepairJournal
import com.knoux.kxprivatevpn.platform.repair.RepairJournalEntry
import com.knoux.kxprivatevpn.platform.security.ShieldAction
import com.knoux.kxprivatevpn.platform.security.ShieldEntry
import com.knoux.kxprivatevpn.platform.security.ShieldInventory
import com.knoux.kxprivatevpn.platform.security.ShieldKind
import com.knoux.kxprivatevpn.platform.security.ShieldReceipt
import com.knoux.kxprivatevpn.platform.security.ShieldRetention
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class SessionShieldUiState(
    val loading: Boolean = true,
    val inventory: ShieldInventory = ShieldInventory(),
    val selected: Set<String> = emptySet(),
    val confirming: ShieldAction? = null,
    val pendingProfileId: String? = null,
    val busy: Boolean = false,
    val receipt: ShieldReceipt? = null,
    val retentionDays: Int = ShieldRetention.DEFAULT_DAYS,
    val message: String? = null,
)

/**
 * KX Session Shield.
 *
 * The shield lists what KX itself keeps and lets the person revoke or erase it. A profile is
 * removed from the encrypted store, so the private key it carried is gone with it; the history
 * is erased from the app's own files. Nothing outside KX's own storage is read or written, and
 * the shield never claims to end another application's session.
 */
class SessionShieldViewModel(app: Application) : AndroidViewModel(app) {
    private val profiles = ProfileStore(app)
    private val usage = UsageStatsStore(app)
    private val preferences = LaunchPreferences(app)
    private val journal = RepairJournal(File(app.noBackupFilesDir, "repair-journal.json"))
    private val mutable = MutableStateFlow(SessionShieldUiState())
    val state: StateFlow<SessionShieldUiState> = mutable.asStateFlow()

    init {
        refresh()
    }

    /** Reads every KX owned item. The result is a snapshot of what KX holds right now. */
    fun refresh() {
        mutable.update { it.copy(loading = true) }
        viewModelScope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) { loadInventory() }
                mutable.update { it.copy(loading = false, inventory = snapshot) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutable.update { it.copy(loading = false, inventory = ShieldInventory(failed = true)) }
            }
        }
    }

    private suspend fun loadInventory(): ShieldInventory {
        val entries = mutableListOf<ShieldEntry>()
        val snapshot = profiles.load()
        snapshot.profiles.forEach { profile ->
            entries += ShieldEntry(
                id = "profile:${profile.id}",
                kind = ShieldKind.PROFILE,
                title = profile.name,
                detail = profile.endpoint,
                atMillis = 0,
                holdsSecret = true,
                removable = true,
                selected = profile.id == snapshot.selectedId,
            )
        }
        journal.load().forEach { entry ->
            entries += ShieldEntry(
                id = "repair:${entry.atMillis}:${entry.serviceKey}",
                kind = ShieldKind.REPAIR_HISTORY,
                title = entry.serviceKey,
                detail = "${entry.action} · ${entry.outcome.name.lowercase()} · ${entry.items}",
                atMillis = entry.atMillis,
            )
        }
        usage.load().records.forEach { record ->
            entries += ShieldEntry(
                id = "usage:${record.startedAtMillis}:${record.profileId}",
                kind = ShieldKind.USAGE_HISTORY,
                title = record.profileName,
                detail = "${record.endpoint} · ${record.rxBytes + record.txBytes} B",
                atMillis = record.startedAtMillis,
            )
        }
        if (preferences.wasVpnActive()) {
            entries += ShieldEntry(
                id = "hint:vpn-active",
                kind = ShieldKind.SESSION_HINT,
                title = "vpn_session_active",
                detail = "advisory_hint_only",
                atMillis = 0,
                removable = true,
            )
        }
        return ShieldInventory(entries = entries, loadedAtMillis = System.currentTimeMillis())
    }

    fun toggle(id: String) = mutable.update { current ->
        val next = if (current.selected.contains(id)) current.selected - id else current.selected + id
        current.copy(selected = next)
    }

    fun selectAll() = mutable.update { current ->
        current.copy(selected = current.inventory.entries.filter { it.removable }.map { it.id }.toSet())
    }

    fun clearSelection() = mutable.update { it.copy(selected = emptySet(), confirming = null) }

    /** Revoking a profile removes a credential, so it always needs an explicit approval. */
    fun requestRevoke(profileId: String) = mutable.update {
        it.copy(confirming = ShieldAction.REVOKE_PROFILE, pendingProfileId = profileId)
    }

    fun requestErase() = mutable.update { it.copy(confirming = ShieldAction.ERASE_HISTORY) }

    fun dismissConfirmation() = mutable.update { it.copy(confirming = null, pendingProfileId = null) }

    /**
     * Removes the profile from the encrypted store and then verifies that the secret is really
     * gone. A store that still reports the profile is a failure, never a silent success.
     */
    fun confirmRevoke() {
        val profileId = mutable.value.pendingProfileId ?: return
        mutable.update { it.copy(confirming = null, pendingProfileId = null, busy = true) }
        viewModelScope.launch {
            var removed = 0
            try {
                profiles.remove(profileId)
                val remaining = profiles.load()
                removed = if (remaining.profiles.none { it.id == profileId }) 1 else 0
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                removed = 0
            }
            val receipt = ShieldReceipt(
                action = ShieldAction.REVOKE_PROFILE,
                removed = removed,
                pruned = 0,
                bytesFreed = 0L,
                atMillis = System.currentTimeMillis(),
                detail = if (removed == 1) "secret_removed" else "secret_still_present",
            )
            mutable.update { it.copy(busy = false, receipt = receipt, selected = emptySet()) }
            refresh()
        }
    }

    /**
     * Erases the history KX keeps. Only KX's own journal, usage store and session hint are
     * touched. The receipt counts what was really removed.
     */
    fun confirmErase() {
        mutable.update { it.copy(confirming = null, busy = true) }
        viewModelScope.launch {
            var removed = 0
            var failed = false
            try {
                removed += journal.clear()
                usage.clear()
                preferences.markVpnActive(false)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                failed = true
            }
            val receipt = ShieldReceipt(
                action = ShieldAction.ERASE_HISTORY,
                removed = removed,
                pruned = 0,
                bytesFreed = 0L,
                atMillis = System.currentTimeMillis(),
                detail = if (failed) "erase_incomplete" else "erased",
            )
            mutable.update { it.copy(busy = false, receipt = receipt, selected = emptySet()) }
            refresh()
        }
    }

    /** Drops the history that fell out of the retention window. Profiles are never pruned. */
    fun pruneOld(days: Int = mutable.value.retentionDays) {
        val window = days.coerceAtLeast(1)
        mutable.update { it.copy(busy = true, retentionDays = window) }
        viewModelScope.launch {
            var pruned = 0
            var failed = false
            try {
                val now = System.currentTimeMillis()
                val kept = mutableListOf<RepairJournalEntry>()
                journal.load().forEach { entry ->
                    if (ShieldRetention.isExpired(entry.atMillis, now, window)) pruned++ else kept += entry
                }
                journal.replace(kept)
                val stored = usage.load()
                val expired = stored.records.filter { ShieldRetention.isExpired(it.startedAtMillis, now, window) }
                pruned += expired.size
                if (expired.isNotEmpty()) {
                    val survivors = stored.records.filterNot { it in expired }
                    usage.saveSurvivors(survivors)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                failed = true
            }
            val receipt = ShieldReceipt(
                action = ShieldAction.PRUNE_OLD,
                removed = 0,
                pruned = pruned,
                bytesFreed = 0L,
                atMillis = System.currentTimeMillis(),
                detail = if (failed) "prune_incomplete" else "pruned",
            )
            mutable.update { it.copy(busy = false, receipt = receipt) }
            refresh()
        }
    }

    /** The system screen where a person withdraws the VPN consent themselves. */
    fun vpnSettingsIntent(): Intent = Intent(Settings.ACTION_VPN_SETTINGS)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** The system screen that shows every app with a VPN consent, KX included. */
    fun appInfoIntent(): Intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        .setData(android.net.Uri.fromParts("package", getApplication<Application>().packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
