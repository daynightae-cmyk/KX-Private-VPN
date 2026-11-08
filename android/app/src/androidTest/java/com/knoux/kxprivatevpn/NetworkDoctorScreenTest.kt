package com.knoux.kxprivatevpn

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.designsystem.KxGlassTheme
import com.knoux.kxprivatevpn.features.NetworkDoctorScreen
import com.knoux.kxprivatevpn.features.NetworkUiState
import com.knoux.kxprivatevpn.platform.network.HttpsReading
import com.knoux.kxprivatevpn.platform.network.NetworkReading
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Presentation only: a fabricated reading proves the screen reports it without inventing more. */
class NetworkDoctorScreenTest {
    @get:Rule val compose = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun show(initial: NetworkUiState, onRefresh: () -> Unit = {}): MutableState<NetworkUiState> {
        val state = mutableStateOf(initial)
        compose.setContent {
            KxGlassTheme(night = true) {
                Box(Modifier.fillMaxSize()) {
                    NetworkDoctorScreen(
                        night = true,
                        state = state.value,
                        onBack = {},
                        onRefresh = onRefresh,
                        onUrlChange = {},
                        onConsentChange = {},
                        onRunProbe = {},
                    )
                }
            }
        }
        return state
    }

    @Test fun reference02HasActualKxOrbAndNoFabricatedHttpsMeasurement() {
        show(NetworkUiState(reading = NetworkReading(
            present = true,
            validated = true,
            captive = false,
            vpn = false,
            transports = listOf("Wi-Fi"),
            dns = listOf("1.1.1.1"),
        )))
        compose.onNodeWithTag("network-ref-hero").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("network-ref-brand-orb").assertIsDisplayed()
        compose.onNodeWithTag("network-ref-summary").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("network-ref-https").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("network-ref-latency").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.network_ref_not_tested)).assertIsDisplayed()
    }

    @Test fun reference02StartCallsReadOnlyNetworkRefreshNotProbe() {
        var refreshes = 0
        show(NetworkUiState(), onRefresh = { refreshes++ })
        compose.onNodeWithTag("network-ref-start").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, refreshes) }
        compose.onNodeWithTag("probe-run").performScrollTo().assertIsNotEnabled()
    }

    @Test fun reportsEveryMeasuredValueAndItsLimits() {
        show(
            NetworkUiState(
                reading = NetworkReading(
                    present = true,
                    validated = false,
                    captive = true,
                    vpn = true,
                    transports = listOf("Wi-Fi", "VPN"),
                    dns = listOf("1.1.1.1"),
                ),
            )
        )
        val yes = context.getString(R.string.answer_yes)
        val no = context.getString(R.string.answer_no)
        compose.onNodeWithTag("network-metric-present").assertTextContains(yes)
        compose.onNodeWithTag("network-metric-validated").assertTextContains(no)
        compose.onNodeWithTag("network-metric-captive").assertTextContains(yes)
        compose.onNodeWithTag("network-metric-vpn").assertTextContains(yes)
        // One Text node carries the label plus both transports; exact-item semantics
        // would only pass if the tag sat on a node whose whole text was the value.
        compose.onNodeWithTag("network-transports")
            .assertTextEquals(context.getString(R.string.network_transports, "Wi-Fi · VPN"))
        compose.onNodeWithTag("network-dns")
            .assertTextEquals(context.getString(R.string.network_dns, "1.1.1.1"))
        // The limits copy is the product promise: no speed, traffic or leak claim.
        compose.onNodeWithTag("network-limits")
            .assertTextEquals(context.getString(R.string.network_readonly_body))
        compose.onNodeWithTag("network-scope").performScrollTo().assertIsDisplayed()
    }

    @Test fun missingReadingIsReportedInsteadOfInventedMetrics() {
        show(NetworkUiState(readFailed = true))
        compose.onNodeWithTag("network-read-failed").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithTag("network-metric-present").assertCountEquals(0)
        compose.onAllNodesWithTag("network-dns").assertCountEquals(0)
    }

    @Test fun probeStaysDisabledUntilConsentAndAUsableTarget() {
        val state = show(NetworkUiState(probeUrl = "https://example.invalid", probeConsent = false))
        compose.onNodeWithTag("probe-run").assertIsNotEnabled()

        compose.runOnIdle {
            state.value = NetworkUiState(probeUrl = "http://example.invalid", probeConsent = true)
        }
        compose.waitForIdle()
        compose.onNodeWithTag("probe-invalid").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("probe-run").assertIsNotEnabled()

        compose.runOnIdle {
            state.value = NetworkUiState(probeUrl = "https://example.invalid", probeConsent = true)
        }
        compose.waitForIdle()
        compose.onNodeWithTag("probe-run").assertIsEnabled()

        compose.runOnIdle {
            state.value = NetworkUiState(
                probeUrl = "https://example.invalid",
                probeConsent = true,
                probeResult = HttpsReading(code = 204, elapsedMs = 87),
            )
        }
        compose.waitForIdle()
        compose.onNodeWithTag("probe-receipt").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("probe-result")
            .assertTextEquals(context.getString(R.string.probe_result, 204, 87))
        compose.onNodeWithTag("probe-receipt-note").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithTag("probe-failed").assertCountEquals(0)
    }

    @Test fun failedProbeShowsNoReceipt() {
        show(NetworkUiState(probeUrl = "https://example.invalid", probeConsent = true, probeFailed = true))
        compose.onNodeWithTag("probe-failed").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithTag("probe-receipt").assertCountEquals(0)
    }
}
