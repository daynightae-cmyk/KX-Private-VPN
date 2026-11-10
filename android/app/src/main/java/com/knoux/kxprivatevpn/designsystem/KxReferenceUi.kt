package com.knoux.kxprivatevpn.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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

/** Deep indigo glass-ribbon atmosphere measured from the owner's approved references. */
@Composable
fun KxReferenceAtmosphere(night: Boolean) {
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (night) {
            drawRect(
                Brush.linearGradient(
                    listOf(Color(0xFF2C195F), Color(0xFF1A1B4A), Color(0xFF12193F), Color(0xFF0D1436)),
                    Offset(0f, 0f), Offset(w * .85f, h),
                ),
            )
            drawCircle(
                Brush.radialGradient(
                    listOf(Color(0xFF8A47F2).copy(alpha = .48f), Color(0xFF4F2AA6).copy(alpha = .20f), Color.Transparent),
                    Offset(w * .08f, h * .02f), w * .85f,
                ),
                w * .85f, Offset(w * .08f, h * .02f),
            )
            drawCircle(
                Brush.radialGradient(
                    listOf(Color(0xFF1FB8EA).copy(alpha = .22f), Color.Transparent),
                    Offset(w * .96f, h * .34f), w * .55f,
                ),
                w * .55f, Offset(w * .96f, h * .34f),
            )
            ribbon(
                w, h,
                startY = .055f, endY = .20f, thickness = .15f,
                colors = listOf(Color(0xFF6E36D8).copy(alpha = .46f), Color(0xFF2A74E8).copy(alpha = .20f), Color.Transparent),
            )
            ribbon(
                w, h,
                startY = .74f, endY = .90f, thickness = .19f,
                colors = listOf(Color(0xFF36C9F3).copy(alpha = .30f), Color(0xFF7A36D8).copy(alpha = .42f), Color(0xFF1D2E7A).copy(alpha = .32f)),
            )
            ribbon(
                w, h,
                startY = .86f, endY = .99f, thickness = .13f,
                colors = listOf(Color(0xFFB73BEA).copy(alpha = .28f), Color(0xFF2BC8F5).copy(alpha = .26f), Color.Transparent),
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
                    listOf(Color(0xFF9A77F5).copy(alpha = .35f), Color.Transparent),
                    Offset(w * .05f, h * .03f), w * .8f,
                ),
                w * .8f, Offset(w * .05f, h * .03f),
            )
            drawCircle(
                Brush.radialGradient(
                    listOf(Color(0xFF54C8F5).copy(alpha = .25f), Color.Transparent),
                    Offset(w * .96f, h * .32f), w * .55f,
                ),
                w * .55f, Offset(w * .96f, h * .32f),
            )
            ribbon(
                w, h,
                startY = .05f, endY = .19f, thickness = .14f,
                colors = listOf(Color(0xFF8B5CF6).copy(alpha = .18f), Color(0xFF38BDF8).copy(alpha = .16f), Color.Transparent),
            )
            ribbon(
                w, h,
                startY = .76f, endY = .91f, thickness = .18f,
                colors = listOf(Color(0xFF38BDF8).copy(alpha = .20f), Color(0xFF8B5CF6).copy(alpha = .20f), Color(0xFFC4B5FD).copy(alpha = .18f)),
            )
        }
    }
}

private fun DrawScope.ribbon(
    w: Float,
    h: Float,
    startY: Float,
    endY: Float,
    thickness: Float,
    colors: List<Color>,
) {
    val band = Path().apply {
        moveTo(-w * .12f, h * startY)
        cubicTo(w * .24f, h * (startY - .08f), w * .58f, h * (startY + .10f), w * 1.12f, h * (endY - .04f))
        lineTo(w * 1.12f, h * (endY + thickness * .55f))
        cubicTo(w * .62f, h * (endY + thickness), w * .22f, h * (startY + thickness * .8f), -w * .12f, h * (startY + thickness))
        close()
    }
    drawPath(
        band,
        Brush.linearGradient(colors, Offset(0f, h * startY), Offset(w, h * endY)),
    )
    val edge = Path().apply {
        moveTo(-w * .12f, h * startY)
        cubicTo(w * .24f, h * (startY - .08f), w * .58f, h * (startY + .10f), w * 1.12f, h * (endY - .04f))
    }
    drawPath(
        edge,
        Brush.linearGradient(
            listOf(Color.Transparent, Color.White.copy(alpha = .34f), Color(0xFF64EEFF).copy(alpha = .24f), Color.Transparent),
            Offset(0f, h * startY), Offset(w, h * endY),
        ),
        style = Stroke(.8.dp.toPx()),
    )
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
        modifier.fillMaxWidth().clip(shape).background(fill).border(1.dp, rim, shape).padding(padding),
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
        modifier.fillMaxWidth().heightIn(min = 42.dp).clip(shape)
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
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 9.dp)
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
    FOLDER, TARGET,
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
