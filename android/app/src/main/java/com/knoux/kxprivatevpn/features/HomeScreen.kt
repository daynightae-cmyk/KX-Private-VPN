package com.knoux.kxprivatevpn.features

import android.content.Context
import android.text.format.Formatter
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knoux.kxprivatevpn.R
import com.knoux.kxprivatevpn.data.config.ProfileSnapshot
import com.knoux.kxprivatevpn.designsystem.KxBrandHeader
import com.knoux.kxprivatevpn.designsystem.KxBottomNav
import com.knoux.kxprivatevpn.designsystem.KxGlassPanel
import com.knoux.kxprivatevpn.designsystem.KxGradientAction
import com.knoux.kxprivatevpn.designsystem.KxIconKind
import com.knoux.kxprivatevpn.designsystem.KxLineIcon
import com.knoux.kxprivatevpn.designsystem.KxReferenceAtmosphere
import com.knoux.kxprivatevpn.designsystem.KxReferenceHeader
import com.knoux.kxprivatevpn.designsystem.KxReferenceTheme
import com.knoux.kxprivatevpn.designsystem.KxSectionHeader
import com.knoux.kxprivatevpn.designsystem.KxServiceCard
import com.knoux.kxprivatevpn.designsystem.KxServiceGlyph
import com.knoux.kxprivatevpn.designsystem.KxStatusPill
import com.knoux.kxprivatevpn.designsystem.KxTitleBlock
import com.knoux.kxprivatevpn.designsystem.KxOrb
import com.knoux.kxprivatevpn.designsystem.RepairGlyph
import com.knoux.kxprivatevpn.designsystem.kxCyan
import com.knoux.kxprivatevpn.designsystem.kxInk
import com.knoux.kxprivatevpn.designsystem.kxMuted
import com.knoux.kxprivatevpn.designsystem.kxRose
import com.knoux.kxprivatevpn.designsystem.kxViolet
import com.knoux.kxprivatevpn.designsystem.repairAccent
import com.knoux.kxprivatevpn.launch.IntroTimeline
import com.knoux.kxprivatevpn.platform.repair.RepairCatalog
import com.knoux.kxprivatevpn.platform.repair.RepairServiceId
import com.knoux.kxprivatevpn.platform.usage.UsageRecord
import com.knoux.kxprivatevpn.platform.vpn.VpnIssue
import com.knoux.kxprivatevpn.platform.vpn.VpnPhase
import com.knoux.kxprivatevpn.platform.vpn.VpnSessionOwnership
import com.knoux.kxprivatevpn.platform.vpn.VpnState
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/**
 * Consumer-first Home reproduced from owner reference 14.
 *
 * Every value comes from the selected profile, the live WireGuard session, Android's formatter
 * or the locally recorded usage history. Disconnected and unmeasured slots show an em dash;
 * no server location, latency, load or protection claim is invented. The full discovery
 * catalog and session telemetry stay below the reference viewport so existing destinations
 * and readability contracts remain available.
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
    history: List<UsageRecord> = emptyList(),
) {
    KxReferenceTheme(night) {
        val context = LocalContext.current
        val selected = snapshot.profiles.firstOrNull { it.id == snapshot.selectedId }
        // While this process owns a tunnel, the live session server is the truth. The saved
        // selection only matters for the next connection.
        val activeSession = if (VpnSessionOwnership.ownsTunnel(vpn)) vpn.session else null
        Box(Modifier.fillMaxSize()) {
            KxReferenceAtmosphere(night)
            Column(Modifier.fillMaxSize()) {
                Column(
                    Modifier.weight(1f).fillMaxWidth().safeDrawingPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp)
                        .testTag("route-home"),
                    verticalArrangement = Arrangement.spacedBy(13.dp),
                ) {
                    HomeHeader(night, onNavigate)
                    KxTitleBlock(
                        title = stringResource(R.string.home),
                        subtitle = stringResource(R.string.home_reference_subtitle),
                        night = night,
                        // The same word labels the Home tab. Announce the heading once for
                        // accessibility without creating a second identical Text node for the
                        // navigation contract.
                        announceTitleAsDescription = true,
                    )
                    HomeHero(night, activeSession?.profileName ?: selected?.name, busy, vpn,
                        onConnect, onDisconnect)
                    SessionPanel(
                        night = night,
                        active = activeSession != null,
                        name = activeSession?.profileName ?: selected?.name,
                        startedAtMillis = activeSession?.startedAtMillis,
                        rxBytes = activeSession?.rxBytes,
                        context = context,
                        onOpenServers = { onNavigate("servers") },
                        onOpenSession = onSession,
                    )
                    QuickActions(night, onNavigate)
                    RecentActivity(night, activeSession, history, context, onNavigate)
                    SessionTelemetry(night, vpn)
                    KxCareCard(night, onNavigate)
                    ToolsGrid(night, onNavigate)
                    vpn.issue?.let { issue ->
                        KxGlassPanel(night, radius = 18.dp) {
                            Text(
                                vpnIssueText(issue),
                                color = kxInk(night),
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                modifier = Modifier.testTag("vpn-issue"),
                            )
                            TextButton(
                                onClick = if (issue == VpnIssue.STOP_FAILED) onDisconnect else onDismissIssue,
                                modifier = Modifier.heightIn(min = 48.dp).testTag("vpn-dismiss-issue"),
                            ) {
                                Text(stringResource(if (issue == VpnIssue.STOP_FAILED) R.string.retry else R.string.dismiss))
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                KxBottomNav(
                    night = night,
                    selected = "home",
                    toolsTab = false,
                    modifier = Modifier.fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    onNavigate = onNavigate,
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(night: Boolean, onNavigate: (String) -> Unit) {
    KxReferenceHeader(
        night = night,
        actionLabel = stringResource(R.string.settings),
        tag = "open-settings",
        icon = KxIconKind.GEAR,
        onClick = { onNavigate("settings") },
    )
}

@Composable
private fun HomeHero(
    night: Boolean,
    profileName: String?,
    busy: Boolean,
    vpn: VpnState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val stopFailed = vpn.issue == VpnIssue.STOP_FAILED
    val connected = vpn.phase == VpnPhase.CONNECTED
    val status = when {
        stopFailed -> stringResource(R.string.unknown_value)
        connected -> stringResource(R.string.connected)
        vpn.phase == VpnPhase.CONNECTING -> stringResource(R.string.connecting)
        vpn.phase == VpnPhase.DISCONNECTING -> stringResource(R.string.disconnecting)
        else -> stringResource(R.string.disconnected)
    }
    val orbEnabled = !busy && (stopFailed || when (vpn.phase) {
        VpnPhase.CONNECTED -> true
        VpnPhase.IDLE, VpnPhase.ERROR -> profileName != null
        else -> false
    })
    val actionLabel = stringResource(if (connected || stopFailed) R.string.disconnect else R.string.connect)
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Box(Modifier.fillMaxWidth().heightIn(min = 158.dp), contentAlignment = Alignment.TopCenter) {
            KxOrb(
                progress = 1f,
                frame = IntroTimeline.frame(1f),
                modifier = Modifier.size(122.dp)
                    .testTag("vpn-orb-action")
                    .clickable(enabled = orbEnabled, role = Role.Button, onClickLabel = actionLabel) {
                        if (connected || stopFailed) onDisconnect() else onConnect()
                    },
            )
            KxStatusPill(
                label = status,
                night = night,
                connected = connected && !stopFailed,
                icon = if (connected || stopFailed) KxIconKind.SHIELD_CHECK else null,
                modifier = Modifier.align(Alignment.BottomCenter)
                    .then(if (connected && !stopFailed) Modifier.testTag("vpn-connected") else Modifier),
            )
        }
        when {
            connected -> Unit
            vpn.phase == VpnPhase.CONNECTING || vpn.phase == VpnPhase.DISCONNECTING ->
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 34.dp))
            else -> Text(
                stringResource(
                    when {
                        profileName == null -> R.string.home_no_profile_hint
                        else -> R.string.home_connect_hint
                    },
                ),
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = kxMuted(night),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 22.dp),
            )
        }
        when {
            connected || stopFailed -> KxGradientAction(
                label = stringResource(if (stopFailed) R.string.retry else R.string.disconnect),
                night = night,
                enabled = !busy,
                tag = "vpn-disconnect",
                icon = KxIconKind.POWER,
            ) { onDisconnect() }
            else -> KxGradientAction(
                label = stringResource(R.string.connect),
                night = night,
                enabled = profileName != null && !busy,
                tag = "vpn-connect",
                icon = KxIconKind.POWER,
            ) { onConnect() }
        }
    }
}

@Composable
private fun SessionPanel(
    night: Boolean,
    active: Boolean,
    name: String?,
    startedAtMillis: Long?,
    rxBytes: Long?,
    context: Context,
    onOpenServers: () -> Unit,
    onOpenSession: () -> Unit,
) {
    val label = stringResource(if (active) R.string.active_server else R.string.selected_server).uppercase()
    val duration = remember(startedAtMillis) { durationLabel(context, startedAtMillis) }
    KxGlassPanel(
        night = night,
        modifier = Modifier
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.servers), onClick = onOpenServers)
            .testTag("open-servers"),
        radius = 20.dp,
        padding = PaddingValues(horizontal = 8.dp, vertical = 11.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SessionCell(
                label = label,
                value = name ?: stringResource(R.string.no_profiles),
                night = night,
                valueTag = "home-profile",
                labelTag = "home-server-label",
                modifier = Modifier.weight(1.25f),
            )
            SessionDivider(night)
            SessionCell(
                label = stringResource(R.string.home_session_received),
                value = rxBytes?.let { formatBytes(context, it) } ?: stringResource(R.string.unknown_value),
                night = night,
                modifier = Modifier.weight(1f),
            )
            SessionDivider(night)
            SessionCell(
                label = stringResource(R.string.home_session_duration),
                value = duration,
                night = night,
                modifier = Modifier.weight(.90f),
            )
            Box(
                Modifier.size(48.dp)
                    .clickable(role = Role.Button, onClickLabel = stringResource(R.string.session_details)) {
                        onOpenSession()
                    }
                    .testTag("open-session"),
                contentAlignment = Alignment.Center,
            ) {
                KxForwardChevron(kxMuted(night), 14.dp)
            }
        }
    }
}

@Composable
private fun SessionCell(
    label: String,
    value: String,
    night: Boolean,
    modifier: Modifier,
    valueTag: String? = null,
    labelTag: String? = null,
) {
    Column(
        modifier.padding(horizontal = 5.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            label,
            color = kxMuted(night),
            fontSize = 8.sp,
            lineHeight = 11.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = if (labelTag != null) Modifier.testTag(labelTag) else Modifier,
        )
        Text(
            value,
            color = kxInk(night),
            fontSize = 13.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = if (valueTag != null) Modifier.testTag(valueTag) else Modifier,
        )
    }
}

@Composable
private fun SessionDivider(night: Boolean) {
    Box(Modifier.width(1.dp).heightIn(min = 30.dp).background(Color.White.copy(alpha = if (night) .16f else .48f)))
}

@Composable
private fun QuickActions(night: Boolean, onNavigate: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            KxLineIcon(KxIconKind.BOLT, kxInk(night), Modifier.size(19.dp))
            Spacer(Modifier.width(7.dp))
            Text(
                stringResource(R.string.home_quick_actions),
                color = kxInk(night),
                fontSize = 16.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Row(
                Modifier.clip(RoundedCornerShape(50))
                    .clickable(role = Role.Button, onClickLabel = stringResource(R.string.home_view_all)) {
                        onNavigate("tools")
                    }
                    .padding(horizontal = 6.dp, vertical = 6.dp)
                    .testTag("home-view-all"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.home_view_all), color = kxMuted(night), fontSize = 10.sp)
                Spacer(Modifier.width(2.dp))
                KxForwardChevron(kxMuted(night), 11.dp)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            QuickActionCard(night, KxIconKind.LINK, kxViolet(night),
                stringResource(R.string.home_quick_import_title), stringResource(R.string.home_quick_import_body),
                "quick-import", Modifier.weight(1f)) { onNavigate("import") }
            QuickActionCard(night, KxIconKind.SHIELD_CHECK, kxCyan(night),
                stringResource(R.string.home_quick_shield_title), stringResource(R.string.home_quick_shield_body),
                "quick-shield", Modifier.weight(1f)) { onNavigate("shield") }
            QuickActionCard(night, KxIconKind.SPLIT, kxCyan(night),
                stringResource(R.string.home_quick_split_title), stringResource(R.string.home_quick_split_body),
                "quick-split", Modifier.weight(1f)) { onNavigate("split") }
            QuickActionCard(night, KxIconKind.SERVER, kxRose(night),
                stringResource(R.string.home_quick_servers_title), stringResource(R.string.home_quick_servers_body),
                "quick-servers", Modifier.weight(1f)) { onNavigate("servers") }
        }
    }
}

@Composable
private fun QuickActionCard(
    night: Boolean,
    icon: KxIconKind,
    accent: Color,
    title: String,
    body: String,
    tag: String,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(15.dp)
    val label = stringResource(R.string.home_quick_open, title)
    Column(
        modifier.clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = if (night) .09f else .48f),
                        accent.copy(alpha = if (night) .16f else .12f),
                    ),
                ),
            )
            .border(1.dp, Brush.linearGradient(listOf(
                Color.White.copy(alpha = if (night) .35f else .86f),
                accent.copy(alpha = if (night) .40f else .30f),
            )), shape)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .heightIn(min = 60.dp)
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        KxLineIcon(icon, if (night) accent else accent.copy(alpha = .92f), Modifier.size(18.dp))
        Text(
            title,
            color = kxInk(night),
            fontSize = 9.5.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                body,
                color = kxMuted(night),
                fontSize = 7.5.sp,
                lineHeight = 10.sp,
                textAlign = TextAlign.Center,
            )
            KxForwardChevron(kxMuted(night), 8.dp)
        }
    }
}

private data class ActivityRow(
    val title: String,
    val detail: String,
    val time: String,
    val icon: KxIconKind,
    val accent: Color,
)

/** A forward chevron follows the reading direction: left in RTL, right in LTR. */
@Composable
private fun KxForwardChevron(tint: Color, size: Dp) {
    KxLineIcon(
        if (LocalLayoutDirection.current == LayoutDirection.Rtl) KxIconKind.BACK else KxIconKind.FORWARD,
        tint,
        Modifier.size(size),
    )
}

@Composable
private fun RecentActivity(
    night: Boolean,
    live: com.knoux.kxprivatevpn.platform.vpn.VpnSession?,
    history: List<UsageRecord>,
    context: Context,
    onNavigate: (String) -> Unit,
) {
    val rows = remember(live, history) {
        val result = mutableListOf<ActivityRow>()
        if (live != null) {
            result += ActivityRow(
                title = context.getString(R.string.home_activity_active),
                detail = context.getString(R.string.home_activity_live_detail, live.profileName),
                time = live.startedAtMillis?.let { clock(context, it) }
                    ?: context.getString(R.string.unknown_value),
                icon = KxIconKind.LINK,
                accent = Color(0xFF5BE8AE),
            )
        }
        history.sortedByDescending { it.startedAtMillis }.take(3 - result.size).forEach { record ->
            result += ActivityRow(
                title = context.getString(R.string.home_activity_ended),
                detail = context.getString(
                    R.string.home_activity_traffic,
                    formatBytes(context, record.rxBytes),
                    formatBytes(context, record.txBytes),
                ),
                time = clock(context, record.endedAtMillis),
                icon = KxIconKind.POWER,
                accent = Color(0xFFFF8AD8),
            )
        }
        result
    }
    KxGlassPanel(night, radius = 20.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                KxLineIcon(KxIconKind.CLOCK, kxInk(night), Modifier.size(19.dp))
                Spacer(Modifier.width(7.dp))
                Text(
                    stringResource(R.string.home_recent_activity),
                    color = kxInk(night),
                    fontSize = 16.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Row(
                    Modifier.clip(RoundedCornerShape(50))
                        .clickable(role = Role.Button, onClickLabel = stringResource(R.string.home_view_all)) {
                            onNavigate("stats")
                        }
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                        .testTag("home-activity-all"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.home_view_all), color = kxMuted(night), fontSize = 10.sp)
                    Spacer(Modifier.width(2.dp))
                    KxForwardChevron(kxMuted(night), 11.dp)
                }
            }
            Text(
                stringResource(R.string.home_recent_subtitle),
                color = kxMuted(night),
                fontSize = 10.sp,
                lineHeight = 15.sp,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        if (rows.isEmpty()) {
            Text(
                stringResource(R.string.home_recent_empty),
                color = kxMuted(night),
                fontSize = 11.sp,
                lineHeight = 17.sp,
                modifier = Modifier.testTag("home-activity-empty"),
            )
        } else {
            rows.forEachIndexed { index, row ->
                if (index > 0) {
                    Box(Modifier.fillMaxWidth().heightIn(min = 1.dp)
                        .background(Color.White.copy(alpha = if (night) .12f else .44f)))
                }
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Box(
                        Modifier.size(34.dp).clip(RoundedCornerShape(11.dp))
                            .background(row.accent.copy(alpha = if (night) .18f else .12f))
                            .border(1.dp, row.accent.copy(alpha = if (night) .42f else .28f),
                                RoundedCornerShape(11.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        KxLineIcon(row.icon, row.accent, Modifier.size(17.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            row.title,
                            color = kxInk(night),
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            row.detail,
                            color = kxMuted(night),
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                        )
                    }
                    Text(
                        row.time,
                        color = kxMuted(night),
                        fontSize = 9.5.sp,
                        lineHeight = 14.sp,
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionTelemetry(night: Boolean, vpn: VpnState) {
    val context = LocalContext.current
    val session = if (VpnSessionOwnership.ownsTunnel(vpn)) vpn.session else null
    val handshake = session?.latestHandshakeMillis?.let { clock(context, it) }
        ?: stringResource(R.string.unknown_value)
    val received = session?.rxBytes?.let { formatBytes(context, it) }
        ?: stringResource(R.string.unknown_value)
    val sent = session?.txBytes?.let { formatBytes(context, it) }
        ?: stringResource(R.string.unknown_value)
    val tunnelIp = session?.addresses?.firstOrNull()?.substringBefore('/')
        ?: stringResource(R.string.unknown_value)
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        KxGlassPanel(night, radius = 20.dp) {
            Text(
                stringResource(R.string.home_session_telemetry),
                color = kxInk(night),
                fontSize = 16.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Bold,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                MetricTile(R.string.metric_handshake, handshake, MetricKind.HANDSHAKE, night,
                    "metric-handshake", Modifier.weight(1f))
                MetricTile(R.string.metric_received, received, MetricKind.RECEIVED, night,
                    "metric-received", Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                MetricTile(R.string.metric_sent, sent, MetricKind.SENT, night,
                    "metric-sent", Modifier.weight(1f))
                MetricTile(R.string.metric_tunnel_ip, tunnelIp, MetricKind.TUNNEL, night,
                    "metric-tunnel", Modifier.weight(1f))
            }
            Text(
                stringResource(R.string.home_session_telemetry_scope),
                color = kxMuted(night),
                fontSize = 10.sp,
                lineHeight = 15.sp,
            )
        }
    }
}

private enum class MetricKind { HANDSHAKE, RECEIVED, SENT, TUNNEL }

@Composable
private fun MetricTile(
    label: Int,
    value: String,
    kind: MetricKind,
    night: Boolean,
    tag: String,
    modifier: Modifier,
) {
    val accent = when (kind) {
        MetricKind.HANDSHAKE -> kxViolet(night)
        MetricKind.RECEIVED -> kxCyan(night)
        MetricKind.SENT -> kxRose(night)
        MetricKind.TUNNEL -> Color(0xFFB9E9FF)
    }
    val tone = if (night) accent else Color(0xFF3B2AA8)
    Column(
        modifier.clip(RoundedCornerShape(15.dp))
            .background(Brush.linearGradient(listOf(
                Color.White.copy(alpha = if (night) .09f else .46f),
                accent.copy(alpha = if (night) .12f else .16f),
            )))
            .border(1.dp, Color.White.copy(alpha = if (night) .22f else .72f),
                RoundedCornerShape(15.dp))
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MetricGlyph(kind, tone, Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(label), fontSize = 9.5.sp, lineHeight = 13.sp,
                color = kxMuted(night), fontWeight = FontWeight.Medium)
        }
        Text(
            value,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Bold,
            color = kxInk(night),
            modifier = Modifier.testTag(tag),
        )
    }
}

@Composable
private fun MetricGlyph(kind: MetricKind, color: Color, modifier: Modifier) {
    androidx.compose.foundation.Canvas(modifier) {
        val s = size.minDimension
        val c = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
        when (kind) {
            MetricKind.HANDSHAKE -> {
                val p = androidx.compose.ui.graphics.Path().apply {
                    moveTo(s * .12f, c.y)
                    lineTo(s * .34f, c.y)
                    lineTo(s * .44f, s * .25f)
                    lineTo(s * .58f, s * .78f)
                    lineTo(s * .68f, c.y)
                    lineTo(s * .88f, c.y)
                }
                drawPath(p, color, style = androidx.compose.ui.graphics.drawscope.Stroke(
                    s * .09f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            }
            MetricKind.RECEIVED, MetricKind.SENT -> {
                val down = kind == MetricKind.RECEIVED
                drawLine(color, androidx.compose.ui.geometry.Offset(c.x, s * .18f),
                    androidx.compose.ui.geometry.Offset(c.x, s * .72f),
                    s * .09f, androidx.compose.ui.graphics.StrokeCap.Round)
                val y1 = if (down) s * .58f else s * .32f
                val y2 = if (down) s * .32f else s * .58f
                drawLine(color, androidx.compose.ui.geometry.Offset(s * .30f, y2),
                    androidx.compose.ui.geometry.Offset(c.x, y1),
                    s * .09f, androidx.compose.ui.graphics.StrokeCap.Round)
                drawLine(color, androidx.compose.ui.geometry.Offset(s * .70f, y2),
                    androidx.compose.ui.geometry.Offset(c.x, y1),
                    s * .09f, androidx.compose.ui.graphics.StrokeCap.Round)
            }
            MetricKind.TUNNEL -> {
                drawCircle(color, s * .34f, c,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(s * .08f))
                drawOval(color, androidx.compose.ui.geometry.Offset(c.x - s * .15f, c.y - s * .34f),
                    androidx.compose.ui.geometry.Size(s * .30f, s * .68f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(s * .07f))
                drawLine(color, androidx.compose.ui.geometry.Offset(c.x - s * .34f, c.y),
                    androidx.compose.ui.geometry.Offset(c.x + s * .34f, c.y), s * .07f)
            }
        }
    }
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

private fun durationLabel(context: Context, startedAtMillis: Long?): String {
    if (startedAtMillis == null || startedAtMillis <= 0L) {
        return context.getString(R.string.unknown_value)
    }
    val elapsed = (System.currentTimeMillis() - startedAtMillis).coerceAtLeast(0L)
    val totalMinutes = elapsed / 60_000L
    return when {
        elapsed < 60_000L -> context.getString(R.string.home_duration_seconds, elapsed / 1_000L)
        totalMinutes < 60L -> context.getString(R.string.home_duration_minutes, totalMinutes)
        else -> context.getString(
            R.string.home_duration_hours,
            totalMinutes / 60L,
            totalMinutes % 60L,
        )
    }
}

private fun formatBytes(context: Context, value: Long): String =
    Formatter.formatFileSize(context, value.coerceAtLeast(0L))

private fun clock(context: Context, millis: Long): String {
    val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
    return DateFormat.getTimeInstance(DateFormat.SHORT, locale).format(Date(millis))
}
