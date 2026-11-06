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
import com.knoux.kxprivatevpn.features.DuplicateDnaScreen
import com.knoux.kxprivatevpn.features.DuplicateDnaUiState
import com.knoux.kxprivatevpn.platform.repair.DuplicateCleanupReceipt
import com.knoux.kxprivatevpn.platform.repair.DuplicateEntry
import com.knoux.kxprivatevpn.platform.repair.DuplicateGroup
import com.knoux.kxprivatevpn.platform.repair.DuplicateScan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Locale

/**
 * Instrumented Compose tests for Duplicate DNA screen.
 *
 * Verified: groups are rendered, selection works, cleanup requires explicit confirmation,
 * read-only tree shows honest degraded state, receipt is accurate, Arabic RTL renders.
 */
class DuplicateDnaScreenTest {
    @get:Rule val compose = createComposeRule()

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val now = 1_760_000_000_000L

    private fun makeEntry(id: String, path: String, bytes: Long, keep: Boolean) = DuplicateEntry(
        documentId = id,
        relativePath = path,
        bytes = bytes,
        lastModified = if (keep) 1_000L else 2_000L,
        sha256 = "abcd1234abcd1234abcd1234abcd1234abcd1234abcd1234abcd1234abcd1234",
        keep = keep,
    )

    private fun makeGroup(sha: String, bytes: Long, keepId: String, keepPath: String,
                          dupIds: List<Pair<String, String>>): DuplicateGroup {
        val entries = listOf(makeEntry(keepId, keepPath, bytes, true)) +
            dupIds.map { (id, path) -> makeEntry(id, path, bytes, false) }
        return DuplicateGroup(sha, bytes, entries)
    }

    private fun twoGroups() = listOf(
        makeGroup("aaaa1111aaaa1111aaaa1111aaaa1111aaaa1111aaaa1111aaaa1111aaaa1111",
            500_000, "keep1", "Photos/keep1.jpg",
            listOf("dup1a" to "Photos/dup1a.jpg", "dup1b" to "Downloads/dup1b.jpg")),
        makeGroup("bbbb2222bbbb2222bbbb2222bbbb2222bbbb2222bbbb2222bbbb2222bbbb2222",
            100_000, "keep2", "Documents/keep2.pdf",
            listOf("dup2a" to "Documents/dup2a.pdf")),
    )

    private fun scan(groups: List<DuplicateGroup> = twoGroups()) = DuplicateScan(
        groups = groups,
        scannedFiles = 40,
        hashedFiles = 10,
        truncated = false,
        cancelled = false,
        atMillis = now,
    )

    private fun state(
        scan: DuplicateScan? = scan(),
        selected: Set<String> = emptySet(),
        writable: Boolean = true,
        scanning: Boolean = false,
        scanCancelled: Boolean = false,
        scanFailed: Boolean = false,
        confirming: Boolean = false,
        cleaning: Boolean = false,
        receipt: DuplicateCleanupReceipt? = null,
        journalFailed: Boolean = false,
        cleanupFailed: Boolean = false,
        treeUri: String? = "content://tree",
        treeName: String? = "DCIM",
    ) = DuplicateDnaUiState(
        treeName = treeName, treeUri = treeUri, writable = writable, scanning = scanning,
        scan = scan, scanCancelled = scanCancelled, scanFailed = scanFailed,
        selectedDocumentIds = selected, confirming = confirming, cleaning = cleaning,
        receipt = receipt, journalFailed = journalFailed, cleanupFailed = cleanupFailed,
    )

    private fun text(id: Int, vararg args: Any): String = context.getString(id, *args)

    private fun localized(base: Context, language: String): Context {
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(language))
        return base.createConfigurationContext(configuration)
    }

    private fun screen(
        uiState: DuplicateDnaUiState,
        night: Boolean = false,
        arabic: Boolean = false,
        fontScale: Float = 1f,
        onBack: () -> Unit = {},
        onChooseFolder: () -> Unit = {},
        onScan: () -> Unit = {},
        onCancelScan: () -> Unit = {},
        onToggleEntry: (DuplicateEntry) -> Unit = {},
        onSelectAll: () -> Unit = {},
        onSelectGroup: (DuplicateGroup) -> Unit = {},
        onClearSelection: () -> Unit = {},
        onRequestCleanup: () -> Unit = {},
        onDismissCleanup: () -> Unit = {},
        onConfirmCleanup: () -> Unit = {},
        onCancelCleanup: () -> Unit = {},
    ) {
        compose.setContent {
            val base = InstrumentationRegistry.getInstrumentation().targetContext
            val localizedCtx = if (arabic) localized(base, "ar") else base
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalContext provides localizedCtx,
                LocalConfiguration provides localizedCtx.resources.configuration,
                LocalDensity provides Density(density.density, fontScale),
                LocalLayoutDirection provides if (arabic) LayoutDirection.Rtl else LayoutDirection.Ltr,
            ) {
                Box(Modifier.fillMaxSize()) {
                    DuplicateDnaScreen(
                        night = night, state = uiState, onBack = onBack,
                        onChooseFolder = onChooseFolder, onScan = onScan,
                        onCancelScan = onCancelScan, onToggleEntry = onToggleEntry,
                        onSelectAll = onSelectAll, onSelectGroup = onSelectGroup,
                        onClearSelection = onClearSelection, onRequestCleanup = onRequestCleanup,
                        onDismissCleanup = onDismissCleanup, onConfirmCleanup = onConfirmCleanup,
                        onCancelCleanup = onCancelCleanup,
                    )
                }
            }
        }
    }

    private fun capture(name: String) {
        captureEvidence(context, compose, name)
    }

    // ---------------------------------------------------------------------------
    // Tests
    // ---------------------------------------------------------------------------

    @Test fun groupsAndCountsAreRendered() {
        screen(state())
        compose.onNodeWithTag("dna-group-count").performScrollTo().assertTextEquals("2")
        compose.onNodeWithTag("dna-scan-counts").assertExists()
        compose.onNodeWithTag("dna-group-1").performScrollTo().assertExists()
        compose.onNodeWithTag("dna-group-2").performScrollTo().assertExists()
        compose.onNodeWithTag("dna-sha-1").performScrollTo().assertExists()
        capture("60-dna-groups")
    }

    @Test fun keepCopyIsMarkedAndNotCheckable() {
        screen(state(selected = emptySet()))
        compose.onNodeWithTag("dna-keep-keep1").performScrollTo().assertIsDisplayed()
        // The keep entry row should not have a Checkbox interaction role
        compose.onNodeWithTag("dna-entry-keep1").performScrollTo().assertHasNoClickAction()
    }

    @Test fun toggleDuplicateEntryCallsCallback() {
        var toggled: DuplicateEntry? = null
        screen(state(), onToggleEntry = { toggled = it })
        compose.onNodeWithTag("dna-entry-dup1a").performScrollTo().performClick()
        assertEquals("dup1a", toggled?.documentId)
    }

    @Test fun selectAllCallsCallback() {
        var called = 0
        screen(state(), onSelectAll = { called++ })
        compose.onNodeWithTag("dna-select-all").performScrollTo().performClick()
        assertEquals(1, called)
    }

    @Test fun selectGroupCallsCallbackWithCorrectGroup() {
        var groupReceived: DuplicateGroup? = null
        screen(state(), onSelectGroup = { groupReceived = it })
        compose.onNodeWithTag("dna-select-group-1").performScrollTo().performClick()
        assertEquals(twoGroups()[0].sha256, groupReceived?.sha256)
    }

    @Test fun cleanupRequiresExplicitConfirmation() {
        var confirmed = 0
        var dismissed = 0
        screen(
            state(selected = setOf("dup1a", "dup2a"), confirming = true),
            onConfirmCleanup = { confirmed++ },
            onDismissCleanup = { dismissed++ },
        )
        compose.onNodeWithText(text(R.string.dna_dialog_title)).assertExists()
        compose.onNodeWithTag("dna-confirm").performClick()
        assertEquals(1, confirmed)
        assertEquals(0, dismissed)
    }

    @Test fun dismissCleanupDialogCallsCallback() {
        var dismissed = 0
        screen(state(confirming = true), onDismissCleanup = { dismissed++ })
        compose.onNodeWithTag("dna-dismiss").performClick()
        assertEquals(1, dismissed)
    }

    @Test fun noAuthorizedTreeShowsChooseFolder() {
        screen(state(scan = null, treeUri = null, treeName = null))
        compose.onNodeWithTag("dna-choose-folder").performScrollTo().assertIsDisplayed()
    }

    @Test fun readOnlyTreeDisablesCleanupAndOffersRegrant() {
        var regrant = 0
        screen(state(writable = false, selected = setOf("dup1a")),
            onChooseFolder = { regrant++ })
        compose.onNodeWithTag("dna-read-only").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("dna-cleanup-read-only").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("dna-cleanup").assertDoesNotExist()
        compose.onNodeWithTag("dna-regrant").performScrollTo().performClick()
        assertEquals(1, regrant)
        capture("61-dna-read-only")
    }

    @Test fun emptyScanShowsHonestEmptyState() {
        screen(state(scan = DuplicateScan(emptyList(), 10, 10, atMillis = now)))
        compose.onNodeWithTag("dna-empty").performScrollTo().assertIsDisplayed()
    }

    @Test fun cancelledScanShowsLabel() {
        screen(state(scan = scan(twoGroups()).copy(truncated = true, cancelled = true),
            scanCancelled = true, scanFailed = true))
        compose.onNodeWithTag("dna-cancelled").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("dna-scan-failed").performScrollTo().assertIsDisplayed()
    }

    @Test fun cleaningStateIsVisibleAndCancellable() {
        var cancelled = 0
        screen(state(cleaning = true), onCancelCleanup = { cancelled++ })
        compose.onNodeWithTag("dna-cleaning").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("dna-cleanup-cancel").performScrollTo().performClick()
        assertEquals(1, cancelled)
    }

    @Test fun receiptShowsReclaimedBytesAndVerifiedCount() {
        val receipt = DuplicateCleanupReceipt(
            deleted = 3, skipped = listOf("skip.jpg"), failed = emptyList(),
            verifiedAbsent = 3, bytesReclaimed = 1_500_000, complete = false,
        )
        screen(state(receipt = receipt, journalFailed = true))
        compose.onNodeWithTag("dna-receipt").performScrollTo()
            .assertTextEquals(text(R.string.dna_receipt, 3, 1, 0))
        compose.onNodeWithTag("dna-receipt-reclaimed").assertExists()
        compose.onNodeWithTag("dna-receipt-partial").assertExists()
        compose.onNodeWithTag("dna-journal-failed").performScrollTo().assertIsDisplayed()
        capture("62-dna-receipt")
    }

    @Test fun nightArabicLargeTextKeepsGroupsReadable() {
        val ar = localized(context, "ar")
        screen(state(), night = true, arabic = true, fontScale = 1.6f)
        compose.onNodeWithText(ar.getString(R.string.repair_service_duplicate)).assertExists()
        compose.onNodeWithTag("dna-group-count").assertExists()
        capture("63-dna-night-ar-large")
    }

    @Test fun reclaimableSummaryTileIsDisplayed() {
        screen(state())
        compose.onNodeWithTag("dna-reclaimable").performScrollTo().assertExists()
        compose.onNodeWithTag("dna-group-reclaimable-1").performScrollTo().assertExists()
    }
}
