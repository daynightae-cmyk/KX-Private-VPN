package com.knoux.kxprivatevpn.platform.guardian

import com.knoux.kxprivatevpn.platform.network.NetworkReading
import com.knoux.kxprivatevpn.platform.repair.RepairMeasurement
import com.knoux.kxprivatevpn.platform.vpn.VpnPhase
import com.knoux.kxprivatevpn.platform.vpn.VpnState

/**
 * Deterministic, consent-preserving local triage. Unknown or stale observations
 * cannot be reported as verified healthy. This is NOT an antivirus/leak test.
 */
data class GuardianEvidence(
    val vpn: VpnState,
    val hasProfile: Boolean,
    val network: NetworkReading?,
    val storage: RepairMeasurement?,
    val visibleApps: Int?,
    val nowMillis: Long? = null,
    val networkReadAtMillis: Long? = null,
    val storageReadAtMillis: Long? = null,
    val appsReadAtMillis: Long? = null,
)

enum class GuardianPriority { ACTION, REVIEW, INFO }

enum class GuardianCode {
    IMPORT_PROFILE, START_VPN, VPN_ISSUE, VPN_TRANSITION, VERIFY_VPN_PATH,
    VPN_TRANSPORT_NOT_REPORTED, NETWORK_UNAVAILABLE, CAPTIVE_PORTAL,
    NETWORK_UNVERIFIED, NETWORK_UNKNOWN, NETWORK_STALE,
    DNS_NOT_REPORTED, STORAGE_CRITICAL, STORAGE_LOW, STORAGE_UNKNOWN, STORAGE_STALE,
    REVIEW_KX_CACHE, APPS_UNKNOWN, APPS_STALE, REVIEW_APPS, NO_ACTIONS,
}

data class GuardianRecommendation(
    val code: GuardianCode,
    val priority: GuardianPriority,
    /** Existing navigation route; the policy never mutates device state. */
    val route: String?,
)

data class GuardianReviewSummary(
    val actions: Int,
    val reviews: Int,
    val information: Int,
    val highestPriorityRoute: String?,
) {
    /** Never marketed as a device-health or protection score. */
    val hasPriorityActions: Boolean get() = actions > 0
}

object GuardianPolicy {
    const val CRITICAL_STORAGE_PERCENT = 5
    const val LOW_STORAGE_PERCENT = 10
    const val NETWORK_MAX_AGE_MILLIS = 2 * 60 * 1000L
    const val STORAGE_MAX_AGE_MILLIS = 10 * 60 * 1000L
    const val APPS_MAX_AGE_MILLIS = 15 * 60 * 1000L

    /**
     * If a time source is provided, stale/future observations are not trusted.
     * Null clock means legacy caller/test: no freshness opinion can be made.
     */
    fun fresh(now: Long?, observed: Long?, lifetime: Long): Boolean =
        now == null || (observed != null && observed > 0L && observed <= now &&
            now - observed <= lifetime)

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
                // Backend UP is not proof that Internet access works or a leak is absent.
                add(GuardianCode.VERIFY_VPN_PATH, GuardianPriority.REVIEW, "security")
            }
        }

        val networkFresh = fresh(evidence.nowMillis, evidence.networkReadAtMillis,
            NETWORK_MAX_AGE_MILLIS)
        val net = evidence.network
        if (net == null) {
            add(GuardianCode.NETWORK_UNKNOWN, GuardianPriority.REVIEW, "network")
        } else if (!networkFresh) {
            add(GuardianCode.NETWORK_STALE, GuardianPriority.REVIEW, null)
        } else {
            when {
                !net.present -> add(GuardianCode.NETWORK_UNAVAILABLE, GuardianPriority.ACTION, "network")
                net.captive -> add(GuardianCode.CAPTIVE_PORTAL, GuardianPriority.ACTION, "network")
                !net.validated ->
                    add(GuardianCode.NETWORK_UNVERIFIED, GuardianPriority.REVIEW, "network")
            }
            if (net.present && net.dns.isEmpty()) {
                // Android may omit DNS for a legitimate network: never call this a leak.
                add(GuardianCode.DNS_NOT_REPORTED, GuardianPriority.REVIEW, "network")
            }
            if (evidence.vpn.phase == VpnPhase.CONNECTED && net.present && !net.vpn) {
                // Device's *default network* may not identify an active app VPN;
                // this is a review-only discrepancy, not a leak verdict.
                add(GuardianCode.VPN_TRANSPORT_NOT_REPORTED, GuardianPriority.REVIEW, "security")
            }
        }

        val storage = evidence.storage
        val total = storage?.totalBytes
        val free = storage?.availableBytes
        when {
            storage != null && !fresh(evidence.nowMillis, evidence.storageReadAtMillis,
                STORAGE_MAX_AGE_MILLIS) ->
                add(GuardianCode.STORAGE_STALE, GuardianPriority.REVIEW, null)
            total == null || free == null || total <= 0L || free < 0L || free > total ->
                add(GuardianCode.STORAGE_UNKNOWN, GuardianPriority.REVIEW, "storage")
            free.toDouble() / total.toDouble() <= CRITICAL_STORAGE_PERCENT / 100.0 ->
                add(GuardianCode.STORAGE_CRITICAL, GuardianPriority.ACTION, "storage")
            free.toDouble() / total.toDouble() <= LOW_STORAGE_PERCENT / 100.0 ->
                add(GuardianCode.STORAGE_LOW, GuardianPriority.ACTION, "storage")
        }
        if (storage != null && fresh(evidence.nowMillis, evidence.storageReadAtMillis,
            STORAGE_MAX_AGE_MILLIS) && storage.cacheBytes != null && storage.cacheBytes > 0L) {
            // Cache bytes != guaranteed reclaimable bytes.
            add(GuardianCode.REVIEW_KX_CACHE, GuardianPriority.INFO, "cache")
        }

        when {
            evidence.visibleApps == null ->
                add(GuardianCode.APPS_UNKNOWN, GuardianPriority.REVIEW, "apps")
            !fresh(evidence.nowMillis, evidence.appsReadAtMillis, APPS_MAX_AGE_MILLIS) ->
                add(GuardianCode.APPS_STALE, GuardianPriority.REVIEW, null)
            else -> add(GuardianCode.REVIEW_APPS, GuardianPriority.INFO, "apps")
        }
        val sorted = results.sortedBy { it.priority.ordinal }
        return sorted.ifEmpty {
            listOf(GuardianRecommendation(GuardianCode.NO_ACTIONS, GuardianPriority.INFO, null))
        }
    }

    fun summary(findings: List<GuardianRecommendation>): GuardianReviewSummary =
        GuardianReviewSummary(
            actions = findings.count { it.priority == GuardianPriority.ACTION },
            reviews = findings.count { it.priority == GuardianPriority.REVIEW },
            information = findings.count { it.priority == GuardianPriority.INFO },
            highestPriorityRoute = findings.firstOrNull { it.route != null &&
                it.priority != GuardianPriority.INFO }?.route,
        )
}
