package com.instachat.app.ui.script

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.instachat.app.R
import com.instachat.app.data.local.entity.ScriptEntity
import com.instachat.app.ui.components.AppSystemBars
import com.instachat.app.ui.components.Avatar
import com.instachat.app.ui.components.ConfirmDialog
import com.instachat.app.ui.settings.InstaSwitch
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ScriptEditorScreen(
    onBack: () -> Unit,
    viewModel: ScriptEditorViewModel = hiltViewModel()
) {
    AppSystemBars(darkTheme = true)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var editorTarget by remember { mutableStateOf<ScriptEditorTarget?>(null) }
    var deleteTarget by remember { mutableStateOf<ScriptEntity?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { snackbarHostState.showSnackbar(it) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_back),
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Scripted replies",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                    state.contact?.let {
                        Text(
                            text = it.name,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = Color(0xFF8E8E93)
                        )
                    }
                }
                IconButton(
                    onClick = { editorTarget = ScriptEditorTarget.New },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_plus),
                        contentDescription = "Add reply",
                        tint = Color(0xFF0095F6),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            HorizontalDivider(color = Color(0xFF1C1C1E), thickness = 0.5.dp)

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = Color(0xFF4A5DF9),
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(28.dp)
                    )
                }
            } else {
                val contact = state.contact
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding(),
                    contentPadding = PaddingValues(bottom = 36.dp)
                ) {
                    item {
                        contact?.let {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Avatar(name = it.name, path = it.avatarPath, size = 44.dp)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        it.name,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontSize = 15.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = Color.White
                                    )
                                    Text(
                                        "@${it.username}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                        color = Color(0xFF8E8E93)
                                    )
                                }
                            }
                            HorizontalDivider(color = Color(0xFF1C1C1E), thickness = 0.5.dp)
                        }
                    }

                    item {
                        SwitchRow(
                            title = "Auto-reply after my messages",
                            description = "Send the next queued reply automatically when you send a message",
                            checked = contact?.autoScriptEnabled == true,
                            onCheckedChange = viewModel::setAutoReply
                        )
                        SwitchRow(
                            title = "Typing indicator",
                            description = "Show animated typing dots before each scripted reply",
                            checked = contact?.typingSimulationEnabled == true,
                            onCheckedChange = viewModel::setTypingSimulation
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .background(Color(0xFF121212))
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Reply queue",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Color(0xFF8E8E93)
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = "Add reply",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Color(0xFF4A5DF9),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { editorTarget = ScriptEditorTarget.New }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (state.scripts.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "No replies queued",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color.White
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Tap Add reply to script what your friend says next.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF8E8E93)
                                )
                            }
                        }
                    } else {
                        itemsIndexed(
                            items = state.scripts,
                            key = { _, line -> line.id }
                        ) { index, line ->
                            ScriptLineRow(
                                index = index,
                                total = state.scripts.size,
                                line = line,
                                onEdit = { editorTarget = ScriptEditorTarget.Edit(line) },
                                onDelete = { deleteTarget = line },
                                onMoveUp = { viewModel.moveScript(line.id, -1) },
                                onMoveDown = { viewModel.moveScript(line.id, 1) }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 52.dp),
                                color = Color(0xFF1C1C1E),
                                thickness = 0.5.dp
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }

    when (val target = editorTarget) {
        is ScriptEditorTarget.New -> ScriptEditDialog(
            title = "New scripted reply",
            initialText = "",
            initialDelayMs = 400L,
            initialTypingMs = 1600L,
            onDismiss = { editorTarget = null },
            onSave = { text, delay, typing ->
                viewModel.addScript(text, delay, typing)
                editorTarget = null
            }
        )

        is ScriptEditorTarget.Edit -> ScriptEditDialog(
            title = "Edit scripted reply",
            initialText = target.script.text,
            initialDelayMs = target.script.delayMs,
            initialTypingMs = target.script.typingMs,
            onDismiss = { editorTarget = null },
            onSave = { text, delay, typing ->
                viewModel.updateScript(target.script.id, text, delay, typing)
                editorTarget = null
            }
        )

        null -> Unit
    }

    deleteTarget?.let { target ->
        ConfirmDialog(
            title = "Remove this reply?",
            message = "It will be removed from the reply queue.",
            confirmLabel = "Remove",
            destructive = true,
            onConfirm = {
                viewModel.deleteScript(target.id)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

private sealed interface ScriptEditorTarget {
    data object New : ScriptEditorTarget
    data class Edit(val script: ScriptEntity) : ScriptEditorTarget
}

@Composable
private fun SwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.5.sp),
                color = Color.White
            )
            Spacer(Modifier.height(1.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                color = Color(0xFF8E8E93)
            )
        }
        Spacer(Modifier.width(12.dp))
        InstaSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ScriptLineRow(
    index: Int,
    total: Int,
    line: ScriptEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Color(0xFF262626)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = Color(0xFF8E8E93)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = line.text,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Delay ${formatSeconds(line.delayMs)} · Typing ${formatSeconds(line.typingMs)}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = Color(0xFF8E8E93)
            )
        }
        IconButton(
            onClick = onMoveUp,
            enabled = index > 0,
            modifier = Modifier.size(34.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_insta_chevron_up),
                contentDescription = "Move up",
                tint = if (index > 0) Color(0xFF8E8E93) else Color(0xFF3A3A3C),
                modifier = Modifier.size(18.dp)
            )
        }
        IconButton(
            onClick = onMoveDown,
            enabled = index < total - 1,
            modifier = Modifier.size(34.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_down),
                contentDescription = "Move down",
                tint = if (index < total - 1) Color(0xFF8E8E93) else Color(0xFF3A3A3C),
                modifier = Modifier.size(18.dp)
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_insta_trash),
                contentDescription = "Delete reply",
                tint = Color(0xFF8E8E93),
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

@Composable
private fun ScriptEditDialog(
    title: String,
    initialText: String,
    initialDelayMs: Long,
    initialTypingMs: Long,
    onDismiss: () -> Unit,
    onSave: (String, Long, Long) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    var delaySeconds by remember {
        mutableStateOf((initialDelayMs / 1000f).coerceIn(0.2f, 5f))
    }
    var typingSeconds by remember {
        mutableStateOf((initialTypingMs / 1000f).coerceIn(0.5f, 10f))
    }
    val delayMs = (delaySeconds * 1000f).roundToInt().toLong()
    val typingMs = (typingSeconds * 1000f).roundToInt().toLong()
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = Color(0xFF262626),
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= 500) text = it },
                    placeholder = { Text("Reply text...", color = Color(0xFF8E8E93)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4A5DF9),
                        unfocusedBorderColor = Color(0xFF3A3A3C),
                        focusedContainerColor = Color(0xFF1C1C1E),
                        unfocusedContainerColor = Color(0xFF1C1C1E),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Reply delay",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8E8E93)
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = formatSeconds(delayMs),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                }
                Slider(
                    value = delaySeconds,
                    onValueChange = { delaySeconds = it },
                    valueRange = 0.2f..5f,
                    steps = 23,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color(0xFF4A5DF9),
                        inactiveTrackColor = Color(0xFF3A3A3C)
                    )
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Typing duration",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8E8E93)
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = formatSeconds(typingMs),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                }
                Slider(
                    value = typingSeconds,
                    onValueChange = { typingSeconds = it },
                    valueRange = 0.5f..10f,
                    steps = 18,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color(0xFF4A5DF9),
                        inactiveTrackColor = Color(0xFF3A3A3C)
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(text.trim(), delayMs, typingMs) },
                enabled = text.isNotBlank()
            ) {
                Text("Save", color = Color(0xFF4A5DF9), fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF8E8E93))
            }
        }
    )
}

private fun formatSeconds(delayMs: Long): String =
    String.format(Locale.US, "%.1fs", delayMs / 1000f)
