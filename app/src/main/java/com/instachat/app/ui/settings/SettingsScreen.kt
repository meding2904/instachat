package com.instachat.app.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.instachat.app.BuildConfig
import com.instachat.app.R
import com.instachat.app.data.prefs.ThemeMode
import com.instachat.app.data.repository.SeedThemes
import com.instachat.app.ui.AppViewModel
import com.instachat.app.ui.components.AppSystemBars
import com.instachat.app.ui.components.ConfirmDialog
import com.instachat.app.util.hasPermission

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: AppViewModel = hiltViewModel()
) {
    AppSystemBars(darkTheme = true)
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val resetting by viewModel.resetting.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showThemeSheet by remember { mutableStateOf(false) }
    var showChatThemeSheet by remember { mutableStateOf(false) }
    var showAboutSheet by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var refreshTick by remember { mutableIntStateOf(0) }

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refreshTick++ }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refreshTick++ }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refreshTick++ }

    LaunchedEffect(Unit) {
        viewModel.events.collect { snackbarHostState.showSnackbar(it) }
    }

    val defaultTheme = SeedThemes.all.firstOrNull { it.id == settings.defaultChatThemeId }
        ?: SeedThemes.all.first()
    val themeLabel = when (settings.themeMode) {
        ThemeMode.SYSTEM -> "System default"
        ThemeMode.LIGHT -> "Light"
        ThemeMode.DARK -> "Dark"
    }

    @Suppress("UNUSED_EXPRESSION")
    refreshTick
    val micGranted = context.hasPermission(Manifest.permission.RECORD_AUDIO)
    val cameraGranted = context.hasPermission(Manifest.permission.CAMERA)

    Scaffold(
        containerColor = Color.Black,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = {
            SnackbarHost(
                snackbarHostState,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
                .statusBarsPadding()
        ) {
            SettingsTopBar(onBack = onBack)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 36.dp)
            ) {
                item {
                    SectionHeader("How you use InstaChat")
                    FlatNavRow(
                        iconRes = R.drawable.ic_insta_moon,
                        title = "Dark mode",
                        value = themeLabel,
                        onClick = { showThemeSheet = true }
                    )
                    FlatNavRow(
                        iconRes = R.drawable.ic_insta_palette,
                        title = "Default chat theme",
                        value = defaultTheme.name,
                        swatch = Brush.linearGradient(
                            listOf(
                                Color(defaultTheme.sentColorStart),
                                Color(defaultTheme.sentColorEnd)
                            )
                        ),
                        onClick = { showChatThemeSheet = true }
                    )
                    SectionDivider()
                }

                item {
                    SectionHeader("Messages and simulation")
                    FlatToggleRow(
                        iconRes = R.drawable.ic_check,
                        title = "Read receipts",
                        subtitle = "Show Seen status on sent messages",
                        checked = settings.readReceipts,
                        onCheckedChange = viewModel::setReadReceipts
                    )
                    FlatInfoRow(
                        iconRes = R.drawable.ic_video,
                        title = "Screen-record view",
                        subtitle = "Available inside any chat via the + menu"
                    )
                    SectionDivider()
                }

                item {
                    SectionHeader("Device permissions")
                    FlatNavRow(
                        iconRes = R.drawable.ic_mic,
                        title = "Microphone",
                        value = if (micGranted) "Allowed" else "Not allowed",
                        onClick = {
                            if (!micGranted) {
                                micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else {
                                openSystemAppSettings(context)
                            }
                        }
                    )
                    FlatNavRow(
                        iconRes = R.drawable.ic_camera,
                        title = "Camera",
                        value = if (cameraGranted) "Allowed" else "Not allowed",
                        onClick = {
                            if (!cameraGranted) {
                                cameraLauncher.launch(Manifest.permission.CAMERA)
                            } else {
                                openSystemAppSettings(context)
                            }
                        }
                    )
                    FlatNavRow(
                        iconRes = R.drawable.ic_gallery,
                        title = "Photos and videos",
                        value = "System picker",
                        onClick = { openSystemAppSettings(context) }
                    )
                    SectionDivider()
                }

                item {
                    SectionHeader("Your app and media")
                    FlatActionRow(
                        iconRes = R.drawable.ic_insta_refresh,
                        title = "Restore demo conversations",
                        subtitle = "Reset contacts and messages to default seed data",
                        loading = resetting,
                        onClick = { confirmReset = true }
                    )
                    SectionDivider()
                }

                item {
                    SectionHeader("More info and support")
                    FlatNavRow(
                        iconRes = R.drawable.ic_info,
                        title = "About",
                        value = "v${BuildConfig.VERSION_NAME}",
                        onClick = { showAboutSheet = true }
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Simulated chat",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = Color(0xFF8E8E93)
                        )
                        Text(
                            text = "com.instachat.app · v${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = Color(0xFF8E8E93)
                        )
                    }
                }
            }
        }
    }

    if (showThemeSheet) {
        ThemeModeSheet(
            selected = settings.themeMode,
            onSelect = {
                viewModel.setThemeMode(it)
                showThemeSheet = false
            },
            onDismiss = { showThemeSheet = false }
        )
    }

    if (showChatThemeSheet) {
        DefaultChatThemeSheet(
            selected = settings.defaultChatThemeId,
            onSelect = {
                viewModel.setDefaultChatTheme(it)
                showChatThemeSheet = false
            },
            onDismiss = { showChatThemeSheet = false }
        )
    }

    if (showAboutSheet) {
        AboutSheet(onDismiss = { showAboutSheet = false })
    }

    if (confirmReset) {
        ConfirmDialog(
            title = "Restore demo conversations?",
            message = "This replaces current contacts and messages with the default demo conversations.",
            confirmLabel = "Restore",
            destructive = true,
            onConfirm = {
                confirmReset = false
                viewModel.resetDemoData()
            },
            onDismiss = { confirmReset = false }
        )
    }
}

private fun openSystemAppSettings(context: android.content.Context) {
    context.startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}")
        )
    )
}

@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
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
            Text(
                text = "Settings and activity",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
        }
        HorizontalDivider(color = Color(0xFF1C1C1E), thickness = 0.5.dp)
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        ),
        color = Color(0xFF8E8E93),
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 6.dp)
    )
}

@Composable
private fun SectionDivider() {
    Spacer(Modifier.height(6.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .background(Color(0xFF121212))
    )
}

@Composable
private fun FlatNavRow(
    @androidx.annotation.DrawableRes iconRes: Int,
    title: String,
    value: String,
    swatch: Brush? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Normal
            ),
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        if (swatch != null) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(swatch)
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Normal
            ),
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

@Composable
private fun FlatToggleRow(
    @androidx.annotation.DrawableRes iconRes: Int,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = Color.White
            )
            if (subtitle != null) {
                Spacer(Modifier.height(1.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = Color(0xFF8E8E93)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        InstaSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun FlatInfoRow(
    @androidx.annotation.DrawableRes iconRes: Int,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = Color.White
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = Color(0xFF8E8E93)
            )
        }
    }
}

@Composable
private fun FlatActionRow(
    @androidx.annotation.DrawableRes iconRes: Int,
    title: String,
    subtitle: String,
    loading: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .clickable(enabled = !loading, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = Color.White
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = Color(0xFF8E8E93)
            )
        }
        Spacer(Modifier.width(12.dp))
        if (loading) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                color = Color(0xFF4A5DF9),
                modifier = Modifier.size(18.dp)
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = Color(0xFF8E8E93),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun InstaSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) Color(0xFF4A5DF9) else Color(0xFF3A3A3C),
        animationSpec = tween(160),
        label = "switchTrack"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 2.dp,
        animationSpec = tween(160),
        label = "switchThumb"
    )
    Box(
        modifier = modifier
            .width(44.dp)
            .height(26.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(trackColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(22.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeModeSheet(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        ThemeMode.DARK to "On",
        ThemeMode.LIGHT to "Off",
        ThemeMode.SYSTEM to "Use system settings"
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
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
        ) {
            Text(
                text = "Dark mode",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
            )
            HorizontalDivider(color = Color(0xFF262626), thickness = 0.5.dp)
            options.forEach { (mode, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(mode) }
                        .padding(horizontal = 20.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.5.sp),
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    RadioDot(selected = selected == mode)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DefaultChatThemeSheet(
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
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
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
        ) {
            Text(
                text = "Default chat theme",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
            )
            HorizontalDivider(color = Color(0xFF262626), thickness = 0.5.dp)
            SeedThemes.all.forEach { theme ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(theme.id) }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(theme.sentColorStart),
                                        Color(theme.sentColorEnd)
                                    )
                                )
                            )
                    )
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text = theme.name,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.5.sp),
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    RadioDot(selected = selected == theme.id)
                }
            }
        }
    }
}

@Composable
private fun RadioDot(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(if (selected) Color(0xFF4A5DF9) else Color.Transparent)
            .then(
                if (!selected) {
                    Modifier.background(Color(0xFF3A3A3C), CircleShape)
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1C1C1E))
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutSheet(onDismiss: () -> Unit) {
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
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "About InstaChat",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Version ${BuildConfig.VERSION_NAME} · Simulated chat",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8E8E93)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "InstaChat is a local-only simulated messaging app for scripted conversations. " +
                    "All contacts, media, and messages stay on this device.",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp),
                color = Color(0xFFD1D1D6)
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}
