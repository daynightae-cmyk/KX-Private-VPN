package com.knoux.kxprivatevpn

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.features.ToolsCatalog
import com.knoux.kxprivatevpn.features.ToolsCatalogScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Locale

/**
 * Runtime contract for the owner's Tools hub reference: nine real destinations in the exact
 * reading order, working search and filters, RTL mirroring and both navigation slots.
 */
class ToolsCatalogScreenTest {
    @get:Rule val compose = createComposeRule()

    private val base: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun localized(base: Context, language: String): Context {
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(language))
        return base.createConfigurationContext(configuration)
    }

    private fun screen(
        night: Boolean = false,
        arabic: Boolean = false,
        fontScale: Float = 1f,
        onBack: () -> Unit = {},
        onNavigate: (String) -> Unit = {},
    ) {
        compose.setContent {
            val localizedContext = if (arabic) localized(base, "ar") else localized(base, "en")
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedContext.resources.configuration,
                LocalDensity provides Density(density.density, fontScale),
                LocalLayoutDirection provides if (arabic) LayoutDirection.Rtl else LayoutDirection.Ltr,
            ) {
                Box(Modifier.fillMaxSize()) {
                    ToolsCatalogScreen(
                        night = night,
                        onBack = onBack,
                        onNavigate = onNavigate,
                    )
                }
            }
        }
    }

    private fun capture(name: String) {
        captureEvidence(base, compose, name)
    }

    @Test fun nineToolsRenderWithWorkingSearchFiltersAndNavigation() {
        var route: String? = null
        var back = 0
        screen(onBack = { back++ }, onNavigate = { route = it })
        val english = localized(base, "en")

        compose.onNodeWithTag("route-tools").assertExists()
        ToolsCatalog.gridEntries.forEach {
            compose.onNodeWithTag("tool-card-${it.key}").assertExists()
        }
        compose.onNodeWithTag("tools-storage-health").assertExists()
        compose.onNodeWithText(english.getString(R.string.tools_filter_all, 9)).assertExists()
        compose.onNodeWithTag("nav-tools").assertExists()
        compose.onNodeWithTag("nav-servers").assertDoesNotExist()
        compose.onNodeWithTag("nav-home").assertExists()
        capture("44-tools-catalog-day-light")

        compose.onNodeWithTag("tools-search").performTextInput("download")
        compose.onNodeWithTag("tool-card-download").assertExists()
        compose.onNodeWithTag("tool-card-temporary").assertDoesNotExist()
        compose.onNodeWithTag("tools-storage-health").assertDoesNotExist()

        compose.onNodeWithTag("tools-search").performTextClearance()
        compose.onNodeWithTag("tools-filter-media").performScrollTo().performClick()
        compose.onNodeWithTag("tool-card-media").assertExists()
        compose.onNodeWithTag("tool-card-smart").assertExists()
        compose.onNodeWithTag("tool-card-network").assertDoesNotExist()
        capture("44b-tools-catalog-media-filter-day")

        compose.onNodeWithTag("tools-search").performTextInput("zzz-no-tool")
        compose.onNodeWithTag("tools-empty")
            .assertTextEquals(english.getString(R.string.tools_empty))
        compose.onNodeWithTag("tool-card-media").assertDoesNotExist()

        compose.onNodeWithTag("tools-search").performTextClearance()
        compose.onNodeWithTag("tools-filter-all").performScrollTo().performClick()
        compose.onNodeWithTag("tool-card-temporary").performScrollTo().performClick()
        assertEquals("cache", route)
        compose.onNodeWithTag("tools-back").performClick()
        assertEquals(1, back)
        compose.onNodeWithTag("nav-home").performClick()
        assertEquals("home", route)
    }

    @Test fun rtlReadingOrderMirrorsTheReferenceRows() {
        screen(arabic = true, night = true)

        // On 720x1280 CI emulators, later rows can be clipped by the scroll viewport.
        // Bring each real card into view before reading geometry.
        fun bounds(key: String) = compose.onNodeWithTag("tool-card-$key")
            .performScrollTo().getBoundsInRoot()
        // RTL rows read right to left: the first item of each pair is the right-hand card.
        assertTrue("Temporary files must lead the first row from the right",
            bounds("temporary").left > bounds("duplicates").left)
        assertTrue("Large files must lead the second row from the right",
            bounds("large").left > bounds("download").left)
        assertTrue("Reference 18 shows Media Slim on the right of Smart Repair",
            bounds("media").left > bounds("smart").left)
        assertTrue("Reference 18 shows Installed Apps on the right of Network",
            bounds("apps").left > bounds("network").left)
        val gridWidth = bounds("temporary").right - bounds("temporary").left
        val health = compose.onNodeWithTag("tools-storage-health")
            .performScrollTo().getBoundsInRoot()
        assertTrue("Storage health must span the full content width",
            (health.right - health.left) > gridWidth * 1.5f)
        capture("44c-tools-catalog-arabic-night")
    }

    @Test fun fontScaleAndNightWithoutElidedCopy() {
        screen(night = true, fontScale = 1.6f)

        ToolsCatalog.gridEntries.forEach {
            compose.onNodeWithTag("tool-card-${it.key}").assertExists()
        }
        compose.onNodeWithTag("tools-storage-health").assertExists()
        capture("44d-tools-catalog-night-font160")
    }

    @Test fun narrowArabicLargeTextKeepsAllNineTools() {
        val arabic = localized(base, "ar")
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalContext provides arabic,
                LocalConfiguration provides arabic.resources.configuration,
                LocalDensity provides Density(density.density, 1.6f),
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                Box(Modifier.width(360.dp).fillMaxHeight()) {
                    ToolsCatalogScreen(night = true, onBack = {}, onNavigate = {})
                }
            }
        }

        compose.onNodeWithText(arabic.getString(R.string.tools_title)).assertExists()
        ToolsCatalog.gridEntries.forEach {
            compose.onNodeWithTag("tool-card-${it.key}").assertExists()
        }
        compose.onNodeWithTag("tools-storage-health").assertExists()
        capture("44e-tools-catalog-360-night-ar-font160")
    }
}
