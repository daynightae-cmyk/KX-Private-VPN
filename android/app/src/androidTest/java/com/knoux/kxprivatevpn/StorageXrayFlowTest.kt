package com.knoux.kxprivatevpn

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.launch.LaunchPreferences
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Real Activity flow: Home opens the measured Storage X-Ray without inventing categories. */
class StorageXrayFlowTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun storageXrayOpensAndShowsOnlyMeasuredState() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        runBlocking {
            LaunchPreferences(context).markSeen()
            LaunchPreferences(context).completeOnboarding()
        }
        ActivityScenario.launch(MainActivity::class.java).use {
                enterKxFromSplash(compose)
            compose.waitUntil(10_000) {
                compose.onAllNodes(hasTestTag("route-home")).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("open-storage").performScrollTo().performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodes(hasTestTag("route-storage")).fetchSemanticsNodes().isNotEmpty()
            }
            compose.waitUntil(20_000) {
                compose.onAllNodes(hasTestTag("storage-measured-at")).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("storage-kx").assertExists()
            compose.waitUntil(20_000) {
                compose.onAllNodes(hasTestTag("storage-category-images")).fetchSemanticsNodes()
                    .isNotEmpty() ||
                    compose.onAllNodes(hasTestTag("storage-media-permission")).fetchSemanticsNodes()
                        .isNotEmpty()
            }
            compose.onNodeWithTag("storage-read-only").performScrollTo().assertIsDisplayed()
            val categories = compose.onAllNodes(hasTestTag("storage-category-images"))
                .fetchSemanticsNodes().isNotEmpty()
            val permission = compose.onAllNodes(hasTestTag("storage-media-permission"))
                .fetchSemanticsNodes().isNotEmpty()
            assertTrue("either real media rows or the permission gate must be shown",
                categories || permission)
            compose.onNodeWithTag("storage-refresh").performScrollTo().assertIsDisplayed()
            capture("52-storage-xray-flow")
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
