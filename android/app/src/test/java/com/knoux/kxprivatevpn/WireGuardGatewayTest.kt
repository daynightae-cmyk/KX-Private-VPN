package com.knoux.kxprivatevpn

import com.knoux.kxprivatevpn.platform.vpn.WireGuardVpnGateway
import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.Statistics
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class WireGuardGatewayTest {
    private class BackendStub : Backend {
        var state = Tunnel.State.UP
        var readFailure: Exception? = null
        var stopResult = Tunnel.State.DOWN
        override fun getRunningTunnelNames(): Set<String> = setOf("kx-private")
        override fun getState(tunnel: Tunnel): Tunnel.State { readFailure?.let { throw it }; return state }
        override fun getStatistics(tunnel: Tunnel): Statistics = error("Statistics are not used in these tests")
        override fun getVersion() = "test"
        override fun isAlwaysOn() = false
        override fun isLockdownEnabled() = false
        override fun setState(tunnel: Tunnel, state: Tunnel.State, config: Config?): Tunnel.State = stopResult
    }

    @Test fun backendUpResponseCannotBeReportedAsSuccessfulStop() = runBlocking {
        val backend = BackendStub().apply { stopResult = Tunnel.State.UP }
        val gateway = WireGuardVpnGateway { backend }
        assertTrue(runCatching { gateway.stop() }.exceptionOrNull() is IllegalStateException)
    }

    @Test fun successfulStopRequiresExplicitDown() = runBlocking {
        val gateway = WireGuardVpnGateway { BackendStub() }
        gateway.stop()
    }

    @Test fun unreadableStateIsNotSilentlyDown() = runBlocking {
        val backend = BackendStub().apply { readFailure = IllegalStateException("unavailable") }
        val gateway = WireGuardVpnGateway { backend }
        assertTrue(runCatching { gateway.isUp() }.exceptionOrNull() is IllegalStateException)
        backend.readFailure = null
        assertTrue(gateway.isUp())
        backend.state = Tunnel.State.DOWN
        assertFalse(gateway.isUp())
    }
}
