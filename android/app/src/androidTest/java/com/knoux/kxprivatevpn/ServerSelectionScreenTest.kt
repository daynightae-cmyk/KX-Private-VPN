package com.knoux.kxprivatevpn

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.data.config.Profile
import com.knoux.kxprivatevpn.data.config.ProfileSnapshot
import com.knoux.kxprivatevpn.features.ClientState
import com.knoux.kxprivatevpn.features.ServerSelectionScreen
import com.knoux.kxprivatevpn.platform.vpn.VpnIssue
import com.knoux.kxprivatevpn.platform.vpn.VpnPhase
import com.knoux.kxprivatevpn.platform.vpn.VpnSession
import com.knoux.kxprivatevpn.platform.vpn.VpnState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.Locale

/** Only imported endpoints are listed; no fake country, load or latency is rendered. */
class ServerSelectionScreenTest {
    @get:Rule val compose = createComposeRule()
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val berlin = Profile("p1", "Berlin", "198.51.100.1:51820")
    private val tokyo = Profile("p2", "Tokyo", "203.0.113.9:51820")

    private fun localized(base: Context, language: String): Context {
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(language))
        return base.createConfigurationContext(configuration)
    }

    private fun screen(
        profiles: List<Profile>,
        selectedId: String? = profiles.firstOrNull()?.id,
        arabic: Boolean = false,
        query: String = "",
        busy: Boolean = false,
        night: Boolean = true,
        widthDp: Int = 411,
        fontScale: Float = 1f,
        vpn: VpnState = VpnState(),
        onQueryChange: (String) -> Unit = {},
        onSelect: (String) -> Unit = {},
        onRemove: (Profile) -> Unit = {},
        onRetryDisconnect: () -> Unit = {},
    ) {
        compose.setContent {
            val base = InstrumentationRegistry.getInstrumentation().targetContext
            val localizedContext = localized(base, if (arabic) "ar" else "en")
            val nativeDensity = LocalDensity.current
            BoxWithConstraints(Modifier.fillMaxSize()) {
                // Scaled layout fixture only: never change the emulator or owner's settings.
                val density = maxWidth.value * nativeDensity.density / widthDp
                CompositionLocalProvider(
                    LocalContext provides localizedContext,
                    LocalConfiguration provides localizedContext.resources.configuration,
                    LocalLayoutDirection provides if (arabic) LayoutDirection.Rtl else LayoutDirection.Ltr,
                    LocalDensity provides Density(density, fontScale),
                ) {
                    ServerSelectionScreen(
                        night = night,
                        state = ClientState(
                            loaded = true,
                            busy = busy,
                            snapshot = ProfileSnapshot(profiles, selectedId),
                        ),
                        vpn = vpn,
                        query = query,
                        onQueryChange = onQueryChange,
                        onBack = {},
                        onImport = {},
                        onSelect = onSelect,
                        onRemove = { id -> onRemove(profiles.first { it.id == id }) },
                        onRetryDisconnect = onRetryDisconnect,
                    )
                }
            }
        }
    }

    private fun capture(name: String) {
        captureEvidence(context, compose, name)
    }

    @Test fun importedEndpointsRenderAndSelect() {
        var selected: String? = null
        screen(listOf(berlin, tokyo), onSelect = { selected = it })

        compose.onNodeWithText("Berlin").assertExists()
        compose.onNodeWithText("Tokyo").assertExists()
        compose.onNodeWithText(berlin.endpoint).assertExists()
        // Every card, not just one, has to admit that latency was never measured.
        compose.onAllNodesWithText(context.getString(R.string.server_latency_unmeasured))
            .assertCountEquals(2)
        compose.onNodeWithTag("server-card-p2").performClick()
        assertEquals("p2", selected)
        // The selected pill is a child of the clickable card, so it only exists in the
        // unmerged tree.
        compose.onNodeWithTag("server-selected", useUnmergedTree = true).assertExists()
        capture("73-servers-imported")
    }

    @Test fun searchFiltersRealProfiles() {
        val query = mutableStateOf("")
        compose.setContent {
            ServerSelectionScreen(
                night = true,
                state = ClientState(
                    loaded = true,
                    snapshot = ProfileSnapshot(listOf(berlin, tokyo), berlin.id),
                ),
                query = query.value,
                onQueryChange = { query.value = it },
                onBack = {},
                onImport = {},
                onSelect = {},
                onRemove = {},
            )
        }
        compose.onNodeWithTag("server-search").performTextInput("ber")
        compose.waitForIdle()
        compose.onNodeWithText("Berlin").assertExists()
        compose.onNodeWithText("Tokyo").assertDoesNotExist()
    }

    @Test fun searchTrimsWhitespaceAndMatchesNamesIgnoringCase() {
        val query = "  bErLiN  "
        screen(listOf(berlin, tokyo), query = query)

        compose.onNodeWithTag("server-search").assertTextEquals(query)
        compose.onNodeWithTag("server-card-p1").assertExists()
        compose.onNodeWithTag("server-card-p2").assertDoesNotExist()
    }

    @Test fun searchTrimsWhitespaceAndMatchesEndpointsIgnoringCase() {
        val office = Profile("p3", "Office", "vpn.example.test:51820")
        val query = "  VPN.ExAmPlE.TEST  "
        screen(listOf(berlin, office), query = query)

        compose.onNodeWithTag("server-search").assertTextEquals(query)
        compose.onNodeWithTag("server-card-p1").assertDoesNotExist()
        compose.onNodeWithTag("server-card-p3").assertExists()
    }

    @Test fun whitespaceOnlySearchShowsAllProfiles() {
        val query = "   "
        screen(listOf(berlin, tokyo), query = query)

        compose.onNodeWithTag("server-search").assertTextEquals(query)
        compose.onNodeWithTag("server-card-p1").assertExists()
        compose.onNodeWithTag("server-card-p2").assertExists()
        compose.onNodeWithTag("server-search-empty").assertDoesNotExist()
    }

    @Test fun everyCardExposesRadioButtonRoleAndSelectedState() {
        screen(listOf(berlin, tokyo), selectedId = tokyo.id)

        listOf(berlin, tokyo).forEach { profile ->
            compose.onNodeWithTag("server-card-${profile.id}")
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
                .assert(SemanticsMatcher.expectValue(
                    SemanticsProperties.Selected, profile.id == tokyo.id,
                ))
                .assert(hasAnyAncestor(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup)))
        }
    }

    @Test fun busyPreventsCardSelectionExplicitSelectionAndRemoval() {
        val selections = mutableListOf<String>()
        val removals = mutableListOf<Profile>()
        screen(
            listOf(berlin, tokyo),
            busy = true,
            onSelect = { selections.add(it) },
            onRemove = { removals.add(it) },
        )

        // Touch input exercises disabled hit handling rather than invoking semantics callbacks.
        listOf(berlin, tokyo).forEach { profile ->
            compose.onNodeWithTag("server-card-${profile.id}")
                .assertIsNotEnabled()
                .performScrollTo()
                .performTouchInput { click() }
            compose.onNodeWithTag("server-remove-${profile.id}")
                .assertIsNotEnabled()
                .performScrollTo()
                .performTouchInput { click() }
        }
        compose.onNodeWithTag("server-select-p2")
            .assertIsNotEnabled()
            .performScrollTo()
            .performTouchInput { click() }
        compose.runOnIdle {
            assertEquals(emptyList<String>(), selections)
            assertEquals(emptyList<Profile>(), removals)
        }
    }

    @Test fun removeActionDoesNotSelectProfile() {
        val selections = mutableListOf<String>()
        val removals = mutableListOf<Profile>()
        screen(
            listOf(berlin, tokyo),
            onSelect = { selections.add(it) },
            onRemove = { removals.add(it) },
        )

        compose.onNodeWithTag("server-remove-p2")
            .performScrollTo()
            .assertHeightIsAtLeast(48.dp)
            .performTouchInput { click() }
        compose.runOnIdle {
            assertEquals("opening the removal dialog must not remove yet", emptyList<Profile>(), removals)
            assertEquals(emptyList<String>(), selections)
        }
        compose.onNodeWithTag("server-remove-confirm").assertExists().performClick()
        compose.runOnIdle {
            assertEquals(listOf(tokyo), removals)
            assertEquals(emptyList<String>(), selections)
        }
    }

    @Test fun dayArabicLargeTextKeepsSelectedStateAndIndependentRemoval() {
        var selected: String? = null
        var removed: Profile? = null
        screen(listOf(berlin, tokyo), selectedId = tokyo.id, arabic = true,
            night = false, widthDp = 360, fontScale = 1.6f,
            onSelect = { selected = it }, onRemove = { removed = it })
        compose.onNodeWithTag("server-card-p2").assertIsSelected()
        compose.onNodeWithTag("server-remove-p2").performScrollTo().performTouchInput { click() }
        compose.onNodeWithTag("server-remove-confirm").assertExists().performClick()
        compose.runOnIdle {
            assertEquals(tokyo, removed)
            assertEquals(null, selected)
        }
        compose.onNodeWithTag("server-card-p2").performScrollTo()
        capture("servers-layout-fixture-360-day-ar-1.6-selected")
    }

    @Test fun emptyStateIsHonestAndArabicRtl() {
        val ar = localized(context, "ar")
        screen(emptyList(), selectedId = null, arabic = true)
        compose.onNodeWithText(ar.getString(R.string.server_no_profiles)).assertExists()
        compose.onNodeWithText(ar.getString(R.string.server_no_profiles_body)).assertExists()
        compose.onNodeWithTag("server-import").assertExists()
        capture("74-servers-empty-ar")
    }

    @Test fun removeUsesARealProfile() {
        var removed: Profile? = null
        screen(listOf(berlin), onRemove = { removed = it })
        compose.onNodeWithTag("server-remove-p1").performClick()
        compose.onNodeWithTag("server-remove-confirm").assertExists().performClick()
        assertEquals(berlin, removed)
    }

    private fun tunnelTo(
        profile: Profile,
        phase: VpnPhase = VpnPhase.CONNECTED,
        issue: VpnIssue? = null,
    ) = VpnState(
        phase = phase,
        session = VpnSession(profile.id, profile.name, profile.endpoint),
        issue = issue,
    )

    @Test fun connectedTunnelShowsActiveServerAndConfirmsSwitch() {
        val selections = mutableListOf<String>()
        screen(
            listOf(berlin, tokyo), selectedId = berlin.id,
            vpn = tunnelTo(berlin), onSelect = { selections.add(it) },
        )

        compose.onNodeWithText(context.getString(R.string.server_active_tunnel, berlin.name))
            .assertExists()
        compose.onNodeWithTag("server-active", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("server-selected", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("server-select-p1").assertDoesNotExist()

        compose.onNodeWithTag("server-select-p2").performScrollTo()
            .performTouchInput { click() }
        compose.onNodeWithText(context.getString(R.string.server_switch_title)).assertExists()
        compose.onNodeWithText(
            context.getString(R.string.server_switch_body, berlin.name, tokyo.name),
        ).assertExists()
        // The saved server must not change before the customer confirms the disconnect.
        compose.runOnIdle { assertEquals(emptyList<String>(), selections) }
        compose.onNodeWithTag("server-switch-cancel").performClick()
        compose.runOnIdle { assertEquals(emptyList<String>(), selections) }

        compose.onNodeWithTag("server-select-p2").performScrollTo()
            .performTouchInput { click() }
        compose.onNodeWithTag("server-switch-confirm").performClick()
        compose.runOnIdle { assertEquals(listOf(tokyo.id), selections) }
        capture("75-servers-connected-active")
    }

    @Test fun connectingTunnelLocksSelectionAndExplainsWhy() {
        val selections = mutableListOf<String>()
        screen(
            listOf(berlin, tokyo), vpn = tunnelTo(berlin, VpnPhase.CONNECTING),
            onSelect = { selections.add(it) },
        )

        compose.onNodeWithText(context.getString(R.string.server_selection_locked)).assertExists()
        compose.onNodeWithText(
            context.getString(R.string.server_connecting_locked, berlin.name),
        ).assertExists()
        listOf(berlin, tokyo).forEach { profile ->
            compose.onNodeWithTag("server-card-${profile.id}").assertIsNotEnabled()
            compose.onNodeWithTag("server-remove-${profile.id}").assertIsNotEnabled()
        }
        compose.onNodeWithTag("server-card-p2").performScrollTo()
            .performTouchInput { click() }
        compose.runOnIdle { assertEquals(emptyList<String>(), selections) }
    }

    @Test fun unresolvedStopLocksSelectionAndOffersRetry() {
        var retries = 0
        val selections = mutableListOf<String>()
        screen(
            listOf(berlin, tokyo),
            vpn = tunnelTo(berlin, VpnPhase.ERROR, VpnIssue.STOP_FAILED),
            onSelect = { selections.add(it) },
            onRetryDisconnect = { retries++ },
        )

        compose.onNodeWithText(context.getString(R.string.server_stop_failed_locked)).assertExists()
        compose.onNodeWithTag("server-card-p2").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("server-retry-disconnect").performScrollTo()
            .assertIsEnabled().performClick()
        compose.runOnIdle {
            assertEquals(1, retries)
            assertEquals(emptyList<String>(), selections)
        }
    }

    @Test fun removingActiveProfileWarnsThatTunnelStopsFirst() {
        var removed: Profile? = null
        screen(listOf(berlin, tokyo), vpn = tunnelTo(berlin),
            onRemove = { removed = it })

        compose.onNodeWithTag("server-remove-p1").performScrollTo().performTouchInput { click() }
        compose.onNodeWithText(context.getString(R.string.remove_active_warning), substring = true)
            .assertExists()
        compose.onNodeWithTag("server-remove-confirm").performClick()
        compose.runOnIdle { assertEquals(berlin, removed) }
    }

    @Test fun removingInactiveProfileDoesNotWarnAboutTheTunnel() {
        screen(listOf(berlin, tokyo), vpn = tunnelTo(berlin))

        compose.onNodeWithTag("server-remove-p2").performScrollTo().performTouchInput { click() }
        compose.onNodeWithText(context.getString(R.string.remove_active_warning), substring = true)
            .assertDoesNotExist()
        compose.onNodeWithTag("server-remove-cancel").performClick()
    }
}