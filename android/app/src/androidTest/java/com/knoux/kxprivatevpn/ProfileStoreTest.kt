package com.knoux.kxprivatevpn

import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.data.config.ConfigValidator
import com.knoux.kxprivatevpn.data.config.ProfileStore
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.Base64
import java.util.UUID

class ProfileStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val key = Base64.getEncoder().encodeToString(ByteArray(32) { (it + 1).toByte() })
    private val config = "[Interface]\nPrivateKey = $key\nAddress = 10.7.0.2/32\n" +
        "[Peer]\nPublicKey = $key\nEndpoint = 192.0.2.1:51820\nAllowedIPs = 0.0.0.0/0"
    private fun namespace() = "test_" + UUID.randomUUID().toString().replace("-", "")
    @Test fun ciphertextPersistsWithoutPlaintextAndRemovalIsScoped() = runBlocking {
        val namespace = namespace()
        val store = ProfileStore(context, namespace)
        val first = store.import("first", config.toByteArray()).profiles.first()
        store.import("sentinel", config.toByteArray())
        val bytes = File(context.noBackupFilesDir, "$namespace.kx").readBytes()
        assertFalse(bytes.toString(Charsets.ISO_8859_1).contains(key))
        assertFalse(bytes.toString(Charsets.ISO_8859_1).contains("sentinel"))
        val reloaded = ProfileStore(context, namespace).load()
        assertEquals(2, reloaded.profiles.size)
        assertEquals(first.id, reloaded.selectedId)
        val removed = store.remove(first.id)
        assertEquals("sentinel", removed.profiles.single().name)
        assertNull(removed.selectedId)
    }
    @Test fun duplicateAndTamperNeverOverwriteExistingData() = runBlocking {
        val namespace = namespace()
        val store = ProfileStore(context, namespace)
        store.import("first", config.toByteArray())
        val file = File(context.noBackupFilesDir, "$namespace.kx")
        val initial = file.readBytes()
        try { store.import("FIRST", config.toByteArray()); fail() } catch (_: IllegalArgumentException) { }
        assertArrayEquals(initial, file.readBytes())
        val damaged = initial.copyOf().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() }
        file.writeBytes(damaged) // Explicit isolated test fixture only.
        try { store.import("next", config.toByteArray()); fail() } catch (_: Exception) { }
        assertArrayEquals(damaged, file.readBytes())
    }
    @Test fun storedConfigIsReadableThroughDecryptionOnly() = runBlocking {
        val namespace = namespace()
        val store = ProfileStore(context, namespace)
        val profile = store.import("tunnel", config.toByteArray()).profiles.single()
        // The store persists the validated, canonical WireGuard serialization, not the raw
        // input order. Prove the decrypted bytes still describe the same tunnel.
        val decrypted = store.config(profile.id) ?: error("Stored config is missing")
        val reloaded = ConfigValidator.parse(decrypted)
        assertEquals("10.7.0.2/32", reloaded.`interface`.addresses.single().toString())
        assertEquals("192.0.2.1:51820", reloaded.peers.single().endpoint.get().toString())
        assertEquals(key, reloaded.`interface`.keyPair.privateKey.toBase64())
        assertNull(store.config("missing"))
    }
    @Test fun concurrentStoresPreserveEveryUniqueProfile() = runBlocking {
        val namespace = namespace()
        coroutineScope { (1..6).map { index -> async(Dispatchers.IO) {
            ProfileStore(context, namespace).import("profile-$index", config.toByteArray())
        } }.awaitAll() }
        assertEquals(6, ProfileStore(context, namespace).load().profiles.size)
    }
}
