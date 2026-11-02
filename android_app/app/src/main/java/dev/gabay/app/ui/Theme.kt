package dev.gabay.app.ui

import androidx.compose.foundation.shape.GenericShape
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
import kotlin.math.cos
import kotlin.math.sin

/**
 * 01 — GABAY TACTICAL DARK THEME — Philippine Sun Amber Identity
 * Engineered for direct tropical sunlight visibility and vehicular cockpit HUDs.
 * Accent: Electric Amber #FFB300 (Philippine sun). Zero plain generic circles for brand buttons.
 */

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// ─── Flat-top hexagon shape for tactical menu / icon frames ──────────────────
val HexagonShape = GenericShape { size, _ ->
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = minOf(cx, cy)
    moveTo(
        (cx + r * cos(Math.toRadians(-90.0))).toFloat(),
        (cy + r * sin(Math.toRadians(-90.0))).toFloat()
    )
    for (i in 1..5) {
        val angle = Math.toRadians(-90.0 + i * 60.0)
        lineTo((cx + r * cos(angle)).toFloat(), (cy + r * sin(angle)).toFloat())
    }
    close()
}

object GabayColors {
    var isSunlightLightMode by mutableStateOf(false)

    // ── Earthy Minimal Core Palette Tokens ────────────────────────────────────
    val EarthyForest = Color(0xFF2E3A2F)        // Primary Forest Slate (#2E3A2F)
    val EarthySage = Color(0xFF6B7F5B)          // Secondary Muted Olive / Sage (#6B7F5B)
    val EarthySand = Color(0xFFD9C9B2)          // Tertiary Warm Sand / Linen (#D9C9B2)
    val EarthyTerracotta = Color(0xFFC96F4F)    // Accent Warm Clay / Terracotta (#C96F4F)
    val EarthyEmber = Color(0xFFE87A54)         // High-luminance night beacon (#E87A54)
    val EarthyAlabaster = Color(0xFFF8F6EE)     // Background Warm Alabaster (#F8F6EE)
    val EarthyObsidian = Color(0xFF141A15)      // Dark Forest Obsidian Canvas (#141A15)

    // ── Map Canvas Palette ────────────────────────────────────────────────────
    val MapCanvasBg: Color get() = if (isSunlightLightMode) EarthyAlabaster else EarthyObsidian
    val MapWater: Color get() = if (isSunlightLightMode) Color(0xFF9BB7C4) else Color(0xFF152228)
    val MapParks: Color get() = if (isSunlightLightMode) Color(0xFFD6E2CE) else Color(0xFF1D2920)
    val MapRoadMinor: Color get() = if (isSunlightLightMode) Color(0xFFFFFFFF) else Color(0xFF1E2820)
    val MapRoadSecondary: Color get() = if (isSunlightLightMode) Color(0xFFCBD6C7) else Color(0xFF28362B)
    val MapRoadPrimary: Color get() = if (isSunlightLightMode) Color(0xFF8C9B88) else Color(0xFF38473B)
    val RoutePrimary: Color get() = if (isSunlightLightMode) EarthyTerracotta else EarthyEmber
    val RouteMint: Color get() = RoutePrimary

    // ── Brand Accent: Terracotta Clay / Philippine Sun Warmth ────────────────
    val AccentPrimary: Color get() = if (isSunlightLightMode) EarthyTerracotta else EarthyEmber
    val AccentAmber: Color get() = if (isSunlightLightMode) EarthyTerracotta else EarthyEmber
    val AccentAmberGlow: Color get() = EarthyEmber
    val AccentAmberDim: Color get() = EarthyTerracotta

    // ── Legacy Aliases → Re-mapped to Earthy Palette ─────────────────────────
    val AccentMint: Color get() = EarthySage
    val AccentCyan: Color get() = EarthySage
    val AccentSky: Color get() = EarthySand
    val AccentSolar: Color get() = AccentPrimary
    val AccentFire: Color get() = AccentCrimson

    // ── Semantic Signals ─────────────────────────────────────────────────────
    val AccentEmerald: Color get() = if (isSunlightLightMode) Color(0xFF2E6B3E) else Color(0xFF52B771)
    val AccentRed: Color get() = if (isSunlightLightMode) Color(0xFFB91C1C) else Color(0xFFEF4444)
    val AccentCrimson: Color get() = if (isSunlightLightMode) Color(0xFFB91C1C) else Color(0xFFEF4444)

    // ── UI Chrome & Liquid Glass Surfaces ─────────────────────────────────────
    val BgObsidian: Color get() = if (isSunlightLightMode) EarthyAlabaster else EarthyObsidian
    val UiCard: Color get() = if (isSunlightLightMode) Color(0xFFFFFFFF) else Color(0xFF1C251E)
    val UiCardGlass: Color get() = if (isSunlightLightMode) Color(0xF2F8F6EE) else Color(0xD81C251E)
    val UiCardElevated: Color get() = if (isSunlightLightMode) Color(0xFFEFECE1) else Color(0xFF243127)
    val UiInput: Color get() = if (isSunlightLightMode) Color(0xFFE7E3D4) else Color(0xFF222C24)
    val UiBorder: Color get() = if (isSunlightLightMode) EarthySand else Color(0xFF3B4C3E)
    val UiBorderActive: Color get() = if (isSunlightLightMode) EarthyTerracotta else EarthyEmber
    val UiBorderLight: Color get() = if (isSunlightLightMode) Color(0x60D9C9B2) else Color(0x3BD9C9B2)
    val UiCardAmber: Color get() = if (isSunlightLightMode) Color(0xFFFBECE6) else Color(0xFF2B1D16)

    // ── Typography ───────────────────────────────────────────────────────────
    val TextPrimary: Color get() = if (isSunlightLightMode) Color(0xFF1E2820) else EarthyAlabaster
    val TextSecondary: Color get() = if (isSunlightLightMode) Color(0xFF4E5F48) else EarthySand
    val TextMuted: Color get() = if (isSunlightLightMode) Color(0xFF7A8B75) else Color(0xFF8A9E85)
    val TextInverse: Color get() = if (isSunlightLightMode) EarthyAlabaster else EarthyObsidian

    // ── Legacy Aliases ───────────────────────────────────────────────────────
    val BgCard: Color get() = UiCard
    val BgElevated: Color get() = UiCardElevated
    val UiSurfaceElevated: Color get() = UiCardElevated
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
    val Card = RoundedCornerShape(18.dp)
    val CardSmall = RoundedCornerShape(12.dp)
    val HairlineBorderWidth = 1.dp
    /** Flat-top hexagon for tactical menu / icon frames */
    val Hexagon = HexagonShape
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
        color = GabayColors.AccentAmber
    )

    val AmberLabel: TextStyle get() = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 1.0.sp,
        color = GabayColors.AccentAmber
    )
}

@Composable
fun GabayTheme(content: @Composable () -> Unit) {
    val colorScheme = darkColorScheme(
        primary = GabayColors.AccentAmber,
        onPrimary = GabayColors.BgObsidian,
        primaryContainer = GabayColors.UiCardAmber,
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
