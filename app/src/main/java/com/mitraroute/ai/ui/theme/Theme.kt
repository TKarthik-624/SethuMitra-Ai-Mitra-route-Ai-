package com.mitraroute.ai.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController

private val DarkColorScheme = darkColorScheme(
    primary = Emerald500,
    onPrimary = Slate950,
    primaryContainer = Emerald600,
    onPrimaryContainer = Slate100,
    secondary = Amber500,
    onSecondary = Slate950,
    secondaryContainer = Amber600,
    onSecondaryContainer = Slate100,
    tertiary = Slate500,
    onTertiary = Slate100,
    background = Slate950,
    onBackground = Slate100,
    surface = Slate900,
    onSurface = Slate100,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate400,
    outline = Slate700,
    error = Rose500,
    onError = Slate100,
)

private val LightColorScheme = lightColorScheme(
    primary = Emerald600,
    onPrimary = Color.White,
    primaryContainer = Emerald500,
    onPrimaryContainer = Color.White,
    secondary = Amber600,
    onSecondary = Color.White,
    background = Color.White,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    outline = Slate300
)

@Composable
fun MitraRouteTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val systemUiController = rememberSystemUiController()

    SideEffect {
        systemUiController.setSystemBarsColor(
            color = if (darkTheme) Slate950 else Color.White,
            darkIcons = !darkTheme
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
