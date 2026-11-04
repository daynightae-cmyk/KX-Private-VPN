package com.knoux.kxprivatevpn.data.config

import android.content.Context
import com.knoux.kxprivatevpn.platform.vpn.AdvancedNetworkSettings
import com.knoux.kxprivatevpn.platform.vpn.AdvancedNetworkValidator
import com.knoux.kxprivatevpn.platform.vpn.DnsMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * MTU, DNS mode and keepalive overrides only. Stored values are sanitized on the way out:
 * anything the validator no longer accepts falls back to "the profile decides", never to a
 * guessed default. A failed synchronous commit is reported to the caller instead of
 * pretending the change will survive a restart.
 */
class NetworkSettingsStore(context: Context, namespace: String = "advanced_network") {
    init { require(namespace.matches(Regex("[a-z0-9_-]{1,64}"))) }
    private val preferences =
        context.applicationContext.getSharedPreferences(namespace, Context.MODE_PRIVATE)

    suspend fun load(): AdvancedNetworkSettings = withContext(Dispatchers.IO) {
        val mtu = preferences.getInt(KEY_MTU, Int.MIN_VALUE)
            .takeIf { AdvancedNetworkValidator.isValidMtu(it) }
        val keepalive = preferences.getInt(KEY_KEEPALIVE, Int.MIN_VALUE)
            .takeIf { AdvancedNetworkValidator.isValidKeepalive(it) }
        val storedMode = DnsMode.entries.firstOrNull {
            it.name == preferences.getString(KEY_DNS_MODE, null)
        } ?: DnsMode.PROFILE
        val hosts = preferences.getString(KEY_DNS, null).orEmpty()
            .split(SEPARATOR)
            .map { it.trim() }
            .filter { it.isNotEmpty() && AdvancedNetworkValidator.isNumericHost(it) }
            .distinct()
        // A custom mode without usable servers cannot be honoured; say so by falling back.
        val mode = if (storedMode == DnsMode.CUSTOM && hosts.isEmpty()) DnsMode.PROFILE else storedMode
        AdvancedNetworkSettings(mtu, mode, hosts, keepalive)
    }

    suspend fun save(settings: AdvancedNetworkSettings) = withContext(Dispatchers.IO) {
        require(AdvancedNetworkValidator.isValidMtu(settings.mtuOverride)) {
            "MTU must be unset or inside 1280..1420"
        }
        require(AdvancedNetworkValidator.isValidKeepalive(settings.keepaliveSeconds)) {
            "Keepalive must be unset or inside 0..65535"
        }
        val hosts = if (settings.dnsMode == DnsMode.CUSTOM) {
            AdvancedNetworkValidator.validDnsHosts(settings.customDns)
                ?: throw IllegalArgumentException("Custom DNS needs 1-3 numeric IP addresses")
            settings.customDns
        } else emptyList()
        val committed = preferences.edit().apply {
            if (settings.mtuOverride == null) remove(KEY_MTU) else putInt(KEY_MTU, settings.mtuOverride)
            putString(KEY_DNS_MODE, settings.dnsMode.name)
            if (hosts.isEmpty()) remove(KEY_DNS) else putString(KEY_DNS, hosts.joinToString(SEPARATOR))
            if (settings.keepaliveSeconds == null) remove(KEY_KEEPALIVE)
            else putInt(KEY_KEEPALIVE, settings.keepaliveSeconds)
        }.commit()
        check(committed) { "Advanced network settings could not be saved" }
    }

    private companion object {
        const val KEY_MTU = "mtu"
        const val KEY_DNS_MODE = "dns_mode"
        const val KEY_DNS = "custom_dns"
        const val KEY_KEEPALIVE = "keepalive"
        const val SEPARATOR = ","
    }
}
