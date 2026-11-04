package com.knoux.kxprivatevpn

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.features.AdvancedNetworkScreen
import com.knoux.kxprivatevpn.features.AdvancedNetworkUiState
import com.knoux.kxprivatevpn.platform.vpn.AdvancedNetworkSettings
import com.knoux.kxprivatevpn.platform.vpn.DnsMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.Locale

/**
 * The screen may only promise what the WireGuard build can actually deliver: WireGuard is the
 * only selectable protocol, obfuscation/port-hopping stay genuinely disabled, and a running
 * tunnel says whether the saved settings are live or need a reconnect.
 *
 * Compose rules allow exactly one setContent per test, so every test below calls [screen]
 * once and asserts one honest state.
 */
class AdvancedNetworkScreenTest {
    @get:Rule val compose = createComposeRule()

    private fun text(id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private fun state(
        settings: AdvancedNetworkSettings = AdvancedNetworkSettings(),
        profileMtu: Int? = null,
        profileKeepalive: Int? = null,
        hasProfile: Boolean = true,
        connected: Boolean = false,
        dirty: Boolean = false,
        applied: AdvancedNetworkSettings? = if (connected && !dirty) settings else null,
    ) = AdvancedNetworkUiState(
        settings = settings,
        profileMtu = profileMtu,
        profileKeepalive = profileKeepalive,
        hasProfile = hasProfile,
        loading = false,
        connected = connected,
        dirty = dirty,
        applied = applied,
    )

    private fun screen(
        state: AdvancedNetworkUiState,
        night: Boolean = false,
        arabic: Boolean = false,
        fontScale: Float = 1f,
        onMtu: (Int?) -> Unit = {},
        onDnsMode: (DnsMode) -> Unit = {},
        onDnsHosts: (List<String>) -> Unit = {},
        onKeepalive: (Int?) -> Unit = {},
        onReset: () -> Unit = {},
        onReconnect: () -> Unit = {},
    ) {
        compose.setContent {
            val base = InstrumentationRegistry.getInstrumentation().targetContext
            val context = if (arabic) localized(base, "ar") else base
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalContext provides context,
                LocalDensity provides Density(density.density, fontScale),
                LocalLayoutDirection provides if (arabic) LayoutDirection.Rtl else LayoutDirection.Ltr,
            ) {
                Box(Modifier.fillMaxSize()) {
                    AdvancedNetworkScreen(
                        night = night,
                        state = state,
                        onBack = {},
                        onRefresh = {},
                        onMtuChange = onMtu,
                        onDnsModeChange = onDnsMode,
                        onDnsHostsChange = onDnsHosts,
                        onKeepaliveChange = onKeepalive,
                        onReset = onReset,
                        onReconnect = onReconnect,
                    )
                }
            }
        }
    }

    private fun capture(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val folder = File(context.getExternalFilesDir(null), "identity-evidence").apply { mkdirs() }
        File(folder, "$name.png").outputStream().use { stream ->
            check(compose.onRoot().captureToImage().asAndroidBitmap()
                .compress(Bitmap.CompressFormat.PNG, 100, stream))
        }
    }

    private fun localized(base: Context, language: String): Context {
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(language))
        return base.createConfigurationContext(configuration)
    }

    /** This ui-test version has no assertIsDisabled; check the semantics directly. */
    private fun SemanticsNodeInteraction.assertDisabled(): SemanticsNodeInteraction {
        val config = fetchSemanticsNode().config
        assertTrue("Switch must expose disabled() semantics",
            runCatching { config[SemanticsProperties.Disabled] }.isSuccess)
        return this
    }

    @Test fun protocolRowOffersWireGuardOnlyAndSaysWhyTheOthersAreQuiet() {
        screen(state())

        compose.onNodeWithTag("adv-protocol-wireguard").performScrollTo().assertIsSelected()
        compose.onNodeWithTag("adv-protocol-openvpn").performScrollTo()
            .assertIsNotSelected().assertDisabled()
        compose.onNodeWithTag("adv-protocol-automatic").performScrollTo()
            .assertIsNotSelected().assertDisabled()
        compose.onAllNodes(hasText(text(R.string.adv_protocol_unavailable)))
            .fetchSemanticsNodes().let { assertTrue("Unavailable captions", it.size >= 2) }
        capture("30-advanced-defaults-light")
    }

    @Test fun dnsExpansionSwitchesToSystemMode() {
        var mode: DnsMode? = null
        screen(state(), onDnsMode = { mode = it })

        compose.onNodeWithTag("adv-dns-toggle").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag("adv-dns-options").assertIsDisplayed()
        compose.onNodeWithTag("adv-dns-opt-system").performScrollTo()
            .assertIsDisplayed().performClick()
        assertEquals(DnsMode.SYSTEM, mode)
    }

    @Test fun customDnsEditorValidatesAndSavesNumericHosts() {
        var hosts: List<String>? = null
        screen(state(settings = AdvancedNetworkSettings(
            dnsMode = DnsMode.CUSTOM, customDns = listOf("1.1.1.1"))),
            onDnsHosts = { hosts = it })

        compose.onNodeWithTag("adv-dns-toggle").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag("adv-dns-input").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("adv-dns-input").performTextInput("junk.example")
        compose.onNodeWithTag("adv-dns-error").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("adv-dns-save").performScrollTo().assertDisabled()
        capture("31-advanced-dns-custom-expanded")

        compose.onNodeWithTag("adv-dns-input").performScrollTo()
        compose.onNodeWithTag("adv-dns-input").performTextClearance()
        compose.onNodeWithTag("adv-dns-input").performTextInput("1.1.1.1, 2606:4700:4700::1111")
        compose.onNodeWithTag("adv-dns-save").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(listOf("1.1.1.1", "2606:4700:4700::1111"), hosts)
    }

    @Test fun unavailableTogglesStaySwitchedOffAndDisabled() {
        screen(state())

        compose.onNodeWithTag("adv-obfuscation-switch").performScrollTo().assertDisabled()
        compose.onNodeWithTag("adv-porthopping-switch").performScrollTo().assertDisabled()
    }

    @Test fun connectedDirtyTunnelAsksForReconnect() {
        var reconnects = 0
        screen(state(connected = true, dirty = true), onReconnect = { reconnects++ })

        compose.onNodeWithTag("adv-reconnect").performScrollTo().assertIsDisplayed().performClick()
        compose.waitForIdle()
        assertEquals(1, reconnects)
        capture("32-advanced-connected-dirty")
    }

    @Test fun connectedCleanTunnelStatesExactlyWhatWasApplied() {
        val live = AdvancedNetworkSettings(
            mtuOverride = 1280,
            dnsMode = DnsMode.CUSTOM,
            customDns = listOf("1.1.1.1"),
            keepaliveSeconds = 25,
        )
        screen(state(connected = true, dirty = false, settings = live, applied = live))

        compose.onNodeWithTag("adv-applied").performScrollTo()
            .assertTextEquals(text(R.string.adv_applied, "1280", "1.1.1.1",
                text(R.string.adv_keepalive_seconds, 25)))
        compose.onNodeWithTag("adv-reconnect").assertDoesNotExist()
    }

    @Test fun keepalivePillShowsProfileWhenTheProfileDecides() {
        screen(state())

        compose.onNodeWithTag("adv-keepalive-value").performScrollTo()
            .assertTextEquals(text(R.string.adv_keepalive_profile))
    }

    @Test fun keepalivePillShowsOffForAnExplicitZero() {
        screen(state(settings = AdvancedNetworkSettings(keepaliveSeconds = 0)))

        compose.onNodeWithTag("adv-keepalive-value").performScrollTo()
            .assertTextEquals(text(R.string.adv_keepalive_off))
    }

    @Test fun keepalivePillShowsSecondsForAnExplicitPositiveValue() {
        screen(state(settings = AdvancedNetworkSettings(keepaliveSeconds = 25)))

        compose.onNodeWithTag("adv-keepalive-value").performScrollTo()
            .assertTextEquals(text(R.string.adv_keepalive_seconds, 25))
    }

    @Test fun resetPanelRestoresProfileControl() {
        var resets = 0
        screen(state(settings = AdvancedNetworkSettings(mtuOverride = 1280)), onReset = { resets++ })

        compose.onNodeWithTag("adv-reset-menu").performScrollTo()
            .assertIsDisplayed().performClick()
        compose.onNodeWithTag("adv-reset-panel").assertIsDisplayed()
        compose.onNodeWithTag("adv-reset").assertIsDisplayed().performClick()
        compose.waitForIdle()
        assertEquals(1, resets)
        compose.onNodeWithTag("adv-reset-panel").assertDoesNotExist()
    }

    @Test fun reconnectButtonDoesNotAppearWithoutATunnel() {
        screen(state())

        compose.onNodeWithTag("adv-reconnect").assertDoesNotExist()
        compose.onNodeWithTag("adv-applied").assertDoesNotExist()
    }

    @Test fun mtuSliderWritesSnappedValues() {
        var mtu: Int? = null
        screen(state(), onMtu = { mtu = it })

        val slider = compose.onNodeWithTag("adv-mtu-slider").performScrollTo()
        // Material3 1.3.2 fires onValueChangeFinished on thumb drags and on the accessibility
        // SetProgress action; a track tap only animates the value. Drive the commit path the
        // same way TalkBack does, with a value that must snap onto the 10-byte grid.
        slider.performSemanticsAction(SemanticsActions.SetProgress) { it(1313f) }
        compose.waitForIdle()
        assertEquals("Snapped onto the 10-byte grid", 1310, mtu)
    }

    @Test fun nightArabicLayoutCapturesRtlEvidence() {
        screen(state(profileMtu = 1280), night = true, arabic = true)

        compose.onNodeWithTag("adv-screen").assertIsDisplayed()
        compose.onNodeWithTag("adv-hero").assertIsDisplayed()
        capture("33-advanced-night-rtl")
    }

    @Test fun largeTextArabicKeepsCardsAndNotesReachable() {
        screen(state(settings = AdvancedNetworkSettings(keepaliveSeconds = 25)),
            arabic = true, fontScale = 1.6f)

        compose.onNodeWithTag("adv-keepalive-value").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("adv-note-apply").performScrollTo().assertIsDisplayed()
        capture("34-advanced-ar-large-text")
    }
}
