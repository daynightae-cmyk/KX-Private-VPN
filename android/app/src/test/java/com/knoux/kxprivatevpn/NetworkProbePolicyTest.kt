package com.knoux.kxprivatevpn

import com.knoux.kxprivatevpn.features.NetworkProbePolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkProbePolicyTest {
    @Test fun changingProbeTargetRequiresNewApprovalAndClearsOldReceipt() {
        val approved = com.knoux.kxprivatevpn.features.NetworkUiState(
            probeUrl = "https://example.invalid/old", probeConsent = true, probeFailed = true,
        )
        val changed = NetworkProbePolicy.editedTarget(approved, "https://example.invalid/new")
        assertFalse(changed.probeConsent)
        assertFalse(changed.probeFailed)
        assertFalse(NetworkProbePolicy.canRun(changed.probeUrl, changed.probeConsent, changed.probing))
        assertTrue(NetworkProbePolicy.editedTarget(approved, approved.probeUrl).probeConsent)
        assertTrue(NetworkProbePolicy.editedTarget(approved, "x".repeat(2049)).probeConsent)
        val busy = approved.copy(probing = true)
        assertTrue(NetworkProbePolicy.editedTarget(busy, "https://example.invalid/new").probing)
        assertTrue(NetworkProbePolicy.editedTarget(busy, "https://example.invalid/new").probeConsent)
    }
    @Test fun requestNeedsConsentAtargetAndNoRunInFlight() {
        assertFalse(NetworkProbePolicy.canRun("https://example.invalid", consent = false, probing = false))
        assertFalse(NetworkProbePolicy.canRun("", consent = true, probing = false))
        assertFalse(NetworkProbePolicy.canRun("https://example.invalid", consent = true, probing = true))
        assertTrue(NetworkProbePolicy.canRun("https://example.invalid", consent = true, probing = false))
    }

    @Test fun targetsTheEngineWouldRejectCannotBeRun() {
        listOf(
            "http://example.invalid",
            "https://user:password@example.invalid",
            "https://example.invalid?token=secret",
            "https://example.invalid/#private",
        ).forEach { url ->
            assertTrue("$url must be reported as unusable", NetworkProbePolicy.targetProblem(url))
            assertFalse("$url must not enable the request",
                NetworkProbePolicy.canRun(url, consent = true, probing = false))
        }
        assertFalse(NetworkProbePolicy.targetProblem("https://example.invalid/status"))
        assertFalse("A blank field is incomplete, not invalid",
            NetworkProbePolicy.targetProblem(""))
    }
}
