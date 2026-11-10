package com.instachat.app.ui.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.instachat.app.data.model.InboxItem
import com.instachat.app.data.repository.ChatRepository
import com.instachat.app.data.repository.DemoSeeder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InboxUiState(
    val chats: List<InboxItem> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = true,
    val totalCount: Int = 0
) {
    val isSearching: Boolean get() = query.isNotBlank()
}

sealed interface InboxEvent {
    data class ShowMessage(val message: String) : InboxEvent
}

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val seeder: DemoSeeder
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val ready = MutableStateFlow(false)

    private val eventsChannel = Channel<InboxEvent>(Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

    val uiState: StateFlow<InboxUiState> = combine(
        repository.observeInbox(),
        query,
        ready
    ) { chats, currentQuery, seeded ->
        val filtered = if (currentQuery.isBlank()) {
            chats
        } else {
            chats.filter {
                it.contact.name.contains(currentQuery, ignoreCase = true) ||
                    it.contact.username.contains(currentQuery, ignoreCase = true)
            }
        }
        InboxUiState(
            chats = filtered,
            query = currentQuery,
            isLoading = !seeded,
            totalCount = chats.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = InboxUiState()
    )

    init {
        viewModelScope.launch {
            runCatching { seeder.seedIfNeeded() }
                .onFailure {
                    eventsChannel.send(InboxEvent.ShowMessage("Could not load demo chats"))
                }
            ready.value = true
        }
    }

    fun setQuery(value: String) {
        query.value = value
    }

    fun deleteContact(contactId: Long) {
        viewModelScope.launch {
            runCatching { repository.deleteContact(contactId) }
                .onSuccess { eventsChannel.send(InboxEvent.ShowMessage("Chat deleted")) }
                .onFailure { eventsChannel.send(InboxEvent.ShowMessage("Could not delete chat")) }
        }
    }
}
