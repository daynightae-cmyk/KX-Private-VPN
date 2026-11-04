package com.knoux.kxprivatevpn

import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.data.config.SplitTunnelStore
import com.knoux.kxprivatevpn.platform.vpn.SplitTunnelMode
import com.knoux.kxprivatevpn.platform.vpn.SplitTunnelSelection
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class SplitTunnelStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun namespace() = "test_split_" + UUID.randomUUID().toString().replace("-", "")

    @Test fun selectionSurvivesANewStoreInstance() = runBlocking {
        val namespace = namespace()
        val selection = SplitTunnelSelection(SplitTunnelMode.BYPASS,
            setOf("com.example.browser", "com.example.chat"))
        SplitTunnelStore(context, namespace).save(selection)
        assertEquals(selection, SplitTunnelStore(context, namespace).load())
    }

    @Test fun invalidAndEmptyValuesFallBackToANonDestructiveDefault() = runBlocking {
        val namespace = namespace()
        context.getSharedPreferences(namespace, 0).edit()
            .putString("mode", "not-a-mode")
            .putStringSet("packages", setOf("not a package", "", "com.example.browser"))
            .commit()
        val loaded = SplitTunnelStore(context, namespace).load()
        assertEquals(SplitTunnelMode.ONLY_VPN, loaded.mode)
        assertEquals(setOf("com.example.browser"), loaded.packages)
    }

    @Test fun missingStoreIsTheHonestEmptySelection() = runBlocking {
        assertEquals(SplitTunnelSelection(), SplitTunnelStore(context, namespace()).load())
    }
}
