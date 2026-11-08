package com.knoux.kxprivatevpn

import com.knoux.kxprivatevpn.features.SessionNotificationAccess
import com.knoux.kxprivatevpn.features.sessionNotificationAccess
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The settings screen must report exactly what Android allows. A missing permission is a
 * requestable denial; a switched-off notification channel is a system-settings state. Neither
 * may be reported as "on", and neither blocks the tunnel.
 */
class SettingsModelTest {
    @Test fun legacyPlatformsWithoutRuntimePermissionReportOnWhenNotificationsAreEnabled() {
        assertEquals(SessionNotificationAccess.ALLOWED,
            sessionNotificationAccess(26, permissionGranted = true, notificationsEnabled = true))
        assertEquals(SessionNotificationAccess.ALLOWED,
            sessionNotificationAccess(32, permissionGranted = true, notificationsEnabled = true))
    }

    @Test fun legacyPlatformsReportDisabledWhenTheUserTurnedNotificationsOff() {
        assertEquals(SessionNotificationAccess.DISABLED,
            sessionNotificationAccess(26, permissionGranted = true, notificationsEnabled = false))
        assertEquals(SessionNotificationAccess.DISABLED,
            sessionNotificationAccess(32, permissionGranted = true, notificationsEnabled = false))
    }

    @Test fun android13AndNewerWithoutPermissionReportDenied() {
        assertEquals(SessionNotificationAccess.DENIED,
            sessionNotificationAccess(33, permissionGranted = false, notificationsEnabled = true))
        assertEquals(SessionNotificationAccess.DENIED,
            sessionNotificationAccess(36, permissionGranted = false, notificationsEnabled = false))
    }

    @Test fun android13AndNewerWithPermissionReportOnOnlyWhenNotificationsAreEnabled() {
        assertEquals(SessionNotificationAccess.ALLOWED,
            sessionNotificationAccess(33, permissionGranted = true, notificationsEnabled = true))
        assertEquals(SessionNotificationAccess.DISABLED,
            sessionNotificationAccess(33, permissionGranted = true, notificationsEnabled = false))
        assertEquals(SessionNotificationAccess.DISABLED,
            sessionNotificationAccess(36, permissionGranted = true, notificationsEnabled = false))
    }

    @Test fun aDisabledSessionChannelIsReportedAsOffEvenWhenTheAppCanPost() {
        assertEquals(SessionNotificationAccess.DISABLED,
            sessionNotificationAccess(26, permissionGranted = true, notificationsEnabled = true,
                channelEnabled = false))
        assertEquals(SessionNotificationAccess.DISABLED,
            sessionNotificationAccess(33, permissionGranted = true, notificationsEnabled = true,
                channelEnabled = false))
        assertEquals(SessionNotificationAccess.ALLOWED,
            sessionNotificationAccess(33, permissionGranted = true, notificationsEnabled = true,
                channelEnabled = true))
    }
}
