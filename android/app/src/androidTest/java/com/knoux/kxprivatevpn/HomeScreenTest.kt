package com.knoux.kxprivatevpn

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
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
import com.knoux.kxprivatevpn.data.config.Profile
import com.knoux.kxprivatevpn.data.config.ProfileSnapshot
import com.knoux.kxprivatevpn.features.KxHomeScreen
import com.knoux.kxprivatevpn.platform.vpn.VpnPhase
import com.knoux.kxprivatevpn.platform.vpn.VpnSession
import com.knoux.kxprivatevpn.platform.vpn.VpnState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File
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
        val folder = File(context.getExternalFilesDir(null), "identity-evidence").apply { mkdirs() }
        File(folder, "$name.png").outputStream().use { stream ->
            check(compose.onRoot().captureToImage().asAndroidBitmap()
                .compress(Bitmap.CompressFormat.PNG, 100, stream))
        }
    }

    @Test fun disconnectedWithoutProfileShowsOneActionAndNoFakeMetrics() {
        screen(ProfileSnapshot(emptyList(), null), VpnState())

        // The profile name lives inside the clickable card, so its semantics are merged into
        // the card node; the tag is only reachable through the unmerged tree.
        compose.onNodeWithTag("home-profile", useUnmergedTree = true)
            .assertTextEquals(context.getString(R.string.no_profiles))
        compose.onNodeWithTag("vpn-connect").assertIsNotEnabled()
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
        val session = VpnSession(
            profileId = profile.id,
            profileName = profile.name,
            endpoint = profile.endpoint,
            startedAtMillis = 1_700_000_000_000,
            rxBytes = 2_048,
            txBytes = 1_024,
            latestHandshakeMillis = 1_700_000_060_000,
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
        compose.onNodeWithTag("open-session").assertIsDisplayed()
        compose.onNodeWithTag("vpn-connect").assertDoesNotExist()
        compose.onNodeWithTag("metric-received")
            .assertTextEquals(android.text.format.Formatter.formatFileSize(context, 2_048))
        compose.onNodeWithTag("metric-sent")
            .assertTextEquals(android.text.format.Formatter.formatFileSize(context, 1_024))
        compose.onNodeWithTag("metric-tunnel").assertTextEquals("10.7.0.2")
        capture("72-home-connected-en")
    }

    @Test fun navigationAndToolsActuallyInvokeTargets() {
        var route: String? = null
        screen(ProfileSnapshot(listOf(profile), profile.id), VpnState(),
            onNavigate = { route = it })

        compose.onNodeWithTag("open-storage").performScrollTo().performClick()
        assertEquals("storage", route)
        route = null
        compose.onNodeWithTag("nav-servers").performClick()
        assertEquals("servers", route)
        route = null
        compose.onNodeWithTag("open-repair").performScrollTo().performClick()
        assertEquals("repair", route)
    }

    @Test fun nightArabicHomeIsRtlAndReadable() {
        val ar = localized(context, "ar")
        screen(ProfileSnapshot(listOf(profile), profile.id), VpnState(),
            arabic = true, night = true)
        compose.onNodeWithText(ar.getString(R.string.nav_home)).assertExists()
        compose.onNodeWithTag("home-profile", useUnmergedTree = true).assertExists()
        capture("71-home-night-ar")
    }
}