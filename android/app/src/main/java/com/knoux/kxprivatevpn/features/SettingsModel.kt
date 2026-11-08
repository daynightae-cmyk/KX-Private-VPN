package com.knoux.kxprivatevpn.features

/**
 * What Android currently allows for KX notifications.
 *
 * [ALLOWED] means the app may post. [DENIED] means the runtime permission is missing on
 * Android 13+ and can still be requested. [DISABLED] means the permission exists (or the
 * platform has no runtime permission) but the user turned KX notifications off, which only
 * Android settings can change.
 */
enum class SessionNotificationAccess { ALLOWED, DENIED, DISABLED }

/**
 * Pure mapping from platform facts to the honest settings state. The tunnel never depends on
 * this value: a missing permission only hides the ongoing session notification.
 */
fun sessionNotificationAccess(
    sdkInt: Int,
    permissionGranted: Boolean,
    notificationsEnabled: Boolean,
    channelEnabled: Boolean = true,
): SessionNotificationAccess = when {
    sdkInt < 33 -> if (notificationsEnabled && channelEnabled) {
        SessionNotificationAccess.ALLOWED
    } else {
        SessionNotificationAccess.DISABLED
    }
    !permissionGranted -> SessionNotificationAccess.DENIED
    notificationsEnabled && channelEnabled -> SessionNotificationAccess.ALLOWED
    else -> SessionNotificationAccess.DISABLED
}
