package com.knoux.kxprivatevpn

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.platform.media.CompressionOutcome
import com.knoux.kxprivatevpn.platform.media.ImageCompressor
import com.knoux.kxprivatevpn.platform.media.MediaCandidate
import com.knoux.kxprivatevpn.platform.media.MediaFailure
import com.knoux.kxprivatevpn.platform.media.MediaQuality
import com.knoux.kxprivatevpn.platform.media.SlimGateway
import com.knoux.kxprivatevpn.platform.media.SlimOutput
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlin.random.Random

/**
 * Real Bitmap/WebP encode and decode behavior is Android-only. The fake gateway isolates the
 * compressor from the folder picker; every byte still goes through the real platform codec.
 */
class MediaSlimCompressorTest {
    private class FakeOutput(
        override val documentId: String,
        override val displayName: String,
    ) : SlimOutput {
        val sink = ByteArrayOutputStream()
        var discarded = false
        var failWrite = false
        var failRead = false

        override fun openWrite(): OutputStream {
            if (failWrite) throw IOException("write refused")
            return sink
        }

        override fun openRead(): InputStream {
            if (failRead) throw CancellationException("cancelled while verifying")
            return ByteArrayInputStream(sink.toByteArray())
        }
        override suspend fun measuredBytes(): Long = sink.size().toLong()
    }

    private class FakeGateway(private val source: ByteArray) : SlimGateway {
        val outputs = mutableListOf<FakeOutput>()
        val readIds = mutableListOf<String>()
        val parents = mutableListOf<String>()
        var failCreate = false
        var failWrite = false

        override suspend fun openRead(documentId: String): InputStream {
            readIds += documentId
            return ByteArrayInputStream(source)
        }

        override suspend fun createOutput(
            parentDocumentId: String,
            displayName: String,
            mime: String,
        ): SlimOutput {
            if (failCreate) throw IOException("create refused")
            parents += parentDocumentId
            return FakeOutput("out-${outputs.size}", displayName).also {
                it.failWrite = failWrite
                outputs += it
            }
        }

        override suspend fun discard(output: SlimOutput) {
            (output as FakeOutput).discarded = true
        }
    }

    private fun noisyJpeg(width: Int, height: Int, quality: Int = 100): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val random = Random(7)
        val pixels = IntArray(width * height) {
            val r = random.nextInt(256)
            val g = random.nextInt(256)
            val b = random.nextInt(256)
            (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        val bytes = ByteArrayOutputStream().use { stream ->
            assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream))
            stream.toByteArray()
        }
        bitmap.recycle()
        return bytes
    }

    private fun orientedJpeg(width: Int, height: Int, orientation: Int): ByteArray {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "kx-exif-${UUID.randomUUID()}.jpg")
        try {
            file.writeBytes(noisyJpeg(width, height, quality = 95))
            val exif = ExifInterface(file.absolutePath)
            exif.setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
            exif.saveAttributes()
            return file.readBytes()
        } finally {
            assertTrue(!file.exists() || file.delete())
        }
    }

    private fun candidate(bytes: ByteArray, mime: String = "image/jpeg") = MediaCandidate(
        documentId = "img",
        parentDocumentId = "folder",
        relativePath = "Camera/IMG_0001.jpg",
        name = "IMG_0001.jpg",
        mime = mime,
        bytes = bytes.size.toLong(),
        lastModified = 0,
    )

    @Test fun realWebpCopyIsMeasuredVerifiedAndSmaller() = runBlocking {
        val source = noisyJpeg(1200, 900)
        val gateway = FakeGateway(source)
        val value = ImageCompressor(gateway).compress(candidate(source), MediaQuality.BALANCED)

        val verified = value as? CompressionOutcome.Verified
            ?: fail("expected verified copy, got $value") as Nothing
        assertTrue("WebP must be smaller than the JPEG source",
            verified.value.outputBytes < source.size)
        assertEquals(1200, verified.value.width)
        assertEquals(900, verified.value.height)
        assertEquals("folder", gateway.parents.single())
        assertEquals(listOf("img", "img", "img"), gateway.readIds)
        assertFalse(gateway.outputs.single().discarded)
        assertTrue(verified.value.outputName.endsWith(".webp"))
        assertTrue(verified.value.outputName.startsWith("IMG_0001-kx-q65-"))
        // The reported copy really decodes outside the compressor as well.
        val decoded = BitmapFactory.decodeByteArray(gateway.outputs.single().sink.toByteArray(), 0,
            gateway.outputs.single().sink.size())
        assertNotNull(decoded)
        assertEquals(1200, decoded!!.width)
        assertEquals(900, decoded.height)
        decoded.recycle()
    }

    @Test fun exifRotationIsBakedIntoTheCopy() = runBlocking {
        val source = orientedJpeg(600, 300, ExifInterface.ORIENTATION_ROTATE_90)
        val gateway = FakeGateway(source)
        val value = ImageCompressor(gateway).compress(candidate(source), MediaQuality.HIGH)

        val verified = value as? CompressionOutcome.Verified
            ?: fail("expected verified copy, got $value") as Nothing
        assertEquals(300, verified.value.width)
        assertEquals(600, verified.value.height)
    }

    @Test fun copyThatIsNotSmallerIsDeletedAndReported() = runBlocking {
        val source = noisyJpeg(320, 240)
        val gateway = FakeGateway(source)
        val tinySource = candidate(source).copy(bytes = 1)

        val value = ImageCompressor(gateway).compress(tinySource, MediaQuality.SMALL)

        assertEquals(CompressionOutcome.Failed(tinySource, MediaFailure.NO_SAVING), value)
        assertTrue(gateway.outputs.single().discarded)
    }

    @Test fun failedWriteAndFailedCreateNeverLeaveAReceipt() = runBlocking {
        val source = noisyJpeg(320, 240)
        val writeGateway = FakeGateway(source).apply { failWrite = true }
        val write = ImageCompressor(writeGateway).compress(candidate(source), MediaQuality.BALANCED)
        assertEquals(CompressionOutcome.Failed(candidate(source), MediaFailure.WRITE_FAILED), write)
        assertTrue(writeGateway.outputs.single().discarded)

        val createGateway = FakeGateway(source).apply { failCreate = true }
        val create = ImageCompressor(createGateway).compress(candidate(source), MediaQuality.BALANCED)
        assertEquals(CompressionOutcome.Failed(candidate(source), MediaFailure.CREATE_FAILED), create)
        assertTrue(createGateway.outputs.isEmpty())
    }

    @Test fun unreadableAndUnsupportedSourcesNeverCreateOutput() = runBlocking {
        val garbage = FakeGateway("not-an-image".toByteArray())
        val corrupt = ImageCompressor(garbage)
            .compress(candidate("not-an-image".toByteArray()), MediaQuality.BALANCED)
        assertEquals(CompressionOutcome.Failed(candidate("not-an-image".toByteArray()),
            MediaFailure.UNREADABLE), corrupt)
        assertTrue(garbage.outputs.isEmpty())

        val videoGateway = FakeGateway(noisyJpeg(100, 100))
        val videoBytes = noisyJpeg(100, 100)
        val video = ImageCompressor(videoGateway)
            .compress(candidate(videoBytes, mime = "video/mp4"), MediaQuality.BALANCED)
        assertEquals(CompressionOutcome.Failed(candidate(videoBytes, mime = "video/mp4"),
            MediaFailure.UNREADABLE), video)
        assertTrue(videoGateway.outputs.isEmpty())
        assertTrue(videoGateway.readIds.isEmpty())
    }

    @Test fun coroutineCancellationAfterCopyCreationDiscardsThePartialCopy() {
        val source = noisyJpeg(640, 480)
        var discarded = false
        val gateway = object : SlimGateway {
            override suspend fun openRead(documentId: String): InputStream =
                ByteArrayInputStream(source)

            override suspend fun createOutput(
                parentDocumentId: String,
                displayName: String,
                mime: String,
            ): SlimOutput = FakeOutput("partial", displayName).apply { failRead = true }

            override suspend fun discard(output: SlimOutput) {
                discarded = true
            }
        }

        try {
            runBlocking {
                ImageCompressor(gateway).compress(candidate(source), MediaQuality.BALANCED)
            }
            fail("cancellation must propagate")
        } catch (_: CancellationException) {
            // Expected: the ViewModel marks the run cancelled, never verified.
        }
        assertTrue("a partial copy must be deleted on cancellation", discarded)
    }
}
