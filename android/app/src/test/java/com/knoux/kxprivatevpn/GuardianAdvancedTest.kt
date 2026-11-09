package com.knoux.kxprivatevpn

import com.knoux.kxprivatevpn.platform.guardian.GuardianCode
import com.knoux.kxprivatevpn.platform.guardian.GuardianEvidence
import com.knoux.kxprivatevpn.platform.guardian.GuardianPassport
import com.knoux.kxprivatevpn.platform.guardian.GuardianPolicy
import com.knoux.kxprivatevpn.platform.guardian.GuardianPriority
import com.knoux.kxprivatevpn.platform.network.NetworkReading
import com.knoux.kxprivatevpn.platform.repair.RepairMeasurement
import com.knoux.kxprivatevpn.platform.vpn.VpnPhase
import com.knoux.kxprivatevpn.platform.vpn.VpnSession
import com.knoux.kxprivatevpn.platform.vpn.VpnState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GuardianAdvancedTest {
    private val now = 1_700_000_000_000L
    private val network = NetworkReading(
        present = true, validated = true, captive = false, vpn = false,
        transports = listOf("Wi-Fi"), dns = listOf("private.dns.test"),
    )
    private val storage = RepairMeasurement(10_000L, 900L, 200L, 4, false, null, false, now)
    private fun evidence(
        net: NetworkReading? = network,
        stored: RepairMeasurement? = storage,
        apps: Int? = 12,
        networkAt: Long? = now,
        storageAt: Long? = now,
        appsAt: Long? = now,
        vpn: VpnState = VpnState(),
    ) = GuardianEvidence(
        vpn = vpn, hasProfile = false, network = net, storage = stored,
        visibleApps = apps, nowMillis = now, networkReadAtMillis = networkAt,
        storageReadAtMillis = storageAt, appsReadAtMillis = appsAt,
    )
    private fun codes(e: GuardianEvidence) = GuardianPolicy.recommendations(e).map { it.code }.toSet()

    @Test fun expiredNetworkDoesNotProposeOfflineOrDnsConclusions() {
        val old = network.copy(present = false, dns = emptyList())
        val found = codes(evidence(net = old, networkAt = now - GuardianPolicy.NETWORK_MAX_AGE_MILLIS - 1))
        assertTrue(GuardianCode.NETWORK_STALE in found)
        assertFalse(GuardianCode.NETWORK_UNAVAILABLE in found)
        assertFalse(GuardianCode.DNS_NOT_REPORTED in found)
    }

    @Test fun expiredStorageDoesNotOfferMisleadingCleanup() {
        val found = codes(evidence(storageAt = now - GuardianPolicy.STORAGE_MAX_AGE_MILLIS - 1))
        assertTrue(GuardianCode.STORAGE_STALE in found)
        assertFalse(GuardianCode.STORAGE_LOW in found)
        assertFalse(GuardianCode.REVIEW_KX_CACHE in found)
    }

    @Test fun expiredAppInventoryDoesNotClaimCurrentCount() {
        val found = codes(evidence(appsAt = now - GuardianPolicy.APPS_MAX_AGE_MILLIS - 1))
        assertTrue(GuardianCode.APPS_STALE in found)
        assertFalse(GuardianCode.REVIEW_APPS in found)
    }

    @Test fun unknownAndFutureTimestampsFailClosed() {
        val found = codes(evidence(networkAt = null, storageAt = now + 1, appsAt = 0))
        assertTrue(GuardianCode.NETWORK_STALE in found)
        assertTrue(GuardianCode.STORAGE_STALE in found)
        assertTrue(GuardianCode.APPS_STALE in found)
    }

    @Test fun networkTimestampUpdateDoesNotRefreshStorage() {
        val found = codes(evidence(networkAt = now, storageAt = now - 700_000L))
        assertFalse(GuardianCode.NETWORK_STALE in found)
        assertTrue(GuardianCode.STORAGE_STALE in found)
    }

    @Test fun storageBelowFivePercentHasCriticalRecommendation() {
        val found = codes(evidence(stored = storage.copy(availableBytes = 500L)))
        assertTrue(GuardianCode.STORAGE_CRITICAL in found)
        assertFalse(GuardianCode.STORAGE_LOW in found)
    }

    @Test fun vpnTransportDisagreementIsReviewNotLeakClaim() {
        val vpn = VpnState(
            phase = VpnPhase.CONNECTED,
            session = VpnSession("id", "Secret VPN", "secret-endpoint.example:51820"),
        )
        val items = GuardianPolicy.recommendations(evidence(vpn = vpn))
        assertTrue(items.any { it.code == GuardianCode.VPN_TRANSPORT_NOT_REPORTED &&
            it.priority == GuardianPriority.REVIEW && it.route == "security" })
    }

    @Test fun topActionIsExistingReviewRoute() {
        val items = GuardianPolicy.recommendations(evidence())
        val summary = GuardianPolicy.summary(items)
        assertEquals("import", summary.highestPriorityRoute)
        assertTrue(summary.actions >= 1)
        assertTrue(summary.reviews >= 0)
    }

    @Test fun noActionRouteWhenOnlyInformationalFindingsRemain() {
        val findings = listOf(com.knoux.kxprivatevpn.platform.guardian.GuardianRecommendation(
            GuardianCode.REVIEW_APPS, GuardianPriority.INFO, "apps",
        ))
        assertNull(GuardianPolicy.summary(findings).highestPriorityRoute)
    }

    @Test fun exportedPassportCannotRevealEndpointProfileDnsOrApps() {
        val privateSession = VpnSession(
            profileId = "private-profile-id-123",
            profileName = "Private Profile Secret",
            endpoint = "my-private-server.example:51820",
            latestHandshakeMillis = now - 5000L,
        )
        val secretNetwork = network.copy(dns = listOf("private.dns.test", "8.8.4.4"))
        val privateEvidence = evidence(net = secretNetwork,
            vpn = VpnState(VpnPhase.CONNECTED, privateSession))
        val report = GuardianPassport.render(privateEvidence)
        assertTrue(report.contains("KX GUARDIAN"))
        assertTrue(report.contains("DNS server count reported: 2"))
        for (secret in listOf("Private Profile Secret", "private-profile-id-123",
            "my-private-server.example", "private.dns.test", "8.8.4.4")) {
            assertFalse("Leaked sensitive value: $secret", report.contains(secret))
        }
    }

    @Test fun exportedPassportMarksStaleMeasurementsUnknown() {
        val report = GuardianPassport.render(evidence(networkAt = now - 300_000L,
            storageAt = now - 900_000L, appsAt = now - 1_100_000L))
        assertTrue(report.contains("Network reading: stale"))
        assertTrue(report.contains("Internal storage reading: stale"))
        assertTrue(report.contains("Visible launcher apps reading: stale"))
        assertTrue(report.contains("unknown / stale"))
    }
}
