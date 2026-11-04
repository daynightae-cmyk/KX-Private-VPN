package com.knoux.kxprivatevpn.features

import android.content.Context
import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knoux.kxprivatevpn.R
import com.knoux.kxprivatevpn.data.config.ProfileSnapshot
import com.knoux.kxprivatevpn.designsystem.GlassAction
import com.knoux.kxprivatevpn.platform.vpn.VpnIssue
import com.knoux.kxprivatevpn.platform.vpn.VpnLinkInfo
import com.knoux.kxprivatevpn.platform.vpn.VpnLinkReader
import com.knoux.kxprivatevpn.platform.vpn.VpnPhase
import com.knoux.kxprivatevpn.platform.vpn.VpnState
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

@Composable
fun VpnHomePanel(
    snapshot: ProfileSnapshot,
    busy: Boolean,
    vpn: VpnState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onSession: () -> Unit,
    onDismissIssue: () -> Unit,
) {
    val context = LocalContext.current
    val selected = snapshot.profiles.firstOrNull { it.id == snapshot.selectedId }
    val session = vpn.session
    CrystalPanel {
        Text(
            selected?.name ?: stringResource(R.string.no_profiles),
            fontWeight = FontWeight.Bold, fontSize = 20.sp,
            modifier = Modifier.testTag("home-profile"),
        )
        if (selected != null && selected.endpoint.isNotBlank()) Text(selected.endpoint, fontSize = 14.sp)
        when (vpn.phase) {
            VpnPhase.CONNECTED -> {
                Text(
                    stringResource(R.string.tunnel_active), fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("vpn-connected"),
                )
                Text(
                    stringResource(
                        R.string.traffic_counters,
                        Formatter.formatFileSize(context, session?.rxBytes ?: 0),
                        Formatter.formatFileSize(context, session?.txBytes ?: 0),
                    ),
                )
                Text(stringResource(R.string.vpn_stats_scope), fontSize = 12.sp)
                val handshake = session?.latestHandshakeMillis
                if (handshake != null) {
                    Text(stringResource(R.string.handshake_value, formatTime(context, handshake)))
                } else {
                    Text(stringResource(R.string.no_handshake), fontSize = 13.sp)
                }
                TextButton(
                    onClick = onSession,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("open-session"),
                ) { Text(stringResource(R.string.session_details)) }
                GlassAction(
                    stringResource(R.string.disconnect),
                    Modifier.testTag("vpn-disconnect"),
                ) { onDisconnect() }
            }
            VpnPhase.CONNECTING, VpnPhase.DISCONNECTING -> {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(stringResource(if (vpn.phase == VpnPhase.CONNECTING) R.string.connecting else R.string.disconnecting))
            }
            else -> {
                GlassAction(
                    stringResource(R.string.connect),
                    Modifier.testTag("vpn-connect"), enabled = selected != null && !busy,
                ) { onConnect() }
                Text(
                    stringResource(if (selected == null) R.string.no_profiles_body else R.string.connect_hint),
                    fontSize = 13.sp,
                )
            }
        }
    }
    vpn.issue?.let { issue ->
        CrystalPanel {
            Text(vpnIssueText(issue), modifier = Modifier.testTag("vpn-issue"))
            TextButton(
                onClick = onDismissIssue,
                modifier = Modifier.heightIn(min = 48.dp).testTag("vpn-dismiss-issue"),
            ) { Text(stringResource(R.string.dismiss)) }
        }
    }
}

@Composable
fun VpnSessionScreen(vpn: VpnState, onDisconnect: () -> Unit) {
    val context = LocalContext.current
    val session = vpn.session
    val link by produceState(VpnLinkInfo(), vpn.phase) {
        while (true) {
            value = VpnLinkReader.read(context)
            if (vpn.phase != VpnPhase.CONNECTED) break
            delay(2_000)
        }
    }
    if (session == null) {
        CrystalPanel { Text(stringResource(R.string.no_active_session)) }
        return
    }
    CrystalPanel {
        Text(session.profileName, fontWeight = FontWeight.Bold, fontSize = 20.sp,
            modifier = Modifier.testTag("session-profile"))
        Text(session.endpoint, fontSize = 14.sp)
        val started = session.startedAtMillis
        if (started != null) {
            Text(stringResource(R.string.connected_since, formatTime(context, started)))
        } else if (vpn.phase == VpnPhase.CONNECTED) {
            Text(stringResource(R.string.connected_at_unknown))
        }
        Text(
            stringResource(
                R.string.traffic_counters,
                Formatter.formatFileSize(context, session.rxBytes),
                Formatter.formatFileSize(context, session.txBytes),
            ),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.testTag("session-traffic"),
        )
        Text(stringResource(R.string.vpn_stats_scope), fontSize = 12.sp)
        val handshake = session.latestHandshakeMillis
        if (handshake != null) {
            Text(stringResource(R.string.handshake_value, formatTime(context, handshake)))
        } else {
            Text(stringResource(R.string.no_handshake), fontSize = 13.sp)
        }
        session.backendVersion?.let {
            Text(stringResource(R.string.backend_version, it), fontSize = 12.sp)
        }
    }
    CrystalPanel {
        Text(stringResource(R.string.session_config_scope), fontSize = 12.sp)
        Detail(R.string.interface_addresses, session.addresses)
        Detail(R.string.allowed_routes, session.allowedIps)
        Detail(R.string.dns_servers, session.dnsServers)
        Text("${stringResource(R.string.mtu_label)}: ${session.mtu ?: stringResource(R.string.unknown_value)}")
    }
    CrystalPanel {
        Text(stringResource(R.string.android_link), fontWeight = FontWeight.Bold)
        if (link.present) {
            Text(
                stringResource(
                    R.string.android_link_present,
                    link.interfaceName ?: stringResource(R.string.unknown_value),
                    link.routes,
                    link.mtu?.toString() ?: stringResource(R.string.unknown_value),
                ),
                modifier = Modifier.testTag("session-link-present"),
            )
            Detail(R.string.interface_addresses, link.addresses)
            Detail(R.string.dns_servers, link.dns)
        } else {
            Text(stringResource(R.string.android_link_absent), modifier = Modifier.testTag("session-link-absent"))
        }
    }
    if (vpn.phase == VpnPhase.CONNECTED || vpn.phase == VpnPhase.DISCONNECTING) {
        GlassAction(
            stringResource(R.string.disconnect),
            Modifier.testTag("session-disconnect"),
            enabled = vpn.phase == VpnPhase.CONNECTED,
        ) { onDisconnect() }
    }
    vpn.issue?.let { issue ->
        CrystalPanel { Text(vpnIssueText(issue), modifier = Modifier.testTag("session-issue")) }
    }
}

@Composable
private fun Detail(label: Int, values: List<String>) {
    Text(stringResource(label), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    Text(
        if (values.isEmpty()) stringResource(R.string.unknown_value) else values.joinToString(", "),
        fontSize = 13.sp,
    )
}

@Composable
fun vpnIssueText(issue: VpnIssue): String = stringResource(
    when (issue) {
        VpnIssue.NO_PROFILE -> R.string.vpn_issue_no_profile
        VpnIssue.CONSENT_DENIED -> R.string.vpn_issue_consent_denied
        VpnIssue.CONFIG_UNREADABLE -> R.string.vpn_issue_config_unreadable
        VpnIssue.CONFIG_INVALID -> R.string.vpn_issue_config_invalid
        VpnIssue.AUTHORIZATION -> R.string.vpn_issue_authorization
        VpnIssue.START_FAILED -> R.string.vpn_issue_start_failed
        VpnIssue.STOP_FAILED -> R.string.vpn_issue_stop_failed
        VpnIssue.BACKEND_STOPPED -> R.string.vpn_issue_backend_stopped
    },
)

internal fun formatTime(context: Context, millis: Long): String {
    val locale = context.resources.configuration.locales[0]
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale)
        .format(Date(millis))
}
