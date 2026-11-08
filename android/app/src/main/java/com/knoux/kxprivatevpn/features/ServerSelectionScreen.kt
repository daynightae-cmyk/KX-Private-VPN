package com.knoux.kxprivatevpn.features

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knoux.kxprivatevpn.R
import com.knoux.kxprivatevpn.data.config.Profile
import com.knoux.kxprivatevpn.designsystem.Atmosphere
import com.knoux.kxprivatevpn.designsystem.GlassSurface
import com.knoux.kxprivatevpn.designsystem.KxPageHeader
import com.knoux.kxprivatevpn.platform.vpn.VpnIssue
import com.knoux.kxprivatevpn.platform.vpn.VpnPhase
import com.knoux.kxprivatevpn.platform.vpn.VpnSessionOwnership
import com.knoux.kxprivatevpn.platform.vpn.VpnState

/**
 * Real endpoint selection: only configurations the user imported are shown. There is no
 * country/load/latency invented for the reference's fictional fleet; a measured latency is
 * deliberately absent until a real probe is wired.
 */
@Composable
fun ServerSelectionRoute(
    state: ClientState,
    vpn: VpnState,
    model: ClientViewModel,
    night: Boolean,
    onBack: () -> Unit,
    onImport: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    ServerSelectionScreen(
        night = night,
        state = state,
        vpn = vpn,
        query = query,
        onQueryChange = { if (it.length <= 64) query = it },
        onBack = onBack,
        onImport = onImport,
        onSelect = model::select,
        onRemove = model::remove,
        onRetryDisconnect = { model.disconnectVpn() },
    )
}

@Composable
fun ServerSelectionScreen(
    night: Boolean,
    state: ClientState,
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onImport: () -> Unit,
    onSelect: (String) -> Unit,
    onRemove: (String) -> Unit,
    vpn: VpnState = VpnState(),
    onRetryDisconnect: () -> Unit = {},
) {
    val foreground = if (night) Color(0xFFF7F3FF) else Color(0xFF171050)
    val profiles = state.snapshot.profiles
    val searchQuery = query.trim()
    val filtered = profiles.filter {
        searchQuery.isBlank() ||
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.endpoint.contains(searchQuery, ignoreCase = true)
    }
    val selectedId = state.snapshot.selectedId
    val activeId = VpnSessionOwnership.activeProfileId(vpn)
    val locked = VpnSessionOwnership.selectionLocked(vpn)
    val activeName = vpn.session?.profileName
    var pendingSwitch by remember { mutableStateOf<Profile?>(null) }
    var removing by remember { mutableStateOf<Profile?>(null) }
    Box(Modifier.fillMaxSize()) {
        Atmosphere(night, 0f)
        Column(
            Modifier.fillMaxSize().safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .testTag("route-servers"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            KxPageHeader(
                title = stringResource(R.string.servers_title),
                night = night,
                backLabel = stringResource(R.string.back),
                onBack = onBack,
            )
            Text(
                stringResource(R.string.servers_body),
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = foreground.copy(alpha = .72f),
            )
            if (activeId != null || locked) {
                TunnelNotice(vpn, night, foreground, onRetryDisconnect)
            }
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                shape = RoundedCornerShape(50),
                placeholder = { Text(stringResource(R.string.search_servers), fontSize = 13.sp) },
                leadingIcon = { SearchGlyph(foreground) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("server-search"),
            )
            if (profiles.isEmpty()) {
                GlassSurface(night) {
                    Text(stringResource(R.string.server_no_profiles), fontSize = 17.sp,
                        fontWeight = FontWeight.Bold, color = foreground)
                    Text(stringResource(R.string.server_no_profiles_body), fontSize = 12.sp,
                        lineHeight = 17.sp, color = foreground.copy(alpha = .72f))
                }
            } else if (filtered.isEmpty()) {
                GlassSurface(night) {
                    Text(stringResource(R.string.servers_search_empty), fontSize = 13.sp,
                        color = foreground.copy(alpha = .78f),
                        modifier = Modifier.testTag("server-search-empty"))
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    filtered.forEach { profile ->
                        ServerCard(
                            profile = profile,
                            selected = activeId == null && profile.id == selectedId,
                            active = profile.id == activeId,
                            night = night,
                            foreground = foreground,
                            busy = state.busy || locked,
                            onSelect = {
                                when {
                                    // The live server is already in use; tapping its card is a no-op.
                                    profile.id == activeId -> Unit
                                    activeName != null &&
                                        VpnSessionOwnership.requiresStopBeforeSelect(vpn, profile.id) ->
                                        pendingSwitch = profile
                                    else -> onSelect(profile.id)
                                }
                            },
                            onRemove = { removing = profile },
                        )
                    }
                }
            }
            OutlinedButton(
                onClick = onImport,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                    .testTag("server-import"),
            ) {
                Text(stringResource(R.string.server_import), fontWeight = FontWeight.SemiBold,
                    color = if (night) Color(0xFF8FF3FF) else Color(0xFF2A6DE0))
            }
            Text(
                stringResource(R.string.server_card_scope),
                fontSize = 10.sp,
                color = foreground.copy(alpha = .60f),
            )
        }
    }
    pendingSwitch?.let { target ->
        // Resolve copy in the localized screen composition, never through the Activity locale.
        val dialogTitle = stringResource(R.string.server_switch_title)
        val dialogBody = stringResource(
            R.string.server_switch_body, activeName ?: "", target.name,
        )
        val dialogConfirm = stringResource(R.string.server_switch_confirm)
        val dialogCancel = stringResource(R.string.server_switch_cancel)
        AlertDialog(
            onDismissRequest = { pendingSwitch = null },
            title = { Text(dialogTitle) },
            text = { Text(dialogBody, modifier = Modifier.verticalScroll(rememberScrollState())) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingSwitch = null
                        onSelect(target.id)
                    },
                    modifier = Modifier.testTag("server-switch-confirm"),
                ) { Text(dialogConfirm) }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingSwitch = null },
                    modifier = Modifier.testTag("server-switch-cancel"),
                ) { Text(dialogCancel) }
            },
        )
    }
    removing?.let { profile ->
        val dialogTitle = stringResource(R.string.remove_title)
        val baseBody = stringResource(R.string.remove_body)
        val activeWarning = if (profile.id == activeId) {
            stringResource(R.string.remove_active_warning)
        } else null
        val dialogBody = listOfNotNull(baseBody, activeWarning).joinToString("\n\n")
        val dialogConfirm = stringResource(R.string.remove_profile)
        val dialogCancel = stringResource(R.string.cancel)
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text(dialogTitle) },
            text = {
                Text(dialogBody, modifier = Modifier.verticalScroll(rememberScrollState()))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        removing = null
                        onRemove(profile.id)
                    },
                    modifier = Modifier.testTag("server-remove-confirm"),
                ) { Text(dialogConfirm) }
            },
            dismissButton = {
                TextButton(
                    onClick = { removing = null },
                    modifier = Modifier.testTag("server-remove-cancel"),
                ) { Text(dialogCancel) }
            },
        )
    }
}

@Composable
private fun TunnelNotice(
    vpn: VpnState,
    night: Boolean,
    foreground: Color,
    onRetryDisconnect: () -> Unit,
) {
    val stopFailed = vpn.issue == VpnIssue.STOP_FAILED
    val title = when {
        vpn.phase == VpnPhase.CONNECTED && !stopFailed ->
            stringResource(R.string.server_active_tunnel, vpn.session?.profileName ?: "")
        else -> stringResource(R.string.server_selection_locked)
    }
    val body = when {
        stopFailed -> stringResource(R.string.server_stop_failed_locked)
        vpn.phase == VpnPhase.CONNECTING ->
            stringResource(R.string.server_connecting_locked, vpn.session?.profileName ?: "")
        vpn.phase == VpnPhase.DISCONNECTING ->
            stringResource(R.string.server_disconnecting_locked)
        else -> stringResource(R.string.server_active_tunnel_body)
    }
    GlassSurface(night, modifier = Modifier.fillMaxWidth().testTag("server-tunnel-notice")) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = foreground)
        Text(body, fontSize = 11.5.sp, lineHeight = 16.sp,
            color = foreground.copy(alpha = .76f))
        if (stopFailed) {
            TextButton(
                onClick = onRetryDisconnect,
                modifier = Modifier.heightIn(min = 48.dp)
                    .testTag("server-retry-disconnect"),
            ) {
                Text(stringResource(R.string.server_retry_disconnect), fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (night) Color(0xFF8FF3FF) else Color(0xFF2A6DE0))
            }
        }
    }
}

@Composable
private fun ServerCard(
    profile: Profile,
    selected: Boolean,
    active: Boolean,
    night: Boolean,
    foreground: Color,
    busy: Boolean,
    onSelect: () -> Unit,
    onRemove: () -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    val inUse = selected || active
    val accent = if (inUse) Color(0xFF64EEFF) else Color(0xFFAD71FF)
    Column(
        Modifier.fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(
                Color.White.copy(alpha = if (night) .10f else .52f),
                accent.copy(alpha = if (night) .14f else .16f))))
            .border(if (inUse) 1.6.dp else 1.dp,
                if (inUse) Color(0xFF64EEFF).copy(alpha = .9f)
                else Color.White.copy(alpha = if (night) .26f else .78f),
                shape)
            .selectable(selected = inUse, enabled = !busy, role = Role.RadioButton, onClick = onSelect)
            .padding(14.dp)
            .testTag("server-card-${profile.id}"),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ServerCardGlyph(accent, inUse, night)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(profile.name, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    color = foreground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(profile.endpoint, fontSize = 11.sp, color = foreground.copy(alpha = .72f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stringResource(R.string.server_latency_unmeasured), fontSize = 10.sp,
                    color = foreground.copy(alpha = .56f))
            }
            when {
                active -> ActivePill(night)
                selected -> SelectedPill(night)
                else -> {
                    TextButton(
                        onClick = onSelect,
                        enabled = !busy,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("server-select-${profile.id}"),
                    ) {
                        // Explicit colour: the label must stay readable on the dark card even when
                        // the screen is rendered outside the app's MaterialTheme (instrumented runs).
                        Text(stringResource(R.string.server_select), fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (night) Color(0xFF8FF3FF) else Color(0xFF2A6DE0))
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(
                onClick = onRemove,
                enabled = !busy,
                modifier = Modifier.heightIn(min = 48.dp).testTag("server-remove-${profile.id}"),
            ) {
                Text(stringResource(R.string.server_remove), fontSize = 11.sp,
                    color = foreground.copy(alpha = .78f))
            }
        }
    }
}

@Composable
private fun ActivePill(night: Boolean) {
    val accent = if (night) Color(0xFF64EEFF) else Color(0xFF2A6DE0)
    Row(
        Modifier.clip(RoundedCornerShape(50))
            .background(accent.copy(alpha = .18f))
            .border(.8.dp, accent.copy(alpha = .70f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("server-active"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.server_active), fontSize = 9.sp, color = accent,
            fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SelectedPill(night: Boolean) {
    val accent = if (night) Color(0xFF64EEFF) else Color(0xFF2A6DE0)
    Row(
        Modifier.clip(RoundedCornerShape(50))
            .background(accent.copy(alpha = .14f))
            .border(.8.dp, accent.copy(alpha = .55f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("server-selected"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("✓", fontSize = 10.sp, color = accent, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(4.dp))
        Text(stringResource(R.string.server_selected), fontSize = 9.sp, color = accent,
            fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ServerCardGlyph(accent: Color, selected: Boolean, night: Boolean) {
    Canvas(Modifier.size(42.dp)) {
        val s = size.minDimension
        val c = Offset(size.width / 2, size.height / 2)
        drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = .35f),
            Color.Transparent), c, s * .5f), s * .5f, c)
        drawCircle(Color.White.copy(alpha = if (night) .22f else .50f), s * .34f, c)
        drawCircle(accent.copy(alpha = .85f), s * .34f, c, style = Stroke(s * .035f))
        val shield = Path().apply {
            moveTo(c.x, c.y - s * .20f)
            cubicTo(c.x - s * .15f, c.y - s * .14f, c.x - s * .20f, c.y - s * .12f,
                c.x - s * .20f, c.y + s * .02f)
            cubicTo(c.x - s * .20f, c.y + s * .15f, c.x - s * .10f, c.y + s * .22f,
                c.x, c.y + s * .26f)
            cubicTo(c.x + s * .10f, c.y + s * .22f, c.x + s * .20f, c.y + s * .15f,
                c.x + s * .20f, c.y + s * .02f)
            cubicTo(c.x + s * .20f, c.y - s * .12f, c.x + s * .15f, c.y - s * .14f,
                c.x, c.y - s * .20f)
        }
        drawPath(shield, accent, style = Stroke(s * .04f))
        if (selected) drawCircle(accent, s * .055f, Offset(c.x, c.y + s * .02f))
    }
}

@Composable
private fun SearchGlyph(color: Color) {
    Canvas(Modifier.size(18.dp)) {
        val s = size.minDimension
        drawCircle(color, s * .30f, Offset(s * .42f, s * .42f), style = Stroke(s * .10f))
        drawLine(color, Offset(s * .64f, s * .64f), Offset(s * .88f, s * .88f),
            s * .10f, StrokeCap.Round)
    }
}