package com.knoux.kxprivatevpn.platform.repair

import com.knoux.kxprivatevpn.platform.security.ShieldRetention
import com.knoux.kxprivatevpn.platform.storage.CachePreview
import com.knoux.kxprivatevpn.platform.usage.UsageRecord

/**
 * KX Repair Autopilot.
 *
 * The autopilot never invents a finding. It aggregates only what the preceding tools really
 * measured: the app's own cache files (SHA-256 + size + mtime from [CachePreview]) and the KX
 * history that fell out of the retention window. Every target is shown with its real side effect,
 * can be deselected, and is acted on only after an explicit approval. The plan is a pure function
 * of its inputs so the same measurement always proposes the same targets in the same order.
 */
enum class AutopilotKind {
    /** A real file inside KX's own cacheDir. Deleting it frees its measured bytes. */
    CACHE_FILE,
    /** The aged slice of KX's repair journal and usage history. Pruning frees no file bytes. */
    HISTORY,
}

/**
 * One proposed, individually selectable action. [title] is the human label (for a cache file it
 * is the relative path the person recognises); [bytes] is measured, never estimated. A history
 * target reports zero bytes because pruning a journal does not delete a measurable file.
 */
data class AutopilotTarget(
    val id: String,
    val kind: AutopilotKind,
    val title: String,
    val detail: String,
    val bytes: Long,
    val destructive: Boolean,
)

/**
 * A deterministic proposal built from one measurement. [cacheMeasured] is false when the cache
 * preview could not be read, so the screen states the gap instead of pretending the cache is empty.
 */
data class AutopilotPlan(
    val targets: List<AutopilotTarget> = emptyList(),
    val measuredAtMillis: Long = 0,
    val cacheMeasured: Boolean = false,
    val cacheLimited: Boolean = false,
    val agedJournal: Int = 0,
    val agedUsage: Int = 0,
    val retentionDays: Int = ShieldRetention.DEFAULT_DAYS,
) {
    val isEmpty: Boolean get() = targets.isEmpty()
    val cacheTargets: List<AutopilotTarget> get() = targets.filter { it.kind == AutopilotKind.CACHE_FILE }
    val historyTarget: AutopilotTarget? get() = targets.firstOrNull { it.kind == AutopilotKind.HISTORY }
    val totalBytes: Long get() = targets.sumOf { it.bytes }
    fun selectedBytes(selected: Set<String>): Long = targets.filter { it.id in selected }.sumOf { it.bytes }
}

/** What an approved run really did. Every count is read back from the operation, never guessed. */
data class AutopilotReceipt(
    val deleted: Int,
    val pruned: Int,
    val bytesFreed: Long,
    val skipped: List<String>,
    val failed: List<String>,
    val complete: Boolean,
    val cancelled: Boolean,
    val atMillis: Long,
    val detail: String,
)

/**
 * Builds the proposal. Cache files come first (largest first, then path for a stable tie-break),
 * followed by a single history target when any journal or usage entry has aged out. Nothing here
 * touches the filesystem; execution and verification live in the ViewModel.
 */
object AutopilotPlanner {
    const val HISTORY_ID = "history"
    fun cacheId(path: String): String = "cache:$path"

    fun build(
        cache: CachePreview?,
        journal: List<RepairJournalEntry>,
        usage: List<UsageRecord>,
        nowMillis: Long,
        retentionDays: Int = ShieldRetention.DEFAULT_DAYS,
    ): AutopilotPlan {
        val days = retentionDays.coerceAtLeast(1)
        val cacheTargets = cache?.files.orEmpty()
            .sortedWith(compareByDescending<com.knoux.kxprivatevpn.platform.storage.CacheCandidate> { it.bytes }
                .thenBy { it.path })
            .map { candidate ->
                AutopilotTarget(
                    id = cacheId(candidate.path),
                    kind = AutopilotKind.CACHE_FILE,
                    title = candidate.path,
                    detail = "kx_cache_file",
                    bytes = candidate.bytes.coerceAtLeast(0),
                    destructive = true,
                )
            }
        val agedJournal = journal.count { ShieldRetention.isExpired(it.atMillis, nowMillis, days) }
        val agedUsage = usage.count { ShieldRetention.isExpired(it.startedAtMillis, nowMillis, days) }
        val targets = cacheTargets.toMutableList()
        if (agedJournal + agedUsage > 0) {
            targets += AutopilotTarget(
                id = HISTORY_ID,
                kind = AutopilotKind.HISTORY,
                title = "aged_kx_history",
                detail = "$agedJournal+$agedUsage",
                bytes = 0L,
                destructive = true,
            )
        }
        return AutopilotPlan(
            targets = targets,
            measuredAtMillis = nowMillis,
            cacheMeasured = cache != null,
            cacheLimited = cache?.limited ?: false,
            agedJournal = agedJournal,
            agedUsage = agedUsage,
            retentionDays = days,
        )
    }
}