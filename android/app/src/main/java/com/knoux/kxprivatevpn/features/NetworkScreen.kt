package com.knoux.kxprivatevpn.features

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.LinkProperties
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.knoux.kxprivatevpn.R
import com.knoux.kxprivatevpn.platform.network.*
import kotlinx.coroutines.*

@Composable
fun NetworkScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var reading by remember { mutableStateOf<NetworkReading?>(null) }
    var failed by remember { mutableStateOf(false) }
    var probeFailed by remember { mutableStateOf(false) }
    var url by remember { mutableStateOf("") }
    var consent by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<HttpsReading?>(null) }
    fun refresh() {
        try { reading = NetworkDoctor.read(context); failed = false }
        catch (_: Exception) { failed = true }
    }
    DisposableEffect(context) {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { refresh() }
            override fun onLost(network: Network) { refresh() }
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) { refresh() }
            override fun onLinkPropertiesChanged(network: Network, properties: LinkProperties) { refresh() }
        }
        try { manager.registerDefaultNetworkCallback(callback); refresh() }
        catch (_: Exception) { failed = true }
        onDispose { try { manager.unregisterNetworkCallback(callback) } catch (_: Exception) { } }
    }
    CrystalPanel {
        Text(stringResource(R.string.network_scope))
        reading?.let {
            Text(stringResource(if (it.present) R.string.network_present else R.string.network_absent), Modifier.testTag("network-status"))
            Text(stringResource(R.string.network_validated, stringResource(if (it.validated) R.string.answer_yes else R.string.answer_no)))
            Text(stringResource(R.string.network_captive, stringResource(if (it.captive) R.string.answer_yes else R.string.answer_no)))
            Text(stringResource(R.string.network_vpn, stringResource(if (it.vpn) R.string.answer_yes else R.string.answer_no)))
            Text(it.transports.joinToString(" · ").ifEmpty { "—" })
            Text(stringResource(R.string.network_dns, it.dns.joinToString().ifEmpty { "—" }))
        }
        if (failed || probeFailed) Text(stringResource(R.string.input_error))
        OutlinedTextField(url, { if (it.length <= 2048) { url = it; consent = false; result = null } },
            label = { Text(stringResource(R.string.probe_url)) }, singleLine = true, enabled = !busy,
            modifier = Modifier.fillMaxWidth().testTag("probe-url"))
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(value = consent,
            enabled = !busy, role = Role.Checkbox, onValueChange = { consent = it })) {
            Checkbox(checked = consent, onCheckedChange = null)
            Text(stringResource(R.string.probe_consent), Modifier.weight(1f))
        }
        Button(onClick = {
            busy = true; probeFailed = false; result = null
            scope.launch {
                try { result = NetworkDoctor.probe(url, consent) }
                catch (_: TimeoutCancellationException) { probeFailed = true }
                catch (e: CancellationException) { throw e }
                catch (_: Exception) { probeFailed = true }
                finally { busy = false }
            }
        }, enabled = consent && url.isNotBlank() && !busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("probe-run")) { Text(stringResource(R.string.probe_run)) }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        result?.let { Text(stringResource(R.string.probe_result, it.code, it.elapsedMs)) }
    }
}
