package com.knoux.kxprivatevpn

import android.Manifest
import android.content.pm.PackageManager
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.features.safeUninstallIntent
import com.knoux.kxprivatevpn.platform.apps.InstalledApp
import com.knoux.kxprivatevpn.platform.apps.InstalledApps
import com.knoux.kxprivatevpn.platform.apps.isProtectedSystemApp
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/** Read-only PackageManager checks. Never starts an activity, installs, clears or removes apps. */
class InstalledAppsInventoryTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun sharedQueryKeepsSplitTunnelScopeAndReportsObservedMetadata() = runBlocking {
        val engine = InstalledApps(context)
        val inventory = engine.inventory()
        val splitChoices = engine.list()
        assertTrue("The app declares a visible launcher entry", inventory.any { it.packageName == context.packageName })
        assertFalse(splitChoices.any { it.packageName == context.packageName })
        assertEquals(inventory.size, inventory.distinctBy { it.packageName }.size)
        assertEquals(inventory.filterNot { it.packageName == context.packageName }.map { it.packageName }.toSet(),
            splitChoices.map { it.packageName }.toSet())
        inventory.forEach { app ->
            assertTrue(app.label.isNotBlank())
            @Suppress("DEPRECATION")
            val observed = context.packageManager.getApplicationInfo(app.packageName, 0)
            assertEquals(isProtectedSystemApp(observed.flags), app.system)
            assertEquals(observed.targetSdkVersion, app.targetSdk)
        }
    }

    @Test fun manifestRequestsConfirmationOnlyAndDoesNotBroadenPackageVisibility() {
        @Suppress("DEPRECATION")
        val permissions = context.packageManager.getPackageInfo(context.packageName,
            PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty().toSet()
        assertTrue(Manifest.permission.REQUEST_DELETE_PACKAGES in permissions)
        assertFalse("android.permission.DELETE_PACKAGES" in permissions)
        assertFalse("android.permission.QUERY_ALL_PACKAGES" in permissions)
    }

    @Test fun executionBoundaryRejectsSelfProtectedMissingAndStaleSystemTargets() = runBlocking {
        assertNull(safeUninstallIntent(context, InstalledApp(context.packageName, "Self")))
        assertNull(safeUninstallIntent(context, InstalledApp("org.example.kx.absent.fixture", "Missing")))
        assertNull(safeUninstallIntent(context, InstalledApp("org.example.fixture", "Protected", system = true)))
        // Pretend a real visible system app was classified as user-owned during an older scan.
        // The execution boundary must re-read flags, not trust that stale approval metadata.
        val systemApps = InstalledApps(context).inventory().filter { it.system }
        assertTrue("API26/35 emulator has a visible system launcher app", systemApps.isNotEmpty())
        systemApps.forEach { app ->
            assertNull(safeUninstallIntent(context, app.copy(system = false)))
        }
    }
}
