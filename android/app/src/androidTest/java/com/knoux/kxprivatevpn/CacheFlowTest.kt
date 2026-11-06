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
                compose.onNodeWithTag("cache-system-settings").performScrollTo().assertIsDisplayed()
                compose.onNodeWithTag("cache-scan").performScrollTo().performClick()
                compose.waitUntil(10000) { compose.onAllNodes(hasText("$prefix/target.tmp")).fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithTag("cache-remove").performScrollTo().assertIsNotEnabled()
                assertTrue(target.exists())
                compose.onNodeWithText("$prefix/target.tmp").performScrollTo().performClick()
                compose.onNodeWithTag("cache-selected-summary").performScrollTo().assertTextEquals(
                    strings(context).getString(R.string.cache_selected_summary, 1,
                        android.text.format.Formatter.formatFileSize(strings(context), 64)))
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

    /**
     * TASK_16 acceptance in the owner's night Arabic identity: the scope, the official
     * Settings route and the measured selection must all remain present and legible.
     */
    @Test fun nightArabicCacheExplainsOwnershipAndMeasuredSelection() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val ar = strings(context, "ar")
        val prefix = "ci_cache_ar_" + UUID.randomUUID().toString().take(8)
        val folder = File(context.cacheDir, prefix).apply { mkdirs() }
        val target = File(folder, "night.tmp").apply { writeBytes(ByteArray(128)) }
        val preferences = LaunchPreferences(context)
        val original = runBlocking { preferences.settings() }
        // A previous test's completed receipt is persisted app state. Keep the night capture
        // about this run by saving and restoring the real journal bytes around the test.
        val journalFile = File(context.noBackupFilesDir, "cache-last-operation.json")
        val journalBackup = File(context.noBackupFilesDir, "cache-last-operation.json.bak")
        val savedJournal = journalFile.takeIf { it.exists() }?.readBytes()
        val savedBackup = journalBackup.takeIf { it.exists() }?.readBytes()
        journalFile.delete(); journalBackup.delete()
        runBlocking {
            preferences.markSeen(); preferences.completeOnboarding()
            preferences.appearance("ar", "night")
        }
        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                enterKxFromSplash(compose)
                compose.waitUntil(10000) {
                    compose.onAllNodes(hasTestTag("route-home")).fetchSemanticsNodes().isNotEmpty()
                }
                compose.onNodeWithTag("open-cache").performScrollTo().performClick()
                compose.waitUntil(10000) {
                    try { compose.onNodeWithTag("cache-scan").assertIsEnabled(); true }
                    catch (_: AssertionError) { false }
                }
                compose.onNodeWithTag("cache-system-settings").performScrollTo().assertIsDisplayed()
                compose.onNodeWithText(ar.getString(R.string.cache_other_apps_action)).assertExists()
                val output = File(context.getExternalFilesDir(null), "identity-evidence").apply { mkdirs() }
                File(output, "60-cache-night-ar-scope.png").outputStream().use { stream ->
                    compose.onRoot().captureToImage().asAndroidBitmap()
                        .compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                }
                compose.onNodeWithTag("cache-scan").performScrollTo().performClick()
                compose.waitUntil(10000) {
                    compose.onAllNodes(hasText("$prefix/night.tmp")).fetchSemanticsNodes().isNotEmpty()
                }
                compose.onNodeWithText("$prefix/night.tmp").performScrollTo().performClick()
                compose.onNodeWithTag("cache-selected-summary").performScrollTo().assertTextEquals(
                    ar.getString(R.string.cache_selected_summary, 1,
                        android.text.format.Formatter.formatFileSize(ar, 128)))
                File(output, "60b-cache-night-ar-selected.png").outputStream().use { stream ->
                    compose.onRoot().captureToImage().asAndroidBitmap()
                        .compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                }
                compose.onNodeWithTag("cache-remove").performScrollTo().performClick()
                compose.onNodeWithText(ar.getString(R.string.cache_confirm)).assertExists()
                compose.waitForIdle()
                val dialogShot = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
                assertNotNull("dialog screenshot must be captured", dialogShot)
                File(output, "60c-cache-night-ar-confirm.png").outputStream().use { stream ->
                    dialogShot!!.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                }
                compose.onNodeWithText(ar.getString(R.string.cancel)).performClick()
                assertTrue(target.exists())
            }
        } finally {
            runBlocking { preferences.appearance(original.language, original.theme) }
            target.delete(); folder.delete()
            journalFile.delete(); journalBackup.delete()
            savedJournal?.let { journalFile.writeBytes(it) }
            savedBackup?.let { journalBackup.writeBytes(it) }
        }
    }
}
