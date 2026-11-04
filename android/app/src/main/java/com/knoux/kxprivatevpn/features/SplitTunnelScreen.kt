package com.knoux.kxprivatevpn.features

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.knoux.kxprivatevpn.R
import com.knoux.kxprivatevpn.designsystem.Atmosphere
import com.knoux.kxprivatevpn.designsystem.GlassSurface
import com.knoux.kxprivatevpn.designsystem.GlassSwitch
import com.knoux.kxprivatevpn.designsystem.KxPageHeader
import com.knoux.kxprivatevpn.platform.apps.InstalledApp
import com.knoux.kxprivatevpn.platform.vpn.SplitTunnelMode
import com.knoux.kxprivatevpn.platform.vpn.SplitTunnelPlan
import com.knoux.kxprivatevpn.platform.vpn.SplitTunnelSelection
import com.knoux.kxprivatevpn.platform.vpn.SplitTunnelWarning
import com.knoux.kxprivatevpn.platform.vpn.SplitTunnelPolicy

/**
 * Everything the screen renders. [plan] is the measured intersection of the saved selection
 * and the launchable apps Android reported; it is supplied by the view model, never guessed
 * in the composable.
 */
data class SplitTunnelUiState(
    val selection: SplitTunnelSelection = SplitTunnelSelection(),
    val installed: List<InstalledApp> = emptyList(),
    val plan: SplitTunnelPlan = SplitTunnelPolicy.plan(SplitTunnelSelection(), emptySet()),
    val loading: Boolean = true,
    val loadFailed: Boolean = false,
    val saving: Boolean = false,
    val saveFailed: Boolean = false,
    val connected: Boolean = false,
    val dirty: Boolean = false,
    val appliedThrough: Int = 0,
    val appliedBypass: Int = 0,
)

/** Real screen route: DataStore + PackageManager on entry, backend rebuild on reconnect. */
@Composable
internal fun SplitTunnelRoute(
    model: ClientViewModel,
    night: Boolean,
    onBack: () -> Unit,
    onReconnect: () -> Unit,
) {
    val state by model.splitState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { model.refreshSplitTunnel() }
    SplitTunnelScreen(
        night = night,
        state = state,
        onBack = onBack,
        onModeChange = model::setSplitMode,
        onToggleApp = model::toggleSplitApp,
        onRefresh = model::refreshSplitTunnel,
        onReconnect = onReconnect,
    )
}

@Composable
fun SplitTunnelScreen(
    night: Boolean,
    state: SplitTunnelUiState,
    onBack: () -> Unit,
    onModeChange: (SplitTunnelMode) -> Unit,
    onToggleApp: (String) -> Unit,
    onRefresh: () -> Unit,
    onReconnect: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val muted = if (night) Color(0xFFC8C5E9) else Color(0xFF4B477F)
    val titleColor = if (night) Color(0xFFF6F1FF) else Color(0xFF171050)
    val filtered = remember(state.installed, query) {
        val needle = query.trim()
        if (needle.isEmpty()) state.installed
        else state.installed.filter {
            it.label.contains(needle, ignoreCase = true) ||
                it.packageName.contains(needle, ignoreCase = true)
        }
    }
    Box(Modifier.fillMaxSize().testTag("split-screen")) {
        Atmosphere(night, 0f)
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    KxPageHeader(
                        title = stringResource(R.string.split_title),
                        night = night,
                        backLabel = stringResource(R.string.back),
                        onBack = onBack,
                    )
                }
                item { SplitHero(night, muted, titleColor) }
                item {
                    ModeSelector(
                        night = night,
                        mode = state.selection.mode,
                        muted = muted,
                        onModeChange = onModeChange,
                    )
                }
                item {
                    SearchField(night, titleColor, muted, query) { if (it.length <= 64) query = it }
                }
                if (state.saveFailed) {
                    item {
                        Text(
                            stringResource(R.string.split_save_failed),
                            color = Color(0xFFC23A5E), fontSize = 12.sp,
                            modifier = Modifier.testTag("split-save-error"),
                        )
                    }
                }
                state.plan.warnings.forEach { warning ->
                    item(key = "warning-${warning.name}") { WarningBanner(night, warning, state) }
                }
                if (state.connected) {
                    item {
                        ConnectedBanner(
                            night = night,
                            state = state,
                            muted = muted,
                            titleColor = titleColor,
                            onReconnect = onReconnect,
                        )
                    }
                }
                when {
                    state.loading -> item { LoadingPanel(night, muted) }
                    state.loadFailed -> item { LoadFailedPanel(night, muted, onRefresh) }
                    state.installed.isEmpty() -> item {
                        Text(
                            stringResource(R.string.split_no_apps),
                            color = muted, fontSize = 12.sp, lineHeight = 17.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                                .testTag("split-empty"),
                        )
                    }
                    filtered.isEmpty() -> item {
                        Text(
                            stringResource(R.string.split_no_match, query.trim()),
                            color = muted, fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                                .testTag("split-no-match"),
                        )
                    }
                    else -> items(filtered, key = { it.packageName }) { app ->
                        AppRow(
                            night = night,
                            app = app,
                            throughVpn = effectiveThroughVpn(state, app.packageName),
                            enabled = !state.saving,
                            muted = muted,
                            titleColor = titleColor,
                            onToggle = { onToggleApp(app.packageName) },
                        )
                    }
                }
                if (state.loadFailed || state.installed.isEmpty()) {
                    item {
                        OutlinedButton(onClick = onRefresh,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .testTag("split-refresh")) {
                            Text(stringResource(R.string.split_refresh))
                        }
                    }
                }
            }
            ScopeNotes(night, muted, state)
            SummaryBar(night, muted, titleColor, state)
        }
    }
}

/**
 * The switch shows measured routing, not list membership. When no allow/deny list is applied
 * (empty ONLY_VPN selection, empty BYPASS selection, or only stale packages), Android routes
 * every app through the tunnel, so every row is truthfully "Only through VPN" and on.
 */
private fun effectiveThroughVpn(state: SplitTunnelUiState, packageName: String): Boolean {
    if (!state.plan.isApplied) return true
    val selected = packageName in state.selection.packages
    return if (state.selection.mode == SplitTunnelMode.ONLY_VPN) selected else !selected
}

@Composable
private fun SplitHero(night: Boolean, muted: Color, titleColor: Color) {
    GlassSurface(
        night, Modifier.testTag("split-hero"), radius = 30.dp,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SplitRouteEmblem(Modifier.size(104.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(stringResource(R.string.split_hero_title), color = titleColor,
                    fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.split_hero_body), color = muted,
                    fontSize = 11.5.sp, lineHeight = 16.sp)
            }
        }
    }
}

@Composable
private fun SplitRouteEmblem(modifier: Modifier = Modifier) {
    Canvas(modifier.semantics { contentDescription = "" }) {
        val c = center
        val r = size.minDimension / 2f
        drawCircle(Brush.radialGradient(listOf(Color(0xFFC39CFF).copy(alpha = .35f), Color.Transparent),
            c, r * 1.3f), r, c)
        drawCircle(Brush.radialGradient(
            listOf(Color.White.copy(alpha = .88f), Color(0xFFB9D4FF).copy(alpha = .52f),
                Color(0xFF8DBBFF).copy(alpha = .35f)),
            Offset(c.x - r * .3f, c.y - r * .35f), r * 1.4f,
        ), r * .70f, c)
        drawCircle(Color.White.copy(alpha = .78f), r * .70f, c, style = Stroke(1.1.dp.toPx()))
        // Two routing ribbons: one stays in the tunnel, one breaks away.
        val through = Path().apply {
            moveTo(c.x - r * .78f, c.y - r * .05f)
            cubicTo(c.x - r * .38f, c.y - r * .42f, c.x - r * .08f, c.y + r * .28f,
                c.x + r * .30f, c.y - r * .22f)
        }
        drawPath(through, Brush.linearGradient(listOf(Color(0xFF67E7FF), Color(0xFF7C7CFF),
            Color(0xFFC86BFF)), Offset(c.x - r * .8f, c.y), Offset(c.x + r * .4f, c.y)),
            style = Stroke(r * .16f, cap = StrokeCap.Round))
        val bypass = Path().apply {
            moveTo(c.x - r * .80f, c.y + r * .30f)
            cubicTo(c.x - r * .42f, c.y + r * .02f, c.x - r * .10f, c.y + r * .56f,
                c.x + r * .34f, c.y + r * .18f)
        }
        drawPath(bypass, Brush.linearGradient(listOf(Color(0xFFE7B8FF), Color(0xFFB784FF),
            Color(0xFF8A6BFF)), Offset(c.x - r * .8f, c.y + r * .3f),
            Offset(c.x + r * .4f, c.y + r * .2f)),
            style = Stroke(r * .16f, cap = StrokeCap.Round))
        // Badges: shield means through the tunnel; crossed eye means direct.
        val shieldCenter = Offset(c.x + r * .48f, c.y - r * .38f)
        drawCircle(Brush.radialGradient(listOf(Color(0xFF6FE9FF), Color(0xFF3E7BFF)),
            shieldCenter, r * .3f), r * .23f, shieldCenter)
        drawShield(Color.White, shieldCenter, r * .16f)
        val eyeCenter = Offset(c.x + r * .50f, c.y + r * .40f)
        drawCircle(Brush.radialGradient(listOf(Color(0xFFD9A6FF), Color(0xFF7A4BFF)),
            eyeCenter, r * .3f), r * .22f, eyeCenter)
        drawBypassGlyph(Color.White, eyeCenter, r * .14f)
        star(Offset(c.x - r * .30f, c.y - r * .62f), r * .035f)
        star(Offset(c.x + r * .12f, c.y + r * .64f), r * .028f)
    }
}

private fun DrawScope.drawShield(color: Color, center: Offset, radius: Float) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        cubicTo(center.x - radius * .35f, center.y - radius * .7f,
            center.x - radius * .8f, center.y - radius * .68f,
            center.x - radius * .8f, center.y - radius * .68f)
        cubicTo(center.x - radius * .82f, center.y + radius * .2f,
            center.x - radius * .3f, center.y + radius * .68f,
            center.x, center.y + radius)
        cubicTo(center.x + radius * .3f, center.y + radius * .68f,
            center.x + radius * .82f, center.y + radius * .2f,
            center.x + radius * .8f, center.y - radius * .68f)
        cubicTo(center.x + radius * .8f, center.y - radius * .68f,
            center.x + radius * .35f, center.y - radius * .7f,
            center.x, center.y - radius)
        close()
    }
    drawPath(path, color.copy(alpha = .95f))
}

private fun DrawScope.drawBypassGlyph(color: Color, center: Offset, radius: Float) {
    drawOval(color, topLeft = Offset(center.x - radius, center.y - radius * .55f),
        size = Size(radius * 2f, radius * 1.1f), style = Stroke(1.4.dp.toPx()))
    drawCircle(color, radius * .28f, center)
    drawLine(color, Offset(center.x - radius * .9f, center.y + radius * .75f),
        Offset(center.x + radius * .9f, center.y - radius * .75f),
        1.6.dp.toPx(), StrokeCap.Round)
}

private fun DrawScope.star(p: Offset, radius: Float) {
    drawLine(Color.White.copy(alpha = .9f), Offset(p.x - radius, p.y), Offset(p.x + radius, p.y),
        1f, StrokeCap.Round)
    drawLine(Color.White.copy(alpha = .9f), Offset(p.x, p.y - radius), Offset(p.x, p.y + radius),
        1f, StrokeCap.Round)
}

@Composable
private fun ModeSelector(
    night: Boolean,
    mode: SplitTunnelMode,
    muted: Color,
    onModeChange: (SplitTunnelMode) -> Unit,
) {
    val rim = if (night) Color.White.copy(alpha = .34f) else Color.White.copy(alpha = .86f)
    Row(
        Modifier.fillMaxWidth()
            .background(Brush.linearGradient(if (night) listOf(
                Color.White.copy(alpha = .07f), Color(0xFF6D63E8).copy(alpha = .13f))
            else listOf(Color.White.copy(alpha = .48f), Color(0xFFB8B5FF).copy(alpha = .22f))),
                RoundedCornerShape(50))
            .border(1.dp, rim, RoundedCornerShape(50))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ModeOption(
            night = night, selected = mode == SplitTunnelMode.BYPASS,
            label = stringResource(R.string.split_mode_bypass), muted = muted,
            glyph = ModeGlyph.BYPASS, modifier = Modifier.weight(1f).testTag("split-mode-bypass"),
            onClick = { onModeChange(SplitTunnelMode.BYPASS) },
        )
        ModeOption(
            night = night, selected = mode == SplitTunnelMode.ONLY_VPN,
            label = stringResource(R.string.split_mode_only), muted = muted,
            glyph = ModeGlyph.SHIELD, modifier = Modifier.weight(1f).testTag("split-mode-only"),
            onClick = { onModeChange(SplitTunnelMode.ONLY_VPN) },
        )
    }
}

private enum class ModeGlyph { SHIELD, BYPASS }

@Composable
private fun ModeOption(
    night: Boolean,
    selected: Boolean,
    label: String,
    muted: Color,
    glyph: ModeGlyph,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val foreground = if (selected) Color.White else muted
    Row(
        modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(50))
            .then(if (selected) Modifier.background(Brush.horizontalGradient(
                listOf(Color(0xFF8B5CFF), Color(0xFF4F7DFF), Color(0xFF64E9FF))))
            else Modifier)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (glyph == ModeGlyph.SHIELD) MiniShield(foreground, Modifier.size(18.dp))
        else MiniBypass(foreground, Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = foreground, fontSize = 12.5.sp, fontWeight = FontWeight.Bold,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SearchField(
    night: Boolean,
    titleColor: Color,
    muted: Color,
    value: String,
    onValueChange: (String) -> Unit,
) {
    val label = stringResource(R.string.split_search)
    val rim = if (night) Color.White.copy(alpha = .30f) else Color.White.copy(alpha = .85f)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .background(if (night) Color.White.copy(alpha = .07f) else Color.White.copy(alpha = .46f),
                RoundedCornerShape(50))
            .border(1.dp, rim, RoundedCornerShape(50))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SearchGlyph(muted.copy(alpha = .9f), Modifier.size(18.dp))
        Spacer(Modifier.width(9.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = titleColor, fontSize = 14.sp),
            cursorBrush = SolidColor(if (night) Color(0xFF99EBFF) else Color(0xFF4420B3)),
            modifier = Modifier.weight(1f).testTag("split-search")
                .semantics { contentDescription = label },
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(stringResource(R.string.split_search), color = muted.copy(alpha = .8f),
                            fontSize = 14.sp, maxLines = 1)
                    }
                    inner()
                }
            },
        )
    }
}

@Composable
private fun MiniShield(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.semantics { contentDescription = "" }) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * .5f, h * .06f)
            cubicTo(w * .4f, h * .16f, w * .24f, h * .17f, w * .18f, h * .18f)
            cubicTo(w * .18f, h * .58f, w * .32f, h * .82f, w * .5f, h * .94f)
            cubicTo(w * .68f, h * .82f, w * .82f, h * .58f, w * .82f, h * .18f)
            cubicTo(w * .76f, h * .17f, w * .6f, h * .16f, w * .5f, h * .06f)
            close()
        }
        drawPath(path, color)
    }
}

@Composable
private fun MiniBypass(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.semantics { contentDescription = "" }) {
        val w = size.width
        val h = size.height
        drawOval(color, topLeft = Offset(w * .06f, h * .28f),
            size = Size(w * .88f, h * .44f), style = Stroke(1.5.dp.toPx()))
        drawCircle(color, w * .13f, Offset(w * .5f, h * .5f))
        drawLine(color, Offset(w * .1f, h * .86f), Offset(w * .9f, h * .14f),
            1.6.dp.toPx(), StrokeCap.Round)
    }
}

@Composable
private fun SearchGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.semantics { contentDescription = "" }) {
        val stroke = 1.8.dp.toPx()
        drawCircle(color, size.minDimension * .32f,
            Offset(size.width * .43f, size.height * .43f), style = Stroke(stroke))
        drawLine(color, Offset(size.width * .64f, size.height * .64f),
            Offset(size.width * .9f, size.height * .9f), stroke, StrokeCap.Round)
    }
}

@Composable
private fun AppRow(
    night: Boolean,
    app: InstalledApp,
    throughVpn: Boolean,
    enabled: Boolean,
    muted: Color,
    titleColor: Color,
    onToggle: () -> Unit,
) {
    val status = stringResource(if (throughVpn) R.string.split_only_vpn else R.string.split_bypass)
    GlassSurface(
        night,
        modifier = Modifier.testTag("split-app-${app.packageName}"),
        radius = 22.dp,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(app.packageName, app.label)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(app.label, color = titleColor, fontSize = 15.sp,
                    fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (throughVpn) MiniShield(muted, Modifier.size(12.dp))
                    else MiniBypass(muted, Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(status, color = muted, fontSize = 11.sp, maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("split-app-status-${app.packageName}"))
                }
            }
            Spacer(Modifier.width(6.dp))
            GlassSwitch(
                checked = throughVpn,
                enabled = enabled,
                stateDescription = status,
                clickLabel = app.label,
                modifier = Modifier.testTag("split-app-switch-${app.packageName}"),
                onClick = onToggle,
            )
        }
    }
}

@Composable
private fun AppIcon(packageName: String, label: String) {
    val context = LocalContext.current
    val icon = remember(packageName) {
        runCatching {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            val width = drawable.intrinsicWidth.takeIf { it in 1..512 } ?: 96
            val height = drawable.intrinsicHeight.takeIf { it in 1..512 } ?: 96
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = AndroidCanvas(bitmap)
            drawable.setBounds(0, 0, width, height)
            drawable.draw(canvas)
            bitmap.asImageBitmap()
        }.getOrNull()
    }
    if (icon != null) {
        Image(icon, contentDescription = null,
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)))
    } else {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF8B5CFF), Color(0xFF4F7DFF)))),
            contentAlignment = Alignment.Center) {
            Text(label.take(1).uppercase(), color = Color.White, fontSize = 16.sp,
                fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun WarningBanner(night: Boolean, warning: SplitTunnelWarning, state: SplitTunnelUiState) {
    val text = when (warning) {
        SplitTunnelWarning.NO_APPS_SELECTED -> stringResource(R.string.split_warning_no_apps)
        SplitTunnelWarning.SAVED_APPS_NOT_INSTALLED ->
            stringResource(R.string.split_warning_stale, state.plan.stalePackages.size)
    }
    val tag = when (warning) {
        SplitTunnelWarning.NO_APPS_SELECTED -> "split-warning-no-apps"
        SplitTunnelWarning.SAVED_APPS_NOT_INSTALLED -> "split-warning-stale"
    }
    Row(
        Modifier.fillMaxWidth().testTag(tag).semantics(mergeDescendants = true) {}
            .background(Color(0xFFFFC94D).copy(alpha = if (night) .13f else .22f),
                RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFFFFC94D).copy(alpha = .45f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(18.dp)
            .background(Color(0xFFFFC94D).copy(alpha = .35f), CircleShape),
            contentAlignment = Alignment.Center) {
            Text("!", color = if (night) Color(0xFFFFD98A) else Color(0xFF7A4E00),
                fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(8.dp))
        Text(text, color = if (night) Color(0xFFFFD98A) else Color(0xFF6B4500),
            fontSize = 11.5.sp, lineHeight = 15.sp)
    }
}

@Composable
private fun ConnectedBanner(
    night: Boolean,
    state: SplitTunnelUiState,
    muted: Color,
    titleColor: Color,
    onReconnect: () -> Unit,
) {
    GlassSurface(night, Modifier.testTag("split-reconnect-panel"), radius = 22.dp,
        contentPadding = PaddingValues(horizontal = 13.dp, vertical = 11.dp)) {
        if (state.dirty) {
            Text(stringResource(R.string.split_reconnect_required), color = titleColor,
                fontSize = 12.5.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            OutlinedButton(onClick = onReconnect, enabled = !state.saving,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .testTag("split-reconnect")) {
                Text(stringResource(R.string.split_reconnect))
            }
        } else {
            Text(
                stringResource(R.string.split_applied,
                    state.appliedThrough, state.appliedBypass),
                color = muted, fontSize = 12.sp, lineHeight = 16.sp,
                modifier = Modifier.testTag("split-applied"),
            )
        }
    }
}

@Composable
private fun LoadingPanel(night: Boolean, muted: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 18.dp).testTag("split-loading"),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(Modifier.size(20.dp), color = muted, strokeWidth = 2.dp)
        Spacer(Modifier.width(10.dp))
        Text(stringResource(R.string.split_loading), color = muted, fontSize = 12.sp)
    }
}

@Composable
private fun LoadFailedPanel(night: Boolean, muted: Color, onRefresh: () -> Unit) {
    GlassSurface(night, Modifier.testTag("split-load-failed"), radius = 22.dp) {
        Text(stringResource(R.string.split_load_failed), color = muted,
            fontSize = 12.sp, lineHeight = 17.sp)
        OutlinedButton(onClick = onRefresh,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .testTag("split-retry")) {
            Text(stringResource(R.string.split_refresh))
        }
    }
}

/** Scope notes live under the list so they stay visible while a long app list scrolls. */
@Composable
private fun ScopeNotes(night: Boolean, muted: Color, state: SplitTunnelUiState) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(stringResource(R.string.split_note_scope), color = muted,
            fontSize = 10.sp, lineHeight = 13.sp)
        Text(stringResource(R.string.split_note_import), color = muted,
            fontSize = 10.sp, lineHeight = 13.sp)
        Text(
            stringResource(if (state.connected) R.string.split_note_reconnect
            else R.string.split_note_apply),
            color = muted, fontSize = 10.sp, lineHeight = 13.sp,
            modifier = Modifier.testTag("split-note-apply"),
        )
    }
}

@Composable
private fun SummaryBar(night: Boolean, muted: Color, titleColor: Color, state: SplitTunnelUiState) {
    val total = state.plan.installedCount
    val through = state.plan.throughVpnCount
    val bypass = state.plan.bypassCount
    Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
        Row(
            Modifier.fillMaxWidth().testTag("split-summary")
                .background(Brush.linearGradient(if (night) listOf(
                    Color.White.copy(alpha = .10f), Color(0xFF6D63E8).copy(alpha = .17f))
                else listOf(Color.White.copy(alpha = .60f), Color(0xFFB8B5FF).copy(alpha = .30f))),
                    RoundedCornerShape(50))
                .border(1.dp, Color.White.copy(alpha = if (night) .30f else .85f),
                    RoundedCornerShape(50))
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.split_summary_apps, total), color = titleColor,
                fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag("split-summary-apps"))
            Spacer(Modifier.weight(1f))
            MiniShield(muted, Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.split_summary_through, through), color = titleColor,
                fontSize = 11.sp, modifier = Modifier.testTag("split-summary-through"))
            Spacer(Modifier.weight(1f))
            MiniBypass(muted, Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.split_summary_bypass, bypass), color = titleColor,
                fontSize = 11.sp, modifier = Modifier.testTag("split-summary-bypass"))
        }
    }
}
