package com.knoux.kxprivatevpn.platform.security

/**
 * KX Session Shield.
 *
 * Everything the shield lists belongs to KX itself: the WireGuard profiles the person imported,
 * the repair journal KX wrote, the usage history KX measured and the session hint KX stored.
 * The shield never reads, lists or touches another application's data, and it never shows a
 * secret: an entry that holds one says so and nothing more.
 */

enum class ShieldKind {
    /** An imported WireGuard profile. It carries a private key inside the encrypted store. */
    PROFILE,
    /** A repair operation KX performed and journaled. */
    REPAIR_HISTORY,
    /** A measured usage record of a KX session. */
    USAGE_HISTORY,
    /** The advisory hint that a tunnel was active when the process last stopped. */
    SESSION_HINT,
}

data class ShieldEntry(
    val id: String,
    val kind: ShieldKind,
    val title: String,
    val detail: String,
    val atMillis: Long,
    /** True when the underlying item holds a credential. The value itself never leaves KX. */
    val holdsSecret: Boolean = false,
    val removable: Boolean = true,
    /** The selected profile is the one the tunnel would use, so it is called out in the list. */
    val selected: Boolean = false,
)

/** Everything KX owns, measured at one moment. */
data class ShieldInventory(
    val entries: List<ShieldEntry> = emptyList(),
    val loadedAtMillis: Long = 0,
    val failed: Boolean = false,
) {
    val profiles: List<ShieldEntry> get() = entries.filter { it.kind == ShieldKind.PROFILE }
    val history: List<ShieldEntry> get() = entries.filter { it.kind != ShieldKind.PROFILE }
    val secretCount: Int get() = entries.count { it.holdsSecret }
    val oldestMillis: Long? get() = entries.minOfOrNull { it.atMillis }
}

enum class ShieldAction { REVOKE_PROFILE, ERASE_HISTORY, PRUNE_OLD }

/** What an approved action really did. Every number is counted, never estimated. */
data class ShieldReceipt(
    val action: ShieldAction,
    val removed: Int,
    val pruned: Int,
    val bytesFreed: Long,
    val atMillis: Long,
    val detail: String = "",
)

/**
 * The retention rule for the history KX keeps. It is a pure function so the matrix is covered
 * by JVM tests: history older than the window is pruned, and nothing else is touched.
 */
object ShieldRetention {
    const val DEFAULT_DAYS = 7
    const val DAY_MILLIS = 24L * 60 * 60 * 1000

    /** The oldest timestamp that survives the rule. */
    fun keepFrom(nowMillis: Long, days: Int = DEFAULT_DAYS): Long =
        nowMillis - days.coerceAtLeast(0) * DAY_MILLIS

    fun isExpired(atMillis: Long, nowMillis: Long, days: Int = DEFAULT_DAYS): Boolean =
        atMillis < keepFrom(nowMillis, days)

    /** Splits entries into the ones that survive and the ones that expired. */
    fun partition(
        entries: List<ShieldEntry>,
        nowMillis: Long,
        days: Int = DEFAULT_DAYS,
    ): Pair<List<ShieldEntry>, List<ShieldEntry>> {
        val keep = mutableListOf<ShieldEntry>()
        val drop = mutableListOf<ShieldEntry>()
        entries.forEach { entry ->
            if (entry.kind == ShieldKind.PROFILE) keep += entry else if (isExpired(entry.atMillis, nowMillis, days)) drop += entry else keep += entry
        }
        return keep to drop
    }
}
