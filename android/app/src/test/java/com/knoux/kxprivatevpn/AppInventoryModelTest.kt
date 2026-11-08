package com.knoux.kxprivatevpn

import android.content.pm.ApplicationInfo
import androidx.lifecycle.viewModelScope
import com.knoux.kxprivatevpn.features.AppFilter
import com.knoux.kxprivatevpn.features.AppInventoryModel
import com.knoux.kxprivatevpn.features.AppInventoryState
import com.knoux.kxprivatevpn.features.AppManagementAction
import com.knoux.kxprivatevpn.features.AppManagementOutcome
import com.knoux.kxprivatevpn.features.AppManagementReceipt
import com.knoux.kxprivatevpn.features.AppScanPhase
import com.knoux.kxprivatevpn.features.AppSort
import com.knoux.kxprivatevpn.platform.apps.InstalledApp
import com.knoux.kxprivatevpn.platform.apps.isProtectedSystemApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale

/** Exercises the real model with scoped inventory fixtures, not Android uninstall execution. */
@OptIn(ExperimentalCoroutinesApi::class)
class AppInventoryModelTest {
    private val ownPackage = "com.knoux.kxprivatevpn"
    private val self = InstalledApp(ownPackage, "KX Private VPN")
    private val alpha = InstalledApp("org.example.alpha", "Alpha Notes", versionName = "2.1", targetSdk = 35)
    private val beta = InstalledApp("org.example.beta", "Beta Browser")
    private val system = InstalledApp("org.example.system", "System Settings",
        system = isProtectedSystemApp(ApplicationInfo.FLAG_SYSTEM))
    private val updatedSystem = InstalledApp("org.example.updated", "Updated System",
        system = isProtectedSystemApp(ApplicationInfo.FLAG_UPDATED_SYSTEM_APP))
    private val observed = listOf(beta, system, self, alpha, updatedSystem)
    private val models = mutableListOf<AppInventoryModel>()

    @Before fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After fun tearDown() {
        models.forEach { it.viewModelScope.cancel() }
        Dispatchers.resetMain()
    }

    private fun model(
        verifyInstalled: (suspend (String) -> Boolean?)? = null,
        readInventory: suspend () -> List<InstalledApp>,
    ): AppInventoryModel =
        AppInventoryModel(ownPackage, readInventory, verifyInstalled).also { models += it }

    private fun TestScope.scannedModel(apps: List<InstalledApp> = observed): AppInventoryModel {
        val model = model { apps }
        model.scan()
        runCurrent()
        assertEquals(AppScanPhase.RESULTS, model.state.value.phase)
        return model
    }

    private fun assertNoResults(model: AppInventoryModel, phase: AppScanPhase) {
        assertEquals(phase, model.state.value.phase)
        assertTrue(model.state.value.apps.isEmpty())
        assertTrue(model.state.value.shown.isEmpty())
        assertFalse(model.state.value.canManage)
    }

    private fun handoff(
        model: AppInventoryModel,
        action: AppManagementAction,
        launch: () -> Boolean,
    ) {
        when (action) {
            AppManagementAction.UNINSTALL -> {
                model.requestUninstall(alpha.packageName)
                model.confirmUninstall { launch() }
            }
            AppManagementAction.DETAILS -> model.openDetails(alpha.packageName) { launch() }
            AppManagementAction.SETTINGS -> model.openSettings(launch)
        }
    }

    @Test fun initialStateIsReadyWithoutMeasuredResultsOrAReceipt() = runTest {
        var reads = 0
        val model = model { reads++; observed }
        runCurrent()

        assertEquals(AppInventoryState(), model.state.value)
        assertNoResults(model, AppScanPhase.READY)
        assertFalse(model.state.value.externalPending)
        assertEquals(0, reads)
    }

    @Test fun scanIsSingleFlightBeforeAndAfterTheReaderStarts() = runTest {
        val result = CompletableDeferred<List<InstalledApp>>()
        var reads = 0
        val model = model { reads++; result.await() }

        model.scan()
        model.scan()
        assertNoResults(model, AppScanPhase.SCANNING)
        assertEquals(0, reads)
        runCurrent()
        repeat(3) { model.scan() }
        runCurrent()
        assertEquals(1, reads)
        assertNoResults(model, AppScanPhase.SCANNING)

        result.complete(observed)
        runCurrent()
        assertEquals(AppScanPhase.RESULTS, model.state.value.phase)
        assertEquals(observed, model.state.value.apps)
        assertEquals(1, reads)
    }

    @Test fun onlySuccessfulScopedObservationsPublishDeduplicatedCountsAndMetadata() = runTest {
        val result = CompletableDeferred<List<InstalledApp>>()
        val model = model { result.await() }
        model.scan()
        runCurrent()
        assertNoResults(model, AppScanPhase.SCANNING)

        result.complete(observed + alpha.copy(label = "Another launcher", versionName = "9.9") + system)
        runCurrent()

        val state = model.state.value
        assertEquals(AppScanPhase.RESULTS, state.phase)
        assertEquals(observed, state.apps)
        assertEquals(5, state.apps.size)
        assertEquals(3, state.apps.count { !it.system })
        assertEquals(2, state.apps.count { it.system })
        assertEquals(observed.map { it.packageName }.toSet(), state.apps.map { it.packageName }.toSet())
        assertEquals(alpha, state.apps.single { it.packageName == alpha.packageName })
        assertTrue(state.apps.contains(self))
        assertNull(state.apps.single { it.packageName == beta.packageName }.versionName)
        assertNull(state.apps.single { it.packageName == beta.packageName }.targetSdk)
        assertNull(state.receipt)
    }

    @Test fun successfulEmptyInventoryIsMeasuredResultsRatherThanReadyOrFailed() = runTest {
        val model = scannedModel(emptyList())

        assertEquals(AppScanPhase.RESULTS, model.state.value.phase)
        assertEquals(0, model.state.value.apps.size)
        assertTrue(model.state.value.shown.isEmpty())
        assertTrue(model.state.value.canManage)
        assertNull(model.state.value.receipt)
    }

    @Test fun failedInitialScanPublishesFailedWithoutResultsAndCanRetry() = runTest {
        var reads = 0
        val model = model {
            if (++reads == 1) throw SecurityException("Scoped inventory unavailable")
            observed
        }
        model.scan()
        runCurrent()

        assertNoResults(model, AppScanPhase.FAILED)
        assertNull(model.state.value.receipt)
        model.scan()
        runCurrent()
        assertEquals(2, reads)
        assertEquals(AppScanPhase.RESULTS, model.state.value.phase)
        assertEquals(observed, model.state.value.apps)
    }

    @Test fun failedRescanDoesNotPresentPreviouslyObservedAppsAsFreshResults() = runTest {
        var reads = 0
        val model = model {
            if (++reads == 1) observed else throw IllegalStateException("Query failed")
        }
        model.scan()
        runCurrent()
        assertEquals(observed, model.state.value.apps)

        model.scan()
        assertNoResults(model, AppScanPhase.SCANNING)
        runCurrent()
        assertNoResults(model, AppScanPhase.FAILED)
        assertEquals(2, reads)
    }

    @Test fun cancellingBeforeDispatchNeverReadsInventoryAndPermitsRetry() = runTest {
        var reads = 0
        val model = model { reads++; observed }
        model.scan()
        model.cancelScan()
        runCurrent()

        assertNoResults(model, AppScanPhase.CANCELLED)
        assertEquals(0, reads)
        model.scan()
        runCurrent()
        assertEquals(1, reads)
        assertEquals(AppScanPhase.RESULTS, model.state.value.phase)
        assertEquals(observed, model.state.value.apps)
    }

    @Test fun cancelledReaderCannotPublishEvenIfItReturnsNormallyAndRetrySucceeds() = runTest {
        val lateResult = CompletableDeferred<List<InstalledApp>>()
        var reads = 0
        val model = model {
            if (++reads == 1) withContext(NonCancellable) { lateResult.await() } else listOf(beta)
        }
        val states = mutableListOf<AppInventoryState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            model.state.collect { states += it }
        }
        try {
            model.scan()
            runCurrent()
            model.cancelScan()
            assertNoResults(model, AppScanPhase.CANCELLED)
            lateResult.complete(listOf(alpha))
            runCurrent()

            assertNoResults(model, AppScanPhase.CANCELLED)
            assertTrue(states.none { it.phase == AppScanPhase.RESULTS || it.apps.isNotEmpty() })
            model.scan()
            runCurrent()
            assertEquals(2, reads)
            assertEquals(AppScanPhase.RESULTS, model.state.value.phase)
            assertEquals(listOf(beta), model.state.value.apps)
        } finally {
            lateResult.complete(emptyList())
            runCurrent()
        }
    }

    @Test fun lateCancelledReadCannotOverwriteANewerSuccessfulRetry() = runTest {
        val lateResult = CompletableDeferred<List<InstalledApp>>()
        var reads = 0
        val model = model {
            if (++reads == 1) withContext(NonCancellable) { lateResult.await() } else listOf(beta)
        }
        try {
            model.scan()
            runCurrent()
            model.cancelScan()
            model.scan()
            runCurrent()
            val retried = model.state.value
            assertEquals(AppScanPhase.RESULTS, retried.phase)
            assertEquals(listOf(beta), retried.apps)
            assertEquals(2, reads)

            lateResult.complete(listOf(alpha))
            runCurrent()
            assertEquals(retried, model.state.value)
        } finally {
            lateResult.complete(emptyList())
            runCurrent()
        }
    }

    @Test fun readerCancellationTerminatesScanningWithoutResultsAndPermitsRetry() = runTest {
        var reads = 0
        val model = model {
            if (++reads == 1) throw CancellationException("Inventory reader cancelled")
            observed
        }
        model.scan()
        runCurrent()

        // A cancelled provider must not strand the model in its single-flight SCANNING guard.
        assertNoResults(model, AppScanPhase.CANCELLED)
        model.scan()
        runCurrent()
        assertEquals(2, reads)
        assertEquals(AppScanPhase.RESULTS, model.state.value.phase)
        assertEquals(observed, model.state.value.apps)
    }

    @Test fun searchMatchesTrimmedCaseInsensitiveLabelsAndPackages() = runTest {
        val model = scannedModel()
        model.query(" \tALpHa nOtEs\n")
        assertEquals(listOf(alpha), model.state.value.shown)
        model.query("  ORG.EXAMPLE.BETA  ")
        assertEquals(listOf(beta), model.state.value.shown)
        model.query("pHa No")
        assertEquals(listOf(alpha), model.state.value.shown)
        assertEquals(observed, model.state.value.apps)
        assertEquals(AppScanPhase.RESULTS, model.state.value.phase)
    }

    @Test fun whitespaceSearchIsUnfilteredAndNoMatchDoesNotEraseObservedResults() = runTest {
        val model = scannedModel()
        val allShown = model.state.value.shown
        model.query(" \t\n\r ")
        assertEquals(allShown, model.state.value.shown)
        model.query("not-an-observed-app")
        assertTrue(model.state.value.shown.isEmpty())
        assertEquals(observed, model.state.value.apps)
        assertEquals(AppScanPhase.RESULTS, model.state.value.phase)
        model.query("")
        assertEquals(allShown, model.state.value.shown)
    }

    @Test fun filtersUseObservedSystemClassificationAndIntersectWithSearch() = runTest {
        val model = scannedModel()
        model.filter(AppFilter.USER)
        assertEquals(listOf(alpha, beta, self), model.state.value.shown)
        model.filter(AppFilter.SYSTEM)
        assertEquals(listOf(system, updatedSystem), model.state.value.shown)
        model.query("  UPDATED  ")
        assertEquals(listOf(updatedSystem), model.state.value.shown)
        model.filter(AppFilter.USER)
        assertTrue(model.state.value.shown.isEmpty())
        model.filter(AppFilter.ALL)
        assertEquals(listOf(updatedSystem), model.state.value.shown)
        model.query("")
        assertEquals(5, model.state.value.shown.size)
        assertEquals(observed, model.state.value.apps)
    }

    @Test fun nameSortIsLocaleIndependentWithDeterministicPackageTieBreaking() = runTest {
        val firstAlpha = InstalledApp("org.example.a", "ALPHA")
        val secondAlpha = InstalledApp("org.example.z", "alpha")
        val india = InstalledApp("org.example.i", "INDIA")
        val juliet = InstalledApp("org.example.j", "juliet")
        val input = listOf(juliet, secondAlpha, india, firstAlpha)
        val previousLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            val model = scannedModel(input)
            val expected = listOf(firstAlpha, secondAlpha, india, juliet)
            assertEquals(expected, model.state.value.shown)
            assertEquals(expected, scannedModel(input.reversed()).state.value.shown)
        } finally {
            Locale.setDefault(previousLocale)
        }
    }

    @Test fun packageSortIgnoresLabelOrderAndIsDeterministicAcrossInputOrders() = runTest {
        val first = InstalledApp("org.example.a", "Zulu")
        val second = InstalledApp("org.example.b", "Alpha")
        val third = InstalledApp("org.example.c", "Beta")
        for (input in listOf(listOf(third, first, second), listOf(second, third, first))) {
            val model = scannedModel(input)
            model.sort(AppSort.PACKAGE)
            assertEquals(listOf(first, second, third), model.state.value.shown)
            model.sort(AppSort.NAME)
            assertEquals(listOf(second, third, first), model.state.value.shown)
        }
    }

    @Test fun uninstallRequiresResultsAndAnObservedEligiblePackage() = runTest {
        val result = CompletableDeferred<List<InstalledApp>>()
        val model = model { result.await() }
        model.requestUninstall(alpha.packageName)
        assertNull(model.state.value.approval)
        model.scan()
        runCurrent()
        model.requestUninstall(alpha.packageName)
        assertNull(model.state.value.approval)
        model.cancelScan()
        runCurrent()
        model.requestUninstall(alpha.packageName)
        assertNull(model.state.value.approval)

        result.complete(observed)
        model.scan()
        runCurrent()
        model.requestUninstall("org.example.unobserved")
        assertNull(model.state.value.approval)
        model.requestUninstall(alpha.packageName)
        assertEquals(alpha, model.state.value.approval)
        assertNull(model.state.value.receipt)
    }

    @Test fun selfSystemAndUpdatedSystemAppsNeverAcquireUninstallApproval() = runTest {
        val model = scannedModel()
        var launches = 0
        for (protectedApp in listOf(self, system, updatedSystem)) {
            model.requestUninstall(protectedApp.packageName)
            assertNull(model.state.value.approval)
            model.confirmUninstall { launches++; true }
            assertNull(model.state.value.receipt)
        }
        assertEquals(0, launches)
        assertEquals(observed, model.state.value.apps)
    }

    @Test fun approvalFreezesTheTargetDespiteQueryFilterSortAndCompetingActions() = runTest {
        var reads = 0
        var otherLaunches = 0
        val model = model { reads++; observed }
        model.scan()
        runCurrent()
        model.requestUninstall(alpha.packageName)
        model.query("SYSTEM")
        model.filter(AppFilter.SYSTEM)
        model.sort(AppSort.PACKAGE)
        model.requestUninstall(beta.packageName)
        model.scan()
        model.openDetails(beta.packageName) { otherLaunches++; true }
        model.openSettings { otherLaunches++; true }
        runCurrent()

        assertFalse(model.state.value.shown.contains(alpha))
        assertEquals(alpha, model.state.value.approval)
        assertEquals(1, reads)
        assertEquals(0, otherLaunches)
        assertNull(model.state.value.receipt)
        val launched = mutableListOf<InstalledApp>()
        model.confirmUninstall { launched += it; true }
        assertEquals(listOf(alpha), launched)
        assertEquals(AppManagementReceipt(AppManagementAction.UNINSTALL, alpha,
            AppManagementOutcome.OPENED), model.state.value.receipt)
    }

    @Test fun confirmationConsumesApprovalBeforeCallbackAndLaunchesExactlyOnce() = runTest {
        val model = scannedModel()
        model.requestUninstall(alpha.packageName)
        var launches = 0
        model.confirmUninstall { approved ->
            launches++
            assertEquals(alpha, approved)
            assertNull(model.state.value.approval)
            assertTrue(model.state.value.externalPending)
            model.confirmUninstall { launches++; true }
            true
        }
        model.confirmUninstall { launches++; true }

        assertEquals(1, launches)
        assertNull(model.state.value.approval)
        assertEquals(observed, model.state.value.apps)
        assertEquals(AppManagementOutcome.OPENED, model.state.value.receipt?.outcome)
    }

    @Test fun dismissingApprovalPreventsLateConfirmationAndAllowsANewTarget() = runTest {
        val model = scannedModel()
        model.requestUninstall(alpha.packageName)
        model.dismissApproval()
        model.dismissApproval()
        var launches = 0
        model.confirmUninstall { launches++; true }

        assertEquals(0, launches)
        assertNull(model.state.value.approval)
        assertNull(model.state.value.receipt)
        assertEquals(observed, model.state.value.apps)
        model.requestUninstall(beta.packageName)
        assertEquals(beta, model.state.value.approval)
        model.confirmUninstall { approved ->
            launches++
            assertEquals(beta, approved)
            true
        }
        assertEquals(1, launches)
    }

    @Test fun falseAndroidLaunchIsUnavailableAndUnblocksManagementForEveryAction() = runTest {
        for (action in AppManagementAction.values()) {
            val model = scannedModel()
            var launches = 0
            handoff(model, action) { launches++; false }
            val expected = AppManagementReceipt(action,
                if (action == AppManagementAction.SETTINGS) null else alpha,
                AppManagementOutcome.UNAVAILABLE)

            assertEquals(1, launches)
            assertEquals(expected, model.state.value.receipt)
            assertNull(model.state.value.approval)
            assertFalse(model.state.value.externalPending)
            assertTrue(model.state.value.canManage)
            assertEquals(observed, model.state.value.apps)
            model.returnedFromAndroid()
            runCurrent()
            assertEquals(AppScanPhase.RESULTS, model.state.value.phase)
            assertEquals(expected, model.state.value.receipt)
            model.requestUninstall(beta.packageName)
            assertEquals(beta, model.state.value.approval)
        }
    }

    @Test fun thrownAndroidLaunchIsUnavailableAndUnblocksRescanningForEveryAction() = runTest {
        for (action in AppManagementAction.values()) {
            var reads = 0
            val model = model { reads++; observed }
            model.scan()
            runCurrent()
            handoff(model, action) { throw SecurityException("Android launch denied") }
            val receipt = model.state.value.receipt

            assertEquals(AppManagementOutcome.UNAVAILABLE, receipt?.outcome)
            assertEquals(action, receipt?.action)
            assertFalse(model.state.value.externalPending)
            assertNull(model.state.value.approval)
            assertTrue(model.state.value.canManage)
            model.scan()
            runCurrent()
            assertEquals(2, reads)
            assertEquals(AppScanPhase.RESULTS, model.state.value.phase)
            assertEquals(receipt, model.state.value.receipt)
        }
    }

    @Test fun successfulHandoffOnlyOpensAndroidAndBlocksCompetingOperations() = runTest {
        for (action in AppManagementAction.values()) {
            var reads = 0
            val model = model { reads++; observed }
            model.scan()
            runCurrent()
            var launches = 0
            handoff(model, action) { launches++; true }
            val opened = AppManagementReceipt(action,
                if (action == AppManagementAction.SETTINGS) null else alpha,
                AppManagementOutcome.OPENED)

            assertEquals(opened, model.state.value.receipt)
            assertTrue(model.state.value.externalPending)
            assertFalse(model.state.value.canManage)
            assertEquals(AppScanPhase.RESULTS, model.state.value.phase)
            assertEquals(observed, model.state.value.apps)
            model.scan()
            model.requestUninstall(beta.packageName)
            model.confirmUninstall { launches++; true }
            model.openDetails(beta.packageName) { launches++; true }
            model.openSettings { launches++; true }
            runCurrent()
            assertNull(model.state.value.approval)
            assertEquals(1, launches)
            assertEquals(1, reads)
            assertEquals(opened, model.state.value.receipt)
            assertEquals(observed, model.state.value.apps)
        }
    }

    @Test fun returnRefreshesOnceAndReportsOnlyReturnedWhetherTargetIsStillVisibleOrNot() = runTest {
        // Missing from a scoped launcher query is not proof of removal or reclaimed storage.
        for (freshInventory in listOf(observed, observed.filterNot { it == alpha })) {
            val refreshed = CompletableDeferred<List<InstalledApp>>()
            var reads = 0
            val model = model { if (++reads == 1) observed else refreshed.await() }
            model.scan()
            runCurrent()
            model.requestUninstall(alpha.packageName)
            model.confirmUninstall { true }
            val returned = AppManagementReceipt(AppManagementAction.UNINSTALL, alpha,
                AppManagementOutcome.RETURNED)

            model.returnedFromAndroid()
            assertNoResults(model, AppScanPhase.SCANNING)
            assertEquals(returned, model.state.value.receipt)
            assertFalse(model.state.value.externalPending)
            model.returnedFromAndroid()
            runCurrent()
            assertEquals(2, reads)
            model.returnedFromAndroid()
            runCurrent()
            assertEquals(2, reads)

            refreshed.complete(freshInventory)
            runCurrent()
            assertEquals(AppScanPhase.RESULTS, model.state.value.phase)
            assertEquals(freshInventory, model.state.value.apps)
            assertEquals(returned, model.state.value.receipt)
            assertTrue(model.state.value.canManage)
            val completed = model.state.value
            repeat(3) { model.returnedFromAndroid() }
            runCurrent()
            assertEquals(2, reads)
            assertEquals(completed, model.state.value)
        }
    }

    @Test fun manualRescanPreservesTheReturnedHandoffReceiptForEveryAction() = runTest {
        for (action in AppManagementAction.values()) {
            var reads = 0
            val model = model { reads++; observed }
            model.scan()
            runCurrent()
            handoff(model, action) { true }
            model.returnedFromAndroid()
            runCurrent()
            val returned = AppManagementReceipt(action,
                if (action == AppManagementAction.SETTINGS) null else alpha,
                AppManagementOutcome.RETURNED)
            assertEquals(returned, model.state.value.receipt)
            assertEquals(2, reads)

            model.scan()
            assertNoResults(model, AppScanPhase.SCANNING)
            assertEquals(returned, model.state.value.receipt)
            runCurrent()
            assertEquals(3, reads)
            assertEquals(AppScanPhase.RESULTS, model.state.value.phase)
            assertEquals(returned, model.state.value.receipt)
            assertEquals(observed, model.state.value.apps)
        }
    }

    @Test fun failedOrCancelledRefreshPreservesReturnedReceiptWithoutInventingResults() = runTest {
        for (cancelRefresh in listOf(false, true)) {
            val refresh = CompletableDeferred<List<InstalledApp>>()
            var reads = 0
            val model = model {
                when (++reads) {
                    1 -> observed
                    2 -> if (cancelRefresh) refresh.await() else throw SecurityException("Refresh denied")
                    else -> observed
                }
            }
            model.scan()
            runCurrent()
            model.requestUninstall(alpha.packageName)
            model.confirmUninstall { true }
            model.returnedFromAndroid()
            runCurrent()
            if (cancelRefresh) {
                model.cancelScan()
                refresh.complete(listOf(beta))
                runCurrent()
            }
            assertNoResults(model, if (cancelRefresh) AppScanPhase.CANCELLED else AppScanPhase.FAILED)
            val returned = AppManagementReceipt(AppManagementAction.UNINSTALL, alpha,
                AppManagementOutcome.RETURNED)
            assertEquals(returned, model.state.value.receipt)
            model.returnedFromAndroid()
            runCurrent()
            assertEquals(2, reads)

            model.scan()
            runCurrent()
            assertEquals(3, reads)
            assertEquals(AppScanPhase.RESULTS, model.state.value.phase)
            assertEquals(returned, model.state.value.receipt)
        }
    }

    @Test fun restoredActivityResultRefreshesOnceWithoutInventingALostReceipt() = runTest {
        var reads = 0
        val model = model { reads++; observed }
        // After process recreation the registry may deliver a result to a fresh ViewModel.
        // The inventory is refreshed, but no lost target or uninstall outcome is reconstructed.
        model.returnedFromAndroid()
        assertNoResults(model, AppScanPhase.SCANNING)
        assertNull(model.state.value.receipt)
        model.returnedFromAndroid()
        runCurrent()
        assertEquals(1, reads)
        assertEquals(AppScanPhase.RESULTS, model.state.value.phase)
        assertEquals(observed, model.state.value.apps)
        assertNull(model.state.value.receipt)

        val completed = model.state.value
        model.returnedFromAndroid()
        runCurrent()
        assertEquals(1, reads)
        assertEquals(completed, model.state.value)
    }

    @Test fun verifiedRemovalRequiresAPackageManagerAbsenceAndStillRefreshesInventory() = runTest {
        var reads = 0
        val verified = mutableListOf<String>()
        val model = model(
            readInventory = {
                reads++
                if (reads == 1) observed else observed.filterNot { it == alpha }
            },
            verifyInstalled = { packageName -> verified += packageName; false },
        )
        model.scan()
        runCurrent()
        model.requestUninstall(alpha.packageName)
        model.confirmUninstall { true }

        model.returnedFromAndroid()
        assertNoResults(model, AppScanPhase.SCANNING)
        assertEquals(AppManagementOutcome.RETURNED, model.state.value.receipt?.outcome)
        assertTrue("the verifier must not run before the UI is released", verified.isEmpty())

        runCurrent()
        assertEquals(listOf(alpha.packageName), verified)
        assertEquals(2, reads)
        assertEquals(AppScanPhase.RESULTS, model.state.value.phase)
        assertEquals(observed.filterNot { it == alpha }, model.state.value.apps)
        assertEquals(AppManagementReceipt(AppManagementAction.UNINSTALL, alpha,
            AppManagementOutcome.REMOVED), model.state.value.receipt)
        assertTrue(model.state.value.canManage)
    }

    @Test fun stillInstalledIsReportedWhenPackageManagerFindsTheExactPackage() = runTest {
        var reads = 0
        var verified = 0
        val model = model(
            readInventory = { reads++; observed },
            verifyInstalled = { verified++; true },
        )
        model.scan()
        runCurrent()
        model.requestUninstall(alpha.packageName)
        model.confirmUninstall { true }
        model.returnedFromAndroid()
        runCurrent()

        assertEquals(1, verified)
        assertEquals(2, reads)
        assertEquals(AppScanPhase.RESULTS, model.state.value.phase)
        assertEquals(observed, model.state.value.apps)
        assertEquals(AppManagementReceipt(AppManagementAction.UNINSTALL, alpha,
            AppManagementOutcome.STILL_INSTALLED), model.state.value.receipt)
    }

    @Test fun failedOrDeniedPackageReadNeverClaimsRemoval() = runTest {
        val verifiers = listOf<suspend (String) -> Boolean?>(
            { null },
            { throw SecurityException("package state denied") },
        )
        verifiers.forEach { verifier ->
            var reads = 0
            val model = model(readInventory = { reads++; observed }, verifyInstalled = verifier)
            model.scan()
            runCurrent()
            model.requestUninstall(alpha.packageName)
            model.confirmUninstall { true }
            model.returnedFromAndroid()
            runCurrent()

            assertEquals(2, reads)
            assertEquals(AppScanPhase.RESULTS, model.state.value.phase)
            assertEquals(AppManagementReceipt(AppManagementAction.UNINSTALL, alpha,
                AppManagementOutcome.VERIFY_FAILED), model.state.value.receipt)
        }
    }

    @Test fun returnFromDetailsOrSettingsNeverRunsTheUninstallVerifier() = runTest {
        for (action in listOf(AppManagementAction.DETAILS, AppManagementAction.SETTINGS)) {
            var reads = 0
            var verified = 0
            val model = model(
                readInventory = { reads++; observed },
                verifyInstalled = { verified++; false },
            )
            model.scan()
            runCurrent()
            handoff(model, action) { true }
            model.returnedFromAndroid()
            runCurrent()

            assertEquals(action, model.state.value.receipt?.action)
            assertEquals(AppManagementOutcome.RETURNED, model.state.value.receipt?.outcome)
            assertEquals(0, verified)
            assertEquals(2, reads)
        }
    }

    @Test fun lateVerificationCannotOverwriteANewerHandoffReceipt() = runTest {
        val pending = CompletableDeferred<Boolean?>()
        var verified = 0
        val model = model(
            readInventory = { observed },
            verifyInstalled = { verified++; pending.await() },
        )
        model.scan()
        runCurrent()
        model.requestUninstall(alpha.packageName)
        model.confirmUninstall { true }
        model.returnedFromAndroid()
        runCurrent()
        assertEquals(AppManagementOutcome.RETURNED, model.state.value.receipt?.outcome)
        assertEquals(1, verified)

        model.requestUninstall(beta.packageName)
        model.confirmUninstall { true }
        val newer = AppManagementReceipt(AppManagementAction.UNINSTALL, beta,
            AppManagementOutcome.OPENED)
        assertEquals(newer, model.state.value.receipt)

        pending.complete(false)
        runCurrent()
        assertEquals(newer, model.state.value.receipt)
    }

    @Test fun repeatedReturnCannotRunASecondVerificationOrRestateTheOutcome() = runTest {
        var verified = 0
        val model = model(
            readInventory = { observed },
            verifyInstalled = { verified++; false },
        )
        model.scan()
        runCurrent()
        model.requestUninstall(alpha.packageName)
        model.confirmUninstall { true }
        model.returnedFromAndroid()
        runCurrent()
        assertEquals(1, verified)
        assertEquals(AppManagementOutcome.REMOVED, model.state.value.receipt?.outcome)

        val completed = model.state.value
        repeat(3) { model.returnedFromAndroid() }
        runCurrent()
        assertEquals(1, verified)
        assertEquals(completed, model.state.value)
    }
}
