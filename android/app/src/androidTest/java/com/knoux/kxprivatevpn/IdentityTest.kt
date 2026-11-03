package com.knoux.kxprivatevpn

import android.graphics.Bitmap
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
}
