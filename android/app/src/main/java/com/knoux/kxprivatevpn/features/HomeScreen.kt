package com.knoux.kxprivatevpn.features

import android.content.Context
import android.text.format.Formatter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knoux.kxprivatevpn.R
import com.knoux.kxprivatevpn.data.config.ProfileSnapshot
import com.knoux.kxprivatevpn.designsystem.Atmosphere
import com.knoux.kxprivatevpn.designsystem.GlassAction
import com.knoux.kxprivatevpn.designsystem.GlassSurface
import com.knoux.kxprivatevpn.designsystem.KxBrandLockup
import com.knoux.kxprivatevpn.designsystem.KxOrb
import com.knoux.kxprivatevpn.designsystem.KxServiceGlyph
import com.knoux.kxprivatevpn.designsystem.KxServiceCard
import com.knoux.kxprivatevpn.designsystem.KxSectionHeader
import com.knoux.kxprivatevpn.designsystem.RepairGlyph
import com.knoux.kxprivatevpn.designsystem.repairAccent
import com.knoux.kxprivatevpn.launch.IntroTimeline
import com.knoux.kxprivatevpn.platform.repair.RepairCatalog
import com.knoux.kxprivatevpn.platform.repair.RepairServiceId
import com.knoux.kxprivatevpn.platform.vpn.VpnPhase
import com.knoux.kxprivatevpn.platform.vpn.VpnSessionOwnership
import com.knoux.kxprivatevpn.platform.vpn.VpnState
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

/**
 * Consumer-first Home per the owner handoff: orb identity, one obvious Connect/Disconnect
 * action, the real selected server, honest measured metrics, and icon-first navigation.
 *
 * Every value here comes from the selected profile, the live WireGuard session or Android's
 * formatter. Disconnected/unmeasured values are an em dash; no location, latency or load is
 * invented.
 */
@Composable
fun KxHomeScreen(
    night: Boolean,
    snapshot: ProfileSnapshot,
    busy: Boolean,
    vpn: VpnState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onSession: () -> Unit,
    onDismissIssue: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    val context = LocalContext.current
    val selected = snapshot.profiles.firstOrNull { it.id == snapshot.selectedId }
    // While this process owns a tunnel, the live session server is the truth. The saved
    // selection only matters for the next connection.
    val activeSession = if (VpnSessionOwnership.ownsTunnel(vpn)) vpn.session else null
    val foreground = if (night) Color(0xFFF7F3FF) else Color(0xFF171050)
    Box(Modifier.fillMaxSize()) {
        Atmosphere(night, 0f)
        Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).fillMaxWidth().safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp)
                .testTag("route-home"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HomeHeader(night, foreground, onNavigate)
            HomeHero(night, activeSession?.profileName ?: selected?.name, busy, vpn, onConnect, onDisconnect, onSession)
            SmartChoiceCard(
                night = night,
                name = activeSession?.profileName ?: selected?.name,
                endpoint = activeSession?.endpoint ?: selected?.endpoint,
                active = activeSession != null,
                foreground = foreground,
                onNavigate = onNavigate,
            )
            MetricGrid(night, vpn, foreground)
            KxCareCard(night, onNavigate)
            ToolsGrid(night, onNavigate)
            vpn.issue?.let { issue ->
                GlassSurface(night) {
                    Text(vpnIssueText(issue), fontSize = 12.sp,
                        modifier = Modifier.testTag("vpn-issue"))
                    TextButton(
                        onClick = if (issue == com.knoux.kxprivatevpn.platform.vpn.VpnIssue.STOP_FAILED) onDisconnect else onDismissIssue,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("vpn-dismiss-issue"),
                    ) { Text(stringResource(if (issue == com.knoux.kxprivatevpn.platform.vpn.VpnIssue.STOP_FAILED) R.string.retry else R.string.dismiss)) }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        HomeBottomBar(
            night = night,
            selected = "home",
            modifier = Modifier.fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            onNavigate = onNavigate,
        )
        }
    }
}

@Composable
private fun HomeHeader(night: Boolean, foreground: Color, onNavigate: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KxBrandLockup(night, orbSize = 32.dp)
        Spacer(Modifier.width(8.dp))
        Text(
            "PRIVATE VPN",
            fontSize = 9.sp,
            letterSpacing = 1.6.sp,
            color = foreground.copy(alpha = .62f),
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.weight(1f))
        Box(
            Modifier.size(48.dp).clip(CircleShape)
                .background(Color.White.copy(alpha = if (night) .10f else .46f))
                .border(1.dp, Color.White.copy(alpha = if (night) .30f else .80f), CircleShape)
                .clickable(role = Role.Button,
                    onClickLabel = stringResource(R.string.settings),
                    onClick = { onNavigate("settings") })
                .testTag("open-settings"),
            contentAlignment = Alignment.Center,
        ) { SettingsGlyph(foreground) }
    }
}

@Composable
private fun HomeHero(
    night: Boolean,
    profileName: String?,
    busy: Boolean,
    vpn: VpnState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onSession: () -> Unit,
) {
    val foreground = if (night) Color(0xFFF7F3FF) else Color(0xFF171050)
    val stopFailed = vpn.issue == com.knoux.kxprivatevpn.platform.vpn.VpnIssue.STOP_FAILED
    val status = when {
        stopFailed -> stringResource(R.string.unknown_value)
        vpn.phase == VpnPhase.CONNECTED -> stringResource(R.string.connected)
        vpn.phase == VpnPhase.CONNECTING -> stringResource(R.string.connecting)
        vpn.phase == VpnPhase.DISCONNECTING -> stringResource(R.string.disconnecting)
        else -> stringResource(R.string.disconnected)
    }
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            val orbEnabled = !busy && (stopFailed || when (vpn.phase) {
                VpnPhase.CONNECTED -> true
                VpnPhase.IDLE, VpnPhase.ERROR -> profileName != null
                else -> false
            })
            val actionLabel = stringResource(
                if (vpn.phase == VpnPhase.CONNECTED || stopFailed) R.string.disconnect else R.string.connect)
            KxOrb(1f, IntroTimeline.frame(1f), Modifier.size(176.dp)
                .testTag("vpn-orb-action")
                .semantics { contentDescription = actionLabel }
                .clickable(enabled = orbEnabled, role = Role.Button,
                    onClick = {
                        if (vpn.phase == VpnPhase.CONNECTED || stopFailed) onDisconnect()
                        else onConnect()
                    }))
            StatusPill(
                status = status,
                connected = vpn.phase == VpnPhase.CONNECTED,
                foreground = foreground,
                night = night,
                modifier = Modifier.align(Alignment.BottomCenter).offset(y = (-4).dp),
            )
        }
        when (vpn.phase) {
            VpnPhase.CONNECTED -> {
                Text(
                    stringResource(R.string.tunnel_active),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = foreground,
                    modifier = Modifier.testTag("vpn-connected"),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = onSession,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("open-session"),
                    ) { Text(stringResource(R.string.session_details)) }
                    Spacer(Modifier.width(8.dp))
                    GlassAction(
                        stringResource(R.string.disconnect),
                        Modifier.weight(1f).testTag("vpn-disconnect"),
                    ) { onDisconnect() }
                }
                Text(stringResource(R.string.home_connected_hint), fontSize = 11.sp,
                    color = foreground.copy(alpha = .68f), textAlign = TextAlign.Center)
            }
            VpnPhase.CONNECTING, VpnPhase.DISCONNECTING -> {
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 28.dp))
            }
            else -> if (stopFailed) {
                GlassAction(stringResource(R.string.disconnect),
                    Modifier.fillMaxWidth().testTag("vpn-disconnect"), enabled = !busy,
                ) { onDisconnect() }
            } else {
                GlassAction(
                    stringResource(R.string.connect),
                    Modifier.fillMaxWidth().testTag("vpn-connect"),
                    enabled = profileName != null && !busy,
                ) { onConnect() }
                Text(
                    stringResource(
                        when {
                            profileName == null -> R.string.home_no_profile_hint
                            else -> R.string.home_connect_hint
                        },
                    ),
                    fontSize = 11.sp,
                    color = foreground.copy(alpha = .68f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }
    }
}

@Composable
private fun StatusPill(
    status: String,
    connected: Boolean,
    foreground: Color,
    night: Boolean,
    modifier: Modifier = Modifier,
) {
    val accent = if (connected) Color(0xFF64EEFF) else Color(0xFF9A94C9)
    val tone = if (night) accent else Color(0xFF3B2AA8)
    Row(
        modifier.clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = if (night) .10f else .42f))
            .border(1.dp, Color.White.copy(alpha = if (night) .28f else .78f),
                RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(7.dp).background(tone, CircleShape))
        Spacer(Modifier.width(7.dp))
        Text(status, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = foreground)
    }
}

@Composable
private fun SmartChoiceCard(
    night: Boolean,
    name: String?,
    endpoint: String?,
    active: Boolean,
    foreground: Color,
    onNavigate: (String) -> Unit,
) {
    GlassSurface(
        night,
        modifier = Modifier.fillMaxWidth()
            .clickable(role = Role.Button, onClick = { onNavigate("servers") })
            .testTag("open-servers"),
        contentPadding = PaddingValues(14.dp),
        radius = 22.dp,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                ServerGlyph(night)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(if (active) R.string.active_server else R.string.selected_server)
                        .uppercase(),
                    fontSize = 9.sp,
                    letterSpacing = 1.4.sp,
                    color = foreground.copy(alpha = .60f),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("home-server-label"),
                )
                Text(
                    name ?: stringResource(R.string.no_profiles),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = foreground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("home-profile"),
                )
                Text(
                    endpoint?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.smart_choice_hint),
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    color = foreground.copy(alpha = .68f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            ChevronGlyph(foreground)
        }
    }
}

@Composable
private fun MetricGrid(night: Boolean, vpn: VpnState, foreground: Color) {
    val context = LocalContext.current
    // A session object can exist on a failed start. Counters are real only while the
    // backend is actually owned by this process; otherwise show an em dash.
    val session = if (VpnSessionOwnership.ownsTunnel(vpn)) vpn.session else null
    val handshake = session?.latestHandshakeMillis?.let { clock(context, it) }
        ?: stringResource(R.string.unknown_value)
    val received = session?.rxBytes?.let { Formatter.formatFileSize(context, it) }
        ?: stringResource(R.string.unknown_value)
    val sent = session?.txBytes?.let { Formatter.formatFileSize(context, it) }
        ?: stringResource(R.string.unknown_value)
    val tunnelIp = session?.addresses?.firstOrNull()?.substringBefore('/')
        ?: stringResource(R.string.unknown_value)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(IntrinsicSize.Min)) {
            MetricTile(R.string.metric_handshake, handshake, MetricKind.HANDSHAKE, night,
                foreground, "metric-handshake", Modifier.weight(1f).fillMaxHeight())
            MetricTile(R.string.metric_received, received, MetricKind.RECEIVED, night,
                foreground, "metric-received", Modifier.weight(1f).fillMaxHeight())
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(IntrinsicSize.Min)) {
            MetricTile(R.string.metric_sent, sent, MetricKind.SENT, night, foreground,
                "metric-sent", Modifier.weight(1f).fillMaxHeight())
            MetricTile(R.string.metric_tunnel_ip, tunnelIp, MetricKind.TUNNEL, night,
                foreground, "metric-tunnel", Modifier.weight(1f).fillMaxHeight())
        }
    }
}

private enum class MetricKind { HANDSHAKE, RECEIVED, SENT, TUNNEL }
private enum class NavKind { HOME, SERVERS, SECURITY, STATS, SETTINGS }

@Composable
private fun MetricTile(
    label: Int,
    value: String,
    kind: MetricKind,
    night: Boolean,
    foreground: Color,
    tag: String,
    modifier: Modifier = Modifier,
) {
    val accent = when (kind) {
        MetricKind.HANDSHAKE -> Color(0xFFAD71FF)
        MetricKind.RECEIVED -> Color(0xFF64EEFF)
        MetricKind.SENT -> Color(0xFFFF8AD8)
        MetricKind.TUNNEL -> Color(0xFFB9E9FF)
    }
    val tone = if (night) accent else Color(0xFF3B2AA8)
    Column(
        modifier.clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(
                Color.White.copy(alpha = if (night) .09f else .46f),
                accent.copy(alpha = if (night) .12f else .18f))))
            .border(1.dp, Color.White.copy(alpha = if (night) .24f else .76f),
                RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MetricGlyph(kind, tone, Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(label), fontSize = 10.sp,
                color = foreground.copy(alpha = .68f), fontWeight = FontWeight.Medium)
        }
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = foreground,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag(tag))
    }
}

@Composable
private fun KxCareCard(night: Boolean, onNavigate: (String) -> Unit) {
    KxServiceCard(
        title = stringResource(R.string.kx_care),
        description = stringResource(R.string.home_repair_description),
        capability = stringResource(R.string.home_repair_scope, RepairCatalog.available()),
        actionLabel = stringResource(R.string.home_action_repair),
        night = night,
        modifier = Modifier.fillMaxWidth().testTag("open-repair"),
        glyph = { RepairGlyph(RepairServiceId.AUTOPILOT, Modifier.size(32.dp)) },
        onClick = { onNavigate("repair") },
    )
}

@Composable
private fun ToolsGrid(night: Boolean, onNavigate: (String) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // Descriptions need a reading column, not a phone-sized dashboard grid. Tablets
        // can show two columns; large fonts always keep one continuous reading order.
        val perRow = if (maxWidth >= 600.dp && LocalDensity.current.fontScale <= 1.3f) 2 else 1
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            HomeServiceSection.entries.forEach { section ->
                KxSectionHeader(stringResource(section.title), night)
                HomeServiceCatalog.entries.filter { it.section == section }.chunked(perRow).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { service ->
                            KxServiceCard(
                                title = stringResource(service.title),
                                description = stringResource(service.description),
                                capability = stringResource(service.capability),
                                actionLabel = stringResource(service.action),
                                night = night,
                                modifier = Modifier.weight(1f).testTag("open-${service.route}"),
                                glyph = {
                                    KxServiceGlyph(service.route,
                                        if (night) Color(0xFF99EBFF) else Color(0xFF4420B3),
                                        Modifier.size(28.dp))
                                },
                                onClick = { onNavigate(service.route) },
                            )
                        }
                        repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeBottomBar(
    night: Boolean,
    selected: String,
    modifier: Modifier,
    onNavigate: (String) -> Unit,
) {
    Row(
        modifier.clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(
                if (night) Color(0xFF151338).copy(alpha = .96f)
                else Color(0xFFF7F4FF).copy(alpha = .97f),
                if (night) Color(0xFF1B1746).copy(alpha = .96f)
                else Color(0xFFEFEAFF).copy(alpha = .97f))))
            .border(1.dp, Color.White.copy(alpha = if (night) .28f else .82f),
                RoundedCornerShape(28.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(
            Triple("home", NavKind.HOME, R.string.nav_home),
            Triple("servers", NavKind.SERVERS, R.string.nav_servers),
            Triple("security", NavKind.SECURITY, R.string.nav_security),
            Triple("stats", NavKind.STATS, R.string.nav_stats),
            Triple("settings", NavKind.SETTINGS, R.string.nav_settings),
        ).forEach { (route, kind, label) ->
            val active = route == selected
            val foreground = if (night) Color(0xFFF7F3FF) else Color(0xFF171050)
            Column(
                Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(22.dp))
                    .then(if (active) Modifier.background(Brush.horizontalGradient(
                        listOf(Color(0xFFAD71FF).copy(alpha = .55f),
                            Color(0xFF64EEFF).copy(alpha = .45f)))) else Modifier)
                    .clickable(role = Role.Button, onClick = {
                        if (!active) onNavigate(route)
                    })
                    .padding(vertical = 5.dp)
                    .testTag("nav-$route"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                NavGlyph(kind, foreground, Modifier.size(19.dp))
                Text(stringResource(label), fontSize = 8.5.sp, color = foreground,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1)
            }
        }
    }
}

@Composable
private fun SettingsGlyph(color: Color) {
    Canvas(Modifier.size(21.dp)) {
        val s = size.minDimension
        val c = Offset(size.width / 2, size.height / 2)
        drawCircle(color, s * .34f, c, style = Stroke(s * .08f))
        drawCircle(color, s * .12f, c)
        repeat(6) { i ->
            val a = Math.toRadians(i * 60.0)
            val start = Offset(c.x + kotlin.math.cos(a).toFloat() * s * .40f,
                c.y + kotlin.math.sin(a).toFloat() * s * .40f)
            drawCircle(color, s * .045f, start)
        }
    }
}

@Composable
private fun ServerGlyph(night: Boolean) {
    val accent = if (night) Color(0xFF64EEFF) else Color(0xFF3B2AA8)
    Canvas(Modifier.size(34.dp)) {
        val s = size.minDimension
        val c = Offset(size.width / 2, size.height / 2)
        drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = .32f),
            Color.Transparent), c, s * .5f), s * .5f, c)
        drawRoundRect(Brush.linearGradient(listOf(Color.White.copy(alpha = .7f),
            accent.copy(alpha = .45f))),
            topLeft = Offset(c.x - s * .28f, c.y - s * .22f),
            size = Size(s * .56f, s * .18f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * .05f))
        drawRoundRect(accent.copy(alpha = .8f),
            topLeft = Offset(c.x - s * .28f, c.y + s * .04f),
            size = Size(s * .56f, s * .18f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * .05f),
            style = Stroke(s * .035f))
        drawCircle(accent, s * .035f, Offset(c.x - s * .13f, c.y - s * .13f))
        drawCircle(accent, s * .035f, Offset(c.x + s * .18f, c.y + s * .13f))
    }
}

@Composable
private fun ChevronGlyph(color: Color) {
    Canvas(Modifier.size(16.dp)) {
        val s = size.minDimension
        drawLine(color, Offset(s * .34f, s * .22f), Offset(s * .66f, s * .5f),
            s * .09f, StrokeCap.Round)
        drawLine(color, Offset(s * .66f, s * .5f), Offset(s * .34f, s * .78f),
            s * .09f, StrokeCap.Round)
    }
}

@Composable
private fun MetricGlyph(kind: MetricKind, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val c = Offset(size.width / 2, size.height / 2)
        when (kind) {
            MetricKind.HANDSHAKE -> {
                val p = Path().apply {
                    moveTo(s * .12f, c.y)
                    lineTo(s * .34f, c.y)
                    lineTo(s * .44f, s * .25f)
                    lineTo(s * .58f, s * .78f)
                    lineTo(s * .68f, c.y)
                    lineTo(s * .88f, c.y)
                }
                drawPath(p, color, style = Stroke(s * .09f, cap = StrokeCap.Round))
            }
            MetricKind.RECEIVED, MetricKind.SENT -> {
                val down = kind == MetricKind.RECEIVED
                drawLine(color, Offset(c.x, s * .18f), Offset(c.x, s * .72f),
                    s * .09f, StrokeCap.Round)
                val y1 = if (down) s * .58f else s * .32f
                val y2 = if (down) s * .32f else s * .58f
                drawLine(color, Offset(s * .30f, y2), Offset(c.x, y1),
                    s * .09f, StrokeCap.Round)
                drawLine(color, Offset(s * .70f, y2), Offset(c.x, y1),
                    s * .09f, StrokeCap.Round)
            }
            MetricKind.TUNNEL -> {
                drawCircle(color, s * .34f, c, style = Stroke(s * .08f))
                drawOval(color, Offset(c.x - s * .15f, c.y - s * .34f),
                    Size(s * .30f, s * .68f), style = Stroke(s * .07f))
                drawLine(color, Offset(c.x - s * .34f, c.y), Offset(c.x + s * .34f, c.y),
                    s * .07f)
            }
        }
    }
}

@Composable
private fun NavGlyph(kind: NavKind, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val c = Offset(size.width / 2, size.height / 2)
        val stroke = Stroke(s * .09f, cap = StrokeCap.Round)
        when (kind) {
            NavKind.HOME -> {
                val roof = Path().apply {
                    moveTo(s * .16f, s * .48f)
                    lineTo(c.x, s * .18f)
                    lineTo(s * .84f, s * .48f)
                }
                drawPath(roof, color, style = stroke)
                drawLine(color, Offset(s * .26f, s * .46f), Offset(s * .26f, s * .82f),
                    s * .09f)
                drawLine(color, Offset(s * .74f, s * .46f), Offset(s * .74f, s * .82f),
                    s * .09f)
                drawLine(color, Offset(s * .26f, s * .82f), Offset(s * .74f, s * .82f),
                    s * .09f)
            }
            NavKind.SERVERS -> {
                repeat(2) { i ->
                    val y = s * (.30f + i * .28f)
                    drawRoundRect(color.copy(alpha = .9f),
                        Offset(s * .18f, y), Size(s * .64f, s * .20f),
                        androidx.compose.ui.geometry.CornerRadius(s * .05f),
                        style = Stroke(s * .075f))
                    drawCircle(color, s * .035f, Offset(s * .30f, y + s * .10f))
                }
            }
            NavKind.SECURITY -> {
                val shield = Path().apply {
                    moveTo(c.x, s * .14f)
                    cubicTo(s * .28f, s * .24f, s * .20f, s * .30f, s * .20f, s * .42f)
                    cubicTo(s * .20f, s * .64f, s * .36f, s * .78f, c.x, s * .88f)
                    cubicTo(s * .64f, s * .78f, s * .80f, s * .64f, s * .80f, s * .42f)
                    cubicTo(s * .80f, s * .30f, s * .72f, s * .24f, c.x, s * .14f)
                }
                drawPath(shield, color, style = stroke)
            }
            NavKind.STATS -> {
                listOf(.28f, .46f, .64f, .80f).forEachIndexed { i, x ->
                    val top = s * (.76f - i * .11f)
                    drawLine(color, Offset(s * x, s * .80f), Offset(s * x, top),
                        s * .10f, StrokeCap.Round)
                }
            }
            NavKind.SETTINGS -> {
                drawCircle(color, s * .30f, c, style = stroke)
                drawCircle(color, s * .10f, c)
                repeat(6) { i ->
                    val a = Math.toRadians(i * 60.0)
                    drawCircle(color, s * .045f,
                        Offset(c.x + kotlin.math.cos(a).toFloat() * s * .37f,
                            c.y + kotlin.math.sin(a).toFloat() * s * .37f))
                }
            }
        }
    }
}

private fun clock(context: Context, millis: Long): String {
    val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
    return DateFormat.getTimeInstance(DateFormat.SHORT, locale).format(Date(millis))
}