package com.instachat.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File

private val avatarPalettes = listOf(
    listOf(Color(0xFFFFB199), Color(0xFFFF6A88)),
    listOf(Color(0xFF9AD9FF), Color(0xFF5D7CFF)),
    listOf(Color(0xFFA6E3C4), Color(0xFF2FBF8F)),
    listOf(Color(0xFFD7B7FF), Color(0xFF8B5CF6)),
    listOf(Color(0xFFFFD59E), Color(0xFFFF8A59)),
    listOf(Color(0xFFB8C4FF), Color(0xFF6C5CE7))
)

fun initialsFor(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (parts.isEmpty()) return "?"
    return if (parts.size == 1) {
        parts.first().take(1).uppercase()
    } else {
        (parts.first().take(1) + parts.last().take(1)).uppercase()
    }
}

@Composable
fun Avatar(
    name: String,
    path: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    borderColor: Color? = null
) {
    val palette = remember(name) {
        avatarPalettes[((name.hashCode() * 31) and Int.MAX_VALUE) % avatarPalettes.size]
    }
    val context = LocalContext.current
    val outline = borderColor
    Box(
        modifier = modifier
            .size(size)
            .then(
                if (outline != null) {
                    Modifier.border(width = 2.dp, color = outline, shape = CircleShape)
                } else Modifier
            )
            .clip(CircleShape)
            .background(Brush.linearGradient(palette)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initialsFor(name),
            color = Color.White.copy(alpha = 0.94f),
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.34f).sp
        )
        if (!path.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(File(path))
                    .crossfade(true)
                    .build(),
                contentDescription = "$name's profile photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun OnlineDot(
    modifier: Modifier = Modifier,
    size: Dp = 10.dp,
    borderColor: Color = MaterialTheme.colorScheme.background
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF2FBF71))
            .border(2.dp, borderColor, CircleShape)
    )
}
