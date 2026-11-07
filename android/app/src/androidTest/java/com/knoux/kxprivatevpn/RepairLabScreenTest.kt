package com.knoux.kxprivatevpn

import android.content.Context
import android.content.res.Configuration
import android.text.format.Formatter
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.features.RepairLabScreen
import com.knoux.kxprivatevpn.features.RepairServiceRoute
import com.knoux.kxprivatevpn.features.RepairUiState
import com.knoux.kxprivatevpn.platform.repair.RepairCatalog
import com.knoux.kxprivatevpn.platform.repair.RepairJournalEntry
import com.knoux.kxprivatevpn.platform.repair.RepairMeasurement
import com.knoux.kxprivatevpn.platform.repair.RepairOutcome
import com.knoux.kxprivatevpn.platform.storage.CacheCandidate
import com.knoux.kxprivatevpn.platform.storage.CachePreview
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.Locale

/**
 * The Repair Lab may only show measured values and honest availability. These tests inject
 * explicit states: no value is inferred from missing data, and every service card routes.
 */
class RepairLabScreenTest {
    @get:Rule val compose = createComposeRule()

    /**
     * The unlabelled runs pin English for both rendering and assertions. Several fixtures here
     * are Latin-digit strings ("free of", "5 files"), and a device whose system locale is
     * Arabic would otherwise render Arabic-Indic numerals and fail on formatting alone.
     */
    private val base: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val context: Context
        get() = localized(base, "en")
    private val now = 1_760_000_000_000L

    private fun preview(files: Int = 2, limited: Boolean = false): CachePreview =
        CachePreview((0 until files).map {
            CacheCandidate("folder/file$it.tmp", 1024L * (it + 1), now, "sha$it")
        }, limited)

    private fun measurement(
        total: Long? = 64L * 1024 * 1024 * 1024,
        available: Long? = 21L * 1024 * 1024 * 1024,
        cache: Long? = 12L * 1024 * 1024,
        cacheFiles: Int = 5,
        preview: CachePreview? = preview(),
        previewFailed: Boolean = false,
        walkLimited: Boolean = false,
    ) = RepairMeasurement(total, available, cache, cacheFiles, walkLimited, preview,
        previewFailed, now)

    private fun text(id: Int, vararg args: Any): String = context.getString(id, *args)

    private fun localized(base: Context, language: String): Context {
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(language))
        return base.createConfigurationContext(configuration)
    }

    private fun screen(
        state: RepairUiState,
        night: Boolean = false,
        arabic: Boolean = false,
        fontScale: Float = 1f,
        onBack: () -> Unit = {},
        onAnalyze: () -> Unit = {},
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
                    RepairLabScreen(
                        night = night,
                        state = state,
                        onBack = onBack,
                        onAnalyze = onAnalyze,
                        onNavigate = onNavigate,
                    )
                }
            }
        }
    }

    private fun capture(name: String) {
        captureEvidence(context, compose, name)
    }

    @Test fun tenServicesAreListedWithHonestAvailability() {
        screen(RepairUiState(loading = false, measurement = measurement()))

        listOf("storage", "ghost", "duplicate", "heavyweight", "cache",
            "media", "download", "session", "network", "autopilot").forEach {
            compose.onNodeWithTag("repair-card-$it").assertExists()
        }
        compose.onNodeWithTag("repair-services-count")
            .assertTextEquals(text(R.string.repair_services_count, 8, 10))
        // The compacted card announces the service once, with exactly one tiny status.
        val ready = text(R.string.repair_status_ready)
        val duplicateDescription = compose.onNodeWithTag("repair-card-duplicate")
            .fetchSemanticsNode().config[SemanticsProperties.ContentDescription].joinToString()
        assertEquals(
            "${text(R.string.repair_service_duplicate)}. $ready", duplicateDescription,
        )
        assertEquals(1, duplicateDescription.split(ready).size - 1)
        compose.onNodeWithTag("repair-volume").assertTextContains("free of", substring = true)
        compose.onNodeWithTag("repair-cache").assertTextContains("5 files", substring = true)
        compose.onNodeWithTag("repair-measured-at").assertExists()
        compose.onNodeWithTag("repair-analyze").assertIsDisplayed()
        capture("43-repair-lab-day-light")
        compose.onNodeWithTag("repair-card-autopilot").performScrollTo().assertIsDisplayed()
        capture("43b-repair-lab-services-day")
    }

    @Test fun analyzeAndCleanupReviewInvokeRealNavigationCallbacks() {
        var analyzed = 0
        var route: String? = null
        screen(RepairUiState(loading = false, measurement = measurement()),
            onAnalyze = { analyzed++ }, onNavigate = { route = it })

        compose.onNodeWithTag("repair-analyze").performScrollTo().performClick()
        assertEquals(1, analyzed)
        compose.onNodeWithTag("repair-review-cache").performScrollTo().performClick()
        assertEquals("cache", route)
    }

    @Test fun partialMeasurementIsStatedInsteadOfFakingZero() {
        screen(RepairUiState(loading = false, measurement = measurement(
            total = null, available = null, cache = 12L * 1024 * 1024, cacheFiles = 5,
            preview = null, previewFailed = true,
        )))

        compose.onNodeWithTag("repair-partial").assertExists()
        compose.onNodeWithTag("repair-partial").assertTextContains(
            text(R.string.repair_volume_missing), substring = true)
        compose.onNodeWithTag("repair-partial").assertTextContains(
            text(R.string.repair_preview_missing), substring = true)
        compose.onNodeWithTag("repair-volume").assertDoesNotExist()
        compose.onNodeWithTag("repair-cleanup").assertDoesNotExist()
    }

    @Test fun failedFirstMeasurementShowsNoInventedValues() {
        screen(RepairUiState(loading = false, measuring = false, failure = true))

        compose.onNodeWithTag("repair-no-measurement")
            .assertTextEquals(text(R.string.repair_measure_failed))
        compose.onNodeWithTag("repair-volume").assertDoesNotExist()
        compose.onNodeWithTag("repair-cleanup").assertDoesNotExist()
    }

    @Test fun journalShowsOnlyRecordedOperationsAndAnHonestEmptyState() {
        val state = mutableStateOf(RepairUiState(loading = false, measurement = measurement()))
        compose.setContent {
            val english = localized(base, "en")
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalContext provides english,
                LocalConfiguration provides english.resources.configuration,
                LocalDensity provides Density(density.density, 1f),
                LocalLayoutDirection provides LayoutDirection.Ltr,
            ) {
                Box(Modifier.fillMaxSize()) {
                    RepairLabScreen(
                        night = false,
                        state = state.value,
                        onBack = {},
                        onAnalyze = {},
                        onNavigate = {},
                    )
                }
            }
        }
        compose.onNodeWithTag("repair-journal-empty")
            .assertTextEquals(text(R.string.repair_journal_empty))

        state.value = state.value.copy(journal = listOf(RepairJournalEntry(
            now, "cache", "cache_remove", RepairOutcome.COMPLETE, 2, 4096)))
        compose.waitForIdle()
        compose.onNodeWithText(text(R.string.repair_journal_complete)).assertExists()
        // The same human-readable label can appear on several service cards.
        // Assert the specific recorded journal action, not an arbitrary text match.
        compose.onNodeWithTag("repair-journal-action")
            .assertTextEquals(text(R.string.repair_action_cache_remove))
        compose.onNodeWithText(
            text(R.string.repair_journal_measure, 2, Formatter.formatFileSize(context, 4096)),
        ).assertExists()
    }

    @Test fun nightArabicLargeTextKeepsRealStateVisible() {
        val ar = localized(context, "ar")
        screen(RepairUiState(loading = false, measurement = measurement()),
            night = true, arabic = true, fontScale = 1.6f)

        compose.onNodeWithText(ar.getString(R.string.repair_title)).assertExists()
        compose.onNodeWithTag("repair-card-storage").performScrollTo().assertExists()
        compose.onNodeWithTag("repair-card-autopilot").performScrollTo().assertExists()
        compose.onNodeWithTag("repair-volume").assertTextContains("من", substring = true)
        capture("44-repair-lab-night-ar-large-top")
        compose.onNodeWithTag("repair-card-autopilot").performScrollTo().assertIsDisplayed()
        capture("44b-repair-lab-night-ar-large-services")
    }

    /**
     * A tool without an implementation must open an honest unavailable panel: no invented
     * measurement, no simulated progress. The subject is read from the catalog instead of being
     * hard coded, so the assertion keeps its meaning when a tool becomes real.
     */
    @Test fun unavailableDetailStatesNoSimulation() {
        val unavailable = RepairCatalog.services.first { !it.id.available }
        val service = RepairCatalog.service(unavailable.id)
        compose.setContent {
            val english = localized(base, "en")
            CompositionLocalProvider(
                LocalContext provides english,
                LocalConfiguration provides english.resources.configuration,
                LocalLayoutDirection provides LayoutDirection.Ltr,
            ) {
                Box(Modifier.fillMaxSize()) {
                    RepairServiceRoute(unavailable.id.route, night = false, onBack = {})
                }
            }
        }

        compose.onNodeWithText(context.getString(service.titleRes)).assertExists()
        compose.onNodeWithText(context.getString(R.string.repair_unavailable_body)).assertExists()
        compose.onNodeWithText(
            context.getString(R.string.repair_requires,
                context.getString(service.requiresRes)),
        ).assertExists()
        compose.onNodeWithText(context.getString(R.string.repair_unavailable)).assertExists()
        capture("45-repair-unavailable-tool")
    }
}
