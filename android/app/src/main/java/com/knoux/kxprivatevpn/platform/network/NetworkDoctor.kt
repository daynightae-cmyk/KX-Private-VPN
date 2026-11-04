package com.knoux.kxprivatevpn.platform.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.SystemClock
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.net.URI
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import javax.net.ssl.HttpsURLConnection

data class NetworkReading(val present: Boolean, val validated: Boolean, val captive: Boolean,
    val vpn: Boolean, val transports: List<String>, val dns: List<String>)
data class HttpsReading(val code: Int, val elapsedMs: Long)

object NetworkDoctor {
    // OS DNS may ignore interruption. A bounded worker and queue prevent repeated requests from
    // spawning unlimited blocked threads; the coroutine deadline still releases the UI.
    private val executor = ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS, ArrayBlockingQueue(1),
        { task -> Thread(task, "kx-network-probe").apply { isDaemon = true } }, ThreadPoolExecutor.AbortPolicy())
    fun read(context: Context): NetworkReading {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val network = manager.activeNetwork
        val caps = network?.let(manager::getNetworkCapabilities)
        val links = network?.let(manager::getLinkProperties)
        val transports = listOf(NetworkCapabilities.TRANSPORT_WIFI to "Wi-Fi",
            NetworkCapabilities.TRANSPORT_CELLULAR to "Cellular", NetworkCapabilities.TRANSPORT_ETHERNET to "Ethernet",
            NetworkCapabilities.TRANSPORT_VPN to "VPN").filter { caps?.hasTransport(it.first) == true }.map { it.second }
        return NetworkReading(network != null, caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true,
            caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL) == true,
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true, transports,
            links?.dnsServers?.mapNotNull { it.hostAddress } ?: emptyList())
    }
    fun validate(value: String): URI {
        require(value.length in 1..2048 && value.none { it.isISOControl() })
        val uri = URI(value.trim())
        require(uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.rawUserInfo == null &&
            uri.rawQuery == null && uri.rawFragment == null && (uri.port == -1 || uri.port in 1..65535))
        return uri
    }
    suspend fun probe(value: String, approved: Boolean): HttpsReading {
        require(approved) { "User approval required before external request" }
        val target = validate(value)
        return withTimeout(7000) { suspendCancellableCoroutine { continuation ->
            val cancelled = AtomicBoolean(false)
            val future = executor.submit {
                if (!cancelled.get()) try {
                    val connection = target.toURL().openConnection() as HttpsURLConnection
                    connection.connectTimeout = 2500
                    connection.readTimeout = 2500
                    connection.instanceFollowRedirects = false
                    connection.requestMethod = "HEAD"
                    val start = SystemClock.elapsedRealtimeNanos()
                    try {
                        val result = HttpsReading(connection.responseCode, (SystemClock.elapsedRealtimeNanos() - start) / 1_000_000)
                        if (continuation.isActive) continuation.resume(result)
                    } finally { connection.disconnect() }
                } catch (e: Exception) { if (continuation.isActive) continuation.resumeWithException(e) }
            }
            continuation.invokeOnCancellation { cancelled.set(true); future.cancel(true); executor.purge() }
        } }
    }
}
