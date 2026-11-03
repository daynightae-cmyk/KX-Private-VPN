package com.knoux.kxprivatevpn

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.knoux.kxprivatevpn.data.config.ProfileStore
import com.knoux.kxprivatevpn.features.ClientViewModel
import com.knoux.kxprivatevpn.features.ClientIssue
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.Base64
import java.util.UUID

class ConfigImportTest {
    @Test fun fileAndQrImportValidateEncryptAndRejectMissingInput() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val store = ProfileStore(context)
        val original = store.load()
        val prefix = "ci-" + UUID.randomUUID().toString().take(8)
        val key = Base64.getEncoder().encodeToString(ByteArray(32) { (it + 1).toByte() })
        val config = "[Interface]\nPrivateKey = $key\nAddress = 10.7.0.2/32\n" +
            "[Peer]\nPublicKey = $key\nEndpoint = 192.0.2.1:51820\nAllowedIPs = 0.0.0.0/0"
        lateinit var model: ClientViewModel
        instrumentation.runOnMainSync { model = ClientViewModel(context.applicationContext as Application) }
        withTimeout(10000) { model.state.first { it.loaded && !it.busy } }
        try {
            val file = File(context.cacheDir, "$prefix.conf").apply { writeText(config) }
            instrumentation.runOnMainSync { model.importFile("$prefix-file", Uri.fromFile(file), false) }
            val imported = withTimeout(10000) { model.state.first { it.importedId != null && !it.busy } }
            assertEquals("$prefix-file", imported.snapshot.profiles.last().name)
            instrumentation.runOnMainSync { model.consumeImport() }
            val qr = QRCodeWriter().encode(config, BarcodeFormat.QR_CODE, 720, 720)
            val bitmap = Bitmap.createBitmap(720, 720, Bitmap.Config.ARGB_8888)
            for (y in 0 until 720) for (x in 0 until 720) bitmap.setPixel(x, y, if (qr[x, y]) -0x1000000 else -1)
            val image = File(context.cacheDir, "$prefix.png")
            image.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
            instrumentation.runOnMainSync { model.importFile("$prefix-qr", Uri.fromFile(image), true) }
            val decoded = withTimeout(10000) { model.state.first { it.importedId != null && !it.busy } }
            assertEquals("$prefix-qr", decoded.snapshot.profiles.last().name)
            instrumentation.runOnMainSync {
                model.consumeImport()
                model.importFile("missing", Uri.parse("content://com.knoux.nonexistent/config"), false)
            }
            val failed = withTimeout(10000) { model.state.first { it.issue != null && !it.busy } }
            assertEquals(ClientIssue.INPUT, failed.issue)
            assertEquals(original.profiles.size + 2, store.load().profiles.size)
        } finally {
            store.load().profiles.filter { it.name.startsWith(prefix) }.forEach { store.remove(it.id) }
            original.selectedId?.let { store.select(it) }
        }
    }
}
