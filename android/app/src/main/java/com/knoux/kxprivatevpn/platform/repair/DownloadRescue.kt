package com.knoux.kxprivatevpn.platform.repair

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.concurrent.CancellationException

/**
 * Download Rescue inspects files the user already owns and reports what can be honestly said
 * about them. Nothing here repairs bytes: a damaged download can be classified, exported,
 * renamed or removed, but its content is never silently rewritten.
 */

/**
 * The measured condition of one download.
 *
 * [TRUNCATED] means the beginning of the file is valid but the end is missing, which is what an
 * interrupted download looks like. [CORRUPTED] means the header itself does not match the
 * format. [UNSUPPORTED] means the format has no validator here, so no claim is made about it.
 */
enum class DownloadHealth {
    INTACT,
    ZERO_BYTE,
    TRUNCATED,
    CORRUPTED,
    UNSUPPORTED,
    UNREADABLE,
    ;

    val needsAttention: Boolean get() = this != INTACT
    val isDeletable: Boolean get() = this == ZERO_BYTE || this == TRUNCATED || this == CORRUPTED
}

/** The longest possible ZIP end of central directory record, comment included. */
private const val ZIP_TAIL_WINDOW = 22 + 65535

/** Formats with a real structural validator. Anything else is reported as unsupported. */
enum class DownloadFormat(
    val mime: String,
    val extension: String,
    val requiresTail: Boolean,
    /** How many trailing bytes have to be read for the end marker to be visible. */
    val tailWindow: Int,
) {
    PDF("application/pdf", "pdf", true, 32),
    PNG("image/png", "png", true, 32),
    JPEG("image/jpeg", "jpg", true, 32),
    MP4("video/mp4", "mp4", false, 0),
    /** The end of central directory record is 22 bytes and may be followed by a comment. */
    ZIP("application/zip", "zip", true, ZIP_TAIL_WINDOW),
    ;

    companion object {
        /**
         * The provider MIME type decides when it is known. The extension is only consulted when
         * the provider reports nothing usable, and the source of the decision is kept so the UI
         * can say how the file was recognised.
         */
        fun fromMime(mime: String, displayName: String): Pair<DownloadFormat, Boolean>? {
            val value = mime.lowercase().trim()
            val known = entries.firstOrNull { it.mime == value }
            if (known != null) return known to true
            val lower = displayName.lowercase()
            val byExtension = entries.firstOrNull { lower.endsWith(".${it.extension}") }
            if (byExtension != null) return byExtension to false
            return null
        }
    }
}

/** One download the user owns, with the bytes actually measured by the provider. */
data class DownloadCandidate(
    val documentId: String,
    val displayName: String,
    val mime: String,
    val bytes: Long,
    val lastModified: Long,
    val health: DownloadHealth,
    val format: DownloadFormat? = null,
    val recognisedByMime: Boolean = false,
    /** Why the file received its state. Always a measured reason, never a guess. */
    val reason: String = "",
)

/** The real outcome of one export, rename or removal. */
data class DownloadActionResult(
    val documentId: String,
    val displayName: String,
    val action: DownloadAction,
    val succeeded: Boolean,
    val bytesCopied: Long = 0,
    val bytesExpected: Long = 0,
    /** Measured immediately before a verified removal; zero for every other outcome. */
    val bytesRemoved: Long = 0,
    val message: String = "",
)

enum class DownloadAction { EXPORT, RENAME, REMOVE }

/**
 * Removal is only reported as a removal when the document was measured present before the
 * provider call and measurably absent afterwards. The provider's boolean alone is not proof.
 */
enum class DownloadRemovalVerdict(val code: String) {
    REMOVED("removed"),
    ALREADY_ABSENT("already_absent"),
    STILL_PRESENT("still_present"),
    DELETE_REJECTED("delete_rejected"),
    UNVERIFIED("presence_unverified"),
}

fun downloadRemovalVerdict(
    presentBefore: Boolean?,
    deleteReturned: Boolean,
    presentAfter: Boolean?,
): DownloadRemovalVerdict = when {
    presentBefore == false -> DownloadRemovalVerdict.ALREADY_ABSENT
    presentBefore == null -> DownloadRemovalVerdict.UNVERIFIED
    !deleteReturned -> DownloadRemovalVerdict.DELETE_REJECTED
    presentAfter == false -> DownloadRemovalVerdict.REMOVED
    presentAfter == true -> DownloadRemovalVerdict.STILL_PRESENT
    else -> DownloadRemovalVerdict.UNVERIFIED
}

/** Only bytes measured for a document proven present before and absent after count as gone. */
fun verifiedRemovalBytes(results: List<DownloadActionResult>): Long =
    results.filter { it.action == DownloadAction.REMOVE && it.succeeded }
        .sumOf { it.bytesRemoved.coerceAtLeast(0L) }

/** Freeze both the inspected candidate and its URI before asking for approval or a destination. */
data class DownloadTarget(val candidate: DownloadCandidate, val uri: String)

fun freezeDownloadSelection(
    candidates: List<DownloadCandidate>,
    selected: Set<String>,
    uriByDocument: Map<String, String>,
): List<DownloadTarget> {
    val matches = candidates.filter { it.documentId in selected }
    // Provider ids can collide across sources. Never guess which URI the user intended.
    if (matches.size != selected.size || matches.map { it.documentId }.toSet() != selected) return emptyList()
    return matches.map { candidate ->
        val uri = uriByDocument[candidate.documentId]?.takeIf { it.isNotBlank() } ?: return emptyList()
        DownloadTarget(candidate, uri)
    }
}

data class DownloadExportRequest(val source: DownloadTarget, val targetUri: String)

/** Main-thread, one-shot picker reservation. A result never consults the current selection. */
class DownloadExportCoordinator {
    private var pending: DownloadTarget? = null

    fun prepare(selection: List<DownloadTarget>, busy: Boolean, scanning: Boolean): Boolean {
        if (busy || scanning || pending != null) return false
        pending = selection.singleOrNull()?.takeIf { it.uri.isNotBlank() } ?: return false
        return true
    }

    /** Null cancels; rejected and duplicate callbacks cannot retain or reuse the reservation. */
    fun complete(targetUri: String?, busy: Boolean, scanning: Boolean): DownloadExportRequest? {
        val source = pending
        pending = null
        if (source == null || targetUri.isNullOrBlank() || busy || scanning) return null
        return DownloadExportRequest(source, targetUri)
    }
}

/** SAF tree and standalone document URIs may identify the same file. */
data class DownloadDocumentIdentity(
    val uri: String,
    val authority: String? = null,
    val documentId: String? = null,
) {
    fun sameDocument(other: DownloadDocumentIdentity): Boolean =
        uri == other.uri || (authority != null && authority == other.authority &&
            documentId != null && documentId == other.documentId)
}

/** Stream-only engine: one source, one destination, no deletion and no success before readback. */
object DownloadExportEngine {
    fun export(
        source: DownloadDocumentIdentity,
        target: DownloadDocumentIdentity,
        expected: Long,
        openSource: () -> InputStream?,
        openTarget: () -> OutputStream?,
        readTarget: () -> InputStream?,
        checkActive: () -> Unit = {},
    ): DownloadActionResult {
        fun result(message: String, verifiedBytes: Long = 0L) = DownloadActionResult(
            documentId = source.uri,
            displayName = target.uri,
            action = DownloadAction.EXPORT,
            succeeded = message == "copied",
            bytesCopied = verifiedBytes,
            bytesExpected = expected,
            message = message,
        )
        // These checks must precede opening the destination in a truncating mode.
        if (source.sameDocument(target)) return result("same_document")
        if (expected < 0L) return result("size_unknown")
        return try {
            val sourceHash = MessageDigest.getInstance("SHA-256")
            var copied = 0L
            checkActive()
            openSource()?.use { input ->
                checkActive()
                openTarget()?.use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        checkActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        if (read == 0) continue
                        copied += read
                        if (copied > expected) return result("size_mismatch")
                        sourceHash.update(buffer, 0, read)
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                } ?: throw IOException("target not writable")
            } ?: throw IOException("source not readable")
            if (copied != expected) return result("size_mismatch")

            // Reopen only after the writer is closed; provider write counts are not proof.
            val targetHash = MessageDigest.getInstance("SHA-256")
            var verified = 0L
            readTarget()?.use { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    checkActive()
                    val read = input.read(buffer)
                    if (read < 0) break
                    if (read == 0) continue
                    verified += read
                    if (verified > copied) return result("readback_mismatch")
                    targetHash.update(buffer, 0, read)
                }
            } ?: throw IOException("target not readable")
            if (verified != copied || !sourceHash.digest().contentEquals(targetHash.digest())) {
                result("readback_mismatch")
            } else {
                result("copied", verified)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Keep any partial destination: it may have existed before this operation.
            result("export_failed")
        }
    }
}

/**
 * Structural validators. Each one compares bytes the provider actually returned; no signature
 * is inferred from the file name alone.
 */
object DownloadValidator {
    const val WINDOW = 32

    private val PDF_HEAD = "%PDF-".toByteArray(Charsets.US_ASCII)
    private val PDF_TAIL = "%%EOF".toByteArray(Charsets.US_ASCII)
    private val PNG_HEAD = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    private val PNG_TAIL = "IEND".toByteArray(Charsets.US_ASCII)
    private val JPEG_HEAD = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
    private val JPEG_TAIL = byteArrayOf(0xFF.toByte(), 0xD9.toByte())
    private val ZIP_HEAD = byteArrayOf(0x50, 0x4B, 0x03, 0x04)
    private val ZIP_EMPTY = byteArrayOf(0x50, 0x4B, 0x05, 0x06)
    private val ZIP_TAIL = byteArrayOf(0x50, 0x4B, 0x05, 0x06)
    private val MP4_FTYP = "ftyp".toByteArray(Charsets.US_ASCII)

    /**
     * Classifies one download from the bytes that were really read.
     *
     * @param bytes the provider reported size.
     * @param head the first bytes of the document, or null when they could not be read.
     * @param tail the last bytes of the document, or null when they could not be read.
     */
    fun classify(
        mime: String,
        displayName: String,
        bytes: Long,
        head: ByteArray?,
        tail: ByteArray?,
    ): DownloadCandidate {
        val recognised = DownloadFormat.fromMime(mime, displayName)
        val format = recognised?.first
        val byMime = recognised?.second == true
        val candidate = DownloadCandidate(
            documentId = "",
            displayName = displayName,
            mime = mime,
            bytes = bytes,
            lastModified = 0,
            health = DownloadHealth.UNSUPPORTED,
            format = format,
            recognisedByMime = byMime,
        )
        if (bytes <= 0L) {
            return candidate.copy(health = DownloadHealth.ZERO_BYTE, reason = "no_bytes")
        }
        if (format == null) {
            return candidate.copy(health = DownloadHealth.UNSUPPORTED, reason = "no_validator")
        }
        val headBytes = head
        val tailBytes = tail
        if (headBytes == null || headBytes.isEmpty()) {
            return candidate.copy(health = DownloadHealth.UNREADABLE, reason = "unreadable")
        }
        if (!headMatches(format, headBytes)) {
            return candidate.copy(health = DownloadHealth.CORRUPTED, reason = "bad_header")
        }
        if (format.requiresTail) {
            val tail = tailBytes
            if (tail == null || tail.isEmpty()) {
                return candidate.copy(health = DownloadHealth.UNREADABLE, reason = "unreadable")
            }
            if (!tailMatches(format, tail)) {
                return candidate.copy(health = DownloadHealth.TRUNCATED, reason = "missing_tail")
            }
        }
        return candidate.copy(health = DownloadHealth.INTACT, reason = "verified")
    }

    fun headMatches(format: DownloadFormat, head: ByteArray): Boolean = when (format) {
        DownloadFormat.PDF -> startsWith(head, PDF_HEAD)
        DownloadFormat.PNG -> startsWith(head, PNG_HEAD)
        DownloadFormat.JPEG -> startsWith(head, JPEG_HEAD)
        DownloadFormat.ZIP -> startsWith(head, ZIP_HEAD) || startsWith(head, ZIP_EMPTY)
        DownloadFormat.MP4 -> head.size >= 8 && containsAt(head, MP4_FTYP, 4)
    }

    fun tailMatches(format: DownloadFormat, tail: ByteArray): Boolean = when (format) {
        DownloadFormat.PDF -> contains(tail, PDF_TAIL)
        DownloadFormat.PNG -> contains(tail, PNG_TAIL)
        DownloadFormat.JPEG -> contains(tail, JPEG_TAIL)
        DownloadFormat.ZIP -> contains(tail, ZIP_TAIL)
        DownloadFormat.MP4 -> true
    }

    /** The tail window a format needs, never larger than the file itself. */
    fun tailWindow(format: DownloadFormat?, bytes: Long): Int {
        if (format == null || !format.requiresTail) return 0
        if (bytes <= 0L) return 0
        return minOf(format.tailWindow.toLong(), bytes).toInt()
    }

    private fun startsWith(data: ByteArray, prefix: ByteArray): Boolean {
        if (data.size < prefix.size) return false
        for (index in prefix.indices) if (data[index] != prefix[index]) return false
        return true
    }

    private fun containsAt(data: ByteArray, needle: ByteArray, offset: Int): Boolean {
        if (data.size < offset + needle.size) return false
        for (index in needle.indices) if (data[offset + index] != needle[index]) return false
        return true
    }

    private fun contains(data: ByteArray, needle: ByteArray): Boolean {
        if (needle.isEmpty() || data.size < needle.size) return false
        for (start in 0..(data.size - needle.size)) {
            var matched = true
            for (index in needle.indices) {
                if (data[start + index] != needle[index]) { matched = false; break }
            }
            if (matched) return true
        }
        return false
    }
}

/** The measured summary of a Download Rescue scan. */
data class DownloadScanSummary(
    val total: Int,
    val inspected: Int,
    val byHealth: Map<DownloadHealth, Int>,
    val bytesInspected: Long,
    val truncated: Boolean,
    val failed: Boolean,
    val atMillis: Long,
) {
    val attention: Int get() = byHealth.entries.filter { it.key.needsAttention }.sumOf { it.value }
    val recoverableBytes: Long get() = 0L
}
