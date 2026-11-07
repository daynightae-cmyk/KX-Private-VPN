package com.knoux.kxprivatevpn

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.runtime.mutableStateOf
import com.knoux.kxprivatevpn.features.VpnPathPanel
import com.knoux.kxprivatevpn.platform.vpn.VpnPathEvidence
import com.knoux.kxprivatevpn.platform.vpn.VpnPathOutcome
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import com.knoux.kxprivatevpn.features.KxHomeScreen
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.data.config.Profile
import com.knoux.kxprivatevpn.data.config.ProfileSnapshot
import com.knoux.kxprivatevpn.features.VpnHomePanel
import com.knoux.kxprivatevpn.features.VpnSessionScreen
import com.knoux.kxprivatevpn.platform.vpn.VpnIssue
import com.knoux.kxprivatevpn.platform.vpn.VpnPhase
import com.knoux.kxprivatevpn.platform.vpn.VpnSession
import com.knoux.kxprivatevpn.platform.vpn.VpnState
import org.junit.Rule
import org.junit.Test

/** UI truthfulness: no profile means no connect claim, and CONNECTED appears only with a session. */
class VpnFlowTest {
    @get:Rule val compose = createComposeRule()

    private val profile = Profile("p1", "My tunnel", "192.0.2.1:51820")

    private fun panel(snapshot: ProfileSnapshot, vpn: VpnState) {
        compose.setContent {
            MaterialTheme {
                VpnHomePanel(
                    snapshot = snapshot,
                    busy = false,
                    vpn = vpn,
                    onConnect = {},
                    onDisconnect = {},
                    onSession = {},
                    onDismissIssue = {},
                )
            }
        }
    }

    private fun text(id: Int) =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun capture(name: String) {
        captureEvidence(InstrumentationRegistry.getInstrumentation().targetContext, compose, name)
    }

    @Test fun emptyStateShowsNoProfileAndDisablesConnect() {
        panel(ProfileSnapshot(emptyList(), null), VpnState())

        compose.onNodeWithTag("home-profile").assertTextEquals(text(R.string.no_profiles))
        compose.onNodeWithTag("vpn-connect").assertIsNotEnabled()
        compose.onNodeWithTag("vpn-connected").assertDoesNotExist()
    }

    @Test fun connectedStateNeedsARealSessionAndOffersDisconnect() {
        panel(
            ProfileSnapshot(listOf(profile), profile.id),
            VpnState(
                VpnPhase.CONNECTED,
                VpnSession(
                    profileId = profile.id,
                    profileName = profile.name,
                    endpoint = profile.endpoint,
                    startedAtMillis = 1_700_000_000_000,
                    rxBytes = 1_024,
                    txBytes = 2_048,
                ),
            ),
        )

        compose.onNodeWithTag("vpn-connected").assertIsDisplayed()
        compose.onNodeWithTag("vpn-disconnect").assertIsEnabled()
        compose.onNodeWithTag("open-session").assertIsDisplayed()
        compose.onNodeWithTag("vpn-connect").assertDoesNotExist()
    }

    @Test fun issueStateExplainsWhyNothingIsProtected() {
        panel(ProfileSnapshot(listOf(profile), profile.id), VpnState(VpnPhase.ERROR, null, VpnIssue.NO_PROFILE))

        compose.onNodeWithTag("vpn-issue")
            .assertTextEquals(text(R.string.vpn_issue_no_profile))
        compose.onNodeWithTag("vpn-connected").assertDoesNotExist()
    }

    @Test fun unresolvedStopOffersRetryOnTheActualHomeInsteadOfConnect() {
        var stops = 0
        var starts = 0
        val state = VpnState(VpnPhase.ERROR,
            VpnSession(profile.id, profile.name, profile.endpoint), VpnIssue.STOP_FAILED)
        compose.setContent {
            MaterialTheme {
                KxHomeScreen(true, ProfileSnapshot(listOf(profile), profile.id), false, state,
                    onConnect = { starts++ }, onDisconnect = { stops++ }, onSession = {},
                    onDismissIssue = {}, onNavigate = {})
            }
        }
        compose.onNodeWithTag("vpn-connect").assertDoesNotExist()
        compose.onNodeWithTag("vpn-disconnect").performScrollTo().assertIsEnabled().performClick()
        compose.onNodeWithTag("vpn-orb-action").performScrollTo().performClick()
        compose.onNodeWithTag("vpn-dismiss-issue").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(3, stops); assertEquals(0, starts) }
    }

    @Test fun failedStopCanBeRetriedFromTheSessionScreen() {
        var stops = 0
        val state = VpnState(VpnPhase.ERROR,
            VpnSession(profile.id, profile.name, profile.endpoint), VpnIssue.STOP_FAILED)
        compose.setContent {
            MaterialTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    VpnSessionScreen(state) { stops++ }
                }
            }
        }
        compose.onNodeWithTag("session-disconnect").performScrollTo().assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, stops) }
    }

    @Test fun sessionPathProofRequiresFreshApprovalAndResetsWhenOwnershipChanges() {
        var calls = 0
        val state = mutableStateOf(VpnState(VpnPhase.CONNECTED,
            VpnSession(profile.id, profile.name, profile.endpoint)))
        compose.setContent {
            MaterialTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    VpnSessionScreen(state.value, verifyPath = { url, approved ->
                        assertEquals("https://probe.example", url)
                        assertEquals(true, approved)
                        calls++
                        VpnPathEvidence(VpnPathOutcome.NO_RECENT_HANDSHAKE, httpCode = 200)
                    }) {}
                }
            }
        }
        compose.runOnIdle { assertEquals(0, calls) }
        compose.onNodeWithTag("vpn-path-url").performScrollTo().performTextInput("https://probe.example")
        compose.onNodeWithTag("vpn-path-run").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("vpn-path-approval").performScrollTo().performClick()
        compose.onNodeWithTag("vpn-path-run").performScrollTo().assertIsEnabled().performClick()
        compose.onNodeWithTag("vpn-path-result").performScrollTo()
            .assertTextEquals(text(R.string.vpn_path_no_handshake))
        compose.runOnIdle { assertEquals(1, calls) }
        compose.onNodeWithTag("vpn-path-url").performScrollTo().performTextReplacement("https://other.example")
        compose.onNodeWithTag("vpn-path-run").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("vpn-path-result").assertDoesNotExist()
        compose.runOnIdle { state.value = state.value.copy(phase = VpnPhase.ERROR, issue = VpnIssue.STOP_FAILED) }
        compose.onNodeWithTag("vpn-path-url").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("vpn-path-approval").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("session-disconnect").performScrollTo().assertIsEnabled()
    }

    @Test fun busyPathProbeDisablesInputsAndAllowsRetryAfterFailure() {
        val result = CompletableDeferred<VpnPathEvidence>()
        var calls = 0
        compose.setContent {
            MaterialTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    VpnPathPanel(enabled = true) { _, _ -> calls++; result.await() }
                }
            }
        }
        compose.onNodeWithTag("vpn-path-url").performScrollTo().performTextInput("https://probe.example")
        compose.onNodeWithTag("vpn-path-approval").performScrollTo().performClick()
        compose.onNodeWithTag("vpn-path-run").performScrollTo().performClick()
        listOf("vpn-path-url", "vpn-path-approval", "vpn-path-run").forEach {
            compose.onNodeWithTag(it).performScrollTo().assertIsNotEnabled()
        }
        compose.runOnIdle { assertEquals(1, calls); result.complete(VpnPathEvidence(VpnPathOutcome.UNAVAILABLE)) }
        compose.onNodeWithTag("vpn-path-result").performScrollTo().assertTextEquals(text(R.string.vpn_path_unavailable))
        compose.onNodeWithTag("vpn-path-run").performScrollTo().assertIsEnabled()
    }

    @Test fun sessionScreenShowsMeasuredValuesAndAbsentAndroidLink() {
        val state = VpnState(
            VpnPhase.CONNECTED,
            VpnSession(
                profileId = profile.id,
                profileName = profile.name,
                endpoint = profile.endpoint,
                startedAtMillis = 1_700_000_000_000,
                rxBytes = 4_096,
                txBytes = 2_048,
                dnsServers = listOf("192.0.2.53"),
                addresses = listOf("10.7.0.2/32"),
                allowedIps = listOf("0.0.0.0/0"),
                mtu = 1420,
                backendVersion = "1.0.20260102",
            ),
        )
        compose.setContent {
            MaterialTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    VpnSessionScreen(state) {}
                }
            }
        }

        compose.onNodeWithTag("session-profile").assertTextEquals(profile.name)
        compose.onNodeWithTag("session-traffic").assertIsDisplayed()
        // The emulator has no VPN network, so the real ConnectivityManager read must say so.
        compose.onNodeWithTag("session-link-absent").assertExists()
        compose.onNodeWithTag("session-link-present").assertDoesNotExist()
        capture("11-vpn-session-connected")
    }
}
