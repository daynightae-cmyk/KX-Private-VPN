package com.knoux.kxprivatevpn.platform.vpn

import com.knoux.kxprivatevpn.data.config.ConfigValidator
import com.knoux.kxprivatevpn.data.config.InvalidConfiguration
import com.knoux.kxprivatevpn.data.config.Profile
import com.wireguard.android.backend.BackendException
import com.wireguard.config.Config
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private class ConfigUnavailable : Exception("Stored configuration unavailable")

/**
 * Owns the only tunnel state. It starts the backend only after Android VPN consent,
 * keeps counters honest, and never invents a connected state.
 */
class VpnController(
    private val scope: CoroutineScope,
    private val gateway: VpnGateway,
    private val loadConfig: suspend (String) -> ByteArray?,
    /**
     * Applies device-owned routing rules (per-app split tunneling) to the parsed profile
     * right before the backend sees it. Kept as a seam so the state machine stays testable.
     */
    private val adjustConfig: suspend (Config) -> Config = { it },
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val mutable = MutableStateFlow(VpnState())
    val state: StateFlow<VpnState> = mutable.asStateFlow()
    private var connecting: Job? = null
    private var monitoring: Job? = null

    fun connect(profile: Profile) {
        val current = mutable.value.phase
        if (current == VpnPhase.CONNECTING || current == VpnPhase.CONNECTED ||
            current == VpnPhase.DISCONNECTING) return
        connecting?.cancel()
        monitoring?.cancel()
        mutable.value = VpnState(VpnPhase.CONNECTING, baseSession(profile))
        connecting = scope.launch(io) {
            val config = try {
                read(profile.id)
            } catch (e: CancellationException) {
                throw e
            } catch (_: InvalidConfiguration) {
                fail(VpnIssue.CONFIG_INVALID); return@launch
            } catch (_: Exception) {
                fail(VpnIssue.CONFIG_UNREADABLE); return@launch
            }
            try {
                val version = gateway.start(config)
                val traffic = runCatching { gateway.traffic() }.getOrNull() ?: VpnTraffic()
                mutable.value = VpnState(VpnPhase.CONNECTED,
                    session(profile, config, now(), version, traffic))
                startMonitor()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                fail(mapIssue(e))
            }
        }
    }

    /**
     * Stops the measured tunnel and starts it again with the current persisted routing rules.
     * Only valid while CONNECTED; a consent revocation surfaces as AUTHORIZATION, never as a
     * silent success. The UI calls this only after it has shown the saved-changes banner.
     */
    fun reconnect(profile: Profile) {
        if (mutable.value.phase != VpnPhase.CONNECTED) return
        monitoring?.cancel()
        val current = mutable.value.session
        mutable.value = VpnState(VpnPhase.DISCONNECTING, current)
        scope.launch(io) {
            try {
                gateway.stop()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutable.value = VpnState(VpnPhase.ERROR, current, VpnIssue.STOP_FAILED)
                return@launch
            }
            mutable.value = VpnState()
            connect(profile)
        }
    }

    fun disconnect() {
        val current = mutable.value
        if (current.phase != VpnPhase.CONNECTED) return
        monitoring?.cancel()
        mutable.value = VpnState(VpnPhase.DISCONNECTING, current.session)
        scope.launch(io) {
            try {
                gateway.stop()
                mutable.value = VpnState()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutable.value = VpnState(VpnPhase.ERROR, current.session, VpnIssue.STOP_FAILED)
            }
        }
    }

    /** Called before deleting a profile; waits for the backend instead of lying about shutdown. */
    suspend fun stopNow() {
        monitoring?.cancel()
        connecting?.cancel()
        try {
            gateway.stop()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // The removal still proceeds; the UI no longer claims this profile is active.
        }
        mutable.value = VpnState()
    }

    /** Re-attach only when the backend itself confirms the tunnel is still up. */
    suspend fun restore(profile: Profile?) {
        if (profile == null || mutable.value.phase != VpnPhase.IDLE) return
        if (!runCatching { gateway.isUp() }.getOrDefault(false)) return
        val config = runCatching { read(profile.id) }.getOrNull()
        val traffic = runCatching { gateway.traffic() }.getOrNull() ?: VpnTraffic()
        mutable.value = VpnState(VpnPhase.CONNECTED, session(profile, config, null, null, traffic))
        startMonitor()
    }

    /** Official backend view of Android's system-wide lockdown. null is "could not read". */
    suspend fun lockdownEnabled(): Boolean? = runCatching { gateway.isLockdownEnabled() }.getOrNull()

    fun reportNoProfile() {
        if (mutable.value.phase == VpnPhase.IDLE || mutable.value.phase == VpnPhase.ERROR) {
            mutable.value = VpnState(VpnPhase.ERROR, null, VpnIssue.NO_PROFILE)
        }
    }

    fun reportConsentDenied() {
        if (mutable.value.phase != VpnPhase.CONNECTED) {
            mutable.value = VpnState(VpnPhase.ERROR, null, VpnIssue.CONSENT_DENIED)
        }
    }

    fun clearIssue() {
        if (mutable.value.phase == VpnPhase.ERROR) mutable.value = VpnState()
    }

    fun reportStartFailed() {
        if (mutable.value.phase == VpnPhase.IDLE || mutable.value.phase == VpnPhase.ERROR) {
            mutable.value = VpnState(VpnPhase.ERROR, null, VpnIssue.START_FAILED)
        }
    }

    private fun startMonitor() {
        monitoring?.cancel()
        monitoring = scope.launch(io) {
            while (isActive) {
                delay(STATS_INTERVAL_MS)
                if (mutable.value.phase != VpnPhase.CONNECTED) break
                try {
                    if (!gateway.isUp()) {
                        mutable.update { it.copy(phase = VpnPhase.ERROR, issue = VpnIssue.BACKEND_STOPPED) }
                        break
                    }
                    val traffic = gateway.traffic()
                    mutable.update {
                        it.copy(session = it.session?.copy(
                            rxBytes = traffic.rxBytes,
                            txBytes = traffic.txBytes,
                            latestHandshakeMillis = traffic.latestHandshakeMillis,
                        ))
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // Transient backend read failure: keep the last measured values and retry.
                }
            }
        }
    }

    private suspend fun read(profileId: String): Config {
        val bytes = loadConfig(profileId) ?: throw ConfigUnavailable()
        return try {
            adjustConfig(ConfigValidator.parse(bytes))
        } finally {
            bytes.fill(0)
        }
    }

    private fun fail(issue: VpnIssue) {
        mutable.value = VpnState(VpnPhase.ERROR, mutable.value.session, issue)
    }

    private fun mapIssue(error: Throwable): VpnIssue = when {
        error is BackendException && error.reason == BackendException.Reason.VPN_NOT_AUTHORIZED ->
            VpnIssue.AUTHORIZATION
        else -> VpnIssue.START_FAILED
    }

    private fun baseSession(profile: Profile) =
        VpnSession(profile.id, profile.name, profile.endpoint)

    private fun session(profile: Profile, config: Config?, startedAt: Long?, version: String?,
                        traffic: VpnTraffic): VpnSession {
        val iface = config?.`interface`
        return VpnSession(
            profileId = profile.id,
            profileName = profile.name,
            endpoint = profile.endpoint,
            startedAtMillis = startedAt,
            rxBytes = traffic.rxBytes,
            txBytes = traffic.txBytes,
            latestHandshakeMillis = traffic.latestHandshakeMillis,
            dnsServers = iface?.dnsServers?.mapNotNull { it.hostAddress }?.sorted().orEmpty(),
            addresses = iface?.addresses?.map { it.toString() }?.sorted().orEmpty(),
            allowedIps = config?.peers?.flatMap { it.allowedIps }?.map { it.toString() }?.distinct()?.sorted().orEmpty(),
            mtu = iface?.mtu?.orElse(null),
            backendVersion = version,
        )
    }

    private companion object {
        const val STATS_INTERVAL_MS = 1_000L
    }
}
