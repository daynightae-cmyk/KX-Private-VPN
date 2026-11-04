package com.knoux.kxprivatevpn.features

import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knoux.kxprivatevpn.R
import com.knoux.kxprivatevpn.designsystem.Atmosphere
import com.knoux.kxprivatevpn.designsystem.GlassSurface
import com.knoux.kxprivatevpn.designsystem.GlassSwitch
import com.knoux.kxprivatevpn.designsystem.KxPageHeader
import com.knoux.kxprivatevpn.platform.security.CheckStatus
import com.knoux.kxprivatevpn.platform.security.DeviceSecurityState
import com.knoux.kxprivatevpn.platform.security.MissingCheck
import com.knoux.kxprivatevpn.platform.security.ProtectionLevel
import com.knoux.kxprivatevpn.platform.security.SecurityAssessment
import com.knoux.kxprivatevpn.platform.security.SecurityDiagnostics
import com.knoux.kxprivatevpn.platform.security.SecurityFacts
import com.knoux.kxprivatevpn.platform.security.SecurityPolicy
import com.knoux.kxprivatevpn.platform.vpn.VpnIssue
import com.knoux.kxprivatevpn.platform.vpn.VpnLinkInfo
import com.knoux.kxprivatevpn.platform.vpn.VpnLinkReader
import com.knoux.kxprivatevpn.platform.vpn.VpnPhase
import com.knoux.kxprivatevpn.platform.vpn.VpnState
import kotlinx.coroutines.delay

/** Everything the truthful Security Center renders. All values are supplied by measured reads. */
data class SecurityUiState(
    val facts: SecurityFacts = SecurityFacts(),
    val assessment: SecurityAssessment = SecurityAssessment(),
    val link: VpnLinkInfo = VpnLinkInfo(),
    val phase: VpnPhase = VpnPhase.IDLE,
    val issue: VpnIssue? = null,
    val measuredAtMillis: Long = 0L,
    val loading: Boolean = false,
    val settingsError: Boolean = false,
)

enum class SecurityGlyph { POWER, SERVER, IPV6, RECONNECT, TRACKER, INFINITY }

private enum class PillTone { POSITIVE, NEGATIVE, WARNING, NEUTRAL }

private enum class HeroTone { VERIFIED, WARNING, INACTIVE, WORKING }

/**
 * Wires the real platform reads to the screen. The screen itself stays a pure function of
 * measured state so instrumentation tests can prove it never invents protection.
 */
@Composable
internal fun SecurityRoute(
    model: ClientViewModel,
    vpn: VpnState,
    night: Boolean,
    onBack: () -> Unit,
    onSession: () -> Unit,
) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    var settingsError by remember { mutableStateOf(false) }
    val device by produceState<DeviceSecurityState?>(null, vpn.phase, refresh) {
        val diagnostics = SecurityDiagnostics(context)
        while (true) {
            val backendLockdown = model.lockdownEnabled()
            value = diagnostics.read(backendLockdown)
            delay(2_000)
        }
    }
    val link by produceState(VpnLinkInfo(), vpn.phase) {
        while (true) {
            value = VpnLinkReader.read(context)
            delay(2_000)
        }
    }
    val measured = device
    val session = vpn.session
    val facts = SecurityFacts(
        tunnelConnected = vpn.phase == VpnPhase.CONNECTED && session != null,
        androidLinkPresent = link.present,
        handshakeMillis = session?.latestHandshakeMillis,
        dnsServers = session?.dnsServers.orEmpty().ifEmpty { link.dns },
        allowedIps = session?.allowedIps.orEmpty(),
        alwaysOnApp = measured?.alwaysOnApp,
        appPackage = context.packageName,
        systemLockdown = measured?.lockdown,
        consentGranted = measured?.consentGranted,
    )
    val open = { action: String ->
        settingsError = false
        try {
            context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
            settingsError = true
        }
    }
    SecurityCenterScreen(
        night = night,
        state = SecurityUiState(
            facts = facts,
            assessment = SecurityPolicy.assess(facts),
            link = link,
            phase = vpn.phase,
            issue = vpn.issue,
            measuredAtMillis = measured?.measuredAtMillis ?: 0L,
            loading = measured == null,
            settingsError = settingsError,
        ),
        onBack = onBack,
        onOpenVpnSettings = { open(Settings.ACTION_VPN_SETTINGS) },
        onOpenSecuritySettings = { open(Settings.ACTION_SECURITY_SETTINGS) },
        onOpenSession = onSession,
        onRefresh = { refresh++ },
    )
}

@Composable
fun SecurityCenterScreen(
    night: Boolean,
    state: SecurityUiState,
    onBack: () -> Unit,
    onOpenVpnSettings: () -> Unit,
    onOpenSecuritySettings: () -> Unit,
    onOpenSession: () -> Unit,
    onRefresh: () -> Unit,
) {
    val muted = if (night) Color(0xFFC8C5E9) else Color(0xFF4B477F)
    Box(Modifier.fillMaxSize().testTag("security-screen")) {
        Atmosphere(night, 0f)
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            KxPageHeader(
                title = stringResource(R.string.security_center),
                night = night,
                backLabel = stringResource(R.string.back),
                onBack = onBack,
            )
            SecurityHero(night, muted, state)
            SecurityRow(
                night = night, glyph = SecurityGlyph.POWER,
                title = stringResource(R.string.security_kill_switch),
                body = stringResource(R.string.security_kill_switch_body),
                status = state.assessment.killSwitch,
                statusText = checkText(state.assessment.killSwitch),
                controlNote = stringResource(R.string.security_control_system),
                switchChecked = state.assessment.killSwitch == CheckStatus.ON,
                switchEnabled = true,
                switchAction = onOpenVpnSettings,
                rowAction = onOpenVpnSettings,
                testTag = "security-row-kill-switch",
            )
            SecurityRow(
                night = night, glyph = SecurityGlyph.SERVER,
                title = stringResource(R.string.security_dns),
                body = stringResource(R.string.security_dns_body),
                status = state.assessment.dnsProtection,
                statusText = checkText(state.assessment.dnsProtection),
                controlNote = stringResource(R.string.security_control_tunnel),
                switchChecked = state.assessment.dnsProtection == CheckStatus.ON,
                switchEnabled = state.assessment.dnsProtection != CheckStatus.PENDING,
                switchAction = null,
                rowAction = if (state.facts.tunnelConnected) onOpenSession else null,
                testTag = "security-row-dns",
            )
            SecurityRow(
                night = night, glyph = SecurityGlyph.IPV6,
                title = stringResource(R.string.security_ipv6),
                body = stringResource(R.string.security_ipv6_body),
                status = state.assessment.ipv6Protection,
                statusText = checkText(state.assessment.ipv6Protection),
                controlNote = stringResource(R.string.security_control_tunnel),
                switchChecked = state.assessment.ipv6Protection == CheckStatus.ON,
                switchEnabled = state.assessment.ipv6Protection != CheckStatus.PENDING,
                switchAction = null,
                rowAction = if (state.facts.tunnelConnected) onOpenSession else null,
                testTag = "security-row-ipv6",
            )
            SecurityRow(
                night = night, glyph = SecurityGlyph.RECONNECT,
                title = stringResource(R.string.security_auto),
                body = stringResource(R.string.security_auto_body),
                status = CheckStatus.UNAVAILABLE,
                statusText = checkText(CheckStatus.UNAVAILABLE),
                controlNote = stringResource(R.string.security_control_unavailable),
                switchChecked = false, switchEnabled = false, switchAction = null,
                rowAction = null, testTag = "security-row-auto-reconnect",
            )
            SecurityRow(
                night = night, glyph = SecurityGlyph.TRACKER,
                title = stringResource(R.string.security_tracker),
                body = stringResource(R.string.security_tracker_body),
                status = CheckStatus.UNAVAILABLE,
                statusText = checkText(CheckStatus.UNAVAILABLE),
                controlNote = stringResource(R.string.security_control_unavailable),
                switchChecked = false, switchEnabled = false, switchAction = null,
                rowAction = null, testTag = "security-row-tracker",
            )
            SecurityRow(
                night = night, glyph = SecurityGlyph.INFINITY,
                title = stringResource(R.string.security_always_on),
                body = stringResource(R.string.security_always_on_body),
                status = state.assessment.alwaysOn,
                statusText = alwaysOnText(state),
                controlNote = stringResource(R.string.security_control_system),
                switchChecked = state.assessment.alwaysOn == CheckStatus.ON,
                switchEnabled = true,
                switchAction = onOpenVpnSettings,
                rowAction = onOpenVpnSettings,
                testTag = "security-row-always-on",
            )
            SecurityDiagnostics(night, muted, state, onOpenVpnSettings, onOpenSecuritySettings,
                onOpenSession, onRefresh)
            Text(
                stringResource(R.string.security_scope), color = muted, fontSize = 11.sp,
                lineHeight = 15.sp, modifier = Modifier.padding(horizontal = 4.dp),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SecurityHero(night: Boolean, muted: Color, state: SecurityUiState) {
    val hero = when {
        state.loading -> HeroTone.WORKING
        state.phase == VpnPhase.CONNECTING || state.phase == VpnPhase.DISCONNECTING -> HeroTone.WORKING
        state.phase == VpnPhase.ERROR -> HeroTone.INACTIVE
        state.assessment.protection == ProtectionLevel.VERIFIED -> HeroTone.VERIFIED
        state.assessment.protection == ProtectionLevel.UNVERIFIED -> HeroTone.WARNING
        else -> HeroTone.INACTIVE
    }
    val title = when {
        state.loading -> R.string.security_hero_loading
        hero == HeroTone.VERIFIED -> R.string.security_hero_verified
        hero == HeroTone.WARNING -> R.string.security_hero_unverified
        hero == HeroTone.WORKING -> R.string.security_hero_connecting
        state.phase == VpnPhase.ERROR -> R.string.security_hero_error
        else -> R.string.security_hero_inactive
    }
    val body = when {
        state.loading -> R.string.security_hero_loading_body
        hero == HeroTone.VERIFIED -> R.string.security_hero_verified_body
        hero == HeroTone.WARNING -> R.string.security_hero_unverified_body
        hero == HeroTone.WORKING -> R.string.security_hero_connecting_body
        state.phase == VpnPhase.ERROR -> R.string.security_hero_error_body
        else -> R.string.security_hero_inactive_body
    }
    GlassSurface(
        night, Modifier.testTag("security-hero"), radius = 32.dp,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(136.dp), contentAlignment = Alignment.Center) {
            HeroEmblem(night)
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically) {
            HeroBadge(hero)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(title), fontSize = 21.sp, fontWeight = FontWeight.Bold,
                color = if (night) Color(0xFFF6F1FF) else Color(0xFF171050),
                modifier = Modifier.testTag("security-posture"))
        }
        Spacer(Modifier.height(5.dp))
        Text(stringResource(body), color = muted, fontSize = 12.5.sp, lineHeight = 17.sp,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().testTag("security-posture-body"))
        if (state.issue != null) {
            Text(
                stringResource(R.string.security_issue_prefix, vpnIssueText(state.issue)),
                color = muted, fontSize = 11.5.sp, lineHeight = 16.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp).testTag("security-issue"),
            )
        }
        if (hero == HeroTone.WARNING && state.assessment.missing.isNotEmpty()) {
            MissingChips(night, state.assessment.missing)
        }
    }
}

@Composable
private fun MissingChips(night: Boolean, missing: List<MissingCheck>) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp).horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(stringResource(R.string.security_missing_title), color = Color(0xFF8A5A00),
            fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.CenterVertically))
        missing.forEach { item ->
            val label = stringResource(
                when (item) {
                    MissingCheck.HANDSHAKE -> R.string.security_missing_handshake
                    MissingCheck.ANDROID_LINK -> R.string.security_missing_link
                    MissingCheck.DNS -> R.string.security_missing_dns
                    MissingCheck.IPV6 -> R.string.security_missing_ipv6
                },
            )
            Text(
                label,
                color = if (night) Color(0xFFFFD98A) else Color(0xFF7A4E00),
                fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(RoundedCornerShape(50))
                    .background(Color(0xFFFFC94D).copy(alpha = if (night) .16f else .24f))
                    .border(1.dp, Color(0xFFFFC94D).copy(alpha = .45f), RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
    }
}

@Composable
private fun HeroBadge(hero: HeroTone) {
    val color = when (hero) {
        HeroTone.VERIFIED -> Color(0xFF0FA76B)
        HeroTone.WARNING -> Color(0xFFE0A100)
        HeroTone.WORKING -> Color(0xFF6D5BE8)
        HeroTone.INACTIVE -> Color(0xFFC23A5E)
    }
    Canvas(Modifier.size(26.dp).semantics { contentDescription = "" }) {
        drawCircle(color.copy(alpha = .16f), radius = size.minDimension / 2f)
        drawCircle(color.copy(alpha = .55f), radius = size.minDimension / 2f, style = Stroke(1.dp.toPx()))
        when (hero) {
            HeroTone.VERIFIED -> {
                drawLine(color, Offset(size.width * .27f, size.height * .53f),
                    Offset(size.width * .44f, size.height * .70f), 2.2.dp.toPx(), StrokeCap.Round)
                drawLine(color, Offset(size.width * .44f, size.height * .70f),
                    Offset(size.width * .74f, size.height * .33f), 2.2.dp.toPx(), StrokeCap.Round)
            }
            HeroTone.WARNING -> {
                drawLine(color, Offset(size.width * .5f, size.height * .28f),
                    Offset(size.width * .5f, size.height * .58f), 2.2.dp.toPx(), StrokeCap.Round)
                drawCircle(color, size.minDimension * .055f, Offset(size.width * .5f, size.height * .72f))
            }
            HeroTone.WORKING -> drawArc(color, -40f, 250f, false,
                topLeft = Offset(size.width * .28f, size.height * .28f),
                size = Size(size.width * .44f, size.height * .44f),
                style = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round))
            HeroTone.INACTIVE -> drawLine(color, Offset(size.width * .32f, size.height * .5f),
                Offset(size.width * .68f, size.height * .5f), 2.2.dp.toPx(), StrokeCap.Round)
        }
    }
}

@Composable
private fun HeroEmblem(night: Boolean) {
    Canvas(Modifier.size(142.dp).semantics { contentDescription = "" }) {
        val c = center
        val r = size.minDimension / 2f
        // Atmospheric halo and the pearl-glass sphere behind the shield.
        drawCircle(Brush.radialGradient(listOf(Color(0xFFB99CFF).copy(alpha = .34f), Color.Transparent),
            c, r * 1.35f), r, c)
        drawCircle(
            Brush.radialGradient(
                if (night) listOf(Color(0xFFF0ECFF).copy(alpha = .55f),
                    Color(0xFF7B6BEA).copy(alpha = .42f), Color(0xFF2B2470).copy(alpha = .58f))
                else listOf(Color.White.copy(alpha = .88f), Color(0xFFD9D4FF).copy(alpha = .55f),
                    Color(0xFFAACBFF).copy(alpha = .52f)),
                Offset(c.x - r * .32f, c.y - r * .38f), r * 1.5f,
            ),
            r * .84f, c,
        )
        drawCircle(Color.White.copy(alpha = if (night) .55f else .88f), r * .84f, c,
            style = Stroke(1.3.dp.toPx()))
        drawCircle(Brush.sweepGradient(listOf(Color.Transparent, Color(0xFF7DE9FF),
            Color(0xFFE4C6FF), Color.Transparent), c), r * .76f, c,
            style = Stroke(1.2.dp.toPx()), alpha = .55f)
        // Orbit ring crosses behind the shield and returns in front of it.
        rotate(-14f, c) {
            drawOval(Color.White.copy(alpha = if (night) .45f else .8f),
                topLeft = Offset(c.x - r * .95f, c.y - r * .28f),
                size = Size(r * 1.9f, r * .56f), style = Stroke(1.dp.toPx()))
        }
        val sw = r * .74f
        val sh = r * .92f
        val left = c.x - sw / 2f
        val top = c.y - sh / 2f - r * .03f
        val shield = Path().apply {
            moveTo(c.x, top)
            cubicTo(c.x - sw * .12f, top + sh * .12f, left + sw * .14f, top + sh * .10f,
                left + sw * .04f, top + sh * .15f)
            cubicTo(left, top + sh * .52f, left + sw * .18f, top + sh * .78f, c.x, top + sh)
            cubicTo(left + sw * .82f, top + sh * .78f, left + sw, top + sh * .52f,
                left + sw * .96f, top + sh * .15f)
            cubicTo(left + sw * .86f, top + sh * .10f, c.x + sw * .12f, top + sh * .12f, c.x, top)
            close()
        }
        drawPath(shield, Brush.linearGradient(
            listOf(Color(0xFFE2A6FF), Color(0xFF6846EC), Color(0xFF1C63E8), Color(0xFF6DECFF)),
            Offset(left, top), Offset(left + sw, top + sh),
        ))
        clipPath(shield) {
            for (i in 0..2) {
                val y = top + sh * (.30f + i * .19f)
                val ribbon = Path().apply {
                    moveTo(left - sw * .22f, y)
                    cubicTo(left + sw * .2f, y + sh * .14f, left + sw * .72f, y - sh * .10f,
                        left + sw * 1.22f, y + sh * .03f)
                    lineTo(left + sw * 1.22f, y + sh * .13f)
                    cubicTo(left + sw * .68f, y, left + sw * .18f, y + sh * .24f,
                        left - sw * .22f, y + sh * .10f)
                    close()
                }
                drawPath(ribbon, Brush.linearGradient(listOf(Color.White.copy(alpha = .95f),
                    Color(0xFF8DEBFF), Color(0xFFDCA8FF), Color.White)),
                    alpha = if (i == 1) .62f else .44f)
            }
            drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = .65f), Color.Transparent),
                Offset(left + sw * .28f, top + sh * .22f), sw * .5f),
                sw * .5f, Offset(left + sw * .28f, top + sh * .22f))
        }
        drawPath(shield, Color.White.copy(alpha = if (night) .55f else .82f),
            style = Stroke(1.3.dp.toPx()))
        // Satellites and glints.
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFD6FF), Color(0xFFB56BFF),
            Color.Transparent), Offset(c.x - r * .93f, c.y + r * .26f), 9f),
            5.5f, Offset(c.x - r * .93f, c.y + r * .26f))
        drawCircle(Brush.radialGradient(listOf(Color(0xFFC9F4FF), Color(0xFF4FB8FF),
            Color.Transparent), Offset(c.x + r * .88f, c.y - r * .22f), 9f),
            5f, Offset(c.x + r * .88f, c.y - r * .22f))
        star(Offset(c.x + r * .62f, c.y - r * .72f), 5f)
        star(Offset(c.x - r * .55f, c.y - r * .18f), 3.5f)
    }
}

private fun DrawScope.star(p: Offset, radius: Float) {
    drawLine(Color.White.copy(alpha = .9f), Offset(p.x - radius, p.y), Offset(p.x + radius, p.y),
        1f, StrokeCap.Round)
    drawLine(Color.White.copy(alpha = .9f), Offset(p.x, p.y - radius), Offset(p.x, p.y + radius),
        1f, StrokeCap.Round)
}

@Composable
private fun SecurityRow(
    night: Boolean,
    glyph: SecurityGlyph,
    title: String,
    body: String,
    status: CheckStatus,
    statusText: String,
    controlNote: String,
    switchChecked: Boolean,
    switchEnabled: Boolean,
    switchAction: (() -> Unit)?,
    rowAction: (() -> Unit)?,
    testTag: String,
) {
    val muted = if (night) Color(0xFFC8C5E9) else Color(0xFF4B477F)
    val titleColor = if (night) Color(0xFFF6F1FF) else Color(0xFF171050)
    val unavailable = status == CheckStatus.UNAVAILABLE
    GlassSurface(
        night,
        modifier = Modifier.testTag(testTag).then(
            if (rowAction != null) Modifier.clickable(role = Role.Button,
                onClickLabel = title, onClick = rowAction) else Modifier,
        ),
        radius = 24.dp, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ShieldTile(glyph, available = !unavailable)
            Spacer(Modifier.width(9.dp))
            Box(Modifier.width(1.dp).height(38.dp)
                .background(Color.White.copy(alpha = if (night) .18f else .55f)))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, color = titleColor, fontSize = 15.5.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f, fill = false))
                    Spacer(Modifier.width(6.dp))
                    StatusPill(statusText, tone(status), night,
                        Modifier.testTag("$testTag-status"))
                }
                Spacer(Modifier.height(2.dp))
                Text(body, color = muted, fontSize = 11.5.sp, lineHeight = 15.sp)
                Text(
                    controlNote, color = muted.copy(alpha = .78f), fontSize = 10.sp,
                    lineHeight = 13.sp, modifier = Modifier.padding(top = 2.dp),
                )
            }
            Spacer(Modifier.width(4.dp))
            GlassSwitch(
                checked = switchChecked,
                enabled = switchEnabled,
                stateDescription = statusText,
                clickLabel = title,
                modifier = Modifier.testTag("$testTag-switch"),
                onClick = switchAction,
            )
        }
    }
}

@Composable
private fun StatusPill(text: String, tone: PillTone, night: Boolean, modifier: Modifier = Modifier) {
    val (bg, fg) = when (tone) {
        PillTone.POSITIVE -> (if (night) Color(0xFF0E7A50).copy(alpha = .35f) else Color(0xFFB9F5D9)) to
            (if (night) Color(0xFF7DFFC3) else Color(0xFF08764C))
        PillTone.NEGATIVE -> (if (night) Color(0xFF8A2440).copy(alpha = .30f) else Color(0xFFFFD3DE)) to
            (if (night) Color(0xFFFF9DBA) else Color(0xFFA31F44))
        PillTone.WARNING -> (if (night) Color(0xFF7A5A00).copy(alpha = .35f) else Color(0xFFFFE7AE)) to
            (if (night) Color(0xFFFFD36A) else Color(0xFF7A4E00))
        PillTone.NEUTRAL -> (if (night) Color.White.copy(alpha = .10f) else Color(0xFFE4E2F3)) to
            (if (night) Color(0xFFD6D2EF) else Color(0xFF5B5780))
    }
    Row(
        modifier.semantics(mergeDescendants = true) {}
            .clip(RoundedCornerShape(50)).background(bg)
            .border(1.dp, fg.copy(alpha = .28f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (tone == PillTone.POSITIVE) {
            Canvas(Modifier.size(10.dp)) {
                drawLine(fg, Offset(size.width * .18f, size.height * .52f),
                    Offset(size.width * .42f, size.height * .76f), 1.4.dp.toPx(), StrokeCap.Round)
                drawLine(fg, Offset(size.width * .42f, size.height * .76f),
                    Offset(size.width * .84f, size.height * .26f), 1.4.dp.toPx(), StrokeCap.Round)
            }
            Spacer(Modifier.width(3.dp))
        }
        Text(text, color = fg, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ShieldTile(glyph: SecurityGlyph, available: Boolean) {
    Canvas(Modifier.size(45.dp)) {
        val w = size.width
        val h = size.height
        val shield = Path().apply {
            moveTo(w * .5f, h * .05f)
            cubicTo(w * .41f, h * .13f, w * .31f, h * .145f, w * .21f, h * .16f)
            cubicTo(w * .20f, h * .44f, w * .28f, h * .70f, w * .5f, h * .92f)
            cubicTo(w * .72f, h * .70f, w * .80f, h * .44f, w * .79f, h * .16f)
            cubicTo(w * .69f, h * .145f, w * .59f, h * .13f, w * .5f, h * .05f)
            close()
        }
        val base = if (available) {
            Brush.linearGradient(listOf(Color(0xFFA88BFF), Color(0xFF5C6BF2), Color(0xFF57E3FF)),
                Offset(w * .18f, h * .1f), Offset(w * .85f, h * .9f))
        } else {
            Brush.linearGradient(listOf(Color(0xFFB7B4C9), Color(0xFF8D89A8), Color(0xFFC8C6D8)),
                Offset(w * .18f, h * .1f), Offset(w * .85f, h * .9f))
        }
        drawPath(shield, base)
        drawPath(shield, Brush.verticalGradient(listOf(Color.White.copy(alpha = .38f), Color.Transparent),
            startY = h * .05f, endY = h * .55f))
        drawPath(shield, Color.White.copy(alpha = if (available) .8f else .5f), style = Stroke(1.dp.toPx()))
        drawGlyph(glyph, available)
    }
}

private fun DrawScope.drawGlyph(glyph: SecurityGlyph, available: Boolean) {
    val w = size.width
    val h = size.height
    val color = Color.White.copy(alpha = if (available) .96f else .72f)
    val stroke = 1.8.dp.toPx()
    when (glyph) {
        SecurityGlyph.POWER -> {
            drawArc(color, -52f, 284f, false,
                topLeft = Offset(w * .34f, h * .34f), size = Size(w * .32f, h * .32f),
                style = Stroke(stroke, cap = StrokeCap.Round))
            drawLine(color, Offset(w * .5f, h * .30f), Offset(w * .5f, h * .49f), stroke, StrokeCap.Round)
        }
        SecurityGlyph.SERVER -> {
            for (i in 0..2) {
                val top = h * (.33f + i * .14f)
                drawRoundRect(color.copy(alpha = .92f), Offset(w * .35f, top),
                    Size(w * .30f, h * .085f), androidx.compose.ui.geometry.CornerRadius(2.5f),
                    style = Stroke(1.2.dp.toPx()))
                drawCircle(color, 1.2f, Offset(w * .40f, top + h * .042f))
            }
        }
        SecurityGlyph.IPV6 -> {
            drawContext.canvas.nativeCanvas.drawText(
                "IPv6", w * .5f, h * .56f,
                Paint().apply {
                    this.color = color.toArgb()
                    textSize = h * .17f
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                },
            )
        }
        SecurityGlyph.RECONNECT -> {
            drawArc(color, -128f, 250f, false, Offset(w * .32f, h * .33f),
                Size(w * .36f, h * .32f), style = Stroke(stroke, cap = StrokeCap.Round))
            drawLine(color, Offset(w * .78f, h * .32f), Offset(w * .80f, h * .45f), stroke, StrokeCap.Round)
            drawLine(color, Offset(w * .78f, h * .32f), Offset(w * .66f, h * .36f), stroke, StrokeCap.Round)
            drawArc(color, 52f, 250f, false, Offset(w * .32f, h * .36f),
                Size(w * .36f, h * .32f), style = Stroke(stroke, cap = StrokeCap.Round))
            drawLine(color, Offset(w * .22f, h * .68f), Offset(w * .20f, h * .55f), stroke, StrokeCap.Round)
            drawLine(color, Offset(w * .22f, h * .68f), Offset(w * .34f, h * .64f), stroke, StrokeCap.Round)
        }
        SecurityGlyph.TRACKER -> {
            drawCircle(color, w * .15f, Offset(w * .5f, h * .52f), style = Stroke(stroke))
            drawCircle(color, w * .04f, Offset(w * .5f, h * .52f), style = Stroke(stroke))
            drawLine(color, Offset(w * .5f, h * .30f), Offset(w * .5f, h * .38f), stroke)
            drawLine(color, Offset(w * .5f, h * .66f), Offset(w * .5f, h * .74f), stroke)
            drawLine(color, Offset(w * .30f, h * .52f), Offset(w * .38f, h * .52f), stroke)
            drawLine(color, Offset(w * .62f, h * .52f), Offset(w * .70f, h * .52f), stroke)
        }
        SecurityGlyph.INFINITY -> {
            val path = Path().apply {
                moveTo(w * .34f, h * .52f)
                cubicTo(w * .32f, h * .34f, w * .50f, h * .32f, w * .50f, h * .5f)
                cubicTo(w * .50f, h * .68f, w * .68f, h * .66f, w * .66f, h * .48f)
                cubicTo(w * .65f, h * .34f, w * .44f, h * .36f, w * .40f, h * .56f)
            }
            drawPath(path, color, style = Stroke(stroke, cap = StrokeCap.Round))
        }
    }
    // Soft inner rim for the glass tile.
    drawArc(Color.White.copy(alpha = .30f), 200f, 140f, false,
        Offset(w * .22f, h * .18f), Size(w * .56f, h * .60f), style = Stroke(1.dp.toPx()))
}

@Composable
private fun SecurityDiagnostics(
    night: Boolean,
    muted: Color,
    state: SecurityUiState,
    onOpenVpnSettings: () -> Unit,
    onOpenSecuritySettings: () -> Unit,
    onOpenSession: () -> Unit,
    onRefresh: () -> Unit,
) {
    val context = LocalContext.current
    val facts = state.facts
    val titleColor = if (night) Color(0xFFF6F1FF) else Color(0xFF171050)
    val unknown = stringResource(R.string.security_diag_unknown)
    val none = stringResource(R.string.security_diag_none)
    GlassSurface(night, Modifier.testTag("security-diagnostics"), radius = 26.dp,
        contentPadding = PaddingValues(16.dp)) {
        Text(stringResource(R.string.security_diagnostics_title), color = titleColor,
            fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        DiagRow(stringResource(R.string.security_diag_state), phaseText(state.phase), muted, titleColor,
            "security-diag-state")
        DiagRow(stringResource(R.string.security_diag_link), when {
            state.link.present -> stringResource(
                R.string.security_diag_link_present,
                state.link.interfaceName ?: unknown,
            )
            state.loading -> unknown
            else -> stringResource(R.string.security_diag_link_absent)
        }, muted, titleColor, "security-diag-link")
        DiagRow(stringResource(R.string.security_diag_dns),
            facts.dnsServers.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: none,
            muted, titleColor, "security-diag-dns")
        DiagRow(stringResource(R.string.security_diag_routes),
            facts.allowedIps.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: none,
            muted, titleColor, "security-diag-routes")
        DiagRow(stringResource(R.string.security_diag_handshake),
            facts.handshakeMillis?.let { formatTime(context, it) } ?: none,
            muted, titleColor, "security-diag-handshake")
        DiagRow(stringResource(R.string.security_diag_lockdown),
            facts.systemLockdown?.let { if (it) stringResource(R.string.security_state_on)
                else stringResource(R.string.security_state_off) } ?: unknown,
            muted, titleColor, "security-diag-lockdown")
        DiagRow(stringResource(R.string.security_diag_always_on), when {
            facts.alwaysOnApp == null -> unknown
            facts.alwaysOnApp.isEmpty() -> stringResource(R.string.security_diag_not_configured)
            facts.alwaysOnApp == facts.appPackage -> stringResource(R.string.security_diag_this_app)
            else -> stringResource(R.string.security_diag_other_app)
        }, muted, titleColor, "security-diag-always-on")
        DiagRow(stringResource(R.string.security_diag_consent),
            facts.consentGranted?.let {
                stringResource(if (it) R.string.security_diag_granted else R.string.security_diag_not_granted)
            } ?: unknown,
            muted, titleColor, "security-diag-consent")
        if (state.measuredAtMillis > 0) {
            Text(stringResource(R.string.security_diag_measured_at,
                formatTime(context, state.measuredAtMillis)), color = muted, fontSize = 10.5.sp)
        }
        if (state.settingsError) {
            Text(stringResource(R.string.security_settings_failed), color = Color(0xFFC23A5E),
                fontSize = 12.sp, modifier = Modifier.testTag("security-settings-error"))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onOpenVpnSettings,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("security-open-vpn-settings")) {
            Text(stringResource(R.string.security_action_vpn_settings))
        }
        OutlinedButton(onClick = onOpenSecuritySettings,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("security-open-security-settings")) {
            Text(stringResource(R.string.security_action_security_settings))
        }
        if (facts.tunnelConnected) {
            OutlinedButton(onClick = onOpenSession,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("security-open-session")) {
                Text(stringResource(R.string.security_action_session))
            }
        }
        OutlinedButton(onClick = onRefresh,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("security-refresh")) {
            Text(stringResource(R.string.security_refresh))
        }
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.security_guide), color = muted, fontSize = 11.sp, lineHeight = 15.sp)
    }
}

@Composable
private fun DiagRow(label: String, value: String, muted: Color, valueColor: Color, testTag: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, color = muted, fontSize = 11.5.sp, modifier = Modifier.weight(.9f))
        Text(value, color = valueColor, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End, modifier = Modifier.weight(1.1f).testTag(testTag))
    }
}

@Composable
private fun phaseText(phase: VpnPhase): String = stringResource(
    when (phase) {
        VpnPhase.CONNECTED -> R.string.connected
        VpnPhase.CONNECTING -> R.string.connecting
        VpnPhase.DISCONNECTING -> R.string.disconnecting
        VpnPhase.ERROR -> R.string.security_state_error
        VpnPhase.IDLE -> R.string.disconnected
    },
)

@Composable
private fun checkText(status: CheckStatus): String = stringResource(
    when (status) {
        CheckStatus.ON -> R.string.security_state_on
        CheckStatus.OFF -> R.string.security_state_off
        CheckStatus.UNKNOWN -> R.string.security_state_unknown
        CheckStatus.UNAVAILABLE -> R.string.security_state_unavailable
        CheckStatus.PENDING -> R.string.security_state_awaiting
    },
)

@Composable
private fun alwaysOnText(state: SecurityUiState): String {
    if (state.assessment.alwaysOn == CheckStatus.OFF) {
        val configured = state.facts.alwaysOnApp
        if (configured != null && configured.isNotEmpty() && configured != state.facts.appPackage) {
            return stringResource(R.string.security_state_other_app)
        }
    }
    return checkText(state.assessment.alwaysOn)
}

private fun tone(status: CheckStatus): PillTone = when (status) {
    CheckStatus.ON -> PillTone.POSITIVE
    CheckStatus.OFF -> PillTone.NEGATIVE
    CheckStatus.UNKNOWN -> PillTone.WARNING
    CheckStatus.UNAVAILABLE, CheckStatus.PENDING -> PillTone.NEUTRAL
}
