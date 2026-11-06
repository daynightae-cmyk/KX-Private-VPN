package com.knoux.kxprivatevpn

import android.content.pm.ApplicationInfo
import com.knoux.kxprivatevpn.features.isProtectedSystemApp
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
    }
}
