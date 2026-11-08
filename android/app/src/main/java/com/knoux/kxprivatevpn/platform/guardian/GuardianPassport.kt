package com.knoux.kxprivatevpn.platform.guardian

import com.knoux.kxprivatevpn.platform.vpn.VpnPhase

/**
 * Explicitly user-triggered, human-readable diagnostic summary.
 * Contains NO profile names/endpoints, VPN keys, DNS/IP addresses, package names,
 * file paths, per-app history or user identifiers. Caller chooses whether to copy it.
 */
object GuardianPassport {
    fun render(
        evidence: GuardianEvidence,
        findings: List<GuardianRecommendation> = GuardianPolicy.recommendations(evidence),
    ): String {
        val net = evidence.network
        val storage = evidence.storage
        val summary = GuardianPolicy.summary(findings)
        val vpn = evidence.vpn
        fun flag(value: Boolean?): String = when (value) {
            true -> "yes"
            false -> "no"
            null -> "unknown"
        }
        val networkFresh = net != null && GuardianPolicy.fresh(evidence.nowMillis,
            evidence.networkReadAtMillis, GuardianPolicy.NETWORK_MAX_AGE_MILLIS)
        val storageFresh = storage != null && GuardianPolicy.fresh(evidence.nowMillis,
            evidence.storageReadAtMillis, GuardianPolicy.STORAGE_MAX_AGE_MILLIS)
        val appsFresh = evidence.visibleApps != null && GuardianPolicy.fresh(
            evidence.nowMillis, evidence.appsReadAtMillis, GuardianPolicy.APPS_MAX_AGE_MILLIS)

        return buildString {
            appendLine("KX GUARDIAN — LOCAL EVIDENCE PASSPORT")
            appendLine("Generated at (Unix ms): ${evidence.nowMillis ?: "unknown"}")
            appendLine("Status: observation only; not proof of privacy, safety or device health.")
            appendLine("VPN phase: ${vpn.phase.name}")
            appendLine("WireGuard session reported: ${flag(vpn.phase == VpnPhase.CONNECTED && vpn.session != null)}")
            appendLine("Handshake timestamp available: ${flag(vpn.session?.latestHandshakeMillis?.let { it > 0L })}")
            appendLine("Network reading: ${if (net == null) "unknown" else if (networkFresh) "current" else "stale"}")
            appendLine("Network present: ${flag(if (networkFresh) net?.present else null)}")
            appendLine("Android network validated: ${flag(if (networkFresh) net?.validated else null)}")
            appendLine("Captive portal reported: ${flag(if (networkFresh) net?.captive else null)}")
            appendLine("Android VPN transport reported: ${flag(if (networkFresh) net?.vpn else null)}")
            appendLine("DNS server count reported: ${if (networkFresh) net?.dns?.size ?: "unknown" else "unknown / stale"}")
            appendLine("Internal storage reading: ${if (storage == null) "unknown" else if (storageFresh) "current" else "stale"}")
            appendLine("Internal free bytes: ${if (storageFresh) storage?.availableBytes ?: "unknown" else "unknown / stale"}")
            appendLine("KX cache bytes: ${if (storageFresh) storage?.cacheBytes ?: "unknown" else "unknown / stale"}")
            appendLine("Visible launcher apps reading: ${if (evidence.visibleApps == null) "unknown" else if (appsFresh) "current" else "stale"}")
            appendLine("Visible launcher apps count: ${if (appsFresh) evidence.visibleApps else "unknown / stale"}")
            appendLine("Priority actions: ${summary.actions}; reviews: ${summary.reviews}; informational: ${summary.information}")
            appendLine("Finding codes: ${findings.joinToString(",") { it.code.name }}")
            appendLine("No VPN secrets, DNS addresses, app names, or file paths included.")
        }
    }
}
