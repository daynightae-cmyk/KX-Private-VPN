package com.knoux.kxprivatevpn

import com.knoux.kxprivatevpn.platform.vpn.VpnIssue
import com.knoux.kxprivatevpn.platform.vpn.VpnPhase
import com.knoux.kxprivatevpn.platform.vpn.VpnSession
import com.knoux.kxprivatevpn.platform.vpn.VpnSessionOwnership
import com.knoux.kxprivatevpn.platform.vpn.VpnState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins when a live session may be shown as the server in use and when selection must be
 * locked. These rules keep the saved server from silently diverging from the real tunnel.
 */
class VpnSessionOwnershipTest {
    private val alpha = VpnSession("alpha", "Alpha", "127.0.0.1:51820")

    private fun state(
        phase: VpnPhase,
        issue: VpnIssue? = null,
        session: VpnSession? = alpha,
    ) = VpnState(phase, session, issue)

    @Test fun idleOwnsNothingAndAllowsSelection() {
        val idle = state(VpnPhase.IDLE)

        assertFalse(VpnSessionOwnership.ownsTunnel(idle))
        assertNull(VpnSessionOwnership.activeProfileId(idle))
        assertFalse(VpnSessionOwnership.selectionLocked(idle))
        assertFalse(VpnSessionOwnership.requiresStopBeforeSelect(idle, "beta"))
    }

    @Test fun connectingOwnsTheTargetAndLocksSelection() {
        val connecting = state(VpnPhase.CONNECTING)

        assertTrue(VpnSessionOwnership.ownsTunnel(connecting))
        assertEquals("alpha", VpnSessionOwnership.activeProfileId(connecting))
        assertTrue(VpnSessionOwnership.selectionLocked(connecting))
        assertFalse(VpnSessionOwnership.requiresStopBeforeSelect(connecting, "beta"))
    }

    @Test fun connectedSessionIsTheOnlyServerInUse() {
        val connected = state(VpnPhase.CONNECTED)

        assertTrue(VpnSessionOwnership.ownsTunnel(connected))
        assertEquals("alpha", VpnSessionOwnership.activeProfileId(connected))
        assertFalse(VpnSessionOwnership.selectionLocked(connected))
        assertFalse(VpnSessionOwnership.requiresStopBeforeSelect(connected, "alpha"))
        assertTrue(VpnSessionOwnership.requiresStopBeforeSelect(connected, "beta"))
    }

    @Test fun disconnectingStillOwnsTheBackendAndLocksSelection() {
        val disconnecting = state(VpnPhase.DISCONNECTING)

        assertTrue(VpnSessionOwnership.ownsTunnel(disconnecting))
        assertEquals("alpha", VpnSessionOwnership.activeProfileId(disconnecting))
        assertTrue(VpnSessionOwnership.selectionLocked(disconnecting))
    }

    @Test fun failedStopKeepsOwnershipAndLocksSelectionForRetry() {
        val stopFailed = state(VpnPhase.ERROR, VpnIssue.STOP_FAILED)

        assertTrue(VpnSessionOwnership.ownsTunnel(stopFailed))
        assertEquals("alpha", VpnSessionOwnership.activeProfileId(stopFailed))
        assertTrue(VpnSessionOwnership.selectionLocked(stopFailed))
        assertFalse(VpnSessionOwnership.requiresStopBeforeSelect(stopFailed, "beta"))
    }

    @Test fun failedStopWithoutSessionStillBlocksSelection() {
        val stopFailed = state(VpnPhase.ERROR, VpnIssue.STOP_FAILED, session = null)

        assertFalse(VpnSessionOwnership.ownsTunnel(stopFailed))
        assertNull(VpnSessionOwnership.activeProfileId(stopFailed))
        assertTrue(VpnSessionOwnership.selectionLocked(stopFailed))
    }

    @Test fun startFailureNeverClaimsOwnership() {
        val startFailed = state(VpnPhase.ERROR, VpnIssue.START_FAILED)

        assertFalse(VpnSessionOwnership.ownsTunnel(startFailed))
        assertNull(VpnSessionOwnership.activeProfileId(startFailed))
        assertFalse(VpnSessionOwnership.selectionLocked(startFailed))
        assertFalse(VpnSessionOwnership.requiresStopBeforeSelect(startFailed, "beta"))
    }

    @Test fun backendStoppedIsNotAnOwnedTunnel() {
        val stopped = state(VpnPhase.ERROR, VpnIssue.BACKEND_STOPPED)

        assertFalse(VpnSessionOwnership.ownsTunnel(stopped))
        assertNull(VpnSessionOwnership.activeProfileId(stopped))
        assertFalse(VpnSessionOwnership.selectionLocked(stopped))
    }

    @Test fun connectingWithoutSessionStillLocksButClaimsNoServer() {
        val connecting = state(VpnPhase.CONNECTING, session = null)

        assertFalse(VpnSessionOwnership.ownsTunnel(connecting))
        assertNull(VpnSessionOwnership.activeProfileId(connecting))
        assertTrue(VpnSessionOwnership.selectionLocked(connecting))
    }

    @Test fun connectedWithoutSessionStopsBeforePersistingAnything() {
        val connected = state(VpnPhase.CONNECTED, session = null)

        assertFalse(VpnSessionOwnership.ownsTunnel(connected))
        assertNull(VpnSessionOwnership.activeProfileId(connected))
        // Defensive: an unresolved CONNECTED owner must still stop before a new server is saved.
        assertTrue(VpnSessionOwnership.requiresStopBeforeSelect(connected, "alpha"))
        assertTrue(VpnSessionOwnership.requiresStopBeforeSelect(connected, "beta"))
    }
}
