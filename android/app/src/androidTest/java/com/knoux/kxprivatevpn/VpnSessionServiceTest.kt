package com.knoux.kxprivatevpn

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.SystemClock
import androidx.annotation.RequiresApi
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.platform.vpn.VpnSessionService
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Proves the part the WireGuard library does not provide: a real foreground service that
 * posts an ongoing session notification and removes it when the session stops.
 *
 * MainActivity is launched first so the process has a visible window. Android 12+ blocks
 * background foreground-service starts, and that platform rule is not a product defect.
 *
 * The notification permission grant is deliberately left in place on API 33+, matching
 * the existing Storage X-Ray device test; revoking a runtime permission can kill the
 * instrumentation process.
 */
class VpnSessionServiceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun waitUntil(timeoutMs: Long, condition: () -> Boolean): Boolean {
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < deadline) {
            if (condition()) return true
            SystemClock.sleep(50)
        }
        return condition()
    }

    @Test fun ongoingNotificationExistsWhileForegroundAndIsRemovedAfterStop() {
        if (Build.VERSION.SDK_INT >= 33) {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        ActivityScenario.launch(MainActivity::class.java).use {
            try {
                VpnSessionService.start(context, "KX device test", System.currentTimeMillis())
                assertTrue(
                    "the foreground session notification was never posted",
                    waitUntil(5_000) {
                        manager.activeNotifications.any { it.id == VpnSessionService.NOTIFICATION_ID }
                    },
                )
                val posted = manager.activeNotifications
                    .first { it.id == VpnSessionService.NOTIFICATION_ID }.notification
                assertTrue(
                    "a live session notification must not be dismissible",
                    posted.flags and Notification.FLAG_ONGOING_EVENT != 0,
                )
                // Leave a text artifact behind for the device evidence bundle.
                val evidence = File(context.getExternalFilesDir(null), "session-evidence.txt")
                evidence.writeText(
                    buildString {
                        appendLine("notification_id=${VpnSessionService.NOTIFICATION_ID}")
                        appendLine("channel_id=${VpnSessionService.CHANNEL_ID}")
                        appendLine("ongoing=${posted.flags and Notification.FLAG_ONGOING_EVENT != 0}")
                        appendLine("only_alert_once=${posted.flags and Notification.FLAG_ONLY_ALERT_ONCE != 0}")
                        appendLine("visibility=${posted.visibility}")
                        appendLine("category=${posted.category}")
                        appendLine("content_title=${posted.extras.getCharSequence(Notification.EXTRA_TITLE)}")
                        appendLine("content_text=${posted.extras.getCharSequence(Notification.EXTRA_TEXT)}")
                        appendLine("posted_while_activity_visible=true")
                    },
                )
            } finally {
                context.stopService(Intent(context, VpnSessionService::class.java))
            }
        }
        assertTrue(
            "stopping the session must remove its notification",
            waitUntil(5_000) {
                manager.activeNotifications.none { it.id == VpnSessionService.NOTIFICATION_ID }
            },
        )
    }

    @Test fun manifestDeclaresForegroundTypeAndPermissions() {
        @Suppress("DEPRECATION")
        val info = context.packageManager.getServiceInfo(
            ComponentName(context, VpnSessionService::class.java),
            0,
        )
        if (Build.VERSION.SDK_INT >= 34) assertSpecialUseType(info)
        @Suppress("DEPRECATION")
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS,
        )
        val requested = packageInfo.requestedPermissions?.toSet().orEmpty()
        assertTrue(requested.contains(Manifest.permission.FOREGROUND_SERVICE))
        assertTrue(requested.contains(Manifest.permission.FOREGROUND_SERVICE_SPECIAL_USE))
        assertTrue(requested.contains(Manifest.permission.POST_NOTIFICATIONS))
    }

    @RequiresApi(34)
    private fun assertSpecialUseType(info: ServiceInfo) {
        assertTrue(
            "specialUse foreground service type is not declared",
            info.foregroundServiceType and ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE != 0,
        )
    }
}
