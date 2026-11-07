package com.knoux.kxprivatevpn.platform.vpn

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.knoux.kxprivatevpn.platform.network.NetworkDoctor
import kotlinx.coroutines.suspendCancellableCoroutine
import java.net.Inet4Address
import java.net.Inet6Address
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.net.ssl.HttpsURLConnection
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Uses the current app-owned gateway, never creates a second GoBackend or changes routing. */
class AndroidVpnProbePort(
    context: Context,
    private val gateway: VpnGateway,
    private val sameSession: () -> Boolean,
) : VpnProbePort {
    private val manager = context.applicationContext.getSystemService(ConnectivityManager::class.java)

    override suspend fun snapshot(): VpnPathSnapshot {
        val network = manager.activeNetwork
        val caps = network?.let(manager::getNetworkCapabilities)
        val properties = network?.let(manager::getLinkProperties)
        val owned = sameSession() && gateway.isUp()
        val traffic = if (owned) gateway.traffic() else VpnTraffic()
        return VpnPathSnapshot(
            ownedTunnelUp = owned && sameSession(),
            activeVpn = caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true,
            networkHandle = network?.networkHandle,
            traffic = traffic,
            dnsConfigured = !properties?.dnsServers.isNullOrEmpty(),
            defaultIpv4 = properties?.routes?.any { it.isDefaultRoute && it.destination.address is Inet4Address } == true,
            defaultIpv6 = properties?.routes?.any { it.isDefaultRoute && it.destination.address is Inet6Address } == true,
        )
    }

    override suspend fun head(networkHandle: Long, httpsUrl: String): Int {
        val url = NetworkDoctor.validate(httpsUrl).toURL()
        val network = checkNotNull(manager.activeNetwork)
        check(network.networkHandle == networkHandle && sameSession())
        check(manager.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true)
        return suspendCancellableCoroutine { continuation ->
            val cancelled = AtomicBoolean(false)
            val connectionRef = AtomicReference<HttpsURLConnection?>()
            // Network DNS may ignore thread interruption. One bounded worker prevents leaks.
            val future = executor.submit {
                if (!cancelled.get()) try {
                    val connection = network.openConnection(url) as HttpsURLConnection
                    connectionRef.set(connection)
                    try {
                        if (!cancelled.get()) {
                            connection.connectTimeout = 3_000
                            connection.readTimeout = 3_000
                            connection.instanceFollowRedirects = false
                            connection.useCaches = false
                            connection.requestMethod = "HEAD"
                            val code = connection.responseCode
                            if (continuation.isActive) continuation.resume(code)
                        }
                    } finally {
                        connectionRef.set(null)
                        connection.disconnect()
                    }
                } catch (failure: Exception) {
                    if (continuation.isActive) continuation.resumeWithException(failure)
                }
            }
            continuation.invokeOnCancellation {
                cancelled.set(true)
                connectionRef.getAndSet(null)?.disconnect()
                future.cancel(true)
                executor.purge()
            }
        }
    }

    private companion object {
        val executor = ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS, ArrayBlockingQueue(1),
            { task -> Thread(task, "kx-vpn-path-probe").apply { isDaemon = true } },
            ThreadPoolExecutor.AbortPolicy())
    }
}
