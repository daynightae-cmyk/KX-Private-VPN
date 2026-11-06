package com.knoux.kxprivatevpn

import android.content.Intent
import android.provider.Settings
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.features.cacheSystemSettingsIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cache must never pretend to clean other apps. The only supported route is Android's own
 * app list, which this test proves is resolvable and task-safe.
 */
class CacheSettingsIntentTest {
    @Test fun officialAppListIntentIsResolvableAndNewTask() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = cacheSystemSettingsIntent()
        assertEquals(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS, intent.action)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertNotNull(
            "Android Settings app list must resolve on the target device",
            intent.resolveActivity(context.packageManager),
        )
    }
}
