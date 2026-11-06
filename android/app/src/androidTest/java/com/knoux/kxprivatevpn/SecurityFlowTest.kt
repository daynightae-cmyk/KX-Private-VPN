package com.knoux.kxprivatevpn

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.launch.LaunchPreferences
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

/**
 * Real-device path: opening the Security Center reads VpnService, Settings.Secure,
 * ConnectivityManager and the WireGuard backend without a tunnel, and stays honest.
 */
class SecurityFlowTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun securityCenterReadsPlatformAndNeverClaimsProtectionWithoutATunnel() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = LaunchPreferences(context)
        val original = runBlocking { preferences.settings() }
        runBlocking {
            preferences.markSeen()
            preferences.completeOnboarding()
            preferences.appearance("en", "day")
        }
        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                enterKxFromSplash(compose)
                compose.waitUntil(15_000) {
                    compose.onAllNodes(hasTestTag("route-home")).fetchSemanticsNodes().isNotEmpty()
                }
                compose.onNodeWithTag("open-security").performScrollTo().performClick()
                compose.waitUntil(15_000) {
                    compose.onAllNodes(hasTestTag("security-screen")).fetchSemanticsNodes().isNotEmpty()
                }
                compose.waitUntil(15_000) {
                    compose.onAllNodes(hasText(strings(context).getString(R.string.security_hero_inactive)))
                        .fetchSemanticsNodes().isNotEmpty()
                }
                compose.onNodeWithTag("security-posture")
                    .assertTextEquals(strings(context).getString(R.string.security_hero_inactive))
                compose.onNodeWithTag("security-row-tracker-status")
                    .performScrollTo()
                    .assertTextEquals(strings(context).getString(R.string.security_state_unavailable))
                compose.onNodeWithTag("security-hero").performScrollTo()
                captureEvidence(context, compose, "24-security-center-device-light")
            }
        } finally {
            runBlocking { preferences.appearance(original.language, original.theme) }
        }
    }
}
