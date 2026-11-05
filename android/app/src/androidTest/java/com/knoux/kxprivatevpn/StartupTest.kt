package com.knoux.kxprivatevpn

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.launch.LaunchPreferences
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class StartupTest {
    @get:Rule val compose = createEmptyComposeRule()

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
