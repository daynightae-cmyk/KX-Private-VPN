package com.knoux.kxprivatevpn

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.launch.LaunchPreferences
import com.knoux.kxprivatevpn.platform.repair.RepairJournal
import com.knoux.kxprivatevpn.platform.repair.RepairServiceId
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
        val started = System.currentTimeMillis()
        val preferences = LaunchPreferences(context)
        val original = runBlocking { preferences.settings() }
        runBlocking {
            preferences.markSeen(); preferences.completeOnboarding()
            // The receipt string is asserted below, so the appearance has to be pinned
            // rather than inherited from whichever language a previous test left behind.
            preferences.appearance("en", "day")
        }
        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                enterKxFromSplash(compose)
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
                val receipt = strings(context).getString(R.string.cache_receipt, 1,
                    android.text.format.Formatter.formatFileSize(strings(context), 64), 0, 0)
                compose.waitUntil(10000) { compose.onAllNodes(hasText(receipt)).fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithText(receipt).performScrollTo().assertIsDisplayed()
                assertEquals("keep", sentinel.readText())
                assertEquals("outside cache", privateSentinel.readText())
                // The dashboard journal is real evidence: this operation is recorded with its
                // measured bytes, and no file path is ever written into the journal.
                val journalFile = File(context.noBackupFilesDir, "repair-journal.json")
                val journal = RepairJournal(journalFile)
                val deadline = System.currentTimeMillis() + 10_000
                var recorded = false
                while (!recorded && System.currentTimeMillis() < deadline) {
                    recorded = journal.load().any {
                        it.serviceKey == RepairServiceId.CACHE.key && it.atMillis >= started &&
                            it.items >= 1 && it.bytes >= 64
                    }
                    if (!recorded) Thread.sleep(100)
                }
                assertTrue(journalFile.exists())
                assertTrue("completed cleanup must be journaled", recorded)
                assertFalse(journalFile.readText().contains(prefix))
                val output = File(context.getExternalFilesDir(null), "identity-evidence").apply { mkdirs() }
                File(output, "09-cache-verified-receipt.png").outputStream().use { stream ->
                    compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                }
            }
        } finally {
            runBlocking { preferences.appearance(original.language, original.theme) }
            // Only this test's owned fixtures; never a recursive cache clear.
            target.delete(); sentinel.delete(); folder.delete(); privateSentinel.delete()
        }
    }
}
