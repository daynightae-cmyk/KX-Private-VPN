package com.knoux.kxprivatevpn.features

import android.app.Application
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.net.VpnService
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.knoux.kxprivatevpn.data.config.*
import com.knoux.kxprivatevpn.data.stats.UsageStatsStore
import com.knoux.kxprivatevpn.launch.LaunchPreferences
import com.knoux.kxprivatevpn.launch.UiPreferences
import com.knoux.kxprivatevpn.platform.apps.InstalledApp
import com.knoux.kxprivatevpn.platform.apps.InstalledApps
import com.knoux.kxprivatevpn.platform.usage.StatsRange
import com.knoux.kxprivatevpn.platform.usage.UsageAggregator
import com.knoux.kxprivatevpn.platform.usage.UsageRecorder
import com.knoux.kxprivatevpn.platform.vpn.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

enum class ClientIssue { INVALID_CONFIG, INPUT, STORE, SAVE }
private class InputUnavailable : Exception("Input unavailable")
data class ClientState(
    val loaded: Boolean = false, val busy: Boolean = false,
    val snapshot: ProfileSnapshot = ProfileSnapshot(emptyList(), null),
    val settings: UiPreferences = UiPreferences(),
    val issue: ClientIssue? = null, val importedId: String? = null,
)

/** What the currently selected WireGuard profile itself says, used only for honest previews. */
private data class AdvancedProfileBaseline(
    val exists: Boolean,
    val mtu: Int? = null,
    val keepalive: Int? = null,
    val hasDns: Boolean = false,
)

class ClientViewModel(app: Application) : AndroidViewModel(app) {
    private val store = ProfileStore(app)
    private val preferences = LaunchPreferences(app)
    private val mutable = MutableStateFlow(ClientState())
    val state: StateFlow<ClientState> = mutable.asStateFlow()
    private val splitConfigurator = SplitTunnelConfigurator(app)
    private val advancedConfigurator = AdvancedNetworkConfigurator(app)
    private val gateway = WireGuardVpnGateway(app)
    private val vpn = VpnController(
        scope = viewModelScope,
        gateway = gateway,
        loadConfig = { id -> store.config(id) },
        // Split tunneling writes the per-app lists; advanced network then rebuilds the
        // interface around them (MTU/DNS) and rewrites the peer keepalive. Both run before
        // the backend ever sees the profile.
        adjustConfig = { config -> advancedConfigurator.apply(splitConfigurator.apply(config)) },
    )
    val vpnState: StateFlow<VpnState> = vpn.state
    private val splitStore = SplitTunnelStore(app)
    private val installedApps = InstalledApps(app)
    private val splitMutable = MutableStateFlow(SplitTunnelUiState())
    val splitState: StateFlow<SplitTunnelUiState> = splitMutable.asStateFlow()
    private var appliedSelection: SplitTunnelSelection? = null
    private val networkStore = NetworkSettingsStore(app)
    private val advancedMutable = MutableStateFlow(AdvancedNetworkUiState())
    val advancedState: StateFlow<AdvancedNetworkUiState> = advancedMutable.asStateFlow()
    private var appliedAdvanced: AdvancedNetworkSettings? = null
    private val usageStore = UsageStatsStore(app)
    private val statsMutable = MutableStateFlow(StatisticsUiState())
    val statsState: StateFlow<StatisticsUiState> = statsMutable.asStateFlow()
    private val usageRecorder = UsageRecorder(
        sink = usageStore,
        elapsedMillis = SystemClock::elapsedRealtime,
        onChanged = { reloadStats() },
    )
    init {
        reload()
        viewModelScope.launch {
            // Restore the persisted hint before observing, so the first IDLE emission cannot clear it.
            val loaded = state.first { it.loaded || it.issue == ClientIssue.STORE }
            if (loaded.loaded) {
                val active = try { preferences.wasVpnActive() } catch (_: Exception) { false }
                if (active) {
                    val selected = loaded.snapshot.profiles.firstOrNull { it.id == loaded.snapshot.selectedId }
                    vpn.restore(selected)
                }
            }
            // Finish an interrupted checkpoint before live collection can overwrite it.
            usageRecorder.recoverCheckpoint()
            if (statsMutable.value.loading) reloadStats()
            var previousPhase: VpnPhase? = null
            vpn.state.collect { value ->
                try {
                    when {
                        value.phase == VpnPhase.CONNECTED -> preferences.markVpnActive(true)
                        value.phase == VpnPhase.CONNECTING || value.phase == VpnPhase.IDLE ->
                            preferences.markVpnActive(false)
                        value.phase == VpnPhase.ERROR && value.issue != VpnIssue.STOP_FAILED ->
                            preferences.markVpnActive(false)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // The session hint is advisory; a failed write must not break the tunnel.
                }
                // The WireGuard library never calls startForeground, so mirror only the
                // measured CONNECTED transition into the companion foreground service.
                // Reconnect and byte-count updates must not restart the notification.
                if (value.phase == VpnPhase.CONNECTED && previousPhase != VpnPhase.CONNECTED) {
                    VpnSessionService.start(
                        getApplication(),
                        value.session?.profileName,
                        value.session?.startedAtMillis,
                    )
                } else if (value.phase != VpnPhase.CONNECTED &&
                    previousPhase == VpnPhase.CONNECTED
                ) {
                    VpnSessionService.stop(getApplication())
                }
                syncSplitState(
                    phase = value.phase,
                    captureApplied = value.phase == VpnPhase.CONNECTING &&
                        previousPhase != VpnPhase.CONNECTING,
                )
                syncAdvancedState(
                    phase = value.phase,
                    captureApplied = value.phase == VpnPhase.CONNECTING &&
                        previousPhase != VpnPhase.CONNECTING,
                )
                usageRecorder.onVpnState(value)
                // Surface a commit/checkpoint failure on the statistics screen immediately.
                statsMutable.update { it.copy(saveFailed = usageRecorder.saveFailed.value) }
                previousPhase = value.phase
            }
        }
    }

    /**
     * Mirrors the measured tunnel phase into the split-tunnel screen without inventing state.
     * [captureApplied] is true only on the transition into CONNECTING: that is the selection
     * the backend is about to read. If a save is still in flight the rules are unknown, so the
     * captured value stays null and the screen keeps showing "reconnect required" until the
     * user actually rebuilds the tunnel.
     */
    private fun syncSplitState(phase: VpnPhase, captureApplied: Boolean = false) {
        if (captureApplied) {
            appliedSelection = if (splitMutable.value.saving) null else splitMutable.value.selection
        }
        if (phase == VpnPhase.IDLE) appliedSelection = null
        val connected = phase == VpnPhase.CONNECTED
        splitMutable.update { state ->
            val dirty = connected && appliedSelection != state.selection
            state.copy(
                connected = connected,
                dirty = dirty,
                appliedThrough = if (connected && !dirty) state.plan.throughVpnCount else 0,
                appliedBypass = if (connected && !dirty) state.plan.bypassCount else 0,
            )
        }
    }

    /**
     * Mirrors the tunnel phase into the advanced-network screen without inventing state.
     * [captureApplied] is true only on the transition into CONNECTING — the settings the
     * configurator is about to read; a save in flight keeps the snapshot null so the screen
     * keeps asking for a reconnect until a tunnel actually picked up the new values.
     */
    private fun syncAdvancedState(phase: VpnPhase, captureApplied: Boolean = false) {
        if (captureApplied) {
            appliedAdvanced = if (advancedMutable.value.saving) null
            else advancedMutable.value.settings
        }
        if (phase == VpnPhase.IDLE) appliedAdvanced = null
        val connected = phase == VpnPhase.CONNECTED
        advancedMutable.update { state ->
            state.copy(
                connected = connected,
                dirty = connected && appliedAdvanced != state.settings,
                applied = if (connected && appliedAdvanced == state.settings) appliedAdvanced
                else null,
            )
        }
    }
    fun reload() {
        if (mutable.value.busy) return
        viewModelScope.launch {
            mutable.update { it.copy(busy = true, issue = null) }
            try {
                val profiles = store.load()
                val settings = preferences.settings()
                mutable.update { it.copy(loaded = true, snapshot = profiles, settings = settings) }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutable.update { it.copy(issue = ClientIssue.STORE) } }
            finally { mutable.update { it.copy(busy = false) } }
        }
    }
    fun dismissIssue() { mutable.update { it.copy(issue = null) } }
    fun consumeImport() { mutable.update { it.copy(importedId = null) } }
    fun inputError() { mutable.update { it.copy(issue = ClientIssue.INPUT) } }
    private fun operation(action: suspend () -> Unit) {
        if (mutable.value.busy || !mutable.value.loaded) return
        mutable.update { it.copy(busy = true, issue = null) }
        viewModelScope.launch {
            try { action() }
            catch (_: TimeoutCancellationException) { mutable.update { it.copy(issue = ClientIssue.INPUT) } }
            catch (_: InputUnavailable) { mutable.update { it.copy(issue = ClientIssue.INPUT) } }
            catch (e: CancellationException) { throw e }
            catch (_: InvalidConfiguration) { mutable.update { it.copy(issue = ClientIssue.INVALID_CONFIG) } }
            catch (_: IllegalArgumentException) { mutable.update { it.copy(issue = ClientIssue.INVALID_CONFIG) } }
            catch (_: Exception) { mutable.update { it.copy(issue = ClientIssue.SAVE) } }
            finally { mutable.update { it.copy(busy = false) } }
        }
    }
    fun onboard() = operation {
        preferences.completeOnboarding()
        mutable.update { it.copy(settings = it.settings.copy(onboarded = true)) }
    }
    fun appearance(language: String, theme: String) = operation {
        preferences.appearance(language, theme)
        mutable.update { it.copy(settings = it.settings.copy(language = language, theme = theme)) }
    }
    fun select(id: String) = operation { val value = store.select(id); mutable.update { it.copy(snapshot = value) } }
    fun remove(id: String) = operation {
        if (vpn.state.value.session?.profileId == id) vpn.stopNow()
        val value = store.remove(id)
        mutable.update { it.copy(snapshot = value) }
    }
    private fun selectedProfile() = mutable.value.snapshot.let { snapshot ->
        snapshot.profiles.firstOrNull { it.id == snapshot.selectedId }
    }
    /** Returns the real Android consent intent when the platform still requires it. */
    fun prepareConnect(): Intent? {
        val selected = selectedProfile() ?: run { vpn.reportNoProfile(); return null }
        val consent = try {
            VpnService.prepare(getApplication())
        } catch (_: Exception) {
            vpn.reportStartFailed(); return null
        }
        if (consent == null) vpn.connect(selected)
        return consent
    }
    fun connectSelected() {
        selectedProfile()?.let(vpn::connect) ?: vpn.reportNoProfile()
    }
    fun consentDenied() = vpn.reportConsentDenied()
    fun disconnectVpn() = vpn.disconnect()
    fun dismissVpnIssue() = vpn.clearIssue()

    /** Explicit, VPN-bound request; no consent means no network operation. Never starts a tunnel. */
    suspend fun verifyVpnPath(url: String, approved: Boolean): VpnPathEvidence {
        val expected = vpn.state.value.session
        val port = AndroidVpnProbePort(getApplication(), gateway) {
            val current = vpn.state.value
            expected != null && current.phase == VpnPhase.CONNECTED &&
                current.session?.profileId == expected.profileId &&
                current.session?.startedAtMillis == expected.startedAtMillis
        }
        return VpnPathVerifier(port).verify(url, approved)
    }

    /** Real PackageManager + DataStore read; nothing is shown before both complete. */
    fun refreshSplitTunnel() {
        if (splitMutable.value.saving) return
        viewModelScope.launch {
            splitMutable.update { it.copy(loading = true, loadFailed = false) }
            try {
                val selection = splitStore.load()
                val apps = installedApps.list()
                splitMutable.update {
                    it.copy(
                        selection = selection,
                        installed = apps,
                        plan = splitPlan(selection, apps),
                        loading = false,
                        loadFailed = false,
                    )
                }
                syncSplitState(vpn.state.value.phase)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                splitMutable.update { it.copy(loading = false, loadFailed = true) }
            }
        }
    }

    fun setSplitMode(mode: SplitTunnelMode) = changeSplitSelection { it.copy(mode = mode) }

    fun toggleSplitApp(packageName: String) {
        val current = splitMutable.value
        if (current.loading || current.saving) return
        val nextPackages = SplitTunnelPolicy.toggle(
            mode = current.selection.mode,
            applied = current.plan.isApplied,
            packages = current.selection.packages,
            installedPackages = current.installed.mapTo(linkedSetOf()) { it.packageName },
            packageName = packageName,
        )
        changeSplitSelection { selection -> selection.copy(packages = nextPackages) }
    }

    private fun changeSplitSelection(transform: (SplitTunnelSelection) -> SplitTunnelSelection) {
        val current = splitMutable.value
        if (current.loading || current.saving) return
        val next = transform(current.selection)
        if (next == current.selection) return
        splitMutable.value = current.copy(
            selection = next,
            plan = splitPlan(next, current.installed),
            saving = true,
            saveFailed = false,
        )
        viewModelScope.launch {
            try {
                splitStore.save(next)
                splitMutable.update { it.copy(saving = false) }
                syncSplitState(vpn.state.value.phase)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Keep the persisted selection authoritative; do not pretend it was saved.
                splitMutable.value = current.copy(saveFailed = true)
            }
        }
    }

    private fun splitPlan(selection: SplitTunnelSelection, apps: List<InstalledApp>) =
        SplitTunnelPolicy.plan(selection, apps.mapTo(linkedSetOf()) { it.packageName },
            getApplication<Application>().packageName)

    /**
     * A running tunnel can be rebuilt without asking Android again unless consent was revoked.
     * Returns the real consent intent when Android demands one; the screen must launch it.
     */
    fun prepareReconnect(): Intent? {
        if (vpn.state.value.phase != VpnPhase.CONNECTED) return null
        val selected = selectedProfile() ?: run {
            vpn.reportNoProfile()
            return null
        }
        val consent = try {
            VpnService.prepare(getApplication())
        } catch (_: Exception) {
            vpn.reportStartFailed()
            return null
        }
        if (consent == null) vpn.reconnect(selected)
        return consent
    }

    fun reconnectSelected() {
        selectedProfile()?.let(vpn::reconnect)
    }

    /** Stored settings plus the selected profile's own MTU/DNS/keepalive baseline. */
    fun refreshAdvanced() {
        if (advancedMutable.value.saving) return
        viewModelScope.launch {
            advancedMutable.update { it.copy(loading = true, loadFailed = false) }
            try {
                val settings = networkStore.load()
                val baseline = profileBaseline()
                advancedMutable.update {
                    it.copy(
                        settings = settings,
                        profileMtu = baseline.mtu,
                        profileKeepalive = baseline.keepalive,
                        profileHasDns = baseline.hasDns,
                        hasProfile = baseline.exists,
                        loading = false,
                        loadFailed = false,
                    )
                }
                syncAdvancedState(vpn.state.value.phase)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                advancedMutable.update { it.copy(loading = false, loadFailed = true) }
            }
        }
    }

    fun setAdvancedMtu(value: Int?) = changeAdvancedSettings { it.copy(mtuOverride = value) }

    fun setAdvancedKeepalive(value: Int?) =
        changeAdvancedSettings { it.copy(keepaliveSeconds = value) }

    fun setAdvancedDnsMode(mode: DnsMode) = changeAdvancedSettings { it.copy(dnsMode = mode) }

    fun setAdvancedDnsHosts(hosts: List<String>) =
        changeAdvancedSettings { it.copy(customDns = hosts) }

    /** Back to "the profile decides": every override cleared, nothing invented. */
    fun resetAdvancedSettings() = changeAdvancedSettings { AdvancedNetworkSettings() }

    private fun changeAdvancedSettings(
        transform: (AdvancedNetworkSettings) -> AdvancedNetworkSettings,
    ) {
        val current = advancedMutable.value
        if (current.loading || current.saving) return
        val next = transform(current.settings)
        if (next == current.settings) return
        advancedMutable.value = current.copy(settings = next, saving = true, saveFailed = false)
        viewModelScope.launch {
            try {
                networkStore.save(next)
                advancedMutable.update { it.copy(saving = false) }
                syncAdvancedState(vpn.state.value.phase)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Keep the stored settings authoritative; do not pretend the change was saved.
                advancedMutable.value = current.copy(saveFailed = true)
            }
        }
    }

    private suspend fun profileBaseline(): AdvancedProfileBaseline {
        val profile = selectedProfile()
            ?: return AdvancedProfileBaseline(exists = false)
        return try {
            val bytes = store.config(profile.id)
            if (bytes == null) AdvancedProfileBaseline(exists = true)
            else try {
                val config = ConfigValidator.parse(bytes)
                val keepalives = config.peers
                    .mapNotNull { it.persistentKeepalive.orElse(null) }
                    .distinct()
                AdvancedProfileBaseline(
                    exists = true,
                    mtu = config.`interface`.mtu.orElse(null),
                    keepalive = keepalives.singleOrNull(),
                    hasDns = config.`interface`.dnsServers.isNotEmpty(),
                )
            } finally {
                bytes.fill(0)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // An unreadable profile only means "no baseline numbers"; settings still load.
            AdvancedProfileBaseline(exists = true)
        }
    }

    /** Reloads the measured session history; loading is shown only for an explicit refresh. */
    fun refreshStats() {
        if (statsMutable.value.clearing) return
        viewModelScope.launch {
            statsMutable.update { it.copy(loading = true, loadFailed = false) }
            reloadStats()
        }
    }

    private suspend fun reloadStats() {
        try {
            val stored = usageStore.load()
            val range = statsMutable.value.range
            statsMutable.update {
                it.copy(
                    loading = false,
                    loadFailed = false,
                    saveFailed = usageRecorder.saveFailed.value,
                    records = stored.records,
                    summary = UsageAggregator.summarize(
                        stored.records, range, System.currentTimeMillis(),
                    ),
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            statsMutable.update { it.copy(loading = false, loadFailed = true) }
        }
    }

    /** Recomputes the visible window from the already-loaded records; no new storage read. */
    fun selectStatsRange(range: StatsRange) {
        val current = statsMutable.value
        if (current.loading || current.clearing) return
        val now = System.currentTimeMillis()
        statsMutable.update {
            it.copy(range = range, summary = UsageAggregator.summarize(it.records, range, now))
        }
    }

    fun openClearStats() {
        if (!statsMutable.value.clearing) {
            statsMutable.update { it.copy(confirmClear = true, clearFailed = false) }
        }
    }

    fun dismissClearStats() {
        if (!statsMutable.value.clearing) statsMutable.update { it.copy(confirmClear = false) }
    }

    /**
     * Deletes the stored history, then rebases a live session so only time and bytes measured
     * after the clear can be recorded. The live tunnel itself is never touched.
     */
    fun confirmClearStats() {
        val current = statsMutable.value
        if (current.clearing) return
        statsMutable.update { it.copy(clearing = true, clearFailed = false) }
        viewModelScope.launch {
            try {
                usageStore.clear()
                usageRecorder.onHistoryCleared()
                val empty = UsageAggregator.summarize(
                    emptyList(), current.range, System.currentTimeMillis(),
                )
                statsMutable.update {
                    it.copy(
                        clearing = false,
                        confirmClear = false,
                        clearFailed = false,
                        records = emptyList(),
                        summary = empty,
                        saveFailed = usageRecorder.saveFailed.value,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                statsMutable.update { it.copy(clearing = false, clearFailed = true) }
            }
        }
    }

    /** Real Android/WireGuard lockdown read for the Security Center; null means unknown. */
    suspend fun lockdownEnabled(): Boolean? = vpn.lockdownEnabled()
    private suspend fun save(name: String, bytes: ByteArray) {
        val value = try { store.import(name, bytes) } finally { bytes.fill(0) }
        mutable.update { it.copy(snapshot = value, importedId = value.profiles.last().id) }
    }
    fun importText(name: String, input: String) = operation { save(name, input.toByteArray(Charsets.UTF_8)) }
    fun importFile(name: String, uri: Uri, qr: Boolean) = operation {
        val bytes = try { withTimeout(15_000) { runInterruptible(Dispatchers.IO) {
            val resolver = getApplication<Application>().contentResolver
            if (!qr) resolver.openInputStream(uri)?.use { ConfigInput.read(it) } ?: throw InvalidConfiguration()
            else {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                if (options.outWidth !in 1..12000 || options.outHeight !in 1..12000) throw InvalidConfiguration()
                var sample = 1
                while (maxOf(options.outWidth, options.outHeight) / sample > 1600) sample *= 2
                options.inSampleSize = sample; options.inJustDecodeBounds = false
                val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                    ?: throw InvalidConfiguration()
                try {
                    val pixels = IntArray(bitmap.width * bitmap.height)
                    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                    val source = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
                    MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(source))).text.toByteArray(Charsets.UTF_8)
                } catch (_: Exception) { throw InvalidConfiguration() }
                finally { bitmap.recycle() }
            }
        } } } catch (e: CancellationException) { throw e }
        catch (e: InvalidConfiguration) { throw e }
        catch (_: Exception) { throw InputUnavailable() }
        save(name, bytes)
    }
}
