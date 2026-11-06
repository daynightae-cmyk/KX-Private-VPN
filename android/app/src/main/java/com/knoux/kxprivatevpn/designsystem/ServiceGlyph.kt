package com.knoux.kxprivatevpn.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Recognisable line glyphs for the compact Home service grid.
 *
 * They are drawn with Canvas so the app needs no icon font, extra dependency or raster asset,
 * and every shape is symmetric enough to stay readable in LTR and RTL layouts.
 */
@Composable
fun KxServiceGlyph(kind: String, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = 1.5.dp.toPx()
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        val cx = w / 2f
        val cy = h / 2f

        fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
            drawLine(tint, Offset(w * x1, h * y1), Offset(w * x2, h * y2),
                strokeWidth, StrokeCap.Round)

        when (kind) {
            "servers" -> {
                drawCircle(tint, w * .32f, Offset(cx, cy), style = stroke)
                drawOval(tint, Offset(w * .30f, h * .18f), Size(w * .40f, h * .64f),
                    style = stroke)
                line(.10f, .5f, .90f, .5f)
            }
            "security" -> {
                val shield = Path().apply {
                    moveTo(cx, h * .10f)
                    cubicTo(w * .64f, h * .18f, w * .76f, h * .20f, w * .80f, h * .22f)
                    lineTo(w * .78f, h * .54f)
                    cubicTo(w * .76f, h * .72f, w * .64f, h * .84f, cx, h * .92f)
                    cubicTo(w * .36f, h * .84f, w * .24f, h * .72f, w * .22f, h * .54f)
                    lineTo(w * .20f, h * .22f)
                    cubicTo(w * .24f, h * .20f, w * .36f, h * .18f, cx, h * .10f)
                    close()
                }
                drawPath(shield, tint, style = stroke)
                line(.36f, .50f, .45f, .60f)
                line(.45f, .60f, .64f, .40f)
            }
            "split" -> {
                line(.50f, .12f, .50f, .44f)
                line(.50f, .44f, .22f, .78f)
                line(.50f, .44f, .78f, .78f)
                drawCircle(tint, w * .07f, Offset(w * .22f, h * .82f), style = stroke)
                drawCircle(tint, w * .07f, Offset(w * .78f, h * .82f), style = stroke)
            }
            "advanced" -> {
                line(.14f, .26f, .86f, .26f)
                drawCircle(tint, w * .065f, Offset(w * .34f, h * .26f), style = stroke)
                line(.14f, .50f, .86f, .50f)
                drawCircle(tint, w * .065f, Offset(w * .64f, h * .50f), style = stroke)
                line(.14f, .74f, .86f, .74f)
                drawCircle(tint, w * .065f, Offset(w * .44f, h * .74f), style = stroke)
            }
            "stats" -> {
                drawRoundRect(tint, Offset(w * .18f, h * .54f), Size(w * .12f, h * .30f),
                    CornerRadius(w * .05f))
                drawRoundRect(tint, Offset(w * .44f, h * .36f), Size(w * .12f, h * .48f),
                    CornerRadius(w * .05f))
                drawRoundRect(tint, Offset(w * .70f, h * .20f), Size(w * .12f, h * .64f),
                    CornerRadius(w * .05f))
            }
            "storage" -> {
                drawRoundRect(tint, Offset(w * .12f, h * .26f), Size(w * .76f, h * .48f),
                    CornerRadius(w * .12f), style = stroke)
                line(.12f, .46f, .88f, .46f)
                drawCircle(tint, w * .045f, Offset(w * .74f, h * .60f))
            }
            "cache" -> {
                val sparkle = Path().apply {
                    moveTo(cx, h * .06f)
                    lineTo(w * .58f, h * .42f)
                    lineTo(w * .94f, cy)
                    lineTo(w * .58f, h * .58f)
                    lineTo(cx, h * .94f)
                    lineTo(w * .42f, h * .58f)
                    lineTo(w * .06f, cy)
                    lineTo(w * .42f, h * .42f)
                    close()
                }
                drawPath(sparkle, tint)
                drawCircle(tint, w * .05f, Offset(w * .82f, h * .18f), style = stroke)
            }
            "network" -> {
                drawArc(tint, 205f, 130f, false, Offset(cx - w * .40f, h * .24f),
                    Size(w * .80f, h * .80f), style = stroke)
                drawArc(tint, 205f, 130f, false, Offset(cx - w * .27f, h * .40f),
                    Size(w * .54f, h * .54f), style = stroke)
                drawArc(tint, 205f, 130f, false, Offset(cx - w * .14f, h * .56f),
                    Size(w * .28f, h * .28f), style = stroke)
                drawCircle(tint, w * .05f, Offset(cx, h * .86f))
            }
            "settings" -> {
                drawCircle(tint, w * .20f, Offset(cx, cy), style = stroke)
                for (i in 0 until 8) {
                    val angle = Math.toRadians(i * 45.0)
                    val x1 = cx + cos(angle).toFloat() * w * .26f
                    val y1 = cy + sin(angle).toFloat() * w * .26f
                    val x2 = cx + cos(angle).toFloat() * w * .38f
                    val y2 = cy + sin(angle).toFloat() * w * .38f
                    drawLine(tint, Offset(x1, y1), Offset(x2, y2),
                        strokeWidth, StrokeCap.Round)
                }
            }
            "import" -> {
                line(.22f, .58f, .18f, .84f)
                line(.18f, .84f, .82f, .84f)
                line(.82f, .84f, .78f, .58f)
                line(.50f, .14f, .50f, .64f)
                line(.34f, .48f, .50f, .64f)
                line(.66f, .48f, .50f, .64f)
            }
            "repair" -> {
                drawCircle(tint, w * .36f, Offset(cx, cy), style = stroke)
                line(.34f, .52f, .46f, .64f)
                line(.46f, .64f, .68f, .38f)
            }
            else -> drawCircle(tint, w * .30f, Offset(cx, cy), style = stroke)
        }
    }
}
