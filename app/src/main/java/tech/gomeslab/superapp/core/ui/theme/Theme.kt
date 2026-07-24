package tech.gomeslab.superapp.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// O app é dark-only por design (espelha o tema do stash). Ignoramos o modo claro do sistema.
private val DarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = OnAccent,
    primaryContainer = AccentHover,
    onPrimaryContainer = TextPrimary,
    secondary = Info,
    onSecondary = OnAccent,
    background = BgPrimary,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = BgTertiary,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = BgSecondary,
    surfaceContainerHigh = SurfaceHover,
    outline = BorderDefault,
    outlineVariant = BorderSubtle,
    error = Error,
    onError = OnAccent,
)

@Composable
fun SuperAppTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = DarkColors,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}
