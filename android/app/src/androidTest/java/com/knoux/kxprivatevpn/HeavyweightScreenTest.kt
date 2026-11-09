package com.knoux.kxprivatevpn

import android.content.Context
import android.content.res.Configuration
import android.text.format.Formatter
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.features.HeavyweightScreen
import com.knoux.kxprivatevpn.features.HeavyweightUiState
import com.knoux.kxprivatevpn.platform.repair.HeavyweightScan
import com.knoux.kxprivatevpn.platform.repair.LargeFile
import com.knoux.kxprivatevpn.platform.repair.LargeFileCategory
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.Locale

/**
 * Large Files must show only measured sizes, rank them largest first, label partial or cancelled
 * scans truthfully, and never expose a destructive action.
 */
class HeavyweightScreenTest {
    @get:Rule val compose = createComposeRule()
    private var hostView: View? = null
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val now = 1_760_000_000_000L

    private fun file(
        id: String,
        name: String,
        bytes: Long,
        mime: String,
        modified: Long = now,
    ) = LargeFile(
        documentId = id,
        relativePath = "Folder/$name",
        name = name,
        mime = mime,
        bytes = bytes,
        lastModified = modified,
    )

    private fun files() = listOf(
        file("movie", "movie.mp4", 100 * MIB, "video/mp4"),
        file("backup", "backup.zip", 50 * MIB, "application/zip"),
        file("photo", "photo.jpg", 6 * MIB, "image/jpeg"),
        file("manual", "manual.pdf", 2 * MIB, "application/pdf"),
    )

    private fun scan(
        items: List<LargeFile> = files(),
        truncated: Boolean = false,
        cancelled: Boolean = false,
        failed: Boolean = false,
        unknown: Int = 0,
    ) = HeavyweightScan(
        files = items,
        scannedFiles = 14,
        largeFileCount = items.size + unknown,
        totalLargeBytes = items.sumOf { it.bytes },
        skippedUnknownSize = unknown,
        minimumBytes = FLOOR_BYTES,
        truncated = truncated,
        cancelled = cancelled,
        failed = failed,
        atMillis = now,
    )

    private fun state(
        scan: HeavyweightScan? = scan(),
        scanning: Boolean = false,
        scanCancelled: Boolean = false,
        scanFailed: Boolean = false,
        minimumBytes: Long = 0L,
        category: LargeFileCategory? = null,
        selectedDocumentId: String? = null,
        openFailed: Boolean = false,
        treeUri: String? = "content://tree",
        treeName: String? = "Downloads",
    ) = HeavyweightUiState(
        treeName = treeName,
        treeUri = treeUri,
        scanning = scanning,
        scan = scan,
        scanCancelled = scanCancelled,
        scanFailed = scanFailed,
        minimumBytes = minimumBytes,
        category = category,
        selectedDocumentId = selectedDocumentId,
        openFailed = openFailed,
    )

    private fun text(id: Int, vararg args: Any): String = context.getString(id, *args)

    private fun localized(base: Context, language: String): Context {
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(language))
        return base.createConfigurationContext(configuration)
    }

    private fun screen(
        state: HeavyweightUiState,
        night: Boolean = false,
        arabic: Boolean = false,
        fontScale: Float = 1f,
        onChooseFolder: () -> Unit = {},
        onScan: () -> Unit = {},
        onCancelScan: () -> Unit = {},
        onMinimumBytes: (Long) -> Unit = {},
        onCategory: (LargeFileCategory?) -> Unit = {},
        onSelect: (LargeFile) -> Unit = {},
        onOpen: (LargeFile) -> Unit = {},
    ) {
        compose.setContent {
            val base = InstrumentationRegistry.getInstrumentation().targetContext
            val localizedContext = if (arabic) localized(base, "ar") else base
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedContext.resources.configuration,
                LocalDensity provides Density(density.density, fontScale),
                LocalLayoutDirection provides if (arabic) LayoutDirection.Rtl else LayoutDirection.Ltr,
            ) {
                Box(Modifier.fillMaxSize()) {
                    HeavyweightScreen(
                        night = night,
                        state = state,
                        onBack = {},
                        onChooseFolder = onChooseFolder,
                        onScan = onScan,
                        onCancelScan = onCancelScan,
                        onMinimumBytes = onMinimumBytes,
                        onCategory = onCategory,
                        onSelect = onSelect,
                        onOpen = onOpen,
                    )
                }
            }
        }
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        captureEvidence(context, compose, name)
    }

    /**
     * Hides the platform IME through the host window token. Never send a back key here: on a
     * headless CI emulator `isAcceptingText` can be true while no IME owns the back press, which
     * finishes the test activity and leaves no Compose hierarchy to capture.
     */
    private fun hideKeyboard() {
        val view = hostView ?: return
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
        compose.waitForIdle()
    }

    @Test fun measuredRankingAndSummaryAreRenderedTruthfully() {
        screen(state())
        capture("60-heavy-reference-hero")
        compose.onNodeWithTag("heavy-scan-counts").performScrollTo()
        compose.onNodeWithTag("heavy-total-size").assertTextEquals(
            Formatter.formatFileSize(context, files().sumOf { it.bytes }))
        compose.onNodeWithTag("heavy-shown").assertTextEquals("4")
        compose.onNodeWithTag("heavy-file-0").performScrollTo()
        compose.onNodeWithTag("heavy-file-name-0", useUnmergedTree = true)
            .assertTextEquals("movie.mp4")
        compose.onNodeWithTag("heavy-file-name-1", useUnmergedTree = true)
            .assertTextEquals("backup.zip")
        compose.onNodeWithTag("heavy-read-only").assertExists()
        compose.onNodeWithTag("heavy-tree-count").assertTextEquals(
            text(R.string.heavy_ref_checked, 14))
        compose.onNodeWithTag("heavy-file-0").performScrollTo()
        capture("60d-heavy-reference-rows")
        compose.onNodeWithTag("heavy-filters").performScrollTo()
        capture("60b-heavy-reference-results")
        capture("59-heavy-ranking")
    }

    @Test fun idleFolderReadyShowsTheRankActionAndReferenceHero() {
        var scans = 0
        screen(
            state(scan = null, treeUri = "content://tree", treeName = "Downloads"),
            onScan = { scans++ },
        )
        compose.onNodeWithTag("heavy-ref-back").performScrollTo()
        capture("60e-heavy-reference-idle")
        compose.onNodeWithTag("heavy-scan").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("heavy-scan").performClick()
        assertEquals(1, scans)
    }

    @Test fun sizeAndCategoryFiltersNarrowWithoutRescanning() {
        var size by mutableStateOf(0L)
        var category by mutableStateOf<LargeFileCategory?>(null)
        val base = state()
        compose.setContent {
            HeavyweightScreen(
                night = false,
                state = base.copy(minimumBytes = size, category = category),
                onBack = {},
                onChooseFolder = {},
                onScan = {},
                onCancelScan = {},
                onMinimumBytes = { size = it },
                onCategory = { category = it },
                onSelect = {},
                onOpen = {},
            )
        }

        compose.onNodeWithTag("heavy-minimum-card").performScrollTo()
        compose.onNodeWithTag("heavy-minimum-select").performClick()
        compose.onNodeWithTag("heavy-filter-size-3").performClick()
        compose.onNodeWithTag("heavy-minimum-value", useUnmergedTree = true)
            .assertTextEquals(text(R.string.heavy_filter_100))
        compose.onNodeWithTag("heavy-filters").performScrollTo()
        compose.onNodeWithTag("heavy-visible-count").assertTextEquals(text(R.string.heavy_visible, 1))
        compose.onNodeWithTag("heavy-file-0").performScrollTo()
        compose.onNodeWithTag("heavy-file-name-0", useUnmergedTree = true)
            .assertTextEquals("movie.mp4")
        compose.onNodeWithTag("heavy-file-1").assertDoesNotExist()

        compose.onNodeWithTag("heavy-minimum-card").performScrollTo()
        compose.onNodeWithTag("heavy-minimum-select").performClick()
        compose.onNodeWithTag("heavy-filter-size-any").performClick()
        compose.onNodeWithTag("heavy-filters").performScrollTo()
        compose.onNodeWithTag("heavy-filter-category-image").performClick()
        compose.onNodeWithTag("heavy-filter-category-image").assertIsSelected()
        compose.onNodeWithTag("heavy-visible-count").assertTextEquals(text(R.string.heavy_visible, 1))
        compose.onNodeWithTag("heavy-file-0").performScrollTo()
        compose.onNodeWithTag("heavy-file-name-0", useUnmergedTree = true)
            .assertTextEquals("photo.jpg")
        compose.onNodeWithTag("heavy-no-match").assertDoesNotExist()
    }

    @Test fun searchFiltersMeasuredResultsLocallyWithoutAnotherScan() {
        var scans = 0
        val base = state()
        compose.setContent {
            val view = LocalView.current
            SideEffect { hostView = view }
            HeavyweightScreen(
                night = false,
                state = base,
                onBack = {},
                onChooseFolder = {},
                onScan = { scans++ },
                onCancelScan = {},
                onMinimumBytes = {},
                onCategory = {},
                onSelect = {},
                onOpen = {},
            )
        }

        compose.onNodeWithTag("heavy-search").performScrollTo().performTextInput("backup")
        compose.onNodeWithTag("heavy-visible-count").assertTextEquals(text(R.string.heavy_visible, 1))
        compose.onNodeWithTag("heavy-file-0").performScrollTo()
        compose.onNodeWithTag("heavy-file-name-0", useUnmergedTree = true)
            .assertTextEquals("backup.zip")
        compose.onNodeWithTag("heavy-file-1").assertDoesNotExist()
        hideKeyboard()
        capture("60c-heavy-reference-search")

        compose.onNodeWithTag("heavy-search-clear").performClick()
        compose.onNodeWithTag("heavy-visible-count").assertTextEquals(text(R.string.heavy_visible, 4))
        assertEquals(0, scans)
    }

    @Test fun sortChoicesReorderOnlyTheMeasuredRows() {
        val dated = listOf(
            file("movie", "movie.mp4", 100 * MIB, "video/mp4", 1_760_000_300_000L),
            file("backup", "backup.zip", 50 * MIB, "application/zip", 1_760_000_200_000L),
            file("photo", "photo.jpg", 6 * MIB, "image/jpeg", 1_760_000_400_000L),
            file("manual", "manual.pdf", 2 * MIB, "application/pdf", 1_760_000_100_000L),
        )
        screen(state(scan = scan(items = dated)))

        compose.onNodeWithTag("heavy-sort-name").performScrollTo().performClick()
        compose.onNodeWithTag("heavy-file-name-0", useUnmergedTree = true)
            .assertTextEquals("backup.zip")
        compose.onNodeWithTag("heavy-sort-date").performClick()
        compose.onNodeWithTag("heavy-file-name-0", useUnmergedTree = true)
            .assertTextEquals("photo.jpg")
        compose.onNodeWithTag("heavy-sort-size").performClick()
        compose.onNodeWithTag("heavy-file-name-0", useUnmergedTree = true)
            .assertTextEquals("movie.mp4")
    }

    @Test fun readOnlyContractExposesOpenButNoDestructiveAction() {
        screen(state())
        compose.onNodeWithTag("heavy-open-0").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("heavy-delete").assertDoesNotExist()
        compose.onNodeWithTag("heavy-move").assertDoesNotExist()
        compose.onNodeWithTag("heavy-compress").assertDoesNotExist()
        compose.onNodeWithTag("heavy-rename").assertDoesNotExist()
    }

    @Test fun selectionShowsDetailsAndOpenIsTheOnlyFileAction() {
        var selected by mutableStateOf<String?>(null)
        var opened: LargeFile? = null
        val base = state()
        compose.setContent {
            HeavyweightScreen(
                night = false,
                state = base.copy(selectedDocumentId = selected),
                onBack = {},
                onChooseFolder = {},
                onScan = {},
                onCancelScan = {},
                onMinimumBytes = {},
                onCategory = {},
                onSelect = { selected = it.documentId },
                onOpen = { opened = it },
            )
        }

        compose.onNodeWithTag("heavy-file-2").performScrollTo().performClick()
        compose.onNodeWithTag("heavy-selected-card").performScrollTo().assertExists()
        compose.onNodeWithTag("heavy-selected-path")
            .assertTextEquals(text(R.string.heavy_file_path, "Folder/photo.jpg"))
        compose.onNodeWithTag("heavy-selected-open").performScrollTo().assertIsDisplayed()
        capture("59b-heavy-selected-card")
        compose.onNodeWithTag("heavy-open-2").performScrollTo().performClick()
        assertEquals("photo", opened?.documentId)
        capture("59b-heavy-selected")
    }

    @Test fun heatmapTilesSelectTheSameMeasuredFile() {
        var selected: LargeFile? = null
        screen(state(), onSelect = { selected = it })
        compose.onNodeWithTag("heavy-heat-1").performScrollTo().performClick()
        assertEquals("backup", selected?.documentId)
    }

    @Test fun emptyScanHasAnHonestEmptyState() {
        screen(state(scan = scan(items = emptyList())))
        compose.onNodeWithTag("heavy-empty").performScrollTo().assertIsDisplayed()
    }

    @Test fun categoryFilterWithoutMatchesSaysSo() {
        screen(state(category = LargeFileCategory.AUDIO))
        compose.onNodeWithTag("heavy-no-match").performScrollTo().assertIsDisplayed()
    }

    @Test fun partialUnknownAndCancelledScansAreLabelled() {
        screen(state(scan = scan(truncated = true, cancelled = true, failed = true, unknown = 3),
            scanCancelled = true, scanFailed = true))
        compose.onNodeWithTag("heavy-partial").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("heavy-unknown").assertExists()
        compose.onNodeWithTag("heavy-cancelled").assertExists()
        compose.onNodeWithTag("heavy-summary-failed").assertExists()
    }

    @Test fun openFailureIsReportedWithoutTouchingTheFile() {
        screen(state(selectedDocumentId = "movie", openFailed = true))
        compose.onNodeWithTag("heavy-open-failed").performScrollTo().assertIsDisplayed()
    }

    @Test fun noAuthorizedTreeAsksForAFolder() {
        screen(state(scan = null, treeUri = null, treeName = null))
        compose.onNodeWithTag("heavy-choose-folder").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(text(R.string.heavy_tree_none)).assertExists()
    }

    @Test fun scanningStateIsVisibleAndCancellable() {
        var cancelled = 0
        screen(state(scanning = true), onCancelScan = { cancelled++ })
        compose.onNodeWithTag("heavy-scan-progress").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("heavy-scan-cancel").performScrollTo().performClick()
        assertEquals(1, cancelled)
    }

    @Test fun nightArabicLargeTextKeepsTheRankingLegible() {
        val ar = localized(context, "ar")
        screen(state(selectedDocumentId = "photo"), night = true, arabic = true, fontScale = 1.6f)
        compose.onNodeWithText(ar.getString(R.string.repair_service_heavyweight)).assertExists()
        capture("59c-heavy-night-ar-large")
        compose.onNodeWithTag("heavy-file-0").performScrollTo().assertIsDisplayed()
        capture("59d-heavy-night-ar-results")
    }
}

private const val MIB = 1024L * 1024L
private const val FLOOR_BYTES = 1024L * 1024L
