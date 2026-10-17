package dev.gabay.app.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
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
    // Map Canvas Palette (Exact match to reference photo)
    val MapCanvasBg = Color(0xFF202328)      // Deep matte charcoal land
    val MapWater = Color(0xFF131519)         // Dark liquid slate water
    val MapParks = Color(0xFF1B231F)         // Subtle dark tone-on-tone park green
    val MapRoadMinor = Color(0xFF2D323C)     // Subtle graphite minor streets
    val MapRoadSecondary = Color(0xFF424A58) // Distinct secondary roads
    val MapRoadPrimary = Color(0xFF565F70)   // Prominent highways & expressways
    val RouteMint = Color(0xFF00F5D4)        // Vibrant neon mint / cyan route glow
    
    // UI Chrome (Apple-style frosted obsidian glass)
    val BgObsidian = Color(0xFF121418)
    val UiCard = Color(0xFF181A1F)
    val UiCardGlass = Color(0xEE181A1F)
    val UiCardElevated = Color(0xFF22262E)
    val UiInput = Color(0xFF232730)
    val UiBorder = Color(0xFF2C313B)
    val UiBorderActive = Color(0xFF00F5D4)
    val UiBorderLight = Color(0xFF383F4C)
    
    // Typography Colors
    val TextPrimary = Color(0xFFF1F5F9)
    val TextSecondary = Color(0xFF94A3B8)
    val TextMuted = Color(0xFF64748B)
    
    // Accents & Signals
    val AccentEmerald = Color(0xFF10B981)
    val AccentMint = Color(0xFF00F5D4)
    val AccentCyan = Color(0xFF06B6D4)
    val AccentAmber = Color(0xFFF59E0B)
    val AccentRed = Color(0xFFEF4444)
    val AccentCrimson = Color(0xFFEF4444)

    // Legacy Component Aliases
    val BgCard = UiCard
    val BgElevated = UiCardElevated
    val BgInput = UiInput
    val SurfacePanel = UiCard
    val SurfaceInteractive = UiCardElevated
    val BorderSubtle = UiBorder
    val BorderHairline = UiBorder
}

object GabayShapes {
    val PillLarge = RoundedCornerShape(28.dp)
    val PillMedium = RoundedCornerShape(20.dp)
    val PillSmall = RoundedCornerShape(12.dp)
    val Circle = RoundedCornerShape(50)
    val Card = RoundedCornerShape(22.dp)
    val HairlineBorderWidth = 1.dp
}

object GabayTypography {
    val SpeedHuge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Black,
        fontSize = 52.sp,
        color = GabayColors.TextPrimary
    )

    val SpeedMph = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        color = GabayColors.TextMuted
    )

    val SearchText = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        color = GabayColors.TextPrimary
    )

    val SearchPlaceholder = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        color = GabayColors.TextMuted
    )

    val SpeedNumber = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        color = GabayColors.TextPrimary
    )

    val SpeedometerReadout = SpeedNumber

    val ManeuverDistance = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        color = GabayColors.TextPrimary
    )

    val PrimaryManeuverDistance = ManeuverDistance

    val StreetName = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        color = GabayColors.TextPrimary
    )

    val TelemetryValue = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        color = GabayColors.TextPrimary
    )

    val MonospaceLabel = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        letterSpacing = 1.2.sp,
        color = GabayColors.TextMuted
    )

    val HudMicroLabel = MonospaceLabel

    val TelemetryUnit = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        color = GabayColors.TextSecondary
    )

    val BadgeText = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        letterSpacing = 0.8.sp,
        color = GabayColors.AccentAmber
    )

    val PillText = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp,
        color = GabayColors.TextPrimary
    )

    val CoordinateDisplay = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        color = GabayColors.AccentCyan
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = GabayColors.AccentMint,
    onPrimary = GabayColors.BgObsidian,
    primaryContainer = GabayColors.UiCard,
    onPrimaryContainer = GabayColors.TextPrimary,
    background = GabayColors.BgObsidian,
    onBackground = GabayColors.TextPrimary,
    surface = GabayColors.UiCard,
    onSurface = GabayColors.TextPrimary,
    surfaceVariant = GabayColors.UiCardElevated,
    onSurfaceVariant = GabayColors.TextSecondary,
    outline = GabayColors.UiBorder,
    error = GabayColors.AccentRed,
    onError = GabayColors.TextPrimary
)

@Composable
fun GabayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
