package com.knoux.kxprivatevpn

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.launch.LaunchPreferences
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.UUID

class CacheFlowTest {
    @get:Rule val compose = createEmptyComposeRule()
    @Test fun approvedCacheRemovalPreservesUnselectedAndPrivateData() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefix = "ci_cache_" + UUID.randomUUID().toString().take(8)
        val folder = File(context.cacheDir, prefix).apply { mkdirs() }
        val target = File(folder, "target.tmp").apply { writeBytes(ByteArray(64)) }
        val sentinel = File(folder, "sentinel.tmp").apply { writeText("keep") }
        val privateSentinel = File(context.noBackupFilesDir, "$prefix.keep").apply { writeText("outside cache") }
        runBlocking { LaunchPreferences(context).markSeen(); LaunchPreferences(context).completeOnboarding() }
        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                compose.waitUntil(10000) { compose.onAllNodes(hasTestTag("route-home")).fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithTag("open-cache").performScrollTo().performClick()
                compose.waitUntil(10000) {
                    try { compose.onNodeWithTag("cache-scan").assertIsEnabled(); true } catch (_: AssertionError) { false }
                }
                compose.onNodeWithTag("cache-scan").performScrollTo().performClick()
                compose.waitUntil(10000) { compose.onAllNodes(hasText("$prefix/target.tmp")).fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithTag("cache-remove").performScrollTo().assertIsNotEnabled()
                assertTrue(target.exists())
                compose.onNodeWithText("$prefix/target.tmp").performScrollTo().performClick()
                compose.onNodeWithTag("cache-remove").performScrollTo().performClick()
                assertTrue(target.exists()) // Preview and dialog do not delete anything.
                compose.onNodeWithTag("cache-confirm").performClick()
                compose.waitUntil(10000) { !target.exists() }
                val receipt = context.getString(R.string.cache_receipt, 1,
                    android.text.format.Formatter.formatFileSize(context, 64), 0, 0)
                compose.waitUntil(10000) { compose.onAllNodes(hasText(receipt)).fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithText(receipt).performScrollTo().assertIsDisplayed()
                assertEquals("keep", sentinel.readText())
                assertEquals("outside cache", privateSentinel.readText())
                val output = File(context.getExternalFilesDir(null), "identity-evidence").apply { mkdirs() }
                File(output, "09-cache-verified-receipt.png").outputStream().use { stream ->
                    compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                }
            }
        } finally {
            // Only this test's owned fixtures; never a recursive cache clear.
            target.delete(); sentinel.delete(); folder.delete(); privateSentinel.delete()
        }
    }
}
