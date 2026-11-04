package com.knoux.kxprivatevpn.platform.vpn

import com.wireguard.config.InetAddresses

/**
 * The device-owned tunnel settings this build can really change. [mtuOverride] and
 * [keepaliveSeconds] are `null`-measured values, not invented defaults: when null the
 * WireGuard profile decides and the UI says so instead of guessing a number.
 *
 * WireGuard is the only protocol configured here. OpenVPN and "Automatic" have no
 * implementation in this build; the protocol row shows them honestly unavailable rather
 * than letting a choice pretend a backend exists.
 */
enum class DnsMode { PROFILE, CUSTOM, SYSTEM }

data class AdvancedNetworkSettings(
    val mtuOverride: Int? = null,
    val dnsMode: DnsMode = DnsMode.PROFILE,
    val customDns: List<String> = emptyList(),
    val keepaliveSeconds: Int? = null,
)

/**
 * Bounds and validation for the settings the tunnel will actually receive. The MTU range
 * keeps the usable IPv6 floor and the WireGuard reference maximum; DNS hosts must be numeric
 * IP addresses so a change never depends on a resolver lookup.
 */
object AdvancedNetworkValidator {
    const val MTU_MIN = 1280
    const val MTU_MAX = 1420
    const val KEEPALIVE_MAX = 65535
    const val DNS_MAX_HOSTS = 3

    fun isValidMtu(value: Int?): Boolean = value == null || value in MTU_MIN..MTU_MAX

    fun isValidKeepalive(value: Int?): Boolean = value == null || value in 0..KEEPALIVE_MAX

    /** Numeric and usable as a resolver: IPv4/IPv6, but never the unspecified address. */
    fun isNumericHost(text: String): Boolean {
        if (text.isEmpty() || text.length > 45) return false
        if (text == "0.0.0.0" || text == "::") return false
        return runCatching { InetAddresses.parse(text) }.isSuccess
    }

    fun validDnsHosts(input: List<String>): List<String>? {
        val cleaned = input.asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toCollection(LinkedHashSet())
        if (cleaned.isEmpty() || cleaned.size > DNS_MAX_HOSTS) return null
        if (cleaned.any { !isNumericHost(it) }) return null
        return cleaned.toList()
    }
}
