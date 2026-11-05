package com.knoux.kxprivatevpn

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.launch.LaunchPreferences
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * Real-device path: the screen loads the stored settings (plus any selected profile
 * baseline), never shows an applied banner without a tunnel, and captures the runtime.
 */
class AdvancedNetworkFlowTest {
    @get:Rule val compose = createEmptyComposeRule()

    /** This ui-test version has no assertIsDisabled; check the semantics directly. */
    private fun SemanticsNodeInteraction.assertDisabled(): SemanticsNodeInteraction {
        val config = fetchSemanticsNode().config
        assertTrue("Switch must expose disabled() semantics",
            runCatching { config[SemanticsProperties.Disabled] }.isSuccess)
        return this
    }

    @Test fun advancedScreenLoadsStoredSettingsWithoutClaimingATunnel() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences("advanced_network", Context.MODE_PRIVATE)
            .edit().clear().commit()
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
                compose.onNodeWithTag("open-advanced").performScrollTo().performClick()
                compose.waitUntil(15_000) {
                    compose.onAllNodes(hasTestTag("adv-screen")).fetchSemanticsNodes().isNotEmpty()
                }
                compose.waitUntil(15_000) {
                    compose.onAllNodes(hasTestTag("adv-loading")).fetchSemanticsNodes().isEmpty()
                }
                compose.onNodeWithTag("adv-load-failed").assertDoesNotExist()
                // No tunnel exists on a fresh device: nothing may claim it is applied.
                compose.onNodeWithTag("adv-applied").assertDoesNotExist()
                compose.onNodeWithTag("adv-reconnect").assertDoesNotExist()
                // Top of the page first: this capture is the reference-comparison shot.
                compose.onNodeWithTag("adv-hero").assertIsDisplayed()
                compose.onNodeWithTag("adv-protocol-wireguard").performScrollTo().assertIsSelected()
                compose.onNodeWithTag("adv-protocol-openvpn").assertDisabled()
                compose.onNodeWithTag("adv-protocol-automatic").assertDisabled()
                val folder = File(context.getExternalFilesDir(null), "identity-evidence").apply { mkdirs() }
                File(folder, "35-advanced-device-light.png").outputStream().use { stream ->
                    check(compose.onRoot().captureToImage().asAndroidBitmap()
                        .compress(Bitmap.CompressFormat.PNG, 100, stream))
                }
                // The honest apply-on-next-start note sits at the very bottom: reach it
                // explicitly instead of asserting against clipped content.
                compose.onNodeWithTag("adv-note-apply").performScrollTo().assertIsDisplayed()
                compose.onNodeWithTag("adv-obfuscation-switch").performScrollTo().assertDisabled()
                compose.onNodeWithTag("adv-porthopping-switch").performScrollTo().assertDisabled()
            }
        } finally {
            runBlocking { preferences.appearance(original.language, original.theme) }
        }
    }
}
