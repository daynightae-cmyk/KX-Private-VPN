package com.knoux.kxprivatevpn

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.designsystem.KxGlassTheme
import com.knoux.kxprivatevpn.features.NetworkDoctorScreen
import com.knoux.kxprivatevpn.features.NetworkUiState
import com.knoux.kxprivatevpn.platform.network.HttpsReading
import com.knoux.kxprivatevpn.platform.network.NetworkReading
import org.junit.Rule
import org.junit.Test

/** Presentation only: a fabricated reading proves the screen reports it without inventing more. */
class NetworkDoctorScreenTest {
    @get:Rule val compose = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun show(state: NetworkUiState) {
        compose.setContent {
            KxGlassTheme(night = true) {
                Box(Modifier.fillMaxSize()) {
                    NetworkDoctorScreen(
                        night = true,
                        state = state,
                        onBack = {},
                        onRefresh = {},
                        onUrlChange = {},
                        onConsentChange = {},
                        onRunProbe = {},
                    )
                }
            }
        }
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
        compose.onNodeWithTag("network-transports").assertTextContains("Wi-Fi · VPN")
        compose.onNodeWithTag("network-dns").assertTextContains("1.1.1.1")
        // The limits copy is the product promise: no speed, traffic or leak claim.
        compose.onNodeWithTag("network-limits")
            .assertTextEquals(context.getString(R.string.network_readonly_body))
        compose.onNodeWithTag("network-scope").assertIsDisplayed()
    }

    @Test fun missingReadingIsReportedInsteadOfInventedMetrics() {
        show(NetworkUiState(readFailed = true))
        compose.onNodeWithTag("network-read-failed").assertIsDisplayed()
        compose.onAllNodesWithTag("network-metric-present").assertCountEquals(0)
        compose.onAllNodesWithTag("network-dns").assertCountEquals(0)
    }

    @Test fun probeStaysDisabledUntilConsentAndAUsableTarget() {
        show(NetworkUiState(probeUrl = "https://example.invalid", probeConsent = false))
        compose.onNodeWithTag("probe-run").assertIsNotEnabled()

        show(NetworkUiState(probeUrl = "http://example.invalid", probeConsent = true))
        compose.waitForIdle()
        compose.onNodeWithTag("probe-invalid").assertIsDisplayed()
        compose.onNodeWithTag("probe-run").assertIsNotEnabled()

        show(NetworkUiState(probeUrl = "https://example.invalid", probeConsent = true))
        compose.waitForIdle()
        compose.onNodeWithTag("probe-run").assertIsEnabled()

        show(
            NetworkUiState(
                probeUrl = "https://example.invalid",
                probeConsent = true,
                probeResult = HttpsReading(code = 204, elapsedMs = 87),
            )
        )
        compose.waitForIdle()
        compose.onNodeWithTag("probe-receipt").assertIsDisplayed()
        compose.onNodeWithTag("probe-result")
            .assertTextEquals(context.getString(R.string.probe_result, 204, 87))
        compose.onNodeWithTag("probe-receipt-note").assertIsDisplayed()
        compose.onAllNodesWithTag("probe-failed").assertCountEquals(0)
    }

    @Test fun failedProbeShowsNoReceipt() {
        show(NetworkUiState(probeUrl = "https://example.invalid", probeConsent = true, probeFailed = true))
        compose.onNodeWithTag("probe-failed").assertIsDisplayed()
        compose.onAllNodesWithTag("probe-receipt").assertCountEquals(0)
    }
}
