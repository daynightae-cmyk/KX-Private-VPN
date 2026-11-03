package com.knoux.kxprivatevpn.data.config

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class Profile(val id: String, val name: String, val endpoint: String)
data class ProfileSnapshot(val profiles: List<Profile>, val selectedId: String?)
private data class StoredProfile(val id: String, val name: String, val config: String)

/** App-private atomic ciphertext, including names/endpoints. No keys in preferences or backups. */
class ProfileStore(context: Context, namespace: String = "wireguard-profiles") {
    init { require(namespace.matches(Regex("[a-z0-9_-]{1,64}"))) }
    private val file = AtomicFile(File(context.noBackupFilesDir, "$namespace.kx"))
    private val alias = "kx.$namespace.v1"
    // All store instances in this process serialize read-modify-write.
    private companion object { val mutex = Mutex() }
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        // A ciphertext without its key is an error, never silently replaced.
        check(!file.baseFile.exists()) { "Profile key unavailable" }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).setRandomizedEncryptionRequired(true).build())
        }.generateKey()
    }
    private fun read(): JSONObject {
        if (!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists())
            return JSONObject().put("version", 1).put("profiles", JSONArray())
        val bytes = file.openRead().use { it.readBytes() }
        check(bytes.size in 30..2_000_000 && bytes[0] == 1.toByte()) { "Invalid profile store" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(1, 13)))
        val decoded = cipher.doFinal(bytes.copyOfRange(13, bytes.size))
        return try { JSONObject(decoded.toString(Charsets.UTF_8)).also { check(it.getInt("version") == 1) } }
        finally { decoded.fill(0) }
    }
    private fun write(json: JSONObject) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val plain = json.toString().toByteArray(Charsets.UTF_8)
        val bytes = try { byteArrayOf(1) + cipher.iv + cipher.doFinal(plain) } finally { plain.fill(0) }
        val output = file.startWrite()
        try { output.write(bytes); file.finishWrite(output) }
        catch (e: Exception) { file.failWrite(output); throw e }
    }
    private fun snapshot(json: JSONObject): ProfileSnapshot {
        val array = json.getJSONArray("profiles")
        val profiles = (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            val config = ConfigValidator.parse(item.getString("config").toByteArray())
            Profile(item.getString("id"), item.getString("name"), config.peers.first().endpoint.get().toString())
        }
        return ProfileSnapshot(profiles, json.optString("selected").takeIf { id -> profiles.any { it.id == id } })
    }
    suspend fun load(): ProfileSnapshot = withContext(Dispatchers.IO) { mutex.withLock { snapshot(read()) } }
    suspend fun import(name: String, input: ByteArray): ProfileSnapshot = withContext(Dispatchers.IO) {
        val clean = ConfigValidator.name(name)
        val config = ConfigValidator.parse(input).toWgQuickString()
        mutex.withLock {
            val json = read()
            val array = json.getJSONArray("profiles")
            require(array.length() < 20) { "Profile limit reached" }
            require((0 until array.length()).none { array.getJSONObject(it).getString("name").equals(clean, true) }) {
                "Profile name already exists"
            }
            val id = UUID.randomUUID().toString()
            array.put(JSONObject().put("id", id).put("name", clean).put("config", config))
            if (!json.has("selected")) json.put("selected", id)
            write(json)
            snapshot(json)
        }
    }
    suspend fun select(id: String): ProfileSnapshot = withContext(Dispatchers.IO) { mutex.withLock {
        val json = read()
        require(snapshot(json).profiles.any { it.id == id })
        json.put("selected", id); write(json); snapshot(json)
    } }
    suspend fun remove(id: String): ProfileSnapshot = withContext(Dispatchers.IO) { mutex.withLock {
        val json = read()
        val array = json.getJSONArray("profiles")
        val index = (0 until array.length()).firstOrNull { array.getJSONObject(it).getString("id") == id }
            ?: throw IllegalArgumentException("Unknown profile")
        array.remove(index)
        if (json.optString("selected") == id) json.remove("selected")
        write(json); snapshot(json)
    } }
}
