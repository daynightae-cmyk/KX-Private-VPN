package com.knoux.kxprivatevpn

import com.knoux.kxprivatevpn.features.DownloadReceipt
import com.knoux.kxprivatevpn.features.DownloadRescueUiState
import com.knoux.kxprivatevpn.platform.repair.DownloadAction
import com.knoux.kxprivatevpn.platform.repair.DownloadActionResult
import com.knoux.kxprivatevpn.platform.repair.DownloadCandidate
import com.knoux.kxprivatevpn.platform.repair.DownloadDocumentIdentity
import com.knoux.kxprivatevpn.platform.repair.DownloadExportCoordinator
import com.knoux.kxprivatevpn.platform.repair.DownloadExportEngine
import com.knoux.kxprivatevpn.platform.repair.DownloadHealth
import com.knoux.kxprivatevpn.platform.repair.DownloadTarget
import com.knoux.kxprivatevpn.platform.repair.freezeDownloadSelection
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.CancellationException

/** Only synthetic in-memory bytes; no provider, personal files, or destructive operations. */
class DownloadExportSafetyTest {
    private fun candidate(id: String) = DownloadCandidate(
        documentId = id, displayName = "$id.bin", mime = "application/octet-stream",
        bytes = 4L, lastModified = 0L, health = DownloadHealth.UNSUPPORTED,
    )
    private val first = DownloadTarget(candidate("a"), "content://fixture/document/a")
    private val second = DownloadTarget(candidate("b"), "content://fixture/document/b")
    private val source = DownloadDocumentIdentity(first.uri, "fixture", "a")
    private val destination = DownloadDocumentIdentity("content://fixture/document/c", "fixture", "c")
    private val payload = byteArrayOf(1, 2, 3, 4)

    @Test fun aSingleDocumentPickerCannotReserveZeroOrMultipleSources() {
        val coordinator = DownloadExportCoordinator()
        assertFalse(coordinator.prepare(emptyList(), busy = false, scanning = false))
        assertFalse(coordinator.prepare(listOf(first, second), busy = false, scanning = false))
        assertNull(coordinator.complete(destination.uri, busy = false, scanning = false))
        assertTrue(coordinator.prepare(listOf(first), busy = false, scanning = false))
        assertEquals(first, coordinator.complete(destination.uri, false, false)?.source)
    }

    @Test fun selectionAndUriChangesWhileThePickerIsOpenCannotReplaceTheFrozenSource() {
        val candidates = mutableListOf(first.candidate, second.candidate)
        val selected = mutableSetOf("a")
        val uris = mutableMapOf("a" to first.uri, "b" to second.uri)
        val coordinator = DownloadExportCoordinator()
        assertTrue(coordinator.prepare(freezeDownloadSelection(candidates, selected, uris), false, false))
        selected.clear()
        selected.add("b")
        candidates[0] = candidate("a").copy(bytes = 200L)
        uris["a"] = "content://fixture/document/replacement"
        val request = coordinator.complete(destination.uri, false, false)!!
        assertEquals(first, request.source)
        assertEquals(destination.uri, request.targetUri)
    }

    @Test fun cancellationClearsTheReservationWithoutExportingANewSelection() {
        val coordinator = DownloadExportCoordinator()
        assertTrue(coordinator.prepare(listOf(first), false, false))
        assertNull(coordinator.complete(null, false, false))
        assertNull(coordinator.complete(destination.uri, false, false))
        assertTrue(coordinator.prepare(listOf(second), false, false))
        assertEquals(second, coordinator.complete(destination.uri, false, false)?.source)
    }

    @Test fun doubleSubmitCannotReplaceTheReservationOrConsumeItTwice() {
        val coordinator = DownloadExportCoordinator()
        assertTrue(coordinator.prepare(listOf(first), false, false))
        assertFalse(coordinator.prepare(listOf(second), false, false))
        assertEquals(first, coordinator.complete(destination.uri, false, false)?.source)
        assertNull(coordinator.complete(destination.uri, false, false))
    }

    @Test fun busyAndScanningRejectBothPreparationAndLatePickerResults() {
        listOf(true to false, false to true).forEach { (busy, scanning) ->
            val coordinator = DownloadExportCoordinator()
            assertFalse(coordinator.prepare(listOf(first), busy, scanning))
            assertNull(coordinator.complete(destination.uri, false, false))
            assertTrue(coordinator.prepare(listOf(first), false, false))
            assertNull(coordinator.complete(destination.uri, busy, scanning))
            assertNull(coordinator.complete(destination.uri, false, false))
        }
    }

    @Test fun missingStaleOrAmbiguousSelectedIdsCannotAuthorizeAnAction() {
        val candidates = listOf(first.candidate, second.candidate)
        assertTrue(freezeDownloadSelection(candidates, setOf("a"), emptyMap()).isEmpty())
        assertTrue(freezeDownloadSelection(candidates, setOf("missing"), mapOf("a" to first.uri)).isEmpty())
        assertTrue(freezeDownloadSelection(listOf(first.candidate, first.candidate),
            setOf("a"), mapOf("a" to first.uri)).isEmpty())
        assertTrue(freezeDownloadSelection(candidates, setOf("a", "missing"),
            mapOf("a" to first.uri)).isEmpty())
    }

    @Test fun uiExportPolicyRequiresExactlyOneSelectionAndAnIdleState() {
        val selected = DownloadRescueUiState(loading = false,
            candidates = listOf(first.candidate, second.candidate), selected = setOf("a"))
        assertTrue(selected.canExport)
        listOf(
            selected.copy(selected = emptySet()), selected.copy(selected = setOf("a", "b")),
            selected.copy(selected = setOf("missing")), selected.copy(loading = true),
            selected.copy(busy = true), selected.copy(scanning = true), selected.copy(exportPending = true),
            selected.copy(removalTargets = listOf(first)),
            selected.copy(candidates = listOf(first.candidate, first.candidate)),
        ).forEach { assertFalse(it.canExport) }
    }

    @Test fun deleteApprovalRetainsExactFrozenTargetsAndRejectsConcurrentSubmission() {
        val selected = mutableSetOf("a")
        val uris = mutableMapOf("a" to first.uri, "b" to second.uri)
        val approved = freezeDownloadSelection(listOf(first.candidate, second.candidate), selected, uris)
        val state = DownloadRescueUiState(loading = false, removalTargets = approved)
        selected.add("b")
        uris["a"] = second.uri
        assertEquals(listOf(first), state.copy(selected = selected).removalTargets)
        assertTrue(state.canConfirmRemove)
        assertFalse(state.copy(busy = true).canConfirmRemove)
        assertFalse(state.copy(scanning = true).canConfirmRemove)
        assertFalse(state.copy(exportPending = true).canConfirmRemove)
        assertFalse(state.copy(loading = true).canConfirmRemove)
        assertFalse(state.copy(removalTargets = emptyList()).canConfirmRemove)
    }

    @Test fun automaticRescansPreserveReceiptsButDeliberateScansClearThem() {
        DownloadAction.entries.forEach { action ->
            val receipt = DownloadReceipt(action, 1, 0, 1, 0L, 0L,
                listOf(DownloadActionResult("a", "a.bin", action, false)), 123L)
            val state = DownloadRescueUiState(loading = false, receipt = receipt)
            val refresh = state.startingScan(clearReceipt = false)
            assertTrue(refresh.scanning)
            assertSame(receipt, refresh.receipt)
            assertSame(receipt, refresh.copy(scanning = false).receipt)
            assertSame(receipt, refresh.copy(scanning = false, scanFailed = true).receipt)
            assertNull(state.startingScan(clearReceipt = true).receipt)
        }
    }

    @Test fun sameUriAndSafAliasesAreRejectedBeforeOpeningAnyStream() {
        val aliases = listOf(source, source.copy(uri = "content://fixture/tree/root/document/a"))
        aliases.forEach { target ->
            val result = DownloadExportEngine.export(source, target, 4L,
                openSource = { error("must not open source") },
                openTarget = { error("must not truncate original") },
                readTarget = { error("must not read target") })
            assertFalse(result.succeeded)
            assertEquals("same_document", result.message)
            assertEquals(0L, result.bytesCopied)
        }
        assertFalse(source.sameDocument(source.copy(uri = "other", authority = "other")))
        assertFalse(source.sameDocument(destination))
        assertFalse(DownloadDocumentIdentity("one").sameDocument(DownloadDocumentIdentity("two")))
    }

    private fun exportFixture(
        expected: Long = payload.size.toLong(),
        readback: (ByteArray) -> InputStream? = { ByteArrayInputStream(it) },
    ): DownloadActionResult {
        val original = payload.copyOf()
        var closed = false
        var readbacks = 0
        val output = object : ByteArrayOutputStream() {
            override fun close() { super.close(); closed = true }
        }
        val result = DownloadExportEngine.export(source, destination, expected,
            openSource = { ByteArrayInputStream(original) }, openTarget = { output },
            readTarget = {
                assertTrue("readback must follow writer close", closed)
                readbacks++
                readback(output.toByteArray())
            })
        assertArrayEquals("the original must never change", payload, original)
        if (expected == payload.size.toLong()) assertEquals(1, readbacks)
        return result
    }

    @Test fun successRequiresReadingBackTheClosedDestinationAndMatchingItsHash() {
        val result = exportFixture()
        assertTrue(result.succeeded)
        assertEquals(4L, result.bytesCopied)
        assertEquals("copied", result.message)
    }

    @Test fun sameLengthAlterationIsNotMistakenForASuccessfulCopy() {
        val result = exportFixture { bytes ->
            ByteArrayInputStream(bytes.copyOf().apply { this[0] = 99 })
        }
        assertFalse(result.succeeded)
        assertEquals("readback_mismatch", result.message)
        assertEquals(0L, result.bytesCopied)
    }

    @Test fun truncatedAndAppendedReadbacksFailWithoutClaimingCopiedBytes() {
        listOf(payload.copyOf(3), payload + byteArrayOf(5)).forEach { bytes ->
            val result = exportFixture { ByteArrayInputStream(bytes) }
            assertFalse(result.succeeded)
            assertEquals("readback_mismatch", result.message)
            assertEquals(0L, result.bytesCopied)
        }
    }

    @Test fun unreadableDestinationOrReadbackIoFailureCannotReportSuccess() {
        val results = listOf(exportFixture { null }, exportFixture { throw IOException("unreadable") })
        results.forEach {
            assertFalse(it.succeeded)
            assertEquals("export_failed", it.message)
            assertEquals(0L, it.bytesCopied)
        }
    }

    @Test fun staleSourceSizeIsAFailureEvenWhenTheWriterAcceptedBytes() {
        listOf(3L, 5L).forEach { expected ->
            val result = exportFixture(expected)
            assertFalse(result.succeeded)
            assertEquals("size_mismatch", result.message)
            assertEquals(0L, result.bytesCopied)
        }
    }

    @Test fun unknownSourceSizeDoesNotOpenTheDestination() {
        val result = DownloadExportEngine.export(source, destination, -1L,
            openSource = { error("must reject unknown size first") },
            openTarget = { error("must not write") }, readTarget = { error("must not read") })
        assertFalse(result.succeeded)
        assertEquals("size_unknown", result.message)
    }

    @Test fun aWriterThatDropsAllBytesCannotClaimSuccess() {
        val result = DownloadExportEngine.export(source, destination, 4L,
            openSource = { ByteArrayInputStream(payload) },
            openTarget = { object : java.io.OutputStream() { override fun write(value: Int) = Unit } },
            readTarget = { ByteArrayInputStream(byteArrayOf()) })
        assertFalse(result.succeeded)
        assertEquals("readback_mismatch", result.message)
    }

    @Test fun zeroByteCopyStillRequiresAReadableEmptyDestination() {
        var read = false
        val result = DownloadExportEngine.export(source, destination, 0L,
            openSource = { ByteArrayInputStream(byteArrayOf()) },
            openTarget = { ByteArrayOutputStream() },
            readTarget = { read = true; ByteArrayInputStream(byteArrayOf()) })
        assertTrue(read)
        assertTrue(result.succeeded)
        assertEquals(0L, result.bytesCopied)
    }

    @Test fun cancellationIsPropagatedRatherThanReportedAsSuccess() {
        assertThrows(CancellationException::class.java) {
            DownloadExportEngine.export(source, destination, 4L,
                openSource = { ByteArrayInputStream(payload) }, openTarget = { ByteArrayOutputStream() },
                readTarget = { ByteArrayInputStream(payload) },
                checkActive = { throw CancellationException("cancelled") })
        }
    }
}
