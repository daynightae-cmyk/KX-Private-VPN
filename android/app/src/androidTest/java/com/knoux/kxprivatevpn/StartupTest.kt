package com.knoux.kxprivatevpn

import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.launch.LaunchPreferences
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class StartupTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun realLauncherKeepsHomeAbsentThroughoutNormalSpin() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // Do not mutate system accessibility settings to manufacture a normal-motion pass.
        assertEquals("normal-motion test requires animator scale 1", 1f,
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f), 0f)
        assertFalse("normal-motion test requires power saver off",
            context.getSystemService(PowerManager::class.java).isPowerSaveMode)
        assertFalse("normal-motion test requires touch exploration off",
            context.getSystemService(AccessibilityManager::class.java).isTouchExplorationEnabled)
        ActivityScenario.launch(MainActivity::class.java).use {
            val orb = compose.onNodeWithTag("entry-orb")
            val home = compose.onNodeWithTag("route-home")
            compose.waitUntil(15_000) { runCatching { orb.assertIsEnabled() }.isSuccess }
            compose.mainClock.autoAdvance = false
            // Genuine MainActivity routing, but deterministic animation time: not a
            // real-time physical capture and not a fixture that merely counts callbacks.
            compose.mainClock.advanceTimeBy(12_000)
            orb.assertIsDisplayed().assertIsEnabled()
            home.assertDoesNotExist()
            orb.performClick()
            val tappedAt = compose.mainClock.currentTime
            orb.assertIsDisplayed()
            home.assertDoesNotExist()
            var repeatedTap = false
            // Touch injection itself advances the test clock. Include it in the elapsed
            // interval rather than counting only explicit advanceTimeByFrame calls.
            while (compose.mainClock.currentTime - tappedAt < 672L) {
                compose.mainClock.advanceTimeByFrame()
                orb.assertIsDisplayed().assertIsNotEnabled()
                home.assertDoesNotExist()
                if (!repeatedTap && compose.mainClock.currentTime - tappedAt >= 200L) {
                    orb.performTouchInput { click() }
                    repeatedTap = true
                    orb.assertIsDisplayed().assertIsNotEnabled()
                    home.assertDoesNotExist()
                }
            }
            assertEquals("sample the full pre-completion interval", 672L,
                compose.mainClock.currentTime - tappedAt)
            compose.mainClock.advanceTimeBy(160)
            home.assertIsDisplayed()
            orb.assertDoesNotExist()
            compose.mainClock.advanceTimeBy(10_000)
            home.assertIsDisplayed()
            orb.assertDoesNotExist()
        }
    }

    @Test fun realLauncherRestoresDashboardAcrossRecreationAndResume() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        runBlocking {
            LaunchPreferences(context).markSeen()
            LaunchPreferences(context).completeOnboarding()
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                enterKxFromSplash(compose)
            compose.waitUntil(5000) {
                compose.onAllNodes(androidx.compose.ui.test.hasTestTag("route-home"))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("route-home").assertIsDisplayed()
            scenario.recreate()
            compose.waitForIdle()
            compose.onNodeWithTag("route-home").assertIsDisplayed()
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            compose.waitForIdle()
            compose.onNodeWithTag("route-home").assertIsDisplayed()
        }
    }
}
