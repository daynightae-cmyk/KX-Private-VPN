package com.knoux.kxprivatevpn

import android.content.Context
import android.content.res.Configuration
import android.text.format.Formatter
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.features.StorageXrayScreen
import com.knoux.kxprivatevpn.features.StorageXrayUiState
import com.knoux.kxprivatevpn.features.VolumeReading
import com.knoux.kxprivatevpn.platform.storage.MediaCategoryTotal
import com.knoux.kxprivatevpn.platform.storage.MediaInventory
import com.knoux.kxprivatevpn.platform.storage.StorageCategory
import com.knoux.kxprivatevpn.platform.storage.StorageEntry
import com.knoux.kxprivatevpn.platform.storage.StorageScan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Locale

/**
 * Storage X-Ray may only render real StatFs/MediaStore/SAF values. Missing permission, failed
 * queries and partial or cancelled scans each have their own honest state.
 */
class StorageXrayScreenTest {
    @get:Rule val compose = createComposeRule()

    /**
     * Rendering and assertions share one pinned English locale. Several fixtures are Latin
     * digits ("120", "4"), which an Arabic-locale device renders as Arabic-Indic — the test
     * must not fail on numeral shape alone.
     */
    private val base: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val context: Context
        get() = localized(base, "en")
    private val now = 1_760_000_000_000L

    private fun media(
        images: Long? = 2L * 1024 * 1024 * 1024,
        videos: Long? = 1L * 1024 * 1024 * 1024,
        audio: Long? = 500L * 1024 * 1024,
        imagesPermitted: Boolean = true,
        documentsPermitted: Boolean = false,
    ) = MediaInventory(listOf(
        MediaCategoryTotal(StorageCategory.IMAGES, images, 120, imagesPermitted),
        MediaCategoryTotal(StorageCategory.VIDEOS, videos, 5, imagesPermitted),
        MediaCategoryTotal(StorageCategory.AUDIO, audio, 8, imagesPermitted),
        MediaCategoryTotal(StorageCategory.DOCUMENTS, null, 0, documentsPermitted),
    ), false, now)

    private fun state(
        volume: VolumeReading? = VolumeReading(64L * 1024 * 1024 * 1024,
            20L * 1024 * 1024 * 1024, now),
        kxBytes: Long? = 5L * 1024 * 1024,
        media: MediaInventory? = media(),
        scan: StorageScan? = null,
        treeUri: String? = null,
        treeName: String? = null,
        scanning: Boolean = false,
        scanProgress: Int = 0,
        scanCancelled: Boolean = false,
        scanFailed: Boolean = false,
        pathStack: List<String> = emptyList(),
        mediaDenied: Boolean = false,
    ) = StorageXrayUiState(
        loading = false,
        volume = volume,
        kxBytes = kxBytes,
        kxFiles = 3,
        media = media,
        mediaDenied = mediaDenied,
        scan = scan,
        scanning = scanning,
        scanProgress = scanProgress,
        scanCancelled = scanCancelled,
        scanFailed = scanFailed,
        treeUri = treeUri,
        treeName = treeName,
        pathStack = pathStack,
    )

    private fun scan(truncated: Boolean = false, cancelled: Boolean = false) = StorageScan(listOf(
        StorageEntry("DCIM/a.jpg", 1000, "image/jpeg"),
        StorageEntry("DCIM/b.jpg", 2000, "image/jpeg"),
        StorageEntry("docs/c.pdf", 3000, "application/pdf"),
        StorageEntry("top.bin", 500, "application/octet-stream"),
    ), truncated, cancelled, now)

    private fun text(id: Int, vararg args: Any): String = context.getString(id, *args)

    private fun localized(base: Context, language: String): Context {
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(language))
        return base.createConfigurationContext(configuration)
    }

    private fun screen(
        state: StorageXrayUiState,
        night: Boolean = false,
        arabic: Boolean = false,
        fontScale: Float = 1f,
        onBack: () -> Unit = {},
        onRefresh: () -> Unit = {},
        onRequestMedia: () -> Unit = {},
        onOpenSettings: () -> Unit = {},
        onChooseFolder: () -> Unit = {},
        onScan: () -> Unit = {},
        onCancel: () -> Unit = {},
        onForget: () -> Unit = {},
        onEnter: (String) -> Unit = {},
        onUp: () -> Unit = {},
    ) {
        compose.setContent {
            val localizedContext = if (arabic) localized(base, "ar") else localized(base, "en")
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedContext.resources.configuration,
                LocalDensity provides Density(density.density, fontScale),
                LocalLayoutDirection provides if (arabic) LayoutDirection.Rtl else LayoutDirection.Ltr,
            ) {
                Box(Modifier.fillMaxSize()) {
                    StorageXrayScreen(
                        night = night,
                        state = state,
                        onBack = onBack,
                        onRefresh = onRefresh,
                        onRequestMedia = onRequestMedia,
                        onOpenSettings = onOpenSettings,
                        onChooseFolder = onChooseFolder,
                        onScan = onScan,
                        onCancel = onCancel,
                        onForget = onForget,
                        onEnter = onEnter,
                        onUp = onUp,
                    )
                }
            }
        }
    }

    private fun capture(name: String) {
        captureEvidence(context, compose, name)
    }

    @Test fun measuredVolumeKxAndMediaRenderAsMeasured() {
        screen(state())

        compose.onNodeWithTag("storage-measured-at").assertExists()
        compose.onNodeWithTag("storage-kx")
            .assertTextContains(Formatter.formatFileSize(context, 5L * 1024 * 1024), substring = true)
        compose.onNodeWithTag("storage-category-images")
            .assertTextContains("120", substring = true)
        compose.onNodeWithTag("storage-category-documents")
            .assertTextContains(text(R.string.storage_category_version_limited), substring = true)
        val accessible = 2L * 1024 * 1024 * 1024 + 1024 * 1024 * 1024 +
            500L * 1024 * 1024 + 5L * 1024 * 1024
        compose.onNodeWithTag("storage-accessible").assertTextEquals(
            text(R.string.storage_reconcile_accessible,
                Formatter.formatFileSize(context, accessible)))
        compose.onNodeWithTag("storage-outside").assertExists()
        compose.onNodeWithTag("storage-media-permission").assertDoesNotExist()
        compose.onNodeWithTag("storage-choose-folder").assertExists()
        compose.onNodeWithTag("storage-read-only").assertExists()
        capture("48-storage-xray-day")
        compose.onNodeWithTag("storage-choose-folder").performScrollTo().assertIsDisplayed()
        capture("48b-storage-xray-folder-empty")
    }

    @Test fun referenceHeroShowsHonestScoreAndMeasuredStatus() {
        screen(state())

        compose.onNodeWithTag("storage-health-hero").assertExists()
        compose.onNodeWithTag("storage-health-panel").assertExists()
        compose.onNodeWithTag("storage-health-score")
            .assertTextEquals(text(R.string.storage_ref_score_unavailable))
        compose.onNodeWithTag("storage-health-status")
            .assertTextEquals(text(R.string.storage_ref_status_partial))
        compose.onNodeWithTag("storage-refresh").assertExists()
        compose.onAllNodesWithText("A5").assertCountEquals(0)
        capture("67-storage-health-hero")
    }

    @Test fun healthRowsShowMeasuredBytesAndVolumeShare() {
        screen(state())

        compose.onNodeWithTag("storage-health-value-kx").assertTextContains(
            Formatter.formatFileSize(context, 5L * 1024 * 1024), substring = true)
        compose.onNodeWithTag("storage-health-percent-kx").assertTextEquals(
            context.getString(R.string.storage_ref_less_than_one))
        compose.onNodeWithTag("storage-health-value-media").assertTextContains(
            Formatter.formatFileSize(context, 3L * 1024 * 1024 * 1024 + 500L * 1024 * 1024),
            substring = true)
        compose.onNodeWithTag("storage-health-percent-media").assertTextEquals("5%")
        compose.onNodeWithTag("storage-health-value-documents")
            .assertTextEquals(text(R.string.storage_category_version_limited))
        compose.onNodeWithTag("storage-health-percent-documents").assertTextEquals("—")
        compose.onNodeWithTag("storage-health-value-other")
            .assertTextEquals(text(R.string.storage_category_unavailable))
        // The panel sits below the hero, so scroll the last row into view before capturing.
        compose.onNodeWithTag("storage-health-row-other").performScrollTo().assertIsDisplayed()
        capture("68-storage-health-measured")
    }

    @Test fun unavailableVolumeShowsDashedRingsNotInventedZeroes() {
        screen(state(volume = null, kxBytes = null, media = null))

        compose.onNodeWithTag("storage-health-status")
            .assertTextEquals(text(R.string.storage_ref_status_unavailable))
        compose.onNodeWithTag("storage-health-score")
            .assertTextEquals(text(R.string.storage_ref_score_unavailable))
        compose.onNodeWithTag("storage-health-percent-kx").assertTextEquals("—")
        compose.onNodeWithTag("storage-health-percent-media").assertTextEquals("—")
        compose.onAllNodesWithText("0%").assertCountEquals(0)
        capture("69-storage-health-unavailable")
    }

    @Test fun startAnalysisRunsTheRealRefreshCallback() {
        var refreshes = 0
        screen(state(), onRefresh = { refreshes++ })

        compose.onNodeWithTag("storage-refresh").performScrollTo().assertIsDisplayed().performClick()
        assertEquals(1, refreshes)
    }

    @Test fun measuringStateShowsProgressAndDisablesTheAction() {
        screen(state(volume = null, kxBytes = null, media = null).copy(measuring = true))

        compose.onNodeWithTag("storage-health-measuring").assertExists()
        compose.onNodeWithTag("storage-health-status")
            .assertTextEquals(text(R.string.storage_ref_status_measuring))
        compose.onNodeWithText(text(R.string.storage_ref_analyzing)).assertExists()
        compose.onNodeWithTag("storage-refresh").assertIsNotEnabled()
    }

    @Test fun missingMediaPermissionShowsEducationNotZeroBytes() {
        val denied = MediaInventory(listOf(
            MediaCategoryTotal(StorageCategory.IMAGES, null, 0, false),
            MediaCategoryTotal(StorageCategory.VIDEOS, null, 0, false),
            MediaCategoryTotal(StorageCategory.AUDIO, null, 0, false),
            MediaCategoryTotal(StorageCategory.DOCUMENTS, null, 0, false),
        ), false, now)
        var requested = 0
        screen(state(media = denied), onRequestMedia = { requested++ })

        compose.onNodeWithTag("storage-media-permission-text")
            .assertTextEquals(text(R.string.storage_media_permission_needed))
        compose.onNodeWithTag("storage-media-permission").performScrollTo().performClick()
        assertEquals(1, requested)
        compose.onNodeWithTag("storage-category-images")
            .assertTextContains(text(R.string.storage_category_not_permitted), substring = true)
        compose.onNodeWithTag("storage-accessible").assertExists()
        capture("49-storage-xray-permission")
    }

    @Test fun deniedMediaOffersSettingsAndFolderScan() {
        val denied = MediaInventory(listOf(
            MediaCategoryTotal(StorageCategory.IMAGES, null, 0, false),
            MediaCategoryTotal(StorageCategory.VIDEOS, null, 0, false),
            MediaCategoryTotal(StorageCategory.AUDIO, null, 0, false),
            MediaCategoryTotal(StorageCategory.DOCUMENTS, null, 0, false),
        ), false, now)
        var settings = 0
        var folder = 0
        screen(state(media = denied, mediaDenied = true),
            onOpenSettings = { settings++ }, onChooseFolder = { folder++ })

        compose.onNodeWithTag("storage-media-settings").performScrollTo().performClick()
        compose.onNodeWithTag("storage-choose-folder").performScrollTo().performClick()
        assertEquals(1, settings)
        assertEquals(1, folder)
    }

    @Test fun folderScanShowsCategoriesAndDrillsDown() {
        var entered: String? = null
        var up = 0
        screen(state(scan = scan(), treeUri = "content://tree", treeName = "Downloads"),
            onEnter = { entered = it }, onUp = { up++ })

        compose.onNodeWithTag("storage-tree-name").performScrollTo().assertTextEquals("Downloads")
        compose.onNodeWithTag("storage-scan-summary").performScrollTo()
            .assertTextContains("4", substring = true)
        compose.onNodeWithTag("storage-folder-DCIM").performScrollTo().performClick()
        assertEquals("DCIM", entered)
        compose.onNodeWithTag("storage-pane-up").assertDoesNotExist()
        capture("50-storage-xray-folder-scan")
    }

    @Test fun drilledDownPaneShowsBreadcrumbFilesAndUp() {
        var up = 0
        screen(state(scan = scan(), treeUri = "content://tree", treeName = "Downloads",
            pathStack = listOf("DCIM")), onUp = { up++ })

        compose.onNodeWithTag("storage-breadcrumb").performScrollTo()
            .assertTextContains("DCIM", substring = true)
        compose.onNodeWithText("a.jpg").assertExists()
        compose.onNodeWithText("b.jpg").assertExists()
        compose.onNodeWithTag("storage-pane-up").performScrollTo().performClick()
        assertEquals(1, up)
    }

    @Test fun partialAndCancelledScansAreLabeled() {
        screen(state(scan = scan(truncated = true, cancelled = true),
            treeUri = "content://tree", treeName = "Downloads", scanCancelled = true))

        compose.onNodeWithTag("storage-scan-cancelled").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("storage-scan-partial").performScrollTo().assertIsDisplayed()
    }

    @Test fun failedScanAndFailedVolumeDoNotInventValues() {
        screen(state(volume = VolumeReading(null, null, now), kxBytes = null,
            media = null, treeUri = "content://tree", treeName = "Downloads", scanFailed = true))

        compose.onNodeWithTag("storage-scan-failed").performScrollTo().assertIsDisplayed()
        assertTrue(compose.onAllNodesWithText(text(R.string.storage_volume_unavailable))
            .fetchSemanticsNodes().isNotEmpty())
        compose.onNodeWithText(text(R.string.storage_reconcile_unavailable)).assertExists()
        compose.onNodeWithTag("storage-accessible").assertDoesNotExist()
    }

    @Test fun nightArabicLargeTextKeepsMeasurementsVisible() {
        val ar = localized(context, "ar")
        screen(state(scan = scan(), treeUri = "content://tree", treeName = "Downloads"),
            night = true, arabic = true, fontScale = 1.6f)

        compose.onNodeWithText(ar.getString(R.string.storage_xray_title)).assertExists()
        compose.onNodeWithTag("storage-kx").assertExists()
        compose.onNodeWithTag("storage-category-images").assertExists()
        compose.onNodeWithTag("storage-health-panel").assertExists()
        compose.onNodeWithTag("storage-health-score")
            .assertTextEquals(ar.getString(R.string.storage_ref_score_unavailable))
        capture("70-storage-health-night-ar-large")
        compose.onNodeWithTag("storage-health-row-other").performScrollTo().assertIsDisplayed()
        capture("70b-storage-health-night-ar-panel")
    }
}
