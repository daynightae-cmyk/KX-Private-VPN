package com.knoux.kxprivatevpn.data.config

import android.content.Context
import com.knoux.kxprivatevpn.platform.vpn.SplitTunnelMode
import com.knoux.kxprivatevpn.platform.vpn.SplitTunnelPolicy
import com.knoux.kxprivatevpn.platform.vpn.SplitTunnelSelection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Package names and the routing mode only. No traffic, labels or icons are persisted.
 * A failed synchronous commit is reported to the caller instead of pretending the change
 * will survive a restart.
 */
class SplitTunnelStore(context: Context, namespace: String = "split_tunnel") {
    init { require(namespace.matches(Regex("[a-z0-9_-]{1,64}"))) }
    private val preferences = context.applicationContext.getSharedPreferences(namespace, Context.MODE_PRIVATE)

    suspend fun load(): SplitTunnelSelection = withContext(Dispatchers.IO) {
        val mode = when (preferences.getString(KEY_MODE, null)) {
            SplitTunnelMode.BYPASS.name -> SplitTunnelMode.BYPASS
            else -> SplitTunnelMode.ONLY_VPN
        }
        val packages = preferences.getStringSet(KEY_PACKAGES, emptySet()).orEmpty()
            .asSequence()
            .filter(SplitTunnelPolicy::isValidPackage)
            .toSet()
        SplitTunnelSelection(mode, packages)
    }

    suspend fun save(selection: SplitTunnelSelection) = withContext(Dispatchers.IO) {
        val clean = selection.packages
            .asSequence()
            .filter(SplitTunnelPolicy::isValidPackage)
            .toSet()
        val saved = preferences.edit()
            .putString(KEY_MODE, selection.mode.name)
            .putStringSet(KEY_PACKAGES, clean)
            .commit()
        check(saved) { "Split tunnel selection could not be saved" }
    }

    private companion object {
        const val KEY_MODE = "mode"
        const val KEY_PACKAGES = "packages"
    }
}
