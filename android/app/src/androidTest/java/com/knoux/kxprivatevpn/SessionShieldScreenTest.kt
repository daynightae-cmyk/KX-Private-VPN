package com.knoux.kxprivatevpn

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.data.config.ProfileStore
import com.knoux.kxprivatevpn.features.SessionShieldScreen
import com.knoux.kxprivatevpn.features.SessionShieldUiState
import com.knoux.kxprivatevpn.features.SessionShieldViewModel
import com.knoux.kxprivatevpn.platform.security.ShieldAction
import com.knoux.kxprivatevpn.platform.security.ShieldEntry
import com.knoux.kxprivatevpn.platform.security.ShieldInventory
import com.knoux.kxprivatevpn.platform.security.ShieldKind
import com.knoux.kxprivatevpn.platform.security.ShieldReceipt
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Base64
import java.util.Locale

/**
 * The session shield lists what KX itself keeps. It must never show a credential, it must ask
 * before it removes anything, and it must be able to prove that the credential is really gone
 * after a revoke.
 */
class SessionShieldScreenTest {
    @get:Rule val compose = createComposeRule()

    private val base: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val context: Context
        get() = localized(base, "en")

    private fun localized(base: Context, language: String): Context {
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(language))
        return base.createConfigurationContext(configuration)
    }

    private fun profileEntry(name: String = "Home") = ShieldEntry(
        id = "profile:abc",
        kind = ShieldKind.PROFILE,
        title = name,
        detail = "vpn.example.invalid:51820",
        atMillis = 0,
        holdsSecret = true,
        selected = true,
    )

    private fun screen(
        state: SessionShieldUiState,
        onRequestRevoke: (String) -> Unit = {},
        onRequestErase: () -> Unit = {},
        onDismissConfirmation: () -> Unit = {},
        onConfirmRevoke: () -> Unit = {},
        onConfirmErase: () -> Unit = {},
        onPrune: () -> Unit = {},
    ) {
        compose.setContent {
            val english = localized(base, "en")
            CompositionLocalProvider(
                LocalContext provides english,
                LocalConfiguration provides english.resources.configuration,
                LocalLayoutDirection provides LayoutDirection.Ltr,
            ) {
                Box(Modifier.fillMaxSize()) {
                    SessionShieldScreen(
                        night = false,
                        state = state,
                        onBack = {},
                        onRefresh = {},
                        onToggle = {},
                        onSelectAll = {},
                        onClearSelection = {},
                        onRequestRevoke = onRequestRevoke,
                        onRequestErase = onRequestErase,
                        onDismissConfirmation = onDismissConfirmation,
                        onConfirmRevoke = onConfirmRevoke,
                        onConfirmErase = onConfirmErase,
                        onPrune = onPrune,
                        onOpenVpnSettings = {},
                        onOpenAppInfo = {},
                    )
                }
            }
        }
    }

    @Test fun theShieldStatesWhatItOwnsAndNeverShowsACredential() {
        screen(
            SessionShieldUiState(
                loading = false,
                inventory = ShieldInventory(
                    entries = listOf(
                        profileEntry(),
                        ShieldEntry("repair:1", ShieldKind.REPAIR_HISTORY, "cache", "clean · complete · 3",
                            1_760_000_000_000L),
                    ),
                    loadedAtMillis = 1_760_000_000_000L,
                ),
            ),
        )
        compose.onNodeWithTag("shield-profile-count").assertTextContains("1", substring = true)
        compose.onNodeWithTag("shield-history-count").assertTextContains("1", substring = true)
        compose.onNodeWithTag("shield-secret-count").assertTextContains("1", substring = true)
        compose.onNodeWithTag("shield-title-0").assertTextEquals("Home")
        compose.onNodeWithTag("shield-detail-0").assertTextEquals("vpn.example.invalid:51820")
        // The credential itself is never rendered, only the fact that one is held.
        compose.onNodeWithTag("shield-secret-0").assertExists()
        compose.onAllNodes(hasText("PrivateKey", substring = true)).assertCountEquals(0)
        compose.onNodeWithTag("shield-selected-0").assertExists()
    }

    @Test fun anEmptyShieldIsAnEmptyResult() {
        screen(SessionShieldUiState(loading = false, inventory = ShieldInventory()))
        compose.onNodeWithTag("shield-profile-count").assertTextContains("0", substring = true)
        compose.onNodeWithTag("shield-history-count").assertTextContains("0", substring = true)
        compose.onNodeWithTag("shield-scope").assertExists()
    }

    @Test fun revokingAProfileAsksFirstAndTheRequestItselfRemovesNothing() {
        var requested = ""
        var confirmed = 0
        screen(
            SessionShieldUiState(
                loading = false,
                inventory = ShieldInventory(entries = listOf(profileEntry())),
            ),
            onRequestRevoke = { requested = it },
            onConfirmRevoke = { confirmed++ },
        )
        compose.onNodeWithTag("shield-revoke-0").performScrollTo().performClick()
        assertEquals("abc", requested)
        assertEquals("asking for a revoke must not revoke", 0, confirmed)
    }

    @Test fun theConfirmationDialogSeparatesTheApprovalFromTheRequest() {
        var confirmed = 0
        var dismissed = 0
        screen(
            SessionShieldUiState(
                loading = false,
                inventory = ShieldInventory(entries = listOf(profileEntry())),
                confirming = ShieldAction.REVOKE_PROFILE,
                pendingProfileId = "abc",
            ),
            onConfirmRevoke = { confirmed++ },
            onDismissConfirmation = { dismissed++ },
        )
        compose.onNodeWithTag("shield-revoke-confirm").assertExists()
        compose.onNodeWithTag("shield-revoke-cancel").assertExists()
        compose.onNodeWithTag("shield-revoke-cancel").performClick()
        assertEquals(0, confirmed)
        assertEquals(1, dismissed)
    }

    @Test fun erasingTheHistoryAlsoAsksFirst() {
        var requested = 0
        var confirmed = 0
        screen(
            SessionShieldUiState(
                loading = false,
                inventory = ShieldInventory(entries = listOf(profileEntry())),
            ),
            onRequestErase = { requested++ },
            onConfirmErase = { confirmed++ },
        )
        compose.onNodeWithTag("shield-erase").performScrollTo().performClick()
        assertEquals(1, requested)
        assertEquals(0, confirmed)
    }

    @Test fun theReceiptOnlyReportsWhatHappened() {
        screen(
            SessionShieldUiState(
                loading = false,
                receipt = ShieldReceipt(
                    action = ShieldAction.REVOKE_PROFILE,
                    removed = 1,
                    pruned = 0,
                    bytesFreed = 0L,
                    atMillis = 1_760_000_000_000L,
                    detail = "secret_removed",
                ),
            ),
        )
        compose.onNodeWithTag("shield-receipt").assertExists()
        compose.onNodeWithTag("shield-receipt-detail").assertTextEquals("secret_removed")
        compose.onNodeWithTag("shield-receipt-counts").assertTextContains("1", substring = true)
    }

    @Test fun theGuidancePointsAtTheSystemScreensAndTheScopeStaysKX() {
        screen(SessionShieldUiState(loading = false))
        compose.onNodeWithTag("shield-vpn-settings").assertExists()
        compose.onNodeWithTag("shield-app-info").assertExists()
        compose.onNodeWithTag("shield-guidance").assertExists()
        compose.onNodeWithTag("shield-scope").assertExists()
        compose.onNodeWithTag("shield-prune").assertExists()
    }

    /**
     * The real thing: a profile with a private key is revoked, and the encrypted store is then
     * asked again. The secret has to be gone, not merely hidden from the list.
     */
    @Test fun revokingAProfileRemovesTheSecretFromTheEncryptedStore() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val store = ProfileStore(app)
        val key = Base64.getEncoder().encodeToString(ByteArray(32) { (it + 1).toByte() })
        val config = "[Interface]\nPrivateKey = $key\nAddress = 10.7.0.2/32\n" +
            "[Peer]\nPublicKey = $key\nEndpoint = 192.0.2.1:51820\nAllowedIPs = 0.0.0.0/0"
        val snapshot = runBlocking { store.import("shield-revoke-test", config.toByteArray()) }
        val profile = snapshot.profiles.first { it.name == "shield-revoke-test" }
        assertNotNull(runBlocking { store.config(profile.id) })

        val model = SessionShieldViewModel(app)
        val deadline = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < deadline &&
            model.state.value.inventory.profiles.none { it.id == "profile:${profile.id}" }
        ) {
            Thread.sleep(100)
        }
        assertTrue(
            "the shield must list the profile it owns",
            model.state.value.inventory.profiles.any { it.id == "profile:${profile.id}" },
        )
        assertEquals(1, model.state.value.inventory.secretCount)

        model.requestRevoke(profile.id)
        model.confirmRevoke()
        val after = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < after &&
            model.state.value.receipt?.detail != "secret_removed"
        ) {
            Thread.sleep(100)
        }
        assertEquals("secret_removed", model.state.value.receipt?.detail)
        assertEquals(1, model.state.value.receipt?.removed)

        val remaining = runBlocking { store.load() }
        assertTrue(
            "the revoked profile must be gone from the encrypted store",
            remaining.profiles.none { it.id == profile.id },
        )
        assertNull("the revoked secret must not be readable", runBlocking { store.config(profile.id) })
    }
}
