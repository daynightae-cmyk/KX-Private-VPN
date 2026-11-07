package com.knoux.kxprivatevpn

import com.knoux.kxprivatevpn.platform.repair.RepairCatalog
import com.knoux.kxprivatevpn.platform.repair.RepairServiceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Availability is build truth. This test fails if a service is declared ready without a real
 * route, or if a new tool claims availability before its implementation slice lands.
 */
class RepairCatalogTest {
    @Test fun exactlyTenSignatureServicesInOwnerOrder() {
        assertEquals(10, RepairCatalog.services.size)
        assertEquals(RepairServiceId.entries.toList(), RepairCatalog.services.map { it.id })
    }

    @Test fun keysAndRoutesAreUnique() {
        assertEquals(10, RepairCatalog.services.map { it.id.key }.toSet().size)
        assertEquals(10, RepairCatalog.services.map { it.id.route }.toSet().size)
    }

    @Test fun onlyImplementedToolsAreAvailable() {
        val ready = RepairCatalog.services.filter { it.id.available }.map { it.id }.toSet()
        assertEquals(
            setOf(
                RepairServiceId.STORAGE, RepairServiceId.GHOST,
                RepairServiceId.DUPLICATE, RepairServiceId.HEAVYWEIGHT,
                RepairServiceId.CACHE, RepairServiceId.MEDIA, RepairServiceId.DOWNLOAD,
                RepairServiceId.SESSION, RepairServiceId.NETWORK,
            ),
            ready,
        )
        assertEquals(9, RepairCatalog.available())
    }

    @Test fun unavailableToolsHaveDedicatedHonestRoutes() {
        RepairCatalog.services.filter { !it.id.available }.forEach {
            assertTrue("${it.id} must have a repair detail route", it.id.route.startsWith("repair/"))
            assertNotNull(RepairCatalog.byRoute(it.id.route))
        }
    }

    @Test fun implementedToolsRouteToRealScreens() {
        RepairCatalog.services.filter { it.id.available }.forEach {
            assertTrue("${it.id} must not use the unavailable route", !it.id.route.startsWith("repair/"))
        }
    }

    @Test fun lookupByKeyAndRouteIsConsistent() {
        RepairCatalog.services.forEach {
            assertEquals(it, RepairCatalog.service(it.id.key))
            assertEquals(it, RepairCatalog.byRoute(it.id.route))
        }
        assertEquals(null, RepairCatalog.service("unknown"))
        assertEquals(null, RepairCatalog.byRoute("repair/unknown"))
    }
}
