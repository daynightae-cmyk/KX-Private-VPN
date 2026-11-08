package com.knoux.kxprivatevpn.features

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knoux.kxprivatevpn.R
import com.knoux.kxprivatevpn.designsystem.KxGlassPanel
import com.knoux.kxprivatevpn.designsystem.KxGradientAction
import com.knoux.kxprivatevpn.designsystem.KxIconKind
import com.knoux.kxprivatevpn.designsystem.KxLineIcon
import com.knoux.kxprivatevpn.designsystem.KxOrbBadge
import com.knoux.kxprivatevpn.designsystem.KxStatusPill
import com.knoux.kxprivatevpn.designsystem.kxCyan
import com.knoux.kxprivatevpn.designsystem.kxGreen
import com.knoux.kxprivatevpn.designsystem.kxInk
import com.knoux.kxprivatevpn.designsystem.kxMuted
import com.knoux.kxprivatevpn.designsystem.kxRose
import com.knoux.kxprivatevpn.designsystem.kxViolet
import com.knoux.kxprivatevpn.launch.rememberReducedMotion
import kotlin.math.cos
import kotlin.math.sin

/** Facts shown on reference 02. Unknowns stay unknown until a real measurement exists. */
data class NetworkReferenceFacts(
    val transport: String?,
    val dnsCount: Int?,
    val httpStatus: Int?,
    val requestDurationMs: Long?,
    val validatedByAndroid: Boolean?,
    val networkPresent: Boolean?,
) {
    val httpObserved: Boolean get() = httpStatus != null
    val dnsReported: Boolean get() = (dnsCount ?: 0) > 0
}

/**
 * Pure presentation policy: connection validation is Android's opinion, not KX leak-proofing;
 * an HTTPS HEAD receipt measures one request and never implies VPN speed or security.
 */
object NetworkReferencePolicy {
    fun facts(state: NetworkUiState): NetworkReferenceFacts {
        val reading = state.reading
        return NetworkReferenceFacts(
            transport = reading?.let {
                when {
                    !it.present -> null
                    else -> it.transports.firstOrNull()
                }
            },
            dnsCount = reading?.dns?.size,
            httpStatus = state.probeResult?.code,
            requestDurationMs = state.probeResult?.elapsedMs,
            validatedByAndroid = reading?.validated,
            networkPresent = reading?.present,
        )
    }
}

@Composable
internal fun NetworkReferenceHero(night: Boolean, probing: Boolean) {
    val reducedMotion = rememberReducedMotion()
    val angle: Float = if (probing && !reducedMotion) {
        val spin = rememberInfiniteTransition(label = "network-doctor-orbit")
        val frame by spin.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
            label = "network-doctor-orbit-angle",
        )
        frame
    } else 0f
    val cyan = kxCyan(night)
    val violet = kxViolet(night)
    Box(
        Modifier.fillMaxWidth().height(275.dp).testTag("network-ref-hero"),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(270.dp).testTag("network-ref-orbit")) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension * .48f
            drawCircle(
                Brush.radialGradient(
                    listOf(violet.copy(alpha = .35f),
                        cyan.copy(alpha = .17f), Color.Transparent),
                    center = center, radius = radius,
                ), radius = radius, center = center,
            )
            drawCircle(
                Brush.sweepGradient(
                    listOf(cyan.copy(alpha = .35f), violet,
                        cyan, Color.Transparent, violet.copy(alpha = .64f)),
                    center,
                ),
                radius = size.minDimension * .442f,
                style = Stroke(2.dp.toPx(), cap = StrokeCap.Round),
            )
            drawCircle(
                cyan.copy(alpha = .27f), radius = size.minDimension * .375f,
                style = Stroke(1.dp.toPx()),
            )
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(violet, cyan, Color.Transparent, violet), center),
                startAngle = -90f + angle, sweepAngle = 115f, useCenter = false,
                topLeft = Offset(size.width * .063f, size.height * .063f),
                size = Size(size.width * .874f, size.height * .874f),
                style = Stroke(4.dp.toPx(), cap = StrokeCap.Round),
            )
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(cyan, Color.Transparent, violet, cyan), center),
                startAngle = 113f - angle * .4f,
                sweepAngle = 75f, useCenter = false,
                topLeft = Offset(size.width * .052f, size.height * .052f),
                size = Size(size.width * .896f, size.height * .896f),
                style = Stroke(2.dp.toPx(), cap = StrokeCap.Round),
            )
            repeat(22) { i ->
                val a = Math.toRadians(i * (360.0 / 22) + angle * .15)
                val r = size.minDimension * .467f
                drawCircle(
                    if (i % 3 == 0) violet else cyan,
                    radius = if (i % 4 == 0) 1.7.dp.toPx() else 1.dp.toPx(),
                    center = Offset(
                        center.x + cos(a).toFloat() * r,
                        center.y + sin(a).toFloat() * r,
                    ),
                    alpha = if (i % 4 == 0) .95f else .45f,
                )
            }
            // The orbital floor is decorative; the center uses the real KX branded orb.
            drawOval(
                brush = Brush.radialGradient(
                    listOf(cyan.copy(alpha = .40f), Color.Transparent),
                    center = Offset(center.x, size.height * .94f), radius = size.width * .33f),
                topLeft = Offset(size.width * .17f, size.height * .85f),
                size = Size(size.width * .66f, size.height * .12f),
            )
        }
        KxOrbBadge(196.dp, night = night, modifier = Modifier.testTag("network-ref-brand-orb"))
        Box(Modifier.align(Alignment.CenterStart).padding(start = 14.dp, top = 115.dp)) {
            NetworkOrbitParticle(night, cyan, 22)
        }
        Box(Modifier.align(Alignment.CenterEnd).padding(end = 9.dp, bottom = 87.dp)) {
            NetworkOrbitParticle(night, violet, 15)
        }
    }
}

@Composable
private fun NetworkOrbitParticle(night: Boolean, glow: Color, sizeDp: Int) {
    Canvas(Modifier.size(sizeDp.dp)) {
        drawCircle(Brush.radialGradient(
            listOf(Color.White, glow, glow.copy(alpha = .25f), Color.Transparent),
            center = center, radius = size.minDimension * .53f,
        ), radius = size.minDimension * .50f)
    }
}

@Composable
internal fun NetworkReferenceControls(
    night: Boolean,
    state: NetworkUiState,
    onReadAgain: () -> Unit,
) {
    val label = when {
        state.readFailed -> R.string.network_ref_read_unavailable
        state.reading == null -> R.string.network_ref_ready
        state.reading.present && state.reading.validated -> R.string.network_ref_android_validated
        state.reading.present -> R.string.network_ref_needs_review
        else -> R.string.network_ref_offline
    }
    Column(
        Modifier.fillMaxWidth().testTag("network-ref-controls"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        KxStatusPill(
            label = stringResource(label),
            night = night,
            connected = state.reading?.present == true && state.reading.validated,
            icon = KxIconKind.BARS,
            modifier = Modifier.testTag("network-ref-status"),
        )
        KxGradientAction(
            label = stringResource(R.string.network_ref_start),
            night = night, enabled = true, tag = "network-ref-start",
            icon = KxIconKind.BOLT,
            modifier = Modifier.heightIn(min = 54.dp),
            onClick = onReadAgain,
        )
        Text(
            stringResource(R.string.network_ref_scan_scope),
            fontSize = 11.sp, lineHeight = 17.sp, color = kxMuted(night),
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
internal fun NetworkReferenceSummary(night: Boolean, state: NetworkUiState) {
    val facts = NetworkReferencePolicy.facts(state)
    val items = listOf(
        NetworkMetric(
            R.string.network_ref_network, KxIconKind.WIFI, "network-ref-network",
            when {
                facts.networkPresent == null -> stringResource(R.string.network_ref_unknown)
                facts.networkPresent == false -> stringResource(R.string.network_ref_offline)
                facts.transport != null -> facts.transport
                else -> stringResource(R.string.network_ref_reported)
            },
        ),
        NetworkMetric(
            R.string.network_ref_dns, KxIconKind.SERVER, "network-ref-dns",
            when {
                facts.dnsCount == null -> stringResource(R.string.network_ref_unknown)
                facts.dnsCount > 0 -> stringResource(R.string.network_ref_dns_reported)
                else -> stringResource(R.string.network_ref_not_reported)
            },
        ),
        NetworkMetric(
            R.string.network_ref_https, KxIconKind.LOCK, "network-ref-https",
            when {
                state.probing -> stringResource(R.string.network_ref_running)
                state.probeFailed -> stringResource(R.string.network_ref_request_failed)
                facts.httpStatus != null -> stringResource(R.string.network_ref_http_status, facts.httpStatus)
                else -> stringResource(R.string.network_ref_not_tested)
            },
        ),
        NetworkMetric(
            R.string.network_ref_latency, KxIconKind.CLOCK, "network-ref-latency",
            when {
                facts.requestDurationMs != null ->
                    stringResource(R.string.network_ref_latency_ms, facts.requestDurationMs)
                else -> stringResource(R.string.network_ref_unknown)
            },
        ),
    )
    KxGlassPanel(night, modifier = Modifier.testTag("network-ref-summary"),
        radius = 28.dp, padding = PaddingValues(14.dp)) {
        Row(Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KxLineIcon(KxIconKind.BARS, kxCyan(night), Modifier.size(24.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.network_ref_summary),
                    color = kxInk(night), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.network_ref_summary_note),
                    color = kxMuted(night), fontSize = 11.sp, lineHeight = 16.sp)
            }
        }
        Spacer(Modifier.height(13.dp))
        val fontScale = LocalDensity.current.fontScale
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth < 355.dp || fontScale >= 1.4f) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items.take(2).forEach { NetworkMetricTile(it, night, Modifier.weight(1f)) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items.drop(2).forEach { NetworkMetricTile(it, night, Modifier.weight(1f)) }
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items.forEach { NetworkMetricTile(it, night, Modifier.weight(1f)) }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            KxStatusPill(
                label = stringResource(
                    if (facts.validatedByAndroid == true) R.string.network_ref_os_validated
                    else R.string.network_ref_os_not_validated
                ),
                night = night,
                connected = facts.validatedByAndroid == true,
                icon = KxIconKind.CHECK,
                modifier = Modifier.testTag("network-ref-chip-internet"),
            )
            KxStatusPill(
                label = stringResource(
                    if (facts.dnsReported) R.string.network_ref_dns_reported
                    else R.string.network_ref_dns_unconfirmed
                ),
                night = night,
                connected = facts.dnsReported,
                icon = KxIconKind.SERVER,
                modifier = Modifier.testTag("network-ref-chip-dns"),
            )
            KxStatusPill(
                label = stringResource(
                    if (facts.httpObserved) R.string.network_ref_https_response
                    else R.string.network_ref_https_unverified
                ),
                night = night,
                connected = facts.httpObserved,
                icon = KxIconKind.LOCK,
                modifier = Modifier.testTag("network-ref-chip-https"),
            )
        }
    }
}

private data class NetworkMetric(
    val title: Int,
    val icon: KxIconKind,
    val tag: String,
    val value: String,
)

@Composable
private fun NetworkMetricTile(item: NetworkMetric, night: Boolean, modifier: Modifier) {
    val bg = if (night) Color(0xFF3F397E).copy(alpha = .60f)
        else Color.White.copy(alpha = .58f)
    Column(
        modifier.heightIn(min = 101.dp).background(bg, RoundedCornerShape(20.dp))
            .padding(horizontal = 5.dp, vertical = 9.dp).testTag(item.tag),
        verticalArrangement = Arrangement.spacedBy(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        KxLineIcon(item.icon, kxCyan(night), Modifier.size(24.dp))
        Text(
            stringResource(item.title), fontSize = 11.sp, lineHeight = 15.sp,
            fontWeight = FontWeight.SemiBold, color = kxInk(night),
            textAlign = TextAlign.Center, maxLines = 2,
        )
        Text(
            item.value, fontSize = 11.sp, lineHeight = 15.sp,
            fontWeight = FontWeight.Bold, color = kxInk(night),
            textAlign = TextAlign.Center, maxLines = 2,
        )
    }
}
