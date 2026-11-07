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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knoux.kxprivatevpn.launch.IntroTimeline

fun glassForeground(night: Boolean): Color =
    if (night) Color(0xFFF7F3FF) else Color(0xFF171050)

/** A screen's explicit mode owns Material controls as well as its custom glass. */
@Composable
fun KxGlassTheme(night: Boolean, content: @Composable () -> Unit) {
    val foreground = glassForeground(night)
    val scheme = if (night) darkColorScheme(
        primary = Color(0xFF99EBFF), onPrimary = Color(0xFF101638),
        onSurface = foreground, onBackground = foreground,
    ) else lightColorScheme(
        primary = Color(0xFF4420B3), onPrimary = Color.White,
        onSurface = foreground, onBackground = foreground,
    )
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(LocalContentColor provides foreground, content = content)
    }
}

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
    // A standalone glass card must not inherit black text from an unrelated host theme.
    CompositionLocalProvider(LocalContentColor provides glassForeground(night)) {
        Column(
            modifier.fillMaxWidth()
                .background(Brush.linearGradient(fill), shape)
                .border(1.dp, Brush.linearGradient(rim), shape)
                .padding(contentPadding),
            content = content,
        )
    }
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

/** A section remains readable at large font scales instead of competing with its cards. */
@Composable
fun KxSectionHeader(title: String, night: Boolean, modifier: Modifier = Modifier) {
    Text(title, modifier.padding(top = 12.dp, bottom = 4.dp).semantics { heading() },
        color = glassForeground(night), fontSize = 18.sp, fontWeight = FontWeight.Bold)
}

/** One semantic action, explicit scope, and content-sized height; never implies a scan ran. */
@Composable
fun KxServiceCard(
    title: String,
    description: String,
    capability: String,
    actionLabel: String,
    night: Boolean,
    modifier: Modifier = Modifier,
    glyph: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    val foreground = glassForeground(night)
    val accent = if (night) Color(0xFF99EBFF) else Color(0xFF4420B3)
    GlassSurface(night, modifier.clip(RoundedCornerShape(20.dp))
        .heightIn(min = 48.dp)
        .clickable(role = Role.Button, onClickLabel = actionLabel, onClick = onClick),
        radius = 20.dp, contentPadding = PaddingValues(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) { glyph() }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, color = foreground, fontSize = 16.sp, lineHeight = 22.sp,
                    fontWeight = FontWeight.Bold)
                Text(description, color = foreground.copy(alpha = .88f),
                    fontSize = 14.sp, lineHeight = 20.sp)
                Text(capability, color = foreground.copy(alpha = .82f),
                    fontSize = 12.sp, lineHeight = 18.sp)
                Row(Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(actionLabel, Modifier.weight(1f), color = accent, fontSize = 14.sp,
                        lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
                    ForwardChevron(accent)
                }
            }
            KxOrb(1f, IntroTimeline.frame(1f), Modifier.size(28.dp).clearAndSetSemantics {})
        }
    }
}

@Composable
private fun ForwardChevron(color: Color) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Canvas(Modifier.size(16.dp)) {
        val start = size.width * if (rtl) .66f else .34f
        val end = size.width * if (rtl) .34f else .66f
        drawLine(color, Offset(start, size.height * .22f), Offset(end, size.height * .5f),
            2.dp.toPx(), StrokeCap.Round)
        drawLine(color, Offset(end, size.height * .5f), Offset(start, size.height * .78f),
            2.dp.toPx(), StrokeCap.Round)
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
            Box(
                Modifier.size(48.dp)
                    .clickable(role = Role.Button, onClickLabel = backLabel, onClick = onBack)
                    .semantics { contentDescription = backLabel },
                contentAlignment = Alignment.Center,
            ) {
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
                        .border(1.dp, Color.White.copy(alpha = if (night) .38f else .85f), CircleShape),
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
        // Back points toward the reading start: left in LTR, right in RTL.
        val near = if (rtl) size.width * .34f else size.width * .66f
        val far = if (rtl) size.width * .66f else size.width * .34f
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
    val interaction = if (onClick != null) {
        Modifier.clickable(enabled = enabled, role = Role.Switch,
            onClickLabel = clickLabel, onClick = onClick)
    } else {
        Modifier.semantics { if (!enabled) disabled() }
    }
    Box(
        modifier.size(52.dp, 48.dp)
            .then(interaction)
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
