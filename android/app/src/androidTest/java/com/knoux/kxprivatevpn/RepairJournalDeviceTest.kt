package com.knoux.kxprivatevpn

import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.platform.repair.RepairJournal
import com.knoux.kxprivatevpn.platform.repair.RepairJournalEntry
import com.knoux.kxprivatevpn.platform.repair.RepairOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.UUID

/**
 * AtomicFile behavior is Android-specific; the pure ordering/corruption codec is covered on the
 * JVM. This test proves the on-device journal is bounded and never rewrites corrupt input on read.
 */
class RepairJournalDeviceTest {
    @Test fun journalIsBoundedPersistentAndPreservesCorruptInput() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "repair-journal-${UUID.randomUUID()}.json"
        val file = File(context.noBackupFilesDir, name)
        val backup = File(file.path + ".bak")
        try {
            val journal = RepairJournal(file, limit = 3)
            assertTrue(journal.load().isEmpty())
            (1..5).forEach { index ->
                journal.append(RepairJournalEntry(
                    atMillis = index * 1_000L,
                    serviceKey = "cache",
                    action = "cache_remove",
                    outcome = if (index == 5) RepairOutcome.PARTIAL else RepairOutcome.COMPLETE,
                    items = index,
                    bytes = index * 100L,
                ))
            }
            val loaded = journal.load()
            assertEquals(3, loaded.size)
            assertEquals(5_000L, loaded.first().atMillis)
            assertEquals(3_000L, loaded.last().atMillis)
            assertEquals(RepairOutcome.PARTIAL, loaded.first().outcome)

            file.writeText("not a journal")
            assertTrue(journal.load().isEmpty())
            assertEquals("not a journal", file.readText())
        } finally {
            file.delete()
            backup.delete()
        }
    }
}
