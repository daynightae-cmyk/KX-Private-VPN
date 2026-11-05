package com.knoux.kxprivatevpn

import com.knoux.kxprivatevpn.platform.repair.GhostCleanup
import com.knoux.kxprivatevpn.platform.repair.GhostConfidence
import com.knoux.kxprivatevpn.platform.repair.GhostKind
import com.knoux.kxprivatevpn.platform.repair.GhostReason
import com.knoux.kxprivatevpn.platform.repair.GhostScanner
import com.knoux.kxprivatevpn.platform.storage.DocumentEntry
import com.knoux.kxprivatevpn.platform.storage.MutableDocumentSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ghost candidates are limited to reported zero-byte files and directories that returned no
 * children. Cleanup re-verifies every candidate and only reports deleted after a real absence
 * check; known sentinels are never offered.
 */
class GhostHunterTest {
    private class FakeTree : MutableDocumentSource {
        val entries = mutableMapOf<String, DocumentEntry>()
        val children = mutableMapOf<String, MutableList<String>>()
        val parent = mutableMapOf<String, String>()
        val unreadable = mutableSetOf<String>()
        val deleteFails = mutableSetOf<String>()
        val deleteWithoutRemoving = mutableSetOf<String>()

        fun dir(parentId: String, id: String, name: String) {
            entries[id] = DocumentEntry(id, name, MIME_DIR, -1, 0)
            children.getOrPut(parentId) { mutableListOf() } += id
            parent[id] = parentId
        }

        fun file(parentId: String, id: String, name: String, bytes: Long,
                 mime: String = "application/octet-stream") {
            entries[id] = DocumentEntry(id, name, mime, bytes, 0)
            children.getOrPut(parentId) { mutableListOf() } += id
            parent[id] = parentId
        }

        override suspend fun list(documentId: String): List<DocumentEntry> {
            if (documentId in unreadable) error("Unreadable")
            return children[documentId].orEmpty().mapNotNull { entries[it] }
        }

        override suspend fun document(documentId: String): DocumentEntry? = entries[documentId]

        override suspend fun delete(documentId: String): Boolean {
            if (documentId in deleteFails) return false
            if (documentId in deleteWithoutRemoving) return true
            entries.remove(documentId)
            parent.remove(documentId)?.let { children[it]?.remove(documentId) }
            return true
        }

        companion object {
            const val MIME_DIR = "vnd.android.document/directory"
        }
    }

    private fun fixture(): FakeTree = FakeTree().apply {
        dir("root", "emptyDir", "Empty")
        dir("root", "fullDir", "Full")
        file("root", "zero", "zero.txt", 0, "text/plain")
        file("root", "sentinel", ".nomedia", 0)
        file("root", "data", "data.bin", 100)
        file("fullDir", "inner", "inner.tmp", 0)
    }

    @Test fun scanFindsOnlyZeroByteFilesAndChildlessDirectories() = runTest {
        val scan = GhostScanner(fixture()).scan("root", 1234)
        val paths = scan.candidates.map { it.relativePath }.toSet()
        assertEquals(setOf("zero.txt", "Empty", "Full/inner.tmp"), paths)
        assertEquals(2, scan.zeroByteCount)
        assertEquals(1, scan.emptyDirectoryCount)
        assertEquals(1, scan.excludedSentinels)
        assertEquals(4, scan.scannedFiles)
        assertEquals(3, scan.scannedDirectories)
        assertEquals(1234, scan.atMillis)
        scan.candidates.forEach {
            when (it.kind) {
                GhostKind.ZERO_BYTE_FILE -> {
                    assertEquals(GhostConfidence.HIGH, it.confidence)
                    assertEquals(GhostReason.REPORTED_ZERO_BYTES, it.reason)
                }
                GhostKind.EMPTY_DIRECTORY -> {
                    assertEquals(GhostConfidence.MEDIUM, it.confidence)
                    assertEquals(GhostReason.NO_CHILDREN, it.reason)
                }
            }
        }
    }

    @Test fun scanTruncatesAndReportsCancellation() = runTest {
        val truncated = GhostScanner(fixture(), maxEntries = 2).scan("root", 1)
        assertTrue(truncated.truncated)

        val scanner = GhostScanner(fixture())
        var requested = false
        val cancelled = scanner.scan("root", 1, onProgress = {
            if (!requested) { requested = true; scanner.requestCancel() }
        })
        assertTrue(cancelled.cancelled)
    }

    @Test fun unreadableBranchIsFailedNotSilentlyClean() = runTest {
        val tree = fixture()
        tree.unreadable += "fullDir"
        val scan = GhostScanner(tree).scan("root", 1)
        assertTrue(scan.failed)
        assertTrue(scan.truncated)
        assertTrue(scan.candidates.none { it.relativePath.startsWith("Full") })
    }

    @Test fun cleanupDeletesOnlySelectedVerifiedItems() = runTest {
        val tree = fixture()
        val scan = GhostScanner(tree).scan("root", 1)
        val selected = scan.candidates.filter {
            it.relativePath == "zero.txt" || it.relativePath == "Empty"
        }
        val receipt = GhostCleanup(tree).remove(selected)
        assertEquals(2, receipt.deleted)
        assertEquals(2, receipt.verifiedAbsent)
        assertTrue(receipt.complete)
        assertTrue(receipt.skipped.isEmpty())
        assertTrue(receipt.failed.isEmpty())
        assertEquals(null, tree.document("zero"))
        assertEquals(null, tree.document("emptyDir"))
        assertEquals(100L, tree.document("data")?.bytes)
        assertEquals(0L, tree.document("sentinel")?.bytes)
        assertTrue(tree.document("inner") != null)
    }

    @Test fun changedFileAndFilledDirectoryAreSkippedAndPreserved() = runTest {
        val tree = fixture()
        val scan = GhostScanner(tree).scan("root", 1)
        // Simulate external changes after the scan.
        tree.entries["zero"] = tree.entries.getValue("zero").copy(bytes = 10)
        tree.file("emptyDir", "new", "new.txt", 5)
        val selected = scan.candidates.filter {
            it.relativePath == "zero.txt" || it.relativePath == "Empty"
        }
        val receipt = GhostCleanup(tree).remove(selected)
        assertEquals(0, receipt.deleted)
        assertEquals(2, receipt.skipped.size)
        assertTrue(tree.document("zero") != null)
        assertTrue(tree.document("emptyDir") != null)
    }

    @Test fun providerFailureOrVanishingSuccessIsReportedNotHidden() = runTest {
        val tree = fixture()
        val scan = GhostScanner(tree).scan("root", 1)
        tree.deleteFails += "zero"
        tree.deleteWithoutRemoving += "emptyDir"
        val selected = scan.candidates.filter {
            it.relativePath == "zero.txt" || it.relativePath == "Empty"
        }
        val receipt = GhostCleanup(tree).remove(selected)
        assertEquals(0, receipt.deleted)
        assertEquals(2, receipt.failed.size)
        assertEquals(0, receipt.verifiedAbsent)
        assertTrue(receipt.failed.contains("zero.txt"))
        assertTrue(receipt.failed.contains("Empty"))
    }

    @Test fun cleanupCancellationReturnsPartialReceipt() = runTest {
        val tree = fixture()
        val scan = GhostScanner(tree).scan("root", 1)
        val cleanup = GhostCleanup(tree)
        val receipt = cleanup.remove(scan.candidates) {
            cleanup.requestCancel()
        }
        assertFalse(receipt.complete)
        assertTrue(receipt.deleted <= 1)
        assertTrue(receipt.verifiedAbsent == receipt.deleted)
        assertTrue(tree.document("data") != null)
    }
}
