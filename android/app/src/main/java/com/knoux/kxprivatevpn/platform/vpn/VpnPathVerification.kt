package com.knoux.kxprivatevpn.platform.vpn

import com.knoux.kxprivatevpn.platform.network.NetworkDoctor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

/** A bounded, on-demand observation, not a saved promise of protection or a DNS leak test. */
data class VpnPathSnapshot(
    val ownedTunnelUp: Boolean,
    val activeVpn: Boolean,
    val networkHandle: Long?,
    val traffic: VpnTraffic,
    val dnsConfigured: Boolean = false,
    val defaultIpv4: Boolean = false,
    val defaultIpv6: Boolean = false,
)

enum class VpnPathOutcome {
    VERIFIED, NO_OWNED_TUNNEL, NOT_ROUTED_THROUGH_VPN, NETWORK_CHANGED,
    NO_RECENT_HANDSHAKE, NO_RECEIVED_TRAFFIC, HTTP_FAILED, UNAVAILABLE,
}

data class VpnPathEvidence(
    val outcome: VpnPathOutcome,
    val httpCode: Int? = null,
    val rxDelta: Long = 0,
    val txDelta: Long = 0,
    val handshakeMillis: Long? = null,
    val dnsConfigured: Boolean = false,
    val defaultIpv4: Boolean = false,
    val defaultIpv6: Boolean = false,
)

interface VpnProbePort {
    suspend fun snapshot(): VpnPathSnapshot
    /** Must bind both DNS and HTTPS to this exact VPN network; no default-network fallback. */
    suspend fun head(networkHandle: Long, httpsUrl: String): Int
}

class VpnPathVerifier(
    private val port: VpnProbePort,
    private val now: () -> Long = System::currentTimeMillis,
) {
    suspend fun verify(httpsUrl: String, approved: Boolean): VpnPathEvidence {
        require(approved) { "Explicit approval is required" }
        NetworkDoctor.validate(httpsUrl)
        return try {
            withTimeoutOrNull(10_000) {
                val before = port.snapshot()
                when {
                    !before.ownedTunnelUp -> VpnPathEvidence(VpnPathOutcome.NO_OWNED_TUNNEL)
                    !before.activeVpn || before.networkHandle == null ->
                        VpnPathEvidence(VpnPathOutcome.NOT_ROUTED_THROUGH_VPN)
                    else -> {
                        val code = port.head(before.networkHandle, httpsUrl)
                        evaluate(before, port.snapshot(), code, now())
                    }
                }
            } ?: VpnPathEvidence(VpnPathOutcome.UNAVAILABLE)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            VpnPathEvidence(VpnPathOutcome.UNAVAILABLE)
        }
    }

    companion object {
        internal fun evaluate(
            before: VpnPathSnapshot, after: VpnPathSnapshot, code: Int, nowMillis: Long,
        ): VpnPathEvidence {
            val rx = (after.traffic.rxBytes - before.traffic.rxBytes).coerceAtLeast(0)
            val tx = (after.traffic.txBytes - before.traffic.txBytes).coerceAtLeast(0)
            val handshake = after.traffic.latestHandshakeMillis
            val outcome = when {
                !before.ownedTunnelUp || !after.ownedTunnelUp -> VpnPathOutcome.NO_OWNED_TUNNEL
                !before.activeVpn || !after.activeVpn -> VpnPathOutcome.NOT_ROUTED_THROUGH_VPN
                before.networkHandle == null || before.networkHandle != after.networkHandle -> VpnPathOutcome.NETWORK_CHANGED
                code !in 200..299 -> VpnPathOutcome.HTTP_FAILED
                handshake == null || handshake <= 0 || handshake > nowMillis ||
                    nowMillis - handshake > 180_000 -> VpnPathOutcome.NO_RECENT_HANDSHAKE
                rx <= 0 || tx <= 0 -> VpnPathOutcome.NO_RECEIVED_TRAFFIC
                else -> VpnPathOutcome.VERIFIED
            }
            return VpnPathEvidence(outcome, code, rx, tx, handshake,
                after.dnsConfigured, after.defaultIpv4, after.defaultIpv6)
        }
    }
}
