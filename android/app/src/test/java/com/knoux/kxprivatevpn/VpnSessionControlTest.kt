package com.knoux.kxprivatevpn

import com.knoux.kxprivatevpn.platform.vpn.VpnSessionControl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The notification may only claim a disconnect when a live controller accepted it. These
 * tests pin the seam; the controller's own state machine is covered by VpnControllerTest.
 */
class VpnSessionControlTest {
    @Test fun noRegisteredControllerCannotClaimADisconnect() {
        assertFalse(VpnSessionControl.requestDisconnect())
    }

    @Test fun aRegisteredControllerReceivesTheRequest() {
        var calls = 0
        val handler: () -> Boolean = { calls++; true }
        VpnSessionControl.register(handler)
        try {
            assertTrue(VpnSessionControl.requestDisconnect())
            assertEquals(1, calls)
        } finally {
            VpnSessionControl.unregister(handler)
        }
    }

    @Test fun aHandlerThatRefusesIsNotReportedAsAccepted() {
        val handler: () -> Boolean = { false }
        VpnSessionControl.register(handler)
        try {
            assertFalse(VpnSessionControl.requestDisconnect())
        } finally {
            VpnSessionControl.unregister(handler)
        }
    }

    @Test fun onlyTheRegisteredHandlerCanUnregisterItself() {
        val registered: () -> Boolean = { true }
        val other: () -> Boolean = { true }
        VpnSessionControl.register(registered)
        try {
            VpnSessionControl.unregister(other)
            assertTrue(VpnSessionControl.requestDisconnect())
        } finally {
            VpnSessionControl.unregister(registered)
        }
        assertFalse(VpnSessionControl.requestDisconnect())
    }
}
