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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object GabayColors {
    var isSunlightLightMode by mutableStateOf(false)

    // Map Canvas Palette (Exact match to reference photo in Dark; High-Contrast in Sunlight)
    val MapCanvasBg: Color get() = if (isSunlightLightMode) Color(0xFFF1F5F9) else Color(0xFF202328)
    val MapWater: Color get() = if (isSunlightLightMode) Color(0xFFBAE6FD) else Color(0xFF131519)
    val MapParks: Color get() = if (isSunlightLightMode) Color(0xFFDCFCE7) else Color(0xFF1B231F)
    val MapRoadMinor: Color get() = if (isSunlightLightMode) Color(0xFFCBD5E1) else Color(0xFF2D323C)
    val MapRoadSecondary: Color get() = if (isSunlightLightMode) Color(0xFF94A3B8) else Color(0xFF424A58)
    val MapRoadPrimary: Color get() = if (isSunlightLightMode) Color(0xFF64748B) else Color(0xFF565F70)
    val RouteMint: Color get() = if (isSunlightLightMode) Color(0xFF0284C7) else Color(0xFF00F5D4)
    
    // UI Chrome (Apple-style frosted obsidian glass in Dark; Crisp Ceramic in Light)
    val BgObsidian: Color get() = if (isSunlightLightMode) Color(0xFFF8FAFC) else Color(0xFF121418)
    val UiCard: Color get() = if (isSunlightLightMode) Color(0xFFFFFFFF) else Color(0xFF181A1F)
    val UiCardGlass: Color get() = if (isSunlightLightMode) Color(0xF2FFFFFF) else Color(0xEE181A1F)
    val UiCardElevated: Color get() = if (isSunlightLightMode) Color(0xFFE2E8F0) else Color(0xFF22262E)
    val UiInput: Color get() = if (isSunlightLightMode) Color(0xFFF1F5F9) else Color(0xFF232730)
    val UiBorder: Color get() = if (isSunlightLightMode) Color(0xFFCBD5E1) else Color(0xFF2C313B)
    val UiBorderActive: Color get() = if (isSunlightLightMode) Color(0xFF0284C7) else Color(0xFF00F5D4)
    val UiBorderLight: Color get() = if (isSunlightLightMode) Color(0xFFE2E8F0) else Color(0xFF383F4C)
    
    // Typography Colors
    val TextPrimary: Color get() = if (isSunlightLightMode) Color(0xFF0F172A) else Color(0xFFF1F5F9)
    val TextSecondary: Color get() = if (isSunlightLightMode) Color(0xFF334155) else Color(0xFF94A3B8)
    val TextMuted: Color get() = if (isSunlightLightMode) Color(0xFF64748B) else Color(0xFF64748B)
    
    // Accents & Signals
    val AccentEmerald: Color get() = if (isSunlightLightMode) Color(0xFF059669) else Color(0xFF10B981)
    val AccentMint: Color get() = if (isSunlightLightMode) Color(0xFF0284C7) else Color(0xFF00F5D4)
    val AccentCyan: Color get() = if (isSunlightLightMode) Color(0xFF0284C7) else Color(0xFF06B6D4)
    val AccentAmber: Color get() = if (isSunlightLightMode) Color(0xFFD97706) else Color(0xFFF59E0B)
    val AccentRed: Color get() = if (isSunlightLightMode) Color(0xFFDC2626) else Color(0xFFEF4444)
    val AccentCrimson: Color get() = if (isSunlightLightMode) Color(0xFFDC2626) else Color(0xFFEF4444)

    // Legacy Component Aliases
    val BgCard: Color get() = UiCard
    val BgElevated: Color get() = UiCardElevated
    val BgInput: Color get() = UiInput
    val SurfacePanel: Color get() = UiCard
    val SurfaceInteractive: Color get() = UiCardElevated
    val BorderSubtle: Color get() = UiBorder
    val BorderHairline: Color get() = UiBorder
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
    val SpeedHuge: TextStyle get() = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Black,
        fontSize = 52.sp,
        color = GabayColors.TextPrimary
    )

    val SpeedMph: TextStyle get() = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        color = GabayColors.TextMuted
    )

    val SearchText: TextStyle get() = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        color = GabayColors.TextPrimary
    )

    val SearchPlaceholder: TextStyle get() = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        color = GabayColors.TextMuted
    )

    val SpeedNumber: TextStyle get() = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        color = GabayColors.TextPrimary
    )

    val SpeedometerReadout: TextStyle get() = SpeedNumber

    val ManeuverDistance: TextStyle get() = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        color = GabayColors.TextPrimary
    )

    val PrimaryManeuverDistance: TextStyle get() = ManeuverDistance

    val StreetName: TextStyle get() = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        color = GabayColors.TextPrimary
    )

    val TelemetryValue: TextStyle get() = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        color = GabayColors.TextPrimary
    )

    val MonospaceLabel: TextStyle get() = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        letterSpacing = 1.2.sp,
        color = GabayColors.TextMuted
    )

    val HudMicroLabel: TextStyle get() = MonospaceLabel

    val TelemetryUnit: TextStyle get() = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        color = GabayColors.TextSecondary
    )

    val BadgeText: TextStyle get() = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        letterSpacing = 0.8.sp,
        color = GabayColors.AccentAmber
    )

    val PillText: TextStyle get() = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp,
        color = GabayColors.TextPrimary
    )

    val CoordinateDisplay: TextStyle get() = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        color = GabayColors.AccentCyan
    )
}

@Composable
fun GabayTheme(content: @Composable () -> Unit) {
    val colorScheme = darkColorScheme(
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
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
