package com.knoux.kxprivatevpn.platform.security

import android.content.Context
import android.net.VpnService
import android.provider.Settings

/** Raw platform reading. Unknown values stay null and are rendered as unknown, never as off. */
data class DeviceSecurityState(
    val alwaysOnApp: String? = null,
    val backendLockdown: Boolean? = null,
    val secureLockdown: Boolean? = null,
    val consentGranted: Boolean? = null,
    val appPackage: String = "",
    val measuredAtMillis: Long = 0L,
) {
    /** Two sources must agree; a disagreement surfaces as unknown instead of a guess. */
    val lockdown: Boolean? get() = SecurityPolicy.resolveLockdown(backendLockdown, secureLockdown)
}

/**
 * Reads only settings Android exposes to the owning user. The app never changes
 * always-on/lockdown itself: those require the system VPN settings screen or device owner.
 */
class SecurityDiagnostics(private val context: Context) {
    fun read(backendLockdown: Boolean?, now: Long = System.currentTimeMillis()): DeviceSecurityState {
        val resolver = context.contentResolver
        val alwaysOn = try {
            Settings.Secure.getString(resolver, ALWAYS_ON_VPN_APP) ?: ""
        } catch (_: Exception) {
            null
        }
        val secureLockdown = try {
            Settings.Secure.getInt(resolver, ALWAYS_ON_VPN_LOCKDOWN, -1).let { value ->
                if (value == 1) true else if (value == 0) false else null
            }
        } catch (_: Exception) {
            null
        }
        val consent = try {
            VpnService.prepare(context) == null
        } catch (_: Exception) {
            null
        }
        return DeviceSecurityState(
            alwaysOnApp = alwaysOn,
            backendLockdown = backendLockdown,
            secureLockdown = secureLockdown,
            consentGranted = consent,
            appPackage = context.packageName,
            measuredAtMillis = now,
        )
    }

    private companion object {
        // Documented stable secure-setting keys; the public constants are hidden in the SDK.
        const val ALWAYS_ON_VPN_APP = "always_on_vpn_app"
        const val ALWAYS_ON_VPN_LOCKDOWN = "always_on_vpn_lockdown"
    }
}
