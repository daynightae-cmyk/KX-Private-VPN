package com.knoux.kxprivatevpn

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.features.ProScreen
import com.knoux.kxprivatevpn.features.ProStatus
import com.knoux.kxprivatevpn.features.ProUiState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.Locale

/** Pure UI fixtures: no ProRepository, identity store, MainActivity or network. */
class ProScreenTest {
    @get:Rule val compose = createComposeRule()
    private fun localized(language: String): Context {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        return base.createConfigurationContext(Configuration(base.resources.configuration).apply {
            setLocale(Locale.forLanguageTag(language))
        })
    }
    private fun screen(state: ProUiState, language: String = "en", fontScale: Float = 1f,
        onRefresh: () -> Unit = {}, onRedeem: (String) -> Unit = {}, onContact: () -> Unit = {}) {
        compose.setContent {
            val context = remember { localized(language) }
            val density = LocalDensity.current
            CompositionLocalProvider(LocalContext provides context,
                LocalConfiguration provides context.resources.configuration,
                LocalDensity provides Density(density.density, fontScale),
                LocalLayoutDirection provides if (language == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr) {
                ProScreen(night = language == "ar", state, {}, onRefresh, onRedeem, onContact)
            }
        }
    }
    private fun disabled(tag: String) = compose.onNodeWithTag(tag).performScrollTo()
        .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Disabled))

    @Test fun unconfiguredBuildExplainsAndDisablesNetworkAndAbsentContact() {
        screen(ProUiState(false, false, ProStatus.UNCONFIGURED))
        compose.onNodeWithTag("pro-status").assertTextEquals(localized("en").getString(R.string.pro_unconfigured))
        disabled("pro-refresh")
        disabled("pro-code")
        disabled("pro-redeem")
        disabled("pro-contact")
        compose.onNodeWithTag("pro-contact-note").performScrollTo()
            .assertTextEquals(localized("en").getString(R.string.pro_contact_absent))
    }

    @Test fun openingConfiguredScreenDoesNotCallAnythingAndRefreshIsExplicit() {
        var refresh = 0
        var redeem = 0
        var contact = 0
        screen(ProUiState(true, true), onRefresh = { refresh++ }, onRedeem = { redeem++ }, onContact = { contact++ })
        compose.runOnIdle { assertEquals(0, refresh + redeem + contact) }
        compose.onNodeWithTag("pro-refresh").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, refresh); assertEquals(0, redeem + contact) }
        compose.onNodeWithTag("pro-contact").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, contact) }
    }

    @Test fun secretIsPasswordTransformedAndClearedBeforeRedeemCallback() {
        var submitted: String? = null
        screen(ProUiState(true, false), onRedeem = { submitted = it })
        val code = "ab".repeat(32)
        compose.onNodeWithTag("pro-code").performScrollTo()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password)).performTextInput(code)
        compose.onNodeWithTag("pro-redeem").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(code, submitted) }
        compose.onNodeWithTag("pro-code").performScrollTo().assertTextContains("")
        val editable = compose.onNodeWithTag("pro-code").fetchSemanticsNode().config[SemanticsProperties.EditableText]
        assertEquals("", editable.text)
        disabled("pro-redeem")
    }

    @Test fun busyDisablesEverySideEffectControl() {
        screen(ProUiState(true, true, busy = true), onRefresh = { fail("busy") },
            onRedeem = { fail("busy") }, onContact = { fail("busy") })
        compose.onNodeWithTag("pro-busy").performScrollTo().assertIsDisplayed()
        listOf("pro-refresh", "pro-code", "pro-redeem", "pro-contact").forEach { disabled(it) }
    }

    @Test fun unavailableErrorIsVisibleAndRetryStaysEnabled() {
        screen(ProUiState(true, false, ProStatus.UNAVAILABLE))
        compose.onNodeWithTag("pro-status").assertTextEquals(localized("en").getString(R.string.pro_unavailable))
        compose.onNodeWithTag("pro-refresh").performScrollTo().assertIsEnabled()
        compose.onNodeWithTag("pro-expiry").assertDoesNotExist()
    }

    @Test fun activeStatusExplicitlySaysSnapshotAndShowsExpiryAndFreeScope() {
        screen(ProUiState(true, false, ProStatus.ACTIVE_SNAPSHOT, expiresAtMillis = 1_900_000_000_000))
        compose.onNodeWithTag("pro-status").assertTextEquals(localized("en").getString(R.string.pro_active_snapshot))
        compose.onNodeWithTag("pro-expiry").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("pro-online").performScrollTo().assertTextEquals(localized("en").getString(R.string.pro_online_only))
        compose.onNodeWithTag("pro-scope").performScrollTo().assertTextEquals(localized("en").getString(R.string.pro_scope))
    }

    @Test fun arabicLargeTextScrollKeepsRevokedStatusAndActionsReachable() {
        screen(ProUiState(true, false, ProStatus.REVOKED), language = "ar", fontScale = 1.6f)
        compose.onNodeWithTag("pro-status").performScrollTo().assertTextEquals(localized("ar").getString(R.string.pro_revoked))
        compose.onNodeWithTag("pro-refresh").performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        compose.onNodeWithTag("pro-code").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("pro-contact-note").performScrollTo().assertTextEquals(localized("ar").getString(R.string.pro_contact_absent))
    }

    @Test fun secureFlagIsAddedAndRemovedWhenNotPreviouslyPresent() = secureFlag(false)
    @Test fun secureFlagThatPredatesScreenIsPreserved() = secureFlag(true)

    private fun secureFlag(preExisting: Boolean) {
        var visible by mutableStateOf(false)
        lateinit var window: Window
        compose.setContent {
            val activity = generateSequence(LocalView.current.context) { (it as? ContextWrapper)?.baseContext }
                .filterIsInstance<Activity>().first()
            SideEffect { window = activity.window }
            if (visible) ProScreen(false, ProUiState(false, false), {}, {}, {}, {})
        }
        compose.runOnIdle {
            if (preExisting) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            visible = true
        }
        compose.runOnIdle { assertTrue(window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0); visible = false }
        compose.runOnIdle {
            assertEquals(preExisting, window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
            // Restore only this test activity's state.
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
