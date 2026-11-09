package com.knoux.kxprivatevpn.features

import com.knoux.kxprivatevpn.platform.storage.StorageCategory
import com.knoux.kxprivatevpn.platform.storage.StoragePermissions
import kotlin.math.roundToInt

/**
 * Honest status of the reference-07 summary panel.
 *
 * There is no approved storage-health formula in the repository, so this model never produces
 * a score. It only reports which real measurements are available and their share of the
 * measured internal volume.
 */
enum class StorageHealthStatus { MEASURING, MEASURED, PARTIAL, UNAVAILABLE }

enum class StorageHealthAvailability {
    MEASURED, PARTIAL, MEASURING, PERMISSION_REQUIRED, VERSION_LIMITED, QUERY_FAILED, NOT_MEASURED,
}

enum class StorageHealthSource { KX_PRIVATE, MEDIA_STORE, AUTHORIZED_FOLDER }

enum class StorageHealthKind { KX, MEDIA, DOCUMENTS, OTHER }

data class StorageHealthCategory(
    val kind: StorageHealthKind,
    val category: StorageCategory,
    val bytes: Long?,
    val files: Int?,
    val availability: StorageHealthAvailability,
    val source: StorageHealthSource,
    /** A truncated KX or SAF scan only proves at least this many bytes. */
    val lowerBound: Boolean = false,
)

data class StorageHealthSnapshot(
    val status: StorageHealthStatus,
    val totalBytes: Long?,
    val usedBytes: Long?,
    val freeBytes: Long?,
    val categories: List<StorageHealthCategory>,
) {
    fun fractionOf(category: StorageHealthCategory): Float? {
        val measured = category.bytes ?: return null
        val total = totalBytes ?: return null
        if (total <= 0L || measured < 0L) return null
        return (measured.toDouble() / total.toDouble()).coerceIn(0.0, 1.0).toFloat()
    }

    fun percentOf(category: StorageHealthCategory): Int? =
        fractionOf(category)?.let { (it * 100f).roundToInt() }
}

/**
 * @param documentsAvailable mirrors [StoragePermissions.documentsAvailable] on the running
 * platform. It is a parameter so the version-limited path is unit-testable without an emulator.
 */
fun storageHealth(
    state: StorageXrayUiState,
    documentsAvailable: Boolean = StoragePermissions.documentsAvailable(android.os.Build.VERSION.SDK_INT),
): StorageHealthSnapshot {
    val total = state.volume?.totalBytes?.takeIf { it > 0L }
    val free = state.volume?.availableBytes?.let { available ->
        if (total == null) available.coerceAtLeast(0L) else available.coerceIn(0L, total)
    }
    val used = if (total != null && free != null) total - free else null
    val busy = state.loading || state.measuring
    val mediaLoading = busy || state.mediaLoading
    val mediaCategories = state.media?.categories.orEmpty()
    val visualCategories = listOf(
        StorageCategory.IMAGES, StorageCategory.VIDEOS, StorageCategory.AUDIO,
    ).map { wanted -> mediaCategories.firstOrNull { it.category == wanted } }

    val kx = when {
        state.kxBytes != null -> StorageHealthCategory(
            StorageHealthKind.KX, StorageCategory.OTHER, state.kxBytes, state.kxFiles,
            StorageHealthAvailability.MEASURED, StorageHealthSource.KX_PRIVATE,
            lowerBound = state.kxLimited,
        )
        busy -> unavailable(StorageHealthKind.KX, StorageCategory.OTHER,
            StorageHealthAvailability.MEASURING, StorageHealthSource.KX_PRIVATE)
        else -> unavailable(StorageHealthKind.KX, StorageCategory.OTHER,
            StorageHealthAvailability.NOT_MEASURED, StorageHealthSource.KX_PRIVATE)
    }

    val measuredVisual = visualCategories.filter { it?.bytes != null }
    val media = when {
        measuredVisual.isNotEmpty() -> StorageHealthCategory(
            StorageHealthKind.MEDIA, StorageCategory.IMAGES,
            measuredVisual.sumOf { it?.bytes ?: 0L },
            measuredVisual.sumOf { it?.files ?: 0 },
            if (measuredVisual.size == visualCategories.size) StorageHealthAvailability.MEASURED
            else StorageHealthAvailability.PARTIAL,
            StorageHealthSource.MEDIA_STORE,
        )
        mediaLoading -> unavailable(StorageHealthKind.MEDIA, StorageCategory.IMAGES,
            StorageHealthAvailability.MEASURING, StorageHealthSource.MEDIA_STORE)
        mediaCategories.isEmpty() -> unavailable(StorageHealthKind.MEDIA, StorageCategory.IMAGES,
            StorageHealthAvailability.NOT_MEASURED, StorageHealthSource.MEDIA_STORE)
        visualCategories.any { it?.failed == true } -> unavailable(StorageHealthKind.MEDIA, StorageCategory.IMAGES,
            StorageHealthAvailability.QUERY_FAILED, StorageHealthSource.MEDIA_STORE)
        visualCategories.isNotEmpty() && visualCategories.none { it?.permitted == true } ->
            unavailable(StorageHealthKind.MEDIA, StorageCategory.IMAGES,
                StorageHealthAvailability.PERMISSION_REQUIRED, StorageHealthSource.MEDIA_STORE)
        else -> unavailable(StorageHealthKind.MEDIA, StorageCategory.IMAGES,
            StorageHealthAvailability.NOT_MEASURED, StorageHealthSource.MEDIA_STORE)
    }

    val documentsEntry = mediaCategories.firstOrNull { it.category == StorageCategory.DOCUMENTS }
    val scanDocuments = state.scan?.categories()?.firstOrNull { it.category == StorageCategory.DOCUMENTS }
    val documents = when {
        documentsEntry?.bytes != null -> StorageHealthCategory(
            StorageHealthKind.DOCUMENTS, StorageCategory.DOCUMENTS,
            documentsEntry.bytes, documentsEntry.files,
            if (documentsEntry.partial) StorageHealthAvailability.PARTIAL
            else StorageHealthAvailability.MEASURED,
            StorageHealthSource.MEDIA_STORE,
        )
        scanDocuments != null -> folderCategory(StorageHealthKind.DOCUMENTS, state, scanDocuments)
        mediaLoading || state.scanning ->
            unavailable(StorageHealthKind.DOCUMENTS, StorageCategory.DOCUMENTS,
                StorageHealthAvailability.MEASURING, StorageHealthSource.MEDIA_STORE)
        documentsEntry?.failed == true ->
            unavailable(StorageHealthKind.DOCUMENTS, StorageCategory.DOCUMENTS,
                StorageHealthAvailability.QUERY_FAILED, StorageHealthSource.MEDIA_STORE)
        !documentsAvailable ->
            unavailable(StorageHealthKind.DOCUMENTS, StorageCategory.DOCUMENTS,
                StorageHealthAvailability.VERSION_LIMITED, StorageHealthSource.MEDIA_STORE)
        documentsEntry != null && !documentsEntry.permitted ->
            unavailable(StorageHealthKind.DOCUMENTS, StorageCategory.DOCUMENTS,
                StorageHealthAvailability.PERMISSION_REQUIRED, StorageHealthSource.MEDIA_STORE)
        state.scanFailed ->
            unavailable(StorageHealthKind.DOCUMENTS, StorageCategory.DOCUMENTS,
                StorageHealthAvailability.QUERY_FAILED, StorageHealthSource.AUTHORIZED_FOLDER)
        else -> unavailable(StorageHealthKind.DOCUMENTS, StorageCategory.DOCUMENTS,
            StorageHealthAvailability.NOT_MEASURED, StorageHealthSource.AUTHORIZED_FOLDER)
    }

    val scanOther = state.scan?.categories()?.firstOrNull { it.category == StorageCategory.OTHER }
    val other = when {
        scanOther != null -> folderCategory(StorageHealthKind.OTHER, state, scanOther)
        state.scanning ->
            unavailable(StorageHealthKind.OTHER, StorageCategory.OTHER,
                StorageHealthAvailability.MEASURING, StorageHealthSource.AUTHORIZED_FOLDER)
        state.scanFailed ->
            unavailable(StorageHealthKind.OTHER, StorageCategory.OTHER,
                StorageHealthAvailability.QUERY_FAILED, StorageHealthSource.AUTHORIZED_FOLDER)
        else -> unavailable(StorageHealthKind.OTHER, StorageCategory.OTHER,
            StorageHealthAvailability.NOT_MEASURED, StorageHealthSource.AUTHORIZED_FOLDER)
    }

    val categories = listOf(kx, media, documents, other)
    val status = when {
        busy -> StorageHealthStatus.MEASURING
        total == null -> StorageHealthStatus.UNAVAILABLE
        categories.all { it.availability == StorageHealthAvailability.MEASURED } -> StorageHealthStatus.MEASURED
        else -> StorageHealthStatus.PARTIAL
    }
    return StorageHealthSnapshot(status, total, used, free, categories)
}

private fun unavailable(
    kind: StorageHealthKind,
    category: StorageCategory,
    availability: StorageHealthAvailability,
    source: StorageHealthSource,
) = StorageHealthCategory(kind, category, null, null, availability, source)

private fun folderCategory(
    kind: StorageHealthKind,
    state: StorageXrayUiState,
    total: com.knoux.kxprivatevpn.platform.storage.CategoryTotal,
): StorageHealthCategory = StorageHealthCategory(
    kind,
    total.category,
    total.bytes,
    total.files,
    if (state.scan?.cancelled == true || state.scan?.truncated == true) StorageHealthAvailability.PARTIAL
    else StorageHealthAvailability.MEASURED,
    StorageHealthSource.AUTHORIZED_FOLDER,
    lowerBound = state.scan?.truncated == true,
)
