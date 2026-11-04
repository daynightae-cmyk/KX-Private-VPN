package com.knoux.kxprivatevpn.platform.vpn

import android.content.Context
import com.knoux.kxprivatevpn.data.config.SplitTunnelStore
import com.knoux.kxprivatevpn.platform.apps.InstalledApps
import com.wireguard.config.Config

/**
 * Reads the persisted intent and Android's current package list at tunnel start, so a
 * package removed since the last run can never be handed to `VpnService.Builder`.
 */
class SplitTunnelConfigurator(
    private val context: Context,
    private val store: SplitTunnelStore = SplitTunnelStore(context),
    private val apps: InstalledApps = InstalledApps(context),
) {
    suspend fun apply(config: Config): Config {
        val selection = store.load()
        val installed = apps.list().mapTo(linkedSetOf()) { it.packageName }
        val plan = SplitTunnelPolicy.plan(selection, installed, context.packageName)
        return SplitTunnelConfig.apply(config, plan)
    }
}
