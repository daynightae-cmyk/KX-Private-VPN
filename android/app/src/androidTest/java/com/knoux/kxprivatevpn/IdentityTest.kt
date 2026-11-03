package com.knoux.kxprivatevpn

import android.graphics.Bitmap
import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.launch.IdentityExperience
import com.knoux.kxprivatevpn.launch.LaunchPreferences
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.Locale

class IdentityTest {
    @get:Rule val compose = createComposeRule()
    private fun capture(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val folder = File(context.getExternalFilesDir(null), "identity-evidence").apply { mkdirs() }
        File(folder, "$name.png").outputStream().buffered().use { stream ->
            check(compose.onRoot().captureToImage().asAndroidBitmap()
                .compress(Bitmap.CompressFormat.PNG, 100, stream))
        }
    }
    private fun captureWindow(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val folder = File(instrumentation.targetContext.getExternalFilesDir(null), "identity-evidence")
            .apply { mkdirs() }
        File(folder, "$name.png").outputStream().use { stream ->
            // Capture the real dialog root. The API35 renderer lost its display during the
            // former full-display capture; retain the same locale/visibility assertions.
            check(compose.onNode(isDialog()).captureToImage().asAndroidBitmap()
                .compress(Bitmap.CompressFormat.PNG, 100, stream))
        }
    }
    @Test fun choreographyFinishesAndStops() {
        compose.mainClock.autoAdvance = false
        var completions = 0
        compose.setContent { IdentityExperience(onFinished = { completions++ }) }
        compose.mainClock.advanceTimeBy(400)
        capture("01-photons")
        compose.mainClock.advanceTimeBy(1000)
        capture("02-ribbons")
        compose.mainClock.advanceTimeBy(1300)
        capture("03-reveal")
        compose.mainClock.advanceTimeBy(1200)
        compose.onNodeWithTag("continue").assertIsDisplayed()
        compose.onNodeWithTag("skip").assertDoesNotExist()
        capture("04-welcome")
        compose.runOnIdle { assertEquals(1, completions) }
        compose.mainClock.advanceTimeBy(10000)
        compose.runOnIdle { assertEquals(1, completions) }
    }
    @Test fun skipIsImmediateAndContinueDisclosesActualScope() {
        compose.mainClock.autoAdvance = false
        compose.setContent { IdentityExperience() }
        compose.onNodeWithTag("skip").performClick()
        compose.mainClock.advanceTimeBy(32)
        compose.onNodeWithTag("continue").assertIsDisplayed().performClick()
        // Resume Material dialog's entrance transition after the deterministic intro assertions.
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.onNodeWithText(context.getString(R.string.scope_notice)).assertIsDisplayed()
    }
    @Test fun reducedMotionHasNoIntroAndDayIsRenderable() {
        compose.setContent { IdentityExperience(reducedMotion = true, night = false) }
        compose.onNodeWithTag("skip").assertDoesNotExist()
        compose.onNodeWithTag("continue").assertIsDisplayed()
        capture("05-day-reduced")
    }
    @Test fun seenPreferenceSurvivesRepositoryRecreation() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        LaunchPreferences(context).markSeen()
        assertTrue(LaunchPreferences(context).hasSeenIntro())
    }

    @Test fun arabicRtlLargeText360dp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val config = Configuration(context.resources.configuration).apply {
            setLocale(Locale.forLanguageTag("ar"))
            setLayoutDirection(Locale.forLanguageTag("ar"))
        }
        val arabicContext = context.createConfigurationContext(config)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalContext provides arabicContext,
                LocalConfiguration provides config,
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDensity provides Density(density.density, 1.5f),
            ) {
                Box(Modifier.fillMaxSize().requiredWidth(360.dp)) {
                    IdentityExperience(reducedMotion = true)
                }
            }
        }
        compose.onNodeWithText(arabicContext.getString(R.string.continue_label))
            .performScrollTo().assertIsDisplayed().performClick()
        compose.waitForIdle()
        captureWindow("06a-ar-dialog")
        compose.waitUntil(5000) {
            try {
                compose.onNodeWithText(arabicContext.getString(R.string.scope_notice)).assertIsDisplayed()
                true
            } catch (_: AssertionError) { false }
        }
        compose.onNodeWithText(arabicContext.getString(R.string.close)).performClick()
        capture("06-ar-rtl-360-large-text")
    }

    @Test fun english411dpDay() {
        compose.setContent {
            Box(Modifier.fillMaxSize().requiredWidth(411.dp)) {
                IdentityExperience(reducedMotion = true, night = false)
            }
        }
        compose.onNodeWithTag("continue").assertIsDisplayed()
        capture("07-en-411-day")
    }
}
