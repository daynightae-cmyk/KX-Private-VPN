package com.knoux.kxprivatevpn.features

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.knoux.kxprivatevpn.platform.vpn.AdvancedNetworkSettings
import com.knoux.kxprivatevpn.platform.vpn.AdvancedNetworkValidator
import com.knoux.kxprivatevpn.platform.vpn.DnsMode
import kotlin.math.roundToInt

/**
 * Everything the screen renders. [settings] is the saved device intent, [applied] the intent
 * the backend last read. Both come from the view model; the composable never invents state.
 */
data class AdvancedNetworkUiState(
    val settings: AdvancedNetworkSettings = AdvancedNetworkSettings(),
    val profileMtu: Int? = null,
    val profileKeepalive: Int? = null,
    val profileHasDns: Boolean = false,
    val hasProfile: Boolean = false,
    val loading: Boolean = true,
    val loadFailed: Boolean = false,
    val saving: Boolean = false,
    val saveFailed: Boolean = false,
    val connected: Boolean = false,
    val dirty: Boolean = false,
    val applied: AdvancedNetworkSettings? = null,
)

/** Real screen route: stored settings plus the selected profile baseline on entry. */
@Composable
internal fun AdvancedNetworkRoute(
    model: ClientViewModel,
    night: Boolean,
    onBack: () -> Unit,
    onReconnect: () -> Unit,
) {
    val state by model.advancedState.collectAsStateWithLifecycle()
    var reloadKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(reloadKey) { model.refreshAdvanced() }
    AdvancedNetworkScreen(
        night = night,
        state = state,
        onBack = onBack,
        onRefresh = { reloadKey++ },
        onMtuChange = model::setAdvancedMtu,
        onDnsModeChange = model::setAdvancedDnsMode,
        onDnsHostsChange = model::setAdvancedDnsHosts,
        onKeepaliveChange = model::setAdvancedKeepalive,
        onReset = model::resetAdvancedSettings,
        onReconnect = onReconnect,
    )
}

@Composable
fun AdvancedNetworkScreen(
    night: Boolean,
    state: AdvancedNetworkUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onMtuChange: (Int?) -> Unit,
    onDnsModeChange: (DnsMode) -> Unit,
    onDnsHostsChange: (List<String>) -> Unit,
    onKeepaliveChange: (Int?) -> Unit,
    onReset: () -> Unit,
    onReconnect: () -> Unit,
) {
    val muted = if (night) Color(0xFFC8C5E9) else Color(0xFF4B477F)
    val titleColor = if (night) Color(0xFFF6F1FF) else Color(0xFF171050)
    var dnsExpanded by rememberSaveable { mutableStateOf(false) }
    var resetOpen by rememberSaveable { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().testTag("adv-screen")) {
        Atmosphere(night, 0f)
        Column(
            Modifier.fillMaxSize().safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .testTag("adv-scroll"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                KxPageHeader(
                    title = stringResource(R.string.adv_title),
                    night = night,
                    backLabel = stringResource(R.string.back),
                    onBack = onBack,
                )
                ThreeDotButton(
                    night = night,
                    muted = muted,
                    expanded = resetOpen,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 6.dp)
                        .testTag("adv-reset-menu"),
                ) { resetOpen = !resetOpen }
            }
            when {
                state.loading -> LoadingPanel(muted)
                state.loadFailed -> LoadFailedPanel(night, muted, onRefresh)
                else -> {
                    if (resetOpen) {
                        GlassSurface(night, Modifier.testTag("adv-reset-panel"), radius = 22.dp,
                            contentPadding = PaddingValues(horizontal = 13.dp, vertical = 11.dp)) {
                            Text(stringResource(R.string.adv_reset_hint), color = muted,
                                fontSize = 11.5.sp, lineHeight = 15.sp)
                            OutlinedButton(onClick = {
                                resetOpen = false
                                onReset()
                            }, enabled = !state.saving,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                    .testTag("adv-reset")) {
                                Text(stringResource(R.string.adv_reset))
                            }
                        }
                    }
                    if (state.saving) LinearSaving(muted)
                    if (state.saveFailed) {
                        Text(stringResource(R.string.adv_save_failed), color = Color(0xFFC23A5E),
                            fontSize = 12.sp, modifier = Modifier.padding(horizontal = 24.dp)
                                .testTag("adv-save-failed"))
                    }
                    HeroCard(night, muted, titleColor)
                    SectionTitle(stringResource(R.string.adv_section_protocol), titleColor)
                    ProtocolRow(night, muted)
                    SectionTitle(stringResource(R.string.adv_section_controls), titleColor)
                    MtuCard(night, muted, titleColor, state, onMtuChange)
                    DnsCard(night, muted, titleColor, state, dnsExpanded,
                        onToggle = { dnsExpanded = !dnsExpanded },
                        onModeChange = onDnsModeChange,
                        onHostsChange = onDnsHostsChange)
                    UnavailableToggleCard(
                        night = night, muted = muted, titleColor = titleColor,
                        glyph = CardGlyphKind.MASK,
                        title = stringResource(R.string.adv_obfuscation_title),
                        body = stringResource(R.string.adv_obfuscation_subtitle),
                        tag = "adv-obfuscation", switchTag = "adv-obfuscation-switch")
                    UnavailableToggleCard(
                        night = night, muted = muted, titleColor = titleColor,
                        glyph = CardGlyphKind.ROTATE,
                        title = stringResource(R.string.adv_porthopping_title),
                        body = stringResource(R.string.adv_porthopping_subtitle),
                        tag = "adv-porthopping", switchTag = "adv-porthopping-switch")
                    KeepaliveCard(night, muted, titleColor, state, onKeepaliveChange)
                    if (state.connected) {
                        ReconnectPanel(night, muted, titleColor, state, onReconnect)
                    }
                    Notes(night, muted, state)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LoadingPanel(muted: Color) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 18.dp).testTag("adv-loading"),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(Modifier.size(20.dp), color = muted, strokeWidth = 2.dp)
        Spacer(Modifier.width(10.dp))
        Text(stringResource(R.string.adv_loading), color = muted, fontSize = 12.sp)
    }
}

@Composable
private fun LoadFailedPanel(night: Boolean, muted: Color, onRefresh: () -> Unit) {
    GlassSurface(night, Modifier.testTag("adv-load-failed"), radius = 22.dp) {
        Text(stringResource(R.string.adv_load_failed), color = muted,
            fontSize = 12.sp, lineHeight = 17.sp)
        OutlinedButton(onClick = onRefresh,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .testTag("adv-reload")) {
            Text(stringResource(R.string.adv_refresh))
        }
    }
}

@Composable
private fun LinearSaving(muted: Color) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.End) {
        CircularProgressIndicator(Modifier.size(14.dp), color = muted, strokeWidth = 2.dp)
    }
}

@Composable
private fun HeroCard(night: Boolean, muted: Color, titleColor: Color) {
    GlassSurface(
        night, Modifier.testTag("adv-hero"), radius = 30.dp,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CardGlyphPanel(CardGlyphKind.STACK, titleColor)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.adv_hero_title), color = titleColor,
                    fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.adv_hero_body), color = muted,
                    fontSize = 11.5.sp, lineHeight = 16.sp)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, titleColor: Color) {
    Text(text, color = titleColor, fontSize = 17.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 24.dp))
}

@Composable
private fun ProtocolRow(night: Boolean, muted: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ProtocolOption(
            night = night, muted = muted, selected = true, enabled = true,
            label = stringResource(R.string.adv_protocol_wireguard),
            glyph = CardGlyphKind.LINK,
            modifier = Modifier.weight(1f).testTag("adv-protocol-wireguard"),
        )
        ProtocolOption(
            night = night, muted = muted, selected = false, enabled = false,
            label = stringResource(R.string.adv_protocol_openvpn),
            glyph = CardGlyphKind.DOME, unavailable = true,
            modifier = Modifier.weight(1f).testTag("adv-protocol-openvpn"),
        )
        ProtocolOption(
            night = night, muted = muted, selected = false, enabled = false,
            label = stringResource(R.string.adv_protocol_automatic),
            glyph = CardGlyphKind.INFINITY, unavailable = true,
            modifier = Modifier.weight(1f).testTag("adv-protocol-automatic"),
        )
    }
}

@Composable
private fun ProtocolOption(
    night: Boolean,
    muted: Color,
    selected: Boolean,
    enabled: Boolean,
    label: String,
    glyph: CardGlyphKind,
    modifier: Modifier = Modifier,
    unavailable: Boolean = false,
) {
    // WireGuard is the only implemented protocol, so it is always the selected one.
    val foreground = if (selected) Color.White else muted
    val alpha = if (enabled) 1f else .55f
    Column(
        modifier.heightIn(min = 88.dp).clip(RoundedCornerShape(22.dp))
            .then(
                if (selected) Modifier.background(Brush.horizontalGradient(
                    listOf(Color(0xFF8B5CFF), Color(0xFF4F7DFF), Color(0xFF64E9FF))))
                else Modifier.background(Color.White.copy(alpha = if (night) .06f else .42f),
                    RoundedCornerShape(22.dp)),
            )
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton,
                onClick = {})
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
            CardGlyph({ CanvasGlyphs.Glyph(glyph, foreground.copy(alpha = alpha)) },
                Modifier.size(20.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = foreground.copy(alpha = alpha), fontSize = 13.sp,
            fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        Text(if (unavailable) stringResource(R.string.adv_protocol_unavailable) else " ",
            fontSize = 9.5.sp, color = muted.copy(alpha = .85f),
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ThreeDotButton(
    night: Boolean,
    muted: Color,
    expanded: Boolean,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit,
) {
    val label = stringResource(R.string.adv_reset_menu)
    val shape = CircleShape
    Box(
        modifier.size(44.dp).clip(shape)
            .background(
                Brush.linearGradient(
                    if (night) listOf(Color.White.copy(alpha = .14f),
                        Color(0xFF6B62E8).copy(alpha = .22f))
                    else listOf(Color.White.copy(alpha = .62f),
                        Color(0xFFB6C9FF).copy(alpha = .34f)),
                ),
            )
            .border(1.dp,
                if (expanded) muted else Color.White.copy(alpha = if (night) .38f else .85f),
                shape)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onOpen)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) { CardGlyph({ CanvasGlyphs.Dots(muted) }, Modifier.size(18.dp)) }
}

@Composable
private fun MtuCard(
    night: Boolean,
    muted: Color,
    titleColor: Color,
    state: AdvancedNetworkUiState,
    onMtuChange: (Int?) -> Unit,
) {
    val baseline = state.settings.mtuOverride
        ?: state.profileMtu ?: AdvancedNetworkValidator.MTU_MAX
    var draft by remember(baseline) { mutableFloatStateOf(baseline.toFloat()) }
    GlassSurface(night, Modifier.testTag("adv-mtu"), radius = 22.dp,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CardGlyphPanel(CardGlyphKind.EXPAND, titleColor)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.adv_mtu_title), color = titleColor,
                    fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.adv_mtu_subtitle), color = muted,
                    fontSize = 11.sp, lineHeight = 15.sp)
            }
            Spacer(Modifier.width(10.dp))
            Text(
                state.settings.mtuOverride?.toString()
                    ?: stringResource(R.string.adv_mtu_profile),
                color = titleColor, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("adv-mtu-value"))
        }
        Spacer(Modifier.height(8.dp))
        Slider(
            value = draft.coerceIn(
                AdvancedNetworkValidator.MTU_MIN.toFloat(),
                AdvancedNetworkValidator.MTU_MAX.toFloat()),
            onValueChange = { draft = it },
            onValueChangeFinished = {
                onMtuChange(
                    AdvancedNetworkValidator.MTU_MIN +
                        (((draft - AdvancedNetworkValidator.MTU_MIN) / 10f).roundToInt() * 10)
                            .coerceIn(0, AdvancedNetworkValidator.MTU_MAX
                                - AdvancedNetworkValidator.MTU_MIN))
            },
            valueRange = AdvancedNetworkValidator.MTU_MIN.toFloat()
                ..AdvancedNetworkValidator.MTU_MAX.toFloat(),
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth().testTag("adv-mtu-slider"),
        )
    }
}

@Composable
private fun DnsCard(
    night: Boolean,
    muted: Color,
    titleColor: Color,
    state: AdvancedNetworkUiState,
    expanded: Boolean,
    onToggle: () -> Unit,
    onModeChange: (DnsMode) -> Unit,
    onHostsChange: (List<String>) -> Unit,
) {
    GlassSurface(night, Modifier.testTag("adv-dns"), radius = 22.dp,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .clickable(role = Role.DropdownList, onClick = onToggle)
                .testTag("adv-dns-toggle"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CardGlyphPanel(CardGlyphKind.DATABASE, titleColor)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.adv_dns_title), color = titleColor,
                    fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.adv_dns_subtitle), color = muted,
                    fontSize = 11.sp, lineHeight = 15.sp)
            }
            Spacer(Modifier.width(10.dp))
            Pill(night, muted,
                when (state.settings.dnsMode) {
                    DnsMode.PROFILE -> stringResource(R.string.adv_dns_mode_profile)
                    DnsMode.CUSTOM -> stringResource(R.string.adv_dns_mode_custom)
                    DnsMode.SYSTEM -> stringResource(R.string.adv_dns_mode_system)
                }, chevron = true, modifier = Modifier.testTag("adv-dns-value"))
        }
        if (expanded) {
            Spacer(Modifier.height(8.dp))
            Column(Modifier.fillMaxWidth().testTag("adv-dns-options"),
                verticalArrangement = Arrangement.spacedBy(2.dp)) {
                DnsOption(night, muted, titleColor, state,
                    mode = DnsMode.PROFILE,
                    label = stringResource(R.string.adv_dns_mode_profile),
                    description = stringResource(R.string.adv_dns_mode_profile_desc),
                    tag = "adv-dns-opt-profile", onModeChange)
                DnsOption(night, muted, titleColor, state,
                    mode = DnsMode.CUSTOM,
                    label = stringResource(R.string.adv_dns_mode_custom),
                    description = stringResource(R.string.adv_dns_mode_custom_desc),
                    tag = "adv-dns-opt-custom", onModeChange)
                DnsOption(night, muted, titleColor, state,
                    mode = DnsMode.SYSTEM,
                    label = stringResource(R.string.adv_dns_mode_system),
                    description = stringResource(R.string.adv_dns_mode_system_desc),
                    tag = "adv-dns-opt-system", onModeChange)
            }
            if (state.settings.dnsMode == DnsMode.CUSTOM) {
                Spacer(Modifier.height(6.dp))
                CustomDnsEditor(muted, titleColor, state, onHostsChange)
            }
        }
    }
}

@Composable
private fun DnsOption(
    night: Boolean,
    muted: Color,
    titleColor: Color,
    state: AdvancedNetworkUiState,
    mode: DnsMode,
    label: String,
    description: String,
    tag: String,
    onModeChange: (DnsMode) -> Unit,
) {
    val selected = state.settings.dnsMode == mode
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(16.dp))
            .then(if (selected) Modifier.background(
                Color.White.copy(alpha = if (night) .08f else .42f)) else Modifier)
            .selectable(selected = selected, enabled = !state.saving,
                role = Role.RadioButton, onClick = { onModeChange(mode) })
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(20.dp).clip(CircleShape)
            .border(1.6.dp, muted, CircleShape), contentAlignment = Alignment.Center) {
            if (selected) Box(Modifier.size(9.dp).background(muted, CircleShape))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, color = titleColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(description, color = muted, fontSize = 10.5.sp, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun CustomDnsEditor(
    muted: Color,
    titleColor: Color,
    state: AdvancedNetworkUiState,
    onHostsChange: (List<String>) -> Unit,
) {
    var text by remember { mutableStateOf(state.settings.customDns.joinToString(", ")) }
    val parsed = text.split(Regex("[,;\\s]+")).filter { it.isNotBlank() }
    val valid = AdvancedNetworkValidator.validDnsHosts(parsed)
    Column {
        OutlinedTextField(
            value = text,
            onValueChange = { if (it.length <= 120) text = it },
            singleLine = true,
            label = { Text(stringResource(R.string.adv_dns_custom_hint), fontSize = 11.sp) },
            modifier = Modifier.fillMaxWidth().testTag("adv-dns-input"),
            enabled = !state.saving,
        )
        if (text.isNotBlank() && valid == null) {
            Text(stringResource(R.string.adv_dns_custom_error), color = Color(0xFFC23A5E),
                fontSize = 11.sp, lineHeight = 15.sp,
                modifier = Modifier.padding(top = 4.dp).testTag("adv-dns-error"))
        }
        if (text.isBlank()) {
            Text(stringResource(R.string.adv_dns_custom_missing), color = muted,
                fontSize = 11.sp, lineHeight = 15.sp,
                modifier = Modifier.padding(top = 4.dp).testTag("adv-dns-missing"))
        }
        OutlinedButton(
            onClick = { valid?.let(onHostsChange) },
            enabled = valid != null && !state.saving,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("adv-dns-save"),
        ) { Text(stringResource(R.string.adv_dns_custom_save)) }
    }
}

@Composable
private fun UnavailableToggleCard(
    night: Boolean,
    muted: Color,
    titleColor: Color,
    glyph: CardGlyphKind,
    title: String,
    body: String,
    tag: String,
    switchTag: String,
) {
    val unavailable = stringResource(R.string.adv_protocol_unavailable)
    GlassSurface(night, Modifier.testTag(tag), radius = 22.dp,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CardGlyphPanel(glyph, titleColor)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = titleColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(body, color = muted, fontSize = 11.sp, lineHeight = 15.sp)
            }
            Spacer(Modifier.width(10.dp))
            GlassSwitch(
                checked = false,
                enabled = false,
                stateDescription = unavailable,
                clickLabel = title,
                modifier = Modifier.testTag(switchTag),
            )
        }
    }
}

@Composable
private fun KeepaliveCard(
    night: Boolean,
    muted: Color,
    titleColor: Color,
    state: AdvancedNetworkUiState,
    onKeepaliveChange: (Int?) -> Unit,
) {
    val baseline = state.settings.keepaliveSeconds ?: state.profileKeepalive ?: 0
    var draft by remember(baseline) { mutableFloatStateOf(baseline.toFloat()) }
    GlassSurface(night, Modifier.testTag("adv-keepalive"), radius = 22.dp,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CardGlyphPanel(CardGlyphKind.PULSE, titleColor)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.adv_keepalive_title), color = titleColor,
                    fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.adv_keepalive_subtitle), color = muted,
                    fontSize = 11.sp, lineHeight = 15.sp)
            }
            Spacer(Modifier.width(10.dp))
            Text(
                when {
                    state.settings.keepaliveSeconds != null &&
                        state.settings.keepaliveSeconds > 0 ->
                        stringResource(R.string.adv_keepalive_seconds,
                            state.settings.keepaliveSeconds)
                    state.settings.keepaliveSeconds != null ->
                        stringResource(R.string.adv_keepalive_off)
                    else -> stringResource(R.string.adv_keepalive_profile)
                },
                color = titleColor, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("adv-keepalive-value"))
        }
        Spacer(Modifier.height(8.dp))
        Slider(
            value = draft.coerceIn(0f, 120f),
            onValueChange = { draft = it },
            onValueChangeFinished = {
                onKeepaliveChange(((draft / 5f).roundToInt() * 5).coerceIn(0, 120))
            },
            valueRange = 0f..120f,
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth().testTag("adv-keepalive-slider"),
        )
    }
}

@Composable
private fun ReconnectPanel(
    night: Boolean,
    muted: Color,
    titleColor: Color,
    state: AdvancedNetworkUiState,
    onReconnect: () -> Unit,
) {
    GlassSurface(night, Modifier.testTag("adv-banner"), radius = 22.dp,
        contentPadding = PaddingValues(horizontal = 13.dp, vertical = 11.dp)) {
        if (state.dirty || state.applied == null) {
            Text(stringResource(R.string.adv_dirty), color = titleColor,
                fontSize = 12.5.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag("adv-banner-dirty"))
            Spacer(Modifier.height(6.dp))
            OutlinedButton(onClick = onReconnect, enabled = !state.saving,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .testTag("adv-reconnect")) {
                Text(stringResource(R.string.adv_reconnect))
            }
        } else {
            Text(stringResource(R.string.adv_applied,
                appliedSegment(state.applied), dnsSegment(state.applied),
                keepaliveSegment(state.applied)),
                color = muted, fontSize = 12.sp, lineHeight = 16.sp,
                modifier = Modifier.testTag("adv-applied"))
        }
    }
}

@Composable
private fun appliedSegment(settings: AdvancedNetworkSettings): String =
    settings.mtuOverride?.toString() ?: stringResource(R.string.adv_mtu_profile)

@Composable
private fun dnsSegment(settings: AdvancedNetworkSettings): String = when (settings.dnsMode) {
    DnsMode.PROFILE -> stringResource(R.string.adv_dns_mode_profile)
    DnsMode.CUSTOM -> if (settings.customDns.isEmpty()) stringResource(R.string.adv_dns_mode_custom)
    else settings.customDns.joinToString(", ")
    DnsMode.SYSTEM -> stringResource(R.string.adv_dns_mode_system)
}

@Composable
private fun keepaliveSegment(settings: AdvancedNetworkSettings): String = when {
    settings.keepaliveSeconds == null -> stringResource(R.string.adv_keepalive_profile)
    settings.keepaliveSeconds == 0 -> stringResource(R.string.adv_keepalive_off)
    else -> stringResource(R.string.adv_keepalive_seconds, settings.keepaliveSeconds)
}

@Composable
private fun Notes(night: Boolean, muted: Color, state: AdvancedNetworkUiState) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(stringResource(R.string.adv_note_wireguard), color = muted,
            fontSize = 10.sp, lineHeight = 13.sp)
        Text(stringResource(R.string.adv_note_import), color = muted,
            fontSize = 10.sp, lineHeight = 13.sp)
        Text(
            stringResource(if (state.connected) R.string.adv_note_reconnect
            else R.string.adv_note_apply),
            color = muted,
            fontSize = 10.sp, lineHeight = 13.sp,
            modifier = Modifier.testTag("adv-note-apply"),
        )
        if (!state.hasProfile) {
            Text(stringResource(R.string.adv_note_no_profile), color = muted,
                fontSize = 10.sp, lineHeight = 13.sp,
                modifier = Modifier.testTag("adv-note-no-profile"))
        }
    }
}

@Composable
private fun Pill(
    night: Boolean,
    muted: Color,
    label: String,
    chevron: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.heightIn(min = 32.dp)
            .background(if (night) Color.White.copy(alpha = .08f) else Color.White.copy(alpha = .5f),
                RoundedCornerShape(50))
            .border(1.dp, Color.White.copy(alpha = if (night) .3f else .8f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(label, color = muted, fontSize = 12.sp, maxLines = 1,
            overflow = TextOverflow.Ellipsis)
        if (chevron) ChevronDown(muted, Modifier.size(12.dp))
    }
}

@Composable
private fun ChevronDown(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.semantics { contentDescription = "" }) {
        val stroke = 1.7.dp.toPx()
        drawLine(color, Offset(size.width * .15f, size.height * .3f),
            Offset(size.width * .5f, size.height * .72f), strokeWidth = stroke,
            cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * .5f, size.height * .72f),
            Offset(size.width * .85f, size.height * .3f), strokeWidth = stroke,
            cap = StrokeCap.Round)
    }
}

@Composable
private fun CardGlyphPanel(kind: CardGlyphKind, titleColor: Color) {
    val light = titleColor == Color(0xFF171050)
    Box(
        Modifier.size(40.dp).clip(CircleShape)
            .background(Brush.linearGradient(
                if (light) listOf(Color(0xFFE7D9FF).copy(alpha = .9f),
                    Color(0xFFC6CFFF).copy(alpha = .7f))
                else listOf(Color(0xFF6B62E8).copy(alpha = .3f),
                    Color(0xFF54D8FF).copy(alpha = .18f))))
            .border(1.dp, Color.White.copy(alpha = .55f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        CardGlyph({ CanvasGlyphs.Glyph(kind, titleColor) }, Modifier.size(20.dp))
    }
}

enum class CardGlyphKind { STACK, LINK, DOME, INFINITY, EXPAND, DATABASE, PULSE, MASK, ROTATE }

/** Small canvas-backed glyphs, one per card, drawn in the screen's title color. */
private object CanvasGlyphs {
    @Composable
    fun Glyph(kind: CardGlyphKind, color: Color, modifier: Modifier = Modifier) {
        Canvas(modifier.semantics { contentDescription = "" }) {
            val strokeWidth = size.minDimension * 0.085f
            val stroke = Stroke(strokeWidth, cap = StrokeCap.Round)
            when (kind) {
                CardGlyphKind.STACK -> {
                    // Three stacked glass panes, slightly offset — the tuning-payload motif.
                    listOf(0f to .30f, .13f to .52f, .26f to .74f).forEach { (shift, y) ->
                        val path = Path().apply {
                            moveTo(size.width * (.30f + shift), size.height * (y + .08f))
                            lineTo(size.width * (.70f + shift), size.height * (y - .02f))
                            lineTo(size.width * (.70f + shift), size.height * (y + .10f))
                            lineTo(size.width * (.30f + shift), size.height * (y + .20f))
                            close()
                        }
                        drawPath(path, color.copy(alpha = .6f), style = stroke)
                    }
                }
                CardGlyphKind.LINK -> {
                    // Chain link: two rounded-rect outlines sharing the middle.
                    drawRoundRect(color, Offset(size.width * .02f, size.height * .30f),
                        Size(size.width * .60f, size.height * .42f),
                        CornerRadius(size.height * .21f), style = stroke)
                    drawRoundRect(color, Offset(size.width * .38f, size.height * .30f),
                        Size(size.width * .60f, size.height * .42f),
                        CornerRadius(size.height * .21f), style = stroke)
                }
                CardGlyphKind.DOME -> {
                    // OpenVPN's dome arc with a keyhole dot.
                    drawArc(color, startAngle = 180f, sweepAngle = 180f, useCenter = false,
                        topLeft = Offset(size.width * .12f, size.height * .18f),
                        size = Size(size.width * .76f, size.height * .76f), style = stroke)
                    drawLine(color, Offset(size.width * .10f, size.height * .56f),
                        Offset(size.width * .90f, size.height * .56f), strokeWidth,
                        StrokeCap.Round)
                    drawCircle(color, size.height * .07f,
                        Offset(size.width * .5f, size.height * .5f))
                }
                CardGlyphKind.INFINITY -> {
                    drawCircle(color, size.height * .21f,
                        Offset(size.width * .32f, size.height * .5f), style = stroke)
                    drawCircle(color, size.height * .21f,
                        Offset(size.width * .68f, size.height * .5f), style = stroke)
                }
                CardGlyphKind.EXPAND -> {
                    // Outward corner brackets with a diagonal double arrow in the middle.
                    drawLine(color, Offset(size.width * .06f, size.height * .28f),
                        Offset(size.width * .06f, size.height * .06f), strokeWidth, StrokeCap.Round)
                    drawLine(color, Offset(size.width * .06f, size.height * .06f),
                        Offset(size.width * .28f, size.height * .06f), strokeWidth, StrokeCap.Round)
                    drawLine(color, Offset(size.width * .94f, size.height * .72f),
                        Offset(size.width * .94f, size.height * .94f), strokeWidth, StrokeCap.Round)
                    drawLine(color, Offset(size.width * .94f, size.height * .94f),
                        Offset(size.width * .72f, size.height * .94f), strokeWidth, StrokeCap.Round)
                    drawLine(color, Offset(size.width * .06f, size.height * .94f),
                        Offset(size.width * .28f, size.height * .94f), strokeWidth, StrokeCap.Round)
                    drawLine(color, Offset(size.width * .06f, size.height * .94f),
                        Offset(size.width * .06f, size.height * .72f), strokeWidth, StrokeCap.Round)
                    drawLine(color, Offset(size.width * .94f, size.height * .06f),
                        Offset(size.width * .72f, size.height * .06f), strokeWidth, StrokeCap.Round)
                    drawLine(color, Offset(size.width * .94f, size.height * .06f),
                        Offset(size.width * .94f, size.height * .28f), strokeWidth, StrokeCap.Round)
                    drawLine(color, Offset(size.width * .40f, size.height * .60f),
                        Offset(size.width * .60f, size.height * .40f), strokeWidth, StrokeCap.Round)
                }
                CardGlyphKind.DATABASE -> {
                    drawArc(color, startAngle = 180f, sweepAngle = 180f, useCenter = false,
                        topLeft = Offset(size.width * .22f, size.height * .06f),
                        size = Size(size.width * .56f, size.height * .3f), style = stroke)
                    drawLine(color, Offset(size.width * .22f, size.height * .21f),
                        Offset(size.width * .22f, size.height * .79f), strokeWidth, StrokeCap.Round)
                    drawLine(color, Offset(size.width * .78f, size.height * .21f),
                        Offset(size.width * .78f, size.height * .79f), strokeWidth, StrokeCap.Round)
                    drawArc(color, startAngle = 20f, sweepAngle = 140f, useCenter = false,
                        topLeft = Offset(size.width * .22f, size.height * .64f),
                        size = Size(size.width * .56f, size.height * .3f), style = stroke)
                    drawArc(color, startAngle = 30f, sweepAngle = 120f, useCenter = false,
                        topLeft = Offset(size.width * .22f, size.height * .45f),
                        size = Size(size.width * .56f, size.height * .3f), style = stroke)
                }
                CardGlyphKind.PULSE -> {
                    val path = Path().apply {
                        moveTo(size.width * .04f, size.height * .58f)
                        lineTo(size.width * .26f, size.height * .58f)
                        lineTo(size.width * .38f, size.height * .18f)
                        lineTo(size.width * .5f, size.height * .84f)
                        lineTo(size.width * .64f, size.height * .34f)
                        lineTo(size.width * .74f, size.height * .58f)
                        lineTo(size.width * .96f, size.height * .58f)
                    }
                    drawPath(path, color, style = stroke)
                }
                CardGlyphKind.MASK -> {
                    drawArc(color, startAngle = 200f, sweepAngle = 140f, useCenter = false,
                        topLeft = Offset(size.width * .08f, size.height * .22f),
                        size = Size(size.width * .84f, size.height * .62f), style = stroke)
                    drawCircle(color, size.height * .07f,
                        Offset(size.width * .36f, size.height * .5f))
                    drawCircle(color, size.height * .07f,
                        Offset(size.width * .64f, size.height * .5f))
                }
                CardGlyphKind.ROTATE -> {
                    drawArc(color, startAngle = 215f, sweepAngle = 290f, useCenter = false,
                        topLeft = Offset(size.width * .1f, size.height * .1f),
                        size = Size(size.width * .8f, size.height * .8f), style = stroke)
                    val tip = Offset(size.width * .32f, size.height * .8f)
                    drawPath(Path().apply {
                        moveTo(tip.x, tip.y)
                        lineTo(tip.x + size.width * .12f, tip.y - size.height * .02f)
                        lineTo(tip.x + size.width * .04f, tip.y - size.height * .16f)
                        close()
                    }, color)
                }
            }
        }
    }

    @Composable
    fun Dots(color: Color, modifier: Modifier = Modifier) {
        Canvas(modifier.semantics { contentDescription = "" }) {
            listOf(.2f, .5f, .8f).forEach { y ->
                drawCircle(color, size.width * .09f, Offset(size.width * .5f, size.height * y))
            }
        }
    }
}

@Composable
private fun CardGlyph(node: @Composable () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier) { node() }
}
