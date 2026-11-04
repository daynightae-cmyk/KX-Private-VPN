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
