package com.knoux.kxprivatevpn

import com.knoux.kxprivatevpn.features.StorageHealthAvailability
import com.knoux.kxprivatevpn.features.StorageHealthKind
import com.knoux.kxprivatevpn.features.StorageHealthSource
import com.knoux.kxprivatevpn.features.StorageHealthStatus
import com.knoux.kxprivatevpn.features.StorageXrayUiState
import com.knoux.kxprivatevpn.features.VolumeReading
import com.knoux.kxprivatevpn.features.storageHealth
import com.knoux.kxprivatevpn.platform.storage.MediaCategoryTotal
import com.knoux.kxprivatevpn.platform.storage.MediaInventory
import com.knoux.kxprivatevpn.platform.storage.StorageCategory
import com.knoux.kxprivatevpn.platform.storage.StorageEntry
import com.knoux.kxprivatevpn.platform.storage.StorageScan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Reference 07 has no approved health formula. These tests prove the summary never substitutes
 * zeroes, invented percentages or a fabricated score for an unavailable measurement.
 */
class StorageHealthModelTest {
    private val now = 1_760_000_000_000L
    private val gb = 1024L * 1024 * 1024

    private fun media(
        images: Long? = 2 * gb,
        videos: Long? = 1 * gb,
        audio: Long? = 500L * 1024 * 1024,
        documents: Long? = null,
        permitted: Boolean = true,
    ) = MediaInventory(listOf(
        MediaCategoryTotal(StorageCategory.IMAGES, images, 10, permitted),
        MediaCategoryTotal(StorageCategory.VIDEOS, videos, 3, permitted),
        MediaCategoryTotal(StorageCategory.AUDIO, audio, 4, permitted),
        MediaCategoryTotal(StorageCategory.DOCUMENTS, documents, 0, false),
    ), false, now)

    private fun state(
        volume: VolumeReading? = VolumeReading(64 * gb, 20 * gb, now),
        kxBytes: Long? = 5L * 1024 * 1024,
        kxLimited: Boolean = false,
        media: MediaInventory? = media(),
        scan: StorageScan? = null,
        measuring: Boolean = false,
        mediaLoading: Boolean = false,
        scanFailed: Boolean = false,
        scanning: Boolean = false,
    ) = StorageXrayUiState(
        loading = false,
        measuring = measuring,
        volume = volume,
        kxBytes = kxBytes,
        kxFiles = 3,
        kxLimited = kxLimited,
        media = media,
        mediaLoading = mediaLoading,
        scan = scan,
        scanning = scanning,
        scanFailed = scanFailed,
    )

    @Test fun unavailableMeasurementsStayNullAndNeverBecomeZero() {
        val health = storageHealth(state(volume = null, kxBytes = null, media = null),
            documentsAvailable = false)

        assertEquals(StorageHealthStatus.UNAVAILABLE, health.status)
        assertNull(health.totalBytes)
        assertTrue(health.categories.all { it.bytes == null })
        health.categories.forEach { category ->
            assertNull(health.fractionOf(category))
            assertNull(health.percentOf(category))
        }
    }

    @Test fun measuringStateIsLabeledWithoutInventingZeroBytes() {
        val health = storageHealth(
            state(volume = null, kxBytes = null, media = null, measuring = true),
            documentsAvailable = false,
        )

        assertEquals(StorageHealthStatus.MEASURING, health.status)
        assertTrue(health.categories.all { it.bytes == null })
        assertTrue(health.categories
            .filter { it.kind != StorageHealthKind.OTHER }
            .all { it.availability == StorageHealthAvailability.MEASURING })
        assertEquals(StorageHealthAvailability.NOT_MEASURED,
            health.categories.first { it.kind == StorageHealthKind.OTHER }.availability)
    }

    @Test fun mediaSubtotalSumsOnlyMeasuredCategoriesAndMarksPartial() {
        val partialMedia = media(videos = null)
        val health = storageHealth(state(media = partialMedia), documentsAvailable = false)
        val mediaRow = health.categories.first { it.kind == StorageHealthKind.MEDIA }

        assertEquals(2 * gb + 500L * 1024 * 1024, mediaRow.bytes)
        assertEquals(StorageHealthAvailability.PARTIAL, mediaRow.availability)
        assertEquals(StorageHealthSource.MEDIA_STORE, mediaRow.source)
        assertEquals(14, mediaRow.files)
    }

    @Test fun platformLimitedDocumentsUseAuthorizedFolderOnlyAndSaySo() {
        val scan = StorageScan(listOf(
            StorageEntry("docs/a.pdf", 3000, "application/pdf"),
            StorageEntry("misc/a.bin", 700, "application/octet-stream"),
        ), atMillis = now)
        val health = storageHealth(state(media = media(documents = null), scan = scan),
            documentsAvailable = false)

        val documents = health.categories.first { it.kind == StorageHealthKind.DOCUMENTS }
        val other = health.categories.first { it.kind == StorageHealthKind.OTHER }
        assertEquals(3000L, documents.bytes)
        assertEquals(StorageHealthSource.AUTHORIZED_FOLDER, documents.source)
        assertEquals(StorageHealthAvailability.MEASURED, documents.availability)
        assertEquals(700L, other.bytes)
        assertEquals(StorageHealthSource.AUTHORIZED_FOLDER, other.source)
    }

    @Test fun completedEmptyFolderScanIsMeasuredZeroNotUnavailable() {
        val health = storageHealth(
            state(media = media(documents = null), scan = StorageScan(emptyList(), atMillis = now)),
            documentsAvailable = false,
        )

        val documents = health.categories.first { it.kind == StorageHealthKind.DOCUMENTS }
        assertEquals(0L, documents.bytes)
        assertEquals(0, documents.files)
        assertEquals(StorageHealthAvailability.MEASURED, documents.availability)
        assertEquals(StorageHealthSource.AUTHORIZED_FOLDER, documents.source)
    }

    @Test fun truncatedKxAndScanAreLowerBoundsNotExactTotals() {
        val scan = StorageScan(listOf(
            StorageEntry("misc/a.bin", 700, "application/octet-stream"),
        ), truncated = true, atMillis = now)
        val health = storageHealth(
            state(kxBytes = 8192L, kxLimited = true, scan = scan),
            documentsAvailable = false,
        )

        val kx = health.categories.first { it.kind == StorageHealthKind.KX }
        val other = health.categories.first { it.kind == StorageHealthKind.OTHER }
        assertTrue(kx.lowerBound)
        assertTrue(other.lowerBound)
        assertEquals(StorageHealthAvailability.PARTIAL, other.availability)
    }

    @Test fun percentageIsRelativeToVolumeAndClamped() {
        val health = storageHealth(
            state(volume = VolumeReading(1000, 400, now), kxBytes = 250),
            documentsAvailable = false,
        )
        val kx = health.categories.first { it.kind == StorageHealthKind.KX }
        val mediaRow = health.categories.first { it.kind == StorageHealthKind.MEDIA }

        assertEquals(0.25f, health.fractionOf(kx))
        assertEquals(25, health.percentOf(kx))
        assertEquals(1f, health.fractionOf(mediaRow))
        assertEquals(100, health.percentOf(mediaRow))
        assertEquals(600L, health.usedBytes)
        assertEquals(400L, health.freeBytes)
    }

    @Test fun failedMediaQueryIsReportedAsFailedNotUnavailableZero() {
        val failed = MediaInventory(listOf(
            MediaCategoryTotal(StorageCategory.IMAGES, null, 0, true, failed = true),
            MediaCategoryTotal(StorageCategory.VIDEOS, null, 0, true, failed = true),
            MediaCategoryTotal(StorageCategory.AUDIO, null, 0, true, failed = true),
            MediaCategoryTotal(StorageCategory.DOCUMENTS, null, 0, false),
        ), false, now)
        val health = storageHealth(state(media = failed), documentsAvailable = false)
        val mediaRow = health.categories.first { it.kind == StorageHealthKind.MEDIA }

        assertEquals(StorageHealthAvailability.QUERY_FAILED, mediaRow.availability)
        assertNull(mediaRow.bytes)
    }
}
