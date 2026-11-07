package com.knoux.kxprivatevpn.platform.apps

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.util.Locale

/** Optional metadata is absent, never inferred, when Android cannot supply it. */
data class InstalledApp(
    val packageName: String,
    val label: String,
    val system: Boolean = false,
    val versionName: String? = null,
    val targetSdk: Int? = null,
)

internal fun isProtectedSystemApp(flags: Int): Boolean =
    flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

internal fun canRequestUninstall(app: InstalledApp, ownPackage: String): Boolean =
    app.packageName.isNotBlank() && app.packageName == app.packageName.trim() &&
        app.packageName != ownPackage && !app.system

/**
 * One scoped PackageManager query for both the routing picker and app inventory.
 * Android 11+ reveals the launcher packages matched by <queries>, not hidden packages
 * or another user's/work profile's apps. No storage usage or uninstall result is inferred.
 */
class InstalledApps(private val context: Context) {
    /** Preserve the routing picker's original contract: exclude KX and omit metadata. */
    suspend fun list(): List<InstalledApp> = readApps(includeSelf = false, metadata = false)

    suspend fun inventory(): List<InstalledApp> = readApps(includeSelf = true, metadata = true)

    private suspend fun readApps(includeSelf: Boolean, metadata: Boolean): List<InstalledApp> =
        withContext(Dispatchers.IO) {
            val manager = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                manager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                manager.queryIntentActivities(intent, 0)
            }
            val seen = mutableSetOf<String>()
            resolved.mapNotNull { info ->
                ensureActive()
                val appInfo = info.activityInfo?.applicationInfo ?: return@mapNotNull null
                val packageName = appInfo.packageName
                if (packageName.isBlank() || (!includeSelf && packageName == context.packageName) ||
                    !seen.add(packageName)) return@mapNotNull null
                val label = info.loadLabel(manager)?.toString()?.trim().orEmpty()
                    .ifEmpty { packageName }
                if (!metadata) return@mapNotNull InstalledApp(packageName, label)
                val version = try {
                    @Suppress("DEPRECATION")
                    manager.getPackageInfo(packageName, 0).versionName
                } catch (_: PackageManager.NameNotFoundException) {
                    null // The app may disappear between the query and metadata lookup.
                } catch (_: SecurityException) {
                    null
                }
                InstalledApp(packageName, label, isProtectedSystemApp(appInfo.flags),
                    version?.takeIf { it.isNotBlank() }, appInfo.targetSdkVersion)
            }.sortedWith(compareBy({ it.label.lowercase(Locale.getDefault()) }, { it.packageName }))
        }
}
