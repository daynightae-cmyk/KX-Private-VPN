package com.knoux.kxprivatevpn

import com.knoux.kxprivatevpn.platform.repair.DuplicateCleanup
import com.knoux.kxprivatevpn.platform.repair.DuplicateContentSource
import com.knoux.kxprivatevpn.platform.repair.DuplicateScanner
import com.knoux.kxprivatevpn.platform.storage.DocumentEntry
import com.knoux.kxprivatevpn.platform.storage.MutableDocumentSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

/**
 * Unit tests for the Duplicate DNA domain layer.
 *
 * The scanner performs size-bucket grouping then streaming SHA-256 hashing. Cleanup re-verifies
 * byte counts before deletion and follows up with an absence check. Zero-byte files are excluded
 * (Ghost Hunter territory). Near-duplicates are never offered.
 */
class DuplicateDnaTest {

    // ---------------------------------------------------------------------------
    // Test doubles
    // ---------------------------------------------------------------------------

    private class FakeTree : MutableDocumentSource {
        val entries = mutableMapOf<String, DocumentEntry>()
        val children = mutableMapOf<String, MutableList<String>>()
        val parent = mutableMapOf<String, String>()
        val unreadable = mutableSetOf<String>()
        val deleteFails = mutableSetOf<String>()
        /** Delete returns true but file stays in the tree (provider lie). */
        val deleteWithoutRemoving = mutableSetOf<String>()

        fun dir(parentId: String, id: String, name: String) {
            entries[id] = DocumentEntry(id, name, MIME_DIR, -1, 0)
            children.getOrPut(parentId) { mutableListOf() } += id
            parent[id] = parentId
        }

        fun file(
            parentId: String,
            id: String,
            name: String,
            bytes: Long,
            lastModified: Long = 1_000L,
            mime: String = "application/octet-stream",
        ) {
            entries[id] = DocumentEntry(id, name, mime, bytes, lastModified)
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

    /** Fake content source backed by a byte-array map. */
    private class FakeContentSource(
        private val content: Map<String, ByteArray>,
    ) : DuplicateContentSource {
        var hashCalls = 0
        override suspend fun hash(documentId: String): ByteArray {
            hashCalls++
            val bytes = content[documentId] ?: error("No content for $documentId")
            return MessageDigest.getInstance("SHA-256").digest(bytes)
        }

        override suspend fun currentBytes(documentId: String): Long? =
            content[documentId]?.size?.toLong()
    }

    // ---------------------------------------------------------------------------
    // Fixtures
    // ---------------------------------------------------------------------------

    private val aBytes = ByteArray(100) { 0x41 }  // 100 bytes, all 'A'
    private val bBytes = ByteArray(100) { 0x42 }  // 100 bytes, all 'B'
    private val cBytes = ByteArray(200) { 0x43 }  // 200 bytes, all 'C'

    private fun fixture(): Pair<FakeTree, FakeContentSource> {
        val tree = FakeTree().apply {
            // Three copies of 'A' (exact duplicates by size AND hash)
            file("root", "a1", "a1.bin", 100, 1_000)
            file("root", "a2", "a2.bin", 100, 2_000)
            file("root", "a3", "a3.bin", 100, 3_000)
            // Two copies of 'B' (exact duplicates by size AND hash)
            file("root", "b1", "b1.bin", 100, 4_000)
            // Note: b1 and a1 share size=100 but differ in hash — they should NOT be grouped together
            // 'C' is unique by size
            file("root", "c1", "unique.bin", 200)
            // Zero-byte file — excluded from duplicate scan
            file("root", "z1", "zero.txt", 0)
        }
        val content = mapOf(
            "a1" to aBytes, "a2" to aBytes, "a3" to aBytes,
            "b1" to bBytes,
            "c1" to cBytes,
            "z1" to ByteArray(0),
        )
        return tree to FakeContentSource(content)
    }

    // ---------------------------------------------------------------------------
    // Scanner tests
    // ---------------------------------------------------------------------------

    @Test fun scanGroupsExactDuplicatesByHash() = runTest {
        val (tree, contentSource) = fixture()
        val scan = DuplicateScanner(tree, contentSource).scan("root", 1_000)

        assertFalse(scan.cancelled)
        assertFalse(scan.failed)
        // 5 non-zero files scanned (a1, a2, a3, b1, c1); z1 excluded.
        assertEquals(5, scan.scannedFiles)
        // c1 has unique size — not hashed. a1/a2/a3/b1 share size=100 — all 4 hashed.
        assertEquals(4, scan.hashedFiles)
        // Only one group: a1/a2/a3 share the same hash.
        // b1 has size=100 but different hash — no partner, so no group.
        assertEquals(1, scan.duplicateGroupCount)
        val group = scan.groups.first()
        assertEquals(3, group.entries.size)
        // Keep copy is oldest (a1, lastModified=1000).
        assertTrue(group.entries.first { it.keep }.documentId == "a1")
        // Other two are offered for deletion.
        assertEquals(2, group.duplicateCount)
        assertEquals(200, group.reclaimableBytes) // 2 * 100
    }

    @Test fun zeroBytFilesAreExcluded() = runTest {
        val (tree, contentSource) = fixture()
        val scan = DuplicateScanner(tree, contentSource).scan("root", 1_000)
        assertTrue(scan.groups.flatMap { it.entries }.none { it.documentId == "z1" })
    }

    @Test fun keepCopyIsEarliestLastModified() = runTest {
        val tree = FakeTree().apply {
            file("root", "old", "old.txt", 50, 1_000)
            file("root", "new", "new.txt", 50, 9_000)
        }
        val bytes = ByteArray(50) { 0x77 }
        val content = FakeContentSource(mapOf("old" to bytes, "new" to bytes))
        val scan = DuplicateScanner(tree, content).scan("root", 1_000)
        assertEquals(1, scan.groups.size)
        val keep = scan.groups[0].entries.first { it.keep }
        assertEquals("old", keep.documentId)
        val del = scan.groups[0].entries.first { !it.keep }
        assertEquals("new", del.documentId)
    }

    @Test fun keepCopyTieIsResolvedByPath() = runTest {
        val tree = FakeTree().apply {
            file("root", "aaa", "zzz.bin", 50, 1_000) // same lastModified
            file("root", "bbb", "aaa.bin", 50, 1_000) // same lastModified, earlier path
        }
        val bytes = ByteArray(50) { 0x55 }
        val content = FakeContentSource(mapOf("aaa" to bytes, "bbb" to bytes))
        val scan = DuplicateScanner(tree, content).scan("root", 1_000)
        val keep = scan.groups[0].entries.first { it.keep }
        // "aaa.bin" < "zzz.bin" lexicographically
        assertEquals("bbb", keep.documentId)
    }

    @Test fun sizeSingletonIsNotHashed() = runTest {
        val tree = FakeTree().apply {
            file("root", "x", "x.bin", 77) // unique size
            file("root", "y", "y.bin", 88) // unique size
        }
        val content = FakeContentSource(mapOf(
            "x" to ByteArray(77), "y" to ByteArray(88)
        ))
        val scan = DuplicateScanner(tree, content).scan("root", 1_000)
        assertEquals(0, scan.duplicateGroupCount)
        assertEquals(0, scan.hashedFiles) // no hashing done
        assertEquals(0, content.hashCalls)
    }

    @Test fun groupsAreSortedByReclaimableBytesDescending() = runTest {
        val tree = FakeTree().apply {
            // Small group: 2 files × 10 bytes = 10 reclaimable
            file("root", "s1", "s1.txt", 10, 1_000)
            file("root", "s2", "s2.txt", 10, 2_000)
            // Large group: 2 files × 500 bytes = 500 reclaimable
            file("root", "l1", "l1.bin", 500, 1_000)
            file("root", "l2", "l2.bin", 500, 2_000)
        }
        val smallBytes = ByteArray(10) { 0x01 }
        val largeBytes = ByteArray(500) { 0x02 }
        val content = FakeContentSource(mapOf(
            "s1" to smallBytes, "s2" to smallBytes,
            "l1" to largeBytes, "l2" to largeBytes,
        ))
        val scan = DuplicateScanner(tree, content).scan("root", 1_000)
        assertEquals(2, scan.groups.size)
        assertTrue(scan.groups[0].reclaimableBytes >= scan.groups[1].reclaimableBytes)
        assertEquals(500, scan.groups[0].reclaimableBytes)
    }

    @Test fun truncationStopsScanAtMaxEntries() = runTest {
        val tree = FakeTree().apply {
            repeat(20) { i ->
                file("root", "f$i", "f$i.bin", 100L + i)
            }
        }
        val content = FakeContentSource(emptyMap())
        val scan = DuplicateScanner(tree, content, maxEntries = 5).scan("root", 1_000)
        assertTrue(scan.truncated)
        assertTrue(scan.scannedFiles <= 5)
    }

    @Test fun cancellationDuringScanIsReported() = runTest {
        val (tree, contentSource) = fixture()
        val scanner = DuplicateScanner(tree, contentSource)
        var requested = false
        val scan = scanner.scan("root", 1_000) { scanned, _ ->
            if (!requested && scanned > 0) { requested = true; scanner.requestCancel() }
        }
        assertTrue(scan.cancelled)
    }

    @Test fun unreadableBranchIsFailedNotSilentlyClean() = runTest {
        val tree = FakeTree().apply {
            dir("root", "sub", "Sub")
            file("sub", "d1", "d1.bin", 100)
            file("sub", "d2", "d2.bin", 100)
            file("root", "r1", "r1.bin", 50)
        }
        tree.unreadable += "sub"
        val content = FakeContentSource(mapOf("r1" to ByteArray(50)))
        val scan = DuplicateScanner(tree, content).scan("root", 1_000)
        assertTrue(scan.failed)
        assertTrue(scan.truncated)
        assertTrue(scan.groups.flatMap { it.entries }.none { it.documentId.startsWith("d") })
    }

    @Test fun hashFailureIsRecordedButScanContinues() = runTest {
        val tree = FakeTree().apply {
            file("root", "ok1", "ok1.bin", 100, 1_000)
            file("root", "ok2", "ok2.bin", 100, 2_000)
            file("root", "bad", "bad.bin", 100, 3_000) // hash will throw
        }
        val bytes = ByteArray(100) { 0xAA.toByte() }
        val content = object : DuplicateContentSource {
            override suspend fun hash(documentId: String): ByteArray {
                if (documentId == "bad") error("I/O failure")
                return MessageDigest.getInstance("SHA-256").digest(bytes)
            }
            override suspend fun currentBytes(documentId: String): Long? = 100L
        }
        val scan = DuplicateScanner(tree, content).scan("root", 1_000)
        assertTrue(scan.failed)
        // ok1 and ok2 still share a hash → they form one group
        assertEquals(1, scan.duplicateGroupCount)
    }

    // ---------------------------------------------------------------------------
    // Cleanup tests
    // ---------------------------------------------------------------------------

    @Test fun cleanupDeletesSelectedNonKeepEntries() = runTest {
        val (tree, contentSource) = fixture()
        val scan = DuplicateScanner(tree, contentSource).scan("root", 1_000)
        val toDelete = scan.groups.flatMap { g -> g.entries.filter { !it.keep } }

        val receipt = DuplicateCleanup(tree).remove(toDelete)

        assertEquals(toDelete.size, receipt.deleted)
        assertEquals(toDelete.size, receipt.verifiedAbsent)
        assertTrue(receipt.complete)
        assertTrue(receipt.skipped.isEmpty())
        assertTrue(receipt.failed.isEmpty())
        // Keep copies must survive
        val keepIds = scan.groups.flatMap { g -> g.entries.filter { it.keep }.map { it.documentId } }
        keepIds.forEach { id -> assertTrue("Keep copy $id was deleted!", tree.document(id) != null) }
    }

    @Test fun changedFileSizeIsSkippedNotDeleted() = runTest {
        val (tree, contentSource) = fixture()
        val scan = DuplicateScanner(tree, contentSource).scan("root", 1_000)
        val toDelete = scan.groups.flatMap { g -> g.entries.filter { !it.keep } }

        // Simulate external write: change byte count of first target
        val firstTarget = toDelete.first()
        tree.entries[firstTarget.documentId] =
            tree.entries.getValue(firstTarget.documentId).copy(bytes = firstTarget.bytes + 1)

        val receipt = DuplicateCleanup(tree).remove(toDelete)

        assertEquals(1, receipt.skipped.size)
        assertEquals(toDelete.size - 1, receipt.deleted)
        assertTrue(receipt.failed.isEmpty())
        // The changed file must still exist
        assertTrue(tree.document(firstTarget.documentId) != null)
    }

    @Test fun providerDeleteFailureIsReportedNotHidden() = runTest {
        val (tree, contentSource) = fixture()
        val scan = DuplicateScanner(tree, contentSource).scan("root", 1_000)
        val toDelete = scan.groups.flatMap { g -> g.entries.filter { !it.keep } }

        tree.deleteFails += toDelete.first().documentId

        val receipt = DuplicateCleanup(tree).remove(toDelete)

        assertTrue(receipt.failed.contains(toDelete.first().relativePath))
        assertEquals(toDelete.size - 1, receipt.deleted)
    }

    @Test fun deleteWithoutRemovingIsReportedAsFailed() = runTest {
        val (tree, contentSource) = fixture()
        val scan = DuplicateScanner(tree, contentSource).scan("root", 1_000)
        val toDelete = scan.groups.flatMap { g -> g.entries.filter { !it.keep } }

        tree.deleteWithoutRemoving += toDelete.first().documentId

        val receipt = DuplicateCleanup(tree).remove(toDelete)

        assertTrue(receipt.failed.isNotEmpty())
        // verifiedAbsent should be less than deleted+skipped
        assertTrue(receipt.verifiedAbsent < toDelete.size)
    }

    @Test fun cancellationDuringCleanupYieldsPartialReceipt() = runTest {
        val (tree, contentSource) = fixture()
        val scan = DuplicateScanner(tree, contentSource).scan("root", 1_000)
        val toDelete = scan.groups.flatMap { g -> g.entries.filter { !it.keep } }

        val cleanup = DuplicateCleanup(tree)
        val receipt = cleanup.remove(toDelete) { cleanup.requestCancel() }

        assertFalse(receipt.complete)
        assertTrue(receipt.deleted <= 1)
        assertEquals(receipt.deleted, receipt.verifiedAbsent)
    }

    @Test fun bytesReclaimedMatchDeletedCount() = runTest {
        val (tree, contentSource) = fixture()
        val scan = DuplicateScanner(tree, contentSource).scan("root", 1_000)
        val toDelete = scan.groups.flatMap { g -> g.entries.filter { !it.keep } }

        val receipt = DuplicateCleanup(tree).remove(toDelete)

        assertEquals(receipt.deleted * 100L, receipt.bytesReclaimed)
    }
}
