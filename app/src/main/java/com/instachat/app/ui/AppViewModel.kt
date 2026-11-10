package com.instachat.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.instachat.app.data.prefs.AppSettings
import com.instachat.app.data.prefs.SettingsStore
import com.instachat.app.data.prefs.ThemeMode
import com.instachat.app.data.repository.DemoSeeder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AppViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
    private val seeder: DemoSeeder
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsStore.settings

    private val _resetting = MutableStateFlow(false)
    val resetting: StateFlow<Boolean> = _resetting.asStateFlow()

    private val eventsChannel = Channel<String>(Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

    fun setThemeMode(mode: ThemeMode) = settingsStore.setThemeMode(mode)

    fun setReadReceipts(enabled: Boolean) = settingsStore.setReadReceipts(enabled)

    fun setDefaultChatTheme(themeId: String) = settingsStore.setDefaultChatTheme(themeId)

    fun resetDemoData() {
        if (_resetting.value) return
        viewModelScope.launch {
            _resetting.value = true
            runCatching { seeder.resetDemoData() }
                .onSuccess { eventsChannel.send("Demo conversations restored") }
                .onFailure { eventsChannel.send("Could not restore demo conversations") }
            _resetting.value = false
        }
    }
}
