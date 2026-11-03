package com.knoux.kxprivatevpn.platform.vpn

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

data class VpnLinkInfo(
    val present: Boolean = false,
    val interfaceName: String? = null,
    val addresses: List<String> = emptyList(),
    val dns: List<String> = emptyList(),
    val routes: Int = 0,
    val mtu: Int? = null,
)

/** Real Android view of the VPN network, read only from ConnectivityManager. */
object VpnLinkReader {
    fun read(context: Context): VpnLinkInfo {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return VpnLinkInfo()
        for (network in manager.allNetworks) {
            val capabilities = manager.getNetworkCapabilities(network) ?: continue
            if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) continue
            val properties = manager.getLinkProperties(network) ?: continue
            return VpnLinkInfo(
                present = true,
                interfaceName = properties.interfaceName,
                addresses = properties.linkAddresses.map { "${it.address.hostAddress}/${it.prefixLength}" },
                dns = properties.dnsServers.mapNotNull { it.hostAddress },
                routes = properties.routes.size,
                // LinkProperties#getMtu exists from API 29; older devices report no MTU.
                mtu = if (android.os.Build.VERSION.SDK_INT >= 29) properties.mtu.takeIf { it > 0 } else null,
            )
        }
        return VpnLinkInfo()
    }
}
