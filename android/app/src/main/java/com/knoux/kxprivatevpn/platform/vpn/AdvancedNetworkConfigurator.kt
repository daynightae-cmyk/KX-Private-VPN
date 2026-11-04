package com.knoux.kxprivatevpn.platform.vpn

import android.content.Context
import com.knoux.kxprivatevpn.data.config.NetworkSettingsStore
import com.wireguard.config.Config
import com.wireguard.config.InetAddresses
import com.wireguard.config.Interface
import com.wireguard.config.Peer

/**
 * Reads the persisted advanced settings at tunnel start. [AdvancedNetworkConfig] performs
 * the pure profile rebuild; this class only supplies the device-stored intent, so no
 * setting is guessed at connect time.
 */
class AdvancedNetworkConfigurator(
    context: Context,
    private val store: NetworkSettingsStore = NetworkSettingsStore(context),
) {
    suspend fun apply(config: Config): Config =
        AdvancedNetworkConfig.apply(config, store.load())
}

/**
 * Rebuilds the parsed profile so the backend really receives the advanced settings.
 * MTU and DNS go through `Interface.Builder` (split-tunnel application lists and every
 * other profile value are carried through untouched); the per-peer keepalive is rebuilt
 * textually because `Peer` has no public builder in the WireGuard config library.
 */
object AdvancedNetworkConfig {
    fun apply(config: Config, settings: AdvancedNetworkSettings): Config {
        val next = withInterface(config, settings)
        return if (settings.keepaliveSeconds == null) next
        else withKeepalive(next, settings.keepaliveSeconds)
    }

    private fun withInterface(config: Config, settings: AdvancedNetworkSettings): Config {
        val source = config.`interface`
        val builder = Interface.Builder()
            .setKeyPair(source.keyPair)
            .addAddresses(source.addresses)
            .addDnsSearchDomains(source.dnsSearchDomains)
        source.listenPort.ifPresent { builder.setListenPort(it) }
        // Split tunneling may have written application lists before this step; carried through.
        source.excludedApplications.forEach(builder::excludeApplication)
        source.includedApplications.forEach(builder::includeApplication)
        when (settings.dnsMode) {
            DnsMode.PROFILE -> source.dnsServers.forEach(builder::addDnsServer)
            DnsMode.CUSTOM -> if (settings.customDns.isEmpty()) {
                // Nothing validated to override with: keep the profile's servers instead of
                // silently switching the device resolver off.
                source.dnsServers.forEach(builder::addDnsServer)
            } else {
                settings.customDns.forEach { host ->
                    runCatching { InetAddresses.parse(host) }.getOrNull()?.let(builder::addDnsServer)
                }
            }
            // No DNS servers are emitted: GoBackend leaves the device resolver alone.
            DnsMode.SYSTEM -> Unit
        }
        if (settings.mtuOverride != null) builder.setMtu(settings.mtuOverride)
        else source.mtu.ifPresent { builder.setMtu(it) }
        return Config.Builder()
            .setInterface(builder.build())
            .addPeers(config.peers)
            .build()
    }

    private fun withKeepalive(config: Config, keepaliveSeconds: Int): Config = Config.Builder()
        .setInterface(config.`interface`)
        .addPeers(config.peers.map { peerWithKeepalive(it, keepaliveSeconds) })
        .build()

    /**
     * Textual rebuild of one peer's wireguard lines from its decoded fields, with the
     * keepalive replaced. WireGuard treats keepalive 0 as "off", so an override of 0
     * really stops periodic packets that an imported profile asked for.
     */
    private fun peerWithKeepalive(peer: Peer, keepaliveSeconds: Int): Peer {
        val lines = buildList {
            add("PublicKey = ${peer.publicKey.toBase64()}")
            peer.preSharedKey.ifPresent { add("PresharedKey = ${it.toBase64()}") }
            if (peer.allowedIps.isNotEmpty()) {
                add("AllowedIPs = ${peer.allowedIps.joinToString(", ") { it.toString() }}")
            }
            peer.endpoint.ifPresent { add("Endpoint = $it") }
            add("PersistentKeepalive = $keepaliveSeconds")
        }
        return Peer.parse(lines)
    }
}
