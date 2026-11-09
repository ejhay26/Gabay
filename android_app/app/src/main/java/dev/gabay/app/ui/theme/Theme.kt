package dev.gabay.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = TerracottaAccent,
    onPrimary = CreamBackground,
    primaryContainer = CreamCard,
    onPrimaryContainer = CharcoalText,
    secondary = SagePark,
    background = CreamBackground,
    surface = CreamSurface,
    onBackground = CharcoalText,
    onSurface = CharcoalText
)

private val DarkColorScheme = darkColorScheme(
    primary = TerracottaLight,
    onPrimary = DarkBackground,
    primaryContainer = DarkCard,
    onPrimaryContainer = DarkText,
    secondary = SagePark,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = DarkText,
    onSurface = DarkText
)

@Composable
fun GabayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
