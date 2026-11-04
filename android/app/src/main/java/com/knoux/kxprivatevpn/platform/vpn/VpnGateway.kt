package com.knoux.kxprivatevpn.platform.vpn

import android.content.Context
import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Thin seam over the tunnel backend so the state machine is testable without a device VPN. */
interface VpnGateway {
    suspend fun start(config: Config): String?
    suspend fun stop()
    suspend fun traffic(): VpnTraffic
    suspend fun isUp(): Boolean

    /** null means the platform could not be read; it must never be reported as "off". */
    suspend fun isLockdownEnabled(): Boolean?
}

/**
 * Real transport: the official WireGuard tunnel library runs its own VpnService.
 * It never fakes a handshake; failures are returned to the controller unchanged.
 */
class WireGuardVpnGateway(context: Context) : VpnGateway {
    private val appContext = context.applicationContext
    // Loading the native Go library must stay off the startup path.
    private val backend: Backend by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { GoBackend(appContext) }
    private val mutex = Mutex()
    private val tunnel = object : Tunnel {
        override fun getName(): String = TUNNEL_NAME
        override fun onStateChange(state: Tunnel.State) = Unit
    }

    override suspend fun start(config: Config): String? = mutex.withLock {
        withContext(Dispatchers.IO) {
            val state = backend.setState(tunnel, Tunnel.State.UP, config)
            check(state == Tunnel.State.UP) { "WireGuard backend did not report UP" }
            backend.version
        }
    }

    override suspend fun stop() = mutex.withLock {
        withContext(Dispatchers.IO) {
            backend.setState(tunnel, Tunnel.State.DOWN, null)
            Unit
        }
    }

    override suspend fun traffic(): VpnTraffic = withContext(Dispatchers.IO) {
        val statistics = backend.getStatistics(tunnel) ?: return@withContext VpnTraffic()
        val handshake = statistics.peers()
            .mapNotNull { statistics.peer(it)?.latestHandshakeEpochMillis() }
            .filter { it > 0 }
            .maxOrNull()
        VpnTraffic(statistics.totalRx(), statistics.totalTx(), handshake)
    }

    override suspend fun isUp(): Boolean = withContext(Dispatchers.IO) {
        try {
            backend.getState(tunnel) == Tunnel.State.UP
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun isLockdownEnabled(): Boolean? = withContext(Dispatchers.IO) {
        try {
            backend.isLockdownEnabled
        } catch (_: Exception) {
            null
        }
    }

    private companion object {
        // Tunnel.NAME_PATTERN allows 1..15 of [a-zA-Z0-9_=+.-].
        const val TUNNEL_NAME = "kx-private"
    }
}
