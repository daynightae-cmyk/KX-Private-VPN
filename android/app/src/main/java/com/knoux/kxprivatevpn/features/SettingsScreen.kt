package com.knoux.kxprivatevpn.features

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.knoux.kxprivatevpn.R
import com.knoux.kxprivatevpn.designsystem.Atmosphere
import com.knoux.kxprivatevpn.designsystem.GlassSurface
import com.knoux.kxprivatevpn.designsystem.KxPageHeader
import com.knoux.kxprivatevpn.launch.UiPreferences
import com.knoux.kxprivatevpn.platform.vpn.VpnSessionService

/**
 * Settings is a live report of what this device really holds: the installed build, the saved
 * profiles, the selected language/theme, and the exact notification access Android grants.
 * Nothing here claims an account or a Pro state that the app does not own.
 */
@Composable
fun SettingsRoute(
    night: Boolean,
    settings: UiPreferences,
    profileCount: Int,
    busy: Boolean,
    onBack: () -> Unit,
    onLanguage: (String) -> Unit,
    onTheme: (String) -> Unit,
    onOpenPro: () -> Unit,
    onReset: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var notificationAccess by remember { mutableStateOf(readNotificationAccess(context)) }
    val permission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { notificationAccess = readNotificationAccess(context) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationAccess = readNotificationAccess(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val packageInfo = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull()
    }
    SettingsScreen(
        night = night,
        settings = settings,
        profileCount = profileCount,
        versionName = packageInfo?.versionName ?: "—",
        versionCode = packageInfo?.let(PackageInfoCompat::getLongVersionCode) ?: 0L,
        notificationAccess = notificationAccess,
        busy = busy,
        onBack = onBack,
        onLanguage = onLanguage,
        onTheme = onTheme,
        onOpenPro = onOpenPro,
        onRequestNotifications = {
            if (Build.VERSION.SDK_INT >= 33) {
                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                openNotificationSettings(context)
            }
        },
        onOpenNotificationSettings = { openNotificationSettings(context) },
        onReset = onReset,
    )
}

/** The stateless screen. Every value comes from parameters; every action is a callback. */
@Composable
fun SettingsScreen(
    night: Boolean,
    settings: UiPreferences,
    profileCount: Int,
    versionName: String,
    versionCode: Long,
    notificationAccess: SessionNotificationAccess,
    busy: Boolean,
    onBack: () -> Unit,
    onLanguage: (String) -> Unit,
    onTheme: (String) -> Unit,
    onOpenPro: () -> Unit,
    onRequestNotifications: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onReset: () -> Unit,
) {
    val foreground = if (night) Color(0xFFF7F3FF) else Color(0xFF171050)
    val muted = foreground.copy(alpha = .80f)
    var confirmingReset by rememberSaveable { mutableStateOf(false) }
    // A dialog owns a separate Android window/context. Resolve its copy in the selected
    // locale here; otherwise the portal falls back to the Activity's previous resources.
    val resetTitle = stringResource(R.string.settings_reset_confirm_title)
    val resetBody = stringResource(R.string.settings_reset_confirm_body)
    val resetConfirm = stringResource(R.string.settings_reset_confirm)
    val cancelLabel = stringResource(R.string.cancel)
    val actionColors = ButtonDefaults.outlinedButtonColors(
        contentColor = if (night) Color(0xFFEDE7FF) else Color(0xFF3F3390),
    )

    Box(Modifier.fillMaxSize().testTag("route-settings")) {
        Atmosphere(night, 0f)
        Column(
            Modifier.fillMaxSize().safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            KxPageHeader(
                title = stringResource(R.string.settings),
                night = night,
                backLabel = stringResource(R.string.back),
                onBack = onBack,
            )

            GlassSurface(night) {
                Text(stringResource(R.string.settings_device_title), fontSize = 19.sp,
                    fontWeight = FontWeight.Bold, color = foreground)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.settings_version, versionName, versionCode),
                    fontSize = 12.5.sp, color = muted,
                    modifier = Modifier.testTag("settings-version"))
                Text(pluralStringResource(R.plurals.settings_profiles_count,
                    profileCount, profileCount), fontSize = 13.sp, color = foreground,
                    modifier = Modifier.testTag("settings-profile-count"))
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.settings_account_body), fontSize = 13.sp,
                    lineHeight = 19.sp, color = muted,
                    modifier = Modifier.testTag("pref-account"))
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onOpenPro,
                    enabled = !busy,
                    colors = actionColors,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("open-pro"),
                ) { Text(stringResource(R.string.pro_title)) }
            }

            GlassSurface(night) {
                SectionHeading(stringResource(R.string.language), foreground)
                Text(stringResource(R.string.settings_language_body), fontSize = 12.5.sp,
                    lineHeight = 18.sp, color = muted)
                Spacer(Modifier.height(6.dp))
                ChoiceRow(
                    label = stringResource(R.string.system_default),
                    selected = settings.language == "system",
                    enabled = !busy,
                    tag = "pref-language-system",
                    night = night,
                    onClick = { onLanguage("system") },
                )
                ChoiceRow(
                    label = "English",
                    selected = settings.language == "en",
                    enabled = !busy,
                    tag = "pref-language-en",
                    night = night,
                    onClick = { onLanguage("en") },
                )
                ChoiceRow(
                    label = "العربية",
                    selected = settings.language == "ar",
                    enabled = !busy,
                    tag = "pref-language-ar",
                    night = night,
                    onClick = { onLanguage("ar") },
                )
            }

            GlassSurface(night) {
                SectionHeading(stringResource(R.string.appearance), foreground)
                Text(stringResource(R.string.settings_appearance_body), fontSize = 12.5.sp,
                    lineHeight = 18.sp, color = muted)
                Spacer(Modifier.height(6.dp))
                ChoiceRow(
                    label = stringResource(R.string.system_default),
                    selected = settings.theme == "system",
                    enabled = !busy,
                    tag = "pref-theme-system",
                    night = night,
                    onClick = { onTheme("system") },
                )
                ChoiceRow(
                    label = stringResource(R.string.night),
                    selected = settings.theme == "night",
                    enabled = !busy,
                    tag = "pref-theme-night",
                    night = night,
                    onClick = { onTheme("night") },
                )
                ChoiceRow(
                    label = stringResource(R.string.day),
                    selected = settings.theme == "day",
                    enabled = !busy,
                    tag = "pref-theme-day",
                    night = night,
                    onClick = { onTheme("day") },
                )
            }

            GlassSurface(night, Modifier.testTag("settings-notifications-card")) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    SectionHeading(stringResource(R.string.settings_notifications_title),
                        foreground, Modifier.weight(1f))
                    NotificationStatusPill(notificationAccess, night)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(when (notificationAccess) {
                        SessionNotificationAccess.ALLOWED -> R.string.settings_notifications_allowed_body
                        SessionNotificationAccess.DENIED -> R.string.settings_notifications_denied_body
                        SessionNotificationAccess.DISABLED -> R.string.settings_notifications_disabled_body
                    }),
                    fontSize = 13.sp, lineHeight = 19.sp, color = muted,
                    modifier = Modifier.testTag("pref-notifications"),
                )
                Spacer(Modifier.height(10.dp))
                if (notificationAccess == SessionNotificationAccess.DENIED) {
                    Button(
                        onClick = onRequestNotifications,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .testTag("pref-notifications-allow"),
                    ) { Text(stringResource(R.string.settings_notifications_allow)) }
                    Spacer(Modifier.height(6.dp))
                }
                OutlinedButton(
                    onClick = onOpenNotificationSettings,
                    enabled = !busy,
                    colors = actionColors,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("pref-notifications-settings"),
                ) { Text(stringResource(R.string.settings_notifications_open)) }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.settings_notifications_scope), fontSize = 11.5.sp,
                    lineHeight = 17.sp, color = foreground.copy(alpha = .72f))
            }

            GlassSurface(night) {
                SectionHeading(stringResource(R.string.settings_privacy_title), foreground)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.pref_privacy), fontSize = 12.5.sp,
                    lineHeight = 18.sp, color = muted,
                    modifier = Modifier.testTag("pref-privacy"))
            }

            GlassSurface(night) {
                SectionHeading(stringResource(R.string.settings_reset_title), foreground)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.pref_reset_hint), fontSize = 12.5.sp,
                    lineHeight = 18.sp, color = muted)
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { confirmingReset = true },
                    enabled = !busy,
                    colors = actionColors,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("pref-reset"),
                ) { Text(stringResource(R.string.pref_reset)) }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmingReset) {
        AlertDialog(
            onDismissRequest = { confirmingReset = false },
            title = { Text(resetTitle) },
            text = { Text(resetBody) },
            confirmButton = {
                TextButton(
                    onClick = { confirmingReset = false; onReset() },
                    modifier = Modifier.testTag("pref-reset-confirm"),
                ) { Text(resetConfirm) }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmingReset = false },
                    modifier = Modifier.testTag("pref-reset-cancel"),
                ) { Text(cancelLabel) }
            },
        )
    }
}

@Composable
private fun SectionHeading(
    text: String,
    foreground: Color,
    modifier: Modifier = Modifier,
) {
    Text(text, modifier, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = foreground)
}

@Composable
private fun ChoiceRow(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    tag: String,
    night: Boolean,
    onClick: () -> Unit,
) {
    val foreground = if (night) Color(0xFFF7F3FF) else Color(0xFF171050)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Text(label, fontSize = 16.sp, color = foreground)
    }
}

@Composable
private fun NotificationStatusPill(access: SessionNotificationAccess, night: Boolean) {
    val label = stringResource(when (access) {
        SessionNotificationAccess.ALLOWED -> R.string.settings_notifications_allowed
        SessionNotificationAccess.DENIED -> R.string.settings_notifications_denied
        SessionNotificationAccess.DISABLED -> R.string.settings_notifications_disabled
    })
    val accent = when (access) {
        SessionNotificationAccess.ALLOWED ->
            if (night) Color(0xFF64E9C0) else Color(0xFF0B7A5A)
        SessionNotificationAccess.DENIED ->
            if (night) Color(0xFFFF9E9E) else Color(0xFFB3261E)
        SessionNotificationAccess.DISABLED ->
            if (night) Color(0xFFFFC46B) else Color(0xFF8A5300)
    }
    Row(
        Modifier.clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = if (night) .10f else .40f))
            .border(1.dp, accent.copy(alpha = .55f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(7.dp).background(accent, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = accent)
    }
}

private fun readNotificationAccess(context: Context): SessionNotificationAccess {
    val granted = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED
    val enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
    val channel = context.getSystemService(NotificationManager::class.java)
        .getNotificationChannel(VpnSessionService.CHANNEL_ID)
    // A missing channel means the service has not run yet; Android will create it enabled.
    val channelEnabled = channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE
    return sessionNotificationAccess(
        Build.VERSION.SDK_INT,
        granted,
        enabled,
        channelEnabled,
    )
}

private fun openNotificationSettings(context: Context) {
    val direct = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    runCatching { context.startActivity(direct) }.onFailure {
        val fallback = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null),
        )
        runCatching { context.startActivity(fallback) }
    }
}
