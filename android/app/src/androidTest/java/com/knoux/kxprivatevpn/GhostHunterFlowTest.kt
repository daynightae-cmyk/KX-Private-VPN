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
            compose.onNodeWithTag("ghost-choose-folder").performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("ghost-scan").assertDoesNotExist()
            compose.onNodeWithTag("ghost-privacy").performScrollTo().assertIsDisplayed()
            capture("56-ghost-flow")
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            compose.waitUntil(10_000) {
                compose.onAllNodes(hasTestTag("repair-lab")).fetchSemanticsNodes().isNotEmpty()
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
}
