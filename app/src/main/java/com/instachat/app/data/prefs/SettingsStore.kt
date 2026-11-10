package com.instachat.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.instachat.app.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore("settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val readReceipts: Boolean = true,
    val defaultChatThemeId: String = "aurora"
)

@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope
) {
    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val readReceipts = booleanPreferencesKey("read_receipts")
        val defaultChatThemeId = stringPreferencesKey("default_chat_theme_id")
    }

    val settings: StateFlow<AppSettings> = context.settingsDataStore.data
        .map { prefs ->
            AppSettings(
                themeMode = prefs[Keys.themeMode]
                    ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                    ?: ThemeMode.DARK,
                readReceipts = prefs[Keys.readReceipts] ?: true,
                defaultChatThemeId = prefs[Keys.defaultChatThemeId] ?: "aurora"
            )
        }
        .stateIn(scope, SharingStarted.Eagerly, AppSettings())

    fun setThemeMode(mode: ThemeMode) {
        scope.launch {
            context.settingsDataStore.edit { it[Keys.themeMode] = mode.name }
        }
    }

    fun setReadReceipts(enabled: Boolean) {
        scope.launch {
            context.settingsDataStore.edit { it[Keys.readReceipts] = enabled }
        }
    }

    fun setDefaultChatTheme(themeId: String) {
        scope.launch {
            context.settingsDataStore.edit { it[Keys.defaultChatThemeId] = themeId }
        }
    }
}
