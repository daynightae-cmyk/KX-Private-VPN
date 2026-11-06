package com.knoux.kxprivatevpn

import com.knoux.kxprivatevpn.platform.repair.DownloadFormat
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

    @Test fun onlyFilesThatNeedAttentionAreFlagged() {
        assertFalse(DownloadHealth.INTACT.needsAttention)
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
