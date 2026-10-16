package com.tasklaunch.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "task_launch")

data class LaunchSettings(
    val enabled: Boolean = false,
    val targetPackage: String = "",
    val delaySeconds: Int = 30,
)

class Preferences(private val context: Context) {
    val settings: Flow<LaunchSettings> = context.dataStore.data.map { prefs ->
        LaunchSettings(
            enabled = prefs[Keys.ENABLED] ?: false,
            targetPackage = prefs[Keys.TARGET_PACKAGE] ?: "",
            delaySeconds = (prefs[Keys.DELAY_SECONDS] ?: DEFAULT_DELAY).coerceIn(MIN_DELAY, MAX_DELAY),
        )
    }

    suspend fun current(): LaunchSettings = settings.first()

    suspend fun setEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ENABLED] = enabled }
    }

    suspend fun setTargetPackage(packageName: String) {
        context.dataStore.edit { it[Keys.TARGET_PACKAGE] = packageName }
    }

    suspend fun setDelaySeconds(seconds: Int) {
        context.dataStore.edit { it[Keys.DELAY_SECONDS] = seconds.coerceIn(MIN_DELAY, MAX_DELAY) }
    }

    private object Keys {
        val ENABLED = booleanPreferencesKey("enabled")
        val TARGET_PACKAGE = stringPreferencesKey("target_package")
        val DELAY_SECONDS = intPreferencesKey("delay_seconds")
    }

    companion object {
        const val MIN_DELAY = 5
        const val MAX_DELAY = 3600
        const val DEFAULT_DELAY = 30
    }
}
