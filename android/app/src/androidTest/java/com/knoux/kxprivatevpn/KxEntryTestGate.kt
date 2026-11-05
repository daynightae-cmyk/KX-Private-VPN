package com.knoux.kxprivatevpn

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick

/**
 * Every full-activity flow test must perform the same explicit entry action as
 * a person. This must never be bypassed in production or test-only app code.
 */
internal fun enterKxFromSplash(rule: ComposeTestRule) {
    rule.waitUntil(timeoutMillis = 15_000) {
        runCatching { rule.onNodeWithTag("entry-orb").assertIsEnabled() }.isSuccess
    }
    rule.onNodeWithTag("route-home").assertDoesNotExist()
    rule.onNodeWithTag("entry-orb").performClick()
}
