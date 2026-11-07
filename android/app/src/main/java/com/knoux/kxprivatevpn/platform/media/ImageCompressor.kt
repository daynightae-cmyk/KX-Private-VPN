package com.knoux.kxprivatevpn.platform.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/** One provider document created by the compressor. The original is never passed here. */
interface SlimOutput {
    val documentId: String
    val displayName: String
    fun openWrite(): OutputStream
    fun openRead(): InputStream
    suspend fun measuredBytes(): Long?
}

/** Narrow SAF seam so the real compressor can be tested on-device without a folder picker. */
interface SlimGateway {
    suspend fun openRead(documentId: String): InputStream
    suspend fun createOutput(parentDocumentId: String, displayName: String, mime: String): SlimOutput
    suspend fun discard(output: SlimOutput)
}

enum class MediaFailure {
    UNREADABLE,
    CREATE_FAILED,
    WRITE_FAILED,
    EMPTY_OUTPUT,
    VERIFY_FAILED,
    NO_SAVING,
}

data class VerifiedCompression(
    val candidate: MediaCandidate,
    val outputDocumentId: String,
    val outputName: String,
    val outputBytes: Long,
    val width: Int,
    val height: Int,
) {
    val savedBytes: Long get() = (candidate.bytes - outputBytes).coerceAtLeast(0)
}

sealed interface CompressionOutcome {
    data class Verified(val value: VerifiedCompression) : CompressionOutcome
    data class Failed(val candidate: MediaCandidate, val reason: MediaFailure) : CompressionOutcome
}

/**
 * Re-encodes one user-selected image into a NEW document in the same granted tree, verifies that
 * the copy decodes, then reports the measured bytes. The source document is only ever opened
 * read-only; a failed/smaller-than-source copy is deleted and no receipt is fabricated.
 */
class ImageCompressor(private val gateway: SlimGateway) {
    suspend fun compress(candidate: MediaCandidate, quality: MediaQuality): CompressionOutcome =
        withContext(Dispatchers.IO) {
            if (!candidate.compressible) {
                return@withContext CompressionOutcome.Failed(candidate, MediaFailure.UNREADABLE)
            }
            val source = try {
                decodeOriented(candidate.documentId)
            } catch (e: CancellationException) {
                throw e
            } catch (_: OutOfMemoryError) {
                return@withContext CompressionOutcome.Failed(candidate, MediaFailure.UNREADABLE)
            } catch (_: Exception) {
                return@withContext CompressionOutcome.Failed(candidate, MediaFailure.UNREADABLE)
            }
            var output: SlimOutput? = null
            try {
                val name = outputName(candidate.name, quality)
                output = try {
                    gateway.createOutput(candidate.parentDocumentId, name, MIME_WEBP)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    return@withContext CompressionOutcome.Failed(candidate, MediaFailure.CREATE_FAILED)
                }
                val written = try {
                    output.openWrite().use { stream ->
                        source.bitmap.compress(format(), quality.percent, stream)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    false
                }
                if (!written) {
                    output.discardQuietly()
                    return@withContext CompressionOutcome.Failed(candidate, MediaFailure.WRITE_FAILED)
                }
                val bytes = try { output.measuredBytes() } catch (_: Exception) { null }
                if (bytes == null || bytes <= 0) {
                    output.discardQuietly()
                    return@withContext CompressionOutcome.Failed(candidate, MediaFailure.EMPTY_OUTPUT)
                }
                val bounds = try {
                    BitmapFactory.Options().apply { inJustDecodeBounds = true }.also { options ->
                        output.openRead().use { BitmapFactory.decodeStream(it, null, options) }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    null
                }
                if (bounds == null || bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                    output.discardQuietly()
                    return@withContext CompressionOutcome.Failed(candidate, MediaFailure.VERIFY_FAILED)
                }
                // Bounds only parse the header. Decode the copy itself at a bounded sample so a
                // truncated payload cannot be reported as a verified output.
                val decodable = try {
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = verificationSample(bounds.outWidth, bounds.outHeight)
                    }
                    output.openRead().use { BitmapFactory.decodeStream(it, null, options) }
                        ?.also { it.recycle() } != null
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    false
                } catch (_: OutOfMemoryError) {
                    false
                }
                if (!decodable) {
                    output.discardQuietly()
                    return@withContext CompressionOutcome.Failed(candidate, MediaFailure.VERIFY_FAILED)
                }
                if (bytes >= candidate.bytes) {
                    output.discardQuietly()
                    return@withContext CompressionOutcome.Failed(candidate, MediaFailure.NO_SAVING)
                }
                CompressionOutcome.Verified(VerifiedCompression(
                    candidate = candidate,
                    outputDocumentId = output.documentId,
                    outputName = output.displayName,
                    outputBytes = bytes,
                    width = bounds.outWidth,
                    height = bounds.outHeight,
                ))
            } catch (e: CancellationException) {
                output?.discardQuietly()
                throw e
            } catch (_: OutOfMemoryError) {
                output?.discardQuietly()
                CompressionOutcome.Failed(candidate, MediaFailure.UNREADABLE)
            } catch (_: Exception) {
                output?.discardQuietly()
                CompressionOutcome.Failed(candidate, MediaFailure.WRITE_FAILED)
            } finally {
                source.bitmap.recycle()
            }
        }

    private suspend fun decodeOriented(documentId: String): OrientedBitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        gateway.openRead(documentId).use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("not an image")
        var sample = 1
        while (bounds.outWidth / sample > MAX_EDGE || bounds.outHeight / sample > MAX_EDGE) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bitmap = gateway.openRead(documentId).use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: throw IOException("image could not be decoded")
        val orientation = try {
            gateway.openRead(documentId).use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL,
                )
            }
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
        val matrix = orientationMatrix(orientation)
        if (matrix == null) return OrientedBitmap(bitmap)
        val rotated = try {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (_: OutOfMemoryError) {
            bitmap.recycle()
            throw IOException("rotation failed")
        }
        if (rotated !== bitmap) bitmap.recycle()
        return OrientedBitmap(rotated)
    }

    private fun outputName(original: String, quality: MediaQuality): String {
        val base = original.substringBeforeLast('.', original)
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .trim('_', '.')
            .take(48)
            .ifBlank { "kx-image" }
        // A unique suffix keeps repeated runs from colliding with an earlier verified copy.
        val stamp = System.nanoTime().toString(36).takeLast(6)
        return "$base-kx-q${quality.percent}-$stamp.webp"
    }

    private fun verificationSample(width: Int, height: Int): Int {
        var sample = 1
        while (width / sample > VERIFY_EDGE || height / sample > VERIFY_EDGE) sample *= 2
        return sample
    }

    private fun format(): Bitmap.CompressFormat =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSY
        else @Suppress("DEPRECATION") Bitmap.CompressFormat.WEBP

    private suspend fun SlimOutput.discardQuietly() {
        withContext(NonCancellable) {
            try { gateway.discard(this@discardQuietly) } catch (_: Exception) { }
        }
    }

    private data class OrientedBitmap(val bitmap: Bitmap)

    companion object {
        const val MAX_EDGE = 2560
        const val VERIFY_EDGE = 1024
        const val MIME_WEBP = "image/webp"

        /** EXIF orientation is baked into the copy so the new file opens upright. */
        fun orientationMatrix(orientation: Int): Matrix? = when (orientation) {
            ExifInterface.ORIENTATION_NORMAL, ExifInterface.ORIENTATION_UNDEFINED -> null
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> Matrix().apply { postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_180 -> Matrix().apply { postRotate(180f) }
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> Matrix().apply { postScale(1f, -1f) }
            ExifInterface.ORIENTATION_TRANSPOSE -> Matrix().apply {
                postRotate(90f); postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> Matrix().apply { postRotate(90f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> Matrix().apply {
                postRotate(270f); postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> Matrix().apply { postRotate(270f) }
            else -> null
        }
    }
}

/** Real DocumentsContract gateway. It only addresses the one persisted user-granted tree. */
class AndroidMediaGateway(
    private val context: Context,
    private val treeUri: Uri,
) : SlimGateway {
    override suspend fun openRead(documentId: String): InputStream =
        context.contentResolver.openInputStream(documentUri(documentId))
            ?: throw IOException("document is not readable")

    override suspend fun createOutput(
        parentDocumentId: String,
        displayName: String,
        mime: String,
    ): SlimOutput {
        val parent = documentUri(parentDocumentId)
        val created = DocumentsContract.createDocument(
            context.contentResolver, parent, mime, displayName,
        ) ?: throw IOException("provider refused to create output")
        return AndroidSlimOutput(context, created, displayName)
    }

    override suspend fun discard(output: SlimOutput) {
        if (output !is AndroidSlimOutput) return
        try {
            DocumentsContract.deleteDocument(context.contentResolver, output.uri)
        } catch (_: Exception) {
            // A failed cleanup is reported by the caller as a failed file, never as success.
        }
    }

    private fun documentUri(documentId: String): Uri =
        DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)

    private class AndroidSlimOutput(
        private val context: Context,
        val uri: Uri,
        override val displayName: String,
    ) : SlimOutput {
        override val documentId: String = DocumentsContract.getDocumentId(uri)

        override fun openWrite(): OutputStream =
            context.contentResolver.openOutputStream(uri)
                ?: throw IOException("output is not writable")

        override fun openRead(): InputStream =
            context.contentResolver.openInputStream(uri)
                ?: throw IOException("output is not readable")

        override suspend fun measuredBytes(): Long? {
            val projection = arrayOf(DocumentsContract.Document.COLUMN_SIZE)
            return context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else null
            }
        }
    }
}
