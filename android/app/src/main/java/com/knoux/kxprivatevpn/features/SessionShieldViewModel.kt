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
import com.knoux.kxprivatevpn.platform.repair.commitApprovedMutation
import com.knoux.kxprivatevpn.platform.security.ShieldAction
import com.knoux.kxprivatevpn.platform.security.ShieldBulkRevoke
import com.knoux.kxprivatevpn.platform.security.ShieldEntry
import com.knoux.kxprivatevpn.platform.security.ShieldInventory
import com.knoux.kxprivatevpn.platform.security.ShieldKind
import com.knoux.kxprivatevpn.platform.security.ShieldReceipt
import com.knoux.kxprivatevpn.platform.security.ShieldReceiptCode
import com.knoux.kxprivatevpn.platform.security.ShieldRetention
import com.knoux.kxprivatevpn.platform.security.ShieldRevoke
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
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
) {
    /** Selection is meaningful only for the profiles that can really be revoked. */
    val selectedProfiles: List<ShieldEntry>
        get() = inventory.profiles.filter { it.removable && it.id in selected }
}

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
                mutable.update { current ->
                    current.copy(
                        loading = false,
                        inventory = snapshot,
                        // A refresh must never leave a stale selection that points at a
                        // profile which no longer exists.
                        selected = current.selected.intersect(snapshot.removableProfileIds),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutable.update {
                    it.copy(
                        loading = false,
                        inventory = ShieldInventory(failed = true),
                        selected = emptySet(),
                    )
                }
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
                serviceKey = entry.serviceKey,
                actionKey = entry.action,
                outcomeKey = entry.outcome.name,
                items = entry.items,
                bytes = entry.bytes,
            )
        }
        usage.load().records.forEach { record ->
            entries += ShieldEntry(
                id = "usage:${record.startedAtMillis}:${record.profileId}",
                kind = ShieldKind.USAGE_HISTORY,
                title = record.profileName.ifBlank { record.profileId },
                detail = record.endpoint,
                atMillis = record.startedAtMillis,
                bytes = record.totalBytes,
                durationMillis = record.durationMillis,
                recovered = record.recovered,
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

    /** Only a real, removable profile can enter the selection. */
    fun toggle(id: String) = mutable.update { current ->
        if (current.busy || current.loading || id !in current.inventory.removableProfileIds) current
        else {
            val next = if (current.selected.contains(id)) current.selected - id
            else current.selected + id
            current.copy(selected = next)
        }
    }

    /** Selects the profiles that can be revoked; history has its own explicit erase action. */
    fun selectAll() = mutable.update { current ->
        if (current.busy || current.loading) current
        else current.copy(selected = current.inventory.removableProfileIds)
    }

    fun clearSelection() = mutable.update {
        if (it.busy) it else it.copy(selected = emptySet(), confirming = null)
    }

    /** Revoking a profile removes a credential, so it always needs an explicit approval. */
    fun requestRevoke(profileId: String) = mutable.update {
        if (it.busy || it.loading || it.confirming != null) it
        else it.copy(confirming = ShieldAction.REVOKE_PROFILE, pendingProfileId = profileId)
    }

    /** A bulk revoke is offered only when at least one real profile is selected. */
    fun requestRevokeSelected() = mutable.update {
        if (it.busy || it.loading || it.confirming != null || it.selectedProfiles.isEmpty()) it
        else it.copy(confirming = ShieldAction.REVOKE_SELECTED, pendingProfileId = null)
    }

    fun requestErase() = mutable.update {
        if (it.busy || it.loading || it.confirming != null) it
        else it.copy(confirming = ShieldAction.ERASE_HISTORY)
    }

    fun dismissConfirmation() = mutable.update { it.copy(confirming = null, pendingProfileId = null) }

    /**
     * Removes the profile from the encrypted store and then verifies that the secret is really
     * gone. A store that still reports the profile is a failure, never a silent success.
     */
    fun confirmRevoke() {
        if (mutable.value.busy || mutable.value.confirming != ShieldAction.REVOKE_PROFILE) return
        val profileId = mutable.value.pendingProfileId ?: return
        mutable.update { it.copy(confirming = null, pendingProfileId = null, busy = true) }
        viewModelScope.launch {
            var verified = false
            var stillPresent = true
            try {
                profiles.remove(profileId)
                val remaining = profiles.load()
                verified = true
                stillPresent = remaining.profiles.any { it.id == profileId }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                verified = false
            }
            val removed = if (verified && !stillPresent) 1 else 0
            val receipt = ShieldReceipt(
                action = ShieldAction.REVOKE_PROFILE,
                removed = removed,
                pruned = 0,
                bytesFreed = 0L,
                atMillis = System.currentTimeMillis(),
                code = ShieldRevoke.code(verified, stillPresent),
            )
            mutable.update { it.copy(busy = false, receipt = receipt, selected = emptySet()) }
            refresh()
        }
    }

    /**
     * Revokes every selected profile and verifies the whole batch against the encrypted store.
     * A failure in one removal never hides the others: the receipt reports the verified count.
     */
    fun confirmRevokeSelected() {
        val current = mutable.value
        if (current.busy || current.confirming != ShieldAction.REVOKE_SELECTED) return
        val targets = current.inventory.selectedProfileIds(current.selected)
        mutable.update { it.copy(confirming = null, pendingProfileId = null, busy = true) }
        if (targets.isEmpty()) {
            mutable.update { it.copy(busy = false, selected = emptySet()) }
            return
        }
        viewModelScope.launch {
            var verified = false
            var removed = 0
            try {
                targets.forEach { id ->
                    try {
                        profiles.remove(id)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        // A concurrent removal is fine; the read-back below decides the truth.
                    }
                }
                val remaining = profiles.load().profiles.map { it.id }.toSet()
                verified = true
                removed = targets.count { it !in remaining }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                verified = false
            }
            val receipt = ShieldReceipt(
                action = ShieldAction.REVOKE_SELECTED,
                removed = removed,
                pruned = 0,
                bytesFreed = 0L,
                atMillis = System.currentTimeMillis(),
                code = ShieldBulkRevoke.code(targets.size, removed, verified),
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
        if (mutable.value.busy || mutable.value.confirming != ShieldAction.ERASE_HISTORY) return
        mutable.update { it.copy(confirming = null, busy = true) }
        viewModelScope.launch {
            var removed = 0
            var failed = false
            try {
                val usageRecords = usage.load().records.size
                val hintActive = preferences.wasVpnActive()
                removed += journal.clear()
                usage.clear()
                preferences.markVpnActive(false)
                removed += usageRecords
                if (hintActive) removed += 1
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
                code = if (failed) ShieldReceiptCode.ERASE_INCOMPLETE else ShieldReceiptCode.ERASED,
            )
            mutable.update { it.copy(busy = false, receipt = receipt, selected = emptySet()) }
            refresh()
        }
    }

    /** Drops the history that fell out of the retention window. Profiles are never pruned. */
    fun pruneOld(days: Int = mutable.value.retentionDays) {
        val state = mutable.value
        if (state.busy || state.loading || state.confirming != null) return
        val window = days.coerceAtLeast(1)
        mutable.update { it.copy(busy = true, retentionDays = window) }
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            var pruned = 0
            var failed = false
            val now = System.currentTimeMillis()
            try {
                val journalTargets = withContext(Dispatchers.IO) {
                    journal.load().filter { ShieldRetention.isExpired(it.atMillis, now, window) }
                }
                val usageTargets = usage.load().records.filter {
                    ShieldRetention.isExpired(it.startedAtMillis, now, window)
                }
                commitApprovedMutation(
                    mutate = { journal.removeApproved(journalTargets) },
                    onCommitted = { removed ->
                        pruned += removed
                        failed = removed != journalTargets.size
                    },
                )
                commitApprovedMutation(
                    mutate = { usage.removeApproved(usageTargets) },
                    onCommitted = { removed ->
                        pruned += removed
                        failed = failed || removed != usageTargets.size
                    },
                )
                currentCoroutineContext().ensureActive()
            } catch (e: CancellationException) {
                failed = true
                throw e
            } catch (_: Exception) {
                failed = true
            } finally {
                failed = failed || !currentCoroutineContext().isActive
                withContext(NonCancellable) {
                    val receipt = ShieldReceipt(
                        action = ShieldAction.PRUNE_OLD, removed = 0, pruned = pruned,
                        bytesFreed = 0L, atMillis = System.currentTimeMillis(),
                        code = if (failed) ShieldReceiptCode.PRUNE_INCOMPLETE
                        else ShieldReceiptCode.PRUNED,
                    )
                    mutable.update { it.copy(busy = false, receipt = receipt) }
                    if (viewModelScope.isActive) refresh()
                }
            }
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
