package com.knoux.kxprivatevpn

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.platform.network.NetworkDoctor
import org.junit.Assert.*
import org.junit.Test
import kotlinx.coroutines.runBlocking
import java.io.File

class NetworkDoctorPlatformTest {
    @Test fun reportsRealProbeOutcomeWithoutAssumingExternalInternet() = runBlocking {
        // CI emulators occasionally have no outbound HTTPS. A timeout is an observed
        // network failure, NOT a fake success, and must be recorded explicitly.
        val outcome = runCatching {
            NetworkDoctor.probe("https://github.com/daynightae-cmyk/KX-Private-VPN", true)
        }
        val evidence = outcome.fold(
            onSuccess = { reading ->
                assertTrue(reading.code in 100..599)
                assertTrue(reading.elapsedMs >= 0)
                "LIVE_HTTP=${reading.code}\nHTTPS_HEAD_elapsedMs=${reading.elapsedMs}\n"
            },
            onFailure = { error ->
                val networkUnavailable = error is java.net.SocketTimeoutException ||
                    error is java.net.ConnectException ||
                    error is java.net.UnknownHostException ||
                    error is kotlinx.coroutines.TimeoutCancellationException
                assertTrue("Unexpected probe failure: ${error::class.java.name}: ${error.message}", networkUnavailable)
                "LIVE_HTTP=UNVERIFIED\nNETWORK_ERROR=${error::class.java.simpleName}\n"
            },
        )
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val folder = File(context.getExternalFilesDir(null), "identity-evidence").apply { mkdirs() }
        File(folder, "10-network-probe.txt").writeText(evidence + "No VPN claim\n")
        // An offline CI emulator cannot provide live-network proof. Record the precise
        // unavailable result and mark this external check SKIPPED, never PASSED.
        org.junit.Assume.assumeTrue("External HTTPS evidence unavailable: ${outcome.exceptionOrNull()?.javaClass?.simpleName}", outcome.isSuccess)
    }
    @Test fun reportsAndroidCapabilitiesWithoutInventingProtection() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val network = manager.activeNetwork
        val capabilities = network?.let(manager::getNetworkCapabilities)
        val reading = NetworkDoctor.read(context)
        assertEquals(network != null, reading.present)
        assertEquals(capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true, reading.validated)
        assertEquals(capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true, reading.vpn)
        assertEquals(network?.let(manager::getLinkProperties)?.dnsServers?.mapNotNull { it.hostAddress } ?: emptyList<String>(), reading.dns)
    }
}
