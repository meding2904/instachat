package com.instachat.app.ui.chat

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.instachat.app.data.local.entity.ContactEntity
import com.instachat.app.data.local.entity.MessageEntity
import com.instachat.app.data.local.entity.MessageStatus
import com.instachat.app.data.local.entity.ScriptEntity
import com.instachat.app.data.local.entity.Sender
import com.instachat.app.data.local.entity.ThemeEntity
import com.instachat.app.data.media.MediaStorage
import com.instachat.app.data.media.PendingCapture
import com.instachat.app.data.prefs.SettingsStore
import com.instachat.app.data.repository.ChatRepository
import com.instachat.app.data.repository.SeedThemes
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class ChatUiState(
    val isLoading: Boolean = true,
    val contact: ContactEntity? = null,
    val messages: List<MessageEntity> = emptyList(),
    val theme: ThemeEntity = SeedThemes.all.first(),
    val scripts: List<ScriptEntity> = emptyList(),
    val isTyping: Boolean = false,
    val isAutoPlaying: Boolean = false,
    val isPresentation: Boolean = false
)

sealed interface ChatEvent {
    data class ShowMessage(val message: String) : ChatEvent
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ChatRepository,
    private val settingsStore: SettingsStore,
    private val mediaStorage: MediaStorage
) : ViewModel() {

    private val contactId: Long = checkNotNull(savedStateHandle["contactId"])
    private val presentation: Boolean = savedStateHandle["presentation"] ?: false

    private val typing = MutableStateFlow(false)
    private val autoPlaying = MutableStateFlow(false)
    private val eventsChannel = Channel<ChatEvent>(Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

    private data class Core(
        val contact: ContactEntity?,
        val messages: List<MessageEntity>,
        val scripts: List<ScriptEntity>,
        val theme: ThemeEntity
    )

    private val core: Flow<Core> = combine(
        repository.observeContact(contactId),
        repository.observeMessages(contactId),
        repository.observeScripts(contactId),
        repository.observeThemes()
    ) { contact, messages, scripts, themes ->
        val theme = contact?.let { c -> themes.firstOrNull { it.id == c.themeId } }
            ?: themes.firstOrNull()
            ?: SeedThemes.all.first()
        Core(contact, messages, scripts, theme)
    }

    val uiState: StateFlow<ChatUiState> = combine(core, typing, autoPlaying) { core, isTyping, auto ->
        ChatUiState(
            isLoading = core.contact == null,
            contact = core.contact,
            messages = core.messages,
            theme = core.theme,
            scripts = core.scripts,
            isTyping = isTyping,
            isAutoPlaying = auto,
            isPresentation = presentation
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ChatUiState(isPresentation = presentation)
    )

    private var simulationJob: Job? = null
    private var autoReplyJob: Job? = null
    private var scriptCursor = 0
    private var presentationStarted = false

    init {
        viewModelScope.launch { repository.markConversationSeen(contactId) }
    }

    fun sendText(text: String, sender: Sender) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            val id = repository.sendText(contactId, sender, clean)
            if (id == null) {
                eventsChannel.send(ChatEvent.ShowMessage("Could not send message"))
                return@launch
            }
            afterInserted(id, sender)
        }
    }

    fun sendImage(uri: Uri, sender: Sender) {
        viewModelScope.launch {
            val path = mediaStorage.importImage(uri)
            if (path == null) {
                eventsChannel.send(ChatEvent.ShowMessage("Could not import that image"))
                return@launch
            }
            val id = repository.sendImage(contactId, sender, path)
            if (id == null) {
                mediaStorage.deletePath(path)
                eventsChannel.send(ChatEvent.ShowMessage("Could not send image"))
                return@launch
            }
            afterInserted(id, sender)
        }
    }

    fun sendCameraImage(capture: PendingCapture, sender: Sender) {
        viewModelScope.launch {
            val path = mediaStorage.importImage(capture.uri)
            mediaStorage.deleteCapture(capture.file)
            if (path == null) {
                eventsChannel.send(ChatEvent.ShowMessage("Could not use that photo"))
                return@launch
            }
            val id = repository.sendImage(contactId, sender, path)
            if (id == null) {
                mediaStorage.deletePath(path)
                eventsChannel.send(ChatEvent.ShowMessage("Could not send image"))
                return@launch
            }
            afterInserted(id, sender)
        }
    }

    fun sendAudioFile(uri: Uri, sender: Sender) {
        viewModelScope.launch {
            val path = mediaStorage.importAudio(uri)
            if (path == null) {
                eventsChannel.send(ChatEvent.ShowMessage("Could not import that audio file"))
                return@launch
            }
            val duration = mediaStorage.durationMs(path) ?: 0L
            val id = repository.sendVoice(contactId, sender, path, duration)
            if (id == null) {
                mediaStorage.deletePath(path)
                eventsChannel.send(ChatEvent.ShowMessage("Could not add voice note"))
                return@launch
            }
            afterInserted(id, sender)
        }
    }

    fun sendRecordedVoice(file: File, durationMs: Long, sender: Sender) {
        viewModelScope.launch {
            if (!file.exists() || file.length() < 64L) {
                mediaStorage.deletePath(file.absolutePath)
                return@launch
            }
            val actualDuration = mediaStorage.durationMs(file.absolutePath) ?: durationMs
            val id = repository.sendVoice(contactId, sender, file.absolutePath, actualDuration)
            if (id == null) {
                mediaStorage.deletePath(file.absolutePath)
                eventsChannel.send(ChatEvent.ShowMessage("Could not save voice message"))
            } else {
                afterInserted(id, sender)
            }
        }
    }

    fun prepareCameraCapture(): PendingCapture = mediaStorage.createCameraCapture()

    fun discardCapture(capture: PendingCapture) = mediaStorage.deleteCapture(capture.file)

    fun createVoiceFile(): File = mediaStorage.createVoiceFile()

    fun discardVoiceFile(file: File) = mediaStorage.deletePath(file.absolutePath)

    private suspend fun afterInserted(messageId: Long, sender: Sender) {
        if (sender == Sender.FRIEND) {
            repository.markConversationSeen(contactId)
        } else {
            scheduleReceipts(messageId)
            maybeAutoReply()
        }
    }

    private fun scheduleReceipts(messageId: Long) {
        viewModelScope.launch {
            delay(850)
            repository.markMessageStatus(messageId, MessageStatus.DELIVERED)
            delay(1_300)
            if (settingsStore.settings.value.readReceipts) {
                repository.markMessageStatus(messageId, MessageStatus.SEEN)
            }
        }
    }

    fun toggleReaction(messageId: Long, emoji: String) {
        viewModelScope.launch { repository.toggleReaction(messageId, emoji) }
    }

    fun deleteMessage(messageId: Long) {
        viewModelScope.launch {
            repository.deleteMessage(messageId)
            eventsChannel.send(ChatEvent.ShowMessage("Message deleted"))
        }
    }

    fun editTimestamp(messageId: Long, timestamp: Long) {
        viewModelScope.launch {
            repository.updateTimestamp(messageId, timestamp)
            eventsChannel.send(ChatEvent.ShowMessage("Timestamp updated"))
        }
    }

    fun backfillMessage(sender: Sender, text: String, timestamp: Long) {
        if (text.isBlank()) return
        viewModelScope.launch {
            val id = repository.sendText(contactId, sender, text.trim(), timestamp = timestamp)
            if (id == null) {
                eventsChannel.send(ChatEvent.ShowMessage("Could not add message"))
                return@launch
            }
            // Historical messages are already read.
            repository.markMessageStatus(id, MessageStatus.SEEN)
            if (sender == Sender.FRIEND) {
                repository.markConversationSeen(contactId)
            }
            eventsChannel.send(ChatEvent.ShowMessage("Added to history"))
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearChat(contactId)
            eventsChannel.send(ChatEvent.ShowMessage("Chat cleared"))
        }
    }

    fun runNextScript() {
        if (simulationJob?.isActive == true || autoReplyJob?.isActive == true) return
        simulationJob = viewModelScope.launch { playOneScript() }
    }

    fun startAutoPlay() {
        if (simulationJob?.isActive == true) return
        autoReplyJob?.cancel()
        simulationJob = viewModelScope.launch {
            autoPlaying.value = true
            try {
                val lines = repository.getScripts(contactId)
                if (lines.isEmpty()) {
                    eventsChannel.send(ChatEvent.ShowMessage("Add scripted replies first"))
                    return@launch
                }
                repeat(lines.size) {
                    if (!isActive) return@launch
                    val played = playOneScript()
                    if (!played) return@launch
                    delay(650)
                }
            } finally {
                typing.value = false
                autoPlaying.value = false
            }
        }
    }

    fun stopSimulation() {
        simulationJob?.cancel()
        autoReplyJob?.cancel()
        typing.value = false
        autoPlaying.value = false
    }

    fun startPresentationPlayback() {
        if (!presentation || presentationStarted) return
        presentationStarted = true
        viewModelScope.launch {
            delay(1_400)
            if (repository.getScripts(contactId).isNotEmpty()) {
                startAutoPlay()
            }
        }
    }

    private suspend fun playOneScript(): Boolean {
        val scripts = repository.getScripts(contactId)
        if (scripts.isEmpty()) {
            eventsChannel.send(ChatEvent.ShowMessage("No scripted replies yet"))
            return false
        }
        val contact = repository.getContact(contactId) ?: return false
        val script = scripts[scriptCursor % scripts.size]
        scriptCursor = (scriptCursor + 1) % scripts.size

        try {
            delay(script.delayMs.coerceIn(200L, 30_000L))
            if (contact.typingSimulationEnabled) {
                typing.value = true
                delay(script.typingMs.coerceIn(300L, 30_000L))
                typing.value = false
            } else {
                delay(script.typingMs.coerceIn(300L, 30_000L))
            }
            val id = repository.sendText(
                contactId = contactId,
                sender = Sender.FRIEND,
                text = script.text,
                scripted = true
            )
            if (id != null) repository.markConversationSeen(contactId)
        } finally {
            typing.value = false
        }
        return true
    }

    private fun maybeAutoReply() {
        if (uiState.value.contact?.autoScriptEnabled != true) return
        if (simulationJob?.isActive == true) return
        autoReplyJob?.cancel()
        autoReplyJob = viewModelScope.launch {
            delay(650)
            if (uiState.value.contact?.autoScriptEnabled == true) {
                playOneScript()
            }
        }
    }
}
