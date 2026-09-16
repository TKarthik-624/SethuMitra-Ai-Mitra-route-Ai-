package com.mitraroute.ai.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "setumitra_prefs")

class PrefsManager(private val context: Context) {

    companion object {
        private val KEY_TOKEN = stringPreferencesKey("auth_token")
        private val KEY_USER_ID = intPreferencesKey("user_id")
        private val KEY_ROLE = stringPreferencesKey("user_role")
        private val KEY_USERNAME = stringPreferencesKey("username")
        private val KEY_LANGUAGE = stringPreferencesKey("language")
        private val KEY_UNITS = stringPreferencesKey("units")
        private val KEY_NOTIFICATIONS = booleanPreferencesKey("notifications")
        private val KEY_VOICE = booleanPreferencesKey("voice")
    }

    suspend fun saveAuth(token: String, userId: Int, role: String, username: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TOKEN] = token
            prefs[KEY_USER_ID] = userId
            prefs[KEY_ROLE] = role
            prefs[KEY_USERNAME] = username
        }
    }

    fun getToken(): String? = runBlocking {
        context.dataStore.data.map { it[KEY_TOKEN] }.first()
    }

    fun getUserId(): Int = runBlocking {
        context.dataStore.data.map { it[KEY_USER_ID] ?: 0 }.first()
    }

    fun getRole(): String? = runBlocking {
        context.dataStore.data.map { it[KEY_ROLE] }.first()
    }

    fun getUsername(): String? = runBlocking {
        context.dataStore.data.map { it[KEY_USERNAME] }.first()
    }

    fun getLanguage(): String = runBlocking {
        context.dataStore.data.map { it[KEY_LANGUAGE] ?: "en" }.first()
    }

    fun getLanguageFlow(): Flow<String> {
        return context.dataStore.data.map { it[KEY_LANGUAGE] ?: "en" }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { it[KEY_LANGUAGE] = lang }
    }

    fun getUnits(): String = runBlocking {
        context.dataStore.data.map { it[KEY_UNITS] ?: "km" }.first()
    }

    suspend fun setUnits(units: String) {
        context.dataStore.edit { it[KEY_UNITS] = units }
    }

    fun getNotifications(): Boolean = runBlocking {
        context.dataStore.data.map { it[KEY_NOTIFICATIONS] ?: true }.first()
    }

    suspend fun setNotifications(enabled: Boolean) {
        context.dataStore.edit { it[KEY_NOTIFICATIONS] = enabled }
    }

    fun getVoice(): Boolean = runBlocking {
        context.dataStore.data.map { it[KEY_VOICE] ?: true }.first()
    }

    suspend fun setVoice(enabled: Boolean) {
        context.dataStore.edit { it[KEY_VOICE] = enabled }
    }

    fun clear() = runBlocking {
        context.dataStore.edit { it.clear() }
    }
}
