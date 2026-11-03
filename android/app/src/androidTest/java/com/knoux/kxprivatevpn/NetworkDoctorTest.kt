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
    @Test fun measuresOneAuthorizedRequestToOwnersRepository() = runBlocking {
        val reading = NetworkDoctor.probe("https://github.com/daynightae-cmyk/KX-Private-VPN", true)
        assertTrue(reading.code in 100..599)
        assertTrue(reading.elapsedMs >= 0)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val folder = File(context.getExternalFilesDir(null), "identity-evidence").apply { mkdirs() }
        File(folder, "10-network-probe.txt").writeText("HTTP ${reading.code}\nHTTPS HEAD elapsedMs=${reading.elapsedMs}\nNo VPN claim\n")
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
