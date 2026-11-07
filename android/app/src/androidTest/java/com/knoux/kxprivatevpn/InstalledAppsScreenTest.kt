package com.knoux.kxprivatevpn

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.features.*
import com.knoux.kxprivatevpn.platform.apps.InstalledApp
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.Locale

/** Screen-only fixtures: no route, ViewModel, inventory reader or Android handoff. */
class InstalledAppsScreenTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val user = InstalledApp("test.fixture.notes", "Fixture Notes", versionName = "7.2", targetSdk = 35)
    private val system = InstalledApp("test.fixture.system", "Fixture System", system = true)
    private val self = InstalledApp("test.fixture.kx", "Fixture KX")
    private val apps = listOf(user, system, self)
    private val ui = mutableStateOf(AppInventoryState())
    private val events = mutableListOf<String>()
    private var fixtureDensity = 1f
    private fun results() = AppInventoryState(phase = AppScanPhase.RESULTS, apps = apps)
    private fun english(): Context = context.createConfigurationContext(
        Configuration(context.resources.configuration).apply { setLocale(Locale.ENGLISH) })
    private fun text(id: Int, vararg args: Any) = english().getString(id, *args)
    private fun show(state: AppInventoryState = AppInventoryState()) {
        ui.value = state
        compose.setContent { LayoutFixture(LayoutMode()) }
    }
    private fun update(state: AppInventoryState) = compose.runOnIdle { ui.value = state }
    private fun expect(vararg expected: String) = compose.runOnIdle {
        assertEquals(expected.toList(), events)
        events.clear()
    }
    // Lazy children must first be brought into composition, including nested action tags.
    private fun at(tag: String): SemanticsNodeInteraction {
        compose.onNodeWithTag("route-apps").performScrollToNode(hasTestTag(tag))
        return compose.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
    }
    private fun textIn(tag: String, value: String) = compose.onNode(
        hasText(value) and hasAnyAncestor(hasTestTag(tag)), useUnmergedTree = true,
    ).assertTextEquals(value)

    @Test fun readyHasNoResultsAndDeliversScanSettingsAndBackCallbacks() {
        show()
        compose.onNodeWithContentDescription(text(R.string.back)).performClick()
        at("apps-scan-orb").assertTextEquals(text(R.string.apps_scan_action)).performClick()
        at("apps-system-settings").assertIsEnabled().performClick()
        compose.onNodeWithTag("apps-visible-count").assertDoesNotExist()
        compose.onNodeWithTag("apps-empty").assertDoesNotExist()
        compose.onNodeWithTag("apps-progress").assertDoesNotExist()
        expect("back", "scan", "settings")
    }

    @Test fun scanningAllowsCancelButNotSettingsOrAnotherScan() {
        show(AppInventoryState(phase = AppScanPhase.SCANNING))
        at("apps-progress")
        at("apps-cancel-scan").assertIsEnabled().performClick()
        at("apps-system-settings").assertIsNotEnabled()
        compose.onNodeWithTag("apps-scan-orb").assertDoesNotExist()
        compose.onNodeWithTag("apps-visible-count").assertDoesNotExist()
        expect("cancel")
    }

    @Test fun failedAndCancelledExplainStatusAndAllowRetry() {
        show(AppInventoryState(phase = AppScanPhase.FAILED))
        listOf(AppScanPhase.FAILED to R.string.app_inventory_failure,
            AppScanPhase.CANCELLED to R.string.apps_cancelled).forEach { (phase, message) ->
            update(AppInventoryState(phase = phase))
            at("apps-scan-status").assertTextEquals(text(message))
            at("apps-scan-orb").assertIsEnabled().assertTextEquals(text(R.string.apps_scan_again)).performClick()
            compose.onNodeWithTag("apps-progress").assertDoesNotExist()
            expect("scan")
        }
    }

    @Test fun actualCountsDistinguishEmptyInventoryFromSearchAndFilterMisses() {
        show(results())
        at("apps-visible-count").assertTextEquals(text(R.string.apps_visible_count, 3))
        at("apps-shown-count").assertTextEquals(text(R.string.apps_shown_count, 3, 3))
        update(results().copy(apps = emptyList()))
        at("apps-visible-count").assertTextEquals(text(R.string.apps_visible_count, 0))
        at("apps-empty")
        textIn("apps-empty", text(R.string.apps_empty_inventory))
        compose.onNodeWithTag("apps-reset-filter").assertDoesNotExist()
        listOf(results().copy(query = "no.such.fixture"),
            results().copy(apps = listOf(user), filter = AppFilter.SYSTEM)).forEach { miss ->
            update(miss)
            at("apps-shown-count").assertTextEquals(text(R.string.apps_shown_count, 0, miss.apps.size))
            at("apps-empty")
            textIn("apps-empty", text(R.string.apps_no_matches))
            at("apps-reset-filter").performClick()
            expect("query:", "filter:ALL")
            at("apps-shown-count").assertTextEquals(text(R.string.apps_shown_count, miss.apps.size, miss.apps.size))
        }
    }

    @Test fun searchFilterAndSortDeliverExactValuesAndReflectControlledState() {
        show(results())
        at("apps-search").performTextReplacement("TEST.FIXTURE.NOTES")
        expect("query:TEST.FIXTURE.NOTES")
        at("apps-shown-count").assertTextEquals(text(R.string.apps_shown_count, 1, 3))
        at("apps-details-${user.packageName}").assertTextContains(user.label)
        at("apps-search").performTextReplacement("")
        expect("query:")
        listOf(AppFilter.SYSTEM to 1, AppFilter.USER to 2, AppFilter.ALL to 3).forEach { (filter, count) ->
            at("apps-filter-${filter.name.lowercase(Locale.ROOT)}").performClick().assertIsSelected()
            expect("filter:${filter.name}")
            at("apps-shown-count").assertTextEquals(text(R.string.apps_shown_count, count, 3))
        }
        listOf(AppSort.PACKAGE, AppSort.NAME).forEach { sort ->
            at("apps-sort-${sort.name.lowercase(Locale.ROOT)}").performClick().assertIsSelected()
            expect("sort:${sort.name}")
        }
    }

    @Test fun inlineDetailsDeliverExactPackagesAndProtectSystemAndSelf() {
        show(results())
        apps.forEach { app ->
            val card = "apps-card-${app.packageName}"
            val details = "apps-details-${app.packageName}"
            at(details).performClick()
            at("apps-info-${app.packageName}").assertIsEnabled().performClick()
            textIn(card, text(R.string.apps_version, app.versionName ?: text(R.string.unknown_value)))
            textIn(card, text(R.string.apps_target_sdk, app.targetSdk?.toString() ?: text(R.string.unknown_value)))
            textIn(card, text(R.string.apps_storage_unknown))
            if (app == user) {
                at("apps-uninstall-${app.packageName}").assertIsEnabled().performClick()
                expect("info:${app.packageName}", "uninstall:${app.packageName}")
            } else {
                textIn(card, text(if (app.system) R.string.app_inventory_system_protected else R.string.apps_self_protected))
                // The protected card is composed here; absence is not an offscreen false positive.
                compose.onNodeWithTag("apps-uninstall-${app.packageName}").assertDoesNotExist()
                expect("info:${app.packageName}")
            }
            at(details).assertTextContains(text(R.string.apps_hide_details)).performClick()
            compose.onNodeWithTag("apps-info-${app.packageName}").assertDoesNotExist()
            at(details).assertTextContains(text(R.string.apps_show_details))
        }
    }

    @Test fun approvalNamesExactPackageAndSeparatesDismissFromConfirm() {
        // Same label cannot substitute for the exact approved package.
        val state = results().copy(apps = apps + user.copy(packageName = "test.fixture.other"), approval = user)
        show(state)
        compose.onNodeWithTag("apps-approved-package").assertTextEquals(user.packageName)
        compose.onNodeWithTag("apps-dismiss-uninstall").assertIsEnabled().performClick()
        expect("dismiss")
        compose.onNodeWithTag("apps-approved-package").assertDoesNotExist()
        update(state)
        compose.onNodeWithTag("apps-approved-package").assertTextEquals(user.packageName)
        compose.onNodeWithTag("apps-confirm-uninstall").assertIsEnabled()
            .assertTextEquals(text(R.string.apps_open_confirmation)).performClick()
        expect("confirm")
        compose.onNodeWithTag("apps-approved-package").assertDoesNotExist()
        compose.runOnIdle { assertEquals(state.apps, ui.value.apps) }
    }

    @Test fun returnedReceiptDoesNotClaimRemovalCancellationOrFreedSpace() {
        show(results().copy(receipt = AppManagementReceipt(
            AppManagementAction.UNINSTALL, user, AppManagementOutcome.RETURNED)))
        at("apps-handoff-status").assertTextEquals(text(R.string.apps_handoff_returned))
            .assertTextContains("does not confirm removal, cancellation, or freed space", substring = true)
        textIn("apps-handoff", user.packageName)
        textIn("apps-handoff", user.label)
        at("apps-visible-count").assertTextEquals(text(R.string.apps_visible_count, 3))
        at("apps-scan-orb").assertIsEnabled()
        expect()
    }

    private data class LayoutMode(val width: Int = 411, val night: Boolean = true,
        val language: String = "en", val font: Float = 1f) {
        val direction get() = if (language == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
        val label get() = "$width-${if (night) "night" else "day"}-$language-$font"
    }

    @Composable
    private fun LayoutFixture(mode: LayoutMode) {
        val base = LocalContext.current
        val original = LocalConfiguration.current
        val nativeDensity = LocalDensity.current
        BoxWithConstraints(Modifier.fillMaxSize()) {
            // Local scaled layout, never wm/settings changes or physical-screen proof.
            val density = maxWidth.value * nativeDensity.density / mode.width
            SideEffect { fixtureDensity = density }
            val config = remember(original, mode) {
                Configuration(original).apply {
                    val locale = Locale.forLanguageTag(mode.language)
                    setLocale(locale); setLayoutDirection(locale)
                    screenWidthDp = mode.width; fontScale = mode.font
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                        if (mode.night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                }
            }
            val localized = remember(base, config) { base.createConfigurationContext(config) }
            CompositionLocalProvider(LocalContext provides localized, LocalConfiguration provides config,
                LocalDensity provides Density(density, mode.font), LocalLayoutDirection provides mode.direction) {
                InstalledAppsScreen(mode.night, ui.value, self.packageName,
                    onBack = { events += "back" }, onScan = { events += "scan" }, onCancel = { events += "cancel" },
                    onQuery = { events += "query:$it"; ui.value = ui.value.copy(query = it) },
                    onFilter = { events += "filter:$it"; ui.value = ui.value.copy(filter = it) },
                    onSort = { events += "sort:$it"; ui.value = ui.value.copy(sort = it) },
                    onRequestUninstall = { events += "uninstall:$it" },
                    onDismissApproval = { events += "dismiss"; ui.value = ui.value.copy(approval = null) },
                    onConfirmUninstall = { events += "confirm"; ui.value = ui.value.copy(approval = null) },
                    onAppSettings = { events += "info:$it" }, onAllSettings = { events += "settings" })
            }
        }
    }

    private fun actionSize(node: SemanticsNodeInteraction, label: String) {
        val bounds = node.assertIsDisplayed().assertIsEnabled().assertHasClickAction().fetchSemanticsNode().boundsInRoot
        // Semantics bounds are pixels; use the fixture density, not the host test-rule density.
        assertTrue("$label needs a 48dp action: $bounds", bounds.width >= 48 * fixtureDensity - 1 &&
            bounds.height >= 48 * fixtureDensity - 1)
    }

    private fun readableComposedText(mode: LayoutMode) {
        val nodes = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult) and
            !SemanticsMatcher.keyIsDefined(SemanticsActions.SetText) and
            hasAnyAncestor(hasTestTag("route-apps")), useUnmergedTree = true)
        val count = nodes.fetchSemanticsNodes().size
        assertTrue("${mode.label}: expected composed text", count > 0)
        repeat(count) { index ->
            val layouts = mutableListOf<TextLayoutResult>()
            nodes[index].performSemanticsAction(SemanticsActions.GetTextLayoutResult) { assertTrue(it(layouts)) }
            val actual = layouts.single()
            val input = actual.layoutInput
            // Inspect the actual rendered TextLayoutResult. A reconstructed narrower
            // constraint can falsely report Arabic 1.6x overflow on API 35.
            // True width, height, or ellipsis overflow on any rendered node still fails.
            assertFalse("${mode.label}: clipped ${input.text}",
                actual.didOverflowWidth || actual.didOverflowHeight ||
                    (0 until actual.lineCount).any(actual::isLineEllipsized))
            assertEquals(mode.direction, input.layoutDirection)
            assertEquals(mode.font, input.density.fontScale, .001f)
        }
    }

    @Test fun scaledWidthThemeLocaleFontMatrixKeepsLongAppDetailsReadable() {
        val modes = listOf(360, 411).flatMap { width -> listOf(LayoutMode(width),
            LayoutMode(width, language = "ar"), LayoutMode(width, language = "ar", font = 1.6f),
            LayoutMode(width, night = false), LayoutMode(width, night = false, language = "ar", font = 1.6f)) }
        val longApp = user.copy(packageName = "test.fixture.extremely.long.application.identifier.for.layout.regression",
            label = "Fixture Notes with a deliberately long application name — تطبيق تجريبي باسم طويل لاختبار الالتفاف")
        ui.value = results().copy(apps = listOf(longApp))
        val mode = mutableStateOf(modes.first())
        compose.setContent { key(mode.value) { LayoutFixture(mode.value) } }
        modes.forEach { current ->
            compose.runOnIdle { mode.value = current }
            val resources = context.createConfigurationContext(Configuration(context.resources.configuration).apply {
                setLocale(Locale.forLanguageTag(current.language))
            })
            actionSize(compose.onNodeWithContentDescription(resources.getString(R.string.back)), "${current.label}/back")
            captureEvidence(context, compose, "apps-layout-fixture-${current.label}-top")
            readableComposedText(current)
            listOf("apps-scan-orb", "apps-system-settings", "apps-search", "apps-filter-all", "apps-filter-user",
                "apps-filter-system", "apps-sort-name", "apps-sort-package").forEach { tag ->
                actionSize(at(tag), "${current.label}/$tag")
                readableComposedText(current)
            }
            val details = "apps-details-${longApp.packageName}"
            actionSize(at(details), "${current.label}/details")
            at(details).performClick()
            at("apps-info-${longApp.packageName}")
            textIn(details, longApp.label)
            textIn(details, longApp.packageName)
            readableComposedText(current)
            listOf("apps-info", "apps-uninstall").forEach { prefix ->
                actionSize(at("$prefix-${longApp.packageName}"), "${current.label}/$prefix")
            }
            at(details)
            captureEvidence(context, compose, "apps-layout-fixture-${current.label}-expanded-app")
        }
        expect()
    }
}
