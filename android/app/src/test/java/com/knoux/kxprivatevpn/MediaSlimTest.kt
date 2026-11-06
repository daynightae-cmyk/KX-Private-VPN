package com.knoux.kxprivatevpn

import com.knoux.kxprivatevpn.platform.media.MediaCandidate
import com.knoux.kxprivatevpn.platform.media.MediaKind
import com.knoux.kxprivatevpn.platform.media.MediaQuality
import com.knoux.kxprivatevpn.platform.media.MediaSelection
import com.knoux.kxprivatevpn.platform.media.MediaSlimPolicy
import com.knoux.kxprivatevpn.platform.media.MediaSlimScanner
import com.knoux.kxprivatevpn.platform.storage.DocumentEntry
import com.knoux.kxprivatevpn.platform.storage.DocumentSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Media Slim only offers formats it can really re-encode; videos are measured but never faked. */
class MediaSlimTest {
    private class FakeSource(private val tree: Map<String, List<DocumentEntry>>) : DocumentSource {
        override suspend fun list(documentId: String): List<DocumentEntry> =
            tree[documentId] ?: error("Unknown document $documentId")
    }

    private fun file(
        id: String,
        name: String,
        bytes: Long,
        mime: String = "image/jpeg",
    ) = DocumentEntry(id, name, mime, bytes, 0)

    private fun dir(id: String, name: String) =
        DocumentEntry(id, name, "vnd.android.document/directory", -1, 0)

    private val source = FakeSource(mapOf(
        "root" to listOf(
            dir("dcim", "DCIM"),
            file("tiny", "tiny.jpg", 100),
            file("unknown", "unknown.jpg", -1),
            file("pdf", "notes.pdf", 900, "application/pdf"),
        ),
        "dcim" to listOf(
            file("photo", "photo.JPG", 800_000, "IMAGE/JPEG"),
            file("gif", "loop.gif", 700_000, "image/gif"),
            file("clip", "clip.mp4", 600_000, "video/mp4"),
            dir("nested", "nested"),
        ),
        "nested" to listOf(
            file("webp", "small.webp", 500_000, "image/webp"),
            file("heic", "camera.heic", 400_000, "image/heic"),
        ),
    ))

    @Test fun scanClassifiesImagesAndVideosWithoutReadingContent() = runTest {
        val scan = MediaSlimScanner(source).scan("root", atMillis = 77)

        assertEquals(77, scan.atMillis)
        assertFalse(scan.cancelled)
        assertFalse(scan.failed)
        assertFalse(scan.truncated)
        assertEquals(7, scan.scannedFiles)
        assertEquals(1, scan.skippedUnknownSize)
        // Tiny files are scanned but not offered; videos/GIFs are measured but unsupported.
        assertEquals(listOf("photo.JPG", "loop.gif", "clip.mp4", "small.webp", "camera.heic"),
            scan.files.map { it.name })
        assertEquals(4, scan.images.size)
        assertEquals(1, scan.videos.size)
        assertEquals(2_400_000, scan.imageBytes)
        assertEquals(600_000, scan.videoBytes)
        assertEquals("DCIM/photo.JPG", scan.images.first().relativePath)
        assertEquals("DCIM/nested/camera.heic", scan.images.last().relativePath)
        assertEquals(MediaKind.IMAGE, scan.images.first().kind)
        assertEquals(MediaKind.VIDEO, scan.videos.single().kind)
        assertTrue(scan.images.first().compressible)
        assertFalse(scan.images.first { it.name == "loop.gif" }.compressible)
        assertFalse(scan.videos.single().compressible)
    }

    @Test fun selectionBytesAndConfirmationOnlyCountEncodableImages() = runTest {
        val scan = MediaSlimScanner(source).scan("root", 1)
        val selection = MediaSelection(
            ids = setOf("photo", "gif", "clip", "heic"),
            quality = MediaQuality.SMALL,
        )

        assertEquals(setOf("photo", "gif", "clip", "heic"),
            selection.candidates(scan).map { it.documentId }.toSet())
        // 800k JPEG + 400k HEIC; the GIF and the video are never promised as compressible.
        assertEquals(1_200_000, selection.supportedBytes(scan))
        assertEquals(2, selection.unsupportedCount(scan))
        assertTrue(MediaSlimPolicy.canCompress(scan, selection))
        assertEquals("2 image(s), 1200000 measured bytes",
            MediaSlimPolicy.confirmation(scan, selection))
        assertEquals(MediaQuality.SMALL, MediaQuality.fromPercent(45))
        assertEquals(MediaQuality.BALANCED, MediaQuality.fromPercent(63))
    }

    @Test fun policyRefusesEmptyOrUnsupportedSelections() = runTest {
        val scan = MediaSlimScanner(source).scan("root", 1)
        val unsupported = MediaSelection(setOf("gif", "clip"))

        assertFalse(MediaSlimPolicy.canCompress(scan, unsupported))
        assertNull(MediaSlimPolicy.confirmation(scan, unsupported))
        assertFalse(MediaSlimPolicy.canCompress(null, unsupported))
        assertFalse(MediaSlimPolicy.canCompress(null, MediaSelection(emptySet())))
    }

    @Test fun unknownSizeAndMinimumFilterKeepTheScanHonest() = runTest {
        val scan = MediaSlimScanner(source, minimumBytes = 600_000).scan("root", 1)

        assertEquals(1, scan.skippedUnknownSize)
        assertEquals(listOf("photo.JPG", "loop.gif", "clip.mp4"), scan.files.map { it.name })
        assertFalse(scan.files.any { it.documentId == "unknown" })
        assertFalse(scan.files.any { it.documentId == "tiny" })
    }

    @Test fun entryLimitStopsWithLowerBoundFlag() = runTest {
        val scan = MediaSlimScanner(source, maxEntries = 3).scan("root", 3)

        assertTrue(scan.truncated)
        assertTrue(scan.files.size <= 3)
    }

    @Test fun depthLimitDoesNotLoopForever() = runTest {
        val cyclic = FakeSource(mapOf(
            "root" to listOf(dir("child", "child")),
            "child" to listOf(dir("child", "child"), file("deep", "deep.jpg", 90_000)),
        ))

        val scan = MediaSlimScanner(cyclic, maxDepth = 1).scan("root", 1)

        assertTrue(scan.truncated)
        assertEquals(listOf("deep.jpg"), scan.files.map { it.name })
    }

    @Test fun unreadableBranchIsFlaggedButKeepsMeasuredFiles() = runTest {
        val broken = FakeSource(mapOf(
            "root" to listOf(dir("gone", "gone"), file("ok", "ok.jpg", 90_000)),
        ))

        val scan = MediaSlimScanner(broken).scan("root", 2)

        assertTrue(scan.failed)
        assertTrue(scan.truncated)
        assertEquals(listOf("ok.jpg"), scan.files.map { it.name })
    }

    @Test fun cancellationReturnsPartialResultsFlagged() = runTest {
        val wide = FakeSource(mapOf(
            "root" to listOf(dir("one", "one"), dir("two", "two"), dir("three", "three")),
            "one" to listOf(file("a", "a.jpg", 90_000)),
            "two" to listOf(file("b", "b.jpg", 90_000)),
            "three" to listOf(file("c", "c.jpg", 90_000)),
        ))
        val scanner = MediaSlimScanner(wide)
        var requested = false

        val scan = scanner.scan("root", 1, onProgress = {
            if (!requested) { requested = true; scanner.requestCancel() }
        })

        assertTrue(scan.cancelled)
        assertTrue(scan.files.size < 3)
    }

    @Test fun selectionOfMissingIdsIsEmpty() = runTest {
        val scan = MediaSlimScanner(source).scan("root", 1)
        val selection = MediaSelection(setOf("missing", "also-missing"))

        assertTrue(selection.candidates(scan).isEmpty())
        assertFalse(MediaSlimPolicy.canCompress(scan, selection))
    }

    @Test fun mediaCandidateTreatsMimeCaseInsensitively() {
        val candidate = MediaCandidate(
            documentId = "id", parentDocumentId = "parent", relativePath = "a/heic",
            name = "heic", mime = "Image/HEIC", bytes = 1, lastModified = 0,
        )

        assertEquals(MediaKind.IMAGE, candidate.kind)
        assertTrue(candidate.compressible)
    }
}
