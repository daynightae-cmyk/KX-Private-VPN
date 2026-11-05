package com.knoux.kxprivatevpn.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.knoux.kxprivatevpn.platform.repair.RepairServiceId
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val Pearl = Color(0xFFF9ECFF)
private val Cyan = Color(0xFF64EEFF)
private val Violet = Color(0xFFAD71FF)
private val Rose = Color(0xFFFF8AD8)
private val Ice = Color(0xFFB9E9FF)

/** Each service keeps the KX palette but gets a distinct accent pair and silhouette. */
fun repairAccentPair(id: RepairServiceId): Pair<Color, Color> = when (id) {
    RepairServiceId.STORAGE -> Cyan to Violet
    RepairServiceId.GHOST -> Violet to Rose
    RepairServiceId.DUPLICATE -> Ice to Violet
    RepairServiceId.HEAVYWEIGHT -> Rose to Cyan
    RepairServiceId.CACHE -> Cyan to Color(0xFF6F8DFF)
    RepairServiceId.MEDIA -> Color(0xFFFFC2F0) to Cyan
    RepairServiceId.DOWNLOAD -> Ice to Color(0xFF4F7DFF)
    RepairServiceId.SESSION -> Violet to Ice
    RepairServiceId.NETWORK -> Cyan to Color(0xFF7C6BFF)
    RepairServiceId.AUTOPILOT -> Rose to Violet
}

fun repairAccent(id: RepairServiceId): Color = repairAccentPair(id).first

/**
 * Original 3D-style vector glyphs for the ten signature services. They are native Canvas
 * drawings, not exported images and not a screenshot of the reference artwork.
 */
@Composable
fun RepairGlyph(id: RepairServiceId, modifier: Modifier = Modifier) {
    val accent = repairAccentPair(id)
    Canvas(modifier) {
        val u = min(size.width, size.height)
        val c = Offset(size.width / 2f, size.height / 2f)
        val glow = Brush.radialGradient(
            listOf(accent.first.copy(alpha = .42f), accent.second.copy(alpha = .18f), Color.Transparent),
            c, u * .52f,
        )
        drawCircle(glow, u * .52f, c)
        drawCircle(
            Brush.linearGradient(listOf(Color.White.copy(alpha = .26f),
                accent.first.copy(alpha = .16f), accent.second.copy(alpha = .22f)),
                Offset(c.x - u * .3f, c.y - u * .3f), Offset(c.x + u * .3f, c.y + u * .3f)),
            u * .34f, c,
        )
        drawCircle(Color.White.copy(alpha = .34f), u * .34f, c, style = Stroke(u * .012f))
        val stroke = Stroke(u * .022f, cap = StrokeCap.Round)
        val strokeSoft = Stroke(u * .016f, cap = StrokeCap.Round)
        val ink = Brush.linearGradient(listOf(Pearl, accent.first, accent.second, Pearl),
            Offset(c.x - u * .3f, c.y - u * .3f), Offset(c.x + u * .3f, c.y + u * .3f))

        when (id) {
            RepairServiceId.STORAGE -> drawStorage(c, u, ink, stroke)
            RepairServiceId.GHOST -> drawGhost(c, u, ink, strokeSoft, accent.first)
            RepairServiceId.DUPLICATE -> drawDuplicate(c, u, ink, strokeSoft, accent)
            RepairServiceId.HEAVYWEIGHT -> drawHeavyweight(c, u, ink, accent)
            RepairServiceId.CACHE -> drawCache(c, u, ink, stroke, strokeSoft, accent)
            RepairServiceId.MEDIA -> drawMedia(c, u, ink, strokeSoft, accent)
            RepairServiceId.DOWNLOAD -> drawDownload(c, u, ink, stroke, strokeSoft, accent)
            RepairServiceId.SESSION -> drawSession(c, u, ink, stroke, accent)
            RepairServiceId.NETWORK -> drawNetwork(c, u, ink, strokeSoft, accent)
            RepairServiceId.AUTOPILOT -> drawAutopilot(c, u, ink, accent)
        }
    }
}

private fun DrawScope.drawStorage(c: Offset, u: Float, ink: Brush, stroke: Stroke) {
    for (i in 0..2) {
        val top = Offset(c.x - u * .22f, c.y - u * .18f + i * u * .10f)
        drawOval(ink, top, Size(u * .44f, u * .16f), style = stroke)
        drawOval(Brush.linearGradient(listOf(Color.White.copy(alpha = .08f),
            Violet.copy(alpha = .22f))), top, Size(u * .44f, u * .16f))
    }
    drawCircle(Brush.radialGradient(listOf(Pearl, Cyan, Violet.copy(alpha = .2f)),
        c - Offset(u * .03f, u * .04f), u * .10f), u * .085f, c - Offset(0f, u * .11f))
}

private fun DrawScope.drawGhost(c: Offset, u: Float, ink: Brush, stroke: Stroke, accent: Color) {
    val path = Path().apply {
        moveTo(c.x - u * .19f, c.y - u * .08f)
        cubicTo(c.x - u * .19f, c.y - u * .31f, c.x + u * .19f, c.y - u * .31f,
            c.x + u * .19f, c.y - u * .08f)
        lineTo(c.x + u * .19f, c.y + u * .21f)
        lineTo(c.x + u * .11f, c.y + u * .14f)
        lineTo(c.x + u * .04f, c.y + u * .22f)
        lineTo(c.x - u * .04f, c.y + u * .14f)
        lineTo(c.x - u * .11f, c.y + u * .22f)
        lineTo(c.x - u * .19f, c.y + u * .14f)
        close()
    }
    drawPath(path, ink, style = stroke)
    drawCircle(Pearl, u * .022f, Offset(c.x - u * .07f, c.y - u * .10f))
    drawCircle(Pearl, u * .022f, Offset(c.x + u * .07f, c.y - u * .10f))
    for (i in 0..2) {
        drawArc(accent.copy(alpha = .55f - i * .12f), startAngle = -42f, sweepAngle = 84f,
            useCenter = false, topLeft = Offset(c.x + u * .16f + i * u * .045f, c.y - u * .16f),
            size = Size(u * (.22f + i * .05f), u * (.40f + i * .06f)), style = Stroke(u * .014f))
    }
}

private fun DrawScope.drawDuplicate(c: Offset, u: Float, ink: Brush, stroke: Stroke,
                                   accent: Pair<Color, Color>) {
    val left = hexPath(Offset(c.x - u * .13f, c.y - u * .04f), u * .17f)
    val right = hexPath(Offset(c.x + u * .13f, c.y + u * .04f), u * .17f)
    drawPath(left, Brush.linearGradient(listOf(Color.White.copy(alpha = .18f),
        accent.first.copy(alpha = .5f))), style = stroke)
    drawPath(right, Brush.linearGradient(listOf(accent.second.copy(alpha = .5f),
        Color.White.copy(alpha = .18f))), style = stroke)
    drawLine(Pearl.copy(alpha = .8f), Offset(c.x - u * .02f, c.y - u * .02f),
        Offset(c.x + u * .03f, c.y + u * .03f), u * .018f, StrokeCap.Round)
    drawCircle(Pearl, u * .022f, Offset(c.x - u * .13f, c.y - u * .04f))
    drawCircle(accent.first, u * .022f, Offset(c.x + u * .13f, c.y + u * .04f))
}

private fun hexPath(center: Offset, radius: Float): Path {
    val path = Path()
    for (i in 0 until 6) {
        val angle = Math.toRadians(60.0 * i - 30.0)
        val point = Offset(center.x + cos(angle).toFloat() * radius,
            center.y + sin(angle).toFloat() * radius)
        if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
    }
    path.close()
    return path
}

private fun DrawScope.drawHeavyweight(c: Offset, u: Float, ink: Brush, accent: Pair<Color, Color>) {
    drawCircle(Brush.radialGradient(listOf(Pearl.copy(alpha = .6f),
        accent.first.copy(alpha = .35f), Color.Transparent), c - Offset(u * .04f, u * .08f), u * .26f),
        u * .22f, c - Offset(0f, u * .07f))
    drawCircle(accent.second.copy(alpha = .8f), u * .22f, c - Offset(0f, u * .07f),
        style = Stroke(u * .016f))
    drawArc(ink, startAngle = 155f, sweepAngle = 230f, useCenter = false,
        topLeft = Offset(c.x - u * .30f, c.y - u * .26f), size = Size(u * .60f, u * .44f),
        style = Stroke(u * .014f))
    for (i in 0..2) {
        val width = u * (.30f - i * .04f)
        drawRoundRect(Brush.linearGradient(listOf(accent.first.copy(alpha = .85f),
            accent.second.copy(alpha = .6f))),
            topLeft = Offset(c.x - width / 2f, c.y + u * .10f + i * u * .055f),
            size = Size(width, u * .038f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(u * .019f))
    }
}

private fun DrawScope.drawCache(c: Offset, u: Float, ink: Brush, stroke: Stroke,
                                soft: Stroke, accent: Pair<Color, Color>) {
    val crystal = Path().apply {
        moveTo(c.x, c.y - u * .20f)
        lineTo(c.x + u * .14f, c.y - u * .04f)
        lineTo(c.x, c.y + u * .20f)
        lineTo(c.x - u * .14f, c.y - u * .04f)
        close()
    }
    drawPath(crystal, Brush.linearGradient(listOf(Color.White.copy(alpha = .22f),
        accent.first.copy(alpha = .45f), accent.second.copy(alpha = .35f)),
        Offset(c.x - u * .2f, c.y - u * .2f), Offset(c.x + u * .2f, c.y + u * .2f)))
    drawPath(crystal, ink, style = soft)
    drawLine(Color.White.copy(alpha = .5f), Offset(c.x, c.y - u * .18f),
        Offset(c.x, c.y + u * .17f), u * .010f)
    drawLine(Color.White.copy(alpha = .32f), Offset(c.x - u * .12f, c.y - u * .04f),
        Offset(c.x + u * .12f, c.y - u * .04f), u * .008f)
    drawArc(ink, startAngle = 208f, sweepAngle = 262f, useCenter = false,
        topLeft = Offset(c.x - u * .30f, c.y - u * .30f), size = Size(u * .60f, u * .60f),
        style = Stroke(u * .022f, cap = StrokeCap.Round))
    val handle = Offset(c.x + u * .24f, c.y + u * .20f)
    drawLine(Pearl, handle, Offset(c.x + u * .32f, c.y + u * .30f), u * .026f, StrokeCap.Round)
}

private fun DrawScope.drawMedia(c: Offset, u: Float, ink: Brush, stroke: Stroke,
                                accent: Pair<Color, Color>) {
    val prism = Path().apply {
        moveTo(c.x, c.y - u * .24f)
        lineTo(c.x + u * .24f, c.y + u * .12f)
        lineTo(c.x - u * .24f, c.y + u * .12f)
        close()
    }
    drawPath(prism, Brush.linearGradient(listOf(Color.White.copy(alpha = .30f),
        accent.first.copy(alpha = .30f), accent.second.copy(alpha = .30f))))
    drawPath(prism, ink, style = stroke)
    val ribbon = Path().apply {
        moveTo(c.x - u * .30f, c.y + u * .19f)
        cubicTo(c.x - u * .10f, c.y + u * .03f, c.x + u * .10f, c.y + u * .33f,
            c.x + u * .30f, c.y + u * .15f)
    }
    drawPath(ribbon, Brush.linearGradient(listOf(accent.first, Pearl, accent.second)),
        style = Stroke(u * .030f, cap = StrokeCap.Round))
    drawCircle(Pearl, u * .024f, Offset(c.x - u * .06f, c.y - u * .04f))
}

private fun DrawScope.drawDownload(c: Offset, u: Float, ink: Brush, stroke: Stroke,
                                   soft: Stroke, accent: Pair<Color, Color>) {
    drawArc(Brush.linearGradient(listOf(accent.first, accent.second)),
        startAngle = 120f, sweepAngle = 300f, useCenter = false,
        topLeft = Offset(c.x - u * .29f, c.y - u * .29f), size = Size(u * .58f, u * .58f),
        style = stroke)
    drawLine(Pearl, Offset(c.x, c.y - u * .18f), Offset(c.x, c.y + u * .10f),
        u * .028f, StrokeCap.Round)
    drawLine(Pearl, Offset(c.x - u * .11f, c.y), Offset(c.x, c.y + u * .11f),
        u * .028f, StrokeCap.Round)
    drawLine(Pearl, Offset(c.x + u * .11f, c.y), Offset(c.x, c.y + u * .11f),
        u * .028f, StrokeCap.Round)
    drawLine(accent.first.copy(alpha = .9f), Offset(c.x - u * .15f, c.y + u * .21f),
        Offset(c.x + u * .15f, c.y + u * .21f), u * .022f, StrokeCap.Round)
    drawArc(ink, startAngle = 125f, sweepAngle = 290f, useCenter = false,
        topLeft = Offset(c.x - u * .37f, c.y - u * .37f), size = Size(u * .74f, u * .74f),
        style = soft)
}

private fun DrawScope.drawSession(c: Offset, u: Float, ink: Brush, stroke: Stroke,
                                  accent: Pair<Color, Color>) {
    val shield = Path().apply {
        moveTo(c.x, c.y - u * .24f)
        cubicTo(c.x - u * .06f, c.y - u * .20f, c.x - u * .15f, c.y - u * .21f,
            c.x - u * .19f, c.y - u * .19f)
        cubicTo(c.x - u * .20f, c.y - u * .02f, c.x - u * .13f, c.y + u * .14f,
            c.x, c.y + u * .24f)
        cubicTo(c.x + u * .13f, c.y + u * .14f, c.x + u * .20f, c.y - u * .02f,
            c.x + u * .19f, c.y - u * .19f)
        cubicTo(c.x + u * .15f, c.y - u * .21f, c.x + u * .06f, c.y - u * .20f,
            c.x, c.y - u * .24f)
        close()
    }
    drawPath(shield, Brush.linearGradient(listOf(Color.White.copy(alpha = .22f),
        accent.first.copy(alpha = .40f), accent.second.copy(alpha = .30f))))
    drawPath(shield, ink, style = stroke)
    for (i in 0..2) {
        val angle = Math.toRadians(210.0 + i * 120.0)
        drawCircle(if (i == 0) Pearl else accent.first, u * .021f,
            Offset(c.x + cos(angle).toFloat() * u * .28f,
                c.y + sin(angle).toFloat() * u * .19f))
    }
}

private fun DrawScope.drawNetwork(c: Offset, u: Float, ink: Brush, stroke: Stroke,
                                  accent: Pair<Color, Color>) {
    drawCircle(ink, u * .24f, c, style = stroke)
    drawOval(ink, Offset(c.x - u * .10f, c.y - u * .24f), Size(u * .20f, u * .48f), style = stroke)
    drawLine(accent.first.copy(alpha = .9f), Offset(c.x - u * .23f, c.y),
        Offset(c.x + u * .23f, c.y), u * .014f)
    drawArc(accent.second.copy(alpha = .8f), 200f, 140f, false,
        Offset(c.x - u * .23f, c.y - u * .17f), Size(u * .46f, u * .22f), style = Stroke(u * .014f))
    val wave = Path().apply {
        moveTo(c.x - u * .30f, c.y + u * .26f)
        cubicTo(c.x - u * .16f, c.y + u * .17f, c.x - u * .05f, c.y + u * .34f,
            c.x + u * .08f, c.y + u * .26f)
        cubicTo(c.x + u * .18f, c.y + u * .20f, c.x + u * .24f, c.y + u * .29f,
            c.x + u * .31f, c.y + u * .25f)
    }
    drawPath(wave, Brush.linearGradient(listOf(accent.first, Pearl, accent.second)),
        style = Stroke(u * .026f, cap = StrokeCap.Round))
}

private fun DrawScope.drawAutopilot(c: Offset, u: Float, ink: Brush, accent: Pair<Color, Color>) {
    drawCircle(Brush.radialGradient(listOf(Pearl, accent.first, accent.second),
        c - Offset(u * .05f, u * .06f), u * .24f), u * .16f, c)
    drawOval(ink, Offset(c.x - u * .30f, c.y - u * .13f), Size(u * .60f, u * .26f),
        style = Stroke(u * .014f))
    drawOval(Brush.linearGradient(listOf(accent.second.copy(alpha = .5f),
        Color.Transparent)), Offset(c.x - u * .30f, c.y - u * .17f),
        Size(u * .60f, u * .34f), style = Stroke(u * .010f))
    for (i in 0..4) {
        val angle = Math.toRadians(25.0 + i * 72.0)
        val x = c.x + cos(angle).toFloat() * u * .29f
        val y = c.y + sin(angle).toFloat() * u * .125f
        drawCircle(if (i % 2 == 0) Cyan else Rose, u * .019f, Offset(x, y))
    }
    rotate(-18f, c) {
        drawArc(ink, 200f, 120f, false, Offset(c.x - u * .26f, c.y - u * .24f),
            Size(u * .52f, u * .48f), style = Stroke(u * .012f))
    }
}
