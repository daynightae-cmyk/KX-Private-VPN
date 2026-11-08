package com.knoux.kxprivatevpn

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
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
import com.knoux.kxprivatevpn.platform.security.ShieldReceiptCode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.Base64
import java.util.Locale

/**
 * The session shield lists what KX itself keeps. It must never show a credential, it must ask
 * before it removes anything, and it must be able to prove that the credential is really gone
 * after a revoke. Selection is only meaningful for profiles that can really be revoked.
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

    private fun profileEntry(name: String = "Home", id: String = "abc") = ShieldEntry(
        id = "profile:$id",
        kind = ShieldKind.PROFILE,
        title = name,
        detail = "vpn.example.invalid:51820",
        atMillis = 0,
        holdsSecret = true,
        selected = id == "abc",
    )

    private fun repairEntry() = ShieldEntry(
        id = "repair:1",
        kind = ShieldKind.REPAIR_HISTORY,
        title = "cache",
        detail = "cache_remove · complete · 3",
        atMillis = 1_760_000_000_000L,
        serviceKey = "cache",
        actionKey = "cache_remove",
        outcomeKey = "COMPLETE",
        items = 3,
        bytes = 1024,
    )

    private fun usageEntry() = ShieldEntry(
        id = "usage:1",
        kind = ShieldKind.USAGE_HISTORY,
        title = "Home",
        detail = "vpn.example.invalid:51820",
        atMillis = 1_760_000_000_000L,
        bytes = 2048,
        durationMillis = 65_000,
        recovered = true,
    )

    private fun hintEntry() = ShieldEntry(
        id = "hint:vpn-active",
        kind = ShieldKind.SESSION_HINT,
        title = "vpn_session_active",
        detail = "advisory_hint_only",
        atMillis = 0,
    )

    private fun screen(
        state: SessionShieldUiState,
        language: String = "en",
        direction: LayoutDirection = LayoutDirection.Ltr,
        night: Boolean = false,
        widthDp: Int = 411,
        fontScale: Float = 1f,
        onRequestRevoke: (String) -> Unit = {},
        onRequestRevokeSelected: () -> Unit = {},
        onRequestErase: () -> Unit = {},
        onDismissConfirmation: () -> Unit = {},
        onConfirmRevoke: () -> Unit = {},
        onConfirmRevokeSelected: () -> Unit = {},
        onConfirmErase: () -> Unit = {},
        onPrune: () -> Unit = {},
    ) {
        compose.setContent {
            val localizedContext = localized(base, language)
            val nativeDensity = LocalDensity.current
            BoxWithConstraints(Modifier.fillMaxSize()) {
                // Scaled layout fixture only: never change the emulator or owner's settings.
                val density = maxWidth.value * nativeDensity.density / widthDp
                CompositionLocalProvider(
                    LocalContext provides localizedContext,
                    LocalConfiguration provides localizedContext.resources.configuration,
                    LocalLayoutDirection provides direction,
                    LocalDensity provides Density(density, fontScale),
                ) {
                    SessionShieldScreen(
                        night = night,
                        state = state,
                        onBack = {},
                        onRefresh = {},
                        onToggle = {},
                        onSelectAll = {},
                        onClearSelection = {},
                        onRequestRevoke = onRequestRevoke,
                        onRequestRevokeSelected = onRequestRevokeSelected,
                        onRequestErase = onRequestErase,
                        onDismissConfirmation = onDismissConfirmation,
                        onConfirmRevoke = onConfirmRevoke,
                        onConfirmRevokeSelected = onConfirmRevokeSelected,
                        onConfirmErase = onConfirmErase,
                        onPrune = onPrune,
                        onOpenVpnSettings = {},
                        onOpenAppInfo = {},
                    )
                }
            }
        }
    }

    private fun populated(
        selected: Set<String> = emptySet(),
        receipt: ShieldReceipt? = null,
    ) = SessionShieldUiState(
        loading = false,
        inventory = ShieldInventory(
            entries = listOf(
                profileEntry(),
                profileEntry(name = "Work", id = "def"),
                repairEntry(),
                usageEntry(),
                hintEntry(),
            ),
            loadedAtMillis = 1_760_000_000_000L,
        ),
        selected = selected,
        receipt = receipt,
    )

    private fun capture(name: String) = captureEvidence(context, compose, name)

    /** A dialog is its own window; UiAutomation captures the whole screen including dialogs. */
    private fun captureDialog(name: String) {
        val folder = File(context.getExternalFilesDir(null), "identity-evidence").apply { mkdirs() }
        // Let the dialog window finish its first frame before the screenshot is composited.
        compose.waitForIdle()
        Thread.sleep(500)
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(folder, "$name.png").outputStream().use { stream ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
        }
    }

    @Test fun theShieldStatesWhatItOwnsAndNeverShowsACredential() {
        screen(
            SessionShieldUiState(
                loading = false,
                inventory = ShieldInventory(
                    entries = listOf(profileEntry(), repairEntry()),
                    loadedAtMillis = 1_760_000_000_000L,
                ),
            ),
        )
        compose.onNodeWithTag("shield-profile-count").assertTextContains("1", substring = true)
        compose.onNodeWithTag("shield-history-count").assertTextContains("1", substring = true)
        compose.onNodeWithTag("shield-secret-count").assertTextContains("1", substring = true)
        // The selectable row merges its children, so the inner labels live in the unmerged tree.
        compose.onNodeWithTag("shield-title-0", useUnmergedTree = true).assertTextEquals("Home")
        compose.onNodeWithTag("shield-detail-0", useUnmergedTree = true)
            .assertTextEquals("vpn.example.invalid:51820")
        // The credential itself is never rendered, only the fact that one is held.
        compose.onNodeWithTag("shield-secret-0", useUnmergedTree = true).assertExists()
        compose.onAllNodes(hasText("PrivateKey", substring = true)).assertCountEquals(0)
        compose.onNodeWithTag("shield-selected-0", useUnmergedTree = true).assertExists()
    }

    @Test fun anEmptyShieldIsAnEmptyResult() {
        screen(SessionShieldUiState(loading = false, inventory = ShieldInventory()))
        compose.onNodeWithTag("shield-profile-count").assertTextContains("0", substring = true)
        compose.onNodeWithTag("shield-history-count").assertTextContains("0", substring = true)
        compose.onNodeWithTag("shield-empty").assertExists()
        compose.onNodeWithTag("shield-scope").assertExists()
    }

    @Test fun loadingEmptyAndFailureAreDistinguishable() {
        screen(SessionShieldUiState(loading = true))
        compose.onNodeWithTag("shield-loading").assertExists()
        compose.onNodeWithTag("shield-empty").assertDoesNotExist()
        compose.onNodeWithTag("shield-failed").assertDoesNotExist()
    }

    @Test fun aFailedInventoryOffersARetryInsteadOfLookingEmpty() {
        screen(SessionShieldUiState(loading = false, inventory = ShieldInventory(failed = true)))
        compose.onNodeWithTag("shield-failed").assertExists()
        compose.onNodeWithTag("shield-retry").assertExists()
        compose.onNodeWithTag("shield-empty").assertDoesNotExist()
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

    @Test fun selectionCountsOnlyProfilesAndBulkRevokeAsksFirst() {
        var requested = 0
        var confirmed = 0
        screen(
            SessionShieldUiState(
                loading = false,
                inventory = ShieldInventory(
                    entries = listOf(profileEntry(), profileEntry(name = "Work", id = "def")),
                ),
                // A stale history id must never inflate the profile selection count.
                selected = setOf("profile:abc", "repair:1"),
            ),
            onRequestRevokeSelected = { requested++ },
            onConfirmRevokeSelected = { confirmed++ },
        )
        compose.onNodeWithTag("shield-selected-count").assertTextContains("1", substring = true)
        compose.onNodeWithTag("shield-revoke-selected").performScrollTo().performClick()
        assertEquals(1, requested)
        assertEquals("asking for a bulk revoke must not revoke", 0, confirmed)
    }

    @Test fun theBulkConfirmationDialogSeparatesApprovalFromTheRequest() {
        var confirmed = 0
        var dismissed = 0
        screen(
            SessionShieldUiState(
                loading = false,
                inventory = ShieldInventory(
                    entries = listOf(profileEntry(), profileEntry(name = "Work", id = "def")),
                ),
                selected = setOf("profile:abc", "profile:def"),
                confirming = ShieldAction.REVOKE_SELECTED,
            ),
            onConfirmRevokeSelected = { confirmed++ },
            onDismissConfirmation = { dismissed++ },
        )
        compose.onNodeWithTag("shield-revoke-selected-confirm").assertExists()
        compose.onNodeWithTag("shield-revoke-selected-cancel").performClick()
        assertEquals(0, confirmed)
        assertEquals(1, dismissed)
    }

    @Test fun erasingTheHistoryAlsoAsksFirst() {
        var requested = 0
        var confirmed = 0
        screen(
            SessionShieldUiState(
                loading = false,
                inventory = ShieldInventory(entries = listOf(profileEntry(), repairEntry())),
            ),
            onRequestErase = { requested++ },
            onConfirmErase = { confirmed++ },
        )
        compose.onNodeWithTag("shield-erase").performScrollTo().performClick()
        assertEquals(1, requested)
        assertEquals(0, confirmed)
    }

    @Test fun theReceiptOnlyReportsWhatHappenedInCustomerLanguage() {
        screen(
            SessionShieldUiState(
                loading = false,
                receipt = ShieldReceipt(
                    action = ShieldAction.REVOKE_PROFILE,
                    removed = 1,
                    pruned = 0,
                    bytesFreed = 0L,
                    atMillis = 1_760_000_000_000L,
                    code = ShieldReceiptCode.SECRET_REMOVED,
                ),
            ),
        )
        compose.onNodeWithTag("shield-receipt").assertExists()
        compose.onNodeWithTag("shield-receipt-detail").assertTextEquals(
            "The profile and its private key are gone from the encrypted store.",
        )
        compose.onNodeWithTag("shield-receipt-counts").assertTextContains("1", substring = true)
        compose.onAllNodes(hasText("secret_removed", substring = true)).assertCountEquals(0)
    }

    @Test fun historyUsesCustomerLabelsInsteadOfStorageKeys() {
        screen(
            SessionShieldUiState(
                loading = false,
                inventory = ShieldInventory(
                    entries = listOf(repairEntry(), usageEntry(), hintEntry()),
                ),
            ),
        )
        compose.onNodeWithTag("shield-history-0").assertTextContains("Cache cleanup", substring = true)
        compose.onNodeWithTag("shield-history-0").assertTextContains("Complete", substring = true)
        compose.onNodeWithTag("shield-history-0").assertTextContains("3 items", substring = true)
        compose.onNodeWithTag("shield-history-1").assertTextContains("vpn.example.invalid:51820", substring = true)
        compose.onNodeWithTag("shield-history-1").assertTextContains("Recovered after the process stopped", substring = true)
        compose.onNodeWithTag("shield-history-2").assertTextContains("Session hint", substring = true)
        compose.onNodeWithTag("shield-history-2").assertTextContains("not a live tunnel", substring = true)
        compose.onAllNodes(hasText("cache_remove", substring = true)).assertCountEquals(0)
        compose.onAllNodes(hasText("COMPLETE", substring = true)).assertCountEquals(0)
        compose.onAllNodes(hasText("vpn_session_active", substring = true)).assertCountEquals(0)
        compose.onAllNodes(hasText("advisory_hint_only", substring = true)).assertCountEquals(0)
    }

    @Test fun aSingleHistoryEntryUsesSingularCopy() {
        screen(
            SessionShieldUiState(
                loading = false,
                inventory = ShieldInventory(entries = listOf(repairEntry().copy(items = 1))),
            ),
        )
        compose.onNodeWithTag("shield-history-count").assertTextEquals("1 history entry")
        compose.onNodeWithTag("shield-history-0").assertTextContains("1 item", substring = true)
        compose.onAllNodes(hasText("1 items", substring = true)).assertCountEquals(0)
    }

    @Test fun arabicHistoryAndReceiptUseArabicCopy() {
        screen(
            SessionShieldUiState(
                loading = false,
                inventory = ShieldInventory(entries = listOf(repairEntry())),
                receipt = ShieldReceipt(
                    action = ShieldAction.PRUNE_OLD,
                    removed = 0,
                    pruned = 2,
                    bytesFreed = 0L,
                    atMillis = 1_760_000_000_000L,
                    code = ShieldReceiptCode.PRUNED,
                ),
            ),
            language = "ar",
            direction = LayoutDirection.Rtl,
        )
        compose.onNodeWithText("خصوصية الجلسات").assertExists()
        compose.onNodeWithTag("shield-history-0").assertTextContains("تنظيف الكاش", substring = true)
        compose.onNodeWithTag("shield-history-0").assertTextContains("مكتملة", substring = true)
        compose.onNodeWithTag("shield-receipt-detail").assertTextEquals(
            "أزالت قاعدة الاحتفاظ السجل المنتهي.",
        )
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
            model.state.value.receipt?.code != ShieldReceiptCode.SECRET_REMOVED
        ) {
            Thread.sleep(100)
        }
        assertEquals(ShieldReceiptCode.SECRET_REMOVED, model.state.value.receipt?.code)
        assertEquals(1, model.state.value.receipt?.removed)

        val remaining = runBlocking { store.load() }
        assertTrue(
            "the revoked profile must be gone from the encrypted store",
            remaining.profiles.none { it.id == profile.id },
        )
        assertNull("the revoked secret must not be readable", runBlocking { store.config(profile.id) })
    }

    /**
     * The real bulk path: two selected profiles are revoked together, the store is read back,
     * and the receipt reports exactly two verified removals.
     */
    @Test fun bulkRevokeRemovesOnlyTheSelectedProfilesAndVerifiesTheStore() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val store = ProfileStore(app)
        val key = Base64.getEncoder().encodeToString(ByteArray(32) { (it + 7).toByte() })
        val config = "[Interface]\nPrivateKey = $key\nAddress = 10.7.0.3/32\n" +
            "[Peer]\nPublicKey = $key\nEndpoint = 192.0.2.2:51820\nAllowedIPs = 0.0.0.0/0"
        val first = runBlocking { store.import("shield-bulk-a", config.toByteArray()) }
            .profiles.first { it.name == "shield-bulk-a" }
        val second = runBlocking { store.import("shield-bulk-b", config.toByteArray()) }
            .profiles.first { it.name == "shield-bulk-b" }
        try {
            val model = SessionShieldViewModel(app)
            val deadline = System.currentTimeMillis() + 10_000
            while (System.currentTimeMillis() < deadline &&
                (model.state.value.inventory.profiles.none { it.id == "profile:${first.id}" } ||
                    model.state.value.inventory.profiles.none { it.id == "profile:${second.id}" })
            ) {
                Thread.sleep(100)
            }
            assertTrue(model.state.value.inventory.removableProfileIds.contains("profile:${first.id}"))
            assertTrue(model.state.value.inventory.removableProfileIds.contains("profile:${second.id}"))

            model.toggle("profile:${first.id}")
            model.toggle("profile:${second.id}")
            assertEquals(2, model.state.value.selectedProfiles.size)
            model.requestRevokeSelected()
            assertEquals(ShieldAction.REVOKE_SELECTED, model.state.value.confirming)
            model.confirmRevokeSelected()

            val after = System.currentTimeMillis() + 10_000
            while (System.currentTimeMillis() < after &&
                model.state.value.receipt?.code != ShieldReceiptCode.SECRETS_REMOVED
            ) {
                Thread.sleep(100)
            }
            assertEquals(ShieldReceiptCode.SECRETS_REMOVED, model.state.value.receipt?.code)
            assertEquals(2, model.state.value.receipt?.removed)

            val remaining = runBlocking { store.load() }
            assertTrue(remaining.profiles.none { it.id == first.id })
            assertTrue(remaining.profiles.none { it.id == second.id })
            assertNull(runBlocking { store.config(first.id) })
            assertNull(runBlocking { store.config(second.id) })
        } finally {
            runBlocking {
                listOf(first.id, second.id).forEach { id ->
                    runCatching { store.remove(id) }
                }
            }
        }
    }

    /**
     * Runtime visual gate. These fixtures render the real screen at the narrowest supported
     * width, in both directions, in both themes and at the largest supported font scale. They
     * assert the state is present and capture the actual Compose frame as evidence.
     */
    @Test fun visualEmptyStateFits360DpInEnglish() {
        screen(SessionShieldUiState(loading = false, inventory = ShieldInventory()), widthDp = 360)
        compose.onNodeWithTag("shield-empty").assertExists()
        compose.onNodeWithTag("shield-refresh").performScrollTo().assertIsDisplayed()
        capture("shield-after-360-day-en-1.0-empty")
    }

    @Test fun visualPopulatedStateShowsLocalizedHistoryAndBulkSelection() {
        screen(populated(selected = setOf("profile:abc", "profile:def")), widthDp = 360)
        compose.onNodeWithTag("shield-history-0").performScrollTo()
            .assertTextContains("Cache cleanup", substring = true)
        compose.onNodeWithTag("shield-selected-count").performScrollTo()
            .assertTextContains("2", substring = true)
        compose.onNodeWithTag("shield-revoke-selected").performScrollTo().assertIsDisplayed()
        capture("shield-after-360-day-en-1.0-selected")
    }

    @Test fun visualReceiptAndActionsFit360DpAtLargeText() {
        screen(
            populated(
                receipt = ShieldReceipt(
                    action = ShieldAction.REVOKE_SELECTED,
                    removed = 2,
                    pruned = 0,
                    bytesFreed = 0L,
                    atMillis = 1_760_000_000_000L,
                    code = ShieldReceiptCode.SECRETS_REMOVED,
                ),
            ),
            widthDp = 360,
            fontScale = 1.6f,
        )
        compose.onNodeWithTag("shield-receipt").performScrollTo()
            .assertTextContains("What actually happened", substring = true)
        compose.onNodeWithTag("shield-receipt-detail").performScrollTo().assertExists()
        compose.onNodeWithTag("shield-receipt-counts").performScrollTo().assertExists()
        capture("shield-after-360-day-en-1.6-receipt")
    }

    @Test fun visualArabicNightAtLargeTextKeepsHistoryAndReceiptReadable() {
        screen(
            populated(
                receipt = ShieldReceipt(
                    action = ShieldAction.PRUNE_OLD,
                    removed = 0,
                    pruned = 2,
                    bytesFreed = 0L,
                    atMillis = 1_760_000_000_000L,
                    code = ShieldReceiptCode.PRUNED,
                ),
            ),
            language = "ar",
            direction = LayoutDirection.Rtl,
            night = true,
            widthDp = 360,
            fontScale = 1.6f,
        )
        compose.onNodeWithText("خصوصية الجلسات").assertExists()
        compose.onNodeWithTag("shield-receipt").performScrollTo()
            .assertTextContains("ما حدث فعلًا", substring = true)
        compose.onNodeWithTag("shield-receipt-detail").performScrollTo().assertExists()
        capture("shield-after-360-night-ar-1.6-receipt")
    }

    @Test fun visualPopulatedTopShowsHonestCountsAt411Dp() {
        screen(populated(), widthDp = 411)
        compose.onNodeWithTag("shield-profile-count").assertTextEquals("2 profiles")
        compose.onNodeWithTag("shield-history-count").assertTextEquals("3 history entries")
        compose.onNodeWithTag("shield-secret-count").assertTextEquals("2 items hold a credential")
        capture("shield-after-411-day-en-1.0-populated-top")
    }

    @Test fun visualFailureStateIsHonestAt411Dp() {
        screen(
            SessionShieldUiState(loading = false, inventory = ShieldInventory(failed = true)),
            widthDp = 411,
        )
        compose.onNodeWithTag("shield-failed").assertExists()
        compose.onNodeWithTag("shield-retry").assertIsDisplayed()
        compose.onNodeWithTag("shield-empty").assertDoesNotExist()
        capture("shield-after-411-day-en-1.0-failed")
    }

    @Test fun visualLoadingStateIsNotMistakenForEmpty() {
        screen(SessionShieldUiState(loading = true), night = true, widthDp = 411)
        compose.onNodeWithTag("shield-loading").assertExists()
        compose.onNodeWithTag("shield-empty").assertDoesNotExist()
        capture("shield-after-411-night-en-1.0-loading")
    }

    @Test fun visualBulkConfirmationDialogFits360DpAtLargeText() {
        screen(
            populated(selected = setOf("profile:abc", "profile:def")).copy(
                confirming = ShieldAction.REVOKE_SELECTED,
            ),
            widthDp = 360,
            fontScale = 1.6f,
        )
        compose.onNodeWithTag("shield-revoke-selected-confirm").assertExists()
        compose.onNodeWithTag("shield-revoke-selected-cancel").assertExists()
        captureDialog("shield-after-360-day-en-1.6-bulk-dialog")
    }
}
