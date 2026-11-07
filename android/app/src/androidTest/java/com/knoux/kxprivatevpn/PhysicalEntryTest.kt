package com.knoux.kxprivatevpn

import android.graphics.Bitmap
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.UUID

/**
 * Opt-in real Activity/real-time entry evidence, without a Compose test clock or store setup.
 * Does not connect VPN, change preferences, or delete anything. Normal app startup may recover
 * its own lifecycle state, so the host must preserve/compare app-private state around this test.
 * Captures remain in the TEST APK's unique directory and require review before publication.
 */
class PhysicalEntryTest {
    @Test fun actualLauncherWaitsForOrbAndRestoresHome() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        org.junit.Assume.assumeTrue("physical entry capture is explicitly opt-in",
            InstrumentationRegistry.getArguments().getString("kxPhysicalEntry") == "true")
        val folder = File(instrumentation.context.getExternalFilesDir(null),
            "entry-${UUID.randomUUID()}").apply { check(mkdirs()) }
        fun capture(name: String) {
            val bitmap = instrumentation.uiAutomation.takeScreenshot()
            assertNotNull("native window screenshot", bitmap)
            File(folder, "$name.png").outputStream().use {
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
            bitmap.recycle()
        }
        fun findOrb(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (node == null) return null
            if (node.isClickable && node.contentDescription?.toString()?.contains("KX") == true) return node
            for (index in 0 until node.childCount) {
                findOrb(node.getChild(index))?.let { return it }
            }
            return null
        }
        fun orb() = findOrb(instrumentation.uiAutomation.rootInActiveWindow)
        val homeLabels = listOf("en", "ar").map {
            strings(instrumentation.targetContext, it).getString(R.string.nav_home)
        }.toSet()
        val serverLabels = listOf("en", "ar").map {
            strings(instrumentation.targetContext, it).getString(R.string.nav_servers)
        }.toSet()
        fun containsLabel(node: AccessibilityNodeInfo?, labels: Set<String>): Boolean {
            if (node == null) return false
            if (node.text?.toString()?.lineSequence()?.any { it.trim() in labels } == true) return true
            return (0 until node.childCount).any { containsLabel(node.getChild(it), labels) }
        }
        fun homeVisible(): Boolean {
            val root = instrumentation.uiAutomation.rootInActiveWindow
            return root?.packageName?.toString() == "com.knoux.kxprivatevpn" &&
                containsLabel(root, homeLabels) && containsLabel(root, serverLabels) && findOrb(root) == null
        }
        fun waitFor(timeout: Long, condition: () -> Boolean): Boolean {
            val deadline = SystemClock.uptimeMillis() + timeout
            while (SystemClock.uptimeMillis() < deadline) {
                if (condition()) return true
                SystemClock.sleep(40)
            }
            return condition()
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            // These are real elapsed-time native window samples, not rendered mockups.
            capture("01-launch-window")
            SystemClock.sleep(350)
            capture("02-arrival")
            SystemClock.sleep(1000)
            capture("03-atmosphere")
            assertTrue("entry becomes operable", waitFor(15_000) { orb()?.isEnabled == true })
            capture("04-explicit-entry")
            SystemClock.sleep(850)
            assertNotNull("no automatic navigation after the intro", orb())
            check(orb()!!.performAction(AccessibilityNodeInfo.ACTION_CLICK))
            SystemClock.sleep(120)
            capture("05-rotation-start")
            assertTrue("entry remains visible during rotation", orb() != null)
            SystemClock.sleep(180)
            capture("06-rotation-middle")
            assertTrue("Home content appears after the rotation", waitFor(5000) { homeVisible() })
            capture("07-home")
            scenario.recreate()
            assertTrue("Home is restored after recreation", waitFor(5000) { homeVisible() })
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            assertTrue("Home is visible after resume", waitFor(5000) { homeVisible() })
            assertFalse("no entry gate after resume", orb()?.isEnabled == true)
            capture("08-resumed")
        }
        File(folder, "provenance.txt").writeText(
            "Real MainActivity; real elapsed time; no Compose test clock.\n" +
                "No test-owned store writes, VPN actions, permissions or deletion.\n" +
                "01 is after ActivityScenario launch, not proof of every system-splash frame.\n" +
                "Native navigation semantics covered separately by instrumentation.\n")
        println("KX_PRIVATE_ENTRY_CAPTURE_DIR=${folder.name}")
    }
}
