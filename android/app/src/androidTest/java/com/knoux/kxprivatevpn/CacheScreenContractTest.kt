package com.knoux.kxprivatevpn

import android.content.Context
import android.content.res.Configuration
import android.text.format.Formatter
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.designsystem.Atmosphere
import com.knoux.kxprivatevpn.features.CacheScreenContent
import com.knoux.kxprivatevpn.features.CacheState
import com.knoux.kxprivatevpn.platform.storage.CacheCandidate
import com.knoux.kxprivatevpn.platform.storage.CachePreview
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.Locale

/**
 * Cache acceptance contract: only confirmed, measured selections reach the remover, and the
 * official-settings route stays visible in night Arabic at a large font scale.
 */
class CacheScreenContractTest {
    @get:Rule val compose = createComposeRule()
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun preview() = CachePreview(
        files = listOf(
            CacheCandidate("first.tmp", 128, 0, "hash-first"),
            CacheCandidate("second.tmp", 256, 0, "hash-second"),
        ),
        limited = false,
    )

    private fun localized(base: Context, language: String): Context {
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(language))
        return base.createConfigurationContext(configuration)
    }

    private fun render(
        state: CacheState = CacheState(preview = preview()),
        arabic: Boolean = false,
        night: Boolean = false,
        fontScale: Float = 1f,
        onScan: () -> Unit = {},
        onRemove: (Set<String>) -> Unit = {},
        onOpenSettings: () -> Unit = {},
    ) {
        compose.setContent {
            val base = InstrumentationRegistry.getInstrumentation().targetContext
            val localizedContext = localized(base, if (arabic) "ar" else "en")
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedContext.resources.configuration,
                LocalDensity provides Density(density.density, fontScale),
                LocalLayoutDirection provides if (arabic) LayoutDirection.Rtl else LayoutDirection.Ltr,
            ) {
                val foreground = if (night) Color(0xFFF7F3FF) else Color(0xFF171050)
                MaterialTheme(colorScheme = if (night) darkColorScheme(primary = Color(0xFF99EBFF))
                    else lightColorScheme(primary = Color(0xFF4420B3))) {
                    CompositionLocalProvider(LocalContentColor provides foreground) {
                        Box(Modifier.fillMaxSize()) {
                            Atmosphere(night, 0f)
                            Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                                CacheScreenContent(
                                    state = state,
                                    onBusy = {},
                                    onScan = onScan,
                                    onRemove = onRemove,
                                    onOpenSettings = onOpenSettings,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        captureEvidence(context, compose, name)
    }

    @Test fun confirmationReceivesExactlyTheSelectedPathsAndSettingsRouteFires() {
        var removed: Set<String>? = null
        var settingsOpened = 0
        render(onRemove = { removed = it }, onOpenSettings = { settingsOpened++ })

        compose.onNodeWithTag("cache-system-settings").performScrollTo().performClick()
        assertEquals(1, settingsOpened)
        compose.onNodeWithText("first.tmp").performScrollTo().performClick()
        val en = localized(context, "en")
        compose.onNodeWithTag("cache-selected-summary").performScrollTo().assertTextEquals(
            en.getString(R.string.cache_selected_summary, 1,
                Formatter.formatFileSize(en, 128)))
        compose.onNodeWithTag("cache-remove").performScrollTo().assertIsEnabled().performClick()
        compose.onNodeWithTag("cache-confirm").performClick()
        assertEquals(setOf("first.tmp"), removed)
    }

    @Test fun nightArabicLargeTextKeepsScopeSettingsAndMeasuredSelectionVisible() {
        val ar = localized(context, "ar")
        render(arabic = true, night = true, fontScale = 1.6f)

        compose.onNodeWithText(ar.getString(R.string.cache_scope)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("cache-system-settings").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(ar.getString(R.string.cache_other_apps_action)).assertExists()
        capture("61-cache-night-ar-large-scope")

        compose.onNodeWithText("first.tmp").performScrollTo().performClick()
        compose.onNodeWithTag("cache-selected-summary").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("cache-remove").performScrollTo().assertIsDisplayed()
        capture("61b-cache-night-ar-large-selected")
    }

    @Test fun nativeHeroDoesNotInventResultsBeforeScan() {
        val ar = localized(context, "ar")
        render(state = CacheState(preview = null), arabic = true, night = true)
        compose.onNodeWithTag("cache-ref-hero").assertExists()
        compose.onNodeWithTag("cache-ref-metrics").assertDoesNotExist()
        compose.onNodeWithText(ar.getString(R.string.cache_scope)).performScrollTo().assertExists()
        compose.onNodeWithTag("cache-scan").performScrollTo().assertIsEnabled()
    }

    @Test fun measuredPreviewShowsRealDataWithExplicitCleanupGate() {
        val en = localized(context, "en")
        render(state = CacheState(preview = preview()), arabic = false, night = false)
        compose.onNodeWithTag("cache-ref-metrics").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(en.getString(R.string.cache_ref_scope_value)).assertExists()
        compose.onNodeWithText(en.getString(R.string.cache_ref_safety)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("cache-remove").performScrollTo().assertIsNotEnabled()
    }
}
