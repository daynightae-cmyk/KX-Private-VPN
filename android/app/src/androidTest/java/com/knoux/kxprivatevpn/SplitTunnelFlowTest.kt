package com.knoux.kxprivatevpn

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
 * Real-device path: the screen loads Android's launchable package list, does not claim any
 * app is routed without a tunnel, and captures the actual runtime.
 */
class SplitTunnelFlowTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun splitTunnelReadsPackageManagerAndStartsDisconnected() {
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
                compose.onNodeWithTag("open-split").performScrollTo().performClick()
                compose.waitUntil(15_000) {
                    compose.onAllNodes(hasTestTag("split-screen")).fetchSemanticsNodes().isNotEmpty()
                }
                compose.waitUntil(15_000) {
                    compose.onAllNodes(hasTestTag("split-loading")).fetchSemanticsNodes().isEmpty()
                }
                compose.onNodeWithTag("split-load-failed").assertDoesNotExist()
                val summary = compose.onNodeWithTag("split-summary-apps").fetchSemanticsNode()
                val countText = runCatching {
                    summary.config[SemanticsProperties.Text].joinToString(" ") { it.text }
                }.getOrDefault("")
                assertTrue("Android must report a real app count, got: $countText", countText.isNotEmpty())
                assertFalse("A disconnected screen cannot claim applied routing: $countText",
                    countText.startsWith("0 "))
                compose.onNodeWithTag("split-note-apply").assertIsDisplayed()
                val folder = File(context.getExternalFilesDir(null), "identity-evidence").apply { mkdirs() }
                File(folder, "28-split-tunnel-device-light.png").outputStream().use { stream ->
                    check(compose.onRoot().captureToImage().asAndroidBitmap()
                        .compress(Bitmap.CompressFormat.PNG, 100, stream))
                }
            }
        } finally {
            runBlocking { preferences.appearance(original.language, original.theme) }
        }
    }
}
