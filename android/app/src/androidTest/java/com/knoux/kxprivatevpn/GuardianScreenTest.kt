package com.knoux.kxprivatevpn

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.knoux.kxprivatevpn.features.GuardianScreen
import com.knoux.kxprivatevpn.features.GuardianUiState
import com.knoux.kxprivatevpn.platform.network.NetworkReading
import com.knoux.kxprivatevpn.platform.repair.RepairMeasurement
import com.knoux.kxprivatevpn.platform.vpn.VpnState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class GuardianScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun freshClientExplainsImportAndNavigatesWithoutAutoConnecting() {
        var destination: String? = null
        compose.setContent {
            GuardianScreen(
                night = true, vpn = VpnState(), hasProfile = false,
                state = GuardianUiState(
                    scanned = true,
                    network = NetworkReading(true, true, false, false, listOf("Wi-Fi"), listOf("1.1.1.1")),
                    storage = RepairMeasurement(1_000L, 500L, 0L, 0, false, null, false, 42L),
                    visibleApps = 2, measuredAtMillis = 42L,
                ),
                onBack = {}, onNavigate = { destination = it }, onScan = {},
            )
        }
        compose.onNodeWithTag("route-guardian").assertIsDisplayed()
        compose.onNodeWithTag("guardian-finding-import_profile").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("guardian-open-import_profile").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("import", destination) }
    }

    @Test fun scanRequiresUserActionAndShowsUnknownBeforeMeasurements() {
        var scans = 0
        compose.setContent {
            GuardianScreen(
                night = false, vpn = VpnState(), hasProfile = false,
                state = GuardianUiState(),
                onBack = {}, onNavigate = {}, onScan = { scans += 1 },
            )
        }
        compose.onNodeWithTag("guardian-scan").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, scans) }
        compose.onNodeWithTag("guardian-evidence-network").performScrollTo().assertIsDisplayed()
    }
}
