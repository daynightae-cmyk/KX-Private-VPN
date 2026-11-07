package com.knoux.kxprivatevpn

import com.knoux.kxprivatevpn.platform.security.ShieldEntry
import com.knoux.kxprivatevpn.platform.security.ShieldInventory
import com.knoux.kxprivatevpn.platform.security.ShieldKind
import com.knoux.kxprivatevpn.platform.security.ShieldRetention
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
}
