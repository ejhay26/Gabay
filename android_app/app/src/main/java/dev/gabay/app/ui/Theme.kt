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

    // ── Map Canvas Palette ────────────────────────────────────────────────────
    val MapCanvasBg: Color get() = if (isSunlightLightMode) Color(0xFFF1F5F9) else Color(0xFF12141A)
    val MapWater: Color get() = if (isSunlightLightMode) Color(0xFF93C5FD) else Color(0xFF0E1520)
    val MapParks: Color get() = if (isSunlightLightMode) Color(0xFFBBF7D0) else Color(0xFF161F1A)
    val MapRoadMinor: Color get() = if (isSunlightLightMode) Color(0xFFFFFFFF) else Color(0xFF242830)
    val MapRoadSecondary: Color get() = if (isSunlightLightMode) Color(0xFFCBD5E1) else Color(0xFF353D4D)
    val MapRoadPrimary: Color get() = if (isSunlightLightMode) Color(0xFF64748B) else Color(0xFF4D5568)
    val RoutePrimary: Color get() = if (isSunlightLightMode) Color(0xFFB45309) else Color(0xFFFFB300)
    val RouteMint: Color get() = RoutePrimary

    // ── Brand Amber / Philippine Sun ─────────────────────────────────────────
    val AccentPrimary: Color get() = if (isSunlightLightMode) Color(0xFFB45309) else Color(0xFFFFB300)
    val AccentAmber: Color get() = if (isSunlightLightMode) Color(0xFFB45309) else Color(0xFFFFB300)
    val AccentAmberGlow: Color get() = if (isSunlightLightMode) Color(0xFFD97706) else Color(0xFFFFCC40)
    val AccentAmberDim: Color get() = if (isSunlightLightMode) Color(0xFFCA8A04) else Color(0xFF8A6010)

    // ── Legacy Mint aliases → re-mapped to Amber ──────────────────────────────
    val AccentMint: Color get() = AccentPrimary
    val AccentCyan: Color get() = AccentPrimary
    val AccentSky: Color get() = Color(0xFF38BDF8)     // kept for misc info
    val AccentSolar: Color get() = AccentPrimary
    val AccentFire: Color get() = AccentCrimson

    // ── Semantic Signals ─────────────────────────────────────────────────────
    val AccentEmerald: Color get() = if (isSunlightLightMode) Color(0xFF047857) else Color(0xFF10B981)
    val AccentRed: Color get() = if (isSunlightLightMode) Color(0xFFB91C1C) else Color(0xFFEF4444)
    val AccentCrimson: Color get() = if (isSunlightLightMode) Color(0xFFB91C1C) else Color(0xFFEF4444)

    // ── UI Chrome ────────────────────────────────────────────────────────────
    val BgObsidian: Color get() = if (isSunlightLightMode) Color(0xFFF8FAFC) else Color(0xFF0D0F14)
    val UiCard: Color get() = if (isSunlightLightMode) Color(0xFFFFFFFF) else Color(0xFF14171D)
    val UiCardGlass: Color get() = if (isSunlightLightMode) Color(0xF8FFFFFF) else Color(0xEE14171D)
    val UiCardElevated: Color get() = if (isSunlightLightMode) Color(0xFFF1F5F9) else Color(0xFF1C2028)
    val UiInput: Color get() = if (isSunlightLightMode) Color(0xFFE2E8F0) else Color(0xFF1E2330)
    val UiBorder: Color get() = if (isSunlightLightMode) Color(0xFF94A3B8) else Color(0xFF2C3345)
    val UiBorderActive: Color get() = if (isSunlightLightMode) Color(0xFFB45309) else Color(0xFFFFB300)
    val UiBorderLight: Color get() = if (isSunlightLightMode) Color(0xFFCBD5E1) else Color(0xFF343D4F)
    // Amber-tinted card for active/selected states
    val UiCardAmber: Color get() = if (isSunlightLightMode) Color(0xFFFEF3C7) else Color(0xFF1E1508)

    // ── Typography ───────────────────────────────────────────────────────────
    val TextPrimary: Color get() = if (isSunlightLightMode) Color(0xFF0F172A) else Color(0xFFF1F5F9)
    val TextSecondary: Color get() = if (isSunlightLightMode) Color(0xFF1E293B) else Color(0xFFCBD5E1)
    val TextMuted: Color get() = if (isSunlightLightMode) Color(0xFF475569) else Color(0xFF718096)
    val TextInverse: Color get() = if (isSunlightLightMode) Color(0xFFFFFFFF) else Color(0xFF0D0F14)

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
