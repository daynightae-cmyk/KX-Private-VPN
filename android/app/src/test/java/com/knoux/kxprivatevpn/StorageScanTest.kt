package com.knoux.kxprivatevpn

import com.knoux.kxprivatevpn.platform.storage.StorageCategory
import com.knoux.kxprivatevpn.platform.storage.StorageEntry
import com.knoux.kxprivatevpn.platform.storage.StorageScan
import com.knoux.kxprivatevpn.platform.storage.categorize
import com.knoux.kxprivatevpn.platform.storage.reconcileStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure aggregation rules: every byte is counted once per category, paths stay relative. */
class StorageScanTest {
    private fun entry(path: String, bytes: Long, mime: String = "") =
        StorageEntry(path, bytes, mime, 0)

    private val scan = StorageScan(listOf(
        entry("DCIM/a.jpg", 100, "image/jpeg"),
        entry("DCIM/b.mp4", 200, "video/mp4"),
        entry("DCIM/sub/c.png", 300, "image/png"),
        entry("docs/c.pdf", 400, "application/pdf"),
        entry("docs/song.mp3", 500, "audio/mpeg"),
        entry("misc/archive.zip", 600, "application/zip"),
        entry("misc/blob.bin", 700, "application/octet-stream"),
    ))

    @Test fun categoriesReconcileWithEntries() {
        val totals = scan.categories()
        assertEquals(scan.totalBytes, totals.sumOf { it.bytes })
        assertEquals(scan.totalFiles, totals.sumOf { it.files })
        assertEquals(400, totals.first { it.category == StorageCategory.IMAGES }.bytes)
        assertEquals(200, totals.first { it.category == StorageCategory.VIDEOS }.bytes)
        assertEquals(500, totals.first { it.category == StorageCategory.AUDIO }.bytes)
        assertEquals(1000, totals.first { it.category == StorageCategory.DOCUMENTS }.bytes)
        assertEquals(700, totals.first { it.category == StorageCategory.OTHER }.bytes)
    }

    @Test fun mimeAndExtensionClassificationAgrees() {
        assertEquals(StorageCategory.IMAGES, categorize("image/heic", "x"))
        assertEquals(StorageCategory.IMAGES, categorize("", "x.JPG"))
        assertEquals(StorageCategory.VIDEOS, categorize("video/webm", "x"))
        assertEquals(StorageCategory.AUDIO, categorize(null, "x.flac"))
        assertEquals(StorageCategory.DOCUMENTS, categorize("text/plain", "x"))
        assertEquals(StorageCategory.DOCUMENTS, categorize("", "x.docx"))
        assertEquals(StorageCategory.OTHER, categorize("application/octet-stream", "x"))
        assertEquals(StorageCategory.OTHER, categorize("application/vnd.unknown", "noext"))
    }

    @Test fun childrenAggregateDescendantsAndKeepRelativePaths() {
        val root = scan.children()
        assertEquals(listOf("misc", "docs", "DCIM"), root.folders.map { it.name })
        assertEquals(600, root.folders.first { it.name == "DCIM" }.bytes)
        assertEquals(3, root.folders.first { it.name == "DCIM" }.files)
        assertTrue(root.files.isEmpty())

        val dcim = scan.children("DCIM")
        assertEquals(listOf("sub"), dcim.folders.map { it.name })
        assertEquals(300, dcim.folders.single().bytes)
        assertEquals(1, dcim.folders.single().files)
        assertEquals(listOf("b.mp4", "a.jpg"), dcim.files.map { it.name })
        assertEquals(200, dcim.files.first().bytes)

        val sub = scan.children("DCIM/sub")
        assertEquals(listOf("c.png"), sub.files.map { it.name })
        assertEquals("DCIM/sub/c.png", sub.files.first().path)
    }

    @Test fun topFilesIsBoundedAndOrdered() {
        assertEquals(2, scan.topFiles(2).size)
        assertEquals(700, scan.topFiles(2).first().bytes)
        assertTrue(scan.topFiles(0).isEmpty())
    }

    @Test fun reconcileClampsAndNeverInvents() {
        assertNull(reconcileStorage(null, 10, 5))
        assertNull(reconcileStorage(0, 0, 0))
        val value = reconcileStorage(1000, 400, 700)!!
        assertEquals(600, value.usedBytes)
        assertEquals(400, value.freeBytes)
        assertEquals(600, value.accessibleBytes)
        assertEquals(0, value.unexplainedBytes)
        val over = reconcileStorage(1000, 400, 9000)!!
        assertEquals(600, over.accessibleBytes)
        assertEquals(0, over.unexplainedBytes)
        val clampedFree = reconcileStorage(1000, 2000, 0)!!
        assertEquals(1000, clampedFree.freeBytes)
        assertEquals(0, clampedFree.usedBytes)
    }
}
