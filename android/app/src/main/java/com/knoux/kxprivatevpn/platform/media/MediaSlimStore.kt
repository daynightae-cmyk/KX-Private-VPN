package com.knoux.kxprivatevpn.platform.media

import android.content.Context

/**
 * App-private memory of the one media tree the user granted. Kept separate from the Storage
 * X-ray tree so picking a photo folder never changes another tool's scope. No path or file
 * content is stored.
 */
class MediaSlimStore(context: Context) {
    private val preferences = context.getSharedPreferences("media_slim", Context.MODE_PRIVATE)

    var treeUri: String?
        get() = preferences.getString("tree_uri", null)
        set(value) {
            preferences.edit().putString("tree_uri", value).apply()
        }

    var treeName: String?
        get() = preferences.getString("tree_name", null)
        set(value) {
            preferences.edit().putString("tree_name", value).apply()
        }

    fun clear() {
        preferences.edit().clear().apply()
    }
}
