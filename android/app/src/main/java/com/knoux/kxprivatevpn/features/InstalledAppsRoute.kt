package com.knoux.kxprivatevpn.features

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.knoux.kxprivatevpn.designsystem.KxServiceGlyph
import com.knoux.kxprivatevpn.platform.apps.InstalledApp
import com.knoux.kxprivatevpn.platform.apps.InstalledApps
import com.knoux.kxprivatevpn.platform.apps.canRequestUninstall
import com.knoux.kxprivatevpn.platform.apps.isProtectedSystemApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun InstalledAppsRoute(night: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val inventory = remember(appContext) { InstalledApps(appContext) }
    val model: AppInventoryModel = viewModel(factory = viewModelFactory {
        initializer {
            AppInventoryModel(
                ownPackage = appContext.packageName,
                readInventory = inventory::inventory,
                verifyInstalled = inventory::isInstalled,
            )
        }
    })
    val state by model.state.collectAsState()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // RESULT_OK, RESULT_CANCELED and a vanished launcher entry are not uninstall proof.
        model.returnedFromAndroid()
    }
    fun launch(intent: Intent): Boolean = try {
        launcher.launch(intent)
        true
    } catch (_: android.content.ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
    InstalledAppsScreen(
        night = night, state = state, ownPackage = appContext.packageName,
        onBack = onBack, onScan = model::scan, onCancel = model::cancelScan,
        onQuery = model::query, onFilter = model::filter, onSort = model::sort,
        onRequestUninstall = model::requestUninstall, onDismissApproval = model::dismissApproval,
        onConfirmUninstall = {
            model.confirmUninstall { approved ->
                val intent = safeUninstallIntent(appContext, approved)
                intent != null && launch(intent)
            }
        },
        onAppSettings = { pkg -> model.openDetails(pkg) { app ->
            launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", app.packageName, null)))
        } },
        onAllSettings = { model.openSettings { launch(Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)) } },
        icon = { app -> InstalledAppIcon(app) },
    )
}

internal fun safeUninstallIntent(context: Context, approved: InstalledApp): Intent? {
    if (!canRequestUninstall(approved, context.packageName)) return null
    // Re-read protection at the execution boundary; a stale scan cannot authorize removal.
    val current = try {
        @Suppress("DEPRECATION")
        context.packageManager.getApplicationInfo(approved.packageName, 0)
    } catch (_: PackageManager.NameNotFoundException) {
        return null
    } catch (_: SecurityException) {
        return null
    }
    if (isProtectedSystemApp(current.flags)) return null
    // Android owns the separate confirmation. No silent removal API is used.
    return Intent(Intent.ACTION_DELETE, Uri.fromParts("package", approved.packageName, null))
        .putExtra(Intent.EXTRA_RETURN_RESULT, true)
}

@Composable
private fun InstalledAppIcon(app: InstalledApp) {
    val context = LocalContext.current
    val image by produceState<ImageBitmap?>(null, app.packageName) {
        value = null
        value = withContext(Dispatchers.IO) {
            try {
                val drawable = context.packageManager.getApplicationIcon(app.packageName)
                val bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
                drawable.setBounds(0, 0, 96, 96)
                drawable.draw(android.graphics.Canvas(bitmap))
                bitmap.asImageBitmap()
            } catch (_: PackageManager.NameNotFoundException) {
                null
            } catch (_: SecurityException) {
                null
            }
        }
    }
    image?.let { Image(it, contentDescription = null, modifier = Modifier.size(40.dp)) }
        ?: KxServiceGlyph("apps", MaterialTheme.colorScheme.primary, Modifier.size(40.dp))
}
