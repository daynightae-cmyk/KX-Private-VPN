package com.knoux.kxprivatevpn

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
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
                compose.waitUntil(10000) { compose.onAllNodes(hasTestTag("route-home")).fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithTag("open-settings").performScrollTo().performClick()
                compose.onNodeWithText("العربية").performScrollTo().performClick()
                compose.waitUntil(10000) { compose.onAllNodes(hasText(ar.getString(R.string.language))).fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithText(ar.getString(R.string.night)).performScrollTo().performClick()
                compose.onNodeWithText(ar.getString(R.string.night)).assertIsSelected()
                scenario.recreate()
                compose.waitForIdle()
                compose.onNodeWithText(ar.getString(R.string.night)).assertIsSelected()
                val folder = File(context.getExternalFilesDir(null), "identity-evidence").apply { mkdirs() }
                File(folder, "11-settings-ar-night.png").outputStream().use {
                    compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
                }
                compose.onNodeWithText("English").performScrollTo().performClick()
                compose.waitUntil(10000) { compose.onAllNodes(hasText(context.getString(R.string.language))).fetchSemanticsNodes().isNotEmpty() }
            }
            val restored = runBlocking { preferences.settings() }
            org.junit.Assert.assertEquals("en", restored.language)
            org.junit.Assert.assertEquals("night", restored.theme)
        } finally { runBlocking { preferences.appearance(original.language, original.theme) } }
    }
    @Test fun manualImportPersistsAndNeverClaimsConnection() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = ProfileStore(context)
        val original = runBlocking { store.load() }
        val name = "CI-" + UUID.randomUUID().toString().take(8)
        val key = Base64.getEncoder().encodeToString(ByteArray(32) { (it + 1).toByte() })
        val config = "[Interface]\nPrivateKey = $key\nAddress = 10.7.0.2/32\n" +
            "[Peer]\nPublicKey = $key\nEndpoint = 192.0.2.1:51820\nAllowedIPs = 0.0.0.0/0"
        runBlocking { LaunchPreferences(context).markSeen(); LaunchPreferences(context).completeOnboarding() }
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                compose.waitUntil(10000) { compose.onAllNodes(hasTestTag("route-home")).fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithTag("open-import").performScrollTo().performClick()
                compose.onNodeWithTag("profile-name").performTextInput(name)
                compose.onNodeWithTag("manual").performScrollTo().performClick()
                compose.onNodeWithTag("config-text").performScrollTo().performTextInput(config)
                compose.onNodeWithTag("save-profile").performScrollTo().performClick()
                compose.waitUntil(10000) { compose.onAllNodes(hasTestTag("route-home")).fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithText(name).assertIsDisplayed()
                compose.onNodeWithText(context.getString(R.string.disconnected)).assertIsDisplayed()
                val folder = File(context.getExternalFilesDir(null), "identity-evidence").apply { mkdirs() }
                File(folder, "08-native-home-imported.png").outputStream().use {
                    compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
                }
                scenario.recreate()
                compose.waitUntil(10000) { compose.onAllNodes(hasText(name)).fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithText(name).assertIsDisplayed()
                compose.onNodeWithTag("open-servers").performScrollTo().performClick()
                compose.onNodeWithText(name).assertIsDisplayed()
            }
        } finally {
            runBlocking {
                store.load().profiles.filter { it.name == name }.forEach { store.remove(it.id) }
                original.selectedId?.let { store.select(it) }
            }
        }
    }
}
