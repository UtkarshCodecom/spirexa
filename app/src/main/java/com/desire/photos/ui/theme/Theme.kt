package com.desire.photos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
fun PhotosTheme(
    content: @Composable () -> Unit,
) {
    val p = Neo.current
    val colorScheme = if (p.isDark) {
        darkColorScheme(
            primary = p.primary, onPrimary = p.onPrimary,
            background = p.bg, onBackground = p.text,
            surface = p.surface, onSurface = p.text,
            surfaceVariant = p.bg, onSurfaceVariant = p.muted,
            error = p.danger,
            secondaryContainer = p.bg, onSecondaryContainer = p.text,
        )
    } else {
        lightColorScheme(
            primary = p.primary, onPrimary = p.onPrimary,
            background = p.bg, onBackground = p.text,
            surface = p.surface, onSurface = p.text,
            surfaceVariant = p.bg, onSurfaceVariant = p.muted,
            error = p.danger,
            secondaryContainer = p.bg, onSecondaryContainer = p.text,
        )
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
