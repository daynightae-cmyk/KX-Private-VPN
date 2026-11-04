package com.knoux.kxprivatevpn.platform.vpn

import com.wireguard.config.Config
import com.wireguard.config.Interface

/**
 * Which apps the WireGuard tunnel may carry. [ONLY_VPN] maps to Android's
 * `addAllowedApplication`; [BYPASS] maps to `addDisallowedApplication`.
 *
 * WireGuard supports one list at a time. An empty selection is deliberately *not* applied:
 * an empty allow-list would route no app at all, so the tunnel keeps its normal all-apps
 * behaviour and the UI says so instead of pretending the mode is active.
 */
enum class SplitTunnelMode { ONLY_VPN, BYPASS }

data class SplitTunnelSelection(
    val mode: SplitTunnelMode = SplitTunnelMode.ONLY_VPN,
    val packages: Set<String> = emptySet(),
)

enum class SplitTunnelWarning {
    /** Only-through-VPN has no installed selection, so no allow-list can be applied. */
    NO_APPS_SELECTED,

    /** Saved packages are no longer installed/visible; they are not sent to Android. */
    SAVED_APPS_NOT_INSTALLED,
}

/**
 * A measured plan: [includedPackages]/[excludedPackages] are exactly the package names that
 * may be handed to `Interface.Builder`. [installedCount] is the number of launchable apps
 * visible to this app (own package excluded).
 */
data class SplitTunnelPlan(
    val mode: SplitTunnelMode,
    val includedPackages: Set<String> = emptySet(),
    val excludedPackages: Set<String> = emptySet(),
    val stalePackages: Set<String> = emptySet(),
    val isApplied: Boolean = false,
    val installedCount: Int = 0,
    val warnings: Set<SplitTunnelWarning> = emptySet(),
) {
    val throughVpnCount: Int
        get() = when {
            includedPackages.isNotEmpty() -> includedPackages.size
            excludedPackages.isNotEmpty() -> (installedCount - excludedPackages.size).coerceAtLeast(0)
            else -> installedCount
        }

    val bypassCount: Int
        get() = (installedCount - throughVpnCount).coerceAtLeast(0)
}

object SplitTunnelPolicy {
    private val PACKAGE_NAME = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+")

    fun isValidPackage(packageName: String): Boolean =
        packageName.length in 3..255 && PACKAGE_NAME.matches(packageName)

    /**
     * Intersects the persisted intent with what Android currently reports. The app's own
     * package is always removed: Android never sends the owner of the VpnService through
     * its own tunnel, so offering it would be a false promise.
     */
    /**
     * Applies a row toggle while preserving the *effective* routing the user sees.
     * ONLY_VPN with no applied list routes every app, so switching one row off has to seed
     * an explicit allow-list with the remaining installed apps instead of doing nothing.
     * Pure so the behaviour is testable without Android.
     */
    fun toggle(
        mode: SplitTunnelMode,
        applied: Boolean,
        packages: Set<String>,
        installedPackages: Set<String>,
        packageName: String,
    ): Set<String> = when (mode) {
        SplitTunnelMode.ONLY_VPN -> when {
            !applied -> installedPackages - packageName
            packageName in packages -> packages - packageName
            else -> packages + packageName
        }
        SplitTunnelMode.BYPASS -> when {
            packageName in packages -> packages - packageName
            else -> packages + packageName
        }
    }

    fun plan(
        selection: SplitTunnelSelection,
        installedPackages: Set<String>,
        ownPackage: String? = null,
    ): SplitTunnelPlan {
        val eligible = installedPackages.filterTo(linkedSetOf()) {
            isValidPackage(it) && it != ownPackage
        }
        val requested = selection.packages.filterTo(linkedSetOf()) {
            isValidPackage(it) && it != ownPackage
        }
        val active = requested.filterTo(linkedSetOf()) { it in eligible }
        val stale = (requested - eligible).toSet()
        val onlyVpn = selection.mode == SplitTunnelMode.ONLY_VPN
        val applied = active.isNotEmpty()
        val warnings = buildSet {
            if (onlyVpn && active.isEmpty()) add(SplitTunnelWarning.NO_APPS_SELECTED)
            if (stale.isNotEmpty()) add(SplitTunnelWarning.SAVED_APPS_NOT_INSTALLED)
        }
        return SplitTunnelPlan(
            mode = selection.mode,
            includedPackages = if (onlyVpn && applied) active else emptySet(),
            excludedPackages = if (!onlyVpn && applied) active else emptySet(),
            stalePackages = stale,
            isApplied = applied,
            installedCount = eligible.size,
            warnings = warnings,
        )
    }
}

/**
 * Rebuilds the parsed profile so WireGuard's `GoBackend` can call
 * `addAllowedApplication`/`addDisallowedApplication`. KX owns per-app routing: any
 * `IncludedApplications`/`ExcludedApplications` present in an imported profile are replaced
 * by [plan], never merged with it.
 */
object SplitTunnelConfig {
    fun apply(config: Config, plan: SplitTunnelPlan): Config {
        val source = config.`interface`
        val builder = Interface.Builder()
            .setKeyPair(source.keyPair)
            .addAddresses(source.addresses)
            .addDnsServers(source.dnsServers)
            .addDnsSearchDomains(source.dnsSearchDomains)
        source.listenPort.ifPresent { builder.setListenPort(it) }
        source.mtu.ifPresent { builder.setMtu(it) }
        if (plan.includedPackages.isNotEmpty()) {
            builder.includeApplications(plan.includedPackages)
        } else if (plan.excludedPackages.isNotEmpty()) {
            builder.excludeApplications(plan.excludedPackages)
        }
        return Config.Builder()
            .setInterface(builder.build())
            .addPeers(config.peers)
            .build()
    }
}
