package com.knoux.kxprivatevpn.platform.security

/**
 * Everything here is measured, not assumed. A protection claim exists only when every
 * check below has a value from the platform or the WireGuard backend.
 */
enum class ProtectionLevel { VERIFIED, UNVERIFIED, INACTIVE }

enum class CheckStatus { ON, OFF, UNKNOWN, UNAVAILABLE, PENDING }

enum class MissingCheck { HANDSHAKE, ANDROID_LINK, DNS, IPV6 }

/**
 * @param alwaysOnApp package name from Settings.Secure, "" when no app is configured,
 *                    null when the setting could not be read on this device.
 * @param systemLockdown result of the WireGuard backend / Settings.Secure lockdown read.
 */
data class SecurityFacts(
    val tunnelConnected: Boolean = false,
    val androidLinkPresent: Boolean = false,
    val handshakeMillis: Long? = null,
    val dnsServers: List<String> = emptyList(),
    val allowedIps: List<String> = emptyList(),
    val alwaysOnApp: String? = null,
    val appPackage: String = "",
    val systemLockdown: Boolean? = null,
    val consentGranted: Boolean? = null,
)

data class SecurityAssessment(
    val protection: ProtectionLevel = ProtectionLevel.INACTIVE,
    val killSwitch: CheckStatus = CheckStatus.UNKNOWN,
    val dnsProtection: CheckStatus = CheckStatus.PENDING,
    val ipv6Protection: CheckStatus = CheckStatus.PENDING,
    val alwaysOn: CheckStatus = CheckStatus.UNKNOWN,
    val missing: List<MissingCheck> = emptyList(),
)

object SecurityPolicy {
    fun assess(facts: SecurityFacts): SecurityAssessment {
        val connected = facts.tunnelConnected
        val dns = when {
            !connected -> CheckStatus.PENDING
            facts.dnsServers.isNotEmpty() -> CheckStatus.ON
            else -> CheckStatus.OFF
        }
        val ipv6 = when {
            !connected -> CheckStatus.PENDING
            coversIpv6(facts.allowedIps) -> CheckStatus.ON
            else -> CheckStatus.OFF
        }
        val alwaysOn = when {
            facts.alwaysOnApp == null -> CheckStatus.UNKNOWN
            facts.alwaysOnApp == facts.appPackage -> CheckStatus.ON
            else -> CheckStatus.OFF
        }
        // Android's lockdown only protects this app when it is also the always-on VPN app.
        val killSwitch = when {
            facts.alwaysOnApp == null || facts.systemLockdown == null -> CheckStatus.UNKNOWN
            facts.systemLockdown && facts.alwaysOnApp == facts.appPackage -> CheckStatus.ON
            else -> CheckStatus.OFF
        }
        val missing = buildList {
            if (connected) {
                if (facts.handshakeMillis == null) add(MissingCheck.HANDSHAKE)
                if (!facts.androidLinkPresent) add(MissingCheck.ANDROID_LINK)
                if (dns != CheckStatus.ON) add(MissingCheck.DNS)
                if (ipv6 != CheckStatus.ON) add(MissingCheck.IPV6)
            }
        }
        val protection = when {
            !connected -> ProtectionLevel.INACTIVE
            missing.isEmpty() -> ProtectionLevel.VERIFIED
            else -> ProtectionLevel.UNVERIFIED
        }
        return SecurityAssessment(
            protection = protection,
            killSwitch = killSwitch,
            dnsProtection = dns,
            ipv6Protection = ipv6,
            alwaysOn = alwaysOn,
            missing = missing,
        )
    }

    /** Only ::/0 (any spelling of the IPv6 unspecified address with prefix 0) covers IPv6. */
    fun coversIpv6(allowedIps: List<String>): Boolean = allowedIps.any(::coversAllIpv6)

    fun coversIpv4(allowedIps: List<String>): Boolean = allowedIps.any(::coversAllIpv4)

    fun resolveLockdown(backend: Boolean?, secure: Boolean?): Boolean? = when {
        backend == null -> secure
        secure == null -> backend
        backend == secure -> backend
        // Two authoritative sources disagree: report an unknown state, never a guess.
        else -> null
    }

    private fun coversAllIpv6(entry: String): Boolean {
        val parts = entry.trim().split('/')
        return parts.size == 2 && parts[0].contains(':') && parts[1].toIntOrNull() == 0
    }

    private fun coversAllIpv4(entry: String): Boolean {
        val parts = entry.trim().split('/')
        return parts.size == 2 && parts[0].isNotEmpty() && !parts[0].contains(':') &&
            parts[0].count { it == '.' } == 3 && parts[1].toIntOrNull() == 0
    }
}
