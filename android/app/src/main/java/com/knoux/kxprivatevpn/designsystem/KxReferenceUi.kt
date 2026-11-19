package com.knoux.kxprivatevpn.designsystem

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knoux.kxprivatevpn.R
import com.knoux.kxprivatevpn.launch.IntroTimeline
import com.knoux.kxprivatevpn.launch.rememberReducedMotion
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

val KxCairo: FontFamily = FontFamily(
    Font(R.font.cairo_regular, FontWeight.Normal),
    Font(R.font.cairo_semibold, FontWeight.SemiBold),
    Font(R.font.cairo_bold, FontWeight.Bold),
    Font(R.font.cairo_extrabold, FontWeight.ExtraBold),
)

private val baseTypography = Typography()
val KxTypography: Typography = baseTypography.copy(
    displayLarge = baseTypography.displayLarge.copy(fontFamily = KxCairo),
    displayMedium = baseTypography.displayMedium.copy(fontFamily = KxCairo),
    displaySmall = baseTypography.displaySmall.copy(fontFamily = KxCairo),
    headlineLarge = baseTypography.headlineLarge.copy(fontFamily = KxCairo),
    headlineMedium = baseTypography.headlineMedium.copy(fontFamily = KxCairo),
    headlineSmall = baseTypography.headlineSmall.copy(fontFamily = KxCairo),
    titleLarge = baseTypography.titleLarge.copy(fontFamily = KxCairo),
    titleMedium = baseTypography.titleMedium.copy(fontFamily = KxCairo),
    titleSmall = baseTypography.titleSmall.copy(fontFamily = KxCairo),
    bodyLarge = baseTypography.bodyLarge.copy(fontFamily = KxCairo),
    bodyMedium = baseTypography.bodyMedium.copy(fontFamily = KxCairo),
    bodySmall = baseTypography.bodySmall.copy(fontFamily = KxCairo),
    labelLarge = baseTypography.labelLarge.copy(fontFamily = KxCairo),
    labelMedium = baseTypography.labelMedium.copy(fontFamily = KxCairo),
    labelSmall = baseTypography.labelSmall.copy(fontFamily = KxCairo),
)

/** The three reference screens share one palette, typeface and glass vocabulary. */
@Composable
fun KxReferenceTheme(night: Boolean, content: @Composable () -> Unit) {
    val scheme = if (night) darkColorScheme(
        primary = Color(0xFF64EEFF), onPrimary = Color(0xFF0C1633),
        secondary = Color(0xFFAD71FF), onSecondary = Color(0xFF120C2B),
        surface = Color(0xFF1B1D4D), onSurface = kxInk(true),
        background = Color(0xFF14173F), onBackground = kxInk(true),
    ) else lightColorScheme(
        primary = Color(0xFF4C1FBF), onPrimary = Color.White,
        secondary = Color(0xFF7C3AED), onSecondary = Color.White,
        surface = Color(0xFFF4F1FF), onSurface = kxInk(false),
        background = Color(0xFFEFEBFF), onBackground = kxInk(false),
    )
    MaterialTheme(colorScheme = scheme, typography = KxTypography) {
        CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides kxInk(night),
            androidx.compose.material3.LocalTextStyle provides androidx.compose.material3.LocalTextStyle.current.copy(
                fontFamily = KxCairo,
            ),
            content = content,
        )
    }
}

fun kxInk(night: Boolean): Color = if (night) Color(0xFFF7F3FF) else Color(0xFF171050)

fun kxMuted(night: Boolean): Color = kxInk(night).copy(alpha = if (night) .72f else .76f)

fun kxCyan(night: Boolean): Color = if (night) Color(0xFF64EEFF) else Color(0xFF0B7899)

fun kxViolet(night: Boolean): Color = if (night) Color(0xFFAD71FF) else Color(0xFF5B28C7)

fun kxRose(night: Boolean): Color = if (night) Color(0xFFFF8AD8) else Color(0xFFB11E78)

fun kxGreen(night: Boolean): Color = if (night) Color(0xFF5BE8AE) else Color(0xFF087A56)

/** Which approved reference backdrop a screen reproduces. */
enum class KxAtmosphereStyle {
    /** Ref 14 / Ref 18: magenta sweep in the upper left, cyan sweep on the right. */
    STANDARD,

    /** Ref 03: a violet diagonal wash and symmetric waves flanking the hero orb. */
    REPAIR,
}

/**
 * Deep indigo glass-ribbon atmosphere measured from the owner's approved references.
 *
 * Ref 14 (Home), Ref 18 (Tools) and Ref 03 (Repair) share one backdrop vocabulary: a deep
 * ink base, a luminous violet/magenta sweep entering from the upper left, a cyan sweep on
 * the right and a soft bloom behind the hero. Every sweep is painted in three passes - a
 * wide translucent halo, a mid tone body and a thin bright core - which reproduces the
 * reference bloom without a per-frame blur or a full-screen RenderEffect.
 */
@Composable
fun KxReferenceAtmosphere(night: Boolean, style: KxAtmosphereStyle = KxAtmosphereStyle.STANDARD) {
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (night) {
            drawRect(
                Brush.linearGradient(
                    listOf(Color(0xFF241355), Color(0xFF191549), Color(0xFF101A42), Color(0xFF080D28)),
                    Offset(0f, 0f), Offset(w * .85f, h),
                ),
            )
            // Corner blooms: violet top left, cyan top right, magenta far left.
            drawCircle(
                Brush.radialGradient(
                    listOf(Color(0xFF9A4BF5).copy(alpha = .50f), Color(0xFF4F2AA6).copy(alpha = .22f), Color.Transparent),
                    Offset(w * .06f, h * .01f), w * .88f,
                ),
                w * .88f, Offset(w * .06f, h * .01f),
            )
            drawCircle(
                Brush.radialGradient(
                    listOf(Color(0xFF1FC6F2).copy(alpha = .26f), Color.Transparent),
                    Offset(w * .97f, h * .22f), w * .58f,
                ),
                w * .58f, Offset(w * .97f, h * .22f),
            )
            drawCircle(
                Brush.radialGradient(
                    listOf(Color(0xFFFF6FD0).copy(alpha = .20f), Color.Transparent),
                    Offset(w * .02f, h * .26f), w * .42f,
                ),
                w * .42f, Offset(w * .02f, h * .26f),
            )
            if (style == KxAtmosphereStyle.REPAIR) {
                // Ref 03 opens with a violet diagonal wash across the top left corner.
                val wash = Path().apply {
                    moveTo(0f, 0f); lineTo(w * .66f, 0f)
                    cubicTo(w * .38f, h * .09f, w * .16f, h * .16f, 0f, h * .31f)
                    close()
                }
                drawPath(wash, Brush.linearGradient(listOf(Color(0xFFB15CFF).copy(alpha = .52f), Color.Transparent)))
                val washEdge = Path().apply {
                    moveTo(0f, h * .31f)
                    cubicTo(w * .16f, h * .16f, w * .38f, h * .09f, w * .66f, 0f)
                }
                drawPath(
                    washEdge,
                    Brush.linearGradient(listOf(Color.Transparent, Color(0xFFD9A8FF).copy(alpha = .70f), Color.Transparent),
                        Offset(0f, h * .31f), Offset(w * .66f, 0f)),
                    style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round),
                )
            }
            // Hero ribbon: the broad diagonal band that crosses the reference hero zone
            // behind the orb. It is painted before the thinner sweeps so it reads as depth,
            // not as a foreground stroke.
            neonSweep(
                Path().apply {
                    moveTo(-w * .06f, h * .43f)
                    cubicTo(w * .18f, h * .31f, w * .34f, h * .22f, w * .60f, h * .23f)
                    cubicTo(w * .80f, h * .24f, w * .94f, h * .30f, w * 1.06f, h * .40f)
                },
                listOf(Color(0xFF9B63FF), Color(0xFF52C8FF), Color(0xFFF08BFF)),
                coreWidth = 2.6.dp.toPx(),
                start = Offset(0f, h * .40f), end = Offset(w, h * .28f),
            )
            // Neon sweep A: violet/magenta entering from the top and leaving through the left edge.
            neonSweep(
                Path().apply {
                    moveTo(w * .48f, -h * .06f)
                    cubicTo(w * .30f, h * .02f, w * .11f, h * .11f, -w * .06f, h * .31f)
                },
                listOf(Color(0xFFD46BFF), Color(0xFF8A44FF), Color(0xFF4C6BFF)),
                coreWidth = 1.9.dp.toPx(),
                start = Offset(w * .48f, 0f), end = Offset(0f, h * .31f),
            )
            // Neon sweep B: a second, dimmer magenta edge below the first one.
            neonSweep(
                Path().apply {
                    moveTo(w * .40f, -h * .03f)
                    cubicTo(w * .21f, h * .06f, w * .02f, h * .18f, -w * .06f, h * .42f)
                },
                listOf(Color(0xFFFF7BD8), Color(0xFFA64BFF), Color(0xFF3E5BE8)),
                coreWidth = 1.1.dp.toPx(),
                start = Offset(w * .40f, 0f), end = Offset(0f, h * .42f),
            )
            // Neon sweep C: cyan wave on the right shoulder of the screen.
            neonSweep(
                Path().apply {
                    moveTo(w * 1.04f, h * .01f)
                    cubicTo(w * .86f, h * .04f, w * .70f, h * .11f, w * .60f, h * .23f)
                    cubicTo(w * .73f, h * .28f, w * .90f, h * .30f, w * 1.04f, h * .32f)
                },
                listOf(Color(0xFF8FF6FF), Color(0xFF29C8F0), Color(0xFF7C63FF)),
                coreWidth = 1.6.dp.toPx(),
                start = Offset(w, h * .02f), end = Offset(w * .60f, h * .27f),
            )
            // Neon sweep D: the lower violet-to-cyan wave that carries the eye to the content.
            neonSweep(
                Path().apply {
                    moveTo(-w * .06f, h * .88f)
                    cubicTo(w * .20f, h * .73f, w * .50f, h * .96f, w * .76f, h * .87f)
                    cubicTo(w * .90f, h * .83f, w * .99f, h * .79f, w * 1.06f, h * .75f)
                },
                listOf(Color(0xFF7B5CFF), Color(0xFF36CBF5), Color(0xFFFF7BD8)),
                coreWidth = 1.5.dp.toPx(),
                start = Offset(0f, h * .88f), end = Offset(w, h * .80f),
            )
            if (style == KxAtmosphereStyle.REPAIR) {
                // Ref 03 flanks the hero orb with two waves: cyan on the left, magenta on the right.
                neonSweep(
                    Path().apply {
                        moveTo(-w * .06f, h * .63f)
                        cubicTo(w * .06f, h * .55f, w * .16f, h * .64f, w * .25f, h * .74f)
                    },
                    listOf(Color(0xFF8FF6FF), Color(0xFF2AA8E8), Color.Transparent),
                    coreWidth = 1.4.dp.toPx(),
                    start = Offset(0f, h * .60f), end = Offset(w * .25f, h * .74f),
                )
                neonSweep(
                    Path().apply {
                        moveTo(w * 1.06f, h * .60f)
                        cubicTo(w * .94f, h * .52f, w * .84f, h * .61f, w * .75f, h * .71f)
                    },
                    listOf(Color(0xFFFF9BE0), Color(0xFFC255F5), Color.Transparent),
                    coreWidth = 1.4.dp.toPx(),
                    start = Offset(w, h * .58f), end = Offset(w * .75f, h * .71f),
                )
            }
            // Dust motes: a fixed constellation adds depth without a per-frame particle
            // system. Positions are hand-placed so the result is deterministic in tests.
            kxDustMotes.forEach { mote ->
                val p = Offset(w * mote.x, h * mote.y)
                drawCircle(
                    Brush.radialGradient(
                        listOf(Color.White.copy(alpha = mote.alpha), Color.Transparent),
                        p, mote.radius * w,
                    ),
                    mote.radius * w, p,
                )
            }
            // Depth: a deep vignette keeps the lower content readable over the waves.
            drawRect(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Transparent, Color(0xFF060A22).copy(alpha = .46f)),
                    startY = h * .45f, endY = h,
                ),
            )
        } else {
            drawRect(
                Brush.linearGradient(
                    listOf(Color(0xFFEDE6FF), Color(0xFFDCE8FF), Color(0xFFF8F4FF), Color(0xFFE7ECFF)),
                    Offset.Zero, Offset(w, h),
                ),
            )
            drawCircle(
                Brush.radialGradient(
                    listOf(Color(0xFF9A77F5).copy(alpha = .38f), Color.Transparent),
                    Offset(w * .05f, h * .03f), w * .82f,
                ),
                w * .82f, Offset(w * .05f, h * .03f),
            )
            drawCircle(
                Brush.radialGradient(
                    listOf(Color(0xFF54C8F5).copy(alpha = .28f), Color.Transparent),
                    Offset(w * .96f, h * .28f), w * .58f,
                ),
                w * .58f, Offset(w * .96f, h * .28f),
            )
            neonSweep(
                Path().apply {
                    moveTo(w * .48f, -h * .06f)
                    cubicTo(w * .30f, h * .02f, w * .11f, h * .11f, -w * .06f, h * .31f)
                },
                listOf(Color(0xFFB58BFF), Color(0xFF8B5CF6), Color(0xFF5B8CF6)),
                coreWidth = 1.7.dp.toPx(),
                start = Offset(w * .48f, 0f), end = Offset(0f, h * .31f),
            )
            neonSweep(
                Path().apply {
                    moveTo(w * 1.04f, h * .01f)
                    cubicTo(w * .86f, h * .04f, w * .70f, h * .11f, w * .60f, h * .23f)
                    cubicTo(w * .73f, h * .28f, w * .90f, h * .30f, w * 1.04f, h * .32f)
                },
                listOf(Color(0xFF7FDDF7), Color(0xFF38BDF8), Color(0xFF9C8BF0)),
                coreWidth = 1.4.dp.toPx(),
                start = Offset(w, h * .02f), end = Offset(w * .60f, h * .27f),
            )
            neonSweep(
                Path().apply {
                    moveTo(-w * .06f, h * .88f)
                    cubicTo(w * .20f, h * .73f, w * .50f, h * .96f, w * .76f, h * .87f)
                    cubicTo(w * .90f, h * .83f, w * .99f, h * .79f, w * 1.06f, h * .75f)
                },
                listOf(Color(0xFF8B5CF6), Color(0xFF38BDF8), Color(0xFFC4B5FD)),
                coreWidth = 1.3.dp.toPx(),
                start = Offset(0f, h * .88f), end = Offset(w, h * .80f),
            )
        }
    }
}

/**
 * Paints one luminous sweep in three passes: a wide translucent halo, a mid tone body and a
 * thin bright core. The overlap reproduces the reference bloom without an expensive blur.
 */
private fun DrawScope.neonSweep(
    path: Path,
    colors: List<Color>,
    coreWidth: Float,
    start: Offset,
    end: Offset,
) {
    drawPath(
        path,
        Brush.linearGradient(colors.map { it.copy(alpha = it.alpha * .20f) }, start, end),
        style = Stroke(coreWidth * 3.6f, cap = StrokeCap.Round),
    )
    drawPath(
        path,
        Brush.linearGradient(colors.map { it.copy(alpha = it.alpha * .50f) }, start, end),
        style = Stroke(coreWidth * 1.8f, cap = StrokeCap.Round),
    )
    drawPath(
        path,
        Brush.linearGradient(colors, start, end),
        style = Stroke(coreWidth, cap = StrokeCap.Round),
    )
}

/**
 * The bloom that sits behind a hero orb. Ref 14 and Ref 03 both show a wide cyan/violet
 * halo around the emblem, which the plain orb drawing alone does not produce.
 */
@Composable
fun KxOrbAura(night: Boolean, modifier: Modifier = Modifier, strength: Float = 1f) {
    Canvas(modifier) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        if (night) {
            drawCircle(
                Brush.radialGradient(
                    listOf(
                        Color(0xFF8FF6FF).copy(alpha = .26f * strength),
                        Color(0xFF9B6BFF).copy(alpha = .17f * strength),
                        Color.Transparent,
                    ),
                    center, radius,
                ),
                radius, center,
            )
        } else {
            drawCircle(
                Brush.radialGradient(
                    listOf(
                        Color(0xFF8ADCF7).copy(alpha = .30f * strength),
                        Color(0xFFB79CF8).copy(alpha = .18f * strength),
                        Color.Transparent,
                    ),
                    center, radius,
                ),
                radius, center,
            )
        }
    }
}

private data class KxDustMote(val x: Float, val y: Float, val radius: Float, val alpha: Float)

/** Deterministic dust field measured from the sparkle density of the owner references. */
private val kxDustMotes = listOf(
    KxDustMote(.12f, .17f, .0040f, .55f), KxDustMote(.22f, .09f, .0024f, .45f),
    KxDustMote(.31f, .21f, .0032f, .38f), KxDustMote(.43f, .13f, .0020f, .50f),
    KxDustMote(.57f, .08f, .0036f, .42f), KxDustMote(.68f, .18f, .0022f, .48f),
    KxDustMote(.79f, .12f, .0042f, .36f), KxDustMote(.88f, .24f, .0026f, .52f),
    KxDustMote(.08f, .40f, .0028f, .44f), KxDustMote(.93f, .45f, .0034f, .38f),
    KxDustMote(.16f, .62f, .0022f, .34f), KxDustMote(.84f, .68f, .0030f, .40f),
    KxDustMote(.49f, .36f, .0020f, .46f), KxDustMote(.72f, .52f, .0026f, .30f),
)

/**
 * Orbital halo drawn around the hero emblem: a slow luminous ring, a dashed orbit and a
 * breathing bloom. Ref 14 and Ref 03 both show this ring; the plain orb alone reads flat.
 * Infinite motion is skipped for reduced-motion users, who still get the static halo.
 */
@Composable
fun KxOrbitalHalo(
    night: Boolean,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
) {
    val reducedMotion = rememberReducedMotion()
    val transition = rememberInfiniteTransition(label = "kx-halo")
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Restart),
        label = "kx-halo-spin",
    )
    val breathe by transition.animateFloat(
        initialValue = .78f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "kx-halo-breathe",
    )
    val rotation = if (animated && !reducedMotion) spin else 0f
    val pulse = if (animated && !reducedMotion) breathe else .92f
    Canvas(modifier) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension / 2f
        drawCircle(
            Brush.radialGradient(
                listOf(
                    Color(0xFF8FF6FF).copy(alpha = .20f * pulse),
                    Color(0xFF9B6BFF).copy(alpha = .13f * pulse),
                    Color.Transparent,
                ),
                c, r,
            ),
            r, c,
        )
        // Bright outer rim: the reference halo is a thin, hot circle, not a soft blob.
        drawCircle(
            Brush.sweepGradient(
                listOf(
                    Color(0xFFF4F0FF).copy(alpha = .55f * pulse),
                    Color(0xFF64EEFF).copy(alpha = .72f * pulse),
                    Color(0xFFAD71FF).copy(alpha = .40f * pulse),
                    Color(0xFF64EEFF).copy(alpha = .30f * pulse),
                    Color(0xFFF4F0FF).copy(alpha = .55f * pulse),
                ),
                c,
            ),
            r * .80f, c, style = Stroke(1.5.dp.toPx()),
        )
        drawCircle(
            Color.White.copy(alpha = .16f),
            r * .68f, c, style = Stroke(.8.dp.toPx()),
        )
        // A dashed orbital arc glides slowly around the emblem.
        rotate(rotation, c) {
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        Color.Transparent,
                        Color(0xFF8FF6FF).copy(alpha = .85f),
                        Color(0xFFFF8AD8).copy(alpha = .65f),
                        Color.Transparent,
                    ),
                    c,
                ),
                startAngle = 0f,
                sweepAngle = 220f,
                useCenter = false,
                topLeft = Offset(c.x - r * .72f, c.y - r * .72f),
                size = Size(r * 1.44f, r * 1.44f),
                style = Stroke(
                    width = 1.7.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(2.4.dp.toPx(), 6.4.dp.toPx()), 0f,
                    ),
                ),
            )
        }
        // Two satellites catch the light at opposite sides of the orbit.
        drawCircle(
            Brush.radialGradient(
                listOf(Color.White, Color(0xFF64EEFF), Color.Transparent),
                Offset(c.x + r * .74f, c.y - r * .26f), 5.dp.toPx(),
            ),
            3.2.dp.toPx(), Offset(c.x + r * .74f, c.y - r * .26f),
        )
        drawCircle(
            Brush.radialGradient(
                listOf(Color.White, Color(0xFFFF8AD8), Color.Transparent),
                Offset(c.x - r * .56f, c.y + r * .48f), 4.dp.toPx(),
            ),
            2.4.dp.toPx(), Offset(c.x - r * .56f, c.y + r * .48f),
        )
    }
}

/**
 * Shared tactile press feedback. The interaction source is supplied by the caller so the
 * same source drives the click, the scale and any ripple. Scale is small enough to keep
 * layout stable and is disabled automatically in tests that do not advance animation.
 */
@Composable
fun Modifier.kxPressScale(
    source: InteractionSource,
    enabled: Boolean = true,
): Modifier {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) .965f else 1f,
        animationSpec = spring(dampingRatio = .62f, stiffness = 720f),
        label = "kx-press-scale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** Light system tick used by primary actions; respects the device's haptic settings. */
@Composable
fun rememberKxTapHaptic(): () -> Unit {
    val view = androidx.compose.ui.platform.LocalView.current
    return remember(view) {
        { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) }
    }
}

/**
 * The glowing platform the Ref 03 hero orb rests on. The audit recorded this platform as the
 * one CRITICAL missing element of the Repair Lab, so it is drawn as concentric luminous
 * ellipses with a hot core rather than a flat decoration.
 */
@Composable
fun KxGlowPlatform(night: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)
        // Ambient bloom around the platform.
        drawOval(
            Brush.radialGradient(
                listOf(
                    Color(0xFF8FF6FF).copy(alpha = if (night) .26f else .30f),
                    Color(0xFFB06BFF).copy(alpha = .14f),
                    Color.Transparent,
                ),
                center, w * .48f,
            ),
            topLeft = Offset(center.x - w * .48f, center.y - h * .78f),
            size = Size(w * .96f, h * 1.56f),
        )
        // Three concentric luminous rings.
        for (i in 0..2) {
            val scale = 1f - i * .19f
            drawOval(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF8FF6FF),
                        Color(0xFFB98CFF),
                        Color(0xFFFF8AD8),
                    ),
                    Offset(center.x - w * scale / 2f, center.y),
                    Offset(center.x + w * scale / 2f, center.y),
                ),
                topLeft = Offset(center.x - w * scale / 2f, center.y - h * scale / 2f),
                size = Size(w * scale, h * scale),
                style = Stroke(
                    if (i == 0) 2.1.dp.toPx() else 1.1.dp.toPx(),
                    cap = StrokeCap.Round,
                ),
                alpha = .88f - i * .22f,
            )
        }
        // Hot core where the orb meets the platform.
        drawOval(
            Brush.radialGradient(
                listOf(Color.White.copy(alpha = .50f), Color(0xFF8FEFFF).copy(alpha = .30f), Color.Transparent),
                center, w * .22f,
            ),
            topLeft = Offset(center.x - w * .22f, center.y - h * .30f),
            size = Size(w * .44f, h * .60f),
        )
    }
}

/** Glass panel matching the reference cards: saturated indigo fill and a dual-tone rim. */
@Composable
fun KxGlassPanel(
    night: Boolean,
    modifier: Modifier = Modifier,
    radius: Dp = 22.dp,
    padding: PaddingValues = PaddingValues(12.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    val fill = if (night) Brush.linearGradient(
        listOf(
            Color(0xFF3A2C74).copy(alpha = .52f),
            Color(0xFF26235C).copy(alpha = .76f),
            Color(0xFF1B224C).copy(alpha = .84f),
        ),
    ) else Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = .82f),
            Color(0xFFE9E4FF).copy(alpha = .72f),
            Color(0xFFDCEEFF).copy(alpha = .66f),
        ),
    )
    val rim = if (night) Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = .34f),
            Color(0xFF8D7CF6).copy(alpha = .34f),
            Color(0xFF64EEFF).copy(alpha = .28f),
        ),
    ) else Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = .98f),
            Color(0xFF9C8BF0).copy(alpha = .42f),
            Color(0xFF6BC8EB).copy(alpha = .34f),
        ),
    )
    Column(
        modifier.fillMaxWidth().clip(shape)
            .drawBehind {
                // The fill and the specular bevel are painted in one pass so the top edge
                // catches the light the way the reference glass does.
                drawRect(fill)
                val bevel = 1.dp.toPx()
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = if (night) .30f else .70f),
                            Color.Transparent,
                        ),
                        startY = 0f, endY = 30.dp.toPx(),
                    ),
                    topLeft = Offset(bevel, bevel),
                    size = Size(size.width - bevel * 2, size.height - bevel * 2),
                    cornerRadius = CornerRadius((radius.toPx() - bevel).coerceAtLeast(0f)),
                    style = Stroke(bevel),
                )
            }
            .border(1.dp, rim, shape).padding(padding),
        content = content,
    )
}

@Composable
fun KxHeaderCircleButton(
    night: Boolean,
    icon: KxIconKind,
    label: String,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = CircleShape
    // The reference uses a small 30dp glass disc. The outer box keeps the 48dp touch target
    // required for accessibility while the visible disc matches the owner's screenshots.
    Box(
        modifier.size(48.dp)
            .clip(shape)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label }
            .testTagCompat(tag),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(34.dp)
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        if (night) listOf(Color.White.copy(alpha = .13f), Color(0xFF6F63D8).copy(alpha = .24f))
                        else listOf(Color.White.copy(alpha = .82f), Color(0xFFCFC6FF).copy(alpha = .52f)),
                    ),
                )
                .border(1.dp, Color.White.copy(alpha = if (night) .32f else .90f), shape),
            contentAlignment = Alignment.Center,
        ) {
            KxLineIcon(icon, if (night) Color(0xFFF6F1FF) else Color(0xFF2B1B62), Modifier.size(16.dp))
        }
    }
}

@Composable
fun KxBrandHeader(night: Boolean, modifier: Modifier = Modifier, orbSize: Dp = 30.dp) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        KxOrb(
            1f,
            IntroTimeline.frame(1f),
            Modifier.size(orbSize).clearAndSetSemantics {},
        )
        Spacer(Modifier.width(7.dp))
        Text(
            "KX",
            color = kxInk(night),
            fontSize = 23.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = .5.sp,
        )
        Spacer(Modifier.width(7.dp))
        Text(
            "PRIVATE VPN",
            color = kxMuted(night),
            fontSize = 7.5.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.6.sp,
        )
    }
}

@Composable
fun KxTitleBlock(
    title: String,
    subtitle: String,
    night: Boolean,
    modifier: Modifier = Modifier,
    announceTitleAsDescription: Boolean = false,
) {
    Column(
        modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            title,
            color = kxInk(night),
            fontSize = 27.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().then(
                if (announceTitleAsDescription) Modifier.clearAndSetSemantics {
                    contentDescription = title
                    heading()
                } else Modifier,
            ),
        )
        Text(
            subtitle,
            color = kxMuted(night),
            fontSize = 12.5.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
        )
        Box(
            Modifier.padding(top = 4.dp).width(52.dp).heightIn(min = 3.dp).clip(RoundedCornerShape(50))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, kxCyan(night), kxViolet(night), Color.Transparent),
                    ),
                ),
        )
    }
}

/** Primary reference CTA: saturated violet-to-cyan gradient with a pearl rim. */
@Composable
fun KxGradientAction(
    label: String,
    night: Boolean,
    enabled: Boolean,
    tag: String,
    modifier: Modifier = Modifier,
    icon: KxIconKind? = null,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val tapHaptic = rememberKxTapHaptic()
    val fill = if (enabled || night) {
        Brush.horizontalGradient(
            listOf(Color(0xFF8B3DFF), Color(0xFF5B45F5), Color(0xFF1E9BE8), Color(0xFF1AC8FF)),
        )
    } else {
        Brush.horizontalGradient(
            listOf(Color(0xFF6F68A8).copy(alpha = .55f), Color(0xFF575A8E).copy(alpha = .55f)),
        )
    }
    Row(
        modifier.fillMaxWidth().drawBehind {
            // Soft coloured bloom around the CTA. Two outward strokes approximate the
            // reference glow without RenderEffect or a blur pass.
            val glow = 8.dp.toPx()
            val bloom = Brush.horizontalGradient(
                listOf(
                    Color(0xFF8B3DFF).copy(alpha = if (enabled) .40f else .16f),
                    Color(0xFF1AC8FF).copy(alpha = if (enabled) .40f else .16f),
                ),
            )
            drawRoundRect(
                brush = bloom,
                topLeft = Offset(-glow, -glow),
                size = Size(size.width + glow * 2, size.height + glow * 2),
                cornerRadius = CornerRadius(size.height / 2f + glow),
                style = Stroke(glow),
            )
            drawRoundRect(
                brush = bloom,
                topLeft = Offset(-glow * .35f, -glow * .35f),
                size = Size(size.width + glow * .7f, size.height + glow * .7f),
                cornerRadius = CornerRadius(size.height / 2f + glow * .35f),
                style = Stroke(glow * 1.4f),
                alpha = .55f,
            )
        }
            .heightIn(min = 44.dp)
            .kxPressScale(interaction, enabled)
            .clip(shape)
            .background(fill)
            .border(
                1.2.dp,
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = if (enabled) .95f else .4f),
                        Color(0xFF93F3FF).copy(alpha = if (enabled) .72f else .28f),
                        Color(0xFFE3B8FF).copy(alpha = if (enabled) .72f else .28f),
                    ),
                ),
                shape,
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClickLabel = label,
            ) {
                tapHaptic()
                onClick()
            }
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .testTagCompat(tag),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            KxLineIcon(icon, Color.White.copy(alpha = if (enabled) 1f else .65f), Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            label,
            color = Color.White.copy(alpha = if (enabled) 1f else .68f),
            fontSize = 15.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun KxBottomNav(
    night: Boolean,
    selected: String,
    toolsTab: Boolean,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    data class Item(val route: String, val label: Int, val icon: KxIconKind)
    val items = buildList {
        add(Item("home", R.string.nav_home, KxIconKind.HOME))
        add(if (toolsTab) Item("tools", R.string.nav_tools, KxIconKind.GRID)
        else Item("servers", R.string.nav_servers, KxIconKind.SERVER))
        add(Item("security", R.string.nav_security, KxIconKind.SHIELD))
        add(Item("stats", R.string.nav_stats, KxIconKind.BARS))
        add(Item("settings", R.string.nav_settings, KxIconKind.GEAR))
    }
    val shape = RoundedCornerShape(28.dp)
    // Arabic nav labels are single words; at 1.6 font scale they would break mid-word in the
    // five narrow slots. Cap their physical size so each label stays on one readable line.
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    val navLabelSize = (9f / fontScale).sp
    Row(
        modifier.fillMaxWidth().clip(shape)
            .background(
                if (night) Brush.linearGradient(listOf(Color(0xFF201C50).copy(alpha = .97f), Color(0xFF171A44).copy(alpha = .97f)))
                else Brush.linearGradient(listOf(Color(0xFFFBF9FF).copy(alpha = .97f), Color(0xFFEDE8FF).copy(alpha = .97f))),
            )
            .border(1.dp, Color.White.copy(alpha = if (night) .20f else .86f), shape)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { item ->
            val active = item.route == selected
            val foreground = kxInk(night)
            Column(
                Modifier.weight(1f).heightIn(min = 48.dp)
                    .clickable(role = Role.Button, onClickLabel = stringResource(item.label)) {
                        if (!active) onNavigate(item.route)
                    }
                    .padding(vertical = 4.dp, horizontal = 2.dp)
                    .testTagCompat("nav-${item.route}"),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    Modifier.clip(RoundedCornerShape(16.dp))
                        .then(
                            if (active) Modifier.background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF8A52FF).copy(alpha = .80f), Color(0xFF6D5BF5).copy(alpha = .72f), Color(0xFF2AA9E8).copy(alpha = .70f)),
                                ),
                            ) else Modifier,
                        )
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    KxLineIcon(item.icon, foreground, Modifier.size(19.dp))
                    Text(
                        stringResource(item.label),
                        color = foreground,
                        fontSize = navLabelSize,
                        lineHeight = 11.sp,
                        maxLines = 1,
                        softWrap = false,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
fun KxOrbBadge(
    size: Dp,
    modifier: Modifier = Modifier,
    night: Boolean,
) {
    Box(
        modifier.size(size).clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(Color.White.copy(alpha = if (night) .28f else .72f), Color(0xFFB9E9FF).copy(alpha = .14f), Color.Transparent),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = if (night) .42f else .90f), CircleShape)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        KxOrb(1f, IntroTimeline.frame(1f), Modifier.fillMaxSize().padding(2.dp).clearAndSetSemantics {})
    }
}

@Composable
fun KxSectionHeaderRow(
    title: String,
    icon: KxIconKind,
    night: Boolean,
    modifier: Modifier = Modifier,
    titleModifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        KxLineIcon(icon, kxInk(night), Modifier.size(19.dp))
        Spacer(Modifier.width(7.dp))
        Text(title, color = kxInk(night), fontSize = 16.sp, lineHeight = 21.sp,
            fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).then(titleModifier))
        if (actionLabel != null && onAction != null) {
            Row(
                Modifier.clip(RoundedCornerShape(50)).clickable(role = Role.Button, onClickLabel = actionLabel, onClick = onAction)
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(actionLabel, color = kxMuted(night), fontSize = 10.sp, lineHeight = 14.sp)
                Spacer(Modifier.width(2.dp))
                KxChevron(kxMuted(night), Modifier.size(12.dp), pointsToStart = true)
            }
        }
    }
}

@Composable
fun KxChevron(color: Color, modifier: Modifier = Modifier, pointsToStart: Boolean = false) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val toStart = if (rtl) !pointsToStart else pointsToStart
    Canvas(modifier) {
        val s = size.minDimension
        val start = if (toStart) s * .64f else s * .36f
        val end = if (toStart) s * .36f else s * .64f
        drawLine(color, Offset(start, s * .22f), Offset(end, s * .5f), s * .10f, StrokeCap.Round)
        drawLine(color, Offset(end, s * .5f), Offset(start, s * .78f), s * .10f, StrokeCap.Round)
    }
}

@Composable
fun KxDivider(night: Boolean, modifier: Modifier = Modifier, vertical: Boolean = true) {
    if (vertical) {
        Box(modifier.width(1.dp).heightIn(min = 34.dp).background(Color.White.copy(alpha = if (night) .16f else .55f)))
    } else {
        Box(modifier.fillMaxWidth().heightIn(min = 1.dp).background(Color.White.copy(alpha = if (night) .12f else .50f)))
    }
}

/**
 * Reference header: the circular action sits on the physical left and the KX lockup on the
 * physical right in both directions, matching the owner-approved screens.
 */
@Composable
fun KxReferenceHeader(
    night: Boolean,
    actionLabel: String,
    tag: String,
    icon: KxIconKind,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Row(modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
        if (rtl) {
            KxBrandHeader(night)
            Spacer(Modifier.weight(1f))
            KxHeaderCircleButton(night, icon, actionLabel, tag, onClick = onClick)
        } else {
            KxHeaderCircleButton(night, icon, actionLabel, tag, onClick = onClick)
            Spacer(Modifier.weight(1f))
            KxBrandHeader(night)
        }
    }
}

@Composable
fun KxStatusPill(
    label: String,
    night: Boolean,
    connected: Boolean,
    modifier: Modifier = Modifier,
    icon: KxIconKind? = null,
) {
    val accent = when {
        connected -> kxGreen(night)
        else -> kxMuted(night)
    }
    Row(
        modifier.clip(RoundedCornerShape(50))
            .background(
                Brush.horizontalGradient(
                    if (connected) listOf(kxGreen(false).copy(alpha = if (night) .34f else .20f),
                        Color(0xFF0F6B57).copy(alpha = if (night) .44f else .16f))
                    else listOf(Color.White.copy(alpha = if (night) .10f else .48f),
                        kxMuted(night).copy(alpha = .10f)),
                ),
            )
            .border(1.dp, if (connected) kxGreen(night).copy(alpha = if (night) .88f else .70f)
            else Color.White.copy(alpha = if (night) .28f else .74f), RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        if (icon != null) KxLineIcon(icon, if (night) Color.White else Color(0xFF0A5540), Modifier.size(15.dp))
        Text(label, color = if (connected) (if (night) Color(0xFFE9FFF7) else Color(0xFF064C39))
        else kxInk(night), fontSize = 11.5.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold)
        if (connected) Box(Modifier.size(6.dp).clip(CircleShape).background(kxGreen(night)))
    }
}

@Composable
fun KxMiniOrb(night: Boolean, size: Dp = 30.dp, modifier: Modifier = Modifier) {
    KxOrbBadge(size, modifier, night)
}

@Composable
fun KxGlassIconTile(
    night: Boolean,
    icon: KxIconKind,
    accent: Color,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
) {
    Box(
        modifier.size(size).clip(RoundedCornerShape(size * .30f))
            .background(
                Brush.linearGradient(
                    listOf(accent.copy(alpha = if (night) .26f else .16f),
                        Color.White.copy(alpha = if (night) .05f else .32f)),
                ),
            )
            .border(1.dp, accent.copy(alpha = if (night) .46f else .32f), RoundedCornerShape(size * .30f)),
        contentAlignment = Alignment.Center,
    ) {
        KxLineIcon(icon, if (night) accent else accent.copy(alpha = .90f), Modifier.size(size * .48f))
    }
}

@Composable
fun KxEmptyReferenceState(text: String, night: Boolean, modifier: Modifier = Modifier) {
    Text(
        text,
        color = kxMuted(night),
        fontSize = 12.sp,
        lineHeight = 18.sp,
        modifier = modifier.fillMaxWidth().padding(vertical = 10.dp),
    )
}

enum class KxIconKind {
    HOME, SERVER, SHIELD, SHIELD_CHECK, BARS, GEAR, GRID, BACK, FORWARD, POWER, CLOCK,
    BOLT, LINK, LOCK, SPLIT, SEARCH, MIC, SPARKLE, COPY, DOWNLOAD, LAYERS, CLAPPER,
    WRENCH, WIFI, ANDROID, DRIVE_HEART, BROOM, CHECK, TAP, WAND, DATABASE, FILE,
    FOLDER, TARGET, CLOSE,
}

@Composable
fun KxLineIcon(kind: KxIconKind, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawKxIcon(kind, tint)
    }
}

private fun DrawScope.drawKxIcon(kind: KxIconKind, tint: Color) {
    val s = min(size.width, size.height)
    val c = Offset(size.width / 2f, size.height / 2f)
    val stroke = Stroke(s * .075f, cap = StrokeCap.Round)
    when (kind) {
        KxIconKind.HOME -> {
            val roof = Path().apply {
                moveTo(s * .14f, s * .46f); lineTo(c.x, s * .14f); lineTo(s * .86f, s * .46f)
            }
            drawPath(roof, tint, style = stroke)
            drawLine(tint, Offset(s * .25f, s * .44f), Offset(s * .25f, s * .85f), s * .075f, StrokeCap.Round)
            drawLine(tint, Offset(s * .75f, s * .44f), Offset(s * .75f, s * .85f), s * .075f, StrokeCap.Round)
            drawLine(tint, Offset(s * .25f, s * .85f), Offset(s * .75f, s * .85f), s * .075f, StrokeCap.Round)
        }
        KxIconKind.SERVER -> {
            for (i in 0..1) {
                val y = s * (.24f + i * .31f)
                drawRoundRect(tint, Offset(s * .16f, y), Size(s * .68f, s * .24f), CornerRadius(s * .05f), style = stroke)
                drawCircle(tint, s * .035f, Offset(s * .29f, y + s * .12f))
                drawLine(tint, Offset(s * .43f, y + s * .12f), Offset(s * .72f, y + s * .12f), s * .055f, StrokeCap.Round)
            }
        }
        KxIconKind.SHIELD, KxIconKind.SHIELD_CHECK -> {
            val shield = Path().apply {
                moveTo(c.x, s * .10f)
                cubicTo(s * .30f, s * .20f, s * .19f, s * .27f, s * .19f, s * .45f)
                cubicTo(s * .19f, s * .66f, s * .35f, s * .81f, c.x, s * .91f)
                cubicTo(s * .65f, s * .81f, s * .81f, s * .66f, s * .81f, s * .45f)
                cubicTo(s * .81f, s * .27f, s * .70f, s * .20f, c.x, s * .10f)
                close()
            }
            drawPath(shield, tint, style = stroke)
            if (kind == KxIconKind.SHIELD_CHECK) {
                drawLine(tint, Offset(s * .34f, s * .48f), Offset(s * .45f, s * .60f), s * .075f, StrokeCap.Round)
                drawLine(tint, Offset(s * .45f, s * .60f), Offset(s * .67f, s * .37f), s * .075f, StrokeCap.Round)
            }
        }
        KxIconKind.BARS -> {
            listOf(.24f, .44f, .64f, .84f).forEachIndexed { index, x ->
                val top = s * (.85f - index * .16f)
                drawLine(tint, Offset(s * x, s * .85f), Offset(s * x, top), s * .085f, StrokeCap.Round)
            }
        }
        KxIconKind.GEAR -> {
            drawCircle(tint, s * .28f, c, style = Stroke(s * .070f))
            drawCircle(tint, s * .055f, c)
            repeat(8) { i ->
                val a = Math.toRadians(i * 45.0)
                drawLine(
                    tint,
                    Offset(c.x + cos(a).toFloat() * s * .26f, c.y + sin(a).toFloat() * s * .26f),
                    Offset(c.x + cos(a).toFloat() * s * .42f, c.y + sin(a).toFloat() * s * .42f),
                    s * .075f, StrokeCap.Round,
                )
            }
        }
        KxIconKind.GRID -> {
            listOf(.20f, .54f).forEach { x ->
                listOf(.20f, .54f).forEach { y ->
                    drawRoundRect(tint, Offset(s * x, s * y), Size(s * .26f, s * .26f), CornerRadius(s * .06f), style = stroke)
                }
            }
        }
        KxIconKind.BACK, KxIconKind.FORWARD -> {
            val tipX = if (kind == KxIconKind.BACK) .38f else .62f
            val tailX = if (kind == KxIconKind.BACK) .74f else .26f
            drawLine(tint, Offset(s * tailX, s * .18f), Offset(s * tipX, s * .5f), s * .075f, StrokeCap.Round)
            drawLine(tint, Offset(s * tipX, s * .5f), Offset(s * tailX, s * .82f), s * .075f, StrokeCap.Round)
        }
        KxIconKind.POWER -> {
            drawArc(tint, startAngle = -48f, sweepAngle = 276f, useCenter = false,
                topLeft = Offset(s * .16f, s * .18f), size = Size(s * .68f, s * .68f), style = stroke)
            drawLine(tint, Offset(c.x, s * .08f), Offset(c.x, s * .46f), s * .080f, StrokeCap.Round)
        }
        KxIconKind.CLOCK -> {
            drawCircle(tint, s * .40f, c, style = stroke)
            drawLine(tint, Offset(c.x, s * .28f), Offset(c.x, s * .50f), s * .075f, StrokeCap.Round)
            drawLine(tint, Offset(c.x, s * .50f), Offset(s * .65f, s * .61f), s * .075f, StrokeCap.Round)
        }
        KxIconKind.BOLT -> {
            val bolt = Path().apply {
                moveTo(s * .56f, s * .06f)
                lineTo(s * .22f, s * .55f)
                lineTo(s * .47f, s * .55f)
                lineTo(s * .39f, s * .94f)
                lineTo(s * .78f, s * .42f)
                lineTo(s * .53f, s * .42f)
                close()
            }
            drawPath(bolt, tint, style = Stroke(s * .065f, cap = StrokeCap.Round))
        }
        KxIconKind.LINK -> {
            drawArc(tint, -60f, 300f, false, Offset(s * .11f, s * .31f), Size(s * .38f, s * .38f), style = stroke)
            drawArc(tint, 120f, 300f, false, Offset(s * .51f, s * .31f), Size(s * .38f, s * .38f), style = stroke)
            drawLine(tint, Offset(s * .40f, s * .5f), Offset(s * .60f, s * .5f), s * .065f, StrokeCap.Round)
        }
        KxIconKind.LOCK -> {
            drawRoundRect(tint, Offset(s * .18f, s * .44f), Size(s * .64f, s * .46f), CornerRadius(s * .09f), style = stroke)
            drawArc(tint, 180f, 180f, false, Offset(s * .30f, s * .14f), Size(s * .40f, s * .52f), style = stroke)
            drawCircle(tint, s * .05f, Offset(c.x, s * .62f))
            drawLine(tint, Offset(c.x, s * .66f), Offset(c.x, s * .76f), s * .055f, StrokeCap.Round)
        }
        KxIconKind.SPLIT -> {
            drawLine(tint, Offset(c.x, s * .78f), Offset(c.x, s * .46f), s * .075f, StrokeCap.Round)
            drawLine(tint, Offset(c.x, s * .46f), Offset(s * .24f, s * .16f), s * .075f, StrokeCap.Round)
            drawLine(tint, Offset(c.x, s * .46f), Offset(s * .76f, s * .16f), s * .075f, StrokeCap.Round)
            drawLine(tint, Offset(s * .24f, s * .16f), Offset(s * .38f, s * .18f), s * .075f, StrokeCap.Round)
            drawLine(tint, Offset(s * .24f, s * .16f), Offset(s * .25f, s * .31f), s * .075f, StrokeCap.Round)
            drawLine(tint, Offset(s * .76f, s * .16f), Offset(s * .62f, s * .18f), s * .075f, StrokeCap.Round)
            drawLine(tint, Offset(s * .76f, s * .16f), Offset(s * .75f, s * .31f), s * .075f, StrokeCap.Round)
        }
        KxIconKind.SEARCH -> {
            drawCircle(tint, s * .27f, Offset(s * .44f, s * .43f), style = stroke)
            drawLine(tint, Offset(s * .63f, s * .63f), Offset(s * .84f, s * .84f), s * .085f, StrokeCap.Round)
        }
        KxIconKind.MIC -> {
            drawRoundRect(tint, Offset(s * .36f, s * .10f), Size(s * .28f, s * .46f), CornerRadius(s * .14f), style = stroke)
            drawArc(tint, 0f, 180f, false, Offset(s * .26f, s * .30f), Size(s * .48f, s * .40f), style = stroke)
            drawLine(tint, Offset(c.x, s * .70f), Offset(c.x, s * .86f), s * .070f, StrokeCap.Round)
            drawLine(tint, Offset(s * .38f, s * .86f), Offset(s * .62f, s * .86f), s * .070f, StrokeCap.Round)
        }
        KxIconKind.SPARKLE -> {
            val path = Path().apply {
                moveTo(c.x, s * .08f)
                lineTo(s * .58f, s * .43f)
                lineTo(s * .92f, c.y)
                lineTo(s * .58f, s * .57f)
                lineTo(c.x, s * .92f)
                lineTo(s * .42f, s * .57f)
                lineTo(s * .08f, c.y)
                lineTo(s * .42f, s * .43f)
                close()
            }
            drawPath(path, tint)
            drawCircle(tint, s * .035f, Offset(s * .84f, s * .16f), style = Stroke(s * .030f))
        }
        KxIconKind.COPY -> {
            drawRoundRect(tint, Offset(s * .30f, s * .12f), Size(s * .50f, s * .60f), CornerRadius(s * .07f), style = stroke)
            drawRoundRect(tint, Offset(s * .16f, s * .28f), Size(s * .50f, s * .60f), CornerRadius(s * .07f), style = stroke)
        }
        KxIconKind.DOWNLOAD -> {
            drawLine(tint, Offset(c.x, s * .10f), Offset(c.x, s * .60f), s * .080f, StrokeCap.Round)
            drawLine(tint, Offset(s * .32f, s * .42f), Offset(c.x, s * .60f), s * .080f, StrokeCap.Round)
            drawLine(tint, Offset(s * .68f, s * .42f), Offset(c.x, s * .60f), s * .080f, StrokeCap.Round)
            drawLine(tint, Offset(s * .20f, s * .78f), Offset(s * .80f, s * .78f), s * .080f, StrokeCap.Round)
        }
        KxIconKind.LAYERS -> {
            repeat(3) { i ->
                val dy = s * .10f * i
                val path = Path().apply {
                    moveTo(c.x, s * .20f + dy)
                    lineTo(s * .84f, s * .38f + dy)
                    lineTo(c.x, s * .56f + dy)
                    lineTo(s * .16f, s * .38f + dy)
                    close()
                }
                drawPath(path, tint, style = stroke)
            }
        }
        KxIconKind.CLAPPER -> {
            drawRoundRect(tint, Offset(s * .14f, s * .28f), Size(s * .72f, s * .56f), CornerRadius(s * .08f), style = stroke)
            drawLine(tint, Offset(s * .14f, s * .44f), Offset(s * .86f, s * .44f), s * .065f)
            drawLine(tint, Offset(s * .26f, s * .28f), Offset(s * .34f, s * .14f), s * .06f, StrokeCap.Round)
            drawLine(tint, Offset(s * .46f, s * .28f), Offset(s * .54f, s * .14f), s * .06f, StrokeCap.Round)
            drawLine(tint, Offset(s * .66f, s * .28f), Offset(s * .74f, s * .14f), s * .06f, StrokeCap.Round)
            val play = Path().apply {
                moveTo(s * .43f, s * .55f); lineTo(s * .43f, s * .74f); lineTo(s * .61f, s * .645f); close()
            }
            drawPath(play, tint)
        }
        KxIconKind.WRENCH -> {
            rotate(-35f, c) {
                drawLine(tint, Offset(c.x, s * .28f), Offset(c.x, s * .80f), s * .10f, StrokeCap.Round)
                drawArc(tint, 20f, 320f, false, Offset(s * .24f, s * .08f), Size(s * .52f, s * .38f), style = stroke)
            }
            rotate(35f, c) {
                drawLine(tint, Offset(c.x, s * .32f), Offset(c.x, s * .82f), s * .10f, StrokeCap.Round)
                drawArc(tint, 20f, 320f, false, Offset(s * .24f, s * .12f), Size(s * .52f, s * .38f), style = stroke)
            }
        }
        KxIconKind.WIFI -> {
            drawArc(tint, 205f, 130f, false, Offset(s * .08f, s * .18f), Size(s * .84f, s * .84f), style = stroke)
            drawArc(tint, 205f, 130f, false, Offset(s * .22f, s * .34f), Size(s * .56f, s * .56f), style = stroke)
            drawArc(tint, 205f, 130f, false, Offset(s * .36f, s * .50f), Size(s * .28f, s * .28f), style = stroke)
            drawCircle(tint, s * .05f, Offset(c.x, s * .80f))
        }
        KxIconKind.ANDROID -> {
            drawArc(tint, 180f, 180f, false, Offset(s * .18f, s * .28f), Size(s * .64f, s * .56f), style = stroke)
            drawLine(tint, Offset(s * .18f, s * .56f), Offset(s * .18f, s * .82f), s * .070f, StrokeCap.Round)
            drawLine(tint, Offset(s * .82f, s * .56f), Offset(s * .82f, s * .82f), s * .070f, StrokeCap.Round)
            drawLine(tint, Offset(s * .18f, s * .82f), Offset(s * .82f, s * .82f), s * .070f, StrokeCap.Round)
            drawLine(tint, Offset(s * .33f, s * .30f), Offset(s * .27f, s * .18f), s * .055f, StrokeCap.Round)
            drawLine(tint, Offset(s * .67f, s * .30f), Offset(s * .73f, s * .18f), s * .055f, StrokeCap.Round)
            drawCircle(tint, s * .025f, Offset(s * .36f, s * .45f))
            drawCircle(tint, s * .025f, Offset(s * .64f, s * .45f))
        }
        KxIconKind.DRIVE_HEART -> {
            drawOval(tint, Offset(s * .16f, s * .18f), Size(s * .68f, s * .22f), style = stroke)
            drawLine(tint, Offset(s * .16f, s * .29f), Offset(s * .16f, s * .64f), s * .065f)
            drawLine(tint, Offset(s * .84f, s * .29f), Offset(s * .84f, s * .50f), s * .065f)
            drawArc(tint, 0f, 180f, false, Offset(s * .16f, s * .53f), Size(s * .68f, s * .22f), style = stroke)
            val heart = Path().apply {
                moveTo(s * .52f, s * .88f)
                cubicTo(s * .40f, s * .79f, s * .40f, s * .68f, s * .49f, s * .68f)
                cubicTo(s * .53f, s * .68f, s * .55f, s * .71f, s * .56f, s * .73f)
                cubicTo(s * .58f, s * .70f, s * .60f, s * .68f, s * .64f, s * .68f)
                cubicTo(s * .73f, s * .68f, s * .73f, s * .79f, s * .61f, s * .88f)
                lineTo(s * .56f, s * .92f)
                close()
            }
            drawPath(heart, tint, style = Stroke(s * .040f, cap = StrokeCap.Round))
        }
        KxIconKind.BROOM -> {
            drawLine(tint, Offset(s * .70f, s * .12f), Offset(s * .48f, s * .52f), s * .080f, StrokeCap.Round)
            drawLine(tint, Offset(s * .30f, s * .52f), Offset(s * .70f, s * .52f), s * .080f, StrokeCap.Round)
            for (i in 0..3) {
                val x = s * (.34f + i * .10f)
                drawLine(tint, Offset(x, s * .56f), Offset(x - s * .05f, s * .86f), s * .055f, StrokeCap.Round)
            }
        }
        KxIconKind.CHECK -> {
            drawLine(tint, Offset(s * .16f, s * .52f), Offset(s * .42f, s * .78f), s * .090f, StrokeCap.Round)
            drawLine(tint, Offset(s * .42f, s * .78f), Offset(s * .86f, s * .22f), s * .090f, StrokeCap.Round)
        }
        KxIconKind.CLOSE -> {
            drawLine(tint, Offset(s * .22f, s * .22f), Offset(s * .78f, s * .78f), s * .085f, StrokeCap.Round)
            drawLine(tint, Offset(s * .78f, s * .22f), Offset(s * .22f, s * .78f), s * .085f, StrokeCap.Round)
        }
        KxIconKind.TAP -> {
            drawCircle(tint, s * .15f, Offset(s * .40f, s * .40f), style = Stroke(s * .060f))
            drawLine(tint, Offset(s * .40f, s * .55f), Offset(s * .40f, s * .82f), s * .100f, StrokeCap.Round)
            drawLine(tint, Offset(s * .30f, s * .80f), Offset(s * .40f, s * .90f), s * .070f, StrokeCap.Round)
            drawLine(tint, Offset(s * .50f, s * .80f), Offset(s * .40f, s * .90f), s * .070f, StrokeCap.Round)
            drawArc(tint, 200f, 120f, false, Offset(s * .52f, s * .18f), Size(s * .34f, s * .34f), style = Stroke(s * .055f))
        }
        KxIconKind.WAND -> {
            drawLine(tint, Offset(s * .22f, s * .82f), Offset(s * .72f, s * .28f), s * .085f, StrokeCap.Round)
            drawLine(tint, Offset(s * .72f, s * .28f), Offset(s * .80f, s * .18f), s * .085f, StrokeCap.Round)
            listOf(
                Offset(s * .82f, s * .46f) to s * .08f,
                Offset(s * .58f, s * .12f) to s * .07f,
                Offset(s * .88f, s * .70f) to s * .05f,
            ).forEach { (p, r) ->
                val star = Path().apply {
                    moveTo(p.x, p.y - r); lineTo(p.x + r * .22f, p.y - r * .22f)
                    lineTo(p.x + r, p.y); lineTo(p.x + r * .22f, p.y + r * .22f)
                    lineTo(p.x, p.y + r); lineTo(p.x - r * .22f, p.y + r * .22f)
                    lineTo(p.x - r, p.y); lineTo(p.x - r * .22f, p.y - r * .22f); close()
                }
                drawPath(star, tint)
            }
        }
        KxIconKind.DATABASE -> {
            drawOval(tint, Offset(s * .18f, s * .14f), Size(s * .64f, s * .22f), style = stroke)
            drawLine(tint, Offset(s * .18f, s * .25f), Offset(s * .18f, s * .75f), s * .065f)
            drawLine(tint, Offset(s * .82f, s * .25f), Offset(s * .82f, s * .75f), s * .065f)
            drawArc(tint, 0f, 180f, false, Offset(s * .18f, s * .64f), Size(s * .64f, s * .22f), style = stroke)
            drawArc(tint, 0f, 180f, false, Offset(s * .18f, s * .43f), Size(s * .64f, s * .22f), style = stroke)
        }
        KxIconKind.FILE -> {
            val page = Path().apply {
                moveTo(s * .24f, s * .10f); lineTo(s * .60f, s * .10f); lineTo(s * .78f, s * .28f)
                lineTo(s * .78f, s * .90f); lineTo(s * .24f, s * .90f); close()
            }
            drawPath(page, tint, style = stroke)
            drawLine(tint, Offset(s * .60f, s * .10f), Offset(s * .60f, s * .28f), s * .055f)
            drawLine(tint, Offset(s * .60f, s * .28f), Offset(s * .78f, s * .28f), s * .055f)
        }
        KxIconKind.FOLDER -> {
            val folder = Path().apply {
                moveTo(s * .12f, s * .28f); lineTo(s * .40f, s * .28f); lineTo(s * .48f, s * .39f)
                lineTo(s * .88f, s * .39f); lineTo(s * .88f, s * .82f); lineTo(s * .12f, s * .82f)
                close()
            }
            drawPath(folder, tint, style = stroke)
            drawLine(tint, Offset(s * .12f, s * .39f), Offset(s * .88f, s * .39f),
                s * .055f, StrokeCap.Round)
        }
        KxIconKind.TARGET -> {
            drawCircle(tint, s * .30f, c, style = stroke)
            drawCircle(tint, s * .10f, c, style = Stroke(s * .060f))
            drawLine(tint, Offset(c.x, s * .04f), Offset(c.x, s * .18f), s * .055f, StrokeCap.Round)
            drawLine(tint, Offset(c.x, s * .82f), Offset(c.x, s * .96f), s * .055f, StrokeCap.Round)
            drawLine(tint, Offset(s * .04f, c.y), Offset(s * .18f, c.y), s * .055f, StrokeCap.Round)
            drawLine(tint, Offset(s * .82f, c.y), Offset(s * .96f, c.y), s * .055f, StrokeCap.Round)
        }
    }
}

private fun Modifier.testTagCompat(tag: String): Modifier = this.testTag(tag)
