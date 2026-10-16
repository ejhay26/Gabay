package dev.gabay.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 01 — ANTI-SLOP TACTICAL DARK THEME FOR GABAY
 * Engineered strictly for direct tropical sunlight visibility and vehicular cockpit HUDs.
 * ZERO gradients, ZERO emojis, ZERO bubbly pastel pill shapes.
 */

object GabayColors {
    val BgObsidian = Color(0xFF0A0D10)
    val SurfacePanel = Color(0xFF12161C)
    val SurfaceInteractive = Color(0xFF1A2028)
    val BorderHairline = Color(0xFF262E38)
    val TextPrimary = Color(0xFFF0F4F8)
    val TextSecondary = Color(0xFF8A94A6)
    val AccentEmerald = Color(0xFF10B981)
    val AccentAmber = Color(0xFFF59E0B)
    val AccentCrimson = Color(0xFFEF4444)
}

object GabayShapes {
    val PanelRadius = RoundedCornerShape(4.dp)
    val CardRadius = RoundedCornerShape(6.dp)
    val ButtonRadius = RoundedCornerShape(4.dp)
    val HairlineBorderWidth = 1.dp
}

object GabayTypography {
    val SpeedometerReadout = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        color = GabayColors.TextPrimary
    )

    val PrimaryManeuverDistance = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        color = GabayColors.TextPrimary
    )

    val StreetName = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        color = GabayColors.TextPrimary
    )

    val HudMicroLabel = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        letterSpacing = 1.2.sp,
        color = GabayColors.TextSecondary
    )

    val TelemetryUnit = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        color = GabayColors.TextSecondary
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = GabayColors.AccentEmerald,
    onPrimary = GabayColors.BgObsidian,
    primaryContainer = GabayColors.SurfacePanel,
    onPrimaryContainer = GabayColors.TextPrimary,
    background = GabayColors.BgObsidian,
    onBackground = GabayColors.TextPrimary,
    surface = GabayColors.SurfacePanel,
    onSurface = GabayColors.TextPrimary,
    surfaceVariant = GabayColors.SurfaceInteractive,
    onSurfaceVariant = GabayColors.TextSecondary,
    outline = GabayColors.BorderHairline,
    error = GabayColors.AccentCrimson,
    onError = GabayColors.TextPrimary
)

@Composable
fun GabayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
