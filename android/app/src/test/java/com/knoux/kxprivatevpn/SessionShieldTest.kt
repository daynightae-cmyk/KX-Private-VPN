package com.knoux.kxprivatevpn

import com.knoux.kxprivatevpn.platform.security.ShieldBulkRevoke
import com.knoux.kxprivatevpn.platform.security.ShieldEntry
import com.knoux.kxprivatevpn.platform.security.ShieldInventory
import com.knoux.kxprivatevpn.platform.security.ShieldKind
import com.knoux.kxprivatevpn.platform.security.ShieldReceiptCode
import com.knoux.kxprivatevpn.platform.security.ShieldRetention
import com.knoux.kxprivatevpn.platform.security.ShieldRevoke
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The session shield keeps KX's own history and prunes it by age. These tests pin the rule:
 * history falls out of the window, a profile never does, and nothing outside KX is ever listed.
 */
class SessionShieldTest {

    private val day = ShieldRetention.DAY_MILLIS

    private fun history(id: String, atMillis: Long) = ShieldEntry(
        id = id,
        kind = ShieldKind.REPAIR_HISTORY,
        title = id,
        detail = "",
        atMillis = atMillis,
    )

    private fun usage(id: String, atMillis: Long) = ShieldEntry(
        id = id,
        kind = ShieldKind.USAGE_HISTORY,
        title = id,
        detail = "",
        atMillis = atMillis,
    )

    private fun profile(id: String) = ShieldEntry(
        id = id,
        kind = ShieldKind.PROFILE,
        title = id,
        detail = "endpoint",
        atMillis = 0,
        holdsSecret = true,
    )

    @Test fun historyOlderThanTheWindowExpires() {
        val now = 1_760_000_000_000L
        assertTrue(ShieldRetention.isExpired(now - 8 * day, now, 7))
        assertFalse(ShieldRetention.isExpired(now - 6 * day, now, 7))
        assertFalse(ShieldRetention.isExpired(now, now, 7))
    }

    @Test fun aProfileIsNeverPrunedNoMatterHowOldItIs() {
        val now = 1_760_000_000_000L
        val entries = listOf(profile("profile:a"), history("repair:old", now - 40 * day))
        val (keep, drop) = ShieldRetention.partition(entries, now, 7)
        assertEquals(listOf("profile:a"), keep.map { it.id })
        assertEquals(listOf("repair:old"), drop.map { it.id })
    }

    @Test fun thePartitionKeepsWhatIsInsideTheWindowAndDropsWhatIsNot() {
        val now = 1_760_000_000_000L
        val entries = listOf(
            history("repair:new", now - day),
            usage("usage:old", now - 30 * day),
            usage("usage:edge", now - 7 * day),
        )
        val (keep, drop) = ShieldRetention.partition(entries, now, 7)
        assertEquals(setOf("repair:new", "usage:edge"), keep.map { it.id }.toSet())
        assertEquals(listOf("usage:old"), drop.map { it.id })
    }

    @Test fun aZeroDayWindowKeepsOnlyWhatIsExactlyNow() {
        val now = 1_760_000_000_000L
        assertTrue(ShieldRetention.isExpired(now - 1, now, 0))
        assertFalse(ShieldRetention.isExpired(now, now, 0))
    }

    @Test fun theInventoryCountsWhatKXOwns() {
        val inventory = ShieldInventory(
            entries = listOf(
                profile("profile:a"),
                profile("profile:b"),
                history("repair:1", 1L),
                usage("usage:1", 2L),
            ),
        )
        assertEquals(2, inventory.profiles.size)
        assertEquals(2, inventory.history.size)
        assertEquals(2, inventory.secretCount)
    }

    @Test fun anEmptyInventoryIsAnEmptyResultAndNotAFailure() {
        val inventory = ShieldInventory()
        assertEquals(0, inventory.profiles.size)
        assertEquals(0, inventory.history.size)
        assertEquals(0, inventory.secretCount)
        assertEquals(null, inventory.oldestMillis)
        assertFalse(inventory.failed)
    }

    @Test fun bulkSelectionAcceptsOnlyRemovableProfiles() {
        val inventory = ShieldInventory(
            entries = listOf(
                profile("profile:a"),
                history("repair:1", 1L),
                usage("usage:1", 2L),
                ShieldEntry("hint:vpn-active", ShieldKind.SESSION_HINT, "hint", "detail", 0L),
            ),
        )
        assertEquals(setOf("profile:a"), inventory.removableProfileIds)
        assertEquals(
            listOf("a"),
            inventory.selectedProfileIds(setOf("profile:a", "repair:1", "hint:vpn-active")),
        )
    }

    @Test fun oldestHistoryIgnoresProfilesWithoutATimestamp() {
        val inventory = ShieldInventory(
            entries = listOf(
                profile("profile:a"),
                history("repair:new", 5L),
                usage("usage:old", 2L),
            ),
        )
        assertEquals(2L, inventory.oldestMillis)
    }

    @Test fun aSingleRevokeNeverClaimsSuccessWithoutAReadBack() {
        assertEquals(ShieldReceiptCode.SECRET_UNVERIFIED, ShieldRevoke.code(verified = false, stillPresent = false))
        assertEquals(ShieldReceiptCode.SECRET_STILL_PRESENT, ShieldRevoke.code(verified = true, stillPresent = true))
        assertEquals(ShieldReceiptCode.SECRET_REMOVED, ShieldRevoke.code(verified = true, stillPresent = false))
    }

    @Test fun aBulkReceiptReportsTheVerifiedCountAndNothingMore() {
        assertEquals(ShieldReceiptCode.SECRETS_REMOVED, ShieldBulkRevoke.code(2, 2, verified = true))
        assertEquals(ShieldReceiptCode.SECRETS_PARTIAL, ShieldBulkRevoke.code(2, 1, verified = true))
        assertEquals(ShieldReceiptCode.SECRETS_NONE_REMOVED, ShieldBulkRevoke.code(2, 0, verified = true))
        assertEquals(ShieldReceiptCode.SECRETS_UNVERIFIED, ShieldBulkRevoke.code(2, 0, verified = false))
        assertEquals(ShieldReceiptCode.SECRETS_NONE_REMOVED, ShieldBulkRevoke.code(0, 0, verified = true))
    }

    @Test fun receiptCodesKeepStableStorageKeys() {
        assertEquals("secret_removed", ShieldReceiptCode.SECRET_REMOVED.key)
        assertEquals("secrets_partial", ShieldReceiptCode.SECRETS_PARTIAL.key)
        assertEquals("prune_incomplete", ShieldReceiptCode.PRUNE_INCOMPLETE.key)
    }
}
