package com.knoux.kxprivatevpn

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.launch.LaunchPreferences
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Ghost Hunter is reachable from the Repair Lab and never offers a scan without a real tree. */
class GhostHunterFlowTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun ghostHunterOpensFromRepairLabAndWaitsForAuthorization() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        runBlocking {
            LaunchPreferences(context).markSeen()
            LaunchPreferences(context).completeOnboarding()
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                enterKxFromSplash(compose)
            compose.waitUntil(10_000) {
                compose.onAllNodes(hasTestTag("route-home")).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("open-repair").performScrollTo().performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodes(hasTestTag("repair-lab")).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("repair-card-ghost").performScrollTo().performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodes(hasTestTag("route-ghost")).fetchSemanticsNodes().isNotEmpty()
            }
            // Ghost Hunter never offers a scan without a real tree. A device that already
            // granted a tree (the owner's phone keeps the SAF grant between sessions, and the
            // app data must not be cleared to make a test pass) shows the authorized panel; a
            // fresh device shows the authorization request. Both states are honest, so the
            // test pins the invariant instead of one device's history.
            val treeAuthorized = compose.onAllNodesWithTag("ghost-tree-name")
                .fetchSemanticsNodes().isNotEmpty()
            val scanOffered = compose.onAllNodesWithTag("ghost-scan")
                .fetchSemanticsNodes().isNotEmpty()
            assertEquals(
                "Ghost Hunter must offer a scan only when a real tree is authorized",
                treeAuthorized, scanOffered,
            )
            if (treeAuthorized) {
                compose.onNodeWithTag("ghost-tree-name").performScrollTo().assertIsDisplayed()
                compose.onNodeWithTag("ghost-scan").performScrollTo().assertIsDisplayed()
            } else {
                compose.onNodeWithTag("ghost-choose-folder").performScrollTo().assertIsDisplayed()
                compose.onNodeWithTag("ghost-scan").assertDoesNotExist()
            }
            compose.onNodeWithTag("ghost-privacy").performScrollTo().assertIsDisplayed()
            capture("56-ghost-flow")
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            compose.waitUntil(10_000) {
                compose.onAllNodes(hasTestTag("repair-lab")).fetchSemanticsNodes().isNotEmpty()
            }
        }
    }

    private fun capture(name: String) {
        captureEvidence(InstrumentationRegistry.getInstrumentation().targetContext, compose, name)
    }
}
