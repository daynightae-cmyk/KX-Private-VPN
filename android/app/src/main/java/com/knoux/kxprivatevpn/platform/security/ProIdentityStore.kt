package com.knoux.kxprivatevpn.platform.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal fun interface ProIdentitySource { suspend fun load(): ProIdentity }

/** Deliberately not a data class: diagnostics must never print the installation credential. */
internal class ProIdentity(val secret: String) {
    init { require(isProHex(secret)) }
    val subject: String = MessageDigest.getInstance("SHA-256")
        .digest(secret.toByteArray(Charsets.US_ASCII)).lowerHex()
    override fun toString() = "ProIdentity([redacted])"
}

internal fun isProHex(value: String) = value.length == 64 && value.all { it in '0'..'9' || it in 'a'..'f' }
internal fun ByteArray.lowerHex(): String = joinToString("") { "%02x".format(it.toInt() and 255) }
internal fun newProNonce(): String {
    val random = ByteArray(32).also(SecureRandom()::nextBytes)
    return try { random.lowerHex() } finally { random.fill(0) }
}

/** Separate from the profile vault. Neither ciphertext nor plaintext participates in backup. */
internal class ProIdentityStore private constructor(context: Context, namespace: String) : ProIdentitySource {
    constructor(context: Context) : this(context, "pro-installation")

    /** Only androidTests use a fresh UUID; never point a test at the production namespace. */
    @VisibleForTesting
    internal constructor(context: Context, testNamespace: UUID) : this(context, "pro-test-$testNamespace")

    private val file = AtomicFile(File(context.noBackupFilesDir, "$namespace.kx"))
    private val alias = "kx.$namespace.aes256gcm.v1"

    override suspend fun load(): ProIdentity = withContext(Dispatchers.IO) {
        mutex.withLock {
            val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val exists = file.baseFile.exists() || File(file.baseFile.path + ".bak").exists() ||
                File(file.baseFile.path + ".new").exists()
            if (exists) {
                val key = store.getKey(alias, null) as? SecretKey ?: error("Pro identity unavailable")
                read(key)
            } else {
                // A lost file with a surviving key is not a new installation. Never rotate silently.
                check(!store.containsAlias(alias)) { "Pro identity unavailable" }
                val key = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
                    init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256).setRandomizedEncryptionRequired(true).build())
                }.generateKey()
                val secret = newProNonce()
                val plain = secret.toByteArray(Charsets.US_ASCII)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key) }
                val encrypted = try { byteArrayOf(1) + cipher.iv + cipher.doFinal(plain) } finally { plain.fill(0) }
                val output = file.startWrite()
                try {
                    output.write(encrypted)
                    output.fd.sync()
                    file.finishWrite(output)
                } catch (e: Exception) {
                    file.failWrite(output)
                    throw e
                }
                read(key).also { check(it.secret == secret) { "Pro identity verification failed" } }
            }
        }
    }

    private fun read(key: SecretKey): ProIdentity {
        // Bound the read even if a corrupt file lies about its length. openRead restores .bak.
        val bytes = file.openRead().use { input ->
            val buffer = ByteArray(513)
            var size = 0
            while (size < buffer.size) {
                val count = input.read(buffer, size, buffer.size - size)
                if (count < 0) break
                size += count
            }
            check(size <= 512) { "Pro identity unavailable" }
            buffer.copyOf(size)
        }
        check(bytes.size == 93 && bytes[0] == 1.toByte()) { "Pro identity unavailable" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, bytes.copyOfRange(1, 13)))
        val plain = cipher.doFinal(bytes.copyOfRange(13, bytes.size))
        return try {
            check(plain.size == 64 && plain.all { it.toInt() in 48..57 || it.toInt() in 97..102 })
            ProIdentity(plain.toString(Charsets.US_ASCII))
        } finally { plain.fill(0) }
    }

    private companion object { val mutex = Mutex() }
}
