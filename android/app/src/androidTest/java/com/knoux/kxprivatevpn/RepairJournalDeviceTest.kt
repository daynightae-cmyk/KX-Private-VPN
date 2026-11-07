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
        // Only this UUID-owned file is opened/removed, never the production journal.
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

            val approved = journal.load().take(1)
            val additional = RepairJournalEntry(6_000, "cache", "cache_remove", RepairOutcome.COMPLETE, 1, 1)
            journal.append(additional)
            assertEquals(1, journal.removeApproved(approved))
            assertTrue(journal.load().contains(additional))
            assertEquals(0, journal.removeApproved(approved))

            // Valid schema, but an unapproved survivor contains an escaped lone surrogate.
            // Mutation must fail without changing any original byte.
            val malformedUnicode = """[{"at":1000,"service":"cache","action":"remove","outcome":"COMPLETE","items":1,"bytes":1},{"at":2000,"service":"\uD800","action":"keep","outcome":"COMPLETE","items":1,"bytes":1}]"""
            file.writeText(malformedUnicode)
            try {
                journal.removeApproved(listOf(RepairJournalEntry(1000, "cache", "remove", RepairOutcome.COMPLETE, 1, 1)))
                org.junit.Assert.fail("Invalid Unicode survivor must prevent mutation")
            } catch (_: IllegalArgumentException) { }
            assertTrue(file.readBytes().contentEquals(malformedUnicode.toByteArray(Charsets.UTF_8)))

            file.writeText("not a journal")
            assertTrue(journal.load().isEmpty())
            for (operation in listOf<() -> Unit>(
                { journal.append(additional); Unit },
                { journal.removeApproved(approved); Unit },
                { journal.replace(emptyList()); Unit },
            )) {
                try {
                    operation()
                    org.junit.Assert.fail("Mutation must preserve corrupt originals")
                } catch (_: IllegalArgumentException) { }
                catch (_: IllegalStateException) { }
                assertEquals("not a journal", file.readText())
            }
        } finally {
            assertTrue(!file.exists() || file.delete())
            assertTrue(!backup.exists() || backup.delete())
        }
    }
}
