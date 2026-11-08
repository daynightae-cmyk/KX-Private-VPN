package com.knoux.kxprivatevpn

import android.app.Application
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
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.data.stats.UsageStatsStore
import com.knoux.kxprivatevpn.features.AutopilotScreen
import com.knoux.kxprivatevpn.features.AutopilotUiState
import com.knoux.kxprivatevpn.features.AutopilotViewModel
import com.knoux.kxprivatevpn.platform.repair.AutopilotKind
import com.knoux.kxprivatevpn.platform.repair.AutopilotPlan
import com.knoux.kxprivatevpn.platform.repair.AutopilotPlanner
import com.knoux.kxprivatevpn.platform.repair.AutopilotReceipt
import com.knoux.kxprivatevpn.platform.repair.AutopilotTarget
import com.knoux.kxprivatevpn.platform.repair.RepairJournal
import com.knoux.kxprivatevpn.platform.repair.RepairJournalEntry
import com.knoux.kxprivatevpn.platform.repair.RepairOutcome
import com.knoux.kxprivatevpn.platform.security.ShieldRetention
import com.knoux.kxprivatevpn.platform.usage.UsageRecord
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.Locale

/**
 * The autopilot shows a measured plan, asks before it deletes anything, and reports only what
 * really happened. The two real tests prove the destructive paths against the app's own storage:
 * an approved cache file is actually deleted and its bytes freed, and aged history is actually
 * pruned while fresh history survives.
 */
class AutopilotScreenTest {
    @get:Rule val compose = createComposeRule()

    private val base: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val context: Context
        get() = localized(base, "en")
    private val now = 1_760_000_000_000L

    private fun localized(base: Context, language: String): Context {
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(language))
        return base.createConfigurationContext(configuration)
    }

    private fun cacheTarget(path: String, bytes: Long) = AutopilotTarget(
        id = AutopilotPlanner.cacheId(path), kind = AutopilotKind.CACHE_FILE,
        title = path, detail = "kx_cache_file", bytes = bytes, destructive = true,
    )

    private fun historyTarget(agedJournal: Int, agedUsage: Int) = AutopilotTarget(
        id = AutopilotPlanner.HISTORY_ID, kind = AutopilotKind.HISTORY,
        title = "aged_kx_history", detail = "$agedJournal+$agedUsage", bytes = 0L, destructive = true,
    )

    private fun samplePlan() = AutopilotPlan(
        targets = listOf(cacheTarget("a.tmp", 1024), cacheTarget("b.tmp", 2048), historyTarget(1, 2)),
        measuredAtMillis = now, cacheMeasured = true, agedJournal = 1, agedUsage = 2,
    )

    private fun screen(
        state: AutopilotUiState,
        night: Boolean = false,
        arabic: Boolean = false,
        fontScale: Float = 1f,
        onToggle: (String) -> Unit = {},
        onSelectAll: () -> Unit = {},
        onClearSelection: () -> Unit = {},
        onRequestRun: () -> Unit = {},
        onDismissConfirmation: () -> Unit = {},
        onConfirmRun: () -> Unit = {},
        onCancel: () -> Unit = {},
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
                    AutopilotScreen(
                        night = night,
                        state = state,
                        onBack = {},
                        onMeasure = {},
                        onToggle = onToggle,
                        onSelectAll = onSelectAll,
                        onClearSelection = onClearSelection,
                        onRequestRun = onRequestRun,
                        onDismissConfirmation = onDismissConfirmation,
                        onConfirmRun = onConfirmRun,
                        onCancel = onCancel,
                    )
                }
            }
        }
    }

    private fun capture(name: String) {
        captureEvidence(context, compose, name)
    }

    @Test fun aMeasuredPlanListsEveryTargetWithItsSideEffect() {
        val plan = samplePlan()
        screen(AutopilotUiState(loading = false, plan = plan, selected = plan.targets.map { it.id }.toSet()))

        compose.onNodeWithTag("autopilot-plan").assertExists()
        compose.onNodeWithTag("autopilot-plan-summary").assertExists()
        compose.onNodeWithTag("autopilot-selected").assertExists()
        compose.onNodeWithTag("autopilot-target-title-0", useUnmergedTree = true).assertTextEquals("a.tmp")
        compose.onNodeWithTag("autopilot-target-title-1", useUnmergedTree = true).assertTextEquals("b.tmp")
        compose.onNodeWithTag("autopilot-target-title-2", useUnmergedTree = true)
            .assertTextEquals(context.getString(R.string.autopilot_history_title))
        compose.onNodeWithTag("autopilot-select-0").assertIsOn()
        compose.onNodeWithTag("autopilot-scope").assertExists()
        compose.onNodeWithTag("autopilot-run").performScrollTo().assertIsEnabled()
        capture("48-autopilot-plan-day")
    }

    @Test fun anEmptyPlanIsAnHonestEmptyStateAndCannotRun() {
        screen(AutopilotUiState(loading = false, plan = AutopilotPlan(measuredAtMillis = now, cacheMeasured = true)))
        compose.onNodeWithTag("autopilot-empty").assertExists()
        compose.onNodeWithTag("autopilot-run").performScrollTo().assertIsNotEnabled()
    }

    @Test fun aFailedMeasurementInventsNoPlan() {
        screen(AutopilotUiState(loading = false, failure = true))
        compose.onNodeWithTag("autopilot-failure").assertExists()
        compose.onNodeWithTag("autopilot-plan-summary").assertDoesNotExist()
    }

    @Test fun anUnmeasuredCacheStatesTheGapInsteadOfClaimingEmpty() {
        val plan = AutopilotPlan(
            targets = listOf(historyTarget(1, 0)), measuredAtMillis = now,
            cacheMeasured = false, agedJournal = 1,
        )
        screen(AutopilotUiState(loading = false, plan = plan, selected = setOf(AutopilotPlanner.HISTORY_ID)))
        compose.onNodeWithTag("autopilot-cache-unavailable").assertExists()
    }

    @Test fun togglingATargetInvokesTheCallbackWithItsId() {
        var toggled = ""
        val plan = samplePlan()
        screen(AutopilotUiState(loading = false, plan = plan, selected = emptySet()), onToggle = { toggled = it })
        // The card sits below the fold on short screens; clicking without scrolling misses it.
        compose.onNodeWithTag("autopilot-select-0").performScrollTo().performClick()
        assertEquals("cache:a.tmp", toggled)
    }

    @Test fun runningAsksFirstAndTheRequestItselfDeletesNothing() {
        var requested = 0
        var confirmed = 0
        val plan = samplePlan()
        screen(
            AutopilotUiState(loading = false, plan = plan, selected = plan.targets.map { it.id }.toSet()),
            onRequestRun = { requested++ }, onConfirmRun = { confirmed++ },
        )
        compose.onNodeWithTag("autopilot-run").performScrollTo().performClick()
        assertEquals(1, requested)
        assertEquals("asking for a run must not delete", 0, confirmed)
    }

    @Test fun theApprovalDialogSeparatesTheApprovalFromTheRequest() {
        var confirmed = 0
        var dismissed = 0
        val plan = samplePlan()
        screen(
            AutopilotUiState(loading = false, plan = plan,
                selected = plan.targets.map { it.id }.toSet(), confirming = true),
            onConfirmRun = { confirmed++ }, onDismissConfirmation = { dismissed++ },
        )
        compose.onNodeWithTag("autopilot-confirm").assertExists()
        compose.onNodeWithTag("autopilot-confirm-cancel").assertExists()
        compose.onNodeWithTag("autopilot-confirm-cancel").performClick()
        assertEquals(0, confirmed)
        assertEquals(1, dismissed)
    }

    @Test fun arabicApprovalDialogKeepsArabicCopy() {
        val arabic = localized(base, "ar")
        val plan = samplePlan()
        screen(
            AutopilotUiState(loading = false, plan = plan,
                selected = plan.targets.map { it.id }.toSet(), confirming = true),
            arabic = true,
        )
        compose.onNodeWithText(arabic.getString(R.string.autopilot_confirm_title)).assertExists()
        compose.onNodeWithText(arabic.getString(R.string.autopilot_confirm)).assertExists()
        compose.onNodeWithText(arabic.getString(R.string.cancel)).assertExists()
    }

    @Test fun aCompleteReceiptOnlyReportsWhatHappened() {
        screen(AutopilotUiState(loading = false, receipt = AutopilotReceipt(
            deleted = 2, pruned = 1, bytesFreed = 3072, skipped = emptyList(), failed = emptyList(),
            complete = true, cancelled = false, atMillis = now, detail = "complete",
        )))
        compose.onNodeWithTag("autopilot-receipt").assertExists()
        compose.onNodeWithTag("autopilot-receipt-detail")
            .assertTextEquals(context.getString(R.string.autopilot_receipt_complete))
        compose.onNodeWithTag("autopilot-receipt-counts").assertTextContains("2", substring = true)
    }

    @Test fun aCancelledRunIsLabelledCancelledNotCompleted() {
        screen(AutopilotUiState(loading = false, receipt = AutopilotReceipt(
            deleted = 1, pruned = 0, bytesFreed = 1024, skipped = emptyList(), failed = emptyList(),
            complete = false, cancelled = true, atMillis = now, detail = "cancelled",
        )))
        compose.onNodeWithTag("autopilot-receipt-detail")
            .assertTextEquals(context.getString(R.string.autopilot_receipt_cancelled))
    }

    @Test fun aPartialReceiptSurfacesSkippedAndFailed() {
        screen(AutopilotUiState(loading = false, receipt = AutopilotReceipt(
            deleted = 1, pruned = 0, bytesFreed = 1024, skipped = listOf("changed.tmp"),
            failed = listOf("locked.tmp"), complete = false, cancelled = false, atMillis = now,
            detail = "partial",
        )))
        compose.onNodeWithTag("autopilot-receipt-detail")
            .assertTextEquals(context.getString(R.string.autopilot_receipt_partial))
        compose.onNodeWithTag("autopilot-receipt-skipped").assertExists()
        compose.onNodeWithTag("autopilot-receipt-failed").assertExists()
    }

    @Test fun busyStateOffersCancelAndHidesTheRunButton() {
        var cancelled = 0
        val plan = samplePlan()
        screen(AutopilotUiState(loading = false, plan = plan, busy = true), onCancel = { cancelled++ })
        compose.onNodeWithTag("autopilot-busy").assertExists()
        compose.onNodeWithTag("autopilot-run").assertDoesNotExist()
        compose.onNodeWithTag("autopilot-cancel").performScrollTo().performClick()
        assertEquals(1, cancelled)
    }

    @Test fun nightArabicLargeTextKeepsThePlanVisible() {
        val ar = localized(context, "ar")
        val plan = samplePlan()
        screen(AutopilotUiState(loading = false, plan = plan, selected = plan.targets.map { it.id }.toSet()),
            night = true, arabic = true, fontScale = 1.6f)
        compose.onNodeWithText(ar.getString(R.string.autopilot_title)).assertExists()
        compose.onNodeWithTag("autopilot-target-0").performScrollTo().assertExists()
        compose.onNodeWithTag("autopilot-target-2").performScrollTo().assertExists()
        compose.onNodeWithTag("autopilot-scope").performScrollTo().assertExists()
        capture("49-autopilot-plan-night-ar-large")
    }

    /**
     * The real thing: a file is seeded into the app's own cache, the plan proposes it, the person
     * approves only that target, and the file must actually be gone with its bytes freed.
     */
    @Test fun runningThePlanReallyDeletesTheApprovedCacheFile() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val name = "autopilot-real-cache-test.tmp"
        val file = File(app.cacheDir, name)
        file.writeBytes(ByteArray(4096) { (it + 1).toByte() })
        try {
            val model = AutopilotViewModel(app)
            val deadline = System.currentTimeMillis() + 15_000
            while (System.currentTimeMillis() < deadline &&
                model.state.value.plan?.cacheTargets?.any { it.title == name } != true
            ) {
                Thread.sleep(100)
            }
            val target = model.state.value.plan?.cacheTargets?.firstOrNull { it.title == name }
            assertTrue("the plan must propose the seeded cache file", target != null)

            model.clearSelection()
            model.toggle(target!!.id)
            model.requestRun()
            assertTrue(model.state.value.confirming)
            model.confirmRun()

            val after = System.currentTimeMillis() + 15_000
            while (System.currentTimeMillis() < after && model.state.value.receipt == null) {
                Thread.sleep(100)
            }
            val receipt = model.state.value.receipt
            assertTrue("a receipt must be produced", receipt != null)
            assertEquals(1, receipt!!.deleted)
            assertEquals(4096L, receipt.bytesFreed)
            assertTrue("a clean single-file run is complete", receipt.complete)
            assertFalse("the approved cache file must really be gone", file.exists())
            val journal = RepairJournal(File(app.noBackupFilesDir, "repair-journal.json"))
            assertTrue("the run must be journaled", journal.load().any { it.serviceKey == "autopilot" })
        } finally {
            if (file.exists()) file.delete()
        }
    }

    /**
     * The real thing: aged journal and usage entries are pruned by an approved history run while
     * fresh entries survive. The read-back proves the prune happened, not just the receipt.
     */
    @Test fun runningThePlanReallyPrunesAgedHistoryAndKeepsFresh() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val wall = System.currentTimeMillis()
        val agedAt = wall - 30 * ShieldRetention.DAY_MILLIS
        val freshAt = wall - ShieldRetention.DAY_MILLIS
        val journal = RepairJournal(File(app.noBackupFilesDir, "repair-journal.json"))
        journal.append(RepairJournalEntry(agedAt, "autopilot-test-aged", "probe", RepairOutcome.COMPLETE, 1, 1))
        journal.append(RepairJournalEntry(freshAt, "autopilot-test-fresh", "probe", RepairOutcome.COMPLETE, 1, 1))
        val usage = UsageStatsStore(app)
        runBlocking {
            usage.commit(UsageRecord("autopilot-aged", "aged", "endpoint", agedAt, 1000, 10, 20))
            usage.commit(UsageRecord("autopilot-fresh", "fresh", "endpoint", freshAt, 1000, 10, 20))
        }

        val model = AutopilotViewModel(app)
        val deadline = System.currentTimeMillis() + 15_000
        while (System.currentTimeMillis() < deadline && model.state.value.plan?.historyTarget == null) {
            Thread.sleep(100)
        }
        assertTrue("the plan must propose the aged history", model.state.value.plan?.historyTarget != null)

        model.clearSelection()
        model.toggle(AutopilotPlanner.HISTORY_ID)
        model.requestRun()
        model.confirmRun()

        val after = System.currentTimeMillis() + 15_000
        while (System.currentTimeMillis() < after && model.state.value.receipt == null) {
            Thread.sleep(100)
        }
        val receipt = model.state.value.receipt
        assertTrue("a receipt must be produced", receipt != null)
        assertTrue("at least the two seeded aged entries were pruned", receipt!!.pruned >= 2)

        val remainingJournal = journal.load()
        assertFalse("the aged journal entry must be gone",
            remainingJournal.any { it.serviceKey == "autopilot-test-aged" })
        assertTrue("the fresh journal entry must survive",
            remainingJournal.any { it.serviceKey == "autopilot-test-fresh" })
        val remainingUsage = runBlocking { usage.load().records }
        assertFalse("the aged usage record must be gone",
            remainingUsage.any { it.profileId == "autopilot-aged" })
        assertTrue("the fresh usage record must survive",
            remainingUsage.any { it.profileId == "autopilot-fresh" })
    }
}