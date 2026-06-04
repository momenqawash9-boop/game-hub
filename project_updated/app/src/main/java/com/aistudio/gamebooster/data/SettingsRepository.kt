package com.aistudio.gamebooster.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "game_booster_prefs")

class SettingsRepository(private val context: Context) {

    companion object {
        val PERFORMANCE_MODE = booleanPreferencesKey("performance_mode")
        val DND_MODE = booleanPreferencesKey("dnd_mode")
        val BRIGHTNESS_LOCK = booleanPreferencesKey("brightness_lock")
        val BRIGHTNESS_LEVEL = floatPreferencesKey("brightness_level")
        val FLOATING_WIDGET = booleanPreferencesKey("floating_widget")
        val USER_GAMES = stringSetPreferencesKey("user_games")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_AVATAR = stringPreferencesKey("user_avatar")
        val GAME_PLAYTIMES = stringPreferencesKey("game_playtimes")
    }

    val userNameFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[USER_NAME] ?: "Challenger" }

    val userAvatarFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[USER_AVATAR] ?: "preset_1" }

    val gamePlaytimesFlow: Flow<Map<String, Long>> = context.dataStore.data
        .map { preferences -> 
            val raw = preferences[GAME_PLAYTIMES]
            parsePlaytime(raw)
        }

    private fun parsePlaytime(raw: String?): Map<String, Long> {
        if (raw.isNullOrBlank()) {
            return emptyMap()
        }
        return try {
            raw.split(",").filter { it.contains(":") }.associate {
                val parts = it.split(":", limit = 2)
                parts[0] to (parts[1].toLongOrNull() ?: 0L)
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun formatPlaytime(map: Map<String, Long>): String {
        return map.map { "${it.key}:${it.value}" }.joinToString(",")
    }

    suspend fun setUserName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_NAME] = name
        }
    }

    suspend fun setUserAvatar(avatar: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_AVATAR] = avatar
        }
    }

    suspend fun addPlaytime(packageName: String, seconds: Long) {
        context.dataStore.edit { preferences ->
            val raw = preferences[GAME_PLAYTIMES]
            val currentPlaytimes = parsePlaytime(raw).toMutableMap()
            val currentVal = currentPlaytimes[packageName] ?: 0L
            currentPlaytimes[packageName] = currentVal + seconds
            preferences[GAME_PLAYTIMES] = formatPlaytime(currentPlaytimes)
        }
    }

    val performanceModeFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[PERFORMANCE_MODE] ?: true }

    val dndModeFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[DND_MODE] ?: false }

    val brightnessLockFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[BRIGHTNESS_LOCK] ?: false }

    val brightnessLevelFlow: Flow<Float> = context.dataStore.data
        .map { preferences -> preferences[BRIGHTNESS_LEVEL] ?: 0.8f }

    val floatingWidgetFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[FLOATING_WIDGET] ?: true }

    val userGamesFlow: Flow<Set<String>> = context.dataStore.data
        .map { preferences -> preferences[USER_GAMES] ?: emptySet() }

    suspend fun setPerformanceMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PERFORMANCE_MODE] = enabled
        }
    }

    suspend fun setDndMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DND_MODE] = enabled
        }
    }

    suspend fun setBrightnessLock(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BRIGHTNESS_LOCK] = enabled
        }
    }

    suspend fun setBrightnessLevel(level: Float) {
        context.dataStore.edit { preferences ->
            preferences[BRIGHTNESS_LEVEL] = level
        }
    }

    suspend fun setFloatingWidget(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[FLOATING_WIDGET] = enabled
        }
    }

    suspend fun addGame(packageName: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[USER_GAMES] ?: emptySet()
            preferences[USER_GAMES] = current + packageName
        }
    }

    suspend fun removeGame(packageName: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[USER_GAMES] ?: emptySet()
            preferences[USER_GAMES] = current - packageName
        }
    }
}
