package com.knoux.kxprivatevpn

import com.knoux.kxprivatevpn.platform.vpn.AdvancedNetworkConfig
import com.knoux.kxprivatevpn.platform.vpn.AdvancedNetworkSettings
import com.knoux.kxprivatevpn.platform.vpn.AdvancedNetworkValidator
import com.knoux.kxprivatevpn.platform.vpn.SplitTunnelConfig
import com.knoux.kxprivatevpn.platform.vpn.SplitTunnelMode
import com.knoux.kxprivatevpn.platform.vpn.SplitTunnelPlan
import com.wireguard.config.Config
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.Base64

/**
 * The rebuilt profile is what the backend will actually start with: MTU/DNS through the
 * interface, keepalive on every peer, and split-tunnel application lists must survive the
 * whole `advanced.apply(split.apply(config))` chain without being dropped or merged.
 */
class AdvancedNetworkConfigTest {
    private val key = Base64.getEncoder().encodeToString(ByteArray(32) { (it + 1).toByte() })

    private fun profile(
        mtu: Int? = null,
        dns: String? = null,
        keepalive: Int? = null,
        secondPeer: Boolean = false,
    ): Config {
        val text = buildString {
            appendLine("[Interface]")
            appendLine("PrivateKey = $key")
            appendLine("Address = 10.7.0.2/32")
            if (dns != null) appendLine("DNS = $dns")
            if (mtu != null) appendLine("MTU = $mtu")
            appendLine("[Peer]")
            appendLine("PublicKey = $key")
            appendLine("Endpoint = 192.0.2.1:51820")
            appendLine("AllowedIPs = 0.0.0.0/0, ::/0")
            if (keepalive != null) appendLine("PersistentKeepalive = $keepalive")
            if (secondPeer) {
                appendLine("[Peer]")
                appendLine("PublicKey = $key")
                appendLine("Endpoint = 192.0.2.2:51820")
                appendLine("AllowedIPs = 10.66.66.0/24")
            }
        }
        return Config.parse(ByteArrayInputStream(text.toByteArray()))
    }

    @Test
    fun mtuOverrideReplacesProfileWithoutTouchingAnythingElse() {
        val config = profile(mtu = 1310)
        val out = AdvancedNetworkConfig.apply(config,
            AdvancedNetworkSettings(mtuOverride = 1280))

        assertEquals(1280, out.`interface`.mtu.get())
        assertEquals(config.`interface`.addresses, out.`interface`.addresses)
        assertEquals(config.`interface`.dnsServers, out.`interface`.dnsServers)
        assertEquals(config.peers.size, out.peers.size)
        assertFalse(out.peers.first().persistentKeepalive.isPresent)
    }

    @Test
    fun unsetMtuKeepsTheProfileValue() {
        val config = profile(mtu = 1310)
        val out = AdvancedNetworkConfig.apply(config, AdvancedNetworkSettings())

        assertEquals(1310, out.`interface`.mtu.get())
    }

    @Test
    fun customDnsReplacesTheProfileServers() {
        val config = profile(dns = "8.8.8.8")
        val out = AdvancedNetworkConfig.apply(config, AdvancedNetworkSettings(
            dnsMode = com.knoux.kxprivatevpn.platform.vpn.DnsMode.CUSTOM,
            customDns = listOf("1.1.1.1", "2606:4700:4700::1111"),
        ))

        val expected = listOf("1.1.1.1", "2606:4700:4700::1111").mapNotNull {
            com.wireguard.config.InetAddresses.parse(it)
        }.sortedBy { it.hostAddress }
        assertEquals(expected, out.`interface`.dnsServers.sortedBy { it.hostAddress })
    }

    @Test
    fun systemModeLeavesTheDeviceResolverAlone() {
        val config = profile(dns = "8.8.8.8")
        val out = AdvancedNetworkConfig.apply(config, AdvancedNetworkSettings(
            dnsMode = com.knoux.kxprivatevpn.platform.vpn.DnsMode.SYSTEM))

        assertTrue(out.`interface`.dnsServers.isEmpty())
    }

    @Test
    fun profileModeKeepsTheImportedServers() {
        val config = profile(dns = "8.8.8.8")
        val out = AdvancedNetworkConfig.apply(config, AdvancedNetworkSettings())

        assertEquals(setOf("8.8.8.8"), out.`interface`.dnsServers.map { it.hostAddress }.toSet())
    }

    @Test
    fun customModeWithoutUsableServersFallsBackToProfileServers() {
        val config = profile(dns = "8.8.8.8")
        val out = AdvancedNetworkConfig.apply(config, AdvancedNetworkSettings(
            dnsMode = com.knoux.kxprivatevpn.platform.vpn.DnsMode.CUSTOM,
            customDns = emptyList(),
        ))

        assertEquals(setOf("8.8.8.8"), out.`interface`.dnsServers.map { it.hostAddress }.toSet())
    }

    @Test
    fun keepaliveOverrideReachesEveryPeer() {
        val config = profile(keepalive = 25, secondPeer = true)
        val out = AdvancedNetworkConfig.apply(config, AdvancedNetworkSettings(keepaliveSeconds = 30))

        assertEquals(2, out.peers.size)
        out.peers.forEach { peer -> assertEquals(30, peer.persistentKeepalive.get()) }
    }

    @Test
    fun keepaliveZeroReallyStopsThePackets() {
        val config = profile(keepalive = 25)
        val out = AdvancedNetworkConfig.apply(config, AdvancedNetworkSettings(keepaliveSeconds = 0))

        assertFalse(out.peers.single().persistentKeepalive.isPresent)
    }

    @Test
    fun unsetKeepaliveLeavesTheImportedValue() {
        val config = profile(keepalive = 25)
        val out = AdvancedNetworkConfig.apply(config, AdvancedNetworkSettings())

        assertEquals(25, out.peers.single().persistentKeepalive.get())
    }

    @Test
    fun rebuiltPeerKeepsItsRoutingAndEndpoint() {
        val config = profile(keepalive = 25)
        val out = AdvancedNetworkConfig.apply(config, AdvancedNetworkSettings(keepaliveSeconds = 90))
        val rebuilt = out.peers.single()
        val original = config.peers.single()

        assertEquals(original.publicKey, rebuilt.publicKey)
        assertEquals(original.endpoint, rebuilt.endpoint)
        assertEquals(original.allowedIps, rebuilt.allowedIps)
        assertEquals(90, rebuilt.persistentKeepalive.get())
    }

    @Test
    fun advancedAfterSplitKeepsAppListsAndAppliesBoth() {
        val base = profile(mtu = 1300, dns = "8.8.8.8", keepalive = 25)
        val plan = SplitTunnelPlan(
            mode = SplitTunnelMode.BYPASS,
            excludedPackages = setOf("com.example.meta"),
            isApplied = true,
            installedCount = 3,
        )
        val afterSplit = SplitTunnelConfig.apply(base, plan)
        val out = AdvancedNetworkConfig.apply(afterSplit, AdvancedNetworkSettings(
            mtuOverride = 1280, keepaliveSeconds = 20))

        assertEquals(setOf("com.example.meta"), out.`interface`.excludedApplications)
        assertEquals(1280, out.`interface`.mtu.get())
        assertEquals(20, out.peers.single().persistentKeepalive.get())
        assertEquals(base.`interface`.addresses, out.`interface`.addresses)
    }

    @Test
    fun validatorBoundsAreTheRealTunnelBounds() {
        assertTrue(AdvancedNetworkValidator.isValidMtu(null))
        assertTrue(AdvancedNetworkValidator.isValidMtu(1280))
        assertTrue(AdvancedNetworkValidator.isValidMtu(1420))
        assertFalse(AdvancedNetworkValidator.isValidMtu(1279))
        assertFalse(AdvancedNetworkValidator.isValidMtu(1421))

        assertTrue(AdvancedNetworkValidator.isValidKeepalive(null))
        assertTrue(AdvancedNetworkValidator.isValidKeepalive(0))
        assertTrue(AdvancedNetworkValidator.isValidKeepalive(65535))
        assertFalse(AdvancedNetworkValidator.isValidKeepalive(-1))
        assertFalse(AdvancedNetworkValidator.isValidKeepalive(65536))
    }

    @Test
    fun dnsHostsMustBeRealNumericAddresses() {
        assertEquals(listOf("1.1.1.1"),
            AdvancedNetworkValidator.validDnsHosts(listOf(" 1.1.1.1 ", "1.1.1.1")))
        assertEquals(listOf("1.1.1.1", "2606:4700:4700::1111"),
            AdvancedNetworkValidator.validDnsHosts(listOf("1.1.1.1", "2606:4700:4700::1111")))
        assertNull(AdvancedNetworkValidator.validDnsHosts(emptyList()))
        assertNull(AdvancedNetworkValidator.validDnsHosts(listOf("example.com")))
        assertNull(AdvancedNetworkValidator.validDnsHosts(listOf("999.1.1.1")))
        assertNull(AdvancedNetworkValidator.validDnsHosts(listOf("0.0.0.0")))
        assertNull(AdvancedNetworkValidator.validDnsHosts(listOf("::")))
        assertNull(AdvancedNetworkValidator.validDnsHosts(listOf("1.1.1.1", "8.8.8.8",
            "9.9.9.9", "149.112.112.112")))
    }
}
