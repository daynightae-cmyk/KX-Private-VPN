package com.knoux.kxprivatevpn

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.AnnotatedString
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.launch.KxFirstRunWizard
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * The in-app Android first-run wizard (splash -> 3 education/access steps -> sign-in).
 * It never silently enables authentication or VPN access: the email/password form only
 * reports the honest unconfigured state, while guest / import / Pro code remain real.
 */
class FirstRunWizardTest {
    @get:Rule val compose = createComposeRule()

    private fun setWizard(
        night: Boolean = true,
        onGuest: () -> Unit = {},
        onImport: () -> Unit = {},
        onOpenPro: () -> Unit = {},
    ) = compose.setContent {
        KxFirstRunWizard(night = night, onGuest = onGuest, onImport = onImport, onOpenPro = onOpenPro)
    }

    @Test fun educationStepsThenAccessThenHonestLogin() {
        var guestCalls = 0
        setWizard(onGuest = { guestCalls++ })
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        compose.onNodeWithTag("kx-first-run-wizard").assertIsDisplayed()
        compose.onNodeWithTag("wizard-step-counter").assertTextEquals("01 / 04")
        compose.onNodeWithTag("wizard-step-title").assertIsDisplayed()

        compose.onNodeWithTag("wizard-next").performClick()
        compose.onNodeWithTag("wizard-step-counter").assertTextEquals("02 / 04")
        compose.onNodeWithTag("wizard-back-or-skip").performClick()
        compose.onNodeWithTag("wizard-step-counter").assertTextEquals("01 / 04")

        // Step 0 -> 1 -> 2 (access choice, reference screen 4).
        compose.onNodeWithTag("wizard-next").performClick()
        compose.onNodeWithTag("wizard-next").performClick()
        compose.onNodeWithTag("wizard-step-counter").assertTextEquals("03 / 04")
        compose.onNodeWithTag("wizard-signin-option").assertIsDisplayed()
        compose.onNodeWithTag("wizard-guest-option").assertIsDisplayed()
        compose.onNodeWithTag("wizard-import-option").assertIsDisplayed()

        // Access -> login (reference screen 5): no step chrome on the login screen.
        compose.onNodeWithTag("wizard-signin-option").performClick()
        compose.onNodeWithTag("wizard-login").assertIsDisplayed()
        compose.onNodeWithTag("wizard-step-counter").assertDoesNotExist()
        compose.onNodeWithTag("wizard-email").assertIsDisplayed()
        compose.onNodeWithTag("wizard-password").assertIsDisplayed()
        compose.onNodeWithTag("wizard-signin").assertIsDisplayed()

        // Empty submit reports the empty state, never a fake success.
        compose.onNodeWithTag("wizard-signin").performClick()
        compose.onNodeWithTag("wizard-login-status")
            .assertTextEquals(context.getString(R.string.onboard_login_empty))

        // Filled submit reports the honest unconfigured state (no account backend in
        // this build) and clears both fields before any callback, like ProScreen.
        compose.onNodeWithTag("wizard-email").performTextInput("user@example.com")
        compose.onNodeWithTag("wizard-password").performTextInput("s3cret-value")
        compose.onNodeWithTag("wizard-signin").performClick()
        compose.onNodeWithTag("wizard-login-status")
            .assertTextEquals(context.getString(R.string.onboard_login_unavailable))
        // Cleared before any callback: the editable value is empty again (the placeholder
        // text is still rendered by the decoration box, so assert the editable state).
        compose.onNodeWithTag("wizard-email").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
        compose.onNodeWithTag("wizard-password").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
        compose.onNodeWithTag("wizard-email").assertTextContains(context.getString(R.string.onboard_email))

        // The login-step guest button shares the onGuest callback with the access-step
        // guest button (covered by guestOptionUsesRealCallback). Just verify it is
        // present and clickable — honest builds never simulate a real sign-in here.
        compose.onNodeWithTag("wizard-login-guest").performClick()
    }

    @Test fun skipJumpsToAccessAndBackWalksTheSteps() {
        setWizard()
        // "تخطي" on the first education step goes straight to the access choice.
        compose.onNodeWithTag("wizard-back-or-skip").performClick()
        compose.onNodeWithTag("wizard-signin-option").assertIsDisplayed()
        // Back from access returns to the last education step.
        compose.onNodeWithTag("wizard-back-or-skip").performClick()
        compose.onNodeWithTag("wizard-step-counter").assertTextEquals("02 / 04")
    }

    @Test fun guestOptionUsesRealCallback() {
        var guest = 0
        setWizard(onGuest = { guest++ })
        compose.onNodeWithTag("wizard-back-or-skip").performClick()   // -> access
        compose.onNodeWithTag("wizard-guest-option").performClick()
        compose.runOnIdle { assertEquals(1, guest) }
    }

    @Test fun importOptionUsesRealCallback() {
        var imports = 0
        setWizard(onImport = { imports++ })
        compose.onNodeWithTag("wizard-back-or-skip").performClick()   // -> access
        compose.onNodeWithTag("wizard-import-option").performClick()
        compose.runOnIdle { assertEquals(1, imports) }
    }

    @Test fun createAccountOpensTheRealProActivation() {
        var pro = 0
        setWizard(onOpenPro = { pro++ })
        compose.onNodeWithTag("wizard-next").performClick()
        compose.onNodeWithTag("wizard-next").performClick()            // -> access
        compose.onNodeWithTag("wizard-signin-option").performClick()  // -> login
        compose.onNodeWithTag("wizard-create-account").performClick()
        compose.runOnIdle { assertEquals(1, pro) }
    }

    @Test fun dayModeRendersAndNavigates() {
        setWizard(night = false)
        compose.onNodeWithTag("kx-first-run-wizard").assertIsDisplayed()
        compose.onNodeWithTag("wizard-next").performClick()
        compose.onNodeWithTag("wizard-step-counter").assertTextEquals("02 / 04")
    }
}
