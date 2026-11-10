package com.knoux.kxprivatevpn

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.text.format.Formatter
import android.view.KeyEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.designsystem.Atmosphere
import com.knoux.kxprivatevpn.features.MediaSlimScreen
import com.knoux.kxprivatevpn.features.MediaSlimUiState
import com.knoux.kxprivatevpn.platform.media.CompressionOutcome
import com.knoux.kxprivatevpn.platform.media.MediaCandidate
import com.knoux.kxprivatevpn.platform.media.MediaFailure
import com.knoux.kxprivatevpn.platform.media.MediaQuality
import com.knoux.kxprivatevpn.platform.media.MediaSlimScan
import com.knoux.kxprivatevpn.platform.media.VerifiedCompression
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.Locale

/**
 * Media Slim contract: only re-encodable images can enter a confirmed batch, video and unknown
 * formats stay honestly visible but inert, and the day/night Arabic layouts keep every action.
 */
class MediaSlimScreenTest {
    @get:Rule val compose = createComposeRule()
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun candidate(
        id: String,
        name: String,
        bytes: Long,
        mime: String,
        parent: String = "tree",
    ) = MediaCandidate(
        documentId = id,
        parentDocumentId = parent,
        relativePath = "Camera/$name",
        name = name,
        mime = mime,
        bytes = bytes,
        lastModified = 0,
    )

    private val image = candidate("img", "portrait.jpg", 1_800_000, "image/jpeg")
    private val video = candidate("vid", "clip.mp4", 24_000_000, "video/mp4")
    private val gif = candidate("gif", "loop.gif", 900_000, "image/gif")
    private val scan = MediaSlimScan(
        files = listOf(video, image, gif),
        scannedFiles = 3,
        skippedUnknownSize = 0,
        atMillis = 1,
    )

    private fun localized(base: Context, language: String): Context {
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(language))
        return base.createConfigurationContext(configuration)
    }

    private fun render(
        initial: MediaSlimUiState = MediaSlimUiState(treeName = "Camera", treeUri = "tree://camera", scan = scan),
        arabic: Boolean = false,
        night: Boolean = false,
        fontScale: Float = 1f,
        onConfirmed: () -> Unit = {},
    ) {
        compose.setContent {
            var state by remember { mutableStateOf(initial) }
            val base = InstrumentationRegistry.getInstrumentation().targetContext
            val localizedContext = localized(base, if (arabic) "ar" else "en")
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedContext.resources.configuration,
                LocalDensity provides Density(density.density, fontScale),
                LocalLayoutDirection provides if (arabic) LayoutDirection.Rtl else LayoutDirection.Ltr,
            ) {
                val foreground = if (night) Color(0xFFF7F3FF) else Color(0xFF171050)
                MaterialTheme(colorScheme = if (night) darkColorScheme(primary = Color(0xFF99EBFF))
                    else lightColorScheme(primary = Color(0xFF4420B3))) {
                    CompositionLocalProvider(LocalContentColor provides foreground) {
                        Box(Modifier.fillMaxSize()) {
                            Atmosphere(night, 0f)
                            MediaSlimScreen(
                                night = night,
                                state = state,
                                onBack = {},
                                onChooseFolder = {},
                                onScan = {},
                                onCancelScan = {},
                                onToggle = { file ->
                                    val next = if (file.documentId in state.selected) {
                                        state.selected - file.documentId
                                    } else {
                                        state.selected + file.documentId
                                    }
                                    state = state.copy(selected = next, selectionError = false)
                                },
                                onSelectAll = {
                                    state = state.copy(selected = setOf(image.documentId))
                                },
                                onClear = { state = state.copy(selected = emptySet()) },
                                onQuality = { state = state.copy(quality = it) },
                                onRequestCompression = {
                                    if (state.selectedSupported.isEmpty()) {
                                        state = state.copy(selectionError = true)
                                    } else {
                                        state = state.copy(confirming = true)
                                    }
                                },
                                onDismissConfirmation = { state = state.copy(confirming = false) },
                                onConfirmCompression = {
                                    state = state.copy(confirming = false)
                                    onConfirmed()
                                },
                                onCancelCompression = {},
                                onResetReceipt = { state = state.copy(results = emptyList()) },
                            )
                        }
                    }
                }
            }
        }
    }

    /** The compose rule hosts no activity handle, so the IME is dismissed with a real back key. */
    private fun hideKeyboard() {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val output = File(context.getExternalFilesDir(null), "identity-evidence").apply { mkdirs() }
        File(output, "$name.png").outputStream().use { stream ->
            compose.onRoot().captureToImage().asAndroidBitmap()
                .compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
    }

    private fun captureDialog(name: String) {
        compose.waitForIdle()
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val output = File(context.getExternalFilesDir(null), "identity-evidence").apply { mkdirs() }
        File(output, "$name.png").outputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        bitmap.recycle()
    }

    @Test fun videoAndGifStayVisibleButCannotEnterTheBatch() {
        var confirmed = 0
        render(onConfirmed = { confirmed++ })
        val en = localized(context, "en")

        compose.onNodeWithTag("media-video-note").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("media-file-img").performScrollTo().assertIsEnabled()
        compose.onNodeWithTag("media-file-vid").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("media-file-gif").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("media-compress").assertDoesNotExist()

        // Selecting only the JPEG enables the confirmed batch; the video contributes no bytes.
        compose.onNodeWithTag("media-file-img").performScrollTo().performClick()
        compose.onNodeWithTag("media-selected-summary").performScrollTo().assertTextEquals(
            en.getString(R.string.media_selected, 1,
                Formatter.formatFileSize(en, image.bytes)),
        )
        compose.onNodeWithTag("media-folder-name").performScrollTo().assertIsDisplayed()
        capture("70a-media-day-top")
        compose.onNodeWithTag("media-compress").performScrollTo().assertIsEnabled().performClick()
        compose.onNodeWithText(en.getString(R.string.media_confirm_title)).assertExists()
        compose.onNodeWithTag("media-confirm").assertIsEnabled().performClick()
        assertEquals(1, confirmed)
        capture("70-media-selected-confirmed")
    }

    @Test fun unsupportedOnlySelectionCannotStartAndShowsHonestNotice() {
        render(initial = MediaSlimUiState(
            treeName = "Camera", treeUri = "tree://camera",
            scan = MediaSlimScan(listOf(video, gif), 2, 0),
        ))
        val en = localized(context, "en")

        compose.onNodeWithTag("media-file-vid").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("media-file-gif").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("media-compress").assertDoesNotExist()
        compose.onNodeWithTag("media-empty").assertDoesNotExist()
        compose.onNodeWithTag("media-originals-note").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(en.getString(R.string.media_originals_note)).assertExists()
    }

    @Test fun emptyScanExplainsTheScopeWithoutInventingFiles() {
        render(initial = MediaSlimUiState(
            treeName = "Empty", treeUri = "tree://empty",
            scan = MediaSlimScan(emptyList(), 0, 0),
        ))
        val en = localized(context, "en")

        compose.onNodeWithTag("media-empty").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("media-compress").assertDoesNotExist()
        compose.onNodeWithText(en.getString(R.string.media_empty)).assertExists()
    }

    @Test fun readableReceiptShowsMeasuredSavingsAndFailures() {
        val verified = CompressionOutcome.Verified(VerifiedCompression(
            candidate = image,
            outputDocumentId = "copy",
            outputName = "portrait-kx-q65.webp",
            outputBytes = 600_000,
            width = 1440,
            height = 1920,
        ))
        val failed = CompressionOutcome.Failed(
            candidate = candidate("big", "huge.png", 3_000_000, "image/png"),
            reason = MediaFailure.NO_SAVING,
        )
        render(initial = MediaSlimUiState(
            treeName = "Camera", treeUri = "tree://camera", scan = scan,
            results = listOf(verified, failed), completed = true, total = 2, currentIndex = 2,
        ))
        val en = localized(context, "en")

        compose.onNodeWithTag("media-result-summary").performScrollTo().assertTextEquals(
            en.getString(R.string.media_result_summary, 1,
                Formatter.formatFileSize(en, 1_200_000)))
        compose.onNodeWithTag("media-result-failed").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("media-new-run").performScrollTo().assertIsEnabled()
        capture("71-media-receipt")
    }

    @Test fun missingWriteGrantIsExplainedInsteadOfOfferingCompression() {
        render(initial = MediaSlimUiState(permissionError = true))
        val en = localized(context, "en")

        compose.onNodeWithTag("media-permission-error").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(en.getString(R.string.media_permission_required)).assertExists()
        compose.onNodeWithTag("media-folder").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("media-compress").assertDoesNotExist()
    }

    @Test fun nightArabicLargeTextKeepsScopeQualityAndConfirmationReachable() {
        val ar = localized(context, "ar")
        render(
            initial = MediaSlimUiState(
                treeName = "Camera", treeUri = "tree://camera", scan = scan,
                selected = setOf(image.documentId), quality = MediaQuality.SMALL,
            ),
            arabic = true, night = true, fontScale = 1.6f,
        )

        compose.onNodeWithTag("media-folder-name").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(ar.getString(R.string.media_scope)).performScrollTo().assertIsDisplayed()
        capture("72a-media-night-ar-large-top")

        compose.onNodeWithTag("media-selected-summary").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("media-compress").performScrollTo().assertIsDisplayed()
        capture("72-media-night-ar-large-selected")

        compose.onNodeWithTag("media-compress").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("media-confirm").assertExists()
        compose.onNodeWithText(ar.getString(R.string.media_confirm_title)).assertExists()
        compose.onNodeWithTag("media-confirm").assertIsEnabled()
        captureDialog("73-media-night-ar-large-confirm")
    }

    @Test fun reference01HeroHasRealMetricsAndConfirmedPrimaryAction() {
        render(initial = MediaSlimUiState(
            treeName = "Camera", treeUri = "tree://camera", scan = scan,
        ), arabic = true, night = true)
        compose.onNodeWithTag("route-media").assertExists()
        compose.onNodeWithTag("media-ref-hero").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("media-ref-metrics").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("media-ref-images").assertExists()
        compose.onNodeWithTag("media-ref-videos").assertExists()
        compose.onNodeWithTag("media-ref-folder").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("media-ref-primary-action").performScrollTo().assertIsNotEnabled()
        capture("01-media-reference-ar-night-unselected")
        compose.onNodeWithTag("media-file-img").performScrollTo().performClick()
        compose.onNodeWithTag("media-ref-primary-action").performScrollTo().assertIsEnabled()
            .performClick()
        compose.onNodeWithTag("media-confirm").assertExists()
    }

    @Test fun reference01UnscannedShowsNoFakeInventoryOrSavings() {
        render(initial = MediaSlimUiState(), night = true)
        compose.onNodeWithTag("media-ref-hero").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("media-ref-metrics").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("media-scan-summary").assertDoesNotExist()
        compose.onNodeWithTag("media-ref-primary-action").performScrollTo().assertIsEnabled()
        compose.onNodeWithTag("media-compress").assertDoesNotExist()
        capture("01-media-reference-en-night-empty")
    }

    @Test fun qualityChoiceIsExplicitAndAccessible() {
        render(initial = MediaSlimUiState(
            treeName = "Camera", treeUri = "tree://camera", scan = scan,
            selected = setOf(image.documentId), quality = MediaQuality.BALANCED,
        ))

        compose.onNodeWithTag("media-quality-high").performScrollTo().assertIsEnabled()
            .assertIsNotSelected().performClick()
        compose.onNodeWithTag("media-quality-high").assertIsSelected()
        compose.onNodeWithTag("media-quality-balanced").assertIsNotSelected()
        compose.onNodeWithTag("media-select-all").performScrollTo().assertIsEnabled()
        compose.onNodeWithTag("media-clear").assertIsEnabled().performClick()
        compose.onNodeWithTag("media-selected-summary").assertTextEquals(
            localized(context, "en").getString(R.string.media_selected, 0,
                Formatter.formatFileSize(localized(context, "en"), 0)))
    }

    // ---------------------------------------------------------------------------
    // Owner reference 17: measured results only
    // ---------------------------------------------------------------------------

    @Test fun reference17UnmeasuredRowsDoNotInventAfterBytesOrSavings() {
        val en = localized(context, "en")
        render(initial = MediaSlimUiState(
            treeName = "Camera", treeUri = "tree://camera", scan = scan,
        ))

        compose.onNodeWithTag("media-ref-hero").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("media-scope-images").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("media-scope-videos").assertIsDisplayed()
        compose.onNodeWithTag("media-savings-after").performScrollTo().assertTextEquals(
            en.getString(R.string.media_ref_not_measured))
        compose.onNodeWithTag("media-savings-percent").assertTextEquals(
            en.getString(R.string.media_ref_not_measured))
        compose.onNodeWithTag("media-file-img").performScrollTo().assertIsEnabled()
        compose.onNodeWithTag("media-row-after-img", useUnmergedTree = true).assertTextEquals(
            en.getString(R.string.media_ref_not_measured))
        compose.onNodeWithTag("media-row-saved-img", useUnmergedTree = true).assertDoesNotExist()
        capture("90-media-ref17-unmeasured")
    }

    @Test fun reference17SearchAndFiltersOnlyNarrowMeasuredRows() {
        val en = localized(context, "en")
        render(initial = MediaSlimUiState(
            treeName = "Camera", treeUri = "tree://camera", scan = scan,
        ))

        // The chip rows sit inside a vertical scroll and a nested horizontal scroll, so drive the
        // real semantics click action instead of injecting coordinates that can land off-viewport.
        compose.onNodeWithTag("media-search").performScrollTo()
        compose.onNodeWithTag("media-filter-videos")
            .performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithTag("media-filter-videos").assertIsSelected()
        compose.onNodeWithTag("media-file-vid").assertExists()
        compose.onNodeWithTag("media-file-img").assertDoesNotExist()
        compose.onNodeWithTag("media-filter-unsupported")
            .performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithTag("media-file-gif").assertExists()
        compose.onNodeWithTag("media-file-vid").assertDoesNotExist()
        compose.onNodeWithTag("media-filter-images")
            .performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithTag("media-file-img").assertExists()
        compose.onNodeWithTag("media-file-gif").assertExists()
        compose.onNodeWithTag("media-file-vid").assertDoesNotExist()
        compose.onNodeWithTag("media-filter-all")
            .performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithTag("media-file-vid").assertExists()
        compose.onNodeWithTag("media-sort-name")
            .performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithTag("media-sort-name").assertIsSelected()
        compose.onNodeWithTag("media-sort-size").assertIsNotSelected()

        compose.onNodeWithTag("media-search").performScrollTo().performTextInput("portrait")
        compose.onNodeWithTag("media-visible-count").assertTextEquals(
            en.getString(R.string.media_ref_visible, 1, 3))
        compose.onNodeWithTag("media-file-img").assertExists()
        compose.onNodeWithTag("media-file-vid").assertDoesNotExist()
        compose.onNodeWithTag("media-file-gif").assertDoesNotExist()
        hideKeyboard()
        compose.onNodeWithTag("media-file-img").performScrollTo().assertIsDisplayed()
        capture("92-media-ref17-search")

        compose.onNodeWithTag("media-search-clear").performScrollTo().performClick()
        compose.onNodeWithTag("media-visible-count").assertTextEquals(
            en.getString(R.string.media_ref_visible, 3, 3))
        compose.onNodeWithTag("media-filter-videos")
            .performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithTag("media-visible-count").assertTextEquals(
            en.getString(R.string.media_ref_visible, 1, 3))
        compose.onNodeWithTag("media-file-vid").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("media-file-img").assertDoesNotExist()
        compose.onNodeWithTag("media-file-gif").assertDoesNotExist()
        capture("92b-media-ref17-filtered")
    }

    @Test fun reference17MeasuredReceiptFeedsRowsAndSavingsWithoutEstimation() {
        val en = localized(context, "en")
        val verified = CompressionOutcome.Verified(VerifiedCompression(
            candidate = image,
            outputDocumentId = "copy",
            outputName = "portrait-kx-q65.webp",
            outputBytes = 600_000,
            width = 1440,
            height = 1920,
        ))
        render(initial = MediaSlimUiState(
            treeName = "Camera", treeUri = "tree://camera", scan = scan,
            selected = setOf(image.documentId), results = listOf(verified),
            completed = true, total = 1, currentIndex = 1,
        ))

        compose.onNodeWithTag("media-savings-after").performScrollTo().assertTextEquals(
            Formatter.formatFileSize(en, 600_000))
        compose.onNodeWithTag("media-savings-percent").assertTextEquals(
            en.getString(R.string.media_ref_saved_percent, 66))
        compose.onNodeWithTag("media-file-img").performScrollTo().assertIsOn()
        compose.onNodeWithTag("media-row-after-img", useUnmergedTree = true).assertTextEquals(
            Formatter.formatFileSize(en, 600_000))
        compose.onNodeWithTag("media-row-saved-img", useUnmergedTree = true).assertTextEquals(
            en.getString(R.string.media_ref_saved_percent, 66))
        compose.onNodeWithTag("media-result-summary").performScrollTo().assertIsDisplayed()
        capture("91-media-ref17-measured")
    }

    @Test fun reference17ArabicNightLargeTextKeepsMeasuredResultsReachable() {
        val ar = localized(context, "ar")
        val verified = CompressionOutcome.Verified(VerifiedCompression(
            candidate = image,
            outputDocumentId = "copy",
            outputName = "portrait-kx-q65.webp",
            outputBytes = 600_000,
            width = 1440,
            height = 1920,
        ))
        render(
            initial = MediaSlimUiState(
                treeName = "Camera", treeUri = "tree://camera", scan = scan,
                selected = setOf(image.documentId), results = listOf(verified),
                completed = true, total = 1, currentIndex = 1,
            ),
            arabic = true, night = true, fontScale = 1.6f,
        )

        compose.onNodeWithTag("media-ref-hero").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("media-scope-images").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("media-savings-after").performScrollTo()
            .assertTextEquals(Formatter.formatFileSize(ar, 600_000))
        compose.onNodeWithTag("media-file-img").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("media-row-after-img", useUnmergedTree = true).assertTextEquals(
            Formatter.formatFileSize(ar, 600_000))
        compose.onNodeWithTag("media-compress").performScrollTo().assertIsEnabled()
        capture("93-media-ref17-night-ar-large")
    }

    @Test fun reference17HasNoDeleteExportOrCompareAction() {
        val en = localized(context, "en")
        render(initial = MediaSlimUiState(
            treeName = "Camera", treeUri = "tree://camera", scan = scan,
            selected = setOf(image.documentId),
        ))

        compose.onNodeWithTag("media-delete").assertDoesNotExist()
        compose.onNodeWithTag("media-export").assertDoesNotExist()
        compose.onNodeWithTag("media-compare").assertDoesNotExist()
        compose.onNodeWithTag("media-ref-results-note").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(en.getString(R.string.media_ref_results_note)).assertExists()
        compose.onNodeWithTag("media-ref-action-note").performScrollTo().assertIsDisplayed()
        capture("94-media-ref17-actions")
    }
}
