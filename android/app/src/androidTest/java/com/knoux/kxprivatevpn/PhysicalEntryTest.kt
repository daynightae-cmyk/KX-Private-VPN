package com.knoux.kxprivatevpn

import android.graphics.Bitmap
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
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
 * Captures remain in a fresh test-owned directory and require review before publication.
 */
class PhysicalEntryTest {
    @Test fun actualLauncherWaitsForOrbAndRestoresHome() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        org.junit.Assume.assumeTrue("physical entry capture is explicitly opt-in",
            InstrumentationRegistry.getArguments().getString("kxPhysicalEntry") == "true")
        val folder = File(checkNotNull(instrumentation.targetContext.getExternalFilesDir(null)),
            "entry-${UUID.randomUUID()}").apply { check(mkdirs()) }
        val startedAt = SystemClock.uptimeMillis()
        val timings = mutableListOf("event\tuptimeSinceStartMs")
        fun record(event: String): Long = SystemClock.uptimeMillis().also {
            timings += "$event\t${it - startedAt}"
        }
        val context = instrumentation.targetContext
        val animatorScale = Settings.Global.getFloat(context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        val powerSave = context.getSystemService(PowerManager::class.java).isPowerSaveMode
        val touchExploration = context.getSystemService(AccessibilityManager::class.java)
            .isTouchExplorationEnabled
        File(folder, "motion-policy.txt").writeText(
            "animatorScale=$animatorScale\npowerSave=$powerSave\ntouchExploration=$touchExploration\n")
        // Do not change the owner's settings or silently pass a reduced-motion capture
        // as evidence of the normal spin. Reduced motion has separate deterministic tests.
        assertTrue("normal-spin evidence requires scale=1, no power saver or touch exploration",
            animatorScale == 1f && !powerSave && !touchExploration)
        fun capture(name: String) {
            record("$name:screenshot-start")
            val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
            record("$name:screenshot-returned")
            try {
                File(folder, "$name.png").outputStream().use {
                    check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
                }
            } finally {
                bitmap.recycle()
                record("$name:png-saved")
            }
        }
        // Presence, not clickability: the product deliberately disables the emblem the
        // instant a tap is accepted (double-tap protection during the 680ms spin), so a
        // clickable-only finder would mistake the still-visible rotating orb for navigation.
        // Match the localized entry hint exactly: Home's brand lockup also describes a "KX"
        // orb (brand_description) and must not count as the entry gate.
        val entryLabels = listOf("en", "ar").map {
            strings(instrumentation.targetContext, it).getString(R.string.tap_kx_orb_to_enter)
        }.toSet()
        fun findOrb(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (node == null) return null
            val description = node.contentDescription?.toString()?.trim()
            if (description != null && description in entryLabels) return node
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
        try {
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
                val target = checkNotNull(orb())
                val tapAt = record("tap-request")
                check(target.performAction(AccessibilityNodeInfo.ACTION_CLICK))
                record("tap-returned")
                SystemClock.sleep(120)
                // Observe BEFORE screenshot/compression/I/O, which can exceed the spin itself.
                // A missed real-time sampling window must fail as incomplete evidence, not pass.
                record("rotation-check-start")
                val root = instrumentation.uiAutomation.rootInActiveWindow
                val rotatingOrb = findOrb(root)
                val homeAlreadyPresent = containsLabel(root, homeLabels) && containsLabel(root, serverLabels)
                val sampledAt = record("rotation-check-end")
                assertTrue("rotation sample missed the 680ms window: ${sampledAt - tapAt}ms; evidence incomplete",
                    sampledAt - tapAt < 680)
                assertNotNull("entry remains visible during rotation", rotatingOrb)
                assertFalse("Home must not appear during the spin", homeAlreadyPresent)
                capture("05-rotation-start")
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
        } finally {
            File(folder, "timing.tsv").writeText(timings.joinToString("\n", postfix = "\n"))
            println("KX_PRIVATE_ENTRY_CAPTURE_DIR=${folder.name}")
        }
        File(folder, "provenance.txt").writeText(
            "Real MainActivity; real elapsed time; no Compose test clock.\n" +
                "No test-owned store writes, VPN actions, permissions or deletion.\n" +
                "01 is after ActivityScenario launch, not proof of every system-splash frame.\n" +
                "05/06 are sequential post-tap screenshots; names do not guarantee animation phase.\n" +
                "See timing.tsv for screenshot and PNG-write times; semantic rotation check precedes capture.\n" +
                "Controlled-clock launcher regression checks are separate from these real-time samples.\n")
    }
}
