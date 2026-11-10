package com.instachat.app.ui.script

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.instachat.app.data.local.entity.ContactEntity
import com.instachat.app.data.local.entity.ScriptEntity
import com.instachat.app.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ScriptEditorUiState(
    val isLoading: Boolean = true,
    val contact: ContactEntity? = null,
    val scripts: List<ScriptEntity> = emptyList()
)

@HiltViewModel
class ScriptEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ChatRepository
) : ViewModel() {

    val contactId: Long = checkNotNull(savedStateHandle["contactId"])

    val uiState: StateFlow<ScriptEditorUiState> = combine(
        repository.observeContact(contactId),
        repository.observeScripts(contactId)
    ) { contact, scripts ->
        ScriptEditorUiState(
            isLoading = contact == null,
            contact = contact,
            scripts = scripts
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ScriptEditorUiState()
    )

    private val eventsChannel = Channel<String>(Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

    fun setAutoReply(enabled: Boolean) {
        viewModelScope.launch { repository.setAutoScript(contactId, enabled) }
    }

    fun setTypingSimulation(enabled: Boolean) {
        viewModelScope.launch { repository.setTypingSimulation(contactId, enabled) }
    }

    fun addScript(text: String, delayMs: Long, typingMs: Long) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.addScript(contactId, text, delayMs, typingMs)
            eventsChannel.send("Reply added to the queue")
        }
    }

    fun updateScript(id: Long, text: String, delayMs: Long, typingMs: Long) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.updateScript(id, text, delayMs, typingMs)
            eventsChannel.send("Reply updated")
        }
    }

    fun deleteScript(id: Long) {
        viewModelScope.launch {
            repository.deleteScript(id, contactId)
            eventsChannel.send("Reply removed")
        }
    }

    fun moveScript(id: Long, direction: Int) {
        viewModelScope.launch { repository.moveScript(contactId, id, direction) }
    }
}
