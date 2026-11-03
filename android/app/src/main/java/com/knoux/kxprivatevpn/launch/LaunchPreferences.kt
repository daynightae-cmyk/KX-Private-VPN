package com.knoux.kxprivatevpn.launch

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.launchDataStore by preferencesDataStore(name = "launch")

class LaunchPreferences(private val context: Context) {
    private val seen = booleanPreferencesKey("identity_intro_seen")
    private val onboarded = booleanPreferencesKey("onboarded")
    private val language = stringPreferencesKey("language")
    private val theme = stringPreferencesKey("theme")
    suspend fun hasSeenIntro(): Boolean = context.launchDataStore.data.first()[seen] ?: false
    suspend fun markSeen() { context.launchDataStore.edit { it[seen] = true } }
    suspend fun settings(): UiPreferences {
        val p = context.launchDataStore.data.first()
        return UiPreferences(p[onboarded] ?: false, p[language] ?: "system", p[theme] ?: "system")
    }
    suspend fun completeOnboarding() { context.launchDataStore.edit { it[onboarded] = true } }
    suspend fun appearance(languageValue: String, themeValue: String) {
        require(languageValue in listOf("system", "ar", "en") && themeValue in listOf("system", "night", "day"))
        context.launchDataStore.edit { it[language] = languageValue; it[theme] = themeValue }
    }
}

data class UiPreferences(val onboarded: Boolean = false, val language: String = "system", val theme: String = "system")
