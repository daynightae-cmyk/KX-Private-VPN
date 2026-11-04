package com.knoux.kxprivatevpn

import com.knoux.kxprivatevpn.data.config.ConfigValidator
import com.knoux.kxprivatevpn.platform.vpn.SplitTunnelConfig
import com.knoux.kxprivatevpn.platform.vpn.SplitTunnelMode
import com.knoux.kxprivatevpn.platform.vpn.SplitTunnelPolicy
import com.knoux.kxprivatevpn.platform.vpn.SplitTunnelSelection
import com.knoux.kxprivatevpn.platform.vpn.SplitTunnelWarning
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

/** Pure routing rules: Android package names only, one list at a time, no invented apps. */
class SplitTunnelPolicyTest {
    private val key = Base64.getEncoder().encodeToString(ByteArray(32) { (it + 1).toByte() })
    private val installed = setOf(
        "com.example.browser",
        "com.example.chat",
        "com.example.music",
        "com.knoux.kxprivatevpn",
    )

    @Test fun onlyVpnSendsExactlyTheInstalledSelection() {
        val plan = SplitTunnelPolicy.plan(
            SplitTunnelSelection(SplitTunnelMode.ONLY_VPN, setOf("com.example.browser")),
            installed, ownPackage = "com.knoux.kxprivatevpn",
        )
        assertTrue(plan.isApplied)
        assertEquals(setOf("com.example.browser"), plan.includedPackages)
        assertTrue(plan.excludedPackages.isEmpty())
        assertEquals(emptySet<SplitTunnelWarning>(), plan.warnings)
    }

    @Test fun bypassModeSendsExactlyTheExcludedSelection() {
        val plan = SplitTunnelPolicy.plan(
            SplitTunnelSelection(SplitTunnelMode.BYPASS,
                setOf("com.example.chat", "com.example.music")),
            installed, ownPackage = "com.knoux.kxprivatevpn",
        )
        assertTrue(plan.isApplied)
        assertTrue(plan.includedPackages.isEmpty())
        assertEquals(setOf("com.example.chat", "com.example.music"), plan.excludedPackages)
    }

    @Test fun ownAndUninstalledPackagesAreNeverSentToAndroid() {
        val plan = SplitTunnelPolicy.plan(
            SplitTunnelSelection(SplitTunnelMode.BYPASS,
                setOf("com.example.gone", "com.knoux.kxprivatevpn", "com.example.browser")),
            installed, ownPackage = "com.knoux.kxprivatevpn",
        )
        assertEquals(setOf("com.example.browser"), plan.excludedPackages)
        assertEquals(setOf("com.example.gone"), plan.stalePackages)
        assertTrue(SplitTunnelWarning.SAVED_APPS_NOT_INSTALLED in plan.warnings)
    }

    @Test fun emptyOnlySelectionIsNotAppliedInsteadOfBlockingEveryApp() {
        val plan = SplitTunnelPolicy.plan(
            SplitTunnelSelection(SplitTunnelMode.ONLY_VPN, emptySet()),
            installed, ownPackage = "com.knoux.kxprivatevpn",
        )
        assertFalse(plan.isApplied)
        assertTrue(plan.includedPackages.isEmpty())
        assertTrue(plan.excludedPackages.isEmpty())
        assertTrue(SplitTunnelWarning.NO_APPS_SELECTED in plan.warnings)
        assertEquals(3, plan.throughVpnCount)
    }

    @Test fun onlySelectionThatIsEntirelyStaleIsNotApplied() {
        val plan = SplitTunnelPolicy.plan(
            SplitTunnelSelection(SplitTunnelMode.ONLY_VPN, setOf("com.example.gone")),
            installed, ownPackage = "com.knoux.kxprivatevpn",
        )
        assertFalse(plan.isApplied)
        assertTrue(SplitTunnelWarning.NO_APPS_SELECTED in plan.warnings)
        assertTrue(SplitTunnelWarning.SAVED_APPS_NOT_INSTALLED in plan.warnings)
    }

    @Test fun emptyBypassSelectionIsNormalAllAppsRouting() {
        val plan = SplitTunnelPolicy.plan(
            SplitTunnelSelection(SplitTunnelMode.BYPASS, emptySet()),
            installed, ownPackage = "com.knoux.kxprivatevpn",
        )
        assertFalse(plan.isApplied)
        assertEquals(emptySet<SplitTunnelWarning>(), plan.warnings)
        assertEquals(3, plan.throughVpnCount)
        assertEquals(0, plan.bypassCount)
    }

    @Test fun countsFollowTheChosenMode() {
        val only = SplitTunnelPolicy.plan(
            SplitTunnelSelection(SplitTunnelMode.ONLY_VPN,
                setOf("com.example.browser", "com.example.chat")),
            installed, ownPackage = "com.knoux.kxprivatevpn",
        )
        assertEquals(3, only.installedCount)
        assertEquals(2, only.throughVpnCount)
        assertEquals(1, only.bypassCount)

        val bypass = SplitTunnelPolicy.plan(
            SplitTunnelSelection(SplitTunnelMode.BYPASS, setOf("com.example.chat")),
            installed, ownPackage = "com.knoux.kxprivatevpn",
        )
        assertEquals(2, bypass.throughVpnCount)
        assertEquals(1, bypass.bypassCount)
    }

    @Test fun togglingOffWithNoAllowListSeedsEveryOtherInstalledApp() {
        val plan = SplitTunnelPolicy.plan(
            SplitTunnelSelection(SplitTunnelMode.ONLY_VPN, emptySet()),
            installed, ownPackage = "com.knoux.kxprivatevpn",
        )
        assertFalse(plan.isApplied)
        val next = SplitTunnelPolicy.toggle(
            mode = SplitTunnelMode.ONLY_VPN,
            applied = plan.isApplied,
            packages = emptySet(),
            installedPackages = setOf("com.example.browser", "com.example.chat",
                "com.example.music"),
            packageName = "com.example.chat",
        )
        assertEquals(setOf("com.example.browser", "com.example.music"), next)
        val applied = SplitTunnelPolicy.plan(
            SplitTunnelSelection(SplitTunnelMode.ONLY_VPN, next),
            installed, ownPackage = "com.knoux.kxprivatevpn",
        )
        assertEquals(setOf("com.example.browser", "com.example.music"),
            applied.includedPackages)
        assertEquals(1, applied.bypassCount)
    }

    @Test fun bypassToggleOnlyTouchesTheExclusionList() {
        val current = setOf("com.example.chat")
        assertEquals(setOf("com.example.chat", "com.example.music"),
            SplitTunnelPolicy.toggle(SplitTunnelMode.BYPASS, true, current,
                installed, "com.example.music"))
        assertEquals(emptySet<String>(),
            SplitTunnelPolicy.toggle(SplitTunnelMode.BYPASS, true, current,
                installed, "com.example.chat"))
    }

    @Test fun onlyVpnToggleAddsAndRemovesFromTheAllowList() {
        val current = setOf("com.example.browser")
        assertEquals(setOf("com.example.browser", "com.example.music"),
            SplitTunnelPolicy.toggle(SplitTunnelMode.ONLY_VPN, true, current,
                installed, "com.example.music"))
        assertEquals(emptySet<String>(),
            SplitTunnelPolicy.toggle(SplitTunnelMode.ONLY_VPN, true, current,
                installed, "com.example.browser"))
    }

    @Test fun invalidPackageNamesNeverReachTheTunnel() {
        val plan = SplitTunnelPolicy.plan(
            SplitTunnelSelection(SplitTunnelMode.ONLY_VPN,
                setOf("com.example.browser", "not a package", "", "com..bad")),
            installed, ownPackage = "com.knoux.kxprivatevpn",
        )
        assertEquals(setOf("com.example.browser"), plan.includedPackages)
        assertTrue(plan.stalePackages.isEmpty())
    }

    private fun config(included: String = "") = """
        [Interface]
        PrivateKey = $key
        Address = 10.7.0.2/32
        DNS = 192.0.2.53
        MTU = 1420
        $included
        [Peer]
        PublicKey = $key
        Endpoint = 192.0.2.1:51820
        AllowedIPs = 0.0.0.0/0, ::/0
    """.trimIndent().toByteArray()

    @Test fun configWriterSendsAllowedApplicationsToWireGuard() {
        val source = ConfigValidator.parse(config())
        val plan = SplitTunnelPolicy.plan(
            SplitTunnelSelection(SplitTunnelMode.ONLY_VPN,
                setOf("com.example.browser", "com.example.chat")),
            installed, ownPackage = "com.knoux.kxprivatevpn",
        )
        val rebuilt = SplitTunnelConfig.apply(source, plan)
        assertEquals(setOf("com.example.browser", "com.example.chat"),
            rebuilt.`interface`.includedApplications)
        assertTrue(rebuilt.`interface`.excludedApplications.isEmpty())
        assertEquals(source.peers, rebuilt.peers)
        assertEquals(source.`interface`.dnsServers, rebuilt.`interface`.dnsServers)
        assertEquals(source.`interface`.mtu, rebuilt.`interface`.mtu)
    }

    @Test fun configWriterSendsDisallowedApplicationsForBypass() {
        val source = ConfigValidator.parse(config())
        val plan = SplitTunnelPolicy.plan(
            SplitTunnelSelection(SplitTunnelMode.BYPASS, setOf("com.example.music")),
            installed, ownPackage = "com.knoux.kxprivatevpn",
        )
        val rebuilt = SplitTunnelConfig.apply(source, plan)
        assertEquals(setOf("com.example.music"), rebuilt.`interface`.excludedApplications)
        assertTrue(rebuilt.`interface`.includedApplications.isEmpty())
    }

    @Test fun notAppliedPlanClearsImportedApplicationRules() {
        val source = ConfigValidator.parse(config("IncludedApplications = com.imported.app"))
        assertEquals(setOf("com.imported.app"), source.`interface`.includedApplications)
        val plan = SplitTunnelPolicy.plan(
            SplitTunnelSelection(SplitTunnelMode.ONLY_VPN, emptySet()),
            installed, ownPackage = "com.knoux.kxprivatevpn",
        )
        val rebuilt = SplitTunnelConfig.apply(source, plan)
        assertTrue(rebuilt.`interface`.includedApplications.isEmpty())
        assertTrue(rebuilt.`interface`.excludedApplications.isEmpty())
    }
}
