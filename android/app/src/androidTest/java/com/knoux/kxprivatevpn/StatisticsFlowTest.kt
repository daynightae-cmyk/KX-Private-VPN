package com.knoux.kxprivatevpn

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.data.stats.UsageStatsStore
import com.knoux.kxprivatevpn.launch.LaunchPreferences
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * Real-device path: the statistics destination is reachable from Home, reads the app-private
 * store, claims no traffic on a fresh device, and its Home tab returns to the real home screen.
 */
class StatisticsFlowTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun statisticsScreenLoadsMeasuredStoreAndNavigatesHome() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        runBlocking { UsageStatsStore(context).clear() }
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
                    compose.onAllNodes(hasTestTag("route-home"))
                        .fetchSemanticsNodes().isNotEmpty()
                }
                compose.onNodeWithTag("open-stats").performScrollTo().performClick()
                compose.waitUntil(15_000) {
                    compose.onAllNodes(hasTestTag("stats-screen"))
                        .fetchSemanticsNodes().isNotEmpty()
                }
                compose.waitUntil(15_000) {
                    compose.onAllNodes(hasTestTag("stats-loading"))
                        .fetchSemanticsNodes().isEmpty()
                }
                compose.onNodeWithTag("stats-error").assertDoesNotExist()
                compose.onNodeWithTag("stats-total-value")
                    .assertTextEquals(strings(context).getString(R.string.stats_no_sessions))
                compose.onNodeWithTag("stats-chart-empty").assertIsDisplayed()
                compose.onNodeWithTag("stats-nav").assertIsDisplayed()
                val folder = File(context.getExternalFilesDir(null), "identity-evidence")
                    .apply { mkdirs() }
                File(folder, "41-statistics-device-light.png").outputStream().use { stream ->
                    check(compose.onRoot().captureToImage().asAndroidBitmap()
                        .compress(Bitmap.CompressFormat.PNG, 100, stream))
                }
                compose.onNodeWithTag("stats-nav-home").performClick()
                compose.waitUntil(15_000) {
                    compose.onAllNodes(hasTestTag("route-home"))
                        .fetchSemanticsNodes().isNotEmpty()
                }
            }
        } finally {
            runBlocking { preferences.appearance(original.language, original.theme) }
        }
    }
}
