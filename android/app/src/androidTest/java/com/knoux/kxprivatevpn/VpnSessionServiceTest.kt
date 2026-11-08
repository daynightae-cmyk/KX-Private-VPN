package com.knoux.kxprivatevpn

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kxprivatevpn.platform.vpn.VpnSessionControl
import com.knoux.kxprivatevpn.platform.vpn.VpnSessionService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

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

    /**
     * Waits for the session notification in the expected language, not merely for an ID.
     * A previous test's notification may still be animating out; matching the title keeps
     * each test bound to the service instance it just started.
     */
    private fun awaitSessionNotification(
        manager: NotificationManager,
        title: String,
    ): Notification {
        assertTrue(
            "the session notification was never posted with title '$title'",
            waitUntil(5_000) {
                manager.activeNotifications.firstOrNull {
                    it.id == VpnSessionService.NOTIFICATION_ID
                }?.notification?.extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString() == title
            },
        )
        return manager.activeNotifications
            .first { it.id == VpnSessionService.NOTIFICATION_ID }.notification
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
                val posted = awaitSessionNotification(
                    manager,
                    context.getString(R.string.vpn_notification_title),
                )
                assertTrue(
                    "a live session notification must not be dismissible",
                    posted.flags and Notification.FLAG_ONGOING_EVENT != 0,
                )
                val disconnect = posted.actions?.singleOrNull()
                assertNotNull("the session notification must offer one disconnect action", disconnect)
                assertEquals(
                    context.getString(R.string.vpn_notification_disconnect),
                    disconnect!!.title.toString(),
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
                        appendLine("action_count=${posted.actions?.size ?: 0}")
                        appendLine("action_title=${disconnect!!.title}")
                        appendLine("posted_while_activity_visible=true")
                    },
                )
                captureShade(
                    "session-notification-en",
                    context.getString(R.string.vpn_notification_title),
                    context.getString(R.string.vpn_notification_disconnect),
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

    @Test fun disconnectActionStopsTheSessionAndRemovesTheNotification() {
        grantNotificationPermission()
        val manager = context.getSystemService(NotificationManager::class.java)
        val requests = AtomicInteger()
        val handler: () -> Boolean = { requests.incrementAndGet() > 0 }
        ActivityScenario.launch(MainActivity::class.java).use {
            VpnSessionControl.register(handler)
            try {
                VpnSessionService.start(context, "KX device test", System.currentTimeMillis(), "en")
                val posted = awaitSessionNotification(
                    manager,
                    context.getString(R.string.vpn_notification_title),
                )
                val action = posted.actions?.singleOrNull()
                assertNotNull("the session notification has no disconnect action", action)
                action!!.actionIntent.send()
                assertTrue(
                    "the notification action never reached the live session controller",
                    waitUntil(5_000) { requests.get() == 1 },
                )
                assertTrue(
                    "a tapped disconnect must remove the session notification",
                    waitUntil(10_000) {
                        manager.activeNotifications.none { it.id == VpnSessionService.NOTIFICATION_ID }
                    },
                )
            } finally {
                VpnSessionControl.unregister(handler)
                context.stopService(Intent(context, VpnSessionService::class.java))
            }
        }
    }

    @Test fun sessionNotificationUsesTheAppChosenLanguage() {
        grantNotificationPermission()
        val manager = context.getSystemService(NotificationManager::class.java)
        val arabic = localized("ar")
        ActivityScenario.launch(MainActivity::class.java).use {
            try {
                VpnSessionService.start(context, "KX device test", System.currentTimeMillis(), "ar")
                val posted = awaitSessionNotification(
                    manager,
                    arabic.getString(R.string.vpn_notification_title),
                )
                assertEquals(
                    arabic.getString(R.string.vpn_notification_title),
                    posted.extras.getCharSequence(Notification.EXTRA_TITLE).toString(),
                )
                assertEquals(
                    arabic.getString(R.string.vpn_notification_disconnect),
                    posted.actions?.singleOrNull()?.title?.toString(),
                )
                assertEquals(
                    arabic.getString(R.string.vpn_notification_channel),
                    manager.getNotificationChannel(VpnSessionService.CHANNEL_ID).name.toString(),
                )
                captureShade(
                    "session-notification-ar",
                    arabic.getString(R.string.vpn_notification_title),
                    arabic.getString(R.string.vpn_notification_disconnect),
                )
            } finally {
                context.stopService(Intent(context, VpnSessionService::class.java))
            }
        }
    }

    @Test fun disconnectActionWithoutALiveControllerStillRemovesTheSurface() {
        grantNotificationPermission()
        val manager = context.getSystemService(NotificationManager::class.java)
        val requests = AtomicInteger()
        val idle: () -> Boolean = { requests.incrementAndGet(); false }
        ActivityScenario.launch(MainActivity::class.java).use {
            VpnSessionControl.register(idle)
            try {
                VpnSessionService.start(context, "KX device test", System.currentTimeMillis(), "en")
                val posted = awaitSessionNotification(
                    manager,
                    context.getString(R.string.vpn_notification_title),
                )
                val action = posted.actions?.singleOrNull()
                assertNotNull("the session notification has no disconnect action", action)
                action!!.actionIntent.send()
                assertTrue(
                    "the action did not consult the session controller",
                    waitUntil(5_000) { requests.get() == 1 },
                )
                assertTrue(
                    "a refused controller request must still stop the surface",
                    waitUntil(10_000) {
                        manager.activeNotifications.none { it.id == VpnSessionService.NOTIFICATION_ID }
                    },
                )
            } finally {
                VpnSessionControl.unregister(idle)
                context.stopService(Intent(context, VpnSessionService::class.java))
            }
        }
    }

    @Test fun repostingTheSessionUpdatesTheLanguageWithoutDroppingIt() {
        grantNotificationPermission()
        val manager = context.getSystemService(NotificationManager::class.java)
        val arabic = localized("ar")
        val startedAt = System.currentTimeMillis()
        ActivityScenario.launch(MainActivity::class.java).use {
            try {
                VpnSessionService.start(context, "KX device test", startedAt, "en")
                awaitSessionNotification(manager, context.getString(R.string.vpn_notification_title))
                VpnSessionService.start(context, "KX device test", startedAt, "ar")
                assertTrue(
                    "reposting did not update the notification language",
                    waitUntil(5_000) {
                        manager.activeNotifications.firstOrNull {
                            it.id == VpnSessionService.NOTIFICATION_ID
                        }?.notification?.extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString() ==
                            arabic.getString(R.string.vpn_notification_title)
                    },
                )
                val updated = manager.activeNotifications
                    .first { it.id == VpnSessionService.NOTIFICATION_ID }.notification
                assertTrue(
                    "the reposted session must stay ongoing",
                    updated.flags and Notification.FLAG_ONGOING_EVENT != 0,
                )
                assertEquals(
                    arabic.getString(R.string.vpn_notification_disconnect),
                    updated.actions?.singleOrNull()?.title?.toString(),
                )
            } finally {
                context.stopService(Intent(context, VpnSessionService::class.java))
            }
        }
    }

    private fun grantNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun localized(language: String): android.content.Context {
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(language))
        return context.createConfigurationContext(configuration)
    }

    /**
     * Captures the real notification shade, not whatever window happens to be focused.
     * Expanding an already-open shade toggles it closed on API26, so every transition is
     * verified against the window manager before the screenshot is taken. The notification
     * is located through the accessibility tree and expanded so its action row is visible.
     */
    private fun captureShade(name: String, title: String, action: String) {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        setShade(open = false)
        check(setShade(open = true)) { "the notification shade did not open" }
        try {
            var bounds = waitForText(title)
            if (bounds == null) {
                // API 13+ can place a low-importance ongoing notification in the collapsed
                // "Silent notifications" section. Expand it before declaring the test failed.
                expandSilentSection()
                bounds = waitForText(title)
            }
            check(bounds != null) { "the session notification was not visible in the shade" }
            val x = bounds.centerX()
            val y = bounds.centerY()
            // A one-finger swipe down inside the notification expands its action row.
            shell("input swipe $x $y $x ${y + 360} 300")
            SystemClock.sleep(900)
            if (!waitUntil(3_000) { textBounds(action) != null }) {
                // Some API levels keep the expand affordance in the notification header.
                expandAffordance()?.let { (cx, cy) ->
                    shell("input tap $cx $cy")
                    SystemClock.sleep(1_000)
                }
            }
            check(waitUntil(3_000) { textBounds(action) != null }) {
                "the session notification's $action action is not reachable in the shade"
            }
            val bitmap = automation.takeScreenshot()
            val file = File(context.getExternalFilesDir(null), "$name.png")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } finally {
            setShade(open = false)
        }
    }

    private fun setShade(open: Boolean): Boolean {
        val command = if (open) "cmd statusbar expand-notifications" else "cmd statusbar collapse"
        repeat(3) {
            shell(command)
            if (waitUntil(3_000) { shadeIsOpen() == open }) return true
            SystemClock.sleep(250)
        }
        return shadeIsOpen() == open
    }

    private fun shadeIsOpen(): Boolean = shell("dumpsys window windows")
        .lineSequence()
        .firstOrNull { it.contains("mCurrentFocus=") }
        ?.let { it.contains("StatusBar") || it.contains("NotificationShade") } == true

    private fun waitForText(text: String, timeoutMs: Long = 8_000): Rect? {
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < deadline) {
            textBounds(text)?.let { return it }
            SystemClock.sleep(200)
        }
        return textBounds(text)
    }

    private fun textBounds(text: String): Rect? = nodeBounds { node ->
        node.text?.toString()?.equals(text, ignoreCase = true) == true ||
            node.contentDescription?.toString()?.equals(text, ignoreCase = true) == true
    }

    private fun expandSilentSection() {
        val bounds = nodeBounds { node ->
            val text = node.text?.toString().orEmpty()
            text.contains("Silent", ignoreCase = true) || text.contains("صامت")
        } ?: return
        shell("input tap ${bounds.centerX()} ${bounds.centerY()}")
        SystemClock.sleep(800)
    }

    private fun expandAffordance(): Pair<Int, Int>? {
        val bounds = nodeBounds { node ->
            val description = node.contentDescription?.toString()
            description.equals("Expand", ignoreCase = true) ||
                description.equals("توسيع", ignoreCase = true)
        } ?: return null
        return bounds.centerX() to bounds.centerY()
    }

    private fun nodeBounds(predicate: (AccessibilityNodeInfo) -> Boolean): Rect? {
        val root = InstrumentationRegistry.getInstrumentation().uiAutomation
            .rootInActiveWindow ?: return null
        return try {
            findNode(root, predicate)
        } finally {
            @Suppress("DEPRECATION")
            root.recycle()
        }
    }

    private fun findNode(
        node: AccessibilityNodeInfo,
        predicate: (AccessibilityNodeInfo) -> Boolean,
    ): Rect? {
        if (predicate(node)) {
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            return bounds
        }
        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            try {
                findNode(child, predicate)?.let { return it }
            } finally {
                @Suppress("DEPRECATION")
                child.recycle()
            }
        }
        return null
    }

    private fun shell(command: String): String {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        return ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command))
            .bufferedReader()
            .use { it.readText() }
    }

    @RequiresApi(34)
    private fun assertSpecialUseType(info: ServiceInfo) {
        assertTrue(
            "specialUse foreground service type is not declared",
            info.foregroundServiceType and ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE != 0,
        )
    }
}
