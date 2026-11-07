package com.knoux.kxprivatevpn.features

import androidx.annotation.StringRes
import com.knoux.kxprivatevpn.R

/** Discovery metadata only; availability and measurements belong to each real service. */
enum class HomeServiceSection(@StringRes val title: Int) {
    DEVICE(R.string.home_section_device),
    DIAGNOSTICS(R.string.home_section_diagnostics),
    VPN(R.string.home_section_vpn),
}

data class HomeServiceEntry(
    val route: String,
    val section: HomeServiceSection,
    @StringRes val title: Int,
    @StringRes val description: Int,
    @StringRes val capability: Int,
    @StringRes val action: Int,
)

object HomeServiceCatalog {
    // Keep destinations explicit: discovery never manufactures repair/<id> fallback routes.
    val entries = listOf(
        HomeServiceEntry("storage", HomeServiceSection.DEVICE, R.string.storage,
            R.string.home_storage_description, R.string.home_scope_chosen_access, R.string.home_action_storage),
        HomeServiceEntry("cache", HomeServiceSection.DEVICE, R.string.cache_surgeon,
            R.string.home_cache_description, R.string.home_scope_kx_only, R.string.home_action_cache),
        HomeServiceEntry("apps", HomeServiceSection.DEVICE, R.string.app_inventory_title,
            R.string.home_apps_description, R.string.home_scope_visible_apps, R.string.home_action_apps),
        HomeServiceEntry("network", HomeServiceSection.DIAGNOSTICS, R.string.network_doctor,
            R.string.home_network_description, R.string.home_scope_read_only, R.string.home_action_network),
        HomeServiceEntry("security", HomeServiceSection.DIAGNOSTICS, R.string.security_center,
            R.string.home_security_description, R.string.home_scope_read_only, R.string.home_action_security),
        HomeServiceEntry("stats", HomeServiceSection.DIAGNOSTICS, R.string.stats_title,
            R.string.home_stats_description, R.string.home_scope_local_history, R.string.home_action_stats),
        HomeServiceEntry("import", HomeServiceSection.VPN, R.string.import_config,
            R.string.home_import_description, R.string.home_scope_own_profile, R.string.home_action_import),
        HomeServiceEntry("split", HomeServiceSection.VPN, R.string.split_title,
            R.string.home_split_description, R.string.home_scope_profile_required, R.string.home_action_split),
        HomeServiceEntry("advanced", HomeServiceSection.VPN, R.string.adv_title,
            R.string.home_advanced_description, R.string.home_scope_profile_required, R.string.home_action_advanced),
    )
}
