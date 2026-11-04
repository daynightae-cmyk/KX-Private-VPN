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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.features.SecurityCenterScreen
import com.knoux.kxprivatevpn.features.SecurityUiState
import com.knoux.kxprivatevpn.platform.security.CheckStatus
import com.knoux.kxprivatevpn.platform.security.ProtectionLevel
import com.knoux.kxprivatevpn.platform.security.SecurityFacts
import com.knoux.kxprivatevpn.platform.security.SecurityPolicy
import com.knoux.kxprivatevpn.platform.vpn.VpnLinkInfo
import com.knoux.kxprivatevpn.platform.vpn.VpnPhase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.Locale

/**
 * UI truth: the Security Center may only show "Fully protected" after every measured
 * check passes, and unimplemented controls stay disabled and labelled unavailable.
 */
class SecurityCenterTest {
    @get:Rule val compose = createComposeRule()
    private val pkg = "com.knoux.kxprivatevpn"

    private fun text(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun buildState(
        connected: Boolean,
        verified: Boolean = false,
        issue: Boolean = false,
    ): SecurityUiState {
        val routes = if (verified) listOf("0.0.0.0/0", "::/0")
        else listOf("0.0.0.0/0", "::/0")
        val facts = SecurityFacts(
            tunnelConnected = connected,
            androidLinkPresent = connected,
            handshakeMillis = if (verified) 1_700_000_000_000 else null,
            dnsServers = if (connected) listOf("10.7.0.1", "fd00::1") else emptyList(),
            allowedIps = if (connected) routes else emptyList(),
            alwaysOnApp = if (verified) pkg else null,
            appPackage = pkg,
            systemLockdown = if (verified) true else null,
            consentGranted = verified,
        )
        return SecurityUiState(
            facts = facts,
            assessment = SecurityPolicy.assess(facts),
            link = if (connected) VpnLinkInfo(
                present = true, interfaceName = "tun0", addresses = listOf("10.7.0.2/32"),
                dns = listOf("10.7.0.1"), routes = 2, mtu = 1420,
            ) else VpnLinkInfo(),
            phase = if (connected) VpnPhase.CONNECTED else VpnPhase.IDLE,
            issue = if (issue) com.knoux.kxprivatevpn.platform.vpn.VpnIssue.START_FAILED else null,
            measuredAtMillis = 1_700_000_000_000,
        )
    }

    private fun screen(state: SecurityUiState, night: Boolean = false, arabic: Boolean = false,
                       onKillSwitch: () -> Unit = {}) {
        compose.setContent {
            val base = InstrumentationRegistry.getInstrumentation().targetContext
            val context = if (arabic) localized(base, "ar") else base
            CompositionLocalProvider(
                LocalContext provides context,
                LocalLayoutDirection provides if (arabic) LayoutDirection.Rtl else LayoutDirection.Ltr,
            ) {
                Box(Modifier.fillMaxSize()) {
                    SecurityCenterScreen(
                        night = night,
                        state = state,
                        onBack = {},
                        onOpenVpnSettings = onKillSwitch,
                        onOpenSecuritySettings = {},
                        onOpenSession = {},
                        onRefresh = {},
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

    @Test fun disconnectedStateNeverClaimsProtection() {
        screen(buildState(connected = false))

        compose.onNodeWithTag("security-posture").assertTextEquals(text(R.string.security_hero_inactive))
        compose.onNodeWithTag("security-row-tracker-status")
            .assertTextEquals(text(R.string.security_state_unavailable))
        compose.onNodeWithTag("security-row-auto-reconnect-status")
            .assertTextEquals(text(R.string.security_state_unavailable))
        compose.onNodeWithTag("security-row-dns-status")
            .assertTextEquals(text(R.string.security_state_awaiting))
        compose.onNodeWithTag("security-hero").assertIsDisplayed()
        compose.onNodeWithTag("security-hero").performScrollTo()
        capture("20-security-center-disconnected-light")
    }

    @Test fun connectedWithoutHandshakeNeverShowsFullyProtected() {
        val state = buildState(connected = true, verified = false)
        screen(state)

        assertEquals(ProtectionLevel.UNVERIFIED, state.assessment.protection)
        compose.onNodeWithTag("security-posture")
            .assertTextEquals(text(R.string.security_hero_unverified))
        compose.onNodeWithTag("security-hero").performScrollTo()
        capture("21-security-center-connected-unverified-light")
    }

    @Test fun fullyVerifiedStateRequiresEveryMeasuredCheck() {
        val state = buildState(connected = true, verified = true)
        screen(state)

        assertEquals(ProtectionLevel.VERIFIED, state.assessment.protection)
        compose.onNodeWithTag("security-posture").assertTextEquals(text(R.string.security_hero_verified))
        compose.onNodeWithTag("security-diag-dns").performScrollTo().assertTextEquals("10.7.0.1, fd00::1")
        compose.onNodeWithTag("security-diag-link").performScrollTo().assertTextEquals(
            text(R.string.security_diag_link_present).replace("%1\$s", "tun0"))
        compose.onNodeWithTag("security-open-session").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("security-hero").performScrollTo()
        capture("22-security-center-verified-light")
    }

    @Test fun unimplementedControlsStayDisabledAndSwitchDoesNotFakeAToggle() {
        val state = buildState(connected = true, verified = true)
        var opened = 0
        screen(state, onKillSwitch = { opened++ })

        compose.onNodeWithTag("security-row-tracker-switch").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("security-row-auto-reconnect-switch").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("security-row-kill-switch-switch").performScrollTo().assertIsEnabled()
        compose.onNodeWithTag("security-row-kill-switch-switch").performClick()
        assertEquals(1, opened)
        // The app never flips a system-owned control; it stayed checked after the click.
        compose.onNodeWithTag("security-row-kill-switch-status")
            .assertTextEquals(text(R.string.security_state_on))
    }

    @Test fun nightArabicLayoutCapturesRtlEvidence() {
        screen(buildState(connected = true, verified = true), night = true, arabic = true)

        compose.onNodeWithTag("security-screen").assertIsDisplayed()
        compose.onNodeWithTag("security-posture")
            .assertTextEquals(localized(InstrumentationRegistry.getInstrumentation().targetContext, "ar")
                .getString(R.string.security_hero_verified))
        capture("23-security-center-verified-night-rtl")
    }

    private fun localized(base: Context, language: String): Context {
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(language))
        return base.createConfigurationContext(configuration)
    }
}
