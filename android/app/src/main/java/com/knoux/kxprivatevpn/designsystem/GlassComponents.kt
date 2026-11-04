package com.knoux.kxprivatevpn.designsystem

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knoux.kxprivatevpn.launch.IntroTimeline

/** Shared native glass surface used by sub-pages; layered gradient plus a light rim. */
@Composable
fun GlassSurface(
    night: Boolean,
    modifier: Modifier = Modifier,
    radius: Dp = 26.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    val fill = if (night) {
        listOf(Color.White.copy(alpha = .08f), Color(0xFF6D63E8).copy(alpha = .16f),
            Color(0xFF54D8FF).copy(alpha = .07f))
    } else {
        listOf(Color.White.copy(alpha = .50f), Color(0xFFB8B5FF).copy(alpha = .24f),
            Color(0xFFCFF8FF).copy(alpha = .26f))
    }
    val rim = if (night) {
        listOf(Color.White.copy(alpha = .34f), Color(0xFF79E7FF).copy(alpha = .26f),
            Color(0xFFC9A6FF).copy(alpha = .32f))
    } else {
        listOf(Color.White.copy(alpha = .92f), Color(0xFF8FDFFF).copy(alpha = .48f),
            Color(0xFFD8B9FF).copy(alpha = .72f))
    }
    Column(
        modifier.fillMaxWidth()
            .background(Brush.linearGradient(fill), shape)
            .border(1.dp, Brush.linearGradient(rim), shape)
            .padding(contentPadding),
        content = content,
    )
}

@Composable
fun KxBrandLockup(night: Boolean, orbSize: Dp = 34.dp, modifier: Modifier = Modifier) {
    val foreground = if (night) Color(0xFFF6F1FF) else Color(0xFF171050)
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        KxOrb(progress = 1f, frame = IntroTimeline.frame(1f), modifier = Modifier.size(orbSize))
        Spacer(Modifier.width(7.dp))
        Text("KX", color = foreground, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp)
    }
}

/** Back target with a 48dp touch area and a chevron that mirrors in RTL. */
@Composable
fun KxPageHeader(
    title: String,
    night: Boolean,
    backLabel: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val foreground = if (night) Color(0xFFF6F1FF) else Color(0xFF171050)
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                if (night) listOf(Color.White.copy(alpha = .14f),
                                    Color(0xFF6B62E8).copy(alpha = .22f))
                                else listOf(Color.White.copy(alpha = .62f),
                                    Color(0xFFB6C9FF).copy(alpha = .34f)),
                            ),
                        )
                        .border(1.dp, Color.White.copy(alpha = if (night) .38f else .85f), CircleShape)
                        .clickable(role = Role.Button, onClickLabel = backLabel, onClick = onBack)
                        .semantics { contentDescription = backLabel },
                    contentAlignment = Alignment.Center,
                ) { BackChevron(foreground) }
            }
            Spacer(Modifier.weight(1f))
            KxBrandLockup(night)
            Spacer(Modifier.weight(1f))
            // Keep the brand lockup optically centered against the back target.
            Spacer(Modifier.size(48.dp))
        }
        Text(
            title,
            Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
            color = foreground,
            fontSize = 29.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            letterSpacing = (-0.4).sp,
        )
    }
}

@Composable
private fun BackChevron(color: Color) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Canvas(Modifier.size(20.dp)) {
        val stroke = 2.4.dp.toPx()
        val near = if (rtl) size.width * .66f else size.width * .34f
        val far = if (rtl) size.width * .34f else size.width * .66f
        drawLine(color, Offset(near, size.height * .18f), Offset(far, size.height * .5f),
            strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(far, size.height * .5f), Offset(near, size.height * .82f),
            strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

/**
 * Custom glass switch. [onClick] may open system settings for platform-owned controls;
 * the switch itself never changes a value the app does not own.
 */
@Composable
fun GlassSwitch(
    checked: Boolean,
    enabled: Boolean,
    stateDescription: String,
    clickLabel: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val thumbOffset by animateDpAsState(if (checked) 24.dp else 3.dp, label = "switch-thumb")
    val track = if (checked) {
        Brush.horizontalGradient(listOf(Color(0xFF8B5CFF), Color(0xFF4F7DFF), Color(0xFF64E9FF)))
    } else if (enabled) {
        Brush.horizontalGradient(listOf(Color(0xFF9A94C9).copy(alpha = .6f),
            Color(0xFF7C76B5).copy(alpha = .5f)))
    } else {
        Brush.horizontalGradient(listOf(Color(0xFF9A94C9).copy(alpha = .28f),
            Color(0xFF7C76B5).copy(alpha = .22f)))
    }
    Box(
        modifier.size(52.dp, 48.dp)
            .clickable(enabled = enabled && onClick != null, role = Role.Switch,
                onClickLabel = clickLabel, onClick = { onClick?.invoke() })
            .semantics { this.stateDescription = stateDescription },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(52.dp, 30.dp).clip(RoundedCornerShape(50))
                .background(track)
                .border(1.dp, Color.White.copy(alpha = if (enabled) .55f else .25f),
                    RoundedCornerShape(50)),
        ) {
            Box(
                Modifier.align(Alignment.CenterStart).offset(x = thumbOffset).size(24.dp)
                    .background(if (enabled) Color.White else Color.White.copy(alpha = .6f), CircleShape),
            )
        }
    }
}
