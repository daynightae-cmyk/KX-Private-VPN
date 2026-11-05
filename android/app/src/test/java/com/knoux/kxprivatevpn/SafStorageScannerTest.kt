package com.knoux.kxprivatevpn

import com.knoux.kxprivatevpn.platform.storage.DocumentEntry
import com.knoux.kxprivatevpn.platform.storage.DocumentSource
import com.knoux.kxprivatevpn.platform.storage.SafStorageScanner
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** SAF traversal is tested against a fake document tree; production supplies DocumentsContract. */
class SafStorageScannerTest {
    private class FakeSource(private val tree: Map<String, List<DocumentEntry>>) : DocumentSource {
        override suspend fun list(documentId: String): List<DocumentEntry> =
            tree[documentId] ?: error("Unknown document $documentId")
    }

    private fun file(id: String, name: String, bytes: Long, mime: String = "image/jpeg") =
        DocumentEntry(id, name, mime, bytes, 0)

    private fun dir(id: String, name: String) =
        DocumentEntry(id, name, "vnd.android.document/directory", -1, 0)

    private val source = FakeSource(mapOf(
        "root" to listOf(dir("dcim", "DCIM"), dir("docs", "docs"), file("zero", "zero.txt", 0, "text/plain")),
        "dcim" to listOf(file("a", "a.jpg", 100), dir("sub", "sub"), file("b", "b.mp4", 200, "video/mp4")),
        "sub" to listOf(file("c", "c.png", 300, "image/png")),
        "docs" to listOf(file("d", "d.pdf", 400, "application/pdf")),
    ))

    @Test fun depthFirstWalkKeepsRelativePathsAndSizes() = runTest {
        val scan = SafStorageScanner(source).scan("root", atMillis = 1234)
        assertEquals(5, scan.totalFiles)
        assertEquals(1000, scan.totalBytes)
        assertFalse(scan.cancelled)
        assertFalse(scan.truncated)
        assertEquals(1234, scan.atMillis)
        val paths = scan.entries.map { it.relativePath }.toSet()
        assertTrue(paths.containsAll(setOf("DCIM/a.jpg", "DCIM/sub/c.png", "DCIM/b.mp4",
            "docs/d.pdf", "zero.txt")))
        val sub = scan.children("DCIM/sub")
        assertEquals(listOf("c.png"), sub.files.map { it.name })
    }

    @Test fun entryLimitIsReportedAsTruncated() = runTest {
        val scan = SafStorageScanner(source, maxEntries = 2).scan("root", 1)
        assertEquals(2, scan.totalFiles)
        assertTrue(scan.truncated)
    }

    @Test fun userCancellationReturnsPartialResultsFlagged() = runTest {
        val scanner = SafStorageScanner(source)
        var requested = false
        val scan = scanner.scan("root", 1, onProgress = {
            if (!requested) { requested = true; scanner.requestCancel() }
        })
        assertTrue(scan.cancelled)
        assertTrue(scan.totalFiles < 5)
    }

    @Test fun unreadableBranchIsTruncatedNotFatal() = runTest {
        val broken = FakeSource(mapOf(
            "root" to listOf(dir("gone", "gone"), file("ok", "ok.txt", 10, "text/plain")),
        ))
        val scan = SafStorageScanner(broken).scan("root", 2)
        assertTrue(scan.truncated)
        assertEquals(1, scan.totalFiles)
        assertEquals(10, scan.totalBytes)
    }

    @Test fun depthLimitDoesNotLoopForever() = runTest {
        val cyclic = FakeSource(mapOf(
            "root" to listOf(dir("child", "child")),
            "child" to listOf(dir("child", "child")),
        ))
        val scan = SafStorageScanner(cyclic, maxDepth = 3).scan("root", 3)
        assertTrue(scan.truncated)
        assertTrue(scan.entries.isEmpty())
    }
}
