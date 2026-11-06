package com.knoux.kxprivatevpn.platform.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.knoux.kxprivatevpn.MainActivity
import com.knoux.kxprivatevpn.R

/**
 * Ongoing session surface and foreground owner for the app process while the WireGuard
 * backend owns an active tunnel. The official tunnel library's VpnService declares the
 * VPN binding but never calls startForeground; without this companion the process can be
 * treated as a background service and the tunnel can be reclaimed.
 *
 * The notification is posted only after the controller reports CONNECTED, and it is
 * removed as soon as the controller leaves CONNECTED. It shows a real session timer and
 * the selected profile name; it never claims that traffic is protected or that a
 * handshake occurred.
 */
class VpnSessionService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.vpn_notification_channel),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.vpn_notification_channel_description)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            },
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        val startedAt = intent?.getLongExtra(EXTRA_STARTED_AT, 0L)
            ?.takeIf { it > 0L } ?: System.currentTimeMillis()
        val profile = intent?.getStringExtra(EXTRA_PROFILE_NAME)?.takeIf { it.isNotBlank() }
        val notification = notification(profile, startedAt)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: RuntimeException) {
            // A rejected foreground start must never take the measured tunnel down with it.
            stopSelf()
        }
        // A restarted notification without a restored tunnel would be a fake status.
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun notification(profile: String?, startedAt: Long): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP,
                )
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_kx_notification)
            .setContentTitle(getString(R.string.vpn_notification_title))
            .setContentText(profile ?: getString(R.string.vpn_notification_no_profile))
            .setContentIntent(open)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .setWhen(startedAt)
            .setUsesChronometer(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 0x4B58
        const val CHANNEL_ID = "kx_vpn_session"
        const val ACTION_START = "com.knoux.kxprivatevpn.action.VPN_SESSION_START"
        const val ACTION_STOP = "com.knoux.kxprivatevpn.action.VPN_SESSION_STOP"
        const val EXTRA_PROFILE_NAME = "profile_name"
        const val EXTRA_STARTED_AT = "started_at"

        /** Mirror a measured CONNECTED transition; never call before the backend reports UP. */
        fun start(context: Context, profileName: String?, startedAtMillis: Long?) {
            val intent = Intent(context, VpnSessionService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_PROFILE_NAME, profileName)
                .putExtra(EXTRA_STARTED_AT, startedAtMillis ?: System.currentTimeMillis())
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (_: RuntimeException) {
                // The tunnel remains the source of truth even if the notification is refused.
            }
        }

        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, VpnSessionService::class.java))
            } catch (_: RuntimeException) {
                // Stopping an absent service is not an error.
            }
        }
    }
}
