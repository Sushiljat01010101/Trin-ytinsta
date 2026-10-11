package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "tiny_crap_settings")

class AppPreferences(private val context: Context) {

    companion object {
        val SERVER_URL = stringPreferencesKey("server_url")
        val API_KEY = stringPreferencesKey("api_key")
        val THEME_MODE = stringPreferencesKey("theme_mode") // "SYSTEM", "DARK", "LIGHT"
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val SPLASH_ANIMATION = booleanPreferencesKey("splash_animation")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val MAX_DOWNLOADS = intPreferencesKey("max_downloads")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val DEFAULT_VIDEO_QUALITY = stringPreferencesKey("default_video_quality")
        val DEFAULT_AUDIO_FORMAT = stringPreferencesKey("default_audio_format")
        val SAVE_LOCATION = stringPreferencesKey("save_location") // "MediaStore" or "AppStorage"
    }

    val serverUrlFlow: Flow<String> = context.dataStore.data.map {
        it[SERVER_URL] ?: "http://10.0.2.2:5000"
    }

    val apiKeyFlow: Flow<String> = context.dataStore.data.map {
        it[API_KEY] ?: ""
    }

    val themeModeFlow: Flow<String> = context.dataStore.data.map {
        it[THEME_MODE] ?: "SYSTEM"
    }

    val dynamicColorFlow: Flow<Boolean> = context.dataStore.data.map {
        it[DYNAMIC_COLOR] ?: true
    }

    val splashAnimationFlow: Flow<Boolean> = context.dataStore.data.map {
        it[SPLASH_ANIMATION] ?: true
    }

    val wifiOnlyFlow: Flow<Boolean> = context.dataStore.data.map {
        it[WIFI_ONLY] ?: false
    }

    val maxDownloadsFlow: Flow<Int> = context.dataStore.data.map {
        it[MAX_DOWNLOADS] ?: 2
    }

    val notificationsEnabledFlow: Flow<Boolean> = context.dataStore.data.map {
        it[NOTIFICATIONS_ENABLED] ?: true
    }

    val defaultVideoQualityFlow: Flow<String> = context.dataStore.data.map {
        it[DEFAULT_VIDEO_QUALITY] ?: "1080p"
    }

    val defaultAudioFormatFlow: Flow<String> = context.dataStore.data.map {
        it[DEFAULT_AUDIO_FORMAT] ?: "MP3 (320 kbps)"
    }

    val saveLocationFlow: Flow<String> = context.dataStore.data.map {
        it[SAVE_LOCATION] ?: "MediaStore"
    }

    suspend fun setServerUrl(url: String) {
        context.dataStore.edit { it[SERVER_URL] = url }
    }

    suspend fun setApiKey(key: String) {
        context.dataStore.edit { it[API_KEY] = key }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[THEME_MODE] = mode }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[DYNAMIC_COLOR] = enabled }
    }

    suspend fun setSplashAnimation(enabled: Boolean) {
        context.dataStore.edit { it[SPLASH_ANIMATION] = enabled }
    }

    suspend fun setWifiOnly(enabled: Boolean) {
        context.dataStore.edit { it[WIFI_ONLY] = enabled }
    }

    suspend fun setMaxDownloads(max: Int) {
        context.dataStore.edit { it[MAX_DOWNLOADS] = max }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setDefaultVideoQuality(quality: String) {
        context.dataStore.edit { it[DEFAULT_VIDEO_QUALITY] = quality }
    }

    suspend fun setDefaultAudioFormat(format: String) {
        context.dataStore.edit { it[DEFAULT_AUDIO_FORMAT] = format }
    }

    suspend fun setSaveLocation(location: String) {
        context.dataStore.edit { it[SAVE_LOCATION] = location }
    }
}
