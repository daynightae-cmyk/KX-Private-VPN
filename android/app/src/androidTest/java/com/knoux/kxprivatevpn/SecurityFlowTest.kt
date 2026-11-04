package com.knoux.kxprivatevpn

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.launch.LaunchPreferences
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.io.File

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
                compose.waitUntil(15_000) {
                    compose.onAllNodes(hasTestTag("route-home")).fetchSemanticsNodes().isNotEmpty()
                }
                compose.onNodeWithTag("open-security").performScrollTo().performClick()
                compose.waitUntil(15_000) {
                    compose.onAllNodes(hasTestTag("security-screen")).fetchSemanticsNodes().isNotEmpty()
                }
                compose.waitUntil(15_000) {
                    compose.onAllNodes(hasText(context.getString(R.string.security_hero_inactive)))
                        .fetchSemanticsNodes().isNotEmpty()
                }
                compose.onNodeWithTag("security-posture")
                    .assertTextEquals(context.getString(R.string.security_hero_inactive))
                compose.onNodeWithTag("security-row-tracker-status")
                    .performScrollTo()
                    .assertTextEquals(context.getString(R.string.security_state_unavailable))
                compose.onNodeWithTag("security-hero").performScrollTo()
                val folder = File(context.getExternalFilesDir(null), "identity-evidence").apply { mkdirs() }
                File(folder, "24-security-center-device-light.png").outputStream().use { stream ->
                    check(compose.onRoot().captureToImage().asAndroidBitmap()
                        .compress(Bitmap.CompressFormat.PNG, 100, stream))
                }
            }
        } finally {
            runBlocking { preferences.appearance(original.language, original.theme) }
        }
    }
}
