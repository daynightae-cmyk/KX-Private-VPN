package com.knoux.kxprivatevpn.features

import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knoux.kxprivatevpn.R
import com.knoux.kxprivatevpn.designsystem.Atmosphere
import com.knoux.kxprivatevpn.designsystem.GlassAction
import com.knoux.kxprivatevpn.designsystem.GlassSurface
import com.knoux.kxprivatevpn.designsystem.KxPageHeader
import com.knoux.kxprivatevpn.designsystem.glassForeground
import com.knoux.kxprivatevpn.platform.network.HttpsReading
import com.knoux.kxprivatevpn.platform.network.NetworkDoctor
import com.knoux.kxprivatevpn.platform.network.NetworkReading
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Everything the screen may render. Keeping the reading, the probe form and the probe outcome
 * here means presentation can be tested without a real network or a real request.
 */
data class NetworkUiState(
    val reading: NetworkReading? = null,
    val readFailed: Boolean = false,
    val probeUrl: String = "",
    val probeConsent: Boolean = false,
    val probing: Boolean = false,
    val probeResult: HttpsReading? = null,
    val probeFailed: Boolean = false,
)

/**
 * The only gate between the form and a real outbound request: explicit consent, a non-blank
 * target, no request already in flight, and an address [NetworkDoctor] accepts.
 */
object NetworkProbePolicy {
    fun targetProblem(url: String): Boolean =
        url.isNotBlank() && runCatching { NetworkDoctor.validate(url) }.isFailure

    fun canRun(url: String, consent: Boolean, probing: Boolean): Boolean =
        consent && !probing && url.isNotBlank() && !targetProblem(url)

    /** Consent belongs to one exact target. Any edit revokes it; active probes are immutable. */
    fun editedTarget(state: NetworkUiState, value: String): NetworkUiState =
        if (state.probing || value.length > 2048 || value == state.probeUrl) state
        else state.copy(probeUrl = value, probeConsent = false,
            probeResult = null, probeFailed = false)
}

@Composable
fun NetworkDoctorRoute(night: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(NetworkUiState()) }

    fun refresh() {
        state = try {
            state.copy(reading = NetworkDoctor.read(context), readFailed = false)
        } catch (_: Exception) {
            state.copy(reading = null, readFailed = true)
        }
    }

    DisposableEffect(context) {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { refresh() }
            override fun onLost(network: Network) { refresh() }
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) { refresh() }
            override fun onLinkPropertiesChanged(network: Network, properties: LinkProperties) { refresh() }
        }
        try {
            manager.registerDefaultNetworkCallback(callback)
            refresh()
        } catch (_: Exception) {
            state = state.copy(readFailed = true)
        }
        onDispose { try { manager.unregisterNetworkCallback(callback) } catch (_: Exception) { } }
    }

    NetworkDoctorScreen(
        night = night,
        state = state,
        onBack = onBack,
        onRefresh = { refresh() },
        onUrlChange = { value ->
            state = NetworkProbePolicy.editedTarget(state, value)
        },
        onConsentChange = { granted ->
            state = state.copy(probeConsent = granted, probeResult = null, probeFailed = false)
        },
        onRunProbe = {
            if (NetworkProbePolicy.canRun(state.probeUrl, state.probeConsent, state.probing)) {
                val target = state.probeUrl
                state = state.copy(probing = true, probeResult = null, probeFailed = false)
                scope.launch {
                    state = try {
                        state.copy(probeResult = NetworkDoctor.probe(target, true), probing = false)
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Exception) {
                        state.copy(probing = false, probeFailed = true)
                    }
                }
            }
        },
    )
}

/**
 * Read-only diagnostics: every value is taken from Android at read time. The screen never
 * derives speed, protection or leak conclusions, and the probe receipt reports only its own
 * single measured request.
 */
@Composable
fun NetworkDoctorScreen(
    night: Boolean,
    state: NetworkUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onUrlChange: (String) -> Unit,
    onConsentChange: (Boolean) -> Unit,
    onRunProbe: () -> Unit,
) {
    val foreground = glassForeground(night)
    val reading = state.reading
    val warnTone = if (night) Color(0xFFFFC98A) else Color(0xFF8A5A00)
    val errorTone = if (night) Color(0xFFFFB4B4) else Color(0xFF8A1C1C)
    val yes = stringResource(R.string.answer_yes)
    val no = stringResource(R.string.answer_no)
    val invalidTarget = NetworkProbePolicy.targetProblem(state.probeUrl)

    Box(Modifier.fillMaxSize().testTag("route-network")) {
        Atmosphere(night, 0f)
        Column(
            Modifier.fillMaxSize().safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            KxPageHeader(
                title = stringResource(R.string.network_doctor),
                night = night,
                backLabel = stringResource(R.string.back),
                onBack = onBack,
            )

            GlassSurface(night) {
                Text(stringResource(R.string.network_readonly_title), fontWeight = FontWeight.Bold,
                    fontSize = 18.sp)
                Text(
                    stringResource(R.string.network_readonly_body),
                    fontSize = 13.sp, lineHeight = 19.sp,
                    color = foreground.copy(alpha = .86f),
                    modifier = Modifier.testTag("network-limits"),
                )
                Text(
                    stringResource(R.string.network_scope),
                    fontSize = 11.sp, lineHeight = 16.sp,
                    color = foreground.copy(alpha = .72f),
                    modifier = Modifier.testTag("network-scope"),
                )
            }

            GlassSurface(night) {
                Text(stringResource(R.string.network_measured_title), fontWeight = FontWeight.Bold,
                    fontSize = 18.sp)
                when {
                    reading != null -> {
                        MetricRow("network-metric-present", stringResource(R.string.network_metric_present),
                            if (reading.present) yes else no, night, warn = !reading.present)
                        MetricRow("network-metric-validated", stringResource(R.string.network_metric_validated),
                            if (reading.validated) yes else no, night)
                        MetricRow("network-metric-captive", stringResource(R.string.network_metric_captive),
                            if (reading.captive) yes else no, night, warn = reading.captive)
                        MetricRow("network-metric-vpn", stringResource(R.string.network_metric_vpn),
                            if (reading.vpn) yes else no, night)
                        Text(
                            stringResource(R.string.network_transports,
                                reading.transports.joinToString(" · ").ifEmpty { "—" }),
                            fontSize = 13.sp, lineHeight = 19.sp, color = foreground.copy(alpha = .88f),
                            modifier = Modifier.testTag("network-transports"),
                        )
                        Text(
                            stringResource(R.string.network_dns,
                                reading.dns.joinToString().ifEmpty {
                                    stringResource(R.string.network_dns_none)
                                }),
                            fontSize = 13.sp, lineHeight = 19.sp, color = foreground.copy(alpha = .88f),
                            modifier = Modifier.testTag("network-dns"),
                        )
                    }
                    state.readFailed -> Text(
                        stringResource(R.string.network_reading_failed),
                        fontSize = 13.sp, color = errorTone,
                        modifier = Modifier.testTag("network-read-failed"),
                    )
                    else -> Text(
                        stringResource(R.string.network_reading_pending),
                        fontSize = 13.sp, color = foreground.copy(alpha = .8f),
                        modifier = Modifier.testTag("network-reading-pending"),
                    )
                }
                OutlinedButton(
                    onClick = onRefresh,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("network-refresh"),
                ) { Text(stringResource(R.string.network_refresh)) }
            }

            GlassSurface(night) {
                Text(stringResource(R.string.network_probe_title), fontWeight = FontWeight.Bold,
                    fontSize = 18.sp)
                Text(
                    stringResource(R.string.network_probe_requires),
                    fontSize = 12.sp, lineHeight = 18.sp, color = foreground.copy(alpha = .8f),
                )
                OutlinedTextField(
                    state.probeUrl,
                    onUrlChange,
                    label = { Text(stringResource(R.string.probe_url)) },
                    singleLine = true,
                    enabled = !state.probing,
                    modifier = Modifier.fillMaxWidth().testTag("probe-url"),
                )
                if (invalidTarget) Text(
                    stringResource(R.string.network_probe_invalid),
                    fontSize = 12.sp, lineHeight = 18.sp, color = errorTone,
                    modifier = Modifier.testTag("probe-invalid"),
                )
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .toggleable(
                            value = state.probeConsent,
                            enabled = !state.probing,
                            role = Role.Checkbox,
                            onValueChange = onConsentChange,
                        )
                        .testTag("probe-consent"),
                ) {
                    Checkbox(checked = state.probeConsent, onCheckedChange = null,
                        enabled = !state.probing,
                        modifier = Modifier.testTag("probe-consent-box"))
                    Text(stringResource(R.string.probe_consent), Modifier.weight(1f),
                        fontSize = 13.sp, lineHeight = 19.sp, color = foreground.copy(alpha = .86f))
                }
                GlassAction(
                    label = stringResource(R.string.probe_run),
                    modifier = Modifier.testTag("probe-run"),
                    enabled = NetworkProbePolicy.canRun(state.probeUrl, state.probeConsent, state.probing),
                ) { onRunProbe() }
                if (state.probing) {
                    LinearProgressIndicator(Modifier.fillMaxWidth().testTag("probe-progress"))
                }
                if (state.probeFailed) Text(
                    stringResource(R.string.network_probe_failed),
                    fontSize = 13.sp, lineHeight = 19.sp, color = errorTone,
                    modifier = Modifier.testTag("probe-failed"),
                )
                state.probeResult?.let { result ->
                    Text(stringResource(R.string.network_probe_receipt_title),
                        fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = foreground,
                        modifier = Modifier.testTag("probe-receipt"))
                    Text(
                        stringResource(R.string.probe_result, result.code, result.elapsedMs),
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = foreground,
                        modifier = Modifier.testTag("probe-result"),
                    )
                    Text(
                        stringResource(R.string.network_probe_receipt_note),
                        fontSize = 11.sp, lineHeight = 16.sp, color = foreground.copy(alpha = .72f),
                        modifier = Modifier.testTag("probe-receipt-note"),
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** One measured line; merged semantics keep the label and its value in one announcement. */
@Composable
private fun MetricRow(tag: String, label: String, value: String, night: Boolean, warn: Boolean = false) {
    val foreground = glassForeground(night)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 40.dp).testTag(tag)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f), fontSize = 13.sp, color = foreground.copy(alpha = .8f))
        Text(
            value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            color = if (warn) (if (night) Color(0xFFFFC98A) else Color(0xFF8A5A00)) else foreground,
        )
    }
}
