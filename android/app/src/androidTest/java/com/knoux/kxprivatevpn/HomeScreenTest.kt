package com.knoux.kxprivatevpn

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.data.config.Profile
import com.knoux.kxprivatevpn.data.config.ProfileSnapshot
import com.knoux.kxprivatevpn.features.HomeServiceCatalog
import com.knoux.kxprivatevpn.features.HomeServiceSection
import com.knoux.kxprivatevpn.features.KxHomeScreen
import com.knoux.kxprivatevpn.platform.vpn.VpnPhase
import com.knoux.kxprivatevpn.platform.vpn.VpnSession
import com.knoux.kxprivatevpn.platform.vpn.VpnState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Locale

/** Consumer Home: one real status, no invented metrics, icon-first tools and navigation. */
class HomeScreenTest {
    @get:Rule val compose = createComposeRule()
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val profile = Profile("p1", "Home tunnel", "198.51.100.7:51820")

    private fun localized(base: Context, language: String): Context {
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(language))
        return base.createConfigurationContext(configuration)
    }

    private fun screen(
        snapshot: ProfileSnapshot,
        vpn: VpnState,
        arabic: Boolean = false,
        night: Boolean = true,
        onConnect: () -> Unit = {},
        onDisconnect: () -> Unit = {},
        onSession: () -> Unit = {},
        onNavigate: (String) -> Unit = {},
    ) {
        compose.setContent {
            val base = InstrumentationRegistry.getInstrumentation().targetContext
            val localizedContext = if (arabic) localized(base, "ar") else base
            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedContext.resources.configuration,
                LocalLayoutDirection provides if (arabic) LayoutDirection.Rtl else LayoutDirection.Ltr,
            ) {
                Box(Modifier.fillMaxSize()) {
                    KxHomeScreen(
                        night = night,
                        snapshot = snapshot,
                        busy = false,
                        vpn = vpn,
                        onConnect = onConnect,
                        onDisconnect = onDisconnect,
                        onSession = onSession,
                        onDismissIssue = {},
                        onNavigate = onNavigate,
                    )
                }
            }
        }
    }

    private fun capture(name: String) {
        captureEvidence(context, compose, name)
    }

    @Test fun disconnectedWithoutProfileShowsOneActionAndNoFakeMetrics() {
        screen(ProfileSnapshot(emptyList(), null), VpnState())

        // The profile name lives inside the clickable card, so its semantics are merged into
        // the card node; the tag is only reachable through the unmerged tree.
        compose.onNodeWithTag("home-profile", useUnmergedTree = true)
            .assertTextEquals(context.getString(R.string.no_profiles))
        compose.onNodeWithTag("vpn-connect").assertIsNotEnabled()
        compose.onNodeWithTag("vpn-orb-action").assertIsNotEnabled()
        compose.onNodeWithTag("vpn-connected").assertDoesNotExist()
        listOf("metric-handshake", "metric-received", "metric-sent", "metric-tunnel")
            .forEach {
                compose.onNodeWithTag(it)
                    .assertTextEquals(context.getString(R.string.unknown_value))
            }
        compose.onNodeWithTag("open-repair").assertExists()
        compose.onNodeWithTag("open-storage").assertExists()
        compose.onNodeWithTag("open-settings").assertExists()
        compose.onNodeWithTag("nav-home").assertExists()
        compose.onNodeWithTag("nav-servers").assertExists()
        capture("70-home-disconnected-en")
    }

    @Test fun connectedStateShowsRealSessionValuesOnly() {
        // A recent fixture timestamp keeps the measured duration readable in evidence
        // instead of showing 26k hours for a session started in 2023.
        val startedAt = System.currentTimeMillis() - 42 * 60_000L
        val session = VpnSession(
            profileId = profile.id,
            profileName = profile.name,
            endpoint = profile.endpoint,
            startedAtMillis = startedAt,
            rxBytes = 2_048,
            txBytes = 1_024,
            latestHandshakeMillis = startedAt + 60_000L,
            addresses = listOf("10.7.0.2/32"),
        )
        screen(
            ProfileSnapshot(listOf(profile), profile.id),
            VpnState(VpnPhase.CONNECTED, session),
            onDisconnect = {},
            onSession = {},
        )

        compose.onNodeWithTag("vpn-connected").assertIsDisplayed()
        compose.onNodeWithTag("vpn-disconnect").assertIsEnabled()
        compose.onNodeWithTag("vpn-orb-action").assertIsEnabled()
        compose.onNodeWithTag("open-session").assertIsDisplayed()
        compose.onNodeWithTag("vpn-connect").assertDoesNotExist()
        compose.onNodeWithTag("metric-received")
            .assertTextEquals(android.text.format.Formatter.formatFileSize(context, 2_048))
        compose.onNodeWithTag("metric-sent")
            .assertTextEquals(android.text.format.Formatter.formatFileSize(context, 1_024))
        compose.onNodeWithTag("metric-tunnel").assertTextEquals("10.7.0.2")
        capture("72-home-connected-en")
    }

    @Test fun connectedHomeShowsTheLiveSessionServerNotTheStoredSelection() {
        val stored = Profile("p2", "Stored next choice", "203.0.113.9:51820")
        val session = VpnSession(profile.id, profile.name, profile.endpoint)
        screen(
            ProfileSnapshot(listOf(profile, stored), stored.id),
            VpnState(VpnPhase.CONNECTED, session),
        )

        compose.onNodeWithTag("home-server-label", useUnmergedTree = true)
            .assertTextEquals(context.getString(R.string.active_server).uppercase())
        compose.onNodeWithTag("home-profile", useUnmergedTree = true)
            .assertTextEquals(profile.name)
        capture("76-home-connected-active-server")
    }

    @Test fun connectedHomeArabicNamesTheActiveServerInArabic() {
        val ar = localized(context, "ar")
        val session = VpnSession(profile.id, profile.name, profile.endpoint)
        screen(
            ProfileSnapshot(listOf(profile), profile.id),
            VpnState(VpnPhase.CONNECTED, session),
            arabic = true,
        )

        compose.onNodeWithTag("home-server-label", useUnmergedTree = true)
            .assertTextEquals(ar.getString(R.string.active_server))
    }

    @Test fun navigationAndToolsActuallyInvokeTargets() {
        val delivered = mutableListOf<String>()
        screen(ProfileSnapshot(listOf(profile), profile.id), VpnState(),
            onNavigate = { delivered += it })

        fun clickAndExpect(tag: String, route: String, scroll: Boolean = true) {
            compose.runOnIdle { delivered.clear() }
            val node = compose.onNodeWithTag(tag)
            if (scroll) node.performScrollTo()
            node.assertIsDisplayed().assertIsEnabled().assertHasClickAction().performClick()
            compose.runOnIdle {
                assertEquals("$tag must deliver exactly one real destination", listOf(route), delivered)
            }
        }

        HomeServiceCatalog.entries.forEach { entry ->
            clickAndExpect("open-${entry.route}", entry.route)
        }
        clickAndExpect("open-repair", "repair")
        clickAndExpect("open-servers", "servers")
        clickAndExpect("open-settings", "settings")
        listOf("servers", "security", "stats", "settings").forEach { route ->
            clickAndExpect("nav-$route", route, scroll = false)
        }
        compose.runOnIdle { delivered.clear() }
        compose.onNodeWithTag("nav-home").assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertTrue("The already selected Home tab must not navigate again", delivered.isEmpty())
        }
    }

    @Test fun nightArabicHomeIsRtlAndReadable() {
        val ar = localized(context, "ar")
        screen(ProfileSnapshot(listOf(profile), profile.id), VpnState(),
            arabic = true, night = true)
        compose.onNodeWithText(ar.getString(R.string.nav_home)).assertExists()
        compose.onNodeWithTag("home-profile", useUnmergedTree = true).assertExists()
        capture("71-home-night-ar")
    }

    private data class HomeLayoutMode(
        val widthDp: Int,
        val night: Boolean = true,
        val language: String = "en",
        val font: Float = 1f,
    ) {
        val direction: LayoutDirection
            get() = if (language == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
        val label: String get() = "$widthDp-${if (night) "night" else "day"}-$language-$font"
    }

    /** Stateless Home only: no activity launch, stores, preferences or system-setting writes. */
    @Composable
    private fun LayoutFixture(mode: HomeLayoutMode) {
        val base = LocalContext.current
        val original = LocalConfiguration.current
        val nativeDensity = LocalDensity.current
        BoxWithConstraints(Modifier.fillMaxSize()) {
            // As in VisualHardeningTest: fit logical widths locally, never change wm/settings.
            // Captures are scaled layout fixtures, not physical-size reference baselines.
            val density = maxWidth.value * nativeDensity.density / mode.widthDp
            val config = remember(original, mode) {
                Configuration(original).apply {
                    val locale = Locale.forLanguageTag(mode.language)
                    setLocale(locale)
                    setLayoutDirection(locale)
                    screenWidthDp = mode.widthDp
                    fontScale = mode.font
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                        if (mode.night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                }
            }
            val localizedContext = remember(base, config) { base.createConfigurationContext(config) }
            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides config,
                LocalLayoutDirection provides mode.direction,
                LocalDensity provides Density(density, mode.font),
            ) {
                KxHomeScreen(
                    night = mode.night,
                    snapshot = ProfileSnapshot(listOf(profile), profile.id),
                    busy = false,
                    vpn = VpnState(),
                    onConnect = {}, onDisconnect = {}, onSession = {},
                    onDismissIssue = {}, onNavigate = {},
                )
            }
        }
    }

    private fun measuredLayout(node: SemanticsNodeInteraction): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
            assertTrue("Text must expose measured layout inputs", action(results))
        }
        val reported = results.single()
        val input = reported.layoutInput
        // Keep the calibrated VisualHardeningTest helper's width correction. Compose 1.8
        // semantics can rebuild a paragraph at parent maxWidth while reporting a smaller
        // actual size; raw didOverflowWidth then falsely diagnoses short text as clipped.
        val measured = TextMeasurer(
            defaultFontFamilyResolver = input.fontFamilyResolver,
            defaultDensity = input.density,
            defaultLayoutDirection = input.layoutDirection,
            cacheSize = 0,
        ).measure(
            text = input.text, style = input.style, placeholders = input.placeholders,
            maxLines = input.maxLines, softWrap = input.softWrap, overflow = input.overflow,
            constraints = Constraints(maxWidth = reported.size.width, maxHeight = input.constraints.maxHeight),
        )
        return measured.copy(size = reported.size)
    }

    private fun assertReadable(label: String, layout: TextLayoutResult) {
        val ellipsized = (0 until layout.lineCount).any(layout::isLineEllipsized)
        assertFalse(
            "$label must wrap without clipping/ellipsis: text=${layout.layoutInput.text}, " +
                "size=${layout.size}, paragraph=${layout.multiParagraph.width}x${layout.multiParagraph.height}, " +
                "widthOverflow=${layout.didOverflowWidth}, heightOverflow=${layout.didOverflowHeight}, " +
                "lines=${layout.lineCount}, ellipsized=$ellipsized",
            layout.hasVisualOverflow || ellipsized,
        )
    }

    private fun assertAllHomeTextReadable(mode: HomeLayoutMode) {
        val nodes = compose.onAllNodes(
            SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),
            useUnmergedTree = true,
        )
        val count = nodes.fetchSemanticsNodes().size
        assertTrue("${mode.label}: catalog text must be composed, not silently absent", count >= 36)
        // Home uses a regular scrolling Column: offscreen text is still fully measured.
        // Inspect all text (including hero, metrics and nav), not only the screenshot viewport.
        repeat(count) { index ->
            val layout = measuredLayout(nodes[index])
            assertReadable("${mode.label}/text-$index", layout)
            assertEquals("${mode.label}/text-$index direction", mode.direction, layout.layoutInput.layoutDirection)
            assertEquals("${mode.label}/text-$index font scale", mode.font, layout.layoutInput.density.fontScale, .001f)
        }
    }

    private fun sectionNode(section: HomeServiceSection, resources: Context) = compose.onNode(
        hasText(resources.getString(section.title)) and hasAnyAncestor(hasTestTag("route-home")),
        useUnmergedTree = true,
    )

    private fun captureLayout(mode: HomeLayoutMode, anchor: String) {
        // Twenty bounded captures in the existing CI evidence upload. Fixtures contain
        // only test-owned values, never profiles or app inventory from the owner's phone.
        compose.waitForIdle()
        capture("home-layout-fixture-${mode.label}-$anchor")
    }

    @Test fun serviceDiscoveryRemainsReadableAcrossWidthThemeLocaleAndFontMatrix() {
        val modes = listOf(360, 411).flatMap { width ->
            listOf(
                HomeLayoutMode(width),
                HomeLayoutMode(width, language = "ar"),
                HomeLayoutMode(width, language = "ar", font = 1.6f),
                HomeLayoutMode(width, night = false),
                HomeLayoutMode(width, night = false, language = "ar", font = 1.6f),
            )
        }
        val mode = mutableStateOf(modes.first())
        compose.setContent {
            // A fresh scroll state per mode makes the top captures reproducible and avoids
            // carrying text-layout results between different density/locale configurations.
            key(mode.value) { LayoutFixture(mode.value) }
        }
        for (current in modes) {
            compose.runOnIdle { mode.value = current }
            val resources = localized(context, current.language)
            compose.onNodeWithTag("open-settings").assertIsDisplayed()
            compose.onNodeWithTag("vpn-orb-action").assertIsDisplayed()
            captureLayout(current, "top")
            assertAllHomeTextReadable(current)

            HomeServiceSection.entries.forEach { section ->
                sectionNode(section, resources).performScrollTo().assertIsDisplayed()
                    .assertTextEquals(resources.getString(section.title))
            }
            HomeServiceCatalog.entries.forEach { entry ->
                val tag = "open-${entry.route}"
                val label = "${current.label}/${entry.route}"
                val card = compose.onNodeWithTag(tag).performScrollTo()
                    .assertIsDisplayed().assertIsEnabled().assertHasClickAction()
                    .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
                    .assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
                val resourceIds = listOf(entry.title, entry.description, entry.capability, entry.action)
                val copy = resourceIds.map { resources.getString(it) }
                assertTrue("$label metadata must not be blank", copy.all { it.isNotBlank() })
                assertEquals("$label needs four different useful labels", 4, copy.toSet().size)
                copy.forEach { value -> card.assertTextContains(value) }
                card.assert(SemanticsMatcher("Decorative orb must not repeat a content description") {
                    !it.config.contains(SemanticsProperties.ContentDescription)
                })
                assertEquals("$label accessible action label", resources.getString(entry.action),
                    card.fetchSemanticsNode().config[SemanticsActions.OnClick].label)
                compose.onAllNodes(
                    hasClickAction() and hasAnyAncestor(hasTestTag(tag)),
                    useUnmergedTree = true,
                ).assertCountEquals(0)

                copy.forEach { value ->
                    val text = compose.onNode(
                        hasText(value) and hasAnyAncestor(hasTestTag(tag)), useUnmergedTree = true,
                    ).performScrollTo().assertIsDisplayed().assertTextEquals(value)
                    val layout = measuredLayout(text)
                    assertReadable(label, layout)
                    assertEquals("$label must not cap discovery text lines", Int.MAX_VALUE, layout.layoutInput.maxLines)
                    assertTrue("$label must allow wrapping", layout.layoutInput.softWrap)
                    if (current.language == "ar") {
                        assertTrue("$label needs Arabic copy, not an English fallback",
                            value.any { it in '\u0600'..'\u06ff' })
                    }
                }
            }
            // Return to the first section after proving every card, including the last, reachable.
            sectionNode(HomeServiceSection.DEVICE, resources).performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("open-storage").assertIsDisplayed()
            captureLayout(current, "services")
        }
    }
}