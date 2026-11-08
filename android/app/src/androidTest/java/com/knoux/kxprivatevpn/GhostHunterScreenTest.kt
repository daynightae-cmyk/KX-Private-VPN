package com.knoux.kxprivatevpn

import android.content.Context
import android.content.res.Configuration
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
import com.knoux.kxprivatevpn.features.GhostHunterScreen
import com.knoux.kxprivatevpn.features.GhostUiState
import com.knoux.kxprivatevpn.platform.repair.GhostCandidate
import com.knoux.kxprivatevpn.platform.repair.GhostCleanupReceipt
import com.knoux.kxprivatevpn.platform.repair.GhostConfidence
import com.knoux.kxprivatevpn.platform.repair.GhostKind
import com.knoux.kxprivatevpn.platform.repair.GhostReason
import com.knoux.kxprivatevpn.platform.repair.GhostScan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Locale

/**
 * Ghost Hunter may only offer evidence-backed candidates, must label confidence, and must never
 * delete without selection plus confirmation. Provider failures stay visible.
 */
class GhostHunterScreenTest {
    @get:Rule val compose = createComposeRule()
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val now = 1_760_000_000_000L

    private fun zero(id: String, path: String) = GhostCandidate(
        id, path, GhostKind.ZERO_BYTE_FILE, "text/plain",
        GhostConfidence.HIGH, GhostReason.REPORTED_ZERO_BYTES,
    )

    private fun empty(id: String, path: String) = GhostCandidate(
        id, path, GhostKind.EMPTY_DIRECTORY, "vnd.android.document/directory",
        GhostConfidence.MEDIUM, GhostReason.NO_CHILDREN,
    )

    private fun scan(truncated: Boolean = false, cancelled: Boolean = false) = GhostScan(
        listOf(zero("zero1", "Download/zero.txt"), zero("zero2", "Download/old.tmp"),
            empty("dir1", "Download/Empty")),
        scannedFiles = 12, scannedDirectories = 4, excludedSentinels = 1,
        truncated = truncated, cancelled = cancelled, atMillis = now,
    )

    private fun state(
        scan: GhostScan? = scan(),
        selected: Set<String> = emptySet(),
        writable: Boolean = true,
        scanning: Boolean = false,
        scanCancelled: Boolean = false,
        scanFailed: Boolean = false,
        confirming: Boolean = false,
        cleaning: Boolean = false,
        receipt: GhostCleanupReceipt? = null,
        journalFailed: Boolean = false,
        treeUri: String? = "content://tree",
        treeName: String? = "Downloads",
    ) = GhostUiState(
        treeName = treeName, treeUri = treeUri, writable = writable, scanning = scanning,
        scan = scan, scanCancelled = scanCancelled, scanFailed = scanFailed, selected = selected,
        confirming = confirming, cleaning = cleaning, receipt = receipt,
        journalFailed = journalFailed,
    )

    private fun text(id: Int, vararg args: Any): String = context.getString(id, *args)

    private fun localized(base: Context, language: String): Context {
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(language))
        return base.createConfigurationContext(configuration)
    }

    private fun screen(
        state: GhostUiState,
        night: Boolean = false,
        arabic: Boolean = false,
        fontScale: Float = 1f,
        onBack: () -> Unit = {},
        onChooseFolder: () -> Unit = {},
        onScan: () -> Unit = {},
        onCancelScan: () -> Unit = {},
        onToggle: (GhostCandidate) -> Unit = {},
        onSelectAll: () -> Unit = {},
        onClearSelection: () -> Unit = {},
        onRequestCleanup: () -> Unit = {},
        onDismissCleanup: () -> Unit = {},
        onConfirmCleanup: () -> Unit = {},
        onCancelCleanup: () -> Unit = {},
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
                    GhostHunterScreen(
                        night = night,
                        state = state,
                        onBack = onBack,
                        onChooseFolder = onChooseFolder,
                        onScan = onScan,
                        onCancelScan = onCancelScan,
                        onToggle = onToggle,
                        onSelectAll = onSelectAll,
                        onClearSelection = onClearSelection,
                        onRequestCleanup = onRequestCleanup,
                        onDismissCleanup = onDismissCleanup,
                        onConfirmCleanup = onConfirmCleanup,
                        onCancelCleanup = onCancelCleanup,
                    )
                }
            }
        }
    }

    private fun capture(name: String) {
        captureEvidence(context, compose, name)
    }

    @Test fun evidenceAndConfidenceAreRenderedWithSelection() {
        var toggled: GhostCandidate? = null
        screen(state(selected = setOf("zero1")), onToggle = { toggled = it })

        compose.onNodeWithTag("ghost-zero-count").performScrollTo().assertTextEquals("2")
        compose.onNodeWithTag("ghost-dir-count").assertTextEquals("1")
        assertTrue(compose.onAllNodesWithText(text(R.string.ghost_reason_zero))
            .fetchSemanticsNodes().isNotEmpty())
        compose.onNodeWithText(text(R.string.ghost_reason_empty)).assertExists()
        assertTrue(compose.onAllNodesWithText(text(R.string.ghost_confidence_high))
            .fetchSemanticsNodes().isNotEmpty())
        compose.onNodeWithText(text(R.string.ghost_confidence_medium)).assertExists()
        compose.onNodeWithTag("ghost-selected").assertTextEquals(text(R.string.ghost_selected, 1))
        compose.onNodeWithTag("ghost-excluded").assertExists()
        compose.onNodeWithTag("ghost-candidate-zero2").performScrollTo().performClick()
        assertEquals("zero2", toggled?.documentId)
        compose.onNodeWithTag("ghost-cleanup").performScrollTo().assertIsEnabled()
        capture("53-ghost-evidence")
    }

    @Test fun cleanupRequiresExplicitConfirmation() {
        var confirmed = 0
        var dismissed = 0
        screen(state(selected = setOf("zero1", "zero2"), confirming = true),
            onConfirmCleanup = { confirmed++ }, onDismissCleanup = { dismissed++ })

        compose.onNodeWithText(text(R.string.ghost_dialog_title)).assertExists()
        compose.onNodeWithText(text(R.string.ghost_dialog_body, 2)).assertExists()
        compose.onNodeWithTag("ghost-confirm").performClick()
        assertEquals(1, confirmed)
        assertEquals(0, dismissed)
    }

    @Test fun arabicCleanupDialogKeepsArabicCopy() {
        val arabic = localized(context, "ar")
        screen(state(selected = setOf("zero1", "zero2"), confirming = true), arabic = true)
        compose.onNodeWithText(arabic.getString(R.string.ghost_dialog_title)).assertExists()
        compose.onNodeWithText(arabic.getString(R.string.ghost_dialog_body, 2)).assertExists()
        compose.onNodeWithText(arabic.getString(R.string.ghost_confirm)).assertExists()
        compose.onNodeWithText(arabic.getString(R.string.ghost_cleanup_cancel)).assertExists()
    }

    @Test fun selectAllAndClearAreRealActions() {
        var all = 0
        var clear = 0
        screen(state(selected = emptySet()), onSelectAll = { all++ }, onClearSelection = { clear++ })
        compose.onNodeWithTag("ghost-select-all").performScrollTo().performClick()
        compose.onNodeWithTag("ghost-clear").performClick()
        assertEquals(1, all)
        assertEquals(1, clear)
    }

    @Test fun readOnlyTreeDisablesCleanupAndOffersRegrant() {
        var regrant = 0
        screen(state(writable = false, selected = setOf("zero1")), onChooseFolder = { regrant++ })
        compose.onNodeWithTag("ghost-read-only").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("ghost-cleanup-read-only").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("ghost-cleanup").assertDoesNotExist()
        compose.onNodeWithTag("ghost-regrant").performScrollTo().performClick()
        assertEquals(1, regrant)
        capture("54-ghost-read-only")
    }

    @Test fun emptyScanHasAnHonestEmptyState() {
        screen(state(scan = GhostScan(emptyList(), 0, 1, 0, atMillis = now)))
        compose.onNodeWithTag("ghost-empty").performScrollTo().assertIsDisplayed()
    }

    @Test fun noAuthorizedTreeAsksForAFolder() {
        screen(state(scan = null, treeUri = null, treeName = null))
        compose.onNodeWithTag("ghost-choose-folder").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(text(R.string.ghost_tree_none)).assertExists()
    }

    @Test fun cancelledPartialAndFailedScansAreLabeled() {
        screen(state(scan = scan(truncated = true, cancelled = true), scanCancelled = true,
            scanFailed = true))
        compose.onNodeWithTag("ghost-cancelled").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("ghost-partial").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("ghost-scan-failed").performScrollTo().assertIsDisplayed()
    }

    @Test fun receiptReportsVerifiedDeletionsAndPartialState() {
        val receipt = GhostCleanupReceipt(2, listOf("kept.txt"), emptyList(), 2, complete = false)
        screen(state(receipt = receipt, journalFailed = true, selected = emptySet()))
        compose.onNodeWithTag("ghost-receipt").performScrollTo()
            .assertTextEquals(text(R.string.ghost_receipt, 2, 1, 0))
        compose.onNodeWithText(text(R.string.ghost_receipt_verified, 2)).assertExists()
        compose.onNodeWithTag("ghost-receipt-partial").assertExists()
        compose.onNodeWithTag("ghost-journal-failed").performScrollTo().assertIsDisplayed()
    }

    @Test fun cleaningStateIsVisibleAndCancellable() {
        var cancelled = 0
        screen(state(cleaning = true, selected = setOf("zero1")), onCancelCleanup = { cancelled++ })
        compose.onNodeWithTag("ghost-cleaning").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("ghost-cleanup-cancel").performScrollTo().performClick()
        assertEquals(1, cancelled)
    }

    @Test fun nightArabicLargeTextKeepsEvidenceReadable() {
        val ar = localized(context, "ar")
        screen(state(selected = setOf("zero1")), night = true, arabic = true, fontScale = 1.6f)
        compose.onNodeWithText(ar.getString(R.string.repair_service_ghost)).assertExists()
        compose.onNodeWithTag("ghost-zero-count").assertExists()
        capture("55-ghost-night-ar-large")
    }
}
