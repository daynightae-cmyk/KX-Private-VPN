package com.knoux.kxprivatevpn.platform.vpn

/** Truthful tunnel state. CONNECTED only means the WireGuard backend owns an active tunnel. */
enum class VpnPhase { IDLE, CONNECTING, CONNECTED, DISCONNECTING, ERROR }

enum class VpnIssue {
    NO_PROFILE, CONSENT_DENIED, CONFIG_UNREADABLE, CONFIG_INVALID,
    AUTHORIZATION, START_FAILED, STOP_FAILED, BACKEND_STOPPED,
}

data class VpnSession(
    val profileId: String,
    val profileName: String,
    val endpoint: String,
    val startedAtMillis: Long? = null,
    val rxBytes: Long = 0,
    val txBytes: Long = 0,
    val latestHandshakeMillis: Long? = null,
    val dnsServers: List<String> = emptyList(),
    val addresses: List<String> = emptyList(),
    val allowedIps: List<String> = emptyList(),
    val mtu: Int? = null,
    /** Measured keepalive actually written into the running config; null = none/unknown. */
    val keepaliveSeconds: Int? = null,
    val backendVersion: String? = null,
)

data class VpnState(
    val phase: VpnPhase = VpnPhase.IDLE,
    val session: VpnSession? = null,
    val issue: VpnIssue? = null,
)

data class VpnTraffic(
    val rxBytes: Long = 0,
    val txBytes: Long = 0,
    val latestHandshakeMillis: Long? = null,
)

/**
 * Pure ownership rules shared by Home, Servers and the client model.
 *
 * A session may be shown as the server in use only while this process can still own the
 * backend. `ERROR + STOP_FAILED` is owned because the backend may still be up; any other
 * `ERROR` is not, because no live tunnel was published. Selection is locked while the
 * tunnel is changing, so a tap can never rewrite the saved server mid-transition.
 */
object VpnSessionOwnership {
    fun ownsTunnel(state: VpnState): Boolean = when (state.phase) {
        VpnPhase.CONNECTING, VpnPhase.CONNECTED, VpnPhase.DISCONNECTING -> state.session != null
        VpnPhase.ERROR -> state.issue == VpnIssue.STOP_FAILED && state.session != null
        VpnPhase.IDLE -> false
    }

    fun activeProfileId(state: VpnState): String? =
        if (ownsTunnel(state)) state.session?.profileId else null

    fun selectionLocked(state: VpnState): Boolean = when (state.phase) {
        VpnPhase.CONNECTING, VpnPhase.DISCONNECTING -> true
        VpnPhase.ERROR -> state.issue == VpnIssue.STOP_FAILED
        else -> false
    }

    /** Persisting a different server while a measured tunnel is up must stop it first. */
    fun requiresStopBeforeSelect(state: VpnState, profileId: String): Boolean =
        state.phase == VpnPhase.CONNECTED && activeProfileId(state) != profileId
}
