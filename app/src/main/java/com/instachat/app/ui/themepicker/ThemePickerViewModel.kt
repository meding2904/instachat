package com.instachat.app.ui.themepicker

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.instachat.app.data.local.entity.ContactEntity
import com.instachat.app.data.local.entity.ThemeEntity
import com.instachat.app.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ThemePickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ChatRepository
) : ViewModel() {

    val contactId: Long = checkNotNull(savedStateHandle["contactId"])

    val contact: StateFlow<ContactEntity?> = repository.observeContact(contactId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val themes: StateFlow<List<ThemeEntity>> = repository.observeThemes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectTheme(themeId: String) {
        viewModelScope.launch { repository.setTheme(contactId, themeId) }
    }
}
