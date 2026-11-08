package com.knoux.kxprivatevpn

import com.knoux.kxprivatevpn.features.HomeServiceCatalog
import com.knoux.kxprivatevpn.features.HomeServiceSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure discovery contract: metadata does not imply that scans or VPN operations ran. */
class HomeServiceCatalogTest {
    @Test fun routesAreExactlyTheImplementedHomeDestinations() {
        assertEquals(
            setOf("storage", "cache", "apps", "network", "security", "stats", "import", "split", "advanced"),
            HomeServiceCatalog.entries.map { it.route }.toSet(),
        )
        assertEquals(9, HomeServiceCatalog.entries.size)
    }

    @Test fun routesAreUniqueAndNeverUseRepairFallbacks() {
        val routes = HomeServiceCatalog.entries.map { it.route }
        assertEquals("Each destination needs exactly one discovery card", routes.size, routes.distinct().size)
        routes.forEach { route ->
            assertTrue("A route must be an explicit nonblank destination: $route", route.isNotBlank())
            assertEquals("Routes must not hide whitespace", route.trim(), route)
            assertTrue("Home discovery must not manufacture repair/<id> routes", !route.startsWith("repair/"))
        }
    }

    @Test fun everyNamedSectionContainsServices() {
        assertEquals(
            setOf(R.string.home_section_device, R.string.home_section_diagnostics, R.string.home_section_vpn),
            HomeServiceSection.entries.map { it.title }.toSet(),
        )
        assertEquals(3, HomeServiceSection.entries.size)
        HomeServiceSection.entries.forEach { section ->
            assertTrue("$section needs a valid section title", section.title > 0)
            assertTrue("$section must not be an empty discovery heading",
                HomeServiceCatalog.entries.any { it.section == section })
        }
    }

    @Test fun eachCardHasSeparatePositiveTitleDescriptionCapabilityAndActionResources() {
        HomeServiceCatalog.entries.forEach { entry ->
            val metadata = listOf(entry.title, entry.description, entry.capability, entry.action)
            assertTrue("${entry.route} needs four valid string resources", metadata.all { it > 0 })
            assertEquals("${entry.route} must explain scope and action instead of repeating its title",
                4, metadata.distinct().size)
            assertTrue("${entry.route} must not use its section heading as card metadata",
                entry.section.title !in metadata)
        }
    }

    @Test fun serviceTitlesDescriptionsAndActionsAreSpecializedPerDestination() {
        val entries = HomeServiceCatalog.entries
        assertEquals("Titles must identify individual services", entries.size, entries.map { it.title }.distinct().size)
        assertEquals("Descriptions must explain each service", entries.size, entries.map { it.description }.distinct().size)
        assertEquals("Actions must identify the service journey", entries.size, entries.map { it.action }.distinct().size)
        // Shared scope resources are intentional (e.g. read-only diagnostics and profile-required VPN tools).
        assertTrue("Not every service has the same capability boundary", entries.map { it.capability }.distinct().size > 1)
    }
}
