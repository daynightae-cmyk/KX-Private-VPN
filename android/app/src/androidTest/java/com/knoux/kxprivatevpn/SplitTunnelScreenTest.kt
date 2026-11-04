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
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.features.SplitTunnelScreen
import com.knoux.kxprivatevpn.features.SplitTunnelUiState
import com.knoux.kxprivatevpn.platform.apps.InstalledApp
import com.knoux.kxprivatevpn.platform.vpn.SplitTunnelMode
import com.knoux.kxprivatevpn.platform.vpn.SplitTunnelPolicy
import com.knoux.kxprivatevpn.platform.vpn.SplitTunnelSelection
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.Locale

/**
 * The screen may only show routing that can actually be sent to Android: installed packages,
 * one mode at a time, and an explicit warning when an allow-list would be empty.
 */
class SplitTunnelScreenTest {
    @get:Rule val compose = createComposeRule()
    private val own = "com.knoux.kxprivatevpn"
    private val apps = listOf(
        InstalledApp("com.example.browser", "Chrome"),
        InstalledApp("com.example.chat", "WhatsApp"),
        InstalledApp("com.example.music", "Spotify"),
    )

    private fun text(id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private fun state(
        mode: SplitTunnelMode = SplitTunnelMode.ONLY_VPN,
        selected: Set<String> = setOf("com.example.browser"),
        connected: Boolean = false,
        dirty: Boolean = false,
    ): SplitTunnelUiState {
        val selection = SplitTunnelSelection(mode, selected)
        val plan = SplitTunnelPolicy.plan(selection, apps.mapTo(linkedSetOf()) { it.packageName }, own)
        return SplitTunnelUiState(
            selection = selection,
            installed = apps,
            plan = plan,
            loading = false,
            connected = connected,
            dirty = dirty,
            appliedThrough = if (connected) plan.throughVpnCount else 0,
            appliedBypass = if (connected) plan.bypassCount else 0,
        )
    }

    private fun screen(
        state: SplitTunnelUiState,
        night: Boolean = false,
        arabic: Boolean = false,
        fontScale: Float = 1f,
        onToggle: (String) -> Unit = {},
        onMode: (SplitTunnelMode) -> Unit = {},
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
                    SplitTunnelScreen(
                        night = night,
                        state = state,
                        onBack = {},
                        onModeChange = onMode,
                        onToggleApp = onToggle,
                        onRefresh = {},
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

    @Test fun onlyThroughVpnShowsInstalledSelectionAndHonestCounts() {
        screen(state(selected = setOf("com.example.browser", "com.example.music")))

        compose.onNodeWithTag("split-summary-apps")
            .assertTextEquals(text(R.string.split_summary_apps, 3))
        compose.onNodeWithTag("split-summary-through")
            .assertTextEquals(text(R.string.split_summary_through, 2))
        compose.onNodeWithTag("split-summary-bypass")
            .assertTextEquals(text(R.string.split_summary_bypass, 1))
        compose.onNodeWithTag("split-mode-only").assertIsSelected()
        capture("25-split-tunnel-only-mode-light")
        compose.onNodeWithTag("split-app-status-com.example.browser")
            .performScrollTo().assertTextEquals(text(R.string.split_only_vpn))
        compose.onNodeWithTag("split-app-status-com.example.chat")
            .performScrollTo().assertTextEquals(text(R.string.split_bypass))
    }

    @Test fun bypassModeIsTheInverseMappingAndNeverSendsBothLists() {
        val bypass = state(mode = SplitTunnelMode.BYPASS, selected = setOf("com.example.chat"))
        assertEquals(emptySet<String>(), bypass.plan.includedPackages)
        assertEquals(setOf("com.example.chat"), bypass.plan.excludedPackages)
        screen(bypass)

        compose.onNodeWithTag("split-summary-through")
            .assertTextEquals(text(R.string.split_summary_through, 2))
        compose.onNodeWithTag("split-summary-bypass")
            .assertTextEquals(text(R.string.split_summary_bypass, 1))
        compose.onNodeWithTag("split-mode-bypass").assertIsSelected()
        capture("26-split-tunnel-bypass-mode-light")
        compose.onNodeWithTag("split-app-status-com.example.chat")
            .performScrollTo().assertTextEquals(text(R.string.split_bypass))
    }

    @Test fun emptyOnlySelectionWarnsInsteadOfBlockingEveryApp() {
        screen(state(selected = emptySet()))

        compose.onNodeWithTag("split-warning-no-apps").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("split-warning-no-apps")
            .assertTextContains(text(R.string.split_warning_no_apps))
        compose.onNodeWithTag("split-summary-through")
            .assertTextEquals(text(R.string.split_summary_through, 3))
        compose.onNodeWithTag("split-summary-bypass")
            .assertTextEquals(text(R.string.split_summary_bypass, 0))
        // No allow-list is applied, so Android routes every app. Rows must say so instead
        // of falsely showing "Bypass VPN" while traffic still enters the tunnel.
        compose.onNodeWithTag("split-app-status-com.example.browser")
            .performScrollTo().assertTextEquals(text(R.string.split_only_vpn))
        compose.onNodeWithTag("split-app-status-com.example.chat")
            .assertTextEquals(text(R.string.split_only_vpn))
    }

    @Test fun searchFiltersByLabelAndReportsNoMatch() {
        screen(state())

        compose.onNodeWithTag("split-search").performTextInput("whatsapp")
        compose.onNodeWithTag("split-app-com.example.chat").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("split-app-com.example.browser").assertDoesNotExist()

        compose.onNodeWithTag("split-search").performTextClearance()
        compose.onNodeWithTag("split-search").performTextInput("no such app")
        compose.onNodeWithTag("split-no-match").performScrollTo().assertIsDisplayed()
    }

    @Test fun toggleAndModeClicksReportOnlyTheTouchedValue() {
        var toggled: String? = null
        var mode: SplitTunnelMode? = null
        screen(state(), onToggle = { toggled = it }, onMode = { mode = it })

        compose.onNodeWithTag("split-app-switch-com.example.chat").performScrollTo().performClick()
        assertEquals("com.example.chat", toggled)

        compose.onNodeWithTag("split-mode-bypass").performClick()
        assertEquals(SplitTunnelMode.BYPASS, mode)
    }

    @Test fun reconnectBannerAppearsForUnappliedChanges() {
        var reconnects = 0
        screen(state(connected = true, dirty = true), onReconnect = { reconnects++ })
        compose.onNodeWithTag("split-reconnect").performScrollTo().assertIsDisplayed()
            .performClick()
        assertEquals(1, reconnects)
    }

    @Test fun appliedBannerReplacesReconnectWhenTheSelectionMatches() {
        screen(state(connected = true, dirty = false))
        compose.onNodeWithTag("split-applied").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("split-reconnect").assertDoesNotExist()
    }

    @Test fun nightArabicLayoutCapturesRtlEvidence() {
        screen(state(selected = setOf("com.example.browser", "com.example.chat")),
            night = true, arabic = true)

        compose.onNodeWithTag("split-screen").assertIsDisplayed()
        compose.onNodeWithTag("split-hero").assertIsDisplayed()
        capture("27-split-tunnel-night-rtl")
    }

    @Test fun largeTextArabicKeepsRowsAndSummaryReachable() {
        screen(state(selected = setOf("com.example.browser", "com.example.chat")),
            arabic = true, fontScale = 1.6f)

        compose.onNodeWithTag("split-summary-apps").assertIsDisplayed()
        compose.onNodeWithTag("split-app-com.example.chat").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("split-app-status-com.example.chat").assertIsDisplayed()
        compose.onNodeWithTag("split-mode-only").assertIsSelected()
        capture("29-split-tunnel-ar-large-text")
    }
}
