package com.instachat.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.instachat.app.data.local.entity.ThemeEntity
import com.instachat.app.ui.theme.Magenta
import com.instachat.app.ui.theme.Purple

@Immutable
data class ChatPalette(
    val background: Color,
    val sentStart: Color,
    val sentEnd: Color,
    val received: Color,
    val onSent: Color,
    val onReceived: Color,
    val onBackground: Color,
    val onBackgroundMuted: Color,
    val inputSurface: Color,
    val inputBorder: Color,
    val topBarSurface: Color,
    val cameraAccent: Color,
    val isDark: Boolean
)

@Composable
fun rememberChatPalette(theme: ThemeEntity): ChatPalette = remember(theme) {
    ChatPalette(
        background = Color.Black,
        sentStart = Color(theme.sentColorStart).takeIf { it != Color.Unspecified } ?: Magenta,
        sentEnd = Color(theme.sentColorEnd).takeIf { it != Color.Unspecified } ?: Purple,
        received = Color(0xFF262626),
        onSent = Color.White,
        onReceived = Color.White,
        onBackground = Color.White,
        onBackgroundMuted = Color(0xFF8E8E93),
        inputSurface = Color(0xFF262626),
        inputBorder = Color(0xFF262626),
        topBarSurface = Color.Black,
        cameraAccent = Color(0xFF4A5DF9),
        isDark = true
    )
}

@Composable
fun ChatBackdrop(palette: ChatPalette, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
    )
}
