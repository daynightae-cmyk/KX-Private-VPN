package com.knoux.kxprivatevpn

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
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
import java.io.File
import java.util.Locale

/**
 * Download Rescue may only report measured states, and it may only delete after the person
 * approved it. These tests pin both: the states that reach the screen come from real
 * measurements, and the destructive action is separated from the request that asks for it.
 */
class DownloadRescueScreenTest {
    @get:Rule val compose = createComposeRule()

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
            val english = localized(base, "en")
            CompositionLocalProvider(
                LocalContext provides english,
                LocalConfiguration provides english.resources.configuration,
                LocalLayoutDirection provides LayoutDirection.Ltr,
            ) {
                Box(Modifier.fillMaxSize()) {
                    DownloadRescueScreen(
                        night = false,
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
        val folder = File(context.getExternalFilesDir(null), "identity-evidence").apply { mkdirs() }
        File(folder, "$name.png").outputStream().use { stream ->
            check(compose.onRoot().captureToImage().asAndroidBitmap()
                .compress(Bitmap.CompressFormat.PNG, 100, stream))
        }
    }

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
}
