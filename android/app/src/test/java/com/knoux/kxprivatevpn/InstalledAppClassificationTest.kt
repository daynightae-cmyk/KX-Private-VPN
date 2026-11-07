package com.knoux.kxprivatevpn

import android.content.pm.ApplicationInfo
import com.knoux.kxprivatevpn.platform.apps.InstalledApp
import com.knoux.kxprivatevpn.platform.apps.canRequestUninstall
import com.knoux.kxprivatevpn.platform.apps.isProtectedSystemApp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InstalledAppClassificationTest {
    @Test fun systemPackagesNeverExposeInlineUninstall() {
        assertTrue(isProtectedSystemApp(ApplicationInfo.FLAG_SYSTEM))
        assertTrue(isProtectedSystemApp(ApplicationInfo.FLAG_UPDATED_SYSTEM_APP))
        assertTrue(isProtectedSystemApp(
            ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP))
    }

    @Test fun ordinaryUserPackageMayRequestSystemConfirmation() {
        assertFalse(isProtectedSystemApp(0))
        assertTrue(canRequestUninstall(
            InstalledApp("org.example.user", "User app"), "com.knoux.kxprivatevpn"))
    }

    @Test fun ownPackageCannotRequestUninstallEvenWithoutSystemFlags() {
        val ownPackage = "com.knoux.kxprivatevpn"
        assertFalse(canRequestUninstall(InstalledApp(ownPackage, "KX Private VPN"), ownPackage))
    }

    @Test fun everyProtectedSystemFlagCombinationBlocksUninstall() {
        for (flags in listOf(
            ApplicationInfo.FLAG_SYSTEM,
            ApplicationInfo.FLAG_UPDATED_SYSTEM_APP,
            ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP,
            ApplicationInfo.FLAG_UPDATED_SYSTEM_APP or ApplicationInfo.FLAG_DEBUGGABLE,
        )) {
            val app = InstalledApp("org.example.system", "System app", system = isProtectedSystemApp(flags))
            assertFalse(canRequestUninstall(app, "com.knoux.kxprivatevpn"))
        }
    }

    @Test fun unrelatedFlagsDoNotBlockAnOrdinaryUserPackage() {
        val flags = ApplicationInfo.FLAG_DEBUGGABLE or ApplicationInfo.FLAG_HAS_CODE
        assertFalse(isProtectedSystemApp(flags))
        val app = InstalledApp("org.example.user", "User app", system = isProtectedSystemApp(flags))
        assertTrue(canRequestUninstall(app, "com.knoux.kxprivatevpn"))
    }

    @Test fun blankAndPaddedPackageNamesCannotRequestUninstall() {
        for (packageName in listOf("", " ", "\t\n", " org.example.user", "org.example.user ", "\torg.example.user\n")) {
            assertFalse(canRequestUninstall(InstalledApp(packageName, "Untrusted label"), "com.knoux.kxprivatevpn"))
        }
    }
}
