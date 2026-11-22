package com.knoux.kxprivatevpn.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.knoux.kxprivatevpn.launch.rememberReducedMotion
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

/**
 * One shared ambient clock for the whole app.
 *
 * Every value is a [State] rather than a plain Float so that a glass surface or an
 * atmosphere can read it *inside* a `drawBehind` / `graphicsLayer` lambda. Reading it
 * there only invalidates the draw (or the layer) phase for that frame, so the whole
 * screen never has to recompose sixty times a second just to stay alive.
 *
 * When the device asks for reduced motion the app provides [Still], whose values are
 * frozen at a stable midpoint, and every consumer renders the same composition it would
 * have drawn mid-animation. Nothing is ever left blank.
 */
@Stable
class KxAmbient internal constructor(
    /** False when the scene is frozen (reduced motion) and consumers should hold still. */
    val enabled: Boolean,
    /** 0..1 slow parallax drift of the background blooms and ribbons. */
    val drift: State<Float>,
    /** 0..1 periodic specular sweep that glides across the glass. */
    val shimmer: State<Float>,
    /** 0..1 soft breathing bloom behind the primary surfaces. */
    val breathe: State<Float>,
    /** 0..1 twinkle used by dust motes and live dots. */
    val twinkle: State<Float>,
    /** 0..360 continuous orbital angle. */
    val orbit: State<Float>,
) {
    companion object {
        /** A deliberately frozen scene: mid-breathe, mid-drift, no sparkle. */
        val Still: KxAmbient = KxAmbient(
            enabled = false,
            drift = mutableFloatStateOf(.5f),
            shimmer = mutableFloatStateOf(.5f),
            breathe = mutableFloatStateOf(.5f),
            twinkle = mutableFloatStateOf(1f),
            orbit = mutableFloatStateOf(0f),
        )
    }
}

val LocalKxAmbient = staticCompositionLocalOf { KxAmbient.Still }

/**
 * Builds the single ambient clock for a screen. All five channels share one frame-paced
 * loop so a screen pays for exactly one bounded frame stream.
 *
 * Why not [androidx.compose.animation.core.rememberInfiniteTransition]: an infinite
 * transition requests a frame on EVERY vsync. CI run 45042088536 proved that on a slow
 * (software) renderer each ambient frame costs ~150ms, so a per-vsync stream keeps the
 * main looper permanently busy and starves every idle wait in the process for minutes
 * (API35: 115s between activity RESUMED and the next test statement; API26: suite wedge
 * at 346/350). It also burns battery on the owner's low-end hardware for motion that is
 * deliberately slow (2.4s..26s periods). This clock therefore:
 *  - caps updates at ~30fps (half-vsync on 60Hz panels),
 *  - backs off adaptively when frames themselves are slow, always leaving idle gaps,
 *  - advances channels by real elapsed time, so periods/waveforms are unchanged,
 *  - stops entirely when the window loses focus or the lifecycle pauses.
 */
@Composable
fun rememberKxAmbient(enabled: Boolean = true): KxAmbient {
    // Reuse the app-level clock when one is already live so nested themes never pay for
    // a second frame-driven loop.
    val inherited = LocalKxAmbient.current
    if (inherited.enabled && enabled) return inherited
    val reduced = rememberReducedMotion()
    // The clock only exists while the window is actually visible. When another activity
    // (or the notification shade) takes the foreground the host pauses; stopping the
    // loop there means zero frame requests from a covered app — real battery savings on
    // low-end hardware, and no ambient frames competing with shade captures.
    val focused = LocalWindowInfo.current.isWindowFocused
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val foreground = focused && lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    if (!enabled || reduced || !foreground) return KxAmbient.Still
    val drift = remember { mutableFloatStateOf(0f) }
    val shimmer = remember { mutableFloatStateOf(0f) }
    val breathe = remember { mutableFloatStateOf(0f) }
    val twinkle = remember { mutableFloatStateOf(0f) }
    val orbit = remember { mutableFloatStateOf(0f) }
    val live = remember { KxAmbient(true, drift, shimmer, breathe, twinkle, orbit) }
    LaunchedEffect(Unit) {
        var startNanos = 0L
        var lastNanos = 0L
        // Minimum spacing between clock updates; grows when the device cannot keep up.
        var intervalMs = 33L
        while (true) {
            // One frame request per update, then a real delay: between updates the
            // recomposer has NO pending frame awaiters, so idle-based tooling (and the
            // OS scheduler) genuinely regains the main thread. A bare withFrameNanos
            // loop would keep a per-vsync awaiter alive and starve idle waits exactly
            // like the infinite transition it replaces.
            val frameNanos = withFrameNanos { it }
            if (startNanos == 0L) startNanos = frameNanos
            if (lastNanos != 0L) {
                val deltaMs = ((frameNanos - lastNanos) / 1_000_000f).coerceAtMost(1_000f)
                // Adaptive pacing: when producing frames is slower than our interval,
                // back off to ~2x the observed frame cost (ceiling 500ms) so the app
                // never monopolises a slow renderer.
                intervalMs = when {
                    deltaMs > intervalMs * 1.5f -> (deltaMs * 2f).toLong().coerceIn(33L, 500L)
                    deltaMs < intervalMs * .75f -> (intervalMs * 9L) / 10L
                    else -> intervalMs
                }.coerceAtLeast(33L)
                // Phase relative to clock start keeps float precision high even after
                // days of device uptime; periods/waveforms match the original specs.
                val t = (frameNanos - startNanos) / 1_000_000f
                drift.floatValue = (t % 26_000f) / 26_000f
                shimmer.floatValue = FastOutSlowInEasing.transform((t % 6_800f) / 6_800f)
                // Reverse-mode channels: triangle wave over double the period.
                val breathePhase = (t % 7_800f) / 3_900f
                breathe.floatValue = FastOutSlowInEasing.transform(
                    if (breathePhase <= 1f) breathePhase else 2f - breathePhase,
                )
                val twinklePhase = (t % 4_800f) / 2_400f
                twinkle.floatValue = if (twinklePhase <= 1f) twinklePhase else 2f - twinklePhase
                orbit.floatValue = (t % 22_000f) / 22_000f * 360f
            }
            lastNanos = frameNanos
            delay(intervalMs)
        }
    }
    return live
}

/** True when the ambient scene is running; consumers may fall back to a still frame. */
val KxAmbient.isLive: Boolean get() = enabled

/**
 * A soft specular band that glides diagonally across a glass surface, fading in and out
 * with a sine envelope so the light enters and leaves instead of snapping. Painting it in
 * `drawBehind` keeps it beneath the card's own content, so text keeps full contrast.
 */
internal fun DrawScope.kxSheen(progress: Float, night: Boolean, strength: Float = 1f) {
    if (size.width <= 0f || size.height <= 0f) return
    val envelope = sin(progress * PI).toFloat().coerceIn(0f, 1f)
    val alpha = (if (night) .13f else .34f) * envelope * strength
    if (alpha <= .002f) return
    val x = -size.width * .3f + progress * (size.width * 1.6f)
    drawRect(
        Brush.linearGradient(
            listOf(Color.Transparent, Color.White.copy(alpha = alpha), Color.Transparent),
            start = Offset(x - size.width * .16f, 0f),
            end = Offset(x + size.width * .16f, size.height),
        ),
    )
}

/**
 * A contained bloom that breathes. Used behind primary actions and the hero orb so the
 * light has a physical rhythm instead of a constant glow.
 */
internal fun DrawScope.kxBreathe(progress: Float, color: Color, radius: Float, center: Offset) {
    val eased = .5f + .5f * sin((progress - .5f) * PI).toFloat()
    drawCircle(
        Brush.radialGradient(
            listOf(color.copy(alpha = color.alpha * (.6f + .4f * eased)), Color.Transparent),
            center, radius,
        ),
        radius, center,
    )
}

/** Cubic Bezier sample used to glide a comet along the reference hero ribbon. */
internal fun cubicPoint(
    x0: Float, y0: Float, x1: Float, y1: Float,
    x2: Float, y2: Float, x3: Float, y3: Float, t: Float,
): Offset {
    val u = 1f - t
    val a = u * u * u
    val b = 3f * u * u * t
    val c = 3f * u * t * t
    val d = t * t * t
    return Offset(a * x0 + b * x1 + c * x2 + d * x3, a * y0 + b * y1 + c * y2 + d * y3)
}

/**
 * Entrance choreography for glass sections. Sections rise and fade in once, staggered by
 * [index], so opening a screen feels designed rather than instant. Reduced-motion users get
 * the final layout with no transform at all.
 *
 * The animation lives entirely in the layer phase: the [Animatable] value is read inside the
 * `graphicsLayer` lambda, never during composition, so a sixty-frame entrance does not
 * recompose the screen sixty times. It is also capped: after [durationMillis] plus the
 * stagger delay the layer stops invalidating, so off-screen sections queued in a scrollable
 * list cannot keep an infinite stream of pending frames alive (which starves Compose test
 * synchronization and low-end devices alike).
 */
@Composable
fun Modifier.kxEntrance(
    index: Int = 0,
    rise: Dp = 16.dp,
    durationMillis: Int = 540,
): Modifier {
    val reduced = rememberReducedMotion()
    if (reduced) return this
    val progress = remember { Animatable(0f) }
    val settled = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = durationMillis,
                delayMillis = index.coerceIn(0, 10) * 55,
                easing = FastOutSlowInEasing,
            ),
        )
        settled.value = true
    }
    return this.graphicsLayer {
        if (settled.value) return@graphicsLayer
        val value = progress.value
        alpha = value.coerceIn(0f, 1f)
        translationY = (1f - value) * rise.toPx()
    }
}

/**
 * A live indicator that breathes while [active]. Reads the ambient clock in the layer
 * phase so the pulse costs a layer update, not a recomposition.
 */
@Composable
fun Modifier.kxLivePulse(active: Boolean, minimumScale: Float = .78f): Modifier {
    val ambient = LocalKxAmbient.current
    if (!active) return this
    return this.graphicsLayer {
        if (ambient.enabled) {
            val t = ambient.twinkle.value
            val scale = minimumScale + (1f - minimumScale) * t
            scaleX = scale
            scaleY = scale
            alpha = .55f + .45f * t
        }
    }
}

/**
 * Weightless float for orbiting satellites and emblem tiles: a few dp of vertical drift on
 * a sine of the shared breathe channel, phase-shifted per element so a constellation never
 * moves in lockstep. Layer-phase only; frozen scenes render at the resting position.
 */
@Composable
fun Modifier.kxFloat(phase: Float = 0f, amplitude: Dp = 3.dp): Modifier {
    val ambient = LocalKxAmbient.current
    if (!ambient.enabled) return this
    return this.graphicsLayer {
        val t = (ambient.breathe.value + phase) % 1f
        translationY = sin(t * 2f * PI.toFloat()) * amplitude.toPx()
    }
}
