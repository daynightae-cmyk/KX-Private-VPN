package com.knoux.kxprivatevpn

import android.Manifest
import android.app.UiAutomation
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.content.ContextCompat
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.platform.storage.MediaStoreInventory
import com.knoux.kxprivatevpn.platform.storage.StorageCategory
import com.knoux.kxprivatevpn.platform.storage.StoragePermissions
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * MediaStore aggregation runs only with the real platform permission. The test grants the
 * legacy read permission and proves categories are permitted on API 26. It deliberately leaves
 * the grant in place: revoking it can kill the instrumentation process, and the denied path is
 * covered by injected screen states and the null invariant.
 */
class StorageXrayDeviceTest {
    @Test fun mediaInventoryRespectsGrantedPermissionAndVersionMatrix() {
        assumeTrue(Build.VERSION.SDK_INT <= 32)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val ui = InstrumentationRegistry.getInstrumentation().uiAutomation
        runShell(ui,
            "pm grant ${context.packageName} ${Manifest.permission.READ_EXTERNAL_STORAGE}")
        val deadline = System.currentTimeMillis() + 5_000
        while (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED && System.currentTimeMillis() < deadline) {
            Thread.sleep(100)
        }
        val inventory = runBlocking { MediaStoreInventory(context).scan() }
        assertEquals(4, inventory.categories.size)
        val images = inventory.categories.first { it.category == StorageCategory.IMAGES }
        assertTrue("images category must be permitted after pm grant", images.permitted)
        assertTrue("granted query must not be reported as failed", !images.failed)
        val documents = inventory.categories.first { it.category == StorageCategory.DOCUMENTS }
        assertEquals(StoragePermissions.documentsAvailable(Build.VERSION.SDK_INT),
            documents.permitted)
    }

    @Test fun everyUnpermittedCategoryStaysNull() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val inventory = runBlocking { MediaStoreInventory(context).scan() }
        inventory.categories.filter { !it.permitted }.forEach {
            assertEquals("${it.category} bytes must stay null", null, it.bytes)
        }
    }

    /** executeShellCommand returns immediately; reading the pipe to EOF waits for completion. */
    private fun runShell(ui: UiAutomation, command: String) {
        val descriptor = ui.executeShellCommand(command)
        ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
    }
}
