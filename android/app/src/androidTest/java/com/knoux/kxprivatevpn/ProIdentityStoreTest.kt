package com.knoux.kxprivatevpn

import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.platform.security.ProIdentityStore
import com.knoux.kxprivatevpn.platform.security.isProHex
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.security.KeyStore
import java.util.UUID

/** Every test uses a fresh UUID namespace. Never reads, mutates or tests the production store. */
class ProIdentityStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val namespace = UUID.randomUUID()
    private val file get() = File(context.noBackupFilesDir, "pro-test-$namespace.kx")
    private val alias get() = "kx.pro-test-$namespace.aes256gcm.v1"
    private fun store() = ProIdentityStore(context, namespace)
    private fun keys() = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    @After fun removeOnlyThisTestFixture() {
        listOf(file, File(file.path + ".bak"), File(file.path + ".new")).forEach { it.delete() }
        keys().deleteEntry(alias)
    }

    @Test fun concurrentInstancesReadBackOneEncryptedIdentity() = runBlocking {
        val identities = List(8) { async { store().load() } }.awaitAll()
        assertEquals(1, identities.map { it.secret }.toSet().size)
        val identity = identities.first()
        assertTrue(isProHex(identity.secret))
        assertTrue(isProHex(identity.subject))
        assertEquals(identity.secret, store().load().secret)
        assertEquals(93L, file.length())
        val ciphertext = file.readBytes().toString(Charsets.ISO_8859_1)
        assertFalse(ciphertext.contains(identity.secret))
        assertFalse(ciphertext.contains(identity.subject))
        assertTrue(keys().containsAlias(alias))
    }

    @Test fun corruptionDoesNotRegenerateOrOverwrite() = runBlocking {
        store().load()
        val broken = file.readBytes().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() }
        file.writeBytes(broken)
        denied()
        assertArrayEquals(broken, file.readBytes())
    }

    @Test fun missingKeyWithCiphertextFailsClosed() = runBlocking {
        store().load()
        val before = file.readBytes()
        keys().deleteEntry(alias)
        denied()
        assertFalse(keys().containsAlias(alias))
        assertArrayEquals(before, file.readBytes())
    }

    @Test fun backupOnlyWithMissingKeyCannotBeMistakenForFreshInstall() = runBlocking {
        store().load()
        val backup = File(file.path + ".bak")
        assertTrue(file.renameTo(backup))
        keys().deleteEntry(alias)
        denied()
        assertTrue(backup.exists())
        assertFalse(keys().containsAlias(alias))
    }

    @Test fun validAtomicBackupRecoversSameIdentity() = runBlocking {
        val original = store().load()
        assertTrue(file.renameTo(File(file.path + ".bak")))
        assertEquals(original.secret, store().load().secret)
    }

    @Test fun lostCiphertextWithSurvivingKeyDoesNotRotateIdentity() = runBlocking {
        store().load()
        assertTrue(file.delete())
        denied()
        assertFalse(file.exists())
        assertTrue(keys().containsAlias(alias))
    }

    @Test fun oversizedOrTruncatedFileIsRejectedUnchanged() = runBlocking {
        store().load()
        for (size in listOf(0, 29, 512, 513, 10_000)) {
            val bytes = ByteArray(size) { 1 }
            file.writeBytes(bytes)
            denied()
            assertArrayEquals(bytes, file.readBytes())
        }
    }

    @Test fun orphanAtomicNewFileDoesNotCreateNewKey() = runBlocking {
        val orphan = File(file.path + ".new")
        orphan.writeBytes(byteArrayOf(1, 2, 3))
        denied()
        assertFalse(keys().containsAlias(alias))
        assertTrue(orphan.exists())
    }

    private suspend fun denied() {
        try { store().load(); fail("Damaged identity must fail closed") }
        catch (_: Exception) { /* Expected; secret and exception details must not be logged. */ }
    }
}
