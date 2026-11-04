package com.knoux.kxprivatevpn

import com.knoux.kxprivatevpn.launch.IntroTimeline
import org.junit.Assert.*
import org.junit.Test

class IntroTimelineTest {
    @Test fun boundedAndOrderedChoreography() {
        for (i in -10..110) {
            val f = IntroTimeline.frame(i / 100f)
            listOf(f.convergence, f.ribbons, f.artwork, f.wordmark, f.welcome, f.pulse)
                .forEach { assertTrue(it in 0f..1f) }
        }
        assertEquals(0f, IntroTimeline.frame(.5f).artwork)
        assertEquals(0f, IntroTimeline.frame(.8f).welcome)
        assertEquals(1f, IntroTimeline.frame(1f).welcome)
        assertEquals(0f, IntroTimeline.frame(1f).pulse, .0001f)
    }
    @Test fun revealNeverReverses() {
        val frames = (0..100).map { IntroTimeline.frame(it / 100f) }
        frames.zipWithNext().forEach { (a, b) ->
            assertTrue(a.convergence <= b.convergence)
            assertTrue(a.ribbons <= b.ribbons)
            assertTrue(a.artwork <= b.artwork)
            assertTrue(a.wordmark <= b.wordmark)
            assertTrue(a.welcome <= b.welcome)
        }
    }
    @Test fun accessibilityAndBatteryAlwaysTakePriority() {
        assertTrue(IntroTimeline.shouldReduceMotion(0f, false, false))
        assertTrue(IntroTimeline.shouldReduceMotion(1f, true, false))
        assertTrue(IntroTimeline.shouldReduceMotion(1f, false, true))
        assertFalse(IntroTimeline.shouldReduceMotion(1f, false, false))
    }
}
