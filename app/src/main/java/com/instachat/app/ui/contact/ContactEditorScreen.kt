package com.instachat.app.ui.contact

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.instachat.app.R
import com.instachat.app.data.media.PendingCapture
import com.instachat.app.ui.components.AppSystemBars
import com.instachat.app.ui.components.Avatar
import com.instachat.app.ui.components.ConfirmDialog
import com.instachat.app.ui.components.PermissionDialog
import com.instachat.app.ui.settings.InstaSwitch
import com.instachat.app.util.hasPermission

@Composable
fun ContactEditorScreen(
    onBack: () -> Unit,
    onOpenTheme: (Long) -> Unit,
    onFinished: (Long, Boolean) -> Unit,
    onDeleted: () -> Unit,
    viewModel: ContactEditorViewModel = hiltViewModel()
) {
    AppSystemBars(darkTheme = true)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showCameraPermissionDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var pendingCapture by remember { mutableStateOf<PendingCapture?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.importAvatar(uri)
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val capture = pendingCapture
        if (capture != null) {
            if (success) viewModel.importAvatarFromCapture(capture)
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
        if (context.hasPermission(Manifest.permission.CAMERA)) launchCamera()
        else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    fun openGallery() {
        galleryLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshTheme()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ContactEditorEvent.Saved -> onFinished(event.contactId, event.isNew)
                ContactEditorEvent.Deleted -> onDeleted()
                is ContactEditorEvent.Message -> snackbarHostState.showSnackbar(event.text)
            }
        }
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
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (state.isNew) "New friend" else "Edit profile",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                if (!state.isLoading) {
                    IconButton(
                        onClick = viewModel::save,
                        enabled = !state.saving,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = "Save",
                            tint = Color(0xFF0095F6),
                            modifier = Modifier.size(26.dp)
                        )
                    }
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
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding(),
                    contentPadding = PaddingValues(top = 20.dp, bottom = 40.dp)
                ) {
                    item {
                        AvatarEditor(
                            name = state.name,
                            avatarPath = state.avatarPath,
                            isImporting = state.importingAvatar,
                            onGallery = ::openGallery,
                            onCamera = ::requestCamera,
                            onRemove = viewModel::removeAvatar
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            ProfileField(
                                label = "Name",
                                value = state.name,
                                onValueChange = viewModel::onNameChange,
                                placeholder = "Ava Mitchell",
                                error = state.nameError,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    keyboardType = KeyboardType.Text
                                )
                            )
                            Spacer(Modifier.height(14.dp))
                            ProfileField(
                                label = "Username",
                                value = state.username,
                                onValueChange = viewModel::onUsernameChange,
                                placeholder = "ava.mitchell",
                                error = state.usernameError,
                                prefix = "@",
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.None,
                                    keyboardType = KeyboardType.Text
                                )
                            )
                            Spacer(Modifier.height(14.dp))
                            ProfileField(
                                label = "Note / bio",
                                value = state.bio,
                                onValueChange = viewModel::onBioChange,
                                placeholder = "Film cameras, oat lattes & playlists",
                                singleLine = false,
                                minLines = 2,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences,
                                    keyboardType = KeyboardType.Text
                                )
                            )
                        }
                    }

                    item {
                        Spacer(Modifier.height(22.dp))
                        HorizontalDivider(color = Color(0xFF1C1C1E), thickness = 0.5.dp)
                        ActiveNowRow(
                            checked = state.activeNow,
                            onCheckedChange = viewModel::setActiveNow
                        )
                        HorizontalDivider(color = Color(0xFF1C1C1E), thickness = 0.5.dp)
                        if (!state.isNew) {
                            ThemeRow(
                                themeName = state.themeName,
                                onClick = { onOpenTheme(state.contactId) }
                            )
                            HorizontalDivider(color = Color(0xFF1C1C1E), thickness = 0.5.dp)
                        }
                    }

                    if (!state.isNew) {
                        item {
                            Spacer(Modifier.height(16.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showDeleteDialog = true }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Delete contact",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.5.sp),
                                    color = Color(0xFFFF453A)
                                )
                            }
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

    if (showDeleteDialog) {
        ConfirmDialog(
            title = "Delete ${state.name}?",
            message = "This removes the profile and every message in the conversation.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                showDeleteDialog = false
                viewModel.delete()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    if (showCameraPermissionDialog) {
        PermissionDialog(
            title = "Camera access needed",
            message = "Allow camera access to take a profile photo, or choose one from your gallery.",
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
}

@Composable
private fun AvatarEditor(
    name: String,
    avatarPath: String?,
    isImporting: Boolean,
    onGallery: () -> Unit,
    onCamera: () -> Unit,
    onRemove: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(92.dp)
                .clip(CircleShape)
                .clickable(onClick = onGallery),
            contentAlignment = Alignment.Center
        ) {
            Avatar(
                name = name.ifBlank { "Friend" },
                path = avatarPath,
                size = 92.dp
            )
            if (isImporting) {
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Edit picture",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color(0xFF4A5DF9),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onGallery)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
            Text(
                text = "Camera",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color(0xFF4A5DF9),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onCamera)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
            if (avatarPath != null) {
                Text(
                    text = "Remove",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = Color(0xFF8E8E93),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onRemove)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ProfileField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    error: String? = null,
    prefix: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp),
            color = Color(0xFF8E8E93),
            modifier = Modifier.padding(start = 2.dp, bottom = 5.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 15.5.sp,
                color = Color.White
            ),
            placeholder = {
                Text(
                    placeholder,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.5.sp),
                    color = Color(0xFF8E8E93).copy(alpha = 0.6f)
                )
            },
            prefix = prefix?.let {
                {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.5.sp),
                        color = Color(0xFF8E8E93)
                    )
                }
            },
            singleLine = singleLine,
            minLines = minLines,
            isError = error != null,
            keyboardOptions = keyboardOptions,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF4A5DF9),
                unfocusedBorderColor = Color(0xFF262626),
                focusedContainerColor = Color(0xFF121212),
                unfocusedContainerColor = Color(0xFF121212),
                cursorColor = Color(0xFF4A5DF9),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFFF453A),
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }
}

@Composable
private fun ActiveNowRow(
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
                "Active status",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.5.sp),
                color = Color.White
            )
            Text(
                "Show as active in chat header",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                color = Color(0xFF8E8E93)
            )
        }
        InstaSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ThemeRow(themeName: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Theme",
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.5.sp),
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = themeName,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.5.sp),
            color = Color(0xFF8E8E93)
        )
        Spacer(Modifier.width(8.dp))
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = Color(0xFF8E8E93),
            modifier = Modifier.size(18.dp)
        )
    }
}
