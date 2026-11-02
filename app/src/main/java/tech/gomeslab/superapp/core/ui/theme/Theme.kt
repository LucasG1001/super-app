package tech.gomeslab.superapp.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = Primary,
    onPrimaryContainer = OnPrimary,
    secondary = ControlActive,
    onSecondary = OnPrimary,
    background = LightBackground,
    onBackground = LightText,
    surface = LightCard,
    onSurface = LightText,
    surfaceVariant = LightCardHigh,
    onSurfaceVariant = LightTextSecondary,
    surfaceContainer = LightCard,
    surfaceContainerHigh = LightCard,
    surfaceContainerHighest = LightCardHigh,
    outline = LightSwitchTrackOff,
    outlineVariant = LightDivider,
    error = Error,
    onError = OnError,
)

private val DarkColors = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = Primary,
    onPrimaryContainer = OnPrimary,
    secondary = ControlActive,
    onSecondary = OnPrimary,
    background = DarkBackground,
    onBackground = DarkText,
    surface = DarkCard,
    onSurface = DarkText,
    surfaceVariant = DarkCardHigh,
    onSurfaceVariant = DarkTextSecondary,
    surfaceContainer = DarkCard,
    surfaceContainerHigh = DarkCard,
    surfaceContainerHighest = DarkCardHigh,
    outline = DarkSwitchTrackOff,
    outlineVariant = DarkDivider,
    error = Error,
    onError = OnError,
)

@Composable
fun SuperAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}
