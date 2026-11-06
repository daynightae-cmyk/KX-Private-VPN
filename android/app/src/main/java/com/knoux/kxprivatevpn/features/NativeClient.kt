package com.knoux.kxprivatevpn.features

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.knoux.kxprivatevpn.R
import com.knoux.kxprivatevpn.designsystem.Atmosphere
import com.knoux.kxprivatevpn.designsystem.GlassAction
import com.knoux.kxprivatevpn.designsystem.KxOrb
import com.knoux.kxprivatevpn.launch.IntroTimeline

@Composable
internal fun CrystalPanel(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(Modifier.fillMaxWidth()
        .background(Brush.linearGradient(listOf(Color.White.copy(alpha = .23f),
            Color(0xFF7679E8).copy(alpha = .19f), Color(0xFFABF7FF).copy(alpha = .20f))), shape)
        .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = .8f),
            Color(0xFF9CDFFF).copy(alpha = .5f), Color(0xFFD9B3FF).copy(alpha = .8f))), shape)
        .padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

@Composable
fun NativeClient(route: String, state: ClientState, model: ClientViewModel, night: Boolean,
                 navigate: (String) -> Unit, back: () -> Unit) {
    var repairBusy by remember { mutableStateOf(false) }
    val vpn by model.vpnState.collectAsStateWithLifecycle()
    val consent = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) model.connectSelected() else model.consentDenied()
    }
    val reconnectConsent = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) model.reconnectSelected()
    }
    val context = LocalContext.current
    // Ask once per process, before the VPN consent dialog. A denial never blocks the tunnel;
    // it only hides the ongoing session notification.
    var notificationAsked by rememberSaveable { mutableStateOf(false) }
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { model.prepareConnect()?.let(consent::launch) }
    fun connectWithPermission() {
        val missing = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (missing && !notificationAsked) {
            notificationAsked = true
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            model.prepareConnect()?.let(consent::launch)
        }
    }
    val foreground = if (night) Color(0xFFF7F3FF) else Color(0xFF171050)
    MaterialTheme(colorScheme = if (night) darkColorScheme(primary = Color(0xFF99EBFF))
        else lightColorScheme(primary = Color(0xFF4420B3))) {
        CompositionLocalProvider(LocalContentColor provides foreground) {
            if (route == "home") {
                KxHomeScreen(
                    night = night,
                    snapshot = state.snapshot,
                    busy = state.busy,
                    vpn = vpn,
                    onConnect = { connectWithPermission() },
                    onDisconnect = { model.disconnectVpn() },
                    onSession = { navigate("session") },
                    onDismissIssue = { model.dismissVpnIssue() },
                    onNavigate = navigate,
                )
                return@CompositionLocalProvider
            }
            if (route == "servers") {
                ServerSelectionRoute(state, model, night, back) { navigate("import") }
                return@CompositionLocalProvider
            }
            if (route == "security") {
                SecurityRoute(model, vpn, night, onBack = back, onSession = { navigate("session") })
                return@CompositionLocalProvider
            }
            if (route == "split") {
                SplitTunnelRoute(model, night, onBack = back) {
                    model.prepareReconnect()?.let(reconnectConsent::launch)
                }
                return@CompositionLocalProvider
            }
            if (route == "advanced") {
                AdvancedNetworkRoute(model, night, onBack = back) {
                    model.prepareReconnect()?.let(reconnectConsent::launch)
                }
                return@CompositionLocalProvider
            }
            if (route == "stats") {
                StatisticsRoute(model, night, onBack = back, onNavigate = { target ->
                    if (target == "home") back() else navigate(target)
                })
                return@CompositionLocalProvider
            }
            if (route == "repair") {
                RepairLabRoute(night, onBack = back, onNavigate = navigate)
                return@CompositionLocalProvider
            }
            if (route == "storage") {
                StorageXrayRoute(night, onBack = back)
                return@CompositionLocalProvider
            }
            if (route == "apps") {
                InstalledAppsRoute(night, onBack = back)
                return@CompositionLocalProvider
            }
            if (route == "ghost") {
                GhostHunterRoute(night, onBack = back)
                return@CompositionLocalProvider
            }
            if (route == "duplicate") {
                DuplicateDnaRoute(night, onBack = back)
                return@CompositionLocalProvider
            }
            if (route == "heavyweight") {
                HeavyweightRoute(night, onBack = back)
                return@CompositionLocalProvider
            }
            if (route == "media") {
                MediaSlimRoute(night, onBack = back)
                return@CompositionLocalProvider
            }
            if (route.startsWith("repair/")) {
                RepairServiceRoute(route, night, onBack = back)
                return@CompositionLocalProvider
            }
            Box(Modifier.fillMaxSize()) {
                Atmosphere(night, 0f)
                Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()).testTag("route-$route"),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = back, enabled = !state.busy && !repairBusy,
                            modifier = Modifier.heightIn(min = 48.dp).testTag("back")) { Text(stringResource(R.string.back)) }
                        Spacer(Modifier.weight(1f))
                        Text("KX PRIVATE VPN", fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    }
                    Text(stringResource(when (route) {
                        "education" -> R.string.setup_title
                        "import" -> R.string.import_config
                        "servers" -> R.string.servers
                        "settings" -> R.string.settings
                        "storage" -> R.string.storage
                        "cache" -> R.string.cache_surgeon
                        "network" -> R.string.network_doctor
                        "session" -> R.string.session_title
                        else -> R.string.home
                    }), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    if (state.issue != null) CrystalPanel {
                        Text(stringResource(when (state.issue) {
                            ClientIssue.INVALID_CONFIG -> R.string.invalid_config
                            ClientIssue.INPUT -> R.string.input_error
                            ClientIssue.STORE -> R.string.store_error
                            ClientIssue.SAVE -> R.string.save_failed
                        }), modifier = Modifier.testTag("client-error"))
                        TextButton(onClick = if (state.loaded) model::dismissIssue else model::reload,
                            enabled = !state.busy) { Text(stringResource(R.string.retry)) }
                    }
                    when (route) {
                        "education" -> CrystalPanel {
                            Text(stringResource(R.string.setup_body), fontSize = 18.sp)
                            Button(onClick = model::onboard, enabled = state.loaded && !state.busy,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("onboard")) {
                                Text(stringResource(R.string.setup_continue))
                            }
                        }
                        "import" -> ImportScreen(state, model)
                        "servers" -> ProfileList(state, model)
                        "settings" -> PreferenceScreen(state, model)
                        "cache" -> CacheScreen { repairBusy = it }
                        "network" -> NetworkScreen()
                        "session" -> VpnSessionScreen(vpn) { model.disconnectVpn() }
                        else -> {
                            KxOrb(1f, IntroTimeline.frame(1f), Modifier.size(210.dp).align(Alignment.CenterHorizontally))
                            Text(stringResource(when (vpn.phase) {
                                com.knoux.kxprivatevpn.platform.vpn.VpnPhase.CONNECTED -> R.string.connected
                                com.knoux.kxprivatevpn.platform.vpn.VpnPhase.CONNECTING -> R.string.connecting
                                com.knoux.kxprivatevpn.platform.vpn.VpnPhase.DISCONNECTING -> R.string.disconnecting
                                else -> R.string.disconnected
                            }), fontSize = 26.sp,
                                fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
                            VpnHomePanel(state.snapshot, state.busy, vpn,
                                onConnect = { connectWithPermission() },
                                onDisconnect = { model.disconnectVpn() },
                                onSession = { navigate("session") },
                                onDismissIssue = { model.dismissVpnIssue() })
                            GlassAction(stringResource(R.string.import_config), Modifier.testTag("open-import")) { navigate("import") }
                            GlassAction(stringResource(R.string.repair_lab), Modifier.testTag("open-repair")) { navigate("repair") }
                            listOf("security" to R.string.security_center,
                                "split" to R.string.split_title,
                                "advanced" to R.string.adv_title,
                                "stats" to R.string.stats_title,
                                "servers" to R.string.servers, "storage" to R.string.storage,
                                "cache" to R.string.cache_surgeon, "network" to R.string.network_doctor,
                                "settings" to R.string.settings).forEach { (target, label) ->
                                OutlinedButton(onClick = { navigate(target) }, modifier = Modifier.fillMaxWidth()
                                    .heightIn(min = 52.dp).testTag("open-$target")) { Text(stringResource(label)) }
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ImportScreen(state: ClientState, model: ClientViewModel) {
    var name by remember { mutableStateOf("") }
    // Never save configuration/private keys into savedInstanceState or a navigation argument.
    var secret by remember { mutableStateOf("") }
    var manual by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = generateSequence(LocalView.current.context) { (it as? android.content.ContextWrapper)?.baseContext }
        .filterIsInstance<ComponentActivity>().firstOrNull()
    DisposableEffect(activity) {
        val alreadySecure = activity?.window?.attributes?.flags?.and(WindowManager.LayoutParams.FLAG_SECURE) != 0
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { if (!alreadySecure) activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
    val file = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) model.importFile(name, uri, false)
    }
    val qr = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) model.importFile(name, uri, true)
    }
    CrystalPanel {
        OutlinedTextField(name, { if (it.length <= 48) name = it }, label = { Text(stringResource(R.string.profile_name)) },
            supportingText = { Text(stringResource(R.string.profile_name_hint)) }, singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("profile-name"), enabled = !state.busy)
        val enabled = state.loaded && !state.busy && name.isNotBlank()
        Button(onClick = { file.launch(arrayOf("*/*")) }, enabled = enabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("import-file")) { Text(stringResource(R.string.import_file)) }
        Button(onClick = { qr.launch(arrayOf("image/*")) }, enabled = enabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("import-qr")) { Text(stringResource(R.string.import_qr)) }
        OutlinedButton(onClick = {
            try {
                val text = context.getSystemService(ClipboardManager::class.java).primaryClip
                    ?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text
                if (text == null || text.length > 65536) model.inputError()
                else { secret = text.toString(); manual = true }
            } catch (_: Exception) { model.inputError() }
        }, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.paste_clipboard))
        }
        OutlinedButton(onClick = { manual = !manual }, enabled = !state.busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("manual")) { Text(stringResource(R.string.manual_setup)) }
        if (manual) {
            OutlinedTextField(secret, { if (it.length <= 65536) secret = it },
                label = { Text(stringResource(R.string.configuration)) }, visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp).testTag("config-text"), enabled = !state.busy)
            Button(onClick = { model.importText(name, secret) }, enabled = enabled && secret.isNotBlank(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("save-profile")) { Text(stringResource(R.string.save_profile)) }
        }
        Text(stringResource(R.string.import_privacy), fontSize = 13.sp)
    }
}

@Composable
private fun ProfileList(state: ClientState, model: ClientViewModel) {
    var removing by remember { mutableStateOf<String?>(null) }
    val title = stringResource(R.string.remove_title)
    val body = stringResource(R.string.remove_body)
    val remove = stringResource(R.string.remove_profile)
    val cancel = stringResource(R.string.cancel)
    if (state.snapshot.profiles.isEmpty()) CrystalPanel {
        Text(stringResource(R.string.no_profiles), fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.no_profiles_body))
    }
    state.snapshot.profiles.forEach { profile -> CrystalPanel {
        Text(profile.name, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(profile.endpoint, fontSize = 14.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { model.select(profile.id) }, enabled = !state.busy,
                modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(if (profile.id == state.snapshot.selectedId) R.string.profile_selected else R.string.select_profile))
            }
            TextButton(onClick = { removing = profile.id }, enabled = !state.busy,
                modifier = Modifier.heightIn(min = 48.dp)) { Text(remove) }
        }
    } }
    if (removing != null) AlertDialog(onDismissRequest = { removing = null }, title = { Text(title) },
        text = { Text(body, Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(onClick = { removing?.let(model::remove); removing = null }) { Text(remove) } },
        dismissButton = { TextButton(onClick = { removing = null }) { Text(cancel) } })
}

@Composable
private fun PreferenceScreen(state: ClientState, model: ClientViewModel) {
    CrystalPanel {
        Text(stringResource(R.string.local_account))
        Text(stringResource(R.string.language), fontWeight = FontWeight.Bold)
        listOf("system" to stringResource(R.string.system_default), "en" to "English", "ar" to "العربية").forEach { (id, label) ->
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(
                selected = state.settings.language == id, enabled = !state.busy, role = Role.RadioButton,
                onClick = { model.appearance(id, state.settings.theme) }), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = state.settings.language == id, onClick = null, enabled = !state.busy)
                Text(label)
            }
        }
        Text(stringResource(R.string.appearance), fontWeight = FontWeight.Bold)
        listOf("system" to R.string.system_default, "night" to R.string.night, "day" to R.string.day).forEach { (id, label) ->
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(
                selected = state.settings.theme == id, enabled = !state.busy, role = Role.RadioButton,
                onClick = { model.appearance(state.settings.language, id) }), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = state.settings.theme == id, onClick = null, enabled = !state.busy)
                Text(stringResource(label))
            }
        }
        Text(stringResource(R.string.pref_notifications_unavailable), fontSize = 12.sp,
            modifier = Modifier.testTag("pref-notifications"))
        OutlinedButton(onClick = { model.appearance("system", "system") }, enabled = !state.busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("pref-reset")) {
            Text(stringResource(R.string.pref_reset))
        }
        Text(stringResource(R.string.pref_reset_hint), fontSize = 11.sp)
        Text(stringResource(R.string.pref_privacy), fontSize = 11.sp, modifier = Modifier.testTag("pref-privacy"))
    }
}
