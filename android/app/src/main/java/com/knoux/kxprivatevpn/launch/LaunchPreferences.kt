package com.knoux.kxprivatevpn.launch

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.launchDataStore by preferencesDataStore(name = "launch")

class LaunchPreferences(private val context: Context) {
    private val seen = booleanPreferencesKey("identity_intro_seen")
    suspend fun hasSeenIntro(): Boolean = context.launchDataStore.data.first()[seen] ?: false
    suspend fun markSeen() { context.launchDataStore.edit { it[seen] = true } }
}
