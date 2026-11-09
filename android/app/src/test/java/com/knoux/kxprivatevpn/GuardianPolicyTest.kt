package com.knoux.kxprivatevpn

import com.knoux.kxprivatevpn.platform.guardian.GuardianCode
import com.knoux.kxprivatevpn.platform.guardian.GuardianEvidence
import com.knoux.kxprivatevpn.platform.guardian.GuardianPolicy
import com.knoux.kxprivatevpn.platform.guardian.GuardianPriority
import com.knoux.kxprivatevpn.platform.network.NetworkReading
import com.knoux.kxprivatevpn.platform.repair.RepairMeasurement
import com.knoux.kxprivatevpn.platform.vpn.VpnIssue
import com.knoux.kxprivatevpn.platform.vpn.VpnPhase
import com.knoux.kxprivatevpn.platform.vpn.VpnSession
import com.knoux.kxprivatevpn.platform.vpn.VpnState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GuardianPolicyTest {
    private fun network(
        present: Boolean = true, validated: Boolean = true,
        captive: Boolean = false, dns: List<String> = listOf("1.1.1.1"),
    ) = NetworkReading(present, validated, captive, false, listOf("Wi-Fi"), dns)

    private fun storage(total: Long = 1_000L, free: Long = 500L, cache: Long? = 0L) =
        RepairMeasurement(total, free, cache, 0, false, null, false, 10L)

    private fun evidence(
        vpn: VpnState = VpnState(),
        profile: Boolean = false,
        network: NetworkReading? = network(),
        storage: RepairMeasurement? = storage(),
        apps: Int? = 5,
    ) = GuardianEvidence(vpn, profile, network, storage, apps)

    private fun codes(value: GuardianEvidence) =
        GuardianPolicy.recommendations(value).map { it.code }.toSet()

    @Test fun neverClaimsVpnForUnconfiguredClient() {
        val found = codes(evidence(profile = false))
        assertTrue(GuardianCode.IMPORT_PROFILE in found)
        assertFalse(GuardianCode.START_VPN in found)
    }

    @Test fun savedProfileIsNotConnected() {
        val found = codes(evidence(profile = true))
        assertTrue(GuardianCode.START_VPN in found)
        assertFalse(GuardianCode.VERIFY_VPN_PATH in found)
    }

    @Test fun connectedWithoutHandshakeRequiresReviewNotLeakClaim() {
        val session = VpnSession("id", "Imported", "example.invalid:51820")
        val list = GuardianPolicy.recommendations(evidence(
            vpn = VpnState(phase = VpnPhase.CONNECTED, session = session),
            profile = true,
        ))
        assertTrue(list.any {
            it.code == GuardianCode.VERIFY_VPN_PATH && it.priority == GuardianPriority.REVIEW
        })
    }

    @Test fun failedNetworkMeasurementIsUnknownNotOffline() {
        val found = codes(evidence(network = null))
        assertTrue(GuardianCode.NETWORK_UNKNOWN in found)
        assertFalse(GuardianCode.NETWORK_UNAVAILABLE in found)
    }

    @Test fun absentNetworkOffersRealNetworkDoctor() {
        val list = GuardianPolicy.recommendations(evidence(network = network(present = false)))
        assertTrue(list.any {
            it.code == GuardianCode.NETWORK_UNAVAILABLE &&
                it.priority == GuardianPriority.ACTION && it.route == "network"
        })
    }

    @Test fun captivePortalNeedsAttention() {
        assertTrue(GuardianCode.CAPTIVE_PORTAL in
            codes(evidence(network = network(captive = true, validated = false))))
    }

    @Test fun missingDnsDoesNotProveDnsLeak() {
        val found = codes(evidence(network = network(dns = emptyList())))
        assertTrue(GuardianCode.DNS_NOT_REPORTED in found)
        assertFalse(GuardianCode.NETWORK_UNAVAILABLE in found)
    }

    @Test fun lowStorageUsesMeasuredInternalVolume() {
        val list = GuardianPolicy.recommendations(evidence(storage = storage(1_000, 100)))
        assertEquals(GuardianPriority.ACTION,
            list.first { it.code == GuardianCode.STORAGE_LOW }.priority)
        assertFalse(GuardianCode.STORAGE_UNKNOWN in list.map { it.code })
    }

    @Test fun badStorageReadingIsUnknownNotLowStorage() {
        val found = codes(evidence(storage = storage(1_000, 2_000)))
        assertTrue(GuardianCode.STORAGE_UNKNOWN in found)
        assertFalse(GuardianCode.STORAGE_LOW in found)
    }

    @Test fun cacheOnlyOffersReviewAndNeverAssumesSavings() {
        val list = GuardianPolicy.recommendations(evidence(storage = storage(cache = 128L)))
        assertTrue(list.any {
            it.code == GuardianCode.REVIEW_KX_CACHE &&
                it.priority == GuardianPriority.INFO && it.route == "cache"
        })
    }

    @Test fun visibilityFailureCannotBecomeZeroApps() {
        assertTrue(GuardianCode.APPS_UNKNOWN in codes(evidence(apps = null)))
        assertTrue(GuardianCode.REVIEW_APPS in codes(evidence(apps = 0)))
    }

    @Test fun vpnErrorTakesPriorityOverInformationalApps() {
        val list = GuardianPolicy.recommendations(evidence(
            vpn = VpnState(phase = VpnPhase.ERROR, issue = VpnIssue.START_FAILED),
        ))
        assertEquals(GuardianCode.VPN_ISSUE, list.first().code)
        assertEquals(GuardianPriority.ACTION, list.first().priority)
    }

    @Test fun allRoutesAreExistingReviewOnlyDestinations() {
        val allowed = setOf("home", "import", "session", "security", "network", "storage", "cache", "apps")
        for (phase in VpnPhase.entries) {
            val value = GuardianPolicy.recommendations(evidence(vpn = VpnState(phase = phase)))
            assertTrue(value.all { it.route == null || it.route in allowed })
        }
    }
}
