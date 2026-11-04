package com.knoux.kxprivatevpn.launch

import kotlin.math.sin
import kotlin.math.PI

/** All values describe artwork only; none represent connection or loading progress. */
data class IntroFrame(
    val convergence: Float, val ribbons: Float, val artwork: Float,
    val wordmark: Float, val welcome: Float, val pulse: Float,
)

object IntroTimeline {
    const val DURATION_MS = 3600
    private fun segment(t: Float, start: Float, end: Float): Float {
        val x = ((t - start) / (end - start)).coerceIn(0f, 1f)
        return x * x * (3f - 2f * x)
    }
    fun frame(progress: Float): IntroFrame {
        val p = progress.coerceIn(0f, 1f)
        return IntroFrame(
            segment(p, 0f, .36f), segment(p, .14f, .60f),
            segment(p, .59f, .82f), segment(p, .56f, .79f),
            segment(p, .86f, 1f),
            sin(segment(p, .74f, .94f) * PI).toFloat().coerceAtLeast(0f),
        )
    }
    fun shouldReduceMotion(animatorScale: Float, powerSave: Boolean, touchExploration: Boolean) =
        animatorScale == 0f || powerSave || touchExploration
}
