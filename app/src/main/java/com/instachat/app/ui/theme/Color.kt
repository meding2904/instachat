package com.instachat.app.ui.theme

import androidx.compose.ui.graphics.Color

// Dark DM reference palette
val Accent = Color(0xFF0A84FF)
val AccentDark = Color(0xFF4FA3FF)
val Magenta = Color(0xFFC42BC1)
val Purple = Color(0xFF6C4DFF)
val SentGradientStart = Magenta
val SentGradientEnd = Purple
val OnlineGreen = Color(0xFF2ECC71)
val WarningAmber = Color(0xFFF2A93B)
val RingOrange = Color(0xFFFF9F43)
val RingPink = Color(0xFFFF4D8D)
val NotificationRed = Color(0xFFFF3040)

val LightBackground = Color(0xFFFFFFFF)
val LightSurface = Color(0xFFF5F5F7)
val LightSurfaceVariant = Color(0xFFEDEDF0)
val LightOnSurface = Color(0xFF111114)
val LightOnSurfaceVariant = Color(0xFF6E6E78)
val LightOutline = Color(0xFFE3E3E8)

val DarkBackground = Color(0xFF000000)
val DarkSurface = Color(0xFF1C1C1E)
val DarkSurfaceVariant = Color(0xFF262626)
val DarkOnSurface = Color(0xFFFFFFFF)
val DarkOnSurfaceVariant = Color(0xFF8E8E93)
val DarkOutline = Color(0xFF2A2A2A)

// Backwards-compatible aliases used by existing components.
val BrandPurple = Accent
val BrandViolet = Magenta
val BrandBlue = Accent
val BrandCoral = RingPink

val BrandGradient = listOf(Magenta, Purple, Color(0xFF2F8CFF))
val CoralGradient = listOf(Color(0xFFFF9F43), RingPink)
val StoryRingGradient = listOf(RingOrange, RingPink, Magenta, Purple)
