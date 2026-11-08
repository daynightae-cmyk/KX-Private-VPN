package com.knoux.kxprivatevpn

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.features.SessionNotificationAccess
import com.knoux.kxprivatevpn.features.SettingsScreen
import com.knoux.kxprivatevpn.launch.UiPreferences
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.Locale

/**
 * Settings is a live report, not a brochure: the build string, the saved profile count, the
 * persisted appearance choice, and the exact notification access state must all be real.
 * A reset needs explicit approval, and no state may promise a notification the app cannot post.
 */
class SettingsScreenTest {
    @get:Rule val compose = createComposeRule()

    private val base: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val context: Context
        get() = localized(base, "en")

    private fun localized(base: Context, language: String): Context {
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(language))
        return base.createConfigurationContext(configuration)
    }

    private fun screen(
        access: SessionNotificationAccess = SessionNotificationAccess.ALLOWED,
        language: String = "en",
        night: Boolean = false,
        direction: LayoutDirection = LayoutDirection.Ltr,
        widthDp: Int = 411,
        fontScale: Float = 1f,
        settings: UiPreferences = UiPreferences(language = "en", theme = "day"),
        profileCount: Int = 2,
        busy: Boolean = false,
        onLanguage: (String) -> Unit = {},
        onTheme: (String) -> Unit = {},
        onOpenPro: () -> Unit = {},
        onRequestNotifications: () -> Unit = {},
        onOpenNotificationSettings: () -> Unit = {},
        onReset: () -> Unit = {},
    ) {
        compose.setContent {
            val localizedContext = localized(base, language)
            val nativeDensity = LocalDensity.current
            BoxWithConstraints(Modifier.fillMaxSize()) {
                // Scaled layout fixture only: never change the emulator or owner's settings.
                val density = maxWidth.value * nativeDensity.density / widthDp
                CompositionLocalProvider(
                    LocalContext provides localizedContext,
                    LocalConfiguration provides localizedContext.resources.configuration,
                    LocalLayoutDirection provides direction,
                    LocalDensity provides Density(density, fontScale),
                ) {
                    SettingsScreen(
                        night = night,
                        settings = settings,
                        profileCount = profileCount,
                        versionName = "0.1.0",
                        versionCode = 1L,
                        notificationAccess = access,
                        busy = busy,
                        onBack = {},
                        onLanguage = onLanguage,
                        onTheme = onTheme,
                        onOpenPro = onOpenPro,
                        onRequestNotifications = onRequestNotifications,
                        onOpenNotificationSettings = onOpenNotificationSettings,
                        onReset = onReset,
                    )
                }
            }
        }
    }

    private fun capture(name: String) = captureEvidence(context, compose, name)

    /** A dialog is its own window; UiAutomation captures the whole screen including dialogs. */
    private fun captureDialog(name: String) {
        val folder = File(context.getExternalFilesDir(null), "identity-evidence").apply { mkdirs() }
        compose.waitForIdle()
        Thread.sleep(500)
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(folder, "$name.png").outputStream().use { stream ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
        }
    }

    @Test fun theScreenReportsTheInstalledBuildProfilesAndRealAccessState() {
        screen(access = SessionNotificationAccess.ALLOWED)
        compose.onNodeWithTag("settings-version").assertTextEquals("Version 0.1.0 (build 1)")
        compose.onNodeWithTag("settings-profile-count").assertTextEquals("2 saved profiles")
        compose.onNodeWithTag("pref-account").assertTextContains("No sign-in", substring = true)
        compose.onNodeWithTag("pref-notifications").performScrollTo()
            .assertTextContains("one ongoing notification", substring = true)
        compose.onNodeWithText("On").assertExists()
        compose.onNodeWithTag("pref-privacy").performScrollTo()
            .assertTextContains("Android Keystore", substring = true)
    }

    @Test fun theLegacyFalseNotificationClaimIsGone() {
        screen()
        compose.onNodeWithText("unavailable in this build", substring = true).assertDoesNotExist()
        compose.onNodeWithText("never posts alerts", substring = true).assertDoesNotExist()
    }

    @Test fun deniedPermissionOffersTheRuntimeRequestAndTheAndroidSettingsFallback() {
        var requests = 0
        var opened = 0
        screen(
            access = SessionNotificationAccess.DENIED,
            onRequestNotifications = { requests++ },
            onOpenNotificationSettings = { opened++ },
        )
        compose.onNodeWithTag("pref-notifications").performScrollTo()
            .assertTextContains("works without this permission", substring = true)
        compose.onNodeWithText("Permission not granted").assertExists()
        compose.onNodeWithTag("pref-notifications-allow").performScrollTo().performClick()
        compose.onNodeWithTag("pref-notifications-settings").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(1, requests)
            assertEquals(1, opened)
        }
    }

    @Test fun disabledNotificationsOnlyPointAtAndroidSettings() {
        var opened = 0
        screen(
            access = SessionNotificationAccess.DISABLED,
            onOpenNotificationSettings = { opened++ },
        )
        compose.onNodeWithTag("pref-notifications").performScrollTo()
            .assertTextContains("turned off in Android", substring = true)
        compose.onNodeWithTag("pref-notifications-allow").assertDoesNotExist()
        compose.onNodeWithTag("pref-notifications-settings").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, opened) }
    }

    @Test fun appearanceRowsExposeThePersistedSelectionAndDeliverRealCallbacks() {
        var language = ""
        var theme = ""
        screen(
            settings = UiPreferences(language = "en", theme = "night"),
            onLanguage = { language = it },
            onTheme = { theme = it },
        )
        compose.onNodeWithText("English").assertIsSelected()
        compose.onNodeWithText("Night").performScrollTo().assertIsSelected()
        compose.onNodeWithTag("pref-language-ar").performScrollTo().performClick()
        compose.onNodeWithTag("pref-theme-day").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals("ar", language)
            assertEquals("day", theme)
        }
    }

    @Test fun resetAsksBeforeChangingAnything() {
        var resets = 0
        screen(onReset = { resets++ })
        compose.onNodeWithTag("pref-reset").performScrollTo().performClick()
        compose.onNodeWithText("Reset preferences?").assertExists()
        compose.runOnIdle { assertEquals(0, resets) }
        compose.onNodeWithTag("pref-reset-cancel").performClick()
        compose.runOnIdle { assertEquals(0, resets) }
        compose.onNodeWithTag("pref-reset").performScrollTo().performClick()
        compose.onNodeWithTag("pref-reset-confirm").performClick()
        compose.runOnIdle { assertEquals(1, resets) }
        compose.onNodeWithText("Reset preferences?").assertDoesNotExist()
    }

    @Test fun visualEnglishDayAt360DpShowsTheTopAndEveryLowerSection() {
        screen(widthDp = 360)
        compose.onNodeWithTag("settings-version").assertIsDisplayed()
        capture("settings-en-day-360-1.0-top")
        compose.onNodeWithTag("pref-notifications").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("pref-reset").performScrollTo().assertIsDisplayed()
        capture("settings-en-day-360-1.0-bottom")
    }

    @Test fun visualArabicNightAtLargeTextKeepsNotificationsAndResetReadable() {
        val arabic = localized(base, "ar")
        screen(
            language = "ar",
            night = true,
            direction = LayoutDirection.Rtl,
            widthDp = 360,
            fontScale = 1.6f,
        )
        compose.onNodeWithTag("route-settings").assertExists()
        compose.onNodeWithTag("pref-notifications").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(arabic.getString(R.string.settings_notifications_title)).assertExists()
        capture("settings-ar-night-360-1.6-notifications")
        compose.onNodeWithTag("pref-reset").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("pref-reset-confirm").assertDoesNotExist()
        capture("settings-ar-night-360-1.6-bottom")
    }

    @Test fun visualResetDialogFits360DpAtLargeText() {
        screen(widthDp = 360, fontScale = 1.6f)
        compose.onNodeWithTag("pref-reset").performScrollTo().performClick()
        compose.onNodeWithText("Reset preferences?").assertExists()
        captureDialog("settings-en-day-360-1.6-reset-dialog")
        compose.onNodeWithTag("pref-reset-cancel").performClick()
        compose.onNodeWithText("Reset preferences?").assertDoesNotExist()
    }

    @Test fun arabicResetDialogUsesArabicCopyAndKeepsRtlButtons() {
        val arabic = localized(base, "ar")
        screen(
            language = "ar",
            night = true,
            direction = LayoutDirection.Rtl,
            settings = UiPreferences(language = "ar", theme = "night"),
        )
        compose.onNodeWithTag("pref-reset").performScrollTo().performClick()
        compose.onNodeWithText(arabic.getString(R.string.settings_reset_confirm_title)).assertExists()
        compose.onNodeWithText(arabic.getString(R.string.settings_reset_confirm_body)).assertExists()
        compose.onNodeWithText(arabic.getString(R.string.cancel)).assertExists()
        captureDialog("settings-ar-night-411-reset-dialog")
        compose.onNodeWithTag("pref-reset-cancel").performClick()
        compose.onNodeWithText(arabic.getString(R.string.settings_reset_confirm_title)).assertDoesNotExist()
    }
}
