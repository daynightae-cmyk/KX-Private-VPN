package com.knoux.kxprivatevpn.features

import android.app.Application
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.knoux.kxprivatevpn.data.config.*
import com.knoux.kxprivatevpn.launch.LaunchPreferences
import com.knoux.kxprivatevpn.launch.UiPreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

enum class ClientIssue { INVALID_CONFIG, INPUT, STORE, SAVE }
private class InputUnavailable : Exception("Input unavailable")
data class ClientState(
    val loaded: Boolean = false, val busy: Boolean = false,
    val snapshot: ProfileSnapshot = ProfileSnapshot(emptyList(), null),
    val settings: UiPreferences = UiPreferences(),
    val issue: ClientIssue? = null, val importedId: String? = null,
)

class ClientViewModel(app: Application) : AndroidViewModel(app) {
    private val store = ProfileStore(app)
    private val preferences = LaunchPreferences(app)
    private val mutable = MutableStateFlow(ClientState())
    val state: StateFlow<ClientState> = mutable.asStateFlow()
    init { reload() }
    fun reload() {
        if (mutable.value.busy) return
        viewModelScope.launch {
            mutable.update { it.copy(busy = true, issue = null) }
            try {
                val profiles = store.load()
                val settings = preferences.settings()
                mutable.update { it.copy(loaded = true, snapshot = profiles, settings = settings) }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutable.update { it.copy(issue = ClientIssue.STORE) } }
            finally { mutable.update { it.copy(busy = false) } }
        }
    }
    fun dismissIssue() { mutable.update { it.copy(issue = null) } }
    fun consumeImport() { mutable.update { it.copy(importedId = null) } }
    fun inputError() { mutable.update { it.copy(issue = ClientIssue.INPUT) } }
    private fun operation(action: suspend () -> Unit) {
        if (mutable.value.busy || !mutable.value.loaded) return
        mutable.update { it.copy(busy = true, issue = null) }
        viewModelScope.launch {
            try { action() }
            catch (_: TimeoutCancellationException) { mutable.update { it.copy(issue = ClientIssue.INPUT) } }
            catch (_: InputUnavailable) { mutable.update { it.copy(issue = ClientIssue.INPUT) } }
            catch (e: CancellationException) { throw e }
            catch (_: InvalidConfiguration) { mutable.update { it.copy(issue = ClientIssue.INVALID_CONFIG) } }
            catch (_: IllegalArgumentException) { mutable.update { it.copy(issue = ClientIssue.INVALID_CONFIG) } }
            catch (_: Exception) { mutable.update { it.copy(issue = ClientIssue.SAVE) } }
            finally { mutable.update { it.copy(busy = false) } }
        }
    }
    fun onboard() = operation {
        preferences.completeOnboarding()
        mutable.update { it.copy(settings = it.settings.copy(onboarded = true)) }
    }
    fun appearance(language: String, theme: String) = operation {
        preferences.appearance(language, theme)
        mutable.update { it.copy(settings = it.settings.copy(language = language, theme = theme)) }
    }
    fun select(id: String) = operation { val value = store.select(id); mutable.update { it.copy(snapshot = value) } }
    fun remove(id: String) = operation { val value = store.remove(id); mutable.update { it.copy(snapshot = value) } }
    private suspend fun save(name: String, bytes: ByteArray) {
        val value = try { store.import(name, bytes) } finally { bytes.fill(0) }
        mutable.update { it.copy(snapshot = value, importedId = value.profiles.last().id) }
    }
    fun importText(name: String, input: String) = operation { save(name, input.toByteArray(Charsets.UTF_8)) }
    fun importFile(name: String, uri: Uri, qr: Boolean) = operation {
        val bytes = try { withTimeout(15_000) { runInterruptible(Dispatchers.IO) {
            val resolver = getApplication<Application>().contentResolver
            if (!qr) resolver.openInputStream(uri)?.use { ConfigInput.read(it) } ?: throw InvalidConfiguration()
            else {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                if (options.outWidth !in 1..12000 || options.outHeight !in 1..12000) throw InvalidConfiguration()
                var sample = 1
                while (maxOf(options.outWidth, options.outHeight) / sample > 1600) sample *= 2
                options.inSampleSize = sample; options.inJustDecodeBounds = false
                val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                    ?: throw InvalidConfiguration()
                try {
                    val pixels = IntArray(bitmap.width * bitmap.height)
                    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                    val source = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
                    MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(source))).text.toByteArray(Charsets.UTF_8)
                } catch (_: Exception) { throw InvalidConfiguration() }
                finally { bitmap.recycle() }
            }
        } } } catch (e: CancellationException) { throw e }
        catch (e: InvalidConfiguration) { throw e }
        catch (_: Exception) { throw InputUnavailable() }
        save(name, bytes)
    }
}
