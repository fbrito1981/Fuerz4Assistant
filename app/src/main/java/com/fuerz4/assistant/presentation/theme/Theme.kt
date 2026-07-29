package com.fuerz4.assistant.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = NaranjaOscuro,
    onPrimary = Blanco,
    primaryContainer = NaranjaOscuroContainer,
    onPrimaryContainer = Blanco,
    background = GrisOscuro,
    onBackground = Blanco,
    surface = GrisOscuro,
    onSurface = Blanco,
    surfaceVariant = GrisOscuroVariant,
    onSurfaceVariant = Blanco
)

private val LightColors = lightColorScheme(
    primary = NaranjaOscuro,
    onPrimary = Blanco,
    primaryContainer = NaranjaOscuroContainer,
    onPrimaryContainer = Blanco,
    background = Blanco,
    onBackground = GrisOscuro,
    surface = Blanco,
    onSurface = GrisOscuro,
    surfaceVariant = Blanco,
    onSurfaceVariant = GrisOscuro
)

@Composable
fun Fuerz4Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Fuerz4Typography,
        content = content
    )
}
