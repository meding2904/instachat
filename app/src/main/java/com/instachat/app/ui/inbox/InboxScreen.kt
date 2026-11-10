package com.instachat.app.ui.inbox

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.instachat.app.R
import com.instachat.app.data.local.entity.MessageType
import com.instachat.app.data.local.entity.Sender
import com.instachat.app.data.model.InboxItem
import com.instachat.app.ui.components.AppSystemBars
import com.instachat.app.ui.components.Avatar
import com.instachat.app.ui.components.ConfirmDialog
import com.instachat.app.ui.components.EmptyState
import com.instachat.app.ui.components.LoadingChats
import com.instachat.app.ui.theme.NotificationRed
import com.instachat.app.util.TimeText
import kotlinx.coroutines.launch

@Composable
fun InboxScreen(
    onOpenChat: (Long) -> Unit,
    onNewContact: () -> Unit,
    onEditContact: (Long) -> Unit,
    onOpenScripts: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: InboxViewModel = hiltViewModel()
) {
    AppSystemBars(darkTheme = true)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val searchFocusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var searchFocusRequest by remember { mutableIntStateOf(0) }
    var titleMenuOpen by remember { mutableStateOf(false) }
    var menuChat by remember { mutableStateOf<InboxItem?>(null) }
    var deleteChat by remember { mutableStateOf<InboxItem?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is InboxEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    LaunchedEffect(searchFocusRequest) {
        if (searchFocusRequest > 0) {
            searchFocusRequester.requestFocus()
            keyboard?.show()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                snackbarHostState,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 58.dp)
            )
        },
        containerColor = Color.Black,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
                .statusBarsPadding()
        ) {
            InboxTopBar(
                unread = state.chats.sumOf { it.unreadCount } > 0,
                menuOpen = titleMenuOpen,
                onMenuOpenChange = { titleMenuOpen = it },
                onNewContact = onNewContact,
                onOpenSettings = onOpenSettings
            )

            SearchField(
                query = state.query,
                onQueryChange = viewModel::setQuery,
                focusRequester = searchFocusRequester,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            if (state.chats.isNotEmpty() && !state.isSearching) {
                NotesRow(
                    chats = state.chats,
                    onOpenChat = onOpenChat,
                    onNewContact = onNewContact
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Messages",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Requests",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = Color(0xFF4A68FF),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            scope.launch {
                                snackbarHostState.showSnackbar("No message requests")
                            }
                        }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            when {
                state.isLoading -> {
                    Column { repeat(6) { LoadingChats() } }
                }

                state.chats.isEmpty() && state.isSearching -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyState(
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_search),
                                    contentDescription = null,
                                    tint = Color(0xFF8E8E93),
                                    modifier = Modifier.size(32.dp)
                                )
                            },
                            title = "No chats found",
                            message = "Try another name or username."
                        )
                    }
                }

                state.chats.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyState(
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_plus_circle),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(38.dp)
                                )
                            },
                            title = "No conversations yet",
                            message = "Create a fictional friend to start a simulated chat.",
                            action = {
                                com.instachat.app.ui.components.GradientPillButton(
                                    text = "New friend",
                                    icon = R.drawable.ic_plus,
                                    onClick = onNewContact
                                )
                            }
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        state = listState,
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        itemsIndexed(
                            items = state.chats,
                            key = { _, item -> item.contact.id }
                        ) { _, item ->
                            Box {
                                ChatRow(
                                    item = item,
                                    onClick = { onOpenChat(item.contact.id) },
                                    onLongClick = { menuChat = item }
                                )
                                DropdownMenu(
                                    expanded = menuChat?.contact?.id == item.contact.id,
                                    onDismissRequest = { menuChat = null },
                                    shape = RoundedCornerShape(14.dp),
                                    containerColor = Color(0xFF262626)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Edit contact", color = Color.White) },
                                        leadingIcon = {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_insta_edit),
                                                contentDescription = null,
                                                tint = Color.White
                                            )
                                        },
                                        onClick = {
                                            menuChat = null
                                            onEditContact(item.contact.id)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Set script", color = Color.White) },
                                        leadingIcon = {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_insta_sparkle),
                                                contentDescription = null,
                                                tint = Color.White
                                            )
                                        },
                                        onClick = {
                                            menuChat = null
                                            onOpenScripts(item.contact.id)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Delete chat",
                                                color = Color(0xFFFF453A)
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_insta_trash),
                                                contentDescription = null,
                                                tint = Color(0xFFFF453A)
                                            )
                                        },
                                        onClick = {
                                            menuChat = null
                                            deleteChat = item
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            InboxBottomBar(
                hasUnread = state.chats.any { it.unreadCount > 0 },
                onHome = {
                    viewModel.setQuery("")
                    if (state.chats.isNotEmpty()) {
                        scope.launch { listState.animateScrollToItem(0) }
                    }
                },
                onReels = {
                    scope.launch {
                        snackbarHostState.showSnackbar("Reels aren't part of simulated chats")
                    }
                },
                onMessages = {
                    viewModel.setQuery("")
                    if (state.chats.isNotEmpty()) {
                        scope.launch { listState.animateScrollToItem(0) }
                    }
                },
                onSearch = { searchFocusRequest++ },
                onProfile = onOpenSettings
            )
        }
    }

    deleteChat?.let { chat ->
        ConfirmDialog(
            title = "Delete this chat?",
            message = "All messages with ${chat.contact.name} will be removed from this device. This can't be undone.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                viewModel.deleteContact(chat.contact.id)
                deleteChat = null
            },
            onDismiss = { deleteChat = null }
        )
    }
}

@Composable
private fun InboxTopBar(
    unread: Boolean,
    menuOpen: Boolean,
    onMenuOpenChange: (Boolean) -> Unit,
    onNewContact: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .padding(horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.size(48.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onMenuOpenChange(true) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "InstaChat",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_down),
                        contentDescription = "Menu",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    if (unread) {
                        Spacer(Modifier.width(5.dp))
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(NotificationRed)
                        )
                    }
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { onMenuOpenChange(false) },
                    containerColor = Color(0xFF262626),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text("New friend", color = Color.White) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_plus),
                                contentDescription = null,
                                tint = Color.White
                            )
                        },
                        onClick = {
                            onMenuOpenChange(false)
                            onNewContact()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Settings", color = Color.White) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_insta_settings),
                                contentDescription = null,
                                tint = Color.White
                            )
                        },
                        onClick = {
                            onMenuOpenChange(false)
                            onOpenSettings()
                        }
                    )
                }
            }
            IconButton(
                onClick = onNewContact,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_compose),
                    contentDescription = "New chat",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
private fun NotesRow(
    chats: List<InboxItem>,
    onOpenChat: (Long) -> Unit,
    onNewContact: () -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "your-note") {
            NoteAvatarItem(
                name = "Your note",
                avatarName = "You",
                avatarPath = null,
                noteText = "Note...",
                isMutedNote = true,
                onClick = onNewContact
            )
        }
        items(chats.take(10), key = { it.contact.id }) { item ->
            NoteAvatarItem(
                name = item.contact.name.substringBefore(" "),
                avatarName = item.contact.name,
                avatarPath = item.contact.avatarPath,
                noteText = item.contact.bio.ifBlank { "Active now" },
                isMutedNote = false,
                onClick = { onOpenChat(item.contact.id) }
            )
        }
    }
}

@Composable
private fun NoteAvatarItem(
    name: String,
    avatarName: String,
    avatarPath: String?,
    noteText: String,
    isMutedNote: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(84.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(84.dp)
                .height(96.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Avatar(
                name = avatarName,
                path = avatarPath,
                size = 68.dp
            )
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFF262626), RoundedCornerShape(14.dp))
                        .padding(horizontal = 9.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = noteText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            lineHeight = 13.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = if (isMutedNote) Color(0xFF8E8E93) else Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
                Box(
                    modifier = Modifier
                        .offset(x = (-10).dp, y = (-2).dp)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF262626))
                )
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            ),
            color = if (isMutedNote) Color(0xFF8E8E93) else Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatRow(
    item: InboxItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val unread = item.unreadCount > 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(
            name = item.contact.name,
            path = item.contact.avatarPath,
            size = 56.dp
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.contact.name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.5.sp,
                    fontWeight = if (unread) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val preview = when {
                    item.previewType == MessageType.IMAGE ->
                        if (item.previewSender == Sender.ME) "You sent a photo" else "Sent a photo"
                    item.previewType == MessageType.VOICE ->
                        if (item.previewSender == Sender.ME) "You sent a voice message" else "Sent a voice message"
                    item.preview == null -> "Tap to chat"
                    item.previewSender == Sender.ME -> "You: ${item.preview}"
                    else -> item.preview
                }
                val time = item.timestamp?.let { TimeText.inboxTime(LocalContext.current, it) }
                Text(
                    text = if (time != null) "$preview · $time" else preview,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 14.sp,
                        fontWeight = if (unread) FontWeight.Medium else FontWeight.Normal
                    ),
                    color = if (unread) Color.White else Color(0xFF8E8E93),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        if (unread) {
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0095F6))
            )
        }
        Spacer(Modifier.width(14.dp))
        Icon(
            painter = painterResource(R.drawable.ic_camera),
            contentDescription = null,
            tint = if (unread) Color.White else Color(0xFF8E8E93),
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .background(Color(0xFF262626), RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = null,
            tint = Color(0xFF8E8E93),
            modifier = Modifier.size(19.dp)
        )
        Spacer(Modifier.width(12.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = Color.White,
                fontSize = 15.5.sp
            ),
            cursorBrush = SolidColor(Color(0xFF4A5DF9)),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(
                        text = "Search",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = Color(0xFF8E8E93)
                    )
                }
                inner()
            }
        )
        if (query.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3A3A3C))
                    .clickable { onQueryChange("") },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = "Clear search",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun InboxBottomBar(
    hasUnread: Boolean,
    onHome: () -> Unit,
    onReels: () -> Unit,
    onMessages: () -> Unit,
    onSearch: () -> Unit,
    onProfile: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(Color(0xFF1C1C1E))
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BottomBarIcon(R.drawable.ic_home, "Home", onHome)
            BottomBarIcon(R.drawable.ic_reels, "Reels", onReels)
            BottomBarIcon(R.drawable.ic_send, "Messages", onMessages)
            BottomBarIcon(R.drawable.ic_search, "Search", onSearch)
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onProfile),
                contentAlignment = Alignment.Center
            ) {
                Box {
                    Avatar(name = "You", path = null, size = 26.dp)
                    if (hasUnread) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(NotificationRed)
                                .align(Alignment.BottomEnd)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomBarIcon(
    @androidx.annotation.DrawableRes icon: Int,
    description: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            tint = Color.White,
            modifier = Modifier.size(25.dp)
        )
    }
}
