package com.knoux.kxprivatevpn

import com.knoux.kxprivatevpn.features.ToolFilter
import com.knoux.kxprivatevpn.features.ToolsCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Tools hub is metadata over real destinations. This contract fails if a card appears twice,
 * points at a screen that does not exist, or claims a filter it cannot actually satisfy.
 */
class ToolsCatalogTest {
    @Test fun nineOwnerApprovedCardsInReadingOrder() {
        assertEquals(
            listOf(
                "temporary", "duplicates", "large", "download",
                "smart", "media", "network", "apps", "storage-health",
            ),
            ToolsCatalog.entries.map { it.key },
        )
        assertEquals(9, ToolsCatalog.entries.size)
        assertEquals(8, ToolsCatalog.gridEntries.size)
        assertEquals(listOf("storage-health"), ToolsCatalog.fullWidthEntries.map { it.key })
    }

    @Test fun everyRouteIsAnImplementedDestination() {
        val routes = ToolsCatalog.entries.map { it.route }
        assertEquals("Each tool needs exactly one destination", routes.size, routes.distinct().size)
        assertEquals(
            setOf(
                "cache", "duplicate", "heavyweight", "download", "autopilot",
                "media", "network", "apps", "storage",
            ),
            routes.toSet(),
        )
        routes.forEach { route ->
            assertTrue("A tool route must be an explicit destination: $route", route.isNotBlank())
            assertEquals("Routes must not hide whitespace", route.trim(), route)
            assertFalse("The catalog is not a repair placeholder page: $route", route.startsWith("repair/"))
        }
    }

    @Test fun fullWidthStorageHealthIsLastAndNotPartOfTheGrid() {
        val entries = ToolsCatalog.entries
        assertEquals(9, entries.lastIndex + 1)
        assertTrue(entries.last().fullWidth)
        assertEquals("storage-health", entries.last().key)
        assertTrue(ToolsCatalog.gridEntries.none { it.fullWidth })
    }

    @Test fun everyCardHasDistinctPositiveCopyAndAnAccent() {
        assertEquals(
            "Titles must identify individual tools",
            ToolsCatalog.entries.size,
            ToolsCatalog.entries.map { it.title }.distinct().size,
        )
        assertEquals(
            "Descriptions must explain individual tools",
            ToolsCatalog.entries.size,
            ToolsCatalog.entries.map { it.body }.distinct().size,
        )
        ToolsCatalog.entries.forEach { entry ->
            assertTrue("${entry.key} needs a title resource", entry.title > 0)
            assertTrue("${entry.key} needs a description resource", entry.body > 0)
            assertTrue("${entry.key} needs at least one filter", entry.filters.isNotEmpty())
            assertTrue("${entry.key} must not be an empty card", entry.accentNight != entry.accentDay)
        }
    }

    @Test fun smartRepairIsTheOnlyCrossCategoryTool() {
        val smart = ToolsCatalog.entries.single { it.key == "smart" }
        assertEquals(ToolFilter.entries.toSet() - ToolFilter.ALL, smart.filters)
        ToolsCatalog.entries.filter { it.key != "smart" }.forEach { entry ->
            assertTrue("${entry.key} must not pretend to be a cross-category repair", entry.filters.size <= 2)
        }
    }

    @Test fun filtersSelectExactlyTheirDeclaredTools() {
        val counts = mapOf(
            ToolFilter.ALL to 9,
            ToolFilter.STORAGE to 5,
            ToolFilter.FILES to 4,
            ToolFilter.MEDIA to 2,
            ToolFilter.NETWORK to 2,
        )
        counts.forEach { (filter, expected) ->
            val matched = ToolsCatalog.entries.filter {
                ToolsCatalog.matches(it, filter, "", "title", "body")
            }
            assertEquals("$filter selects the declared tools", expected, matched.size)
        }
        assertEquals(
            setOf("temporary", "large", "smart", "apps", "storage-health"),
            keys(ToolFilter.STORAGE),
        )
        assertEquals(setOf("duplicates", "download", "smart", "apps"), keys(ToolFilter.FILES))
        assertEquals(setOf("smart", "media"), keys(ToolFilter.MEDIA))
        assertEquals(setOf("smart", "network"), keys(ToolFilter.NETWORK))
    }

    @Test fun searchTrimsWhitespaceAndIgnoresCaseInTitleAndBody() {
        val duplicate = ToolsCatalog.entries.single { it.key == "duplicates" }
        assertTrue(ToolsCatalog.matches(duplicate, ToolFilter.ALL, "  DUPLICATE  ", "Duplicate files", "Find copies"))
        assertTrue(ToolsCatalog.matches(duplicate, ToolFilter.ALL, "copies", "Duplicate files", "Find copies"))
        assertFalse(ToolsCatalog.matches(duplicate, ToolFilter.ALL, "network", "Duplicate files", "Find copies"))
        assertTrue("An empty query keeps every tool", ToolsCatalog.matches(duplicate, ToolFilter.ALL, "   ", "", ""))
        assertFalse(
            "A query never overrides a failing filter",
            ToolsCatalog.matches(duplicate, ToolFilter.NETWORK, "duplicate", "Duplicate files", "Find copies"),
        )
    }

    private fun keys(filter: ToolFilter) = ToolsCatalog.entries
        .filter { ToolsCatalog.matches(it, filter, "", "title", "body") }
        .map { it.key }
        .toSet()
}
