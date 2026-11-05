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
 * Real Activity flow: Home opens the Repair Lab, the dashboard performs a real StatFs/cache
 * measurement, every available service navigates to its existing screen, and an unavailable
 * service opens an honest explanation instead of a fake scan.
 */
class RepairFlowTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun repairLabMeasuresRoutesAndNeverFakesUnavailableTools() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        runBlocking {
            LaunchPreferences(context).markSeen()
            LaunchPreferences(context).completeOnboarding()
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.waitUntil(10_000) {
                compose.onAllNodes(hasTestTag("route-home")).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("open-repair").performScrollTo().performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodes(hasTestTag("repair-lab")).fetchSemanticsNodes().isNotEmpty()
            }
            compose.waitUntil(20_000) {
                compose.onAllNodes(hasTestTag("repair-volume")).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("repair-services-count").assertTextEquals(
                context.getString(R.string.repair_services_count, 4, 10),
            )
            capture("46-repair-lab-flow")

            compose.onNodeWithTag("repair-card-duplicate").performScrollTo().performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodes(hasText(context.getString(R.string.repair_unavailable_body)))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("repair-card-duplicate").assertDoesNotExist()
            capture("47-repair-unavailable-flow")
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            compose.waitUntil(10_000) {
                compose.onAllNodes(hasTestTag("repair-lab")).fetchSemanticsNodes().isNotEmpty()
            }

            listOf("storage" to "route-storage", "cache" to "route-cache",
                "network" to "route-network").forEach { (card, route) ->
                compose.onNodeWithTag("repair-card-$card").performScrollTo().performClick()
                compose.waitUntil(10_000) {
                    compose.onAllNodes(hasTestTag(route)).fetchSemanticsNodes().isNotEmpty()
                }
                scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
                compose.waitUntil(10_000) {
                    compose.onAllNodes(hasTestTag("repair-lab")).fetchSemanticsNodes().isNotEmpty()
                }
            }

            compose.onNodeWithTag("repair-analyze").performScrollTo().performClick()
            compose.waitUntil(20_000) {
                compose.onAllNodes(hasTestTag("repair-measured-at")).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("repair-measured-at").performScrollTo().assertIsDisplayed()
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
