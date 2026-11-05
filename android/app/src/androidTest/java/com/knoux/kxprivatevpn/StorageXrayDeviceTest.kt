package com.knoux.kxprivatevpn

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.platform.storage.MediaStoreInventory
import com.knoux.kxprivatevpn.platform.storage.StorageCategory
import com.knoux.kxprivatevpn.platform.storage.StoragePermissions
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * MediaStore aggregation runs only with the real platform permission. The test grants the
 * legacy read permission, proves categories are permitted on API 26, then revokes it so other
 * tests keep their starting state.
 */
class StorageXrayDeviceTest {
    @Test fun mediaInventoryRespectsGrantedPermissionAndVersionMatrix() {
        assumeTrue(Build.VERSION.SDK_INT <= 32)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val ui = InstrumentationRegistry.getInstrumentation().uiAutomation
        try {
            ui.grantRuntimePermission(context.packageName,
                Manifest.permission.READ_EXTERNAL_STORAGE)
        } catch (_: Exception) {
            // Fall through to the poll: the assertion reports the real permission state.
        }
        val deadline = System.currentTimeMillis() + 5_000
        while (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED && System.currentTimeMillis() < deadline) {
            Thread.sleep(100)
        }
        try {
            val inventory = runBlocking { MediaStoreInventory(context).scan() }
            assertEquals(4, inventory.categories.size)
            val images = inventory.categories.first { it.category == StorageCategory.IMAGES }
            assertTrue("images category must be permitted after pm grant", images.permitted)
            assertTrue("granted query must not be reported as failed", !images.failed)
            val documents = inventory.categories.first { it.category == StorageCategory.DOCUMENTS }
            assertEquals(StoragePermissions.documentsAvailable(Build.VERSION.SDK_INT),
                documents.permitted)
        } finally {
            try {
                ui.revokeRuntimePermission(context.packageName,
                    Manifest.permission.READ_EXTERNAL_STORAGE)
            } catch (_: Exception) {
                // Best effort restoration for the next test.
            }
        }
    }

    @Test fun withoutPermissionCategoriesAreNotReportedAsZero() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val ui = InstrumentationRegistry.getInstrumentation().uiAutomation
        val permissions = if (Build.VERSION.SDK_INT >= 34) listOf(
            Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        ) else if (Build.VERSION.SDK_INT >= 33) listOf(
            Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO,
        ) else listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        permissions.forEach {
            try { ui.revokeRuntimePermission(context.packageName, it) } catch (_: Exception) { }
        }
        val inventory = runBlocking { MediaStoreInventory(context).scan() }
        val media = inventory.categories.filter { it.category != StorageCategory.DOCUMENTS }
        media.forEach {
            assertFalse("${it.category} must not claim permission", it.permitted)
            assertEquals("${it.category} bytes must stay null", null, it.bytes)
        }
    }
}
