package com.knoux.kxprivatevpn

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.designsystem.GlassSurface
import com.knoux.kxprivatevpn.features.AutopilotScreen
import com.knoux.kxprivatevpn.features.AutopilotUiState
import com.knoux.kxprivatevpn.launch.IdentityExperience
import com.knoux.kxprivatevpn.platform.repair.AutopilotKind
import com.knoux.kxprivatevpn.platform.repair.AutopilotPlan
import com.knoux.kxprivatevpn.platform.repair.AutopilotTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.Locale
import java.util.UUID

/** Stateless fixtures only: no app launch, repositories, preferences, cache or history access. */
class VisualHardeningTest {
    @get:Rule val compose = createComposeRule()

    private data class Mode(
        val widthDp: Int? = null,
        val night: Boolean = true,
        val language: String = "en",
        val font: Float = 1f,
    ) {
        val foreground: Color get() = if (night) Color(0xFFF7F3FF) else Color(0xFF171050)
        val label: String get() = "${widthDp ?: "full"}-${if (night) "night" else "day"}-$language-$font"
    }

    // These are labels, not paths that will ever be opened or deleted.
    private fun cache(name: String, bytes: Long) = AutopilotTarget(
        "cache:$name", AutopilotKind.CACHE_FILE, name, "kx_cache_file", bytes, true,
    )

    private fun samplePlan() = AutopilotPlan(
        targets = listOf(cache("a.tmp", 1024), cache("b.tmp", 2048), AutopilotTarget(
            "history", AutopilotKind.HISTORY, "aged_kx_history", "1+2", 0, true,
        )),
        measuredAtMillis = 1_760_000_000_000L,
        cacheMeasured = true, agedJournal = 1, agedUsage = 2,
    )

    private fun selected(plan: AutopilotPlan) = AutopilotUiState(
        loading = false, plan = plan, selected = plan.targets.map { it.id }.toSet(),
    )

    @Composable
    private fun Fixture(mode: Mode, content: @Composable () -> Unit) {
        val base = LocalContext.current
        val original = LocalConfiguration.current
        val nativeDensity = LocalDensity.current
        BoxWithConstraints(Modifier.fillMaxSize()) {
            // Fit 360/411dp into the host using ONLY local density; never change wm/settings.
            // These scaled layouts are not physical-size baseline comparisons.
            val density = mode.widthDp?.let { maxWidth.value * nativeDensity.density / it }
                ?: nativeDensity.density
            val config = remember(original, mode, density) {
                Configuration(original).apply {
                    val locale = Locale.forLanguageTag(mode.language)
                    setLocale(locale)
                    setLayoutDirection(locale)
                    fontScale = mode.font
                    screenWidthDp = mode.widthDp ?: original.screenWidthDp
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                        if (mode.night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                }
            }
            val localized = remember(base, config) { base.createConfigurationContext(config) }
            CompositionLocalProvider(
                LocalContext provides localized,
                LocalConfiguration provides config,
                LocalLayoutDirection provides if (mode.language == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr,
                LocalDensity provides Density(density, mode.font),
            ) {
                // Deliberately wrong host: each screen/card must own its foreground.
                MaterialTheme(colorScheme = lightColorScheme()) {
                    CompositionLocalProvider(LocalContentColor provides Color.Black) {
                        Box(Modifier.fillMaxSize()) { content() }
                    }
                }
            }
        }
    }

    @Composable
    private fun PlanScreen(
        mode: Mode, state: AutopilotUiState,
        toggle: (String) -> Unit = {}, request: () -> Unit = {}, confirm: () -> Unit = {},
    ) {
        AutopilotScreen(
            night = mode.night, state = state, onBack = {}, onMeasure = {},
            onToggle = toggle, onSelectAll = {}, onClearSelection = {},
            onRequestRun = request, onDismissConfirmation = {}, onConfirmRun = confirm, onCancel = {},
        )
    }

    private fun text(tag: String) = compose.onNodeWithTag(tag, useUnmergedTree = true)

    private fun layout(node: SemanticsNodeInteraction): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
            assertTrue("Text must expose its actual layout", action(results))
        }
        return results.single()
    }

    private fun readable(tag: String, color: Color? = null): TextLayoutResult {
        val node = text(tag).performScrollTo().assertIsDisplayed()
        return layout(node).also {
            assertFalse("$tag must wrap without clipping/ellipsis", it.hasVisualOverflow)
            if (color != null) assertEquals("$tag resolved Text color", color, it.layoutInput.style.color)
        }
    }

    @Test fun standaloneGlassOverridesWrongHostTextColorAcrossThemeChanges() {
        val mode = mutableStateOf(Mode())
        compose.setContent {
            Fixture(mode.value) {
                GlassSurface(night = mode.value.night) {
                    Text("Default glass text / نص الزجاج", Modifier.testTag("glass-default"))
                }
            }
        }
        for (night in listOf(true, false, true)) {
            compose.runOnIdle { mode.value = Mode(night = night) }
            val result = layout(text("glass-default").assertIsDisplayed())
            assertEquals(mode.value.foreground, result.layoutInput.style.color)
            assertFalse(result.hasVisualOverflow)
        }
    }

    @Test fun autopilotSampleSurvivesLocalThemeFontLocaleAndWidthTransitions() = layoutMatrix(false)

    @Test fun mixedFilenameAndArabicCopyWrapWithoutOverflowAcrossLayoutFixtures() = layoutMatrix(true)

    private fun layoutMatrix(stress: Boolean) {
        val sample = samplePlan()
        val longName = "report_20261107_نسخة-نهائية_".repeat(5) + "1234567890.tmp"
        val plan = if (stress) sample.copy(targets = listOf(cache(longName, 1024)) + sample.targets.drop(1)) else sample
        val mode = mutableStateOf(Mode(widthDp = 360))
        compose.setContent { Fixture(mode.value) { PlanScreen(mode.value, selected(plan)) } }
        for (width in listOf(360, 411)) for (language in listOf("en", "ar")) {
            for (font in listOf(1f, 1.6f)) for (night in listOf(false, true)) {
                val current = Mode(width, night, language, font)
                compose.runOnIdle { mode.value = current }
                readable("autopilot-plan", current.foreground)
                readable("autopilot-plan-summary", current.foreground)
                for (index in plan.targets.indices) {
                    val title = readable("autopilot-target-title-$index", current.foreground)
                    if (index < 2) text("autopilot-target-title-$index").assertTextEquals(plan.targets[index].title)
                    if (stress && index == 0) assertTrue("Long filename must exceed two lines", title.lineCount > 2)
                    readable("autopilot-target-detail-$index")
                    readable("autopilot-approval-$index")
                }
                if (font == 1.6f) {
                    compose.onNodeWithTag("autopilot-clear").performScrollTo().assertIsDisplayed()
                    val actions = listOf("measure", "select-all", "clear").map {
                        compose.onNodeWithTag("autopilot-$it").assertIsDisplayed()
                            .assertHeightIsAtLeast(48.dp).fetchSemanticsNode().boundsInRoot
                    }
                    assertTrue("Large-font actions must stack", actions.zipWithNext().all { (a, b) -> a.bottom <= b.top })
                }
                compose.onNodeWithTag("autopilot-run").performScrollTo().assertIsDisplayed().assertIsEnabled()
                readable("autopilot-scope")
                capture("layout-fixture-autopilot-${if (stress) "mixed-name" else "sample"}-${current.label}", current)
            }
        }
    }

    @Test fun labelledCheckboxTargetsToggleOnceMeet48dpAndDisableWhileBusy() {
        val mode = Mode(360, true, "ar", 1.6f)
        val state = mutableStateOf(selected(samplePlan()))
        var toggles = 0
        var requests = 0
        var confirmations = 0
        compose.setContent {
            Fixture(mode) {
                PlanScreen(mode, state.value, toggle = { id ->
                    toggles++
                    state.value = state.value.copy(selected = state.value.selected.let {
                        if (id in it) it - id else it + id
                    })
                }, request = { requests++ }, confirm = { confirmations++ })
            }
        }
        val ar = strings(InstrumentationRegistry.getInstrumentation().targetContext, "ar")
        compose.onNodeWithContentDescription(ar.getString(R.string.back))
            .assertHasClickAction().assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        for (index in 0..2) {
            val title = if (index == 2) ar.getString(R.string.autopilot_history_title) else listOf("a.tmp", "b.tmp")[index]
            compose.onNodeWithTag("autopilot-select-$index").performScrollTo()
                .assertIsDisplayed().assertIsOn().assertIsEnabled().assertHasClickAction()
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
                .assertTextContains(title)
                .assertTextContains(ar.getString(R.string.autopilot_destructive))
                .assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
            text("autopilot-approval-$index").performScrollTo().assertIsDisplayed()
        }
        val first = compose.onNodeWithTag("autopilot-select-0")
        first.performScrollTo().performClick().assertIsOff()
        compose.runOnIdle { assertEquals(1, toggles) }
        compose.onNodeWithTag("autopilot-run").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(1, requests)
            assertEquals("Request is not destructive approval", 0, confirmations)
            state.value = state.value.copy(busy = true)
        }
        for (index in 0..2) {
            compose.onNodeWithTag("autopilot-select-$index").performScrollTo()
                .assertIsNotEnabled().performTouchInput { click() }
        }
        for (action in listOf("measure", "select-all", "clear")) {
            compose.onNodeWithTag("autopilot-$action").assertIsNotEnabled()
        }
        compose.onNodeWithTag("autopilot-run").assertDoesNotExist()
        compose.onNodeWithTag("autopilot-cancel").performScrollTo().assertIsDisplayed().assertIsEnabled()
        compose.runOnIdle { assertEquals("Disabled taps cannot toggle", 1, toggles) }
    }

    @Test fun baseline49SampleAtNativePhysicalSizeEndsAtArabicLargeTextScope() {
        val mode = Mode(night = true, language = "ar", font = 1.6f)
        compose.setContent { Fixture(mode) { PlanScreen(mode, selected(samplePlan())) } }
        readable("autopilot-plan", mode.foreground)
        readable("autopilot-target-title-2", mode.foreground)
        readable("autopilot-approval-2")
        compose.onNodeWithTag("autopilot-run").performScrollTo().assertIsDisplayed().assertIsEnabled()
        // Same synthetic sample and final scroll anchor as baseline 49; no forced width/crop.
        readable("autopilot-scope")
        capture("autopilot-baseline49-sample-after-native-night-ar-1.6-scope", mode)
    }

    @Test fun identityNeverEntersAutomaticallyOrOffersContinueAlongsideOrb() {
        compose.mainClock.autoAdvance = false
        var enters = 0
        compose.setContent { Fixture(Mode()) { IdentityExperience(onEnter = { enters++ }) } }
        compose.mainClock.advanceTimeBy(12_000)
        compose.onNodeWithTag("entry-orb").assertIsDisplayed().assertIsEnabled()
        compose.onNodeWithTag("continue").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, enters) }
    }

    @Test fun identityNormalMotionWaitsForSpinIgnoresRepeatedTapAndDeliversOnce() {
        compose.mainClock.autoAdvance = false
        val mode = Mode()
        var enters = 0
        compose.setContent {
            Fixture(mode) { IdentityExperience(finished = true, reducedMotion = false, onEnter = { enters++ }) }
        }
        val orb = compose.onNodeWithTag("entry-orb")
        compose.onNodeWithTag("continue").assertDoesNotExist()
        capture("identity-fixture-not-cold-launch-before-tap", mode)
        orb.assertIsEnabled().performClick()
        compose.mainClock.advanceTimeBy(200, ignoreFrameDuration = true)
        compose.runOnIdle { assertEquals("No navigation at 200ms", 0, enters) }
        orb.assertIsNotEnabled().performTouchInput { click() }
        capture("identity-fixture-not-cold-launch-mid-spin-200ms", mode)
        compose.mainClock.advanceTimeBy(600)
        compose.runOnIdle { assertEquals("One delivery after the 680ms spin", 1, enters) }
        orb.assertIsNotEnabled()
        capture("identity-fixture-not-cold-launch-after-spin", mode)
        compose.mainClock.advanceTimeBy(10_000)
        compose.runOnIdle { assertEquals(1, enters) }
    }

    @Test fun identityRestorationMidSpinCompletesOnceAndRemainsDeliveredAfterRestoration() {
        compose.mainClock.autoAdvance = false
        // The tester's runOnIdle boundaries need frames to actually dispose/recreate children.
        // Keep manual time: autoAdvance would finish the in-flight spin before saving it.
        val restoration = StateRestorationTester(object : ComposeContentTestRule by compose {
            override fun <T> runOnIdle(action: () -> T): T {
                compose.mainClock.advanceTimeByFrame()
                return compose.runOnIdle(action)
            }
        })
        var enters = 0
        var creations = 0
        var disposals = 0
        restoration.setContent {
            DisposableEffect(Unit) {
                creations++
                onDispose { disposals++ }
            }
            Fixture(Mode()) { IdentityExperience(finished = true, reducedMotion = false, onEnter = { enters++ }) }
        }
        compose.onNodeWithTag("entry-orb").performClick()
        compose.mainClock.advanceTimeBy(200)
        compose.runOnIdle { assertEquals(0, enters) }
        restoration.emulateSavedInstanceStateRestore()
        compose.mainClock.advanceTimeBy(200)
        compose.onNodeWithTag("entry-orb").assertIsNotEnabled()
        compose.onNodeWithTag("continue").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals("Recreation must not bypass the spin", 0, enters)
            assertEquals(2, creations)
            assertEquals(1, disposals)
        }
        compose.mainClock.advanceTimeBy(600)
        compose.runOnIdle { assertEquals(1, enters) }
        restoration.emulateSavedInstanceStateRestore()
        compose.mainClock.advanceTimeBy(10_000)
        compose.onNodeWithTag("entry-orb").assertIsNotEnabled()
        compose.runOnIdle {
            assertEquals("Delivered requests stay stopped across recreation", 1, enters)
            assertEquals(3, creations)
            assertEquals(2, disposals)
        }
    }

    private fun capture(label: String, mode: Mode) {
        // No readback at all unless explicitly opted in, including on headless API35.
        if (InstrumentationRegistry.getArguments().getString("kxCapture") != "true") return
        compose.waitForIdle()
        val bitmap = checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        try {
            val stem = "physical-evidence-$label-${UUID.randomUUID()}"
            val image = File(evidenceDirectory, "$stem.png")
            check(image.createNewFile())
            image.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            // Only dimensions/theme/font, never device identifiers, app state or user content.
            val metadata = File(evidenceDirectory, "$stem.txt")
            check(metadata.createNewFile())
            metadata.writeText("widthPx=${bitmap.width}\nheightPx=${bitmap.height}\n" +
                (mode.widthDp?.let { "layoutWidthDp=$it\n" } ?: "") +
                "theme=${if (mode.night) "night" else "day"}\nfontScale=${mode.font}\n")
        } finally {
            bitmap.recycle()
        }
    }

    private companion object {
        // One fresh directory per test-class run, in the TEST APK sandbox, never targetContext.
        val evidenceDirectory: File by lazy {
            val testContext = InstrumentationRegistry.getInstrumentation().context
            File(checkNotNull(testContext.getExternalFilesDir(null)), "physical-evidence/${UUID.randomUUID()}")
                .apply { check(mkdirs()) }
        }
    }
}
