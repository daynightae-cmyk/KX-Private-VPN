package com.knoux.kxprivatevpn

import com.knoux.kxprivatevpn.platform.security.CheckStatus
import com.knoux.kxprivatevpn.platform.security.MissingCheck
import com.knoux.kxprivatevpn.platform.security.ProtectionLevel
import com.knoux.kxprivatevpn.platform.security.SecurityFacts
import com.knoux.kxprivatevpn.platform.security.SecurityPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityPolicyTest {
    private val pkg = "com.knoux.kxprivatevpn"

    private fun connected(
        handshake: Long? = 1_700_000_000_000,
        link: Boolean = true,
        dns: List<String> = listOf("10.7.0.1"),
        routes: List<String> = listOf("0.0.0.0/0", "::/0"),
        alwaysOnApp: String? = pkg,
        lockdown: Boolean? = true,
    ) = SecurityFacts(
        tunnelConnected = true,
        androidLinkPresent = link,
        handshakeMillis = handshake,
        dnsServers = dns,
        allowedIps = routes,
        alwaysOnApp = alwaysOnApp,
        appPackage = pkg,
        systemLockdown = lockdown,
        consentGranted = true,
    )

    @Test fun disconnectedFactsNeverClaimProtection() {
        val result = SecurityPolicy.assess(SecurityFacts(tunnelConnected = false))
        assertEquals(ProtectionLevel.INACTIVE, result.protection)
        assertEquals(CheckStatus.PENDING, result.dnsProtection)
        assertEquals(CheckStatus.PENDING, result.ipv6Protection)
        assertTrue(result.missing.isEmpty())
    }

    @Test fun connectedWithoutHandshakeIsUnverified() {
        val result = SecurityPolicy.assess(connected(handshake = null))
        assertEquals(ProtectionLevel.UNVERIFIED, result.protection)
        assertEquals(listOf(MissingCheck.HANDSHAKE), result.missing)
    }

    @Test fun missingAndroidLinkBlocksVerification() {
        val result = SecurityPolicy.assess(connected(link = false))
        assertEquals(ProtectionLevel.UNVERIFIED, result.protection)
        assertTrue(result.missing.contains(MissingCheck.ANDROID_LINK))
    }

    @Test fun emptyTunnelDnsIsHonestOffAndBlocksVerification() {
        val result = SecurityPolicy.assess(connected(dns = emptyList()))
        assertEquals(CheckStatus.OFF, result.dnsProtection)
        assertTrue(result.missing.contains(MissingCheck.DNS))
    }

    @Test fun ipv4OnlyRoutesLeaveIpv6Uncovered() {
        val result = SecurityPolicy.assess(connected(routes = listOf("0.0.0.0/0")))
        assertEquals(CheckStatus.OFF, result.ipv6Protection)
        assertTrue(result.missing.contains(MissingCheck.IPV6))
    }

    @Test fun fullMeasuredEvidenceIsTheOnlyVerifiedPath() {
        val result = SecurityPolicy.assess(connected())
        assertEquals(ProtectionLevel.VERIFIED, result.protection)
        assertTrue(result.missing.isEmpty())
        assertEquals(CheckStatus.ON, result.dnsProtection)
        assertEquals(CheckStatus.ON, result.ipv6Protection)
    }

    @Test fun ipv6CoverageNeedsPrefixZero() {
        assertTrue(SecurityPolicy.coversIpv6(listOf("::/0")))
        assertTrue(SecurityPolicy.coversIpv6(listOf("0:0:0:0:0:0:0:0/0")))
        assertFalse(SecurityPolicy.coversIpv6(listOf("::/64")))
        assertFalse(SecurityPolicy.coversIpv6(listOf("2001:db8::/32")))
        assertFalse(SecurityPolicy.coversIpv6(listOf("0.0.0.0/0")))
        assertFalse(SecurityPolicy.coversIpv6(listOf("::/0/extra", "::", "")))
    }

    @Test fun ipv4CoverageIsReportedSeparately() {
        assertTrue(SecurityPolicy.coversIpv4(listOf("0.0.0.0/0")))
        assertFalse(SecurityPolicy.coversIpv4(listOf("10.0.0.0/8")))
    }

    @Test fun killSwitchNeedsLockdownAndThisAppAsAlwaysOn() {
        assertEquals(CheckStatus.ON, SecurityPolicy.assess(connected()).killSwitch)
        assertEquals(CheckStatus.OFF,
            SecurityPolicy.assess(connected(alwaysOnApp = "com.example.other")).killSwitch)
        assertEquals(CheckStatus.OFF,
            SecurityPolicy.assess(connected(lockdown = false)).killSwitch)
    }

    @Test fun unreadableSystemSettingsStayUnknown() {
        val result = SecurityPolicy.assess(connected(alwaysOnApp = null, lockdown = null))
        assertEquals(CheckStatus.UNKNOWN, result.killSwitch)
        assertEquals(CheckStatus.UNKNOWN, result.alwaysOn)
    }

    @Test fun alwaysOnOtherAppIsOffNotOn() {
        val result = SecurityPolicy.assess(connected(alwaysOnApp = "com.example.other"))
        assertEquals(CheckStatus.OFF, result.alwaysOn)
    }

    @Test fun lockdownSourcesMustAgree() {
        assertEquals(true, SecurityPolicy.resolveLockdown(true, true))
        assertEquals(false, SecurityPolicy.resolveLockdown(false, false))
        assertEquals(true, SecurityPolicy.resolveLockdown(true, null))
        assertEquals(false, SecurityPolicy.resolveLockdown(null, false))
        assertNull(SecurityPolicy.resolveLockdown(true, false))
        assertNull(SecurityPolicy.resolveLockdown(null, null))
    }

    @Test fun consentIsReportedButNotUsedAsProtection() {
        val result = SecurityPolicy.assess(connected().copy(consentGranted = false))
        assertEquals(ProtectionLevel.VERIFIED, result.protection)
        assertEquals(false, connected().copy(consentGranted = false).consentGranted)
    }
}
