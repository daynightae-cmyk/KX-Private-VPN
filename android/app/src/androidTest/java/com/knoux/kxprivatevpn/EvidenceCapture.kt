package com.knoux.kxprivatevpn

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import java.io.File

/**
 * Writes one runtime screenshot into the app's external evidence folder.
 *
 * Every screenshot in this suite is a byproduct of a behaviour the test body already asserted,
 * so the capture itself must never be the reason a run fails. The hosted API 35 emulator
 * rasterises in software and occasionally misses the frame that [captureToImage] waits for:
 * CI run 42270269107 failed `RepairLabScreenTest` with a redraw timeout inside the capture,
 * not with a wrong value. A bounded retry absorbs that renderer hiccup; a window that truly
 * cannot be drawn still fails the run after the last attempt.
 */
internal fun captureEvidence(
    context: Context,
    interactions: SemanticsNodeInteractionsProvider,
    name: String,
    attempts: Int = 3,
) {
    val folder = File(context.getExternalFilesDir(null), "identity-evidence").apply { mkdirs() }
    var failure: Throwable? = null
    repeat(attempts) { attempt ->
        try {
            val bitmap = interactions.onRoot().captureToImage().asAndroidBitmap()
            File(folder, "$name.png").outputStream().use { stream ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
            }
            return
        } catch (error: Throwable) {
            failure = error
            if (attempt < attempts - 1) Thread.sleep(750)
        }
    }
    throw failure ?: IllegalStateException("evidence capture produced no image for $name")
}
