package com.knoux.kxprivatevpn

import com.knoux.kxprivatevpn.platform.storage.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class CacheMaintenanceTest {
    @get:Rule val temp = TemporaryFolder()
    @Test fun approvalAndExactSelectionProtectSentinels() = runBlocking {
        val cache = temp.newFolder("cache")
        val target = File(cache, "target.tmp").apply { writeBytes(ByteArray(41)) }
        val sentinel = File(cache, "sentinel.tmp").apply { writeText("keep") }
        val outside = temp.newFile("profile.kx").apply { writeText("encrypted profile sentinel") }
        val tool = CacheMaintenance(cache)
        val preview = tool.preview()
        try { tool.removeApproved(preview, setOf("target.tmp"), false); fail() } catch (_: IllegalArgumentException) { }
        assertTrue(target.exists())
        val receipt = tool.removeApproved(preview, setOf("target.tmp"), true)
        assertEquals(1, receipt.deleted)
        assertEquals(41L, receipt.removedFileBytes)
        assertFalse(target.exists())
        assertEquals("keep", sentinel.readText())
        assertTrue(outside.exists())
    }
    @Test fun changedFilesAndTraversalAreNeverRemoved() = runBlocking {
        val cache = temp.newFolder("cache")
        val target = File(cache, "target.tmp").apply { writeText("before") }
        val outside = temp.newFile("sentinel")
        val tool = CacheMaintenance(cache)
        val preview = tool.preview()
        val modified = target.lastModified()
        target.writeText("AFTER!"); target.setLastModified(modified) // Same size + timestamp; hash must catch it.
        assertEquals(listOf("target.tmp"), tool.removeApproved(preview, setOf("target.tmp"), true).skipped)
        try { tool.removeApproved(preview, setOf("../sentinel"), true); fail() } catch (_: IllegalArgumentException) { }
        assertTrue(target.exists()); assertTrue(outside.exists())
    }
    @Test fun emptySelectionIsAnHonestNoOp() = runBlocking {
        val cache = temp.newFolder("cache")
        val target = File(cache, "x").apply { writeText("keep") }
        val tool = CacheMaintenance(cache)
        val result = tool.removeApproved(tool.preview(), emptySet(), true)
        assertEquals(0, result.deleted); assertEquals(0L, result.removedFileBytes); assertTrue(target.exists())
    }
}
