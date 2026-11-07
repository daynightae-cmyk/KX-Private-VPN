package com.knoux.kxprivatevpn.features

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.knoux.kxprivatevpn.R
import com.knoux.kxprivatevpn.platform.vpn.VpnPathEvidence
import com.knoux.kxprivatevpn.platform.vpn.VpnPathOutcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun VpnPathPanel(
    enabled: Boolean,
    verify: suspend (String, Boolean) -> VpnPathEvidence,
) {
    var url by remember { mutableStateOf("") }
    var approved by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var evidence by remember { mutableStateOf<VpnPathEvidence?>(null) }
    val scope = rememberCoroutineScope()
    CrystalPanel {
        Text(stringResource(R.string.vpn_path_title))
        Text(stringResource(R.string.vpn_path_scope))
        OutlinedTextField(url, {
            if (it.length <= 2048) { url = it; approved = false; evidence = null }
        }, label = { Text(stringResource(R.string.probe_url)) }, singleLine = true,
            enabled = enabled && !busy, modifier = Modifier.fillMaxWidth().testTag("vpn-path-url"))
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("vpn-path-approval")
            .toggleable(value = approved, enabled = enabled && !busy, role = Role.Checkbox,
                onValueChange = { approved = it })) {
            Checkbox(checked = approved, onCheckedChange = null)
            Text(stringResource(R.string.probe_consent), Modifier.weight(1f))
        }
        Button(onClick = {
            busy = true
            evidence = null
            val requestedUrl = url
            scope.launch {
                try { evidence = verify(requestedUrl, approved) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { evidence = VpnPathEvidence(VpnPathOutcome.UNAVAILABLE) }
                finally { busy = false }
            }
        }, enabled = enabled && approved && url.isNotBlank() && !busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("vpn-path-run")) {
            Text(stringResource(R.string.vpn_path_run))
        }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth().testTag("vpn-path-busy"))
        evidence?.let { result ->
            Text(stringResource(when (result.outcome) {
                VpnPathOutcome.VERIFIED -> R.string.vpn_path_verified
                VpnPathOutcome.NO_OWNED_TUNNEL -> R.string.vpn_path_no_tunnel
                VpnPathOutcome.NOT_ROUTED_THROUGH_VPN -> R.string.vpn_path_not_routed
                VpnPathOutcome.NETWORK_CHANGED -> R.string.vpn_path_changed
                VpnPathOutcome.NO_RECENT_HANDSHAKE -> R.string.vpn_path_no_handshake
                VpnPathOutcome.NO_RECEIVED_TRAFFIC -> R.string.vpn_path_no_traffic
                VpnPathOutcome.HTTP_FAILED -> R.string.vpn_path_http_failed
                VpnPathOutcome.UNAVAILABLE -> R.string.vpn_path_unavailable
            }), modifier = Modifier.testTag("vpn-path-result"))
            result.httpCode?.let { code ->
                Text(stringResource(R.string.vpn_path_measurements, code, result.rxDelta, result.txDelta))
                Text(stringResource(R.string.vpn_path_routes,
                    stringResource(if (result.defaultIpv4) R.string.answer_yes else R.string.answer_no),
                    stringResource(if (result.defaultIpv6) R.string.answer_yes else R.string.answer_no),
                    stringResource(if (result.dnsConfigured) R.string.answer_yes else R.string.answer_no)))
            }
        }
    }
}
