package com.knoux.kxprivatevpn.platform.guardian

import com.knoux.kxprivatevpn.platform.network.NetworkReading
import com.knoux.kxprivatevpn.platform.repair.RepairMeasurement
import com.knoux.kxprivatevpn.platform.vpn.VpnPhase
import com.knoux.kxprivatevpn.platform.vpn.VpnState

/**
 * Deterministic triage over measured device facts, not an antivirus or an AI diagnosis.
 * Unknown readings never imply "safe", and navigation never mutates device state.
 */
data class GuardianEvidence(
    val vpn: VpnState,
    val hasProfile: Boolean,
    val network: NetworkReading?,
    val storage: RepairMeasurement?,
    val visibleApps: Int?,
)

enum class GuardianPriority { ACTION, REVIEW, INFO }

enum class GuardianCode {
    IMPORT_PROFILE, START_VPN, VPN_ISSUE, VPN_TRANSITION, VERIFY_VPN_PATH,
    NETWORK_UNAVAILABLE, CAPTIVE_PORTAL, NETWORK_UNVERIFIED, NETWORK_UNKNOWN,
    DNS_NOT_REPORTED, STORAGE_LOW, STORAGE_UNKNOWN, REVIEW_KX_CACHE,
    APPS_UNKNOWN, REVIEW_APPS, NO_ACTIONS,
}

data class GuardianRecommendation(
    val code: GuardianCode,
    val priority: GuardianPriority,
    /** Existing route. Guardian cannot delete files, uninstall apps, or alter VPN settings. */
    val route: String?,
)

object GuardianPolicy {
    const val LOW_STORAGE_PERCENT = 10

    fun recommendations(evidence: GuardianEvidence): List<GuardianRecommendation> {
        val results = mutableListOf<GuardianRecommendation>()
        fun add(code: GuardianCode, priority: GuardianPriority, route: String?) {
            results += GuardianRecommendation(code, priority, route)
        }
        when (evidence.vpn.phase) {
            VpnPhase.IDLE -> if (evidence.hasProfile) {
                add(GuardianCode.START_VPN, GuardianPriority.REVIEW, "home")
            } else {
                add(GuardianCode.IMPORT_PROFILE, GuardianPriority.ACTION, "import")
            }
            VpnPhase.ERROR -> add(GuardianCode.VPN_ISSUE, GuardianPriority.ACTION, "security")
            VpnPhase.CONNECTING, VpnPhase.DISCONNECTING ->
                add(GuardianCode.VPN_TRANSITION, GuardianPriority.INFO, "session")
            VpnPhase.CONNECTED -> if (evidence.vpn.session?.latestHandshakeMillis == null) {
                add(GuardianCode.VERIFY_VPN_PATH, GuardianPriority.REVIEW, "security")
            }
        }
        val network = evidence.network
        if (network == null) {
            add(GuardianCode.NETWORK_UNKNOWN, GuardianPriority.REVIEW, "network")
        } else {
            when {
                !network.present -> add(GuardianCode.NETWORK_UNAVAILABLE, GuardianPriority.ACTION, "network")
                network.captive -> add(GuardianCode.CAPTIVE_PORTAL, GuardianPriority.ACTION, "network")
                !network.validated ->
                    add(GuardianCode.NETWORK_UNVERIFIED, GuardianPriority.REVIEW, "network")
            }
            if (network.present && network.dns.isEmpty()) {
                add(GuardianCode.DNS_NOT_REPORTED, GuardianPriority.REVIEW, "network")
            }
        }
        val storage = evidence.storage
        val total = storage?.totalBytes
        val free = storage?.availableBytes
        when {
            total == null || free == null || total <= 0L || free < 0L || free > total ->
                add(GuardianCode.STORAGE_UNKNOWN, GuardianPriority.REVIEW, "storage")
            free <= total / 100L * LOW_STORAGE_PERCENT ->
                add(GuardianCode.STORAGE_LOW, GuardianPriority.ACTION, "storage")
        }
        // Total KX cache bytes are not necessarily reclaimable, so recommend review only.
        if (storage?.cacheBytes != null && storage.cacheBytes > 0L) {
            add(GuardianCode.REVIEW_KX_CACHE, GuardianPriority.INFO, "cache")
        }
        if (evidence.visibleApps == null) {
            add(GuardianCode.APPS_UNKNOWN, GuardianPriority.REVIEW, "apps")
        } else {
            add(GuardianCode.REVIEW_APPS, GuardianPriority.INFO, "apps")
        }
        val sorted = results.sortedBy { it.priority.ordinal }
        return sorted.ifEmpty {
            listOf(GuardianRecommendation(GuardianCode.NO_ACTIONS, GuardianPriority.INFO, null))
        }
    }
}
