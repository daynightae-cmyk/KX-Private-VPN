package com.knoux.kxprivatevpn.platform.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class InstalledApp(val packageName: String, val label: String)

/**
 * The launchable apps Android actually exposes to this app. Android 11+ only reveals
 * packages matched by the `<queries>` launcher intent in the manifest. Work-profile apps
 * live in another user and are not visible here; the UI states that limitation.
 */
class InstalledApps(private val context: Context) {
    suspend fun list(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val manager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            manager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            manager.queryIntentActivities(intent, 0)
        }
        resolved.asSequence()
            .mapNotNull { info ->
                val packageName = info.activityInfo?.applicationInfo?.packageName
                    ?: return@mapNotNull null
                if (packageName == context.packageName) return@mapNotNull null
                val label = info.loadLabel(manager)?.toString()?.trim().orEmpty()
                InstalledApp(packageName, label.ifEmpty { packageName })
            }
            .distinctBy { it.packageName }
            .sortedWith(compareBy({ it.label.lowercase(Locale.getDefault()) }, { it.packageName }))
            .toList()
    }
}
