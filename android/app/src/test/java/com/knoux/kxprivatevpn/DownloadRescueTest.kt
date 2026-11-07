package com.knoux.kxprivatevpn

import com.knoux.kxprivatevpn.platform.repair.DownloadFormat
import com.knoux.kxprivatevpn.platform.storage.DownloadCollections
import com.knoux.kxprivatevpn.platform.repair.DownloadHealth
import com.knoux.kxprivatevpn.platform.repair.DownloadValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Download Rescue may only claim what the bytes prove. These tests pin the classification of
 * every supported format and, just as importantly, the cases where no claim is allowed.
 */
class DownloadRescueTest {

    private fun bytes(text: String) = text.toByteArray(Charsets.US_ASCII)

    private val pdfHead = bytes("%PDF-1.7\n%binary")
    private val pdfTail = bytes("end\n%%EOF\n")
    private val pngHead = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3)
    private val pngTail = bytes("xxIEND") + byteArrayOf(0xAE.toByte(), 0x42, 0x60, 0x82.toByte())
    private val jpegHead = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0, 1)
    private val jpegTail = byteArrayOf(0, 1, 0xFF.toByte(), 0xD9.toByte())
    private val zipHead = byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0, 0)
    private val zipTail = byteArrayOf(0, 0, 0x50, 0x4B, 0x05, 0x06, 0, 0)
    private val mp4Head = byteArrayOf(0, 0, 0, 24) + bytes("ftypisom") + byteArrayOf(0, 0, 0, 0)

    @Test fun zeroByteDownloadIsReportedAsEmpty() {
        val result = DownloadValidator.classify("application/pdf", "empty.pdf", 0L, pdfHead, pdfTail)
        assertEquals(DownloadHealth.ZERO_BYTE, result.health)
        assertEquals("no_bytes", result.reason)
    }

    @Test fun unknownFormatIsUnsupportedAndNeverCalledCorrupt() {
        val result = DownloadValidator.classify("application/x-weird", "thing.bin", 128L, pdfHead, pdfTail)
        assertEquals(DownloadHealth.UNSUPPORTED, result.health)
        assertEquals("no_validator", result.reason)
        assertNull(result.format)
    }

    @Test fun unreadableBytesAreReportedAsUnreadable() {
        val result = DownloadValidator.classify("application/pdf", "a.pdf", 10L, null, null)
        assertEquals(DownloadHealth.UNREADABLE, result.health)
        assertEquals("unreadable", result.reason)
    }

    @Test fun intactPdfIsVerified() {
        val result = DownloadValidator.classify("application/pdf", "a.pdf", 2048L, pdfHead, pdfTail)
        assertEquals(DownloadHealth.INTACT, result.health)
        assertEquals(DownloadFormat.PDF, result.format)
        assertTrue(result.recognisedByMime)
    }

    @Test fun pdfWithValidHeaderAndMissingEndIsTruncated() {
        val result = DownloadValidator.classify("application/pdf", "a.pdf", 2048L, pdfHead, bytes("end\n"))
        assertEquals(DownloadHealth.TRUNCATED, result.health)
        assertEquals("missing_tail", result.reason)
    }

    @Test fun pdfWithBrokenHeaderIsCorrupted() {
        val result = DownloadValidator.classify("application/pdf", "a.pdf", 2048L, bytes("NOT-A-PDF"), pdfTail)
        assertEquals(DownloadHealth.CORRUPTED, result.health)
        assertEquals("bad_header", result.reason)
    }

    @Test fun pngIsJudgedByItsSignatureAndIendChunk() {
        assertEquals(
            DownloadHealth.INTACT,
            DownloadValidator.classify("image/png", "a.png", 900L, pngHead, pngTail).health,
        )
        assertEquals(
            DownloadHealth.CORRUPTED,
            DownloadValidator.classify("image/png", "a.png", 900L, bytes("garbage"), pngTail).health,
        )
        assertEquals(
            DownloadHealth.TRUNCATED,
            DownloadValidator.classify("image/png", "a.png", 900L, pngHead, bytes("half")).health,
        )
    }

    @Test fun jpegIsJudgedByItsStartAndEndMarkers() {
        assertEquals(
            DownloadHealth.INTACT,
            DownloadValidator.classify("image/jpeg", "a.jpg", 900L, jpegHead, jpegTail).health,
        )
        assertEquals(
            DownloadHealth.TRUNCATED,
            DownloadValidator.classify("image/jpeg", "a.jpg", 900L, jpegHead, bytes("xx")).health,
        )
    }

    @Test fun zipIsJudgedByItsLocalHeaderAndCentralDirectory() {
        assertEquals(
            DownloadHealth.INTACT,
            DownloadValidator.classify("application/zip", "a.zip", 900L, zipHead, zipTail).health,
        )
        assertEquals(
            DownloadHealth.TRUNCATED,
            DownloadValidator.classify("application/zip", "a.zip", 900L, zipHead, bytes("xx")).health,
        )
    }

    @Test fun mp4IsAcceptedFromItsFileTypeBoxWithoutPretendingToParseTheContainer() {
        val result = DownloadValidator.classify("video/mp4", "a.mp4", 4096L, mp4Head, null)
        assertEquals(DownloadHealth.INTACT, result.health)
        assertEquals(DownloadFormat.MP4, result.format)
        val broken = DownloadValidator.classify("video/mp4", "a.mp4", 4096L, bytes("xxxxxxx"), null)
        assertEquals(DownloadHealth.CORRUPTED, broken.health)
    }

    @Test fun providerMimeDecidesTheFormatAndTheExtensionIsOnlyAFallback() {
        val byMime = DownloadFormat.fromMime("image/png", "misleading.txt")
        assertEquals(DownloadFormat.PNG, byMime?.first)
        assertTrue(byMime?.second == true)

        val byExtension = DownloadFormat.fromMime("application/octet-stream", "photo.jpg")
        assertEquals(DownloadFormat.JPEG, byExtension?.first)
        assertFalse(byExtension?.second == true)

        assertNull(DownloadFormat.fromMime("application/octet-stream", "file.bin"))
    }

    /**
     * Regression: a ZIP end of central directory record is 22 bytes and may be followed by a
     * comment, so a small fixed tail window misses it and a perfectly intact archive would be
     * reported as interrupted. The window has to follow the format.
     */
    @Test fun aRealArchiveIsJudgedByItsRealEndRecord() {
        val archive = realZipBytes()
        val window = DownloadValidator.tailWindow(DownloadFormat.ZIP, archive.size.toLong())
        val tail = archive.copyOfRange(archive.size - window, archive.size)
        val head = archive.copyOfRange(0, DownloadValidator.WINDOW)
        assertFalse(
            "a 16 byte window cannot see the end record",
            DownloadValidator.tailMatches(
                DownloadFormat.ZIP, archive.copyOfRange(archive.size - 16, archive.size),
            ),
        )
        val result = DownloadValidator.classify(
            "application/zip", "a.zip", archive.size.toLong(), head, tail,
        )
        assertEquals(DownloadHealth.INTACT, result.health)
        assertEquals("verified", result.reason)
    }

    @Test fun aRealPngIsJudgedByItsRealEndChunk() {
        val image = realPngBytes()
        val window = DownloadValidator.tailWindow(DownloadFormat.PNG, image.size.toLong())
        val tail = image.copyOfRange(image.size - window, image.size)
        val head = image.copyOfRange(0, DownloadValidator.WINDOW)
        val result = DownloadValidator.classify(
            "image/png", "a.png", image.size.toLong(), head, tail,
        )
        assertEquals(DownloadHealth.INTACT, result.health)
    }

    @Test fun theTailWindowNeverReadsPastTheEndOfTheFile() {
        assertEquals(0, DownloadValidator.tailWindow(null, 100L))
        assertEquals(0, DownloadValidator.tailWindow(DownloadFormat.MP4, 100L))
        assertEquals(32, DownloadValidator.tailWindow(DownloadFormat.PDF, 100L))
        assertEquals(20, DownloadValidator.tailWindow(DownloadFormat.PDF, 20L))
        assertEquals(0, DownloadValidator.tailWindow(DownloadFormat.PDF, 0L))
    }

    /** A real ZIP with one stored entry, built the way a downloader would deliver it. */
    private fun realZipBytes(): ByteArray {
        val payload = ("KX download rescue verification payload" + System.lineSeparator())
            .toByteArray(Charsets.US_ASCII)
        val name = "note.txt".toByteArray(Charsets.US_ASCII)
        val crc = java.util.zip.CRC32().apply { update(payload) }.value
        val local = java.io.ByteArrayOutputStream()
        local.write(byteArrayOf(0x50, 0x4B, 0x03, 0x04))
        local.write(byteArrayOf(0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00))
        local.write(byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00))
        local.write(intLE(payload.size.toInt()))
        local.write(intLE(payload.size.toInt()))
        local.write(byteArrayOf(name.size.toByte(), 0x00))
        local.write(name)
        local.write(payload)
        val localBytes = local.toByteArray()
        val central = java.io.ByteArrayOutputStream()
        central.write(byteArrayOf(0x50, 0x4B, 0x01, 0x02))
        central.write(byteArrayOf(0x14, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00))
        central.write(byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00))
        central.write(intLE(crc.toInt()))
        central.write(intLE(payload.size.toInt()))
        central.write(intLE(payload.size.toInt()))
        central.write(byteArrayOf(name.size.toByte(), 0x00))
        central.write(name)
        central.write(byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00))
        central.write(intLE(0))
        central.write(intLE(0))
        val centralBytes = central.toByteArray()
        val end = java.io.ByteArrayOutputStream()
        end.write(byteArrayOf(0x50, 0x4B, 0x05, 0x06))
        end.write(byteArrayOf(0x00, 0x00, 0x00, 0x00))
        end.write(intLE(1)); end.write(intLE(1))
        end.write(intLE(centralBytes.size)); end.write(intLE(localBytes.size))
        end.write(byteArrayOf(0x00, 0x00))
        val endBytes = end.toByteArray()
        return localBytes + centralBytes + endBytes
    }

    /** A real PNG with a single red pixel and a proper IEND chunk. */
    private fun realPngBytes(): ByteArray {
        fun chunk(type: String, data: ByteArray): ByteArray {
            val payload = type.toByteArray(Charsets.US_ASCII) + data
            val out = java.io.ByteArrayOutputStream()
            out.write(intBE(data.size))
            out.write(payload)
            out.write(intBE(java.util.zip.CRC32().apply { update(payload) }.value.toInt()))
            return out.toByteArray()
        }
        val out = java.io.ByteArrayOutputStream()
        out.write(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
        out.write(chunk("IHDR", byteArrayOf(0, 0, 0, 1, 0, 0, 0, 1, 8, 2, 0, 0, 0)))
        out.write(chunk("IDAT", byteArrayOf(0x78, 0x9C.toByte(), 0x63, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01)))
        out.write(chunk("IEND", ByteArray(0)))
        return out.toByteArray()
    }

    private fun intLE(value: Int) = byteArrayOf(
        (value and 0xFF).toByte(), ((value shr 8) and 0xFF).toByte(),
        ((value shr 16) and 0xFF).toByte(), ((value shr 24) and 0xFF).toByte(),
    )

    private fun intBE(value: Int) = byteArrayOf(
        ((value shr 24) and 0xFF).toByte(), ((value shr 16) and 0xFF).toByte(),
        ((value shr 8) and 0xFF).toByte(), (value and 0xFF).toByte(),
    )

    @Test fun theSharedDownloadCollectionOnlyExistsFromAndroidTen() {
        assertFalse(DownloadCollections.mediaStoreSupported(26))
        assertFalse(DownloadCollections.mediaStoreSupported(28))
        assertTrue(DownloadCollections.mediaStoreSupported(29))
        assertTrue(DownloadCollections.mediaStoreSupported(35))
    }

    @Test fun onlyFilesThatNeedAttentionAreFlagged() {        assertFalse(DownloadHealth.INTACT.needsAttention)
        assertTrue(DownloadHealth.ZERO_BYTE.needsAttention)
        assertTrue(DownloadHealth.TRUNCATED.needsAttention)
        assertTrue(DownloadHealth.CORRUPTED.needsAttention)
        assertTrue(DownloadHealth.UNSUPPORTED.needsAttention)
        assertTrue(DownloadHealth.UNREADABLE.needsAttention)
    }

    @Test fun onlyClearlyBrokenFilesAreOfferedForRemoval() {
        assertTrue(DownloadHealth.ZERO_BYTE.isDeletable)
        assertTrue(DownloadHealth.TRUNCATED.isDeletable)
        assertTrue(DownloadHealth.CORRUPTED.isDeletable)
        assertFalse(DownloadHealth.INTACT.isDeletable)
        assertFalse(DownloadHealth.UNSUPPORTED.isDeletable)
        assertFalse(DownloadHealth.UNREADABLE.isDeletable)
    }
}
