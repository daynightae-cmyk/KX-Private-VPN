package com.knoux.kxprivatevpn

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.data.config.NetworkSettingsStore
import com.knoux.kxprivatevpn.platform.vpn.AdvancedNetworkSettings
import com.knoux.kxprivatevpn.platform.vpn.DnsMode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** Mounted values must come back sane: nothing invalid may reach the tunnel configurator. */
class NetworkSettingsStoreTest {
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val store get() = NetworkSettingsStore(context)

    @Before
    fun clearStore() {
        context.getSharedPreferences("advanced_network", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test fun roundTripsEveryField() = runBlocking {
        val settings = AdvancedNetworkSettings(
            mtuOverride = 1280,
            dnsMode = DnsMode.CUSTOM,
            customDns = listOf("1.1.1.1", "2606:4700:4700::1111"),
            keepaliveSeconds = 25,
        )
        store.save(settings)
        assertEquals(settings, store.load())
    }

    @Test fun sanitizedLoadGivesBackProfileControl() = runBlocking {
        assertEquals(AdvancedNetworkSettings(), store.load())
    }

    @Test fun mountsGarbageAndStillLoadsSomethingHonest() = runBlocking {
        context.getSharedPreferences("advanced_network", Context.MODE_PRIVATE).edit()
            .putInt("mtu", 999)
            .putString("dns_mode", "NONSENSE")
            .putString("custom_dns", "example.com, 1.1.1.1")
            .putInt("keepalive", 70000)
            .commit()
        val loaded = store.load()

        assertEquals(null, loaded.mtuOverride)
        assertEquals(null, loaded.keepaliveSeconds)
        assertEquals(DnsMode.PROFILE, loaded.dnsMode)
        assertEquals(emptyList<String>(), loaded.customDns)
    }

    @Test fun customModeKeepsItsValidPartWhenOnlySomeHostsSurvive() = runBlocking {
        context.getSharedPreferences("advanced_network", Context.MODE_PRIVATE).edit()
            .putString("dns_mode", "CUSTOM")
            .putString("custom_dns", "8.8.8.8, junk-host")
            .commit()
        val loaded = store.load()

        assertEquals(DnsMode.CUSTOM, loaded.dnsMode)
        assertEquals(listOf("8.8.8.8"), loaded.customDns)
    }

    @Test fun customModeWithoutUsableHostsFallsBackToProfile() = runBlocking {
        context.getSharedPreferences("advanced_network", Context.MODE_PRIVATE).edit()
            .putString("dns_mode", "CUSTOM")
            .putString("custom_dns", "example.com")
            .commit()

        assertEquals(DnsMode.PROFILE, store.load().dnsMode)
    }

    @Test fun saveRejectsOutageRangeValuesInsteadOfClamping() = runBlocking {
        try {
            store.save(AdvancedNetworkSettings(mtuOverride = 900))
            org.junit.Assert.fail("Out-of-range MTU must be rejected")
        } catch (_: IllegalArgumentException) {
        }
        try {
            store.save(AdvancedNetworkSettings(
                dnsMode = DnsMode.CUSTOM, customDns = listOf("example.com")))
            org.junit.Assert.fail("Non-numeric DNS must be rejected")
        } catch (_: IllegalArgumentException) {
        }
        assertEquals(AdvancedNetworkSettings(), store.load())
    }
}
