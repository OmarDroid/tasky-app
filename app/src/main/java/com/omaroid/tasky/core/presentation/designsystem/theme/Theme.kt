package com.omaroid.tasky.core.presentation.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class ExtendedColors(
    val needle: Color,
    val taskyGreen: Color,
    val taskyLime: Color,
    val taskyError: Color,
    val headerBackground: Color,
    val headerOnBackground: Color,
)

val LocalExtendedColors = staticCompositionLocalOf<ExtendedColors> {
    error("No ExtendedColors provided")
}

val extendedColors: ExtendedColors
    @Composable @ReadOnlyComposable get() = LocalExtendedColors.current

private val DarkColorScheme = darkColorScheme(
    primary = TaskyGreen,
    onPrimary = TaskyDarkOnSurface,
    secondary = TaskyLime,
    onSecondary = TaskyDarkBackground,
    tertiary = TaskyInfo,
    background = TaskyDarkBackground,
    onBackground = TaskyDarkOnSurface,
    surface = TaskyDarkSurface,
    onSurface = TaskyDarkOnSurface,
    surfaceVariant = TaskyDarkSurfaceVariant,
    onSurfaceVariant = TaskyDarkOnSurfaceVariant,
    outlineVariant = TaskyDarkOutlineVariant,
    error = TaskyError,
    errorContainer = TaskyErrorContainer,
)

private val LightColorScheme = lightColorScheme(
    primary = TaskyGreen,
    onPrimary = TaskyDarkOnSurface,
    secondary = TaskyLime,
    onSecondary = TaskyDarkBackground,
    tertiary = TaskyInfo,
    background = TaskyLightBackground,
    onBackground = TaskyLightOnSurface,
    surface = TaskyLightSurface,
    onSurface = TaskyLightOnSurface,
    surfaceVariant = TaskyLightSurfaceVariant,
    onSurfaceVariant = TaskyLightOnSurfaceVariant,
    outlineVariant = TaskyLightOutlineVariant,
    error = TaskyError,
    errorContainer = TaskyErrorContainer,
)

@Composable
fun TaskyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = ExtendedColors(
        needle = if (darkTheme) TaskyLime else TaskyGreen,
        taskyGreen = TaskyGreen,
        taskyLime = TaskyLime,
        taskyError = TaskyError,
        headerBackground = TaskyDarkBackground,
        headerOnBackground = TaskyDarkOnSurface,
    )

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}
