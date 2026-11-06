package com.knoux.kxprivatevpn

import com.knoux.kxprivatevpn.platform.repair.HeavyweightFilter
import com.knoux.kxprivatevpn.platform.repair.HeavyweightScanner
import com.knoux.kxprivatevpn.platform.repair.LargeFileCategory
import com.knoux.kxprivatevpn.platform.storage.DocumentEntry
import com.knoux.kxprivatevpn.platform.storage.DocumentSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Large Files ranks only provider-reported sizes, keeps a bounded ranking, and never lets an
 * unreadable branch or an unknown size masquerade as a measured result.
 */
class HeavyweightTest {
    private class FakeTree : DocumentSource {
        val entries = mutableMapOf<String, DocumentEntry>()
        val children = mutableMapOf<String, MutableList<String>>()
        val unreadable = mutableSetOf<String>()

        fun dir(parentId: String, id: String, name: String) {
            entries[id] = DocumentEntry(id, name, MIME_DIR, -1, 0)
            children.getOrPut(parentId) { mutableListOf() } += id
        }

        fun file(
            parentId: String,
            id: String,
            name: String,
            bytes: Long,
            mime: String = "application/octet-stream",
            modified: Long = 0,
        ) {
            entries[id] = DocumentEntry(id, name, mime, bytes, modified)
            children.getOrPut(parentId) { mutableListOf() } += id
        }

        override suspend fun list(documentId: String): List<DocumentEntry> {
            if (documentId in unreadable) error("Unreadable branch")
            return children[documentId].orEmpty().mapNotNull { entries[it] }
        }

        companion object {
            const val MIME_DIR = "vnd.android.document/directory"
        }
    }

    private fun fixture(): FakeTree = FakeTree().apply {
        dir("root", "nested", "Nested")
        file("root", "huge", "movie.mp4", 100 * MIB, "video/mp4")
        file("root", "small", "note.txt", 512 * 1024, "text/plain")
        file("root", "empty", "empty.bin", 0, "application/octet-stream")
        file("root", "photo", "photo.jpg", 6 * MIB, "image/jpeg")
        file("nested", "archive", "backup.zip", 10 * MIB, "application/zip")
    }

    private fun total(bytes: Long) = bytes * 1024 * 1024

    @Test fun ranksOnlyMeasuredFilesAndAppliesTheByteFloor() = runTest {
        val scan = HeavyweightScanner(fixture()).scan("root", 1234)

        assertEquals(listOf("movie.mp4", "backup.zip", "photo.jpg"),
            scan.files.map { it.name })
        assertEquals(listOf("huge", "archive", "photo"), scan.files.map { it.documentId })
        assertEquals(5, scan.scannedFiles)
        assertEquals(3, scan.largeFileCount)
        assertEquals(total(116), scan.totalLargeBytes)
        assertEquals(0, scan.skippedUnknownSize)
        assertEquals(HeavyweightScanner.MINIMUM_BYTES, scan.minimumBytes)
        assertFalse(scan.truncated)
        assertFalse(scan.cancelled)
        assertFalse(scan.failed)
        assertEquals(1234, scan.atMillis)
    }

    @Test fun rankingKeepsTheLargestLimitButCountsEveryLargeFile() = runTest {
        val scan = HeavyweightScanner(fixture(), limit = 2).scan("root", 1)

        assertEquals(listOf("movie.mp4", "backup.zip"), scan.files.map { it.name })
        assertEquals(3, scan.largeFileCount)
        assertEquals(total(116), scan.totalLargeBytes)
    }

    @Test fun unknownSizeIsCountedAndNeverRanked() = runTest {
        val tree = fixture()
        tree.file("root", "unknown", "stream.bin", -1, "application/octet-stream")
        val scan = HeavyweightScanner(tree).scan("root", 1)

        assertEquals(1, scan.skippedUnknownSize)
        assertTrue(scan.files.none { it.documentId == "unknown" })
        assertEquals(6, scan.scannedFiles)
    }

    @Test fun unreadableBranchIsReportedAsPartial() = runTest {
        val tree = fixture()
        tree.unreadable += "nested"
        val scan = HeavyweightScanner(tree).scan("root", 1)

        assertTrue(scan.failed)
        assertTrue(scan.truncated)
        assertTrue(scan.files.none { it.documentId == "archive" })
        assertTrue(scan.files.any { it.documentId == "huge" })
    }

    @Test fun entryBudgetTruncatesTheWalk() = runTest {
        val scan = HeavyweightScanner(fixture(), maxEntries = 2).scan("root", 1)

        assertTrue(scan.truncated)
        assertEquals(1, scan.scannedFiles)
        assertEquals(1, scan.files.size)
    }

    @Test fun cancellationStopsTheWalkAndIsReported() = runTest {
        val scanner = HeavyweightScanner(fixture())
        var requested = false
        val scan = scanner.scan("root", 1, onProgress = {
            if (!requested) {
                requested = true
                scanner.requestCancel()
            }
        })

        assertTrue(scan.cancelled)
        assertTrue(scan.files.size <= 3)
    }

    @Test fun viewFilterUsesMeasuredBytesAndMimeFamily() {
        val files = listOf(
            com.knoux.kxprivatevpn.platform.repair.LargeFile(
                "a", "Movies/a.mp4", "a.mp4", "video/mp4", total(100), 10),
            com.knoux.kxprivatevpn.platform.repair.LargeFile(
                "b", "Pictures/b.jpg", "b.jpg", "image/jpeg", total(6), 20),
            com.knoux.kxprivatevpn.platform.repair.LargeFile(
                "c", "Docs/c.pdf", "c.pdf", "application/pdf", total(12), 30),
        )

        assertEquals(listOf("a", "b", "c"),
            HeavyweightFilter().apply(files).map { it.documentId })
        assertEquals(listOf("a"),
            HeavyweightFilter(minimumBytes = total(50)).apply(files).map { it.documentId })
        assertEquals(listOf("c"),
            HeavyweightFilter(category = LargeFileCategory.DOCUMENT).apply(files)
                .map { it.documentId })
        assertEquals(emptyList<String>(),
            HeavyweightFilter(minimumBytes = total(50), category = LargeFileCategory.IMAGE)
                .apply(files).map { it.documentId })
    }

    @Test fun categoryMappingFollowsTheReportedMimeFamily() {
        assertEquals(LargeFileCategory.IMAGE, LargeFileCategory.fromMime("image/png"))
        assertEquals(LargeFileCategory.VIDEO, LargeFileCategory.fromMime("video/mp4"))
        assertEquals(LargeFileCategory.AUDIO, LargeFileCategory.fromMime("audio/mpeg"))
        assertEquals(LargeFileCategory.ARCHIVE, LargeFileCategory.fromMime("application/zip"))
        assertEquals(LargeFileCategory.ARCHIVE, LargeFileCategory.fromMime("application/x-7z-compressed"))
        assertEquals(LargeFileCategory.DOCUMENT, LargeFileCategory.fromMime("application/pdf"))
        assertEquals(LargeFileCategory.DOCUMENT, LargeFileCategory.fromMime("text/plain"))
        assertEquals(LargeFileCategory.DOCUMENT, LargeFileCategory.fromMime(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
        assertEquals(LargeFileCategory.OTHER, LargeFileCategory.fromMime(
            "application/vnd.android.package-archive"))
        assertEquals(LargeFileCategory.OTHER, LargeFileCategory.fromMime(""))
    }

    private companion object {
        const val MIB = 1024L * 1024L
    }
}
