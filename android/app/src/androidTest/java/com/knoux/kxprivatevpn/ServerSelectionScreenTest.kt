package com.knoux.kxprivatevpn

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.data.config.Profile
import com.knoux.kxprivatevpn.data.config.ProfileSnapshot
import com.knoux.kxprivatevpn.features.ClientState
import com.knoux.kxprivatevpn.features.ServerSelectionScreen
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
        onQueryChange: (String) -> Unit = {},
        onSelect: (String) -> Unit = {},
        onRemove: (Profile) -> Unit = {},
    ) {
        compose.setContent {
            val base = InstrumentationRegistry.getInstrumentation().targetContext
            val localizedContext = if (arabic) localized(base, "ar") else base
            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedContext.resources.configuration,
                LocalLayoutDirection provides if (arabic) LayoutDirection.Rtl else LayoutDirection.Ltr,
            ) {
                Box(Modifier.fillMaxSize()) {
                    ServerSelectionScreen(
                        night = true,
                        state = ClientState(
                            loaded = true,
                            snapshot = ProfileSnapshot(profiles, selectedId),
                        ),
                        query = query,
                        onQueryChange = onQueryChange,
                        onBack = {},
                        onImport = {},
                        onSelect = onSelect,
                        onRemove = onRemove,
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
        assertEquals(berlin, removed)
    }
}