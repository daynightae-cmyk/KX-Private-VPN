package com.knoux.kxprivatevpn

import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
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
import com.knoux.kxprivatevpn.features.DownloadRescueScreen
import com.knoux.kxprivatevpn.features.DownloadRescueUiState
import com.knoux.kxprivatevpn.features.DownloadReceipt
import com.knoux.kxprivatevpn.platform.repair.DownloadAction
import com.knoux.kxprivatevpn.platform.repair.DownloadActionResult
import com.knoux.kxprivatevpn.platform.repair.DownloadCandidate
import com.knoux.kxprivatevpn.platform.repair.DownloadFormat
import com.knoux.kxprivatevpn.platform.repair.DownloadHealth
import com.knoux.kxprivatevpn.platform.repair.DownloadTarget
import com.knoux.kxprivatevpn.platform.storage.downloadDocumentIdentity
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.Locale

/**
 * Download Rescue may only report measured states, and it may only delete after the person
 * approved it. These tests pin both: the states that reach the screen come from real
 * measurements, and the destructive action is separated from the request that asks for it.
 */
class DownloadRescueScreenTest {
    @get:Rule val compose = createComposeRule()
    private var hostView: View? = null

    private val base: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val context: Context
        get() = localized(base, "en")

    private fun localized(base: Context, language: String): Context {
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(language))
        return base.createConfigurationContext(configuration)
    }

    private fun candidate(
        id: String,
        name: String,
        bytes: Long,
        health: DownloadHealth,
        reason: String,
        format: DownloadFormat? = DownloadFormat.PDF,
    ) = DownloadCandidate(
        documentId = id,
        displayName = name,
        mime = format?.mime ?: "application/octet-stream",
        bytes = bytes,
        lastModified = 0,
        health = health,
        format = format,
        recognisedByMime = format != null,
        reason = reason,
    )

    private fun screen(
        state: DownloadRescueUiState,
        language: String = "en",
        night: Boolean = false,
        fontScale: Float = 1f,
        onBack: () -> Unit = {},
        onChooseFolder: () -> Unit = {},
        onScan: () -> Unit = {},
        onForget: () -> Unit = {},
        onToggle: (String) -> Unit = {},
        onSelectAttention: () -> Unit = {},
        onClearSelection: () -> Unit = {},
        onRequestRemove: () -> Unit = {},
        onDismissConfirmation: () -> Unit = {},
        onConfirmRemove: () -> Unit = {},
        onExport: () -> Unit = {},
        onRename: (String) -> Unit = {},
    ) {
        compose.setContent {
            val localizedContext = localized(base, language)
            val density = LocalDensity.current
            val view = LocalView.current
            SideEffect { hostView = view }
            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedContext.resources.configuration,
                LocalDensity provides Density(density.density, fontScale),
                LocalLayoutDirection provides
                    if (language == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr,
            ) {
                Box(Modifier.fillMaxSize()) {
                    DownloadRescueScreen(
                        night = night,
                        state = state,
                        onBack = onBack,
                        onChooseFolder = onChooseFolder,
                        onScan = onScan,
                        onForget = onForget,
                        onToggle = onToggle,
                        onSelectAttention = onSelectAttention,
                        onClearSelection = onClearSelection,
                        onRequestRemove = onRequestRemove,
                        onDismissConfirmation = onDismissConfirmation,
                        onConfirmRemove = onConfirmRemove,
                        onExport = onExport,
                        onRename = onRename,
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
     * Hides the platform IME through the host window token. A back key can finish the host
     * activity on a headless emulator, so the window token is the only safe route.
     */
    private fun hideKeyboard() {
        val view = hostView ?: return
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
        compose.waitForIdle()
    }

    /** Seven measured rows with the mix shown by the owner results reference. */
    private fun referenceCandidates() = listOf(
        candidate(
            "kx", "KX_Setup_v3.2.6.apk", 82_700_000L,
            DownloadHealth.TRUNCATED, "missing_tail", DownloadFormat.ZIP,
        ),
        candidate(
            "chrome", "Chrome_v120.0.6099.116.apk", 1_240_000_000L,
            DownloadHealth.CORRUPTED, "bad_header", DownloadFormat.ZIP,
        ),
        candidate(
            "wa", "WhatsApp_v2.24.25.71.apk", 218_900_000L,
            DownloadHealth.TRUNCATED, "missing_tail", DownloadFormat.ZIP,
        ),
        candidate(
            "telegram", "Telegram_v10.9.2.apk", 71_800_000L,
            DownloadHealth.TRUNCATED, "missing_tail", DownloadFormat.ZIP,
        ),
        candidate(
            "photo", "IMG_20261109_182233.png", 2_100_000L,
            DownloadHealth.INTACT, "verified", DownloadFormat.PNG,
        ),
        candidate(
            "empty", "resume-final.zip", 0L,
            DownloadHealth.ZERO_BYTE, "no_bytes", DownloadFormat.ZIP,
        ),
        candidate(
            "notes", "notes.txt", 12_000L,
            DownloadHealth.UNSUPPORTED, "no_validator", null,
        ),
    )

    @Test fun theScopeAndEveryMeasuredStateAreShown() {
        screen(
            DownloadRescueUiState(
                loading = false,
                treeName = "Download",
                treeUri = "content://tree/download",
                candidates = listOf(
                    candidate("a", "good.pdf", 2048L, DownloadHealth.INTACT, "verified"),
                    candidate("b", "cut.pdf", 900L, DownloadHealth.TRUNCATED, "missing_tail"),
                    candidate("c", "empty.pdf", 0L, DownloadHealth.ZERO_BYTE, "no_bytes"),
                    candidate("d", "thing.bin", 10L, DownloadHealth.UNSUPPORTED, "no_validator", null),
                ),
            ),
        )
        compose.onNodeWithTag("download-scope").assertExists()
        compose.onNodeWithTag("download-count").assertTextContains("4", substring = true)
        compose.onNodeWithTag("download-attention").assertTextContains("3", substring = true)
        compose.onNodeWithTag("download-name-0").assertTextEquals("good.pdf")
        compose.onNodeWithTag("download-health-0").assertTextContains("Intact", substring = true)
        compose.onNodeWithTag("download-health-1").assertTextContains("Incomplete", substring = true)
        compose.onNodeWithTag("download-reason-1").assertTextContains("interrupted", substring = true)
        compose.onNodeWithTag("download-health-2").assertTextContains("Empty", substring = true)
        compose.onNodeWithTag("download-health-3").assertTextContains("No validator", substring = true)
        compose.onNodeWithTag("download-no-repair").assertExists()
        capture("74-download-rescue-states")
    }

    @Test fun withoutAFolderTheScopeIsEmptyAndNoCountIsInvented() {
        screen(DownloadRescueUiState(loading = false))
        compose.onNodeWithTag("download-count").assertTextContains("0", substring = true)
        compose.onNodeWithTag("download-attention").assertTextContains("0", substring = true)
        compose.onNodeWithTag("download-choose-folder").assertExists()
        compose.onNodeWithTag("download-scan").assertDoesNotExist()
    }

    @Test fun anEmptyFolderIsAnEmptyResultNotAFailure() {
        screen(DownloadRescueUiState(loading = false, treeName = "Download", treeUri = "content://tree/d"))
        compose.onNodeWithTag("download-empty").assertExists()
        compose.onNodeWithTag("download-failed").assertDoesNotExist()
        compose.onNodeWithTag("download-count").assertTextContains("0", substring = true)
    }

    @Test fun removalNeedsAnExplicitConfirmation() {
        var requested = 0
        var confirmed = 0
        val state = DownloadRescueUiState(
            loading = false,
            treeName = "Download",
            treeUri = "content://tree/download",
            candidates = listOf(candidate("a", "cut.pdf", 900L, DownloadHealth.TRUNCATED, "missing_tail")),
            selected = setOf("a"),
        )
        screen(state, onRequestRemove = { requested++ }, onConfirmRemove = { confirmed++ })
        compose.onNodeWithTag("download-remove").performScrollTo().performClick()
        assertEquals("asking for removal must not remove anything", 0, confirmed)
        assertEquals(1, requested)
    }

    @Test fun theConfirmationDialogOffersCancelAndAnExplicitRemoval() {
        var confirmed = 0
        var dismissed = 0
        screen(
            DownloadRescueUiState(
                loading = false,
                treeName = "Download",
                treeUri = "content://tree/download",
                candidates = listOf(candidate("a", "cut.pdf", 900L, DownloadHealth.TRUNCATED, "missing_tail")),
                selected = setOf("a"),
                removalTargets = listOf(DownloadTarget(
                    candidate("a", "cut.pdf", 900L, DownloadHealth.TRUNCATED, "missing_tail"),
                    "content://fixture/document/a",
                )),
            ),
            onConfirmRemove = { confirmed++ },
            onDismissConfirmation = { dismissed++ },
        )
        compose.onNodeWithTag("download-confirm").assertExists()
        compose.onNodeWithTag("download-cancel").assertExists()
        compose.onNodeWithTag("download-cancel").performClick()
        assertEquals(0, confirmed)
        assertEquals(1, dismissed)
    }

    @Test fun arabicConfirmationDialogKeepsArabicCopy() {
        val arabic = localized(base, "ar")
        val file = candidate("a", "cut.pdf", 900L, DownloadHealth.TRUNCATED, "missing_tail")
        screen(
            DownloadRescueUiState(
                loading = false,
                treeName = "Download",
                treeUri = "content://tree/download",
                candidates = listOf(file),
                selected = setOf("a"),
                removalTargets = listOf(DownloadTarget(file, "content://fixture/document/a")),
            ),
            language = "ar",
        )
        compose.onNodeWithText(arabic.getString(R.string.download_confirm_title)).assertExists()
        compose.onNodeWithText(arabic.getString(R.string.download_confirm)).assertExists()
        compose.onNodeWithText(arabic.getString(R.string.download_cancel)).assertExists()
    }

    @Test fun arabicRenameDialogKeepsArabicCopy() {
        val arabic = localized(base, "ar")
        screen(
            DownloadRescueUiState(
                loading = false,
                treeName = "Download",
                treeUri = "content://tree/download",
                candidates = listOf(candidate("a", "cut.pdf", 900L, DownloadHealth.TRUNCATED, "missing_tail")),
                selected = setOf("a"),
            ),
            language = "ar",
        )
        compose.onNodeWithTag("download-rename").performScrollTo().performClick()
        compose.onNodeWithText(arabic.getString(R.string.download_rename_title)).assertExists()
        compose.onNodeWithText(arabic.getString(R.string.download_rename_hint)).assertExists()
        compose.onNodeWithText(arabic.getString(R.string.download_rename_confirm)).assertExists()
    }

    @Test fun theReceiptReportsWhatActuallyHappened() {
        val receipt = DownloadReceipt(
            action = DownloadAction.REMOVE,
            attempted = 2,
            succeeded = 1,
            failed = 1,
            bytesCopied = 0L,
            bytesRemoved = 900L,
            results = listOf(
                DownloadActionResult("a", "cut.pdf", DownloadAction.REMOVE, true, message = "removed"),
                DownloadActionResult("b", "empty.pdf", DownloadAction.REMOVE, false, message = "delete_rejected"),
            ),
            atMillis = 1_760_000_000_000L,
        )
        screen(DownloadRescueUiState(loading = false, treeName = "Download", treeUri = "content://tree/d", receipt = receipt))
        compose.onNodeWithTag("download-receipt").assertExists()
        compose.onNodeWithTag("download-receipt-summary").assertTextContains("1", substring = true)
        compose.onNodeWithText("cut.pdf — removed").assertExists()
        compose.onNodeWithText("empty.pdf — delete_rejected").assertExists()
    }

    private fun exportState(selected: Set<String>) = DownloadRescueUiState(
        loading = false,
        candidates = listOf(
            candidate("a", "one.pdf", 1024L, DownloadHealth.INTACT, "verified"),
            candidate("b", "two.pdf", 2048L, DownloadHealth.TRUNCATED, "missing_tail"),
        ),
        selected = selected,
    )

    @Test fun exportIsDisabledWithNoSelection() {
        screen(exportState(emptySet()))
        compose.onNodeWithTag("download-export").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("download-export-hint").assertTextContains("exactly one", substring = true)
    }

    @Test fun exportIsDisabledForMultipleFilesAndExplainsTheLimit() {
        screen(exportState(setOf("a", "b")))
        compose.onNodeWithTag("download-export").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("download-export-hint").assertTextContains("exactly one", substring = true)
        assertTrue(localized(base, "ar").getString(R.string.download_export_hint).contains("واحدًا فقط"))
    }

    @Test fun oneIdleSelectionCanRequestTheDocumentPicker() {
        var requested = 0
        screen(exportState(setOf("a")), onExport = { requested++ })
        compose.onNodeWithTag("download-export").performScrollTo().assertIsEnabled().performClick()
        assertEquals(1, requested)
    }

    @Test fun pendingPickerDisablesAnotherExportAndOtherMutations() {
        screen(exportState(setOf("a")).copy(exportPending = true))
        compose.onNodeWithTag("download-export").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("download-remove").assertIsNotEnabled()
        compose.onNodeWithTag("download-rename").assertIsNotEnabled()
        compose.onNodeWithTag("download-select-0").assertIsNotEnabled()
    }

    @Test fun scanningDisablesExportEvenWithOneSelectedFile() {
        screen(exportState(setOf("a")).copy(scanning = true))
        compose.onNodeWithTag("download-export").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("download-remove").assertIsNotEnabled()
    }

    @Test fun theConfirmationDescribesFrozenTargetsNotANewSelection() {
        val original = candidate("a", "one.pdf", 1024L, DownloadHealth.INTACT, "verified")
        screen(exportState(setOf("b")).copy(
            removalTargets = listOf(DownloadTarget(original, "content://fixture/document/a")),
        ))
        compose.onNodeWithText(context.getString(R.string.download_confirm_body, 1, "1 KB")).assertExists()
        compose.onNodeWithTag("download-confirm").assertIsEnabled()
    }

    @Test fun renameReceiptRemainsReadableDuringAutomaticRescan() {
        val receipt = DownloadReceipt(DownloadAction.RENAME, 1, 1, 0, 0L, 0L,
            listOf(DownloadActionResult("a", "new.pdf", DownloadAction.RENAME, true, message = "renamed")), 1L)
        screen(DownloadRescueUiState(loading = false, receipt = receipt).startingScan(clearReceipt = false))
        compose.onNodeWithTag("download-receipt-summary").assertTextEquals("1 renamed, 0 failed")
    }

    @Test fun androidDocumentIdsIdentifyTreeAndStandaloneAliasesWithoutProviderAccess() {
        val standalone = downloadDocumentIdentity(Uri.parse("content://fixture/document/primary%3ADownload%2Fa.bin"))
        val tree = downloadDocumentIdentity(Uri.parse("content://fixture/tree/primary%3ADownload/document/primary%3ADownload%2Fa.bin"))
        assertEquals("primary:Download/a.bin", standalone.documentId)
        assertEquals(standalone.documentId, tree.documentId)
        assertTrue(standalone.sameDocument(tree))
    }

    @Test fun selectionIsOwnedByTheCallerAndCountsTheRealBytes() {
        var toggled = ""
        val state = DownloadRescueUiState(
            loading = false,
            treeName = "Download",
            treeUri = "content://tree/download",
            candidates = listOf(
                candidate("a", "one.pdf", 1024L, DownloadHealth.INTACT, "verified"),
                candidate("b", "two.pdf", 2048L, DownloadHealth.TRUNCATED, "missing_tail"),
            ),
        )
        screen(state, onToggle = { toggled = it })
        compose.onNodeWithTag("download-select-1").performScrollTo().performClick()
        assertEquals("b", toggled)
        compose.onNodeWithTag("download-selected").assertTextContains("0", substring = true)
    }

    // ---------------------------------------------------------------------------
    // Owner references 08 (hero) and 19 (measured results)
    // ---------------------------------------------------------------------------

    private fun referenceResultsState(selected: Set<String> = emptySet()) = DownloadRescueUiState(
        loading = false,
        treeName = "Download",
        treeUri = "content://tree/download",
        mediaStoreRows = 7,
        candidates = referenceCandidates(),
        selected = selected,
    )

    @Test fun referenceHeroShowsTheLocalStructuralCheckPromise() {
        screen(DownloadRescueUiState(loading = false))
        compose.onNodeWithTag("download-hero").assertExists()
        compose.onNodeWithText(context.getString(R.string.download_ref_hero_hint)).assertExists()
        compose.onNodeWithTag("download-choose-folder").assertExists()
        // No result, no selected file and no invented progress before a real scan.
        compose.onNodeWithTag("download-selection-bar").assertDoesNotExist()
        compose.onNodeWithTag("download-health-0").assertDoesNotExist()
        capture("80-download-rescue-hero")
    }

    @Test fun referenceResultsShowMeasuredSummaryFiltersAndSelectionBar() {
        var requested = 0
        screen(referenceResultsState(setOf("kx", "chrome", "wa")), onRequestRemove = { requested++ })
        compose.onNodeWithTag("download-summary-total").performScrollTo().assertTextEquals("7")
        compose.onNodeWithTag("download-summary-incomplete").assertTextEquals("3")
        compose.onNodeWithTag("download-summary-failed").assertTextEquals("2")
        compose.onNodeWithTag("download-visible-count").assertTextEquals("7 of 7 shown")
        compose.onNodeWithText(context.getString(R.string.download_ref_selected_chip, 3)).assertExists()
        compose.onNodeWithTag("download-selection-bar").assertExists()
        compose.onNodeWithTag("download-process").assertIsEnabled()
        capture("81-download-rescue-results-top")

        // The action panel repeats the measured receipt contract, not a fake progress claim.
        compose.onNodeWithTag("download-selected").performScrollTo()
            .assertTextContains("3 selected", substring = true)
        compose.onNodeWithTag("download-actions").assertExists()
        compose.onNodeWithTag("download-name-4").assertExists()
        capture("82-download-rescue-results-rows")

        // The bottom bar asks for confirmation; it never removes by itself.
        compose.onNodeWithTag("download-process").performClick()
        assertEquals(1, requested)
    }

    @Test fun measuredFilterChipsAreTouchable() {
        screen(referenceResultsState())
        compose.onNodeWithTag("route-download")
            .performScrollToNode(hasTestTag("download-filter-failed"))
        compose.onNodeWithTag("download-filter-failed").performClick()
        compose.onNodeWithTag("download-filter-failed").assertIsSelected()
        compose.onNodeWithTag("download-visible-count").assertTextEquals("2 of 7 shown")
    }

    @Test fun searchAndMeasuredFiltersNarrowTheListWithoutAScan() {
        var scans = 0
        screen(referenceResultsState(), onScan = { scans++ })
        compose.onNodeWithTag("download-search").performScrollTo().performTextInput("chrome")
        compose.onNodeWithTag("download-visible-count").assertTextEquals("1 of 7 shown")
        compose.onNodeWithTag("download-name-1").assertExists()
        compose.onNodeWithTag("download-name-0").assertDoesNotExist()

        // The IME is its own window: a tap on a chip behind it never reaches the app.
        hideKeyboard()
        compose.onNodeWithTag("download-search-clear").performClick()
        compose.onNodeWithTag("download-visible-count").assertTextEquals("7 of 7 shown")

        compose.onNodeWithTag("download-filter-failed").assertIsNotSelected()
        compose.onNodeWithTag("route-download")
            .performScrollToNode(hasTestTag("download-filter-failed"))
        compose.onNodeWithTag("download-filter-failed").performClick()
        compose.onNodeWithTag("download-filter-failed").assertIsSelected()
        compose.onNodeWithTag("download-visible-count").assertTextEquals("2 of 7 shown")
        compose.onNodeWithTag("download-name-1").assertExists()
        compose.onNodeWithTag("download-name-5").assertExists()
        compose.onNodeWithTag("download-name-0").assertDoesNotExist()
        assertEquals("search and filters must never trigger a provider rescan", 0, scans)
        capture("83-download-rescue-filtered")
    }

    @Test fun nightResultsKeepMeasuredContrast() {
        screen(referenceResultsState(setOf("chrome")), night = true)
        compose.onNodeWithTag("download-summary-total").performScrollTo().assertTextEquals("7")
        compose.onNodeWithTag("download-health-2").assertTextContains("Incomplete", substring = true)
        compose.onNodeWithTag("download-selection-bar").assertExists()
        capture("84-download-rescue-results-night")
    }

    @Test fun arabicResultsAreLocalizedAndReadRtl() {
        val arabic = localized(base, "ar")
        screen(referenceResultsState(), language = "ar")
        compose.onNodeWithTag("download-filter-all").performScrollTo()
            .assertTextContains(arabic.getString(R.string.download_ref_filter_all), substring = true)
        compose.onNodeWithTag("download-visible-count")
            .assertTextContains(arabic.getString(R.string.download_ref_visible, 7, 7))
        // In an RTL row the first metric is laid out on the right-hand side.
        val totalX = compose.onNodeWithTag("download-summary-total")
            .fetchSemanticsNode().boundsInRoot.center.x
        val failedX = compose.onNodeWithTag("download-summary-failed")
            .fetchSemanticsNode().boundsInRoot.center.x
        assertTrue("RTL should place the total metric right of the failed metric", totalX > failedX)
        capture("85-download-rescue-results-ar")
    }

    @Test fun largeFontKeepsMeasuredActionsReachable() {
        screen(referenceResultsState(setOf("kx")), fontScale = 2f)
        compose.onNodeWithTag("download-summary-total").performScrollTo().assertTextEquals("7")
        compose.onNodeWithTag("download-remove").performScrollTo().assertIsEnabled()
        compose.onNodeWithTag("download-process").assertIsEnabled()
        compose.onNodeWithTag("download-selection-bar").assertExists()
        capture("86-download-rescue-large-font")
    }
}
