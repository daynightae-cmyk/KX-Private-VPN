package com.knoux.kxprivatevpn

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.data.config.ProfileStore
import com.knoux.kxprivatevpn.launch.LaunchPreferences
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.Base64
import java.util.UUID
import android.content.res.Configuration
import java.util.Locale

class ClientFlowTest {
    @get:Rule val compose = createEmptyComposeRule()
    @Test fun languageAndThemePersistAcrossActivityRecreation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = LaunchPreferences(context)
        val original = runBlocking { preferences.settings() }
        runBlocking {
            preferences.markSeen(); preferences.completeOnboarding(); preferences.appearance("en", "day")
        }
        val ar = context.createConfigurationContext(Configuration(context.resources.configuration).apply {
            setLocale(Locale.forLanguageTag("ar"))
        })
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                enterKxFromSplash(compose)
                compose.waitUntil(10000) { compose.onAllNodes(hasTestTag("route-home")).fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithTag("open-settings").performScrollTo().performClick()
                compose.onNodeWithText("العربية").performScrollTo().performClick()
                compose.waitUntil(10000) { compose.onAllNodes(hasText(ar.getString(R.string.language))).fetchSemanticsNodes().isNotEmpty() }
                // The language write flips the header before the previous operation clears
                // busy, so the appearance radios are briefly disabled while the new locale is
                // already visible. Clicking in that window is swallowed (CI run 42205808894);
                // wait until the night row is both enabled and still unselected, then wait for
                // the persisted selection instead of assuming it is synchronous.
                awaitRadio(ar.getString(R.string.night), selected = false)
                compose.onNodeWithText(ar.getString(R.string.night)).performScrollTo().performClick()
                awaitRadio(ar.getString(R.string.night), selected = true)
                compose.onNodeWithText(ar.getString(R.string.night)).assertIsSelected()
                scenario.recreate()
                awaitRadio(ar.getString(R.string.night), selected = true)
                compose.onNodeWithText(ar.getString(R.string.night)).assertIsSelected()
                val folder = File(context.getExternalFilesDir(null), "identity-evidence").apply { mkdirs() }
                File(folder, "11-settings-ar-night.png").outputStream().use {
                    compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
                }
                compose.onNodeWithText("English").performScrollTo().performClick()
                compose.waitUntil(10000) { compose.onAllNodes(hasText(strings(context).getString(R.string.language))).fetchSemanticsNodes().isNotEmpty() }
            }
            val restored = runBlocking { preferences.settings() }
            org.junit.Assert.assertEquals("en", restored.language)
            org.junit.Assert.assertEquals("night", restored.theme)
        } finally { runBlocking { preferences.appearance(original.language, original.theme) } }
    }
    /**
     * Regression for CI run 42205808894: a second appearance tap must not be dispatched while
     * the locale write is still holding the rows disabled, and the selection must already be
     * persisted when the row reports selected.
     */
    @Test fun appearanceTapIsNotLostWhileTheLocaleWriteSettles() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = LaunchPreferences(context)
        val original = runBlocking { preferences.settings() }
        val ar = context.createConfigurationContext(Configuration(context.resources.configuration).apply {
            setLocale(Locale.forLanguageTag("ar"))
        })
        runBlocking {
            preferences.markSeen(); preferences.completeOnboarding(); preferences.appearance("en", "day")
        }
        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                enterKxFromSplash(compose)
                compose.waitUntil(10000) { compose.onAllNodes(hasTestTag("route-home")).fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithTag("open-settings").performScrollTo().performClick()
                compose.onNodeWithText("\u0627\u0644\u0639\u0631\u0628\u064A\u0629").performScrollTo().performClick()
                compose.waitUntil(10000) { compose.onAllNodes(hasText(ar.getString(R.string.language))).fetchSemanticsNodes().isNotEmpty() }
                awaitRadio(ar.getString(R.string.night), selected = false)
                compose.onNodeWithText(ar.getString(R.string.night)).performScrollTo().performClick()
                awaitRadio(ar.getString(R.string.night), selected = true)
                compose.onNodeWithText(ar.getString(R.string.night)).assertIsSelected()
                val persisted = runBlocking { preferences.settings() }
                org.junit.Assert.assertEquals("ar", persisted.language)
                org.junit.Assert.assertEquals("night", persisted.theme)
            }
        } finally { runBlocking { preferences.appearance(original.language, original.theme) } }
    }

    /**
     * Waits for a selectable row to reflect [selected] while it is enabled. This pins the real
     * asynchronous preference write and the busy gate instead of racing them.
     */
    private fun awaitRadio(text: String, selected: Boolean) {
        compose.waitUntil(10000) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().any { node ->
                runCatching { node.config[SemanticsProperties.Selected] }.getOrNull() == selected &&
                    runCatching { node.config[SemanticsProperties.Disabled] }.isFailure
            }
        }
    }

    @Test fun manualImportPersistsAndNeverClaimsConnection() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = ProfileStore(context)
        val original = runBlocking { store.load() }
        val name = "CI-" + UUID.randomUUID().toString().take(8)
        val key = Base64.getEncoder().encodeToString(ByteArray(32) { (it + 1).toByte() })
        val config = "[Interface]\nPrivateKey = $key\nAddress = 10.7.0.2/32\n" +
            "[Peer]\nPublicKey = $key\nEndpoint = 192.0.2.1:51820\nAllowedIPs = 0.0.0.0/0"
        val preferences = LaunchPreferences(context)
        val appearanceBefore = runBlocking { preferences.settings() }
        runBlocking {
            preferences.markSeen(); preferences.completeOnboarding()
            // Pin the appearance so the Home status asserted below is not inherited from
            // whichever language another test happened to leave persisted.
            preferences.appearance("en", "day")
        }
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                enterKxFromSplash(compose)
                compose.waitUntil(10000) { compose.onAllNodes(hasTestTag("route-home")).fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithTag("open-import").performScrollTo().performClick()
                compose.onNodeWithTag("profile-name").performTextInput(name)
                // CI disables the soft keyboard for this run (see scripts/verify-device.sh):
                // its IME covered the manual tab and swallowed the click in run 42143462462.
                compose.onNodeWithTag("manual").performScrollTo().performClick()
                compose.onNodeWithTag("config-text").performScrollTo().performTextInput(config)
                compose.onNodeWithTag("save-profile").performScrollTo().performClick()
                compose.waitUntil(10000) { compose.onAllNodes(hasTestTag("route-home")).fetchSemanticsNodes().isNotEmpty() }
                // Home is a scrollable page and resets to the top on navigation by design.
                compose.onNodeWithText(name).performScrollTo().assertIsDisplayed()
                compose.onNodeWithText(strings(context).getString(R.string.disconnected)).performScrollTo().assertIsDisplayed()
                val folder = File(context.getExternalFilesDir(null), "identity-evidence").apply { mkdirs() }
                File(folder, "08-native-home-imported.png").outputStream().use {
                    compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
                }
                scenario.recreate()
                compose.waitUntil(10000) { compose.onAllNodes(hasText(name)).fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithText(name).performScrollTo().assertIsDisplayed()
                compose.onNodeWithTag("open-servers").performScrollTo().performClick()
                compose.onNodeWithText(name).performScrollTo().assertIsDisplayed()
            }
        } finally {
            runBlocking {
                preferences.appearance(appearanceBefore.language, appearanceBefore.theme)
                store.load().profiles.filter { it.name == name }.forEach { store.remove(it.id) }
                original.selectedId?.let { store.select(it) }
            }
        }
    }
}
