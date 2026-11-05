package com.knoux.kxprivatevpn

import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.platform.repair.AndroidRepairMeasurer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.UUID

/** The dashboard measurement is real StatFs plus a real KX cache walk, not a canned value. */
class RepairMeasurementDeviceTest {
    @Test fun measurementReadsVolumeAndOnlyTheKxCache() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefix = "repair_measure_" + UUID.randomUUID().toString().take(8)
        val folder = File(context.cacheDir, prefix).apply { mkdirs() }
        val file = File(folder, "fixture.tmp").apply { writeBytes(ByteArray(2048) { 1 }) }
        try {
            val value = runBlocking {
                AndroidRepairMeasurer(context.filesDir, context.cacheDir).measure()
            }
            assertTrue(value.totalBytes != null && value.totalBytes!! > 0)
            assertTrue(value.availableBytes != null && value.availableBytes!! in 0..value.totalBytes!!)
            assertTrue("cache bytes must include the fixture", value.cacheBytes != null &&
                value.cacheBytes!! >= 2048)
            assertTrue(value.cacheFileCount >= 1)
            val preview = value.preview
            assertTrue(preview != null)
            assertTrue("preview must list the real fixture",
                preview!!.files.any { it.path.endsWith("$prefix/fixture.tmp") })
            assertEquals(2048L, preview.files.first { it.path.endsWith("fixture.tmp") }.bytes)
            assertTrue(value.atMillis > 0)
        } finally {
            file.delete()
            folder.delete()
        }
    }
}
