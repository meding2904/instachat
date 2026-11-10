package com.instachat.app.ui.chat

import android.Manifest
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.instachat.app.R
import com.instachat.app.data.local.entity.ContactEntity
import com.instachat.app.data.local.entity.MessageEntity
import com.instachat.app.data.local.entity.Sender
import com.instachat.app.data.media.PendingCapture
import com.instachat.app.data.media.VoiceRecorder
import com.instachat.app.ui.components.AppSystemBars
import com.instachat.app.ui.components.Avatar
import com.instachat.app.ui.components.ConfirmDialog
import com.instachat.app.ui.components.PermissionDialog
import com.instachat.app.util.TimeText
import com.instachat.app.util.findActivity
import com.instachat.app.util.hasPermission
import com.instachat.app.util.showDateTimePicker
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val GROUP_BREAK_MS = 60 * 60 * 1000L

private sealed interface ChatItem {
    val key: String

    data class Separator(val timestamp: Long) : ChatItem {
        override val key: String = "separator-$timestamp"
    }

    data class Message(
        val message: MessageEntity,
        val isFirstInGroup: Boolean,
        val isLastInGroup: Boolean
    ) : ChatItem {
        override val key: String = "msg-${message.id}"
    }

    data object Typing : ChatItem {
        override val key: String = "typing"
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    onBack: () -> Unit,
    onEditContact: (Long) -> Unit,
    onOpenTheme: (Long) -> Unit,
    onOpenScripts: (Long) -> Unit,
    onOpenPresentation: (Long) -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = rememberChatPalette(state.theme)
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val haptic = LocalHapticFeedback.current
    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var draft by rememberSaveable { mutableStateOf("") }
    var sender by rememberSaveable { mutableStateOf(Sender.ME) }
    var selectedMessage by remember { mutableStateOf<MessageEntity?>(null) }
    var viewerImage by remember { mutableStateOf<String?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showCameraPermissionDialog by remember { mutableStateOf(false) }
    var showMicPermissionDialog by remember { mutableStateOf(false) }
    var pendingCapture by remember { mutableStateOf<PendingCapture?>(null) }
    var showBackfillDialog by remember { mutableStateOf(false) }
    var showToolsSheet by remember { mutableStateOf(false) }
    var showEmojiSheet by remember { mutableStateOf(false) }

    val recorder = remember(context) { VoiceRecorder(context) }
    var isRecording by remember { mutableStateOf(false) }
    var isRecordingLocked by remember { mutableStateOf(false) }
    var recordingSender by remember { mutableStateOf(Sender.ME) }
    val recordingAmplitudes by recorder.amplitudes.collectAsStateWithLifecycle()
    val recordingElapsed by recorder.elapsedMs.collectAsStateWithLifecycle()

    DisposableEffect(recorder) {
        onDispose { recorder.release() }
    }

    AppSystemBars(darkTheme = palette.isDark)

    DisposableEffect(state.isPresentation, activity) {
        if (state.isPresentation) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            if (state.isPresentation) {
                activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.sendImage(uri, sender)
    }

    val audioFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.sendAudioFile(uri, Sender.FRIEND)
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scope.launch { snackbarHostState.showSnackbar("Hold the mic again to record") }
        } else {
            showMicPermissionDialog = true
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val capture = pendingCapture
        if (capture != null) {
            if (success) viewModel.sendCameraImage(capture, sender)
            else viewModel.discardCapture(capture)
        }
        pendingCapture = null
    }

    val launchCamera: () -> Unit = {
        val capture = viewModel.prepareCameraCapture()
        pendingCapture = capture
        takePictureLauncher.launch(capture.uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera() else showCameraPermissionDialog = true
    }

    fun requestCamera() {
        if (context.hasPermission(Manifest.permission.CAMERA)) {
            launchCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun openGallery() {
        galleryLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    fun startRecording() {
        if (isRecording) return
        if (!context.hasPermission(Manifest.permission.RECORD_AUDIO)) {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        val target = viewModel.createVoiceFile()
        if (recorder.start(target)) {
            isRecording = true
            isRecordingLocked = false
            recordingSender = sender
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } else {
            viewModel.discardVoiceFile(target)
            scope.launch { snackbarHostState.showSnackbar("Could not start recording") }
        }
    }

    fun finishRecording(cancel: Boolean = false) {
        if (!isRecording) return
        isRecording = false
        isRecordingLocked = false
        if (cancel) {
            recorder.cancel()
            return
        }
        val result = recorder.stop()
        if (result == null) {
            scope.launch { snackbarHostState.showSnackbar("Could not save that recording") }
        } else if (result.durationMs < 700L) {
            viewModel.discardVoiceFile(result.file)
            scope.launch { snackbarHostState.showSnackbar("Hold a little longer to record") }
        } else {
            viewModel.sendRecordedVoice(result.file, result.durationMs, recordingSender)
        }
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ChatEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    LaunchedEffect(state.isPresentation, state.scripts.size) {
        if (state.isPresentation && state.scripts.isNotEmpty()) {
            viewModel.startPresentationPlayback()
        }
    }

    val chatItems = remember(state.messages, state.isTyping) {
        buildChatItems(state.messages, state.isTyping)
    }

    var didInitialScroll by remember { mutableStateOf(false) }
    LaunchedEffect(chatItems.size, chatItems.lastOrNull()?.key) {
        if (chatItems.isNotEmpty()) {
            if (!didInitialScroll) {
                listState.scrollToItem(chatItems.lastIndex)
                didInitialScroll = true
            } else {
                listState.animateScrollToItem(chatItems.lastIndex)
            }
        }
    }

    val imeVisible = WindowInsets.isImeVisible
    LaunchedEffect(imeVisible) {
        if (imeVisible && chatItems.isNotEmpty()) {
            delay(80)
            listState.animateScrollToItem(chatItems.lastIndex)
        }
    }

    var knownIds by remember { mutableStateOf<Set<Long>?>(null) }
    var arrivalId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(state.messages) {
        val ids = state.messages.map { it.id }.toSet()
        val known = knownIds
        if (known == null) {
            knownIds = ids
        } else {
            val added = ids - known
            if (added.isNotEmpty()) {
                arrivalId = state.messages.lastOrNull { it.id in added }?.id
            }
            knownIds = ids
        }
    }
    LaunchedEffect(arrivalId) {
        val current = arrivalId ?: return@LaunchedEffect
        delay(800)
        if (arrivalId == current) arrivalId = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ChatBackdrop(palette)
        Column(modifier = Modifier.fillMaxSize()) {
            ChatTopBar(
                contact = state.contact,
                isTyping = state.isTyping,
                isPresentation = state.isPresentation,
                onBack = onBack,
                onEditContact = { state.contact?.let { onEditContact(it.id) } },
                onCall = {
                    scope.launch {
                        snackbarHostState.showSnackbar("Calls aren't available in a simulated chat")
                    }
                },
                onVideo = {
                    scope.launch {
                        snackbarHostState.showSnackbar("Video calls aren't available in a simulated chat")
                    }
                }
            )

            Box(modifier = Modifier.weight(1f)) {
                when {
                    state.isLoading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                color = palette.sentEnd,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    state.messages.isEmpty() -> {
                        EmptyChat(
                            contact = state.contact,
                            palette = palette
                        )
                    }

                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 6.dp, bottom = 8.dp)
                        ) {
                            items(
                                count = chatItems.size,
                                key = { chatItems[it].key }
                            ) { index ->
                                when (val item = chatItems[index]) {
                                    is ChatItem.Separator -> {
                                        DateChip(
                                            timestamp = item.timestamp,
                                            palette = palette,
                                            modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
                                        )
                                    }

                                    is ChatItem.Message -> {
                                        val message = item.message
                                        val position = when {
                                            item.isFirstInGroup && item.isLastInGroup -> MessagePosition.SINGLE
                                            item.isFirstInGroup -> MessagePosition.FIRST
                                            item.isLastInGroup -> MessagePosition.LAST
                                            else -> MessagePosition.MIDDLE
                                        }
                                        val showAvatar = message.sender != Sender.ME &&
                                            item.isLastInGroup
                                        val prevIsSeparator = chatItems.getOrNull(index - 1) is ChatItem.Separator
                                        val topGap = when {
                                            prevIsSeparator -> 2.dp
                                            item.isFirstInGroup -> 12.dp
                                            else -> 2.dp
                                        }
                                        MessageBubble(
                                            message = message,
                                            position = position,
                                            showAvatar = showAvatar,
                                            contactName = state.contact?.name.orEmpty(),
                                            contactAvatar = state.contact?.avatarPath,
                                            showReceipt = state.messages.lastOrNull()?.id == message.id &&
                                                message.sender == Sender.ME,
                                            palette = palette,
                                            animateArrival = arrivalId == message.id,
                                            onLongPress = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                selectedMessage = it
                                            },
                                            onImageClick = { viewerImage = it },
                                            modifier = Modifier.padding(top = topGap)
                                        )
                                    }

                                    ChatItem.Typing -> {
                                        Box(Modifier.padding(top = 10.dp)) {
                                            TypingMessageRow(
                                                contactName = state.contact?.name.orEmpty(),
                                                contactAvatar = state.contact?.avatarPath,
                                                palette = palette
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            ChatComposer(
                contact = state.contact,
                sender = sender,
                draft = draft,
                onDraftChange = { draft = it },
                palette = palette,
                isPresentation = state.isPresentation,
                onSend = {
                    if (draft.isNotBlank()) {
                        viewModel.sendText(draft, sender)
                        draft = ""
                    }
                },
                onGallery = ::openGallery,
                onCamera = ::requestCamera,
                isRecording = isRecording,
                isRecordingLocked = isRecordingLocked,
                recordingElapsedMs = recordingElapsed,
                recordingAmplitudes = recordingAmplitudes,
                onRecordStart = ::startRecording,
                onRecordLock = {
                    isRecordingLocked = true
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                onRecordEnd = { finishRecording() },
                onCancelRecording = { finishRecording(cancel = true) },
                onOpenTools = { showToolsSheet = true },
                onOpenEmoji = { showEmojiSheet = true }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 132.dp, start = 16.dp, end = 16.dp)
        )
    }

    selectedMessage?.let { message ->
        MessageActionsSheet(
            message = message,
            canCopy = !message.text.isNullOrBlank(),
            onDismiss = { selectedMessage = null },
            onReact = { emoji ->
                viewModel.toggleReaction(message.id, emoji)
                selectedMessage = null
            },
            onCopy = {
                clipboard.setText(AnnotatedString(message.text.orEmpty()))
                selectedMessage = null
                scope.launch { snackbarHostState.showSnackbar("Copied to clipboard") }
            },
            onEditTimestamp = {
                selectedMessage = null
                showDateTimePicker(context, message.timestamp) { picked ->
                    viewModel.editTimestamp(message.id, picked)
                }
            },
            onDelete = {
                viewModel.deleteMessage(message.id)
                selectedMessage = null
            }
        )
    }

    viewerImage?.let { path ->
        ImageViewer(path = path, onDismiss = { viewerImage = null })
    }

    if (showClearDialog) {
        ConfirmDialog(
            title = "Clear this chat?",
            message = "Every message in this conversation will be deleted. This can't be undone.",
            confirmLabel = "Clear chat",
            destructive = true,
            onConfirm = {
                showClearDialog = false
                viewModel.clearChat()
            },
            onDismiss = { showClearDialog = false }
        )
    }

    if (showCameraPermissionDialog) {
        PermissionDialog(
            title = "Camera access needed",
            message = "Allow camera access to take a photo for this chat. You can change this later in Android settings.",
            onDismiss = { showCameraPermissionDialog = false },
            onOpenSettings = {
                showCameraPermissionDialog = false
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:${context.packageName}")
                    )
                )
            }
        )
    }

    if (showMicPermissionDialog) {
        PermissionDialog(
            title = "Microphone access needed",
            message = "Allow microphone access to hold the mic button and record a voice message. You can change this later in Android settings.",
            onDismiss = { showMicPermissionDialog = false },
            onOpenSettings = {
                showMicPermissionDialog = false
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:${context.packageName}")
                    )
                )
            }
        )
    }

    if (showBackfillDialog) {
        BackfillDialog(
            contact = state.contact,
            onDismiss = { showBackfillDialog = false },
            onAdd = { backfillSender, text, timestamp ->
                viewModel.backfillMessage(backfillSender, text, timestamp)
                showBackfillDialog = false
            }
        )
    }

    if (showToolsSheet) {
        ChatToolsSheet(
            contact = state.contact,
            sender = sender,
            scriptsCount = state.scripts.size,
            simulationActive = state.isTyping || state.isAutoPlaying,
            onSenderChange = { sender = it },
            onDismiss = { showToolsSheet = false },
            onEditProfile = { state.contact?.let { onEditContact(it.id) } },
            onOpenTheme = { state.contact?.let { onOpenTheme(it.id) } },
            onOpenScripts = { state.contact?.let { onOpenScripts(it.id) } },
            onRunNextScript = viewModel::runNextScript,
            onAutoPlay = viewModel::startAutoPlay,
            onStopScript = viewModel::stopSimulation,
            onBackfill = { showBackfillDialog = true },
            onImportVoice = { audioFileLauncher.launch(arrayOf("audio/*")) },
            onScreenRecord = { state.contact?.let { onOpenPresentation(it.id) } },
            onClearChat = { showClearDialog = true }
        )
    }

    if (showEmojiSheet) {
        EmojiSheet(
            onDismiss = { showEmojiSheet = false },
            onEmoji = { emoji ->
                draft += emoji
                showEmojiSheet = false
            }
        )
    }
}

private fun buildChatItems(messages: List<MessageEntity>, typing: Boolean): List<ChatItem> {
    val result = mutableListOf<ChatItem>()
    var previousDay: java.time.LocalDate? = null
    // Messages are ordered oldest -> newest. If the list ever switches to
    // reverseLayout, swap the previous/next lookups so first/last stay correct.
    messages.forEachIndexed { index, message ->
        val day = TimeText.localDate(message.timestamp)
        val previous = messages.getOrNull(index - 1)
        val newDay = day != previousDay
        val longGap = previous != null && message.timestamp - previous.timestamp > GROUP_BREAK_MS
        val insertedSeparator = newDay || longGap
        if (insertedSeparator) {
            result += ChatItem.Separator(timestamp = message.timestamp)
            previousDay = day
        }
        val next = messages.getOrNull(index + 1)
        val nextHasSeparator = next != null && (
            !TimeText.isSameDay(message.timestamp, next.timestamp) ||
                next.timestamp - message.timestamp > GROUP_BREAK_MS
            )
        val groupedWithPrevious = !insertedSeparator &&
            previous != null &&
            previous.sender == message.sender
        val groupedWithNext = !nextHasSeparator &&
            next != null &&
            next.sender == message.sender
        result += ChatItem.Message(
            message = message,
            isFirstInGroup = !groupedWithPrevious,
            isLastInGroup = !groupedWithNext
        )
    }
    if (typing) result += ChatItem.Typing
    return result
}

@Composable
private fun ChatTopBar(
    contact: ContactEntity?,
    isTyping: Boolean,
    isPresentation: Boolean,
    onBack: () -> Unit,
    onEditContact: () -> Unit,
    onCall: () -> Unit,
    onVideo: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(start = 6.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(42.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.width(4.dp))
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = !isPresentation, onClick = onEditContact)
                    .padding(vertical = 4.dp, horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(
                    name = contact?.name.orEmpty(),
                    path = contact?.avatarPath,
                    size = 36.dp
                )
                Spacer(Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = contact?.name ?: "Conversation",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(Modifier.width(3.dp))
                        Icon(
                            painter = painterResource(R.drawable.ic_chevron_right),
                            contentDescription = null,
                            tint = Color(0xFF8E8E93),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    AnimatedContent(
                        targetState = isTyping,
                        transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(100)) },
                        label = "status"
                    ) { typing ->
                        val subtitle = when {
                            typing -> "typing..."
                            contact?.activeNow == false -> contact.username.ifBlank { "Active 12m ago" }
                            else -> contact?.username?.takeIf { it.isNotBlank() } ?: "Active now"
                        }
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                lineHeight = 15.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            color = Color(0xFF8E8E93),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            IconButton(onClick = onCall, modifier = Modifier.size(44.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_phone),
                    contentDescription = "Call",
                    tint = Color.White,
                    modifier = Modifier.size(25.dp)
                )
            }
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onVideo, modifier = Modifier.size(44.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_video),
                    contentDescription = "Video call",
                    tint = Color.White,
                    modifier = Modifier.size(27.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyChat(contact: ContactEntity?, palette: ChatPalette) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Avatar(
            name = contact?.name.orEmpty(),
            path = contact?.avatarPath,
            size = 84.dp
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = contact?.name.orEmpty(),
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            ),
            color = palette.onBackground
        )
        if (!contact?.username.isNullOrBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = contact?.username.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = palette.onBackgroundMuted
            )
        }
    }
}

@Composable
private fun ChatComposer(
    contact: ContactEntity?,
    sender: Sender,
    draft: String,
    onDraftChange: (String) -> Unit,
    palette: ChatPalette,
    isPresentation: Boolean,
    onSend: () -> Unit,
    onGallery: () -> Unit,
    onCamera: () -> Unit,
    isRecording: Boolean,
    isRecordingLocked: Boolean,
    recordingElapsedMs: Long,
    recordingAmplitudes: List<Float>,
    onRecordStart: () -> Unit,
    onRecordLock: () -> Unit,
    onRecordEnd: () -> Unit,
    onCancelRecording: () -> Unit,
    onOpenTools: () -> Unit,
    onOpenEmoji: () -> Unit
) {
    val first = contact?.name?.substringBefore(" ").orEmpty().ifBlank { "friend" }
    val hasText = draft.isNotBlank()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
            .padding(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 8.dp)
    ) {
        AnimatedVisibility(
            visible = isRecording,
            enter = fadeIn(tween(120)) + expandVertically(),
            exit = fadeOut(tween(100)) + shrinkVertically()
        ) {
            RecordingPill(
                elapsedMs = recordingElapsedMs,
                amplitudes = recordingAmplitudes,
                palette = palette,
                locked = isRecordingLocked,
                onSend = onRecordEnd,
                onCancel = onCancelRecording
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp)
                .background(Color(0xFF262626), RoundedCornerShape(26.dp))
                .padding(start = 5.dp, end = 8.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(palette.cameraAccent)
                    .clickable(onClick = onCamera),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_camera),
                    contentDescription = "Take a photo",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            BasicTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 36.dp)
                    .padding(vertical = 6.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White,
                    fontSize = 15.5.sp
                ),
                cursorBrush = SolidColor(palette.cameraAccent),
                maxLines = 5,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (draft.isEmpty()) {
                            Text(
                                text = if (sender == Sender.ME) {
                                    "Message..."
                                } else {
                                    "Reply as $first..."
                                },
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.Normal
                                ),
                                color = Color(0xFF8E8E93)
                            )
                        }
                        innerTextField()
                    }
                }
            )
            if (hasText) {
                ChatSendButton(palette = palette, onClick = onSend)
            } else {
                MicButton(
                    palette = palette,
                    isRecording = isRecording,
                    onStart = onRecordStart,
                    onLock = onRecordLock,
                    onEnd = onRecordEnd,
                    onCancel = onCancelRecording
                )
                ComposerIcon(
                    icon = R.drawable.ic_gallery,
                    description = "Send a photo",
                    tint = Color.White,
                    onClick = onGallery
                )
                ComposerIcon(
                    icon = R.drawable.ic_sticker,
                    description = "Emoji",
                    tint = Color.White,
                    onClick = onOpenEmoji
                )
                if (!isPresentation) {
                    ComposerIcon(
                        icon = R.drawable.ic_plus_circle,
                        description = "Chat tools",
                        tint = Color.White,
                        onClick = onOpenTools
                    )
                }
            }
        }
    }
}

@Composable
private fun ComposerIcon(
    @androidx.annotation.DrawableRes icon: Int,
    description: String,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(25.dp)
        )
    }
}

@Composable
private fun ChatSendButton(palette: ChatPalette, onClick: () -> Unit) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, label = "sendScale")
    Box(
        modifier = Modifier
            .scale(scale)
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(palette.cameraAccent)
            .clickable(
                interactionSource = interactions,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_send),
            contentDescription = "Send",
            tint = Color.White,
            modifier = Modifier.size(19.dp)
        )
    }
}

@Composable
private fun MicButton(
    palette: ChatPalette,
    isRecording: Boolean,
    onStart: () -> Unit,
    onLock: () -> Unit,
    onEnd: () -> Unit,
    onCancel: () -> Unit
) {
    val density = LocalDensity.current
    val cancelThreshold = with(density) { 80.dp.toPx() }
    val lockThreshold = with(density) { 70.dp.toPx() }
    val scale by animateFloatAsState(if (isRecording) 0.9f else 1f, label = "micScale")
    Box(
        modifier = Modifier
            .scale(scale)
            .size(38.dp)
            .clip(CircleShape)
            .background(if (isRecording) palette.cameraAccent else Color.Transparent)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    onStart()
                    var drag = Offset.Zero
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        drag += change.positionChange()
                        change.consume()
                    }
                    when {
                        drag.x <= -cancelThreshold -> onCancel()
                        drag.y <= -lockThreshold -> onLock()
                        else -> onEnd()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_mic),
            contentDescription = "Hold to record a voice message. Slide left to cancel or up to lock.",
            tint = Color.White,
            modifier = Modifier.size(25.dp)
        )
    }
}

@Composable
private fun RecordingPill(
    elapsedMs: Long,
    amplitudes: List<Float>,
    palette: ChatPalette,
    locked: Boolean,
    onSend: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = palette.inputSurface,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val transition = rememberInfiniteTransition(label = "recording")
            val dotAlpha by transition.animateFloat(
                initialValue = 0.35f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
                label = "dot"
            )
            Box(
                Modifier
                    .size(9.dp)
                    .background(Color(0xFFFF4D67).copy(alpha = dotAlpha), CircleShape)
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = TimeText.formatDuration(elapsedMs),
                style = MaterialTheme.typography.labelMedium,
                color = palette.onBackground
            )
            Spacer(Modifier.width(10.dp))
            LiveWaveform(
                amplitudes = amplitudes,
                color = palette.sentEnd,
                modifier = Modifier
                    .weight(1f)
                    .height(26.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = if (locked) "Locked" else "← cancel · ↑ lock",
                style = MaterialTheme.typography.labelSmall,
                color = palette.onBackgroundMuted
            )
            if (locked) {
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(palette.cameraAccent)
                        .clickable(onClick = onSend),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_send),
                        contentDescription = "Send voice message",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            Spacer(Modifier.width(2.dp))
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onCancel),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = "Cancel recording",
                    tint = palette.onBackgroundMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun LiveWaveform(
    amplitudes: List<Float>,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val count = 32
        val gap = 2.5f
        val barWidth = ((size.width - gap * (count - 1)) / count).coerceAtLeast(1.5f)
        for (index in 0 until count) {
            val sourceIndex = (amplitudes.size - count + index).coerceAtLeast(0)
            val value = amplitudes.getOrNull(sourceIndex) ?: 0.12f
            val height = size.height * (0.18f + value * 0.82f)
            val top = (size.height - height) / 2f
            val left = index * (barWidth + gap)
            drawRoundRect(
                color = color.copy(alpha = 0.35f + value * 0.6f),
                topLeft = Offset(left, top),
                size = Size(barWidth, height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

@Composable
private fun ImageViewer(path: String, onDismiss: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var dragY by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val dragThreshold = with(density) { 140.dp.toPx() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val dialogView = LocalView.current
        val dialogWindowProvider = dialogView.parent as? DialogWindowProvider
        DisposableEffect(dialogWindowProvider) {
            val window = dialogWindowProvider?.window
            if (window != null) {
                window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.BLACK))
                val controller = WindowCompat.getInsetsController(window, dialogView)
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior =
                    androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
            onDispose { }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        var finished = false
                        while (!finished) {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.count { it.pressed }
                            if (pressed >= 2) {
                                val zoomChange = event.calculateZoom()
                                val panChange = event.calculatePan()
                                if (zoomChange != 1f) {
                                    scale = (scale * zoomChange).coerceIn(1f, 4f)
                                    if (scale == 1f) offset = Offset.Zero
                                }
                                if (scale > 1f) {
                                    offset += panChange
                                }
                                event.changes.forEach { it.consume() }
                            } else if (pressed == 1) {
                                val change = event.changes.first()
                                if (scale > 1f) {
                                    offset += change.positionChange()
                                    change.consume()
                                } else {
                                    val dy = change.positionChange().y
                                    if (dy != 0f) {
                                        dragY = (dragY + dy).coerceAtLeast(0f)
                                        change.consume()
                                    }
                                }
                            }
                            finished = event.changes.none { it.pressed }
                        }
                        if (dragY > dragThreshold) {
                            onDismiss()
                        } else {
                            dragY = 0f
                        }
                    }
                }
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(File(path))
                    .crossfade(true)
                    .build(),
                contentDescription = "Full screen photo. Pinch to zoom, swipe down to close.",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y + dragY
                        alpha = 1f - (dragY / (dragThreshold * 2.5f)).coerceIn(0f, 0.55f)
                    }
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun BackfillDialog(
    contact: ContactEntity?,
    onDismiss: () -> Unit,
    onAdd: (Sender, String, Long) -> Unit
) {
    var backfillSender by remember { mutableStateOf(Sender.ME) }
    var text by remember { mutableStateOf("") }
    var timestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = Color(0xFF262626),
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = {
            Text(
                "Backfill message",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BackfillSenderChip(
                        label = "You",
                        selected = backfillSender == Sender.ME,
                        onClick = { backfillSender = Sender.ME }
                    )
                    BackfillSenderChip(
                        label = contact?.name?.substringBefore(" ") ?: "Friend",
                        selected = backfillSender == Sender.FRIEND,
                        onClick = { backfillSender = Sender.FRIEND }
                    )
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= 1000) text = it },
                    placeholder = { Text("Message...", color = Color(0xFF8E8E93)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 5,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4A5DF9),
                        unfocusedBorderColor = Color(0xFF3A3A3C),
                        focusedContainerColor = Color(0xFF1C1C1E),
                        unfocusedContainerColor = Color(0xFF1C1C1E),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            showDateTimePicker(context, timestamp) { timestamp = it }
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_insta_clock),
                        contentDescription = null,
                        tint = Color(0xFF4A5DF9),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = TimeText.fullDateTime(context, timestamp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF4A5DF9)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(backfillSender, text.trim(), timestamp) },
                enabled = text.isNotBlank()
            ) {
                Text("Add", color = Color(0xFF4A5DF9), fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF8E8E93))
            }
        }
    )
}

@Composable
private fun BackfillSenderChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) Color(0xFF4A5DF9) else Color(0xFF1C1C1E)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color.White else Color(0xFF8E8E93),
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatToolsSheet(
    contact: ContactEntity?,
    sender: Sender,
    scriptsCount: Int,
    simulationActive: Boolean,
    onSenderChange: (Sender) -> Unit,
    onDismiss: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenTheme: () -> Unit,
    onOpenScripts: () -> Unit,
    onRunNextScript: () -> Unit,
    onAutoPlay: () -> Unit,
    onStopScript: () -> Unit,
    onBackfill: () -> Unit,
    onImportVoice: () -> Unit,
    onScreenRecord: () -> Unit,
    onClearChat: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1C1C1E),
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sending as",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF8E8E93),
                    modifier = Modifier.weight(1f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AsChip(
                        label = "You",
                        avatarPath = null,
                        name = "You",
                        selected = sender == Sender.ME,
                        onClick = {
                            onSenderChange(Sender.ME)
                            onDismiss()
                        }
                    )
                    AsChip(
                        label = contact?.name?.substringBefore(" ") ?: "Friend",
                        avatarPath = contact?.avatarPath,
                        name = contact?.name.orEmpty(),
                        selected = sender == Sender.FRIEND,
                        onClick = {
                            onSenderChange(Sender.FRIEND)
                            onDismiss()
                        }
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            HorizontalDivider(color = Color(0xFF262626), thickness = 0.5.dp)
            ToolAction(
                icon = R.drawable.ic_insta_sparkle,
                label = "Scripted replies",
                onClick = {
                    onDismiss()
                    onOpenScripts()
                }
            )
            ToolAction(
                icon = R.drawable.ic_reply,
                label = "Run next scripted reply",
                enabled = scriptsCount > 0 && !simulationActive,
                onClick = {
                    onDismiss()
                    onRunNextScript()
                }
            )
            ToolAction(
                icon = if (simulationActive) R.drawable.ic_insta_stop else R.drawable.ic_play,
                label = if (simulationActive) "Stop simulation" else "Auto-play queue",
                enabled = scriptsCount > 0 || simulationActive,
                onClick = {
                    onDismiss()
                    if (simulationActive) onStopScript() else onAutoPlay()
                }
            )
            ToolAction(
                icon = R.drawable.ic_insta_palette,
                label = "Theme",
                onClick = {
                    onDismiss()
                    onOpenTheme()
                }
            )
            ToolAction(
                icon = R.drawable.ic_insta_clock,
                label = "Backfill message",
                onClick = {
                    onDismiss()
                    onBackfill()
                }
            )
            ToolAction(
                icon = R.drawable.ic_insta_audio,
                label = "Import friend voice note",
                onClick = {
                    onDismiss()
                    onImportVoice()
                }
            )
            ToolAction(
                icon = R.drawable.ic_video,
                label = "Screen-record view",
                onClick = {
                    onDismiss()
                    onScreenRecord()
                }
            )
            ToolAction(
                icon = R.drawable.ic_insta_edit,
                label = "Profile details",
                onClick = {
                    onDismiss()
                    onEditProfile()
                }
            )
            HorizontalDivider(color = Color(0xFF262626), thickness = 0.5.dp)
            ToolAction(
                icon = R.drawable.ic_insta_trash,
                label = "Clear chat",
                destructive = true,
                onClick = {
                    onDismiss()
                    onClearChat()
                }
            )
        }
    }
}

@Composable
private fun AsChip(
    label: String,
    avatarPath: String?,
    name: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Color(0xFF4A5DF9) else Color(0xFF262626))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Avatar(name = name.ifBlank { "You" }, path = avatarPath, size = 18.dp)
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White
        )
    }
}

@Composable
private fun ToolAction(
    @androidx.annotation.DrawableRes icon: Int,
    label: String,
    enabled: Boolean = true,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = when {
                !enabled -> Color(0xFF5A5A5E)
                destructive -> Color(0xFFFF453A)
                else -> Color.White
            },
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.5.sp),
            color = when {
                !enabled -> Color(0xFF5A5A5E)
                destructive -> Color(0xFFFF453A)
                else -> Color.White
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmojiSheet(
    onDismiss: () -> Unit,
    onEmoji: (String) -> Unit
) {
    val emojis = listOf(
        "😀", "😄", "😁", "😂", "🥹", "😊", "😍", "🥰",
        "😘", "😎", "🤔", "😏", "😴", "😢", "😭", "😡",
        "🤯", "🥳", "😇", "🙃", "😉", "😌", "🤗", "🫶",
        "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍",
        "👍", "👎", "👏", "🙌", "🔥", "✨", "🎉", "🥂"
    )
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1C1C1E),
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
        ) {
            Text(
                text = "Stickers & emoji",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Spacer(Modifier.height(12.dp))
            emojis.chunked(8).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    row.forEach { emoji ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable { onEmoji(emoji) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 24.sp)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}
