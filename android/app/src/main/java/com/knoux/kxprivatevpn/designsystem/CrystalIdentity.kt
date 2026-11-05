package com.knoux.kxprivatevpn.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knoux.kxprivatevpn.R
import com.knoux.kxprivatevpn.launch.IntroFrame
import kotlin.math.*

private val Cyan = Color(0xFF64EEFF)
private val Violet = Color(0xFFAD71FF)
private val Pearl = Color(0xFFF9ECFF)

@Composable
fun Atmosphere(night: Boolean, pulse: Float) {
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(Brush.linearGradient(if (night) listOf(Color(0xFF06091F), Color(0xFF19134B),
            Color(0xFF112453)) else listOf(Color(0xFFC8C1FF), Color(0xFFA4C5FF), Color(0xFFE6D9FF)),
            Offset.Zero, Offset(w, h)))
        drawCircle(Brush.radialGradient(listOf(Violet.copy(alpha = .22f), Color.Transparent),
            Offset(w * .5f, h * .31f), w * .8f), w * .8f, Offset(w * .5f, h * .31f))
        // Bounded native translucent ribbons; no full-screen per-frame blur or bitmap background.
        for (i in 0..3) {
            val y = h * (.70f + i * .095f)
            val path = Path().apply {
                moveTo(-w * .1f, y)
                cubicTo(w * .25f, y + h * .23f, w * .62f, y - h * .19f, w * 1.12f, y - h * .06f)
                lineTo(w * 1.12f, y + h * .09f)
                cubicTo(w * .58f, y - h * .1f, w * .25f, y + h * .30f, -w * .1f, y + h * .06f)
                close()
            }
            drawPath(path, Brush.linearGradient(listOf(Cyan.copy(alpha = .07f),
                Violet.copy(alpha = .34f), Pearl.copy(alpha = .18f), Cyan.copy(alpha = .26f)),
                Offset(0f, y), Offset(w, y + h * .1f)))
            drawPath(path, Brush.linearGradient(listOf(Color.Transparent, Pearl.copy(alpha = .7f),
                Cyan.copy(alpha = .6f), Color.Transparent)), style = Stroke(.8.dp.toPx()))
        }
        // Subtle corner veil, faithful to the Welcome reference's diagonal upper light.
        val veil = Path().apply {
            moveTo(0f, 0f); lineTo(w * .48f, 0f)
            cubicTo(w * .31f, h * .13f, w * .12f, h * .14f, 0f, h * .18f); close()
        }
        drawPath(veil, Brush.linearGradient(listOf(Violet.copy(alpha = .45f), Color.Transparent)))
        val pulseAlpha = pulse * .12f
        if (pulseAlpha > 0f) drawRect(Brush.radialGradient(listOf(Pearl.copy(alpha = pulseAlpha),
            Color.Transparent), Offset(w * .5f, h * .31f), w * .9f))
    }
}

@Composable
fun KxOrb(progress: Float, frame: IntroFrame, modifier: Modifier = Modifier) {
    val artwork = ImageBitmap.imageResource(R.drawable.kx_brand)
    val description = stringResource(R.string.brand_description)
    Canvas(modifier.semantics { contentDescription = description }) {
        val unit = size.minDimension / 1000f
        scale(unit, unit, Offset.Zero) {
            val center = Offset(500f, 500f)
            val r = 407f
            // Atmospheric halo, layered glass thickness, and cool near/far light planes.
            drawCircle(Brush.radialGradient(listOf(Cyan.copy(alpha = .06f),
                Violet.copy(alpha = .2f), Color.Transparent), center, 500f), 500f, center)
            drawCircle(Brush.radialGradient(listOf(Pearl.copy(alpha = .30f * frame.ribbons),
                Color(0xFF4A4CD5).copy(alpha = .25f), Color(0xFF8C78FE).copy(alpha = .55f)),
                Offset(410f, 330f), 600f), r, center)
            for (i in 0..4) {
                drawCircle(Brush.sweepGradient(listOf(Cyan.copy(alpha = .05f), Pearl,
                    Violet.copy(alpha = .7f), Cyan, Pearl.copy(alpha = .9f), Cyan.copy(alpha = .05f)),
                    center), r - i * 4f, center, style = Stroke(if (i == 0) 3.5f else 1.5f),
                    alpha = .25f + frame.ribbons * .13f)
            }
            val orbit = Path().apply {
                moveTo(66f, 484f)
                cubicTo(88f, 255f, 911f, 278f, 936f, 480f)
                cubicTo(960f, 691f, 91f, 728f, 66f, 484f)
            }
            rotate(-17f, center) {
                drawPath(orbit, Brush.linearGradient(listOf(Cyan.copy(alpha = .2f), Pearl,
                    Violet.copy(alpha = .6f))), style = Stroke(1.7f), alpha = frame.convergence)
            }
            // Light photons follow deterministic parallax trajectories into the sphere.
            if (progress < .65f) repeat(48) { i ->
                val angle = i * 2.399963f + progress * .3f
                val start = 530f + (i % 7) * 17f
                val distance = start + (r * (.65f + (i % 5) * .05f) - start) * frame.convergence
                val p = center + Offset(cos(angle) * distance, sin(angle) * distance * .90f)
                val c = if (i % 2 == 0) Cyan else Violet
                drawCircle(Brush.radialGradient(listOf(c.copy(alpha = .8f), Color.Transparent), p, 9f), 9f, p,
                    alpha = (1f - frame.artwork) * .6f)
                drawCircle(Pearl, 1f + (i % 3) * .5f, p, alpha = 1f - frame.artwork)
            }
            // Reference shield contour, woven ribbons with pearlescent facing and shaded reverse.
            val shield = Path().apply {
                moveTo(500f, 177f)
                cubicTo(445f, 238f, 365f, 225f, 305f, 248f)
                cubicTo(292f, 425f, 334f, 533f, 500f, 624f)
                cubicTo(666f, 533f, 708f, 425f, 695f, 248f)
                cubicTo(635f, 225f, 555f, 238f, 500f, 177f); close()
            }
            val nativeAlpha = frame.ribbons * (1f - frame.artwork)
            if (nativeAlpha > 0f) {
                // Measured against the cropped source: apex ~182, base ~573, width ~350/1000.
                scale(.90f, .88f, Offset(500f, 177f)) {
                    drawPath(shield, Brush.linearGradient(listOf(Color(0xFFDD91FF), Color(0xFF3412BB),
                        Color(0xFF0059D0), Cyan), Offset(300f, 250f), Offset(700f, 540f)), alpha = nativeAlpha)
                    drawPath(shield, Brush.linearGradient(listOf(Pearl, Cyan, Violet, Pearl)),
                        style = Stroke(3f), alpha = nativeAlpha)
                    clipPath(shield) {
                        for (i in 0..3) {
                            val y = 238f + i * 96f
                            val shift = (1f - frame.ribbons) * (if (i % 2 == 0) -280f else 280f)
                            translate(shift, 0f) {
                                val ribbon = Path().apply {
                                    if (i % 2 == 0) {
                                        moveTo(290f, y); cubicTo(310f, y + 80f, 625f, y + 96f, 710f, y + 230f)
                                        lineTo(710f, y + 165f)
                                        cubicTo(590f, y + 50f, 335f, y + 24f, 290f, y - 35f)
                                    } else {
                                        moveTo(710f, y - 80f); cubicTo(670f, y, 360f, y + 75f, 300f, y + 180f)
                                        lineTo(300f, y + 106f)
                                        cubicTo(355f, y + 25f, 640f, y - 42f, 710f, y - 119f)
                                    }; close()
                                }
                                drawPath(ribbon, Brush.linearGradient(listOf(Pearl, Cyan.copy(alpha = .95f),
                                    Color(0xFF7D5FE9), Color(0xFFF2C4FF), Pearl), Offset(295f, y), Offset(710f, y + 160f)),
                                    alpha = nativeAlpha)
                                drawPath(ribbon, Pearl.copy(alpha = .8f), style = Stroke(1.6f), alpha = nativeAlpha)
                            }
                        }
                    }
                }
                rotate(24f, Offset(630f, 178f)) {
                    drawOval(Brush.linearGradient(listOf(Pearl, Cyan.copy(alpha = .4f), Color.Transparent)),
                        Offset(545f, 140f), Size(240f, 56f), alpha = nativeAlpha * .75f)
                }
            }
            // Reveal original, byte-preserved owner artwork after the native formation.
            // Only the orb is drawn, never a screenshot-as-screen. The outside black margin is clipped.
            if (frame.artwork > 0f) {
                // Inset inside the source orb's irregular black silhouette; the native glass rim
                // supplies the outer edge, preventing black fringes against the day atmosphere.
                val crop = Path().apply { addOval(androidx.compose.ui.geometry.Rect(101f, 101f, 899f, 899f)) }
                clipPath(crop) {
                    drawImage(artwork, srcOffset = IntOffset(80, 88), srcSize = IntSize(1085, 1085),
                        dstOffset = IntOffset(88, 88), dstSize = IntSize(824, 824), alpha = frame.artwork,
                        filterQuality = FilterQuality.Medium)
                }
            }
            // Native satellites, soft caustic sweep and one final halo pulse remain above the artwork.
            for (i in 0..2) {
                val angle = i * 2.2f + progress * .55f
                val p = center + Offset(cos(angle) * 445f, sin(angle) * 230f)
                val c = if (i == 1) Violet else Cyan
                drawCircle(Brush.radialGradient(listOf(Pearl, c, c.copy(alpha = .1f)),
                    p - Offset(5f, 7f), 19f), 15f, p, alpha = frame.convergence)
            }
            if (progress in .42f.. .86f) {
                val x = 100f + ((progress - .42f) / .44f) * 800f
                val mask = Path().apply { addOval(androidx.compose.ui.geometry.Rect(94f, 94f, 906f, 906f)) }
                clipPath(mask) {
                    drawRect(Brush.linearGradient(listOf(Color.Transparent, Pearl.copy(alpha = .20f),
                        Cyan.copy(alpha = .12f), Color.Transparent), Offset(x - 95f, 120f), Offset(x + 95f, 800f)))
                }
            }
            if (frame.pulse > 0f) drawCircle(Cyan, r + 5f + frame.pulse * 24f,
                center, style = Stroke(2f), alpha = frame.pulse * .55f)
            star(Offset(790f, 172f), 18f, frame.ribbons)
            star(Offset(192f, 640f), 13f, frame.ribbons)
        }
    }
}

private fun DrawScope.star(p: Offset, radius: Float, alpha: Float) {
    drawCircle(Brush.radialGradient(listOf(Pearl.copy(alpha = .7f), Color.Transparent), p, radius * 2),
        radius * 2, p, alpha = alpha)
    val path = Path().apply {
        moveTo(p.x, p.y - radius)
        quadraticTo(p.x + 2f, p.y - 2f, p.x + radius * .7f, p.y)
        quadraticTo(p.x + 2f, p.y + 2f, p.x, p.y + radius)
        quadraticTo(p.x - 2f, p.y + 2f, p.x - radius * .7f, p.y)
        quadraticTo(p.x - 2f, p.y - 2f, p.x, p.y - radius); close()
    }
    drawPath(path, Pearl, alpha = alpha)
}

@Composable
fun GlassAction(label: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(40.dp)
    // The stops are deliberately deep: white label text needs >=3:1 against the
    // lightest end of the fill even in day theme, where the page behind is pale.
    Box(modifier.fillMaxWidth().heightIn(min = 58.dp).clip(shape).alpha(if (enabled) 1f else .5f)
        .background(Brush.linearGradient(listOf(Color(0xFF8A4BFF), Color(0xFF4930D6), Color(0xFF1580C8))))
        .border(1.5.dp, Brush.linearGradient(listOf(Pearl, Cyan, Violet, Pearl)), shape)
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center) {
        Text(label, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
    }
}
