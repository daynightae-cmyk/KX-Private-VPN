package com.knoux.kxprivatevpn.features

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knoux.kxprivatevpn.R
import com.knoux.kxprivatevpn.designsystem.Atmosphere
import com.knoux.kxprivatevpn.designsystem.KxOrb
import com.knoux.kxprivatevpn.launch.IntroTimeline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** This is a scoped launcher-app inventory, NOT a claim to list Android's hidden apps. */
internal data class VisibleApp(
    val packageName: String,
    val name: String,
    val system: Boolean,
)

internal fun isProtectedSystemApp(flags: Int): Boolean =
    flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

internal fun queryVisibleLaunchableApps(context: Context): List<VisibleApp> {
    val pm = context.packageManager
    val request = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val resolved = if (Build.VERSION.SDK_INT >= 33) {
        pm.queryIntentActivities(request, PackageManager.ResolveInfoFlags.of(0L))
    } else {
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(request, 0)
    }
    return resolved.mapNotNull { activity ->
        val info = activity.activityInfo?.applicationInfo ?: return@mapNotNull null
        val pkg = info.packageName
        if (pkg.isBlank()) return@mapNotNull null
        VisibleApp(pkg, pm.getApplicationLabel(info).toString(), isProtectedSystemApp(info.flags))
    }.distinctBy { it.packageName }.sortedWith(compareBy({ it.system }, { it.name.lowercase() }))
}

@Composable
fun InstalledAppsRoute(night: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var apps by remember { mutableStateOf<List<VisibleApp>?>(null) }
    var scanning by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf("all") }
    var uninstall by remember { mutableStateOf<VisibleApp?>(null) }
    val title = stringResource(R.string.app_inventory_title)
    val foreground = if (night) androidx.compose.ui.graphics.Color(0xFFF7F3FF)
        else androidx.compose.ui.graphics.Color(0xFF171050)

    fun scan() {
        if (scanning) return
        scanning = true
        failed = false
        scope.launch {
            try {
                apps = withContext(Dispatchers.Default) {
                    queryVisibleLaunchableApps(context.applicationContext)
                }
            } catch (_: Exception) {
                apps = null
                failed = true
            } finally {
                scanning = false
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Atmosphere(night, 0f)
        LazyColumn(
            modifier = Modifier.fillMaxSize().safeDrawingPadding()
                .padding(horizontal = 16.dp).testTag("route-apps"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onBack, modifier = Modifier.testTag("apps-back")) {
                        Text(stringResource(R.string.back))
                    }
                    Spacer(Modifier.weight(1f))
                    Text(title, style = MaterialTheme.typography.titleLarge, color = foreground)
                }
            }
            item {
                CrystalPanel {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        KxOrb(1f, IntroTimeline.frame(1f), Modifier.size(100.dp)
                            .testTag("apps-scan-orb")
                            .semantics { contentDescription = title }
                            .clickable(enabled = !scanning, role = Role.Button) { scan() })
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.app_inventory_scan), color = foreground)
                            Text(stringResource(R.string.app_inventory_scope), color = foreground,
                                fontSize = 12.sp, lineHeight = 17.sp)
                        }
                    }
                    if (scanning) LinearProgressIndicator(Modifier.fillMaxWidth())
                    if (failed) Text(stringResource(R.string.app_inventory_failure), color = foreground)
                    TextButton(onClick = {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }, modifier = Modifier.fillMaxWidth().testTag("apps-system-settings")) {
                        Text(stringResource(R.string.app_inventory_all_settings))
                    }
                }
            }
            if (apps != null) {
                val all = apps.orEmpty()
                val shown = all.filter {
                    filter == "all" || (filter == "system") == it.system
                }
                item {
                    CrystalPanel {
                        Text(stringResource(R.string.app_inventory_counts,
                            all.count { !it.system }, all.count { it.system }), color = foreground)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                "all" to R.string.app_inventory_filter_all,
                                "user" to R.string.app_inventory_filter_user,
                                "system" to R.string.app_inventory_filter_system,
                            ).forEach { (value, textId) ->
                                TextButton(onClick = { filter = value }, modifier = Modifier
                                    .weight(1f).testTag("apps-filter-$value")) {
                                    Text(stringResource(textId), fontSize = 12.sp,
                                        color = foreground)
                                }
                            }
                        }
                    }
                }
                items(shown, key = { it.packageName }) { app ->
                    CrystalPanel {
                        Text(app.name, style = MaterialTheme.typography.titleMedium,
                            color = foreground)
                        Text(app.packageName, fontSize = 11.sp, color = foreground)
                        Text(stringResource(if (app.system)
                            R.string.app_inventory_system_protected
                            else R.string.app_inventory_user_app), color = foreground,
                            fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.parse("package:${app.packageName}"))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            }) { Text(stringResource(R.string.app_inventory_app_info)) }
                            if (!app.system && app.packageName != context.packageName) {
                                TextButton(onClick = { uninstall = app },
                                    modifier = Modifier.testTag("apps-uninstall-${app.packageName}")) {
                                    Text(stringResource(R.string.app_inventory_uninstall))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    uninstall?.let { app ->
        AlertDialog(onDismissRequest = { uninstall = null },
            title = { Text(stringResource(R.string.app_inventory_confirm_title)) },
            text = { Text(stringResource(R.string.app_inventory_confirm_body, app.name)) },
            confirmButton = {
                TextButton(onClick = {
                    uninstall = null
                    // No silent uninstall. Android displays its own separate confirmation.
                    context.startActivity(Intent(Intent.ACTION_DELETE,
                        Uri.parse("package:${app.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }, modifier = Modifier.testTag("apps-confirm-uninstall")) {
                    Text(stringResource(R.string.app_inventory_uninstall))
                }
            },
            dismissButton = {
                TextButton(onClick = { uninstall = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
