package com.knoux.kxprivatevpn

import com.knoux.kxprivatevpn.data.config.*
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.Base64

class ConfigValidatorTest {
    private val key = Base64.getEncoder().encodeToString(ByteArray(32) { (it + 1).toByte() })
    private val valid = """
        [Interface]
        PrivateKey = $key
        Address = 10.7.0.2/32
        [Peer]
        PublicKey = $key
        Endpoint = 192.0.2.1:51820
        AllowedIPs = 0.0.0.0/0, ::/0
    """.trimIndent()
    @Test fun parsesActualWireGuardModel() {
        val config = ConfigValidator.parse(valid.toByteArray())
        assertEquals(1, config.peers.size)
        assertEquals(2, config.peers.first().allowedIps.size)
    }
    @Test fun rejectsUnusableAndUnsafeInputs() {
        listOf("", "[Interface]\nPrivateKey = secret", valid.replace("Address = 10.7.0.2/32", ""),
            valid.replace("Endpoint = 192.0.2.1:51820", ""),
            valid.replace("AllowedIPs = 0.0.0.0/0, ::/0", ""), valid + "\nPostUp = do something").forEach {
            try { ConfigValidator.parse(it.toByteArray()); fail("Input should be rejected") }
            catch (e: InvalidConfiguration) { assertFalse(e.toString().contains(key)) }
        }
    }
    @Test fun boundsInputAndRejectsNul() {
        listOf(ByteArray(65537) { 65 }, byteArrayOf(0)).forEach {
            try { ConfigValidator.parse(it); fail() } catch (_: InvalidConfiguration) { }
        }
        try { ConfigInput.read(ByteArrayInputStream(ByteArray(65537))); fail() }
        catch (_: InvalidConfiguration) { }
        assertEquals(65536, ConfigInput.read(ByteArrayInputStream(ByteArray(65536))).size)
    }
    @Test fun validatesProfileNameWithoutBidiOverrides() {
        assertEquals("خادم شخصي", ConfigValidator.name(" خادم شخصي "))
        listOf("", "x".repeat(49), "profile\nnext", "profile\u202Econf").forEach {
            try { ConfigValidator.name(it); fail() } catch (_: IllegalArgumentException) { }
        }
    }
}
