package com.instachat.app.ui.contact

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.instachat.app.data.local.entity.ContactEntity
import com.instachat.app.data.media.MediaStorage
import com.instachat.app.data.media.PendingCapture
import com.instachat.app.data.prefs.SettingsStore
import com.instachat.app.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ContactEditorUiState(
    val contactId: Long = 0L,
    val isNew: Boolean = true,
    val isLoading: Boolean = false,
    val saving: Boolean = false,
    val name: String = "",
    val username: String = "",
    val bio: String = "",
    val avatarPath: String? = null,
    val themeId: String = "aurora",
    val themeName: String = "Default",
    val activeNow: Boolean = true,
    val importingAvatar: Boolean = false,
    val nameError: String? = null,
    val usernameError: String? = null
)

sealed interface ContactEditorEvent {
    data class Saved(val contactId: Long, val isNew: Boolean) : ContactEditorEvent
    data object Deleted : ContactEditorEvent
    data class Message(val text: String) : ContactEditorEvent
}

@HiltViewModel
class ContactEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ChatRepository,
    private val mediaStorage: MediaStorage,
    private val settingsStore: SettingsStore
) : ViewModel() {

    private val contactId: Long = savedStateHandle["contactId"] ?: -1L
    private val isNew = contactId <= 0

    private val _state = MutableStateFlow(
        ContactEditorUiState(
            contactId = if (isNew) 0L else contactId,
            isNew = isNew,
            isLoading = !isNew
        )
    )
    val state: StateFlow<ContactEditorUiState> = _state.asStateFlow()

    private val eventsChannel = Channel<ContactEditorEvent>(Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

    private var originalAvatarPath: String? = null
    private var currentAvatarIsTemporary = false
    private var savedOrDeleted = false

    init {
        if (isNew) {
            viewModelScope.launch {
                val preferred = settingsStore.settings.value.defaultChatThemeId
                val theme = repository.getTheme(preferred) ?: repository.getTheme("aurora")
                if (theme != null) {
                    _state.update { it.copy(themeId = theme.id, themeName = theme.name) }
                }
            }
        } else {
            viewModelScope.launch {
                val contact = repository.getContact(contactId)
                if (contact == null) {
                    _state.update { it.copy(isLoading = false) }
                    eventsChannel.send(ContactEditorEvent.Deleted)
                    savedOrDeleted = true
                    return@launch
                }
                originalAvatarPath = contact.avatarPath
                val theme = repository.getTheme(contact.themeId)
                _state.update {
                    it.copy(
                        isLoading = false,
                        name = contact.name,
                        username = contact.username,
                        bio = contact.bio,
                        avatarPath = contact.avatarPath,
                        themeId = contact.themeId,
                        themeName = theme?.name ?: "Default",
                        activeNow = contact.activeNow
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) {
        if (value.length > 40) return
        _state.update { it.copy(name = value, nameError = null) }
    }

    fun onUsernameChange(value: String) {
        val cleaned = value.replace("@", "").replace(" ", "").take(30)
        _state.update { it.copy(username = cleaned, usernameError = null) }
    }

    fun onBioChange(value: String) {
        if (value.length > 160) return
        _state.update { it.copy(bio = value) }
    }

    fun setActiveNow(enabled: Boolean) {
        _state.update { it.copy(activeNow = enabled) }
    }

    fun importAvatar(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(importingAvatar = true) }
            val path = mediaStorage.importAvatar(uri)
            if (path == null) {
                _state.update { it.copy(importingAvatar = false) }
                eventsChannel.send(ContactEditorEvent.Message("Could not load that photo"))
                return@launch
            }
            applyNewAvatar(path)
        }
    }

    fun importAvatarFromCapture(capture: PendingCapture) {
        viewModelScope.launch {
            _state.update { it.copy(importingAvatar = true) }
            val path = mediaStorage.importAvatar(capture.uri)
            mediaStorage.deleteCapture(capture.file)
            if (path == null) {
                _state.update { it.copy(importingAvatar = false) }
                eventsChannel.send(ContactEditorEvent.Message("Could not use that photo"))
                return@launch
            }
            applyNewAvatar(path)
        }
    }

    private fun applyNewAvatar(path: String) {
        val previous = _state.value.avatarPath
        if (currentAvatarIsTemporary && previous != null && previous != path) {
            mediaStorage.deletePath(previous)
        }
        currentAvatarIsTemporary = true
        _state.update { it.copy(avatarPath = path, importingAvatar = false) }
    }

    fun removeAvatar() {
        val current = _state.value.avatarPath ?: return
        if (currentAvatarIsTemporary) {
            mediaStorage.deletePath(current)
        }
        currentAvatarIsTemporary = false
        _state.update { it.copy(avatarPath = null) }
    }

    fun prepareCameraCapture(): PendingCapture = mediaStorage.createCameraCapture()

    fun discardCapture(capture: PendingCapture) = mediaStorage.deleteCapture(capture.file)

    fun refreshTheme() {
        if (isNew) return
        viewModelScope.launch {
            val contact = repository.getContact(contactId) ?: return@launch
            val theme = repository.getTheme(contact.themeId)
            _state.update {
                it.copy(themeId = contact.themeId, themeName = theme?.name ?: it.themeName)
            }
        }
    }

    fun save() {
        val current = _state.value
        if (current.saving) return
        val name = current.name.trim()
        val username = current.username.trim().removePrefix("@").lowercase()

        var valid = true
        val nameError = if (name.isBlank()) "Give your friend a name".also { valid = false } else null
        val usernameError = when {
            username.isBlank() -> "Pick a username".also { valid = false }
            !Regex("^[a-z0-9._]{2,30}$").matches(username) ->
                "Use 2–30 letters, numbers, dots or underscores".also { valid = false }
            else -> null
        }
        if (!valid) {
            _state.update { it.copy(nameError = nameError, usernameError = usernameError) }
            return
        }

        _state.update { it.copy(saving = true, nameError = null, usernameError = null) }
        viewModelScope.launch {
            runCatching {
                if (isNew) {
                    val id = repository.insertContact(
                        name = name,
                        username = username,
                        bio = current.bio.trim(),
                        avatarPath = current.avatarPath,
                        themeId = current.themeId,
                        activeNow = current.activeNow
                    )
                    savedOrDeleted = true
                    ContactEditorEvent.Saved(id, isNew = true)
                } else {
                    val contact = repository.getContact(contactId)
                        ?: error("Contact no longer exists")
                    repository.updateContact(
                        contact.copy(
                            name = name,
                            username = username,
                            bio = current.bio.trim(),
                            avatarPath = current.avatarPath,
                            activeNow = current.activeNow
                        )
                    )
                    savedOrDeleted = true
                    ContactEditorEvent.Saved(contactId, isNew = false)
                }
            }.onSuccess { event ->
                eventsChannel.send(event)
            }.onFailure {
                _state.update { state -> state.copy(saving = false) }
                eventsChannel.send(ContactEditorEvent.Message("Could not save profile"))
            }
        }
    }

    fun delete() {
        if (isNew) return
        viewModelScope.launch {
            runCatching { repository.deleteContact(contactId) }
                .onSuccess {
                    savedOrDeleted = true
                    eventsChannel.send(ContactEditorEvent.Deleted)
                }
                .onFailure {
                    eventsChannel.send(ContactEditorEvent.Message("Could not delete profile"))
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (!savedOrDeleted && currentAvatarIsTemporary) {
            mediaStorage.deletePath(_state.value.avatarPath)
        }
    }
}
