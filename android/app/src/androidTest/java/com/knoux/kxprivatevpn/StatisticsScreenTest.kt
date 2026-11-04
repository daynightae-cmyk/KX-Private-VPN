package com.knoux.kxprivatevpn

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.text.format.Formatter
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.features.StatisticsScreen
import com.knoux.kxprivatevpn.features.StatisticsUiState
import com.knoux.kxprivatevpn.platform.usage.StatsRange
import com.knoux.kxprivatevpn.platform.usage.UsageAggregator
import com.knoux.kxprivatevpn.platform.usage.UsageRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/**
 * The statistics screen is only allowed to render measured values. Cards without a measurement
 * must say so, the chart and donut must stay empty instead of pretending traffic, and the clear
 * flow must never delete anything without confirmation.
 *
 * Exactly one setContent call per test (Compose test rule requirement); reactive tests use a
 * single content block whose state changes through the screen callbacks.
 */
class StatisticsScreenTest {
    @get:Rule val compose = createComposeRule()

    private val today = LocalDate.now()
    private val zone: ZoneId = ZoneId.systemDefault()
    private val now = today.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun record(
        daysAgo: Long = 0,
        hour: Int = 10,
        duration: Long = 30 * 60_000,
        rx: Long = 1_500_000,
        tx: Long = 500_000,
        endpoint: String = "sg.example:51820",
        name: String = "Singapore",
    ) = UsageRecord(
        profileId = "profile-$endpoint",
        profileName = name,
        endpoint = endpoint,
        startedAtMillis = today.minusDays(daysAgo).atTime(hour, 0).atZone(zone)
            .toInstant().toEpochMilli(),
        durationMillis = duration,
        rxBytes = rx,
        txBytes = tx,
    )

    private fun state(
        records: List<UsageRecord> = emptyList(),
        range: StatsRange = StatsRange.WEEK,
        loading: Boolean = false,
        loadFailed: Boolean = false,
        saveFailed: Boolean = false,
        clearFailed: Boolean = false,
        confirmClear: Boolean = false,
        clearing: Boolean = false,
    ) = StatisticsUiState(
        loading = loading,
        loadFailed = loadFailed,
        saveFailed = saveFailed,
        clearFailed = clearFailed,
        clearing = clearing,
        confirmClear = confirmClear,
        range = range,
        records = records,
        summary = UsageAggregator.summarize(records, range, now, zone),
    )

    private fun text(id: Int, vararg args: Any): String = context.getString(id, *args)

    private fun localized(base: Context, language: String): Context {
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(language))
        return base.createConfigurationContext(configuration)
    }

    private fun screen(
        state: StatisticsUiState,
        night: Boolean = false,
        arabic: Boolean = false,
        fontScale: Float = 1f,
        onBack: () -> Unit = {},
        onRefresh: () -> Unit = {},
        onRangeChange: (StatsRange) -> Unit = {},
        onOpenClear: () -> Unit = {},
        onDismissClear: () -> Unit = {},
        onConfirmClear: () -> Unit = {},
        onNavigate: (String) -> Unit = {},
    ) {
        compose.setContent {
            val base = InstrumentationRegistry.getInstrumentation().targetContext
            val localizedContext = if (arabic) localized(base, "ar") else base
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedContext.resources.configuration,
                LocalDensity provides Density(density.density, fontScale),
                LocalLayoutDirection provides if (arabic) LayoutDirection.Rtl else LayoutDirection.Ltr,
            ) {
                Box(Modifier.fillMaxSize()) {
                    StatisticsScreen(
                        night = night,
                        state = state,
                        onBack = onBack,
                        onRefresh = onRefresh,
                        onRangeChange = onRangeChange,
                        onOpenClear = onOpenClear,
                        onDismissClear = onDismissClear,
                        onConfirmClear = onConfirmClear,
                        onNavigate = onNavigate,
                    )
                }
            }
        }
    }

    private fun capture(name: String) {
        val folder = File(context.getExternalFilesDir(null), "identity-evidence").apply { mkdirs() }
        File(folder, "$name.png").outputStream().use { stream ->
            check(compose.onRoot().captureToImage().asAndroidBitmap()
                .compress(Bitmap.CompressFormat.PNG, 100, stream))
        }
    }

    /** Dialogs create a second Compose root; capture the tagged node instead of onRoot(). */
    private fun captureNode(tag: String, name: String) {
        val folder = File(context.getExternalFilesDir(null), "identity-evidence").apply { mkdirs() }
        File(folder, "$name.png").outputStream().use { stream ->
            check(compose.onNodeWithTag(tag).captureToImage().asAndroidBitmap()
                .compress(Bitmap.CompressFormat.PNG, 100, stream))
        }
    }

    @Test fun emptyHistoryShowsNoSessionsInsteadOfInventedNumbers() {
        screen(state())

        compose.onNodeWithTag("stats-total-value")
            .assertTextEquals(text(R.string.stats_no_sessions))
        compose.onNodeWithTag("stats-speed-value")
            .assertTextEquals(text(R.string.stats_no_data))
        compose.onNodeWithTag("stats-server-value")
            .assertTextEquals(text(R.string.stats_no_data))
        compose.onNodeWithTag("stats-time-value")
            .assertTextEquals(text(R.string.stats_no_data))
        compose.onNodeWithTag("stats-chart-empty").assertIsDisplayed()
        compose.onNodeWithTag("stats-time-total").assertTextEquals(text(R.string.stats_no_data))
        capture("36-statistics-defaults-light")
        compose.onNodeWithTag("stats-no-servers").performScrollTo().assertIsDisplayed()
    }

    @Test fun measuredTotalsRateServerAndHistoryComeFromRecords() {
        val records = listOf(
            record(duration = 30 * 60_000, rx = 1_500_000, tx = 500_000),
            record(
                duration = 10 * 60_000, rx = 400_000, tx = 100_000,
                endpoint = "de.example:51820", name = "Germany",
            ),
        )
        screen(state(records))

        compose.onNodeWithTag("stats-total-value").assertTextEquals(
            Formatter.formatFileSize(context, 2_500_000),
        )
        compose.onNodeWithTag("stats-server-value").assertTextEquals("Singapore")
        compose.onNodeWithTag("stats-time-value").assertTextEquals(text(R.string.stats_duration_m, 40))
        compose.onNodeWithTag("stats-speed-total").assertTextEquals(
            text(R.string.stats_rate_kbps, "8"),
        )
        compose.onNodeWithTag("stats-speed-rx").assertTextContains(
            text(R.string.stats_rate_kbps, "6"),
        )
        compose.onNodeWithTag("stats-speed-tx").assertTextContains(
            text(R.string.stats_rate_kbps, "2"),
        )
        capture("37-statistics-populated-light")
        compose.onNodeWithTag("stats-server-row-sg.example:51820").performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithTag("stats-server-row-de.example:51820").performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithTag("stats-note").performScrollTo().assertIsDisplayed()
    }

    @Test fun rangeSelectionRebuildsTheWindowFromStoredRecords() {
        val records = listOf(
            record(daysAgo = 0, rx = 1_000_000, tx = 0, duration = 20 * 60_000),
            record(
                daysAgo = 20, rx = 3_000_000, tx = 0, duration = 40 * 60_000,
                endpoint = "us.example:51820", name = "United States",
            ),
        )
        var range by mutableStateOf(StatsRange.WEEK)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, 1f),
                LocalLayoutDirection provides LayoutDirection.Ltr,
            ) {
                Box(Modifier.fillMaxSize()) {
                    StatisticsScreen(
                        night = false,
                        state = state(records, range),
                        onBack = {},
                        onRefresh = {},
                        onRangeChange = { range = it },
                        onOpenClear = {},
                        onDismissClear = {},
                        onConfirmClear = {},
                        onNavigate = {},
                    )
                }
            }
        }

        compose.onNodeWithTag("stats-range-7").assertIsSelected()
        compose.onNodeWithTag("stats-range-30").performScrollTo().performClick()
        compose.waitForIdle()

        assertEquals(StatsRange.MONTH, range)
        compose.onNodeWithTag("stats-range-30").assertIsSelected()
        compose.onNodeWithTag("stats-total-value").assertTextEquals(
            Formatter.formatFileSize(context, 4_000_000),
        )
        capture("42-statistics-30d-range")
    }

    @Test fun clearHistoryDialogOpensAndRequiresConfirmation() {
        var confirm by mutableStateOf(false)
        var confirmed = 0
        var dismissed = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, 1f),
                LocalLayoutDirection provides LayoutDirection.Ltr,
            ) {
                Box(Modifier.fillMaxSize()) {
                    StatisticsScreen(
                        night = false,
                        state = state(confirmClear = confirm),
                        onBack = {},
                        onRefresh = {},
                        onRangeChange = {},
                        onOpenClear = { confirm = true },
                        onDismissClear = { dismissed++; confirm = false },
                        onConfirmClear = { confirmed++; confirm = false },
                        onNavigate = {},
                    )
                }
            }
        }

        compose.onNodeWithTag("stats-menu").performClick()
        compose.onNodeWithTag("stats-clear-dialog").assertIsDisplayed()
        compose.onNodeWithText(text(R.string.stats_clear_title)).assertIsDisplayed()
        captureNode("stats-clear-dialog", "38-statistics-clear-dialog")

        compose.onNodeWithTag("stats-clear-cancel").performClick()
        compose.waitForIdle()
        assertEquals(1, dismissed)
        compose.onNodeWithTag("stats-clear-dialog").assertDoesNotExist()

        compose.onNodeWithTag("stats-menu").performClick()
        compose.onNodeWithTag("stats-clear-confirm").performClick()
        compose.waitForIdle()
        assertEquals(1, confirmed)
        compose.onNodeWithTag("stats-clear-dialog").assertDoesNotExist()
    }

    @Test fun saveAndClearFailuresAreSurfacedHonestly() {
        screen(state(emptyList()).copy(saveFailed = true, clearFailed = true))

        compose.onNodeWithTag("stats-save-error").assertIsDisplayed()
        compose.onNodeWithTag("stats-clear-error").assertIsDisplayed()
    }

    @Test fun loadFailureOffersRetryAndNothingElse() {
        var retries = 0
        screen(state(loadFailed = true), onRefresh = { retries++ })

        compose.onNodeWithTag("stats-error").assertIsDisplayed()
        compose.onNodeWithTag("stats-total").assertDoesNotExist()
        compose.onNodeWithTag("stats-retry").assertIsDisplayed().performClick()
        compose.waitForIdle()
        assertEquals(1, retries)
    }

    @Test fun loadingShowsProgressWithoutInventedCards() {
        screen(state(loading = true))

        compose.onNodeWithTag("stats-loading").assertIsDisplayed()
        compose.onNodeWithTag("stats-total").assertDoesNotExist()
        compose.onNodeWithTag("stats-chart-card").assertDoesNotExist()
    }

    @Test fun bottomNavigationRoutesToRealDestinations() {
        val targets = mutableListOf<String>()
        screen(state(), onNavigate = { targets += it })

        compose.onNodeWithTag("stats-nav-stats").assertIsSelected()
        compose.onNodeWithTag("stats-nav-servers").performClick()
        compose.onNodeWithTag("stats-nav-security").performClick()
        compose.onNodeWithTag("stats-nav-profile").performClick()
        compose.onNodeWithTag("stats-nav-home").performClick()
        compose.onNodeWithTag("stats-nav-stats").performClick()
        compose.waitForIdle()

        assertEquals(listOf("servers", "security", "settings", "home"), targets)
    }

    @Test fun nightRtlKeepsStatisticsReadable() {
        screen(state(orderRecords()), night = true, arabic = true)

        compose.onNodeWithTag("stats-screen").assertIsDisplayed()
        compose.onNodeWithTag("stats-total").assertIsDisplayed()
        compose.onNodeWithTag("stats-range-7").assertIsSelected()
        capture("39-statistics-night-rtl")
    }

    @Test fun arabicLargeTextReachesEverySection() {
        screen(state(orderRecords()), arabic = true, fontScale = 1.6f)

        compose.onNodeWithTag("stats-total").assertIsDisplayed()
        capture("40-statistics-ar-large-text")
        compose.onNodeWithTag("stats-chart-card").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("stats-note").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("stats-nav").assertIsDisplayed()
    }

    private fun orderRecords(): List<UsageRecord> = listOf(
        record(duration = 45 * 60_000, rx = 2_000_000, tx = 500_000),
        record(
            duration = 25 * 60_000, rx = 900_000, tx = 200_000,
            endpoint = "us.example:51820", name = "United States",
        ),
    )
}
