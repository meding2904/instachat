package com.instachat.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5F2FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Purple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE8FF),
    onSecondaryContainer = Color(0xFF1D1250),
    tertiary = Magenta,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE5FA),
    onTertiaryContainer = Color(0xFF4D0A47),
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = Color(0xFFF0F0F3),
    error = Color(0xFFD92D3C),
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF00325B),
    onPrimaryContainer = Color(0xFFD6E9FF),
    secondary = Color(0xFFD8C8FF),
    onSecondary = Color(0xFF2A1B5E),
    secondaryContainer = Color(0xFF3A2A70),
    onSecondaryContainer = Color(0xFFEDE5FF),
    tertiary = Color(0xFFFFA8EA),
    onTertiary = Color(0xFF53104B),
    tertiaryContainer = Color(0xFF6C1F61),
    onTertiaryContainer = Color(0xFFFFD7F6),
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = Color(0xFF232323),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

@Immutable
data class InstaColors(
    val brandGradient: List<Color>,
    val coralGradient: List<Color>,
    val ringGradient: List<Color>,
    val online: Color,
    val warning: Color,
    val notification: Color = NotificationRed,
    val sentText: Color = Color.White,
    val unreadDot: Color = Accent,
    val wallpaperDot: Color = Color.Black,
    val scrim: Color = Color(0x99000000)
)

val LocalInstaColors = staticCompositionLocalOf {
    InstaColors(
        brandGradient = BrandGradient,
        coralGradient = CoralGradient,
        ringGradient = StoryRingGradient,
        online = OnlineGreen,
        warning = WarningAmber
    )
}

val LocalAppDarkTheme = staticCompositionLocalOf { true }

@Composable
fun InstaChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val instaColors = InstaColors(
        brandGradient = if (darkTheme) {
            listOf(Magenta, Purple, Accent)
        } else {
            BrandGradient
        },
        coralGradient = CoralGradient,
        ringGradient = StoryRingGradient,
        online = OnlineGreen,
        warning = WarningAmber,
        unreadDot = Accent
    )
    CompositionLocalProvider(LocalInstaColors provides instaColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = InstaTypography,
            shapes = InstaShapes,
            content = content
        )
    }
}
