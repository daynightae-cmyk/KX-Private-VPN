package com.knoux.kxprivatevpn.platform.repair

import com.knoux.kxprivatevpn.R

/**
 * The ten signature services of the KX Phone Repair Lab.
 *
 * [available] is a build-level truth, not a marketing flag. A service is listed as available
 * only when its screen performs the real Android work described in its scope. Services that
 * are scheduled for a later slice stay visible but honestly unavailable; they never show
 * simulated results, estimates or progress.
 */
enum class RepairServiceId(val key: String, val route: String, val available: Boolean) {
    STORAGE("storage", "storage", true),
    GHOST("ghost", "ghost", true),
    DUPLICATE("duplicate", "duplicate", true),
    HEAVYWEIGHT("heavyweight", "heavyweight", true),
    CACHE("cache", "cache", true),
    MEDIA("media", "media", true),
    DOWNLOAD("download", "download", true),
    SESSION("session", "repair/session", false),
    NETWORK("network", "network", true),
    AUTOPILOT("autopilot", "repair/autopilot", false),
}

data class RepairService(
    val id: RepairServiceId,
    val titleRes: Int,
    val descriptionRes: Int,
    val scopeRes: Int,
    val requiresRes: Int,
)

object RepairCatalog {
    /** Order is the owner-defined signature order, used by the dashboard grid and the hero ring. */
    val services: List<RepairService> = listOf(
        RepairService(RepairServiceId.STORAGE, R.string.repair_service_storage,
            R.string.repair_service_storage_body, R.string.repair_scope_storage,
            R.string.repair_requires_none),
        RepairService(RepairServiceId.GHOST, R.string.repair_service_ghost,
            R.string.repair_service_ghost_body, R.string.repair_scope_files,
            R.string.repair_requires_saf),
        RepairService(RepairServiceId.DUPLICATE, R.string.repair_service_duplicate,
            R.string.repair_service_duplicate_body, R.string.repair_scope_files,
            R.string.repair_requires_saf),
        RepairService(RepairServiceId.HEAVYWEIGHT, R.string.repair_service_heavyweight,
            R.string.repair_service_heavyweight_body, R.string.repair_scope_files,
            R.string.repair_requires_saf),
        RepairService(RepairServiceId.CACHE, R.string.repair_service_cache,
            R.string.repair_service_cache_body, R.string.repair_scope_cache,
            R.string.repair_requires_none),
        RepairService(RepairServiceId.MEDIA, R.string.repair_service_media,
            R.string.repair_service_media_body, R.string.repair_scope_files,
            R.string.repair_requires_saf),
        RepairService(RepairServiceId.DOWNLOAD, R.string.repair_service_download,
            R.string.repair_service_download_body, R.string.repair_scope_download,
            R.string.repair_requires_saf),
        RepairService(RepairServiceId.SESSION, R.string.repair_service_session,
            R.string.repair_service_session_body, R.string.repair_scope_session,
            R.string.repair_requires_none),
        RepairService(RepairServiceId.NETWORK, R.string.repair_service_network,
            R.string.repair_service_network_body, R.string.repair_scope_network,
            R.string.repair_requires_none),
        RepairService(RepairServiceId.AUTOPILOT, R.string.repair_service_autopilot,
            R.string.repair_service_autopilot_body, R.string.repair_scope_autopilot,
            R.string.repair_requires_none),
    )

    private val byKey = services.associateBy { it.id.key }
    private val byRoute = services.associateBy { it.id.route }
    private val availableCount = services.count { it.id.available }

    fun service(id: RepairServiceId): RepairService = byKey.getValue(id.key)
    fun service(key: String): RepairService? = byKey[key]
    fun byRoute(route: String): RepairService? = byRoute[route]

    /** Used by the dashboard heading; measured, not a health score. */
    fun available(): Int = availableCount
}
