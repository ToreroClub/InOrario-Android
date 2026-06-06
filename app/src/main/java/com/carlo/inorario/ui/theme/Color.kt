package com.carlo.inorario.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt

// Light Theme Palette
val LightPrimary = Color(0xFFFF9500)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFFFE0B2)
val LightOnPrimaryContainer = Color(0xFFE65100)
val LightSecondary = Color(0xFF007AFF)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightBackground = Color(0xFFF2F2F7)
val LightOnBackground = Color(0xFF1C1C1E)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF1C1C1E)
val LightSurfaceVariant = Color(0xFFE5E5EA)
val LightOnSurfaceVariant = Color(0xFF3A3A3C)

// Dark Theme Palette (Deep Glassmorphic Dark)
val DarkPrimary = Color(0xFFFF9500)
val DarkOnPrimary = Color(0xFF1C1C1E)
val DarkPrimaryContainer = Color(0xFFE65100)
val DarkOnPrimaryContainer = Color(0xFFFFE0B2)
val DarkSecondary = Color(0xFF0A84FF)
val DarkOnSecondary = Color(0xFFFFFFFF)
val DarkBackground = Color(0xFF000000)
val DarkOnBackground = Color(0xFFF2F2F7)
val DarkSurface = Color(0xFF1C1C1E)
val DarkOnSurface = Color(0xFFF2F2F7)
val DarkSurfaceVariant = Color(0xFF2C2C2E)
val DarkOnSurfaceVariant = Color(0xFFAEAEB2)

// Suburban line color parsing helper
fun getSuburbanColor(hex: String): Color {
    return try {
        Color(hex.toColorInt())
    } catch (_: Exception) {
        Color.Gray
    }
}