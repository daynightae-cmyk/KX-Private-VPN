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
    /**
     * Structured journal fields. They exist so the UI can translate a stored operation instead
     * of printing the storage keys. They are null for profiles and the session hint.
     */
    val serviceKey: String? = null,
    val actionKey: String? = null,
    val outcomeKey: String? = null,
    val items: Int = 0,
    val bytes: Long = 0,
    /** A measured usage duration; never an estimate. */
    val durationMillis: Long = 0,
    val recovered: Boolean = false,
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
    val oldestMillis: Long? get() = entries.mapNotNull { it.atMillis.takeIf { time -> time > 0 } }.minOrNull()

    /** Only profiles can be revoked in bulk; history has its own explicit erase action. */
    val removableProfileIds: Set<String>
        get() = profiles.filter { it.removable }.map { it.id }.toSet()

    /**
     * Resolves the selected ids against the profiles that actually exist right now. A stale id
     * (a profile removed in another screen) is ignored instead of revoking the wrong thing.
     */
    fun selectedProfileIds(selected: Set<String>): List<String> =
        profiles.filter { it.removable && it.id in selected }.map { it.id.substringAfter(':') }
}

enum class ShieldAction { REVOKE_PROFILE, REVOKE_SELECTED, ERASE_HISTORY, PRUNE_OLD }

/**
 * The outcome of an approved action. The stable [key] is what the app stores and what tests
 * pin; the screen translates it and never renders the key itself.
 */
enum class ShieldReceiptCode(val key: String) {
    SECRET_REMOVED("secret_removed"),
    SECRET_STILL_PRESENT("secret_still_present"),
    SECRET_UNVERIFIED("secret_unverified"),
    SECRETS_REMOVED("secrets_removed"),
    SECRETS_PARTIAL("secrets_partial"),
    SECRETS_NONE_REMOVED("secrets_none_removed"),
    SECRETS_UNVERIFIED("secrets_unverified"),
    ERASED("erased"),
    ERASE_INCOMPLETE("erase_incomplete"),
    PRUNED("pruned"),
    PRUNE_INCOMPLETE("prune_incomplete"),
}

/** What an approved action really did. Every number is counted, never estimated. */
data class ShieldReceipt(
    val action: ShieldAction,
    val removed: Int,
    val pruned: Int,
    val bytesFreed: Long,
    val atMillis: Long,
    val code: ShieldReceiptCode,
)

/**
 * A single revoke may only claim success after the encrypted store was read back. If the read
 * itself failed, the receipt says "unverified" rather than guessing either way.
 */
object ShieldRevoke {
    fun code(verified: Boolean, stillPresent: Boolean): ShieldReceiptCode = when {
        !verified -> ShieldReceiptCode.SECRET_UNVERIFIED
        stillPresent -> ShieldReceiptCode.SECRET_STILL_PRESENT
        else -> ShieldReceiptCode.SECRET_REMOVED
    }
}

/** The bulk receipt derives its code from the number of targets and the verified removals. */
object ShieldBulkRevoke {
    fun code(targets: Int, removed: Int, verified: Boolean): ShieldReceiptCode = when {
        !verified -> ShieldReceiptCode.SECRETS_UNVERIFIED
        targets <= 0 || removed <= 0 -> ShieldReceiptCode.SECRETS_NONE_REMOVED
        removed >= targets -> ShieldReceiptCode.SECRETS_REMOVED
        else -> ShieldReceiptCode.SECRETS_PARTIAL
    }
}

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
