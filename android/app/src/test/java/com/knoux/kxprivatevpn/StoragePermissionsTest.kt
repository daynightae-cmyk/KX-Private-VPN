package com.knoux.kxprivatevpn

import android.Manifest
import com.knoux.kxprivatevpn.platform.storage.StorageCategory
import com.knoux.kxprivatevpn.platform.storage.StoragePermissions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The permission matrix must match Android's actual media permission split. */
class StoragePermissionsTest {
    @Test fun android13AndLaterUsesGranularMediaPermissions() {
        assertEquals(
            listOf(Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO),
            StoragePermissions.required(33),
        )
        assertEquals(
            listOf(Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED),
            StoragePermissions.required(34),
        )
        assertEquals(StoragePermissions.required(36), StoragePermissions.required(34))
        assertEquals(Manifest.permission.READ_MEDIA_IMAGES,
            StoragePermissions.forCategory(StorageCategory.IMAGES, 34))
        assertEquals(Manifest.permission.READ_MEDIA_AUDIO,
            StoragePermissions.forCategory(StorageCategory.AUDIO, 34))
        assertNull(StoragePermissions.forCategory(StorageCategory.DOCUMENTS, 34))
    }

    @Test fun legacyVersionsUseOneReadPermission() {
        assertEquals(listOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            StoragePermissions.required(32))
        assertEquals(Manifest.permission.READ_EXTERNAL_STORAGE,
            StoragePermissions.forCategory(StorageCategory.IMAGES, 26))
        assertEquals(Manifest.permission.READ_EXTERNAL_STORAGE,
            StoragePermissions.forCategory(StorageCategory.DOCUMENTS, 30))
    }

    @Test fun documentsAreOnlyQueriedOnAndroid10To12() {
        assertFalse(StoragePermissions.documentsAvailable(26))
        assertFalse(StoragePermissions.documentsAvailable(28))
        assertTrue(StoragePermissions.documentsAvailable(29))
        assertTrue(StoragePermissions.documentsAvailable(32))
        assertFalse(StoragePermissions.documentsAvailable(33))
    }
}
