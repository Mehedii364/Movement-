package com.example.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "movement_preferences")

class AppPreferences(private val context: Context) {

    companion object {
        val KEY_LANGUAGE = stringPreferencesKey("app_language")
        val KEY_THEME = stringPreferencesKey("app_theme")
        val KEY_TIME_FORMAT = stringPreferencesKey("app_time_format") // 12H, 24H
        val KEY_DATE_FORMAT = stringPreferencesKey("app_date_format")
        val KEY_DEFAULT_PERSON_STATUS = stringPreferencesKey("default_person_status")
        val KEY_LOGGED_IN_USER = stringPreferencesKey("logged_in_username")
        val KEY_LOGGED_IN_ROLE = stringPreferencesKey("logged_in_role")
        val KEY_LOGGED_IN_NAME = stringPreferencesKey("logged_in_name")
        val KEY_BACKEND_URL = stringPreferencesKey("backend_url")
        val KEY_API_KEY = stringPreferencesKey("api_key")
        val KEY_CUSTOM_PURPOSES = stringPreferencesKey("custom_purposes")
        val KEY_CUSTOM_DESTINATIONS = stringPreferencesKey("custom_destinations")
        val KEY_LAST_SYNC_TIME = stringPreferencesKey("last_sync_time")
    }

    val languageFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_LANGUAGE] ?: "en"
    }

    val themeFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_THEME] ?: "SYSTEM"
    }

    val timeFormatFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_TIME_FORMAT] ?: "12H"
    }

    val currentUserFlow: Flow<UserSession> = context.dataStore.data.map { prefs ->
        UserSession(
            username = prefs[KEY_LOGGED_IN_USER] ?: "admin",
            role = prefs[KEY_LOGGED_IN_ROLE] ?: "ADMIN",
            fullName = prefs[KEY_LOGGED_IN_NAME] ?: "Security Supervisor"
        )
    }

    val backendUrlFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_BACKEND_URL] ?: "https://example.com/api"
    }

    val customPurposesFlow: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_CUSTOM_PURPOSES]
        if (!raw.isNullOrBlank()) {
            raw.split(";;").filter { it.isNotBlank() }
        } else {
            emptyList()
        }
    }

    val customDestinationsFlow: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_CUSTOM_DESTINATIONS]
        if (!raw.isNullOrBlank()) {
            raw.split(";;").filter { it.isNotBlank() }
        } else {
            emptyList()
        }
    }

    val lastSyncTimeFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_SYNC_TIME] ?: "Never"
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { it[KEY_LANGUAGE] = lang }
    }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { it[KEY_THEME] = theme }
    }

    suspend fun setTimeFormat(format: String) {
        context.dataStore.edit { it[KEY_TIME_FORMAT] = format }
    }

    suspend fun setUserSession(username: String, role: String, fullName: String) {
        context.dataStore.edit {
            it[KEY_LOGGED_IN_USER] = username
            it[KEY_LOGGED_IN_ROLE] = role
            it[KEY_LOGGED_IN_NAME] = fullName
        }
    }

    suspend fun setBackendUrl(url: String) {
        context.dataStore.edit { it[KEY_BACKEND_URL] = url }
    }

    suspend fun addCustomPurpose(purpose: String) {
        context.dataStore.edit { prefs ->
            val existing = prefs[KEY_CUSTOM_PURPOSES]?.split(";;")?.toMutableList() ?: mutableListOf()
            if (!existing.contains(purpose.trim())) {
                existing.add(purpose.trim())
                prefs[KEY_CUSTOM_PURPOSES] = existing.joinToString(";;")
            }
        }
    }

    suspend fun addCustomDestination(destination: String) {
        context.dataStore.edit { prefs ->
            val existing = prefs[KEY_CUSTOM_DESTINATIONS]?.split(";;")?.toMutableList() ?: mutableListOf()
            if (!existing.contains(destination.trim())) {
                existing.add(destination.trim())
                prefs[KEY_CUSTOM_DESTINATIONS] = existing.joinToString(";;")
            }
        }
    }

    suspend fun updateLastSync(timeString: String) {
        context.dataStore.edit { it[KEY_LAST_SYNC_TIME] = timeString }
    }
}

data class UserSession(
    val username: String,
    val role: String, // ADMIN, OPERATOR, VIEWER
    val fullName: String
)
