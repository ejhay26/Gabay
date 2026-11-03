package dev.gabay.app.ui

import android.content.Context
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gabay.app.audio.AudioController
import dev.gabay.app.audio.AudioGuidanceMode
import dev.gabay.app.audio.VoiceEngineType
import dev.gabay.app.map.FusedMapMode
import uniffi.gabay_core.HudTelemetry
import uniffi.gabay_core.PoiSearchResult
import uniffi.gabay_core.RouteResult
import uniffi.gabay_core.VehicleMode
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

data class ChatMessage(
    val sender: String,
    val text: String,
    val timestamp: String
)

data class DriveHistoryItem(
    val title: String,
    val distanceKm: Float,
    val durationMin: Int,
    val date: String
)

object GabayHudSettings {
    var useMph by mutableStateOf(false)
    var showTurnArrows by mutableStateOf(true)
    var showGForce by mutableStateOf(true)
    var showPacePill by mutableStateOf(true)
    var snapToCenterline by mutableStateOf(true)
    var avoidExpressways by mutableStateOf(true)

    fun init(context: Context) {
        val prefs = context.getSharedPreferences("gabay_hud_settings", Context.MODE_PRIVATE)
        useMph = prefs.getBoolean("use_mph", false)
        showTurnArrows = prefs.getBoolean("show_turn_arrows", true)
        showGForce = prefs.getBoolean("show_g_force", true)
        showPacePill = prefs.getBoolean("show_pace_pill", true)
        snapToCenterline = prefs.getBoolean("snap_centerline", true)
        avoidExpressways = prefs.getBoolean("avoid_expressways", true)
    }

    fun save(context: Context) {
        val prefs = context.getSharedPreferences("gabay_hud_settings", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("use_mph", useMph)
            .putBoolean("show_turn_arrows", showTurnArrows)
            .putBoolean("show_g_force", showGForce)
            .putBoolean("show_pace_pill", showPacePill)
            .putBoolean("snap_centerline", snapToCenterline)
            .putBoolean("avoid_expressways", avoidExpressways)
            .apply()
    }
}

/**
 * Earthy Minimal Liquid Glass System Tokens
 * Palette:
 * Primary: #2E3A2F (Deep Forest Slate)
 * Secondary: #6B7F5B (Muted Sage Green)
 * Tertiary: #D9C9B2 (Warm Sand / Linen)
 * Accent: #C96F4F (Terracotta Clay) / #E87A54 (Glowing Ember)
 * Background: #F8F6EE (Warm Alabaster Linen) / #141A15 (Deep Forest Obsidian)
 */
private object EarthyLiquidTokens {
    val ForestSlate = Color(0xFF2E3A2F)
    val ForestObsidian = Color(0xFF141A15)
    val ForestGlassDark = Color(0xD01C251E)
    val AlabasterGlassLight = Color(0xF2F8F6EE)
    val GlassBorderDark = Color(0x3BD9C9B2)
    val GlassBorderLight = Color(0x55D9C9B2)

    val SageGreen = Color(0xFF6B7F5B)
    val WarmSand = Color(0xFFD9C9B2)
    val Terracotta = Color(0xFFC96F4F)
    val TerracottaGlow = Color(0xFFE87A54)

    val CardShape = RoundedCornerShape(22.dp)
    val PillShape = RoundedCornerShape(26.dp)
    val SmallPillShape = RoundedCornerShape(16.dp)

    fun glassBg(): Color {
        val op = if (GabayGlassSettings.globalEnabled) GabayGlassSettings.surfaceOpacity else 0.90f
        return if (GabayColors.isSunlightLightMode) {
            Color(0xFFF8F6EE).copy(alpha = op)
        } else {
            Color(0xFF141A15).copy(alpha = op)
        }
    }
    fun glassBorder(): Color = if (GabayColors.isSunlightLightMode) GlassBorderLight else GlassBorderDark
    fun glassBorderBrush(): Brush {
        if (!GabayGlassSettings.specularRimEnabled || !GabayGlassSettings.globalEnabled) {
            return SolidColor(glassBorder())
        }
        val isLight = GabayColors.isSunlightLightMode
        return if (isLight) {
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.75f),
                    Color(0x35D9C9B2),
                    Color(0x15D9C9B2),
                    Color(0x40D9C9B2)
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.48f),
                    Color(0x25D9C9B2),
                    Color(0x10141A15),
                    Color(0x38D9C9B2)
                )
            )
        }
    }
    fun accent(): Color = if (GabayColors.isSunlightLightMode) Terracotta else TerracottaGlow
    fun textPrimary(): Color = if (GabayColors.isSunlightLightMode) Color(0xFF1E2820) else Color(0xFFF8F6EE)
    fun textSecondary(): Color = if (GabayColors.isSunlightLightMode) Color(0xFF4E5F48) else WarmSand
    fun textMuted(): Color = if (GabayColors.isSunlightLightMode) Color(0xFF7A8B75) else Color(0xFF8A9E85)
}

/**
 * Authentic Convx & iOS Liquid Glass Toggle Switch.
 * Leverages GlassSwitch with 51dp x 31dp stadium track, 27dp circular thumb,
 * dynamic viscous fluid squash-and-stretch, and tactile haptic feedback.
 */
@Composable
fun LiquidToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    activeColor: Color = Color(0xFF34C759), // Convx Liquid Green
    inactiveTrackColor: Color = if (GabayColors.isSunlightLightMode) Color(0xFFE2E0D8) else Color(0xFF39393D)
) {
    GlassSwitch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        activeTrackColor = activeColor,
        inactiveTrackColor = inactiveTrackColor
    )
}

// Backward-compatible alias
@Composable
fun IosLiquidToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    activeColor: Color = Color(0xFF34C759),
    inactiveTrackColor: Color = if (GabayColors.isSunlightLightMode) Color(0xFFE2E0D8) else Color(0xFF39393D)
) {
    GlassSwitch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        activeTrackColor = activeColor,
        inactiveTrackColor = inactiveTrackColor
    )
}

private fun resolveManeuverIcon(iconStr: String): ImageVector {
    val norm = iconStr.lowercase(Locale.US).trim()
    return when {
        norm.contains("u-turn") || norm.contains("uturn") -> GabayIcons.ManeuverUTurn
        norm.contains("slight-left") || norm.contains("slight left") -> GabayIcons.ManeuverSlightLeft
        norm.contains("slight-right") || norm.contains("slight right") -> GabayIcons.ManeuverSlightRight
        norm.contains("left") -> GabayIcons.ManeuverLeft
        norm.contains("right") -> GabayIcons.ManeuverRight
        norm.contains("fork") -> GabayIcons.ManeuverFork
        norm.contains("merge") -> GabayIcons.ManeuverMerge
        else -> GabayIcons.ManeuverStraight
    }
}

/**
 * Earthy Liquid Glass Two-Way Origin to Destination Header
 */
@Composable
fun TwoWayRouteHeader(
    originName: String = "Your location",
    destinationName: String = "Dropped pin",
    isLoading: Boolean = false,
    isNavigating: Boolean = false,
    onSwap: () -> Unit,
    onClose: () -> Unit,
    onPickOrigin: (() -> Unit)? = null,
    onPickDestination: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .shadow(elevation = 10.dp, shape = EarthyLiquidTokens.CardShape),
        shape = EarthyLiquidTokens.CardShape,
        colors = CardDefaults.cardColors(containerColor = EarthyLiquidTokens.glassBg()),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(if (isLoading) EarthyLiquidTokens.accent().copy(alpha = 0.8f) else EarthyLiquidTokens.glassBorder()),
            width = 0.8.dp
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Route Visual Indicator (Sage Ring -> Vertical Connector -> Terracotta Dot)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 2.dp, end = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(Color.Transparent, CircleShape)
                            .border(2.5.dp, EarthyLiquidTokens.SageGreen, CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .padding(vertical = 3.dp)
                            .width(1.5.dp)
                            .height(16.dp)
                            .background(EarthyLiquidTokens.textMuted().copy(alpha = 0.4f))
                    )
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(EarthyLiquidTokens.accent(), CircleShape)
                    )
                }

                // Center Origin & Destination Labels
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = onPickOrigin != null) { onPickOrigin?.invoke() }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = originName,
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = EarthyLiquidTokens.textPrimary()
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = onPickDestination != null) { onPickDestination?.invoke() }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = destinationName,
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = EarthyLiquidTokens.textPrimary()
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Action Buttons: Swap and Close
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isNavigating) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (GabayColors.isSunlightLightMode) Color(0xFFE2E8F0) else Color(0x33FFFFFF))
                                .clickable { onSwap() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = GabayIcons.ModeSwap,
                                contentDescription = "Swap Locations",
                                tint = EarthyLiquidTokens.textPrimary(),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (GabayColors.isSunlightLightMode) Color(0xFFE2E8F0) else Color(0x33FFFFFF))
                            .clickable { onClose() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = GabayIcons.ActionClose,
                            contentDescription = "Cancel Route",
                            tint = EarthyLiquidTokens.textPrimary(),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp),
                    color = EarthyLiquidTokens.accent(),
                    trackColor = Color.Transparent
                )
            }
        }
    }
}

/**
 * Route Calculating Minimal Shimmer
 */
@Composable
fun RouteCalculatingIndicator(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .shadow(8.dp, EarthyLiquidTokens.PillShape),
        shape = EarthyLiquidTokens.PillShape,
        colors = CardDefaults.cardColors(containerColor = EarthyLiquidTokens.glassBg()),
        border = CardDefaults.outlinedCardBorder().copy(brush = EarthyLiquidTokens.glassBorderBrush(), width = 0.8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = EarthyLiquidTokens.accent()
            )
            Text(
                text = "Calculating fastest route...",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = EarthyLiquidTokens.textPrimary()
                )
            )
        }
    }
}

/**
 * Earthy Liquid Glass Search Bar Capsule
 */
@Composable
fun TopSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    searchResults: List<PoiSearchResult>,
    onSelectPlace: (PoiSearchResult) -> Unit,
    onMenuClick: () -> Unit,
    audioGuidanceMode: dev.gabay.app.audio.AudioGuidanceMode = dev.gabay.app.audio.AudioGuidanceMode.FULL_VOICE,
    onToggleAudioMode: () -> Unit = {},
    recentDestinations: List<dev.gabay.app.data.RecentDestination> = emptyList(),
    onSelectRecent: (dev.gabay.app.data.RecentDestination) -> Unit = {},
    onClearRecents: () -> Unit = {},
    onCategoryClick: (String) -> Unit = {},
    hint: String = "Search destinations, roads, landmarks...",
    onCancelSearch: (() -> Unit)? = null,
    onUseCurrentLocation: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isSearchFocused by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        // Main Search Capsule
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .shadow(elevation = 12.dp, shape = EarthyLiquidTokens.PillShape),
            shape = EarthyLiquidTokens.PillShape,
            colors = CardDefaults.cardColors(containerColor = EarthyLiquidTokens.glassBg()),
            border = CardDefaults.outlinedCardBorder().copy(brush = EarthyLiquidTokens.glassBorderBrush(), width = 0.8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onCancelSearch != null) {
                    IconButton(
                        onClick = onCancelSearch,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = GabayIcons.ChevronLeft,
                            contentDescription = "Back",
                            tint = EarthyLiquidTokens.textPrimary(),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (GabayColors.isSunlightLightMode) Color(0xFFF1F5F9) else Color(0x33FFFFFF))
                            .clickable { onMenuClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = GabayIcons.Menu,
                            contentDescription = "Menu",
                            tint = EarthyLiquidTokens.textPrimary(),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (query.isEmpty()) {
                        Text(
                            text = hint,
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Normal,
                                fontSize = 15.sp,
                                color = EarthyLiquidTokens.textMuted()
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = {
                            onQueryChange(it)
                            isSearchFocused = true
                        },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Normal,
                            fontSize = 15.sp,
                            color = EarthyLiquidTokens.textPrimary()
                        ),
                        cursorBrush = SolidColor(EarthyLiquidTokens.accent()),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            onQueryChange("")
                            isSearchFocused = false
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = GabayIcons.ActionClose,
                            contentDescription = "Clear",
                            tint = EarthyLiquidTokens.textMuted(),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (GabayColors.isSunlightLightMode) Color(0xFFF1F5F9) else Color(0x33FFFFFF))
                        .clickable { onToggleAudioMode() },
                    contentAlignment = Alignment.Center
                ) {
                    val audioIcon = when (audioGuidanceMode) {
                        AudioGuidanceMode.FULL_VOICE -> GabayIcons.AudioTts
                        AudioGuidanceMode.ALERTS_ONLY -> GabayIcons.AudioTts
                        AudioGuidanceMode.MUTED -> GabayIcons.AudioMute
                    }
                    val audioTint = when (audioGuidanceMode) {
                        AudioGuidanceMode.FULL_VOICE -> EarthyLiquidTokens.SageGreen
                        AudioGuidanceMode.ALERTS_ONLY -> EarthyLiquidTokens.accent()
                        AudioGuidanceMode.MUTED -> Color(0xFFB91C1C)
                    }
                    Icon(
                        imageVector = audioIcon,
                        contentDescription = "Voice Guidance",
                        tint = audioTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Clean Professional Typographic Category Pills (Zero Emojis / Zero Slop)
        if (query.isEmpty() && searchResults.isEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val categories = listOf(
                    "Gas Stations" to "gas station",
                    "Coffee & Cafes" to "coffee",
                    "Restaurants" to "restaurant",
                    "Parking" to "parking",
                    "Expressway Tolls" to "expressway toll",
                    "Hospitals" to "hospital",
                    "Convenience Stores" to "convenience store"
                )
                categories.forEach { (label, keyword) ->
                    Box(
                        modifier = Modifier
                            .clip(EarthyLiquidTokens.SmallPillShape)
                            .background(EarthyLiquidTokens.glassBg())
                            .border(0.8.dp, EarthyLiquidTokens.glassBorder(), EarthyLiquidTokens.SmallPillShape)
                            .clickable {
                                onQueryChange(keyword)
                                onCategoryClick(keyword)
                            }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = label,
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = EarthyLiquidTokens.textPrimary()
                            )
                        )
                    }
                }
            }
        }

        // Search Results & Recent Suggestions Card
        if (searchResults.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
                    .shadow(16.dp, EarthyLiquidTokens.CardShape),
                shape = EarthyLiquidTokens.CardShape,
                colors = CardDefaults.cardColors(containerColor = EarthyLiquidTokens.glassBg()),
                border = CardDefaults.outlinedCardBorder().copy(brush = EarthyLiquidTokens.glassBorderBrush(), width = 0.8.dp)
            ) {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(searchResults) { result ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    onSelectPlace(result)
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = GabayIcons.DestinationPin,
                                contentDescription = null,
                                tint = EarthyLiquidTokens.accent(),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = result.name,
                                    style = TextStyle(
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = EarthyLiquidTokens.textPrimary()
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val distanceLabel = String.format(Locale.US, "%.1f km", result.distanceMeters / 1000.0)
                                Text(
                                    text = "${result.category.ifBlank { "Point of Interest" }} • $distanceLabel",
                                    style = TextStyle(
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 12.sp,
                                        color = EarthyLiquidTokens.textSecondary()
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        HorizontalDivider(color = EarthyLiquidTokens.glassBorder(), thickness = 0.5.dp)
                    }
                }
            }
        } else if (query.isEmpty() && recentDestinations.isNotEmpty() && isSearchFocused) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .shadow(16.dp, EarthyLiquidTokens.CardShape),
                shape = EarthyLiquidTokens.CardShape,
                colors = CardDefaults.cardColors(containerColor = EarthyLiquidTokens.glassBg()),
                border = CardDefaults.outlinedCardBorder().copy(brush = EarthyLiquidTokens.glassBorderBrush(), width = 0.8.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RECENT DESTINATIONS",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = EarthyLiquidTokens.textMuted()
                            )
                        )
                        Text(
                            text = "Clear",
                            modifier = Modifier.clickable { onClearRecents() },
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = EarthyLiquidTokens.accent()
                            )
                        )
                    }
                    HorizontalDivider(color = EarthyLiquidTokens.glassBorder(), thickness = 0.5.dp)
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(recentDestinations) { recent ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                        onSelectRecent(recent)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = GabayIcons.TimePace,
                                    contentDescription = null,
                                    tint = EarthyLiquidTokens.textMuted(),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = recent.name,
                                    style = TextStyle(
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = EarthyLiquidTokens.textPrimary()
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            HorizontalDivider(color = EarthyLiquidTokens.glassBorder(), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Earthy Liquid Glass Bottom Telemetry & Speedometer Deck
 */
@Composable
fun MiddleBottomSpeedometer(
    speedKmh: Float,
    nextTurnIcon: String,
    bottomLabel: String = "",
    modifier: Modifier = Modifier
) {
    val speedInt = if (speedKmh < 1.0f) 0 else speedKmh.toInt()
    val speedMph = (speedInt * 0.621371f).toInt()
    val displaySpeed = if (GabayHudSettings.useMph) "$speedMph" else "$speedInt"
    val displayUnit = if (GabayHudSettings.useMph) "MPH" else "KM/H"

    val hasActiveTurn = bottomLabel.isNotBlank() || (nextTurnIcon.isNotBlank() && nextTurnIcon != "straight")

    Card(
        modifier = modifier
            .shadow(elevation = 12.dp, shape = EarthyLiquidTokens.PillShape),
        shape = EarthyLiquidTokens.PillShape,
        colors = CardDefaults.cardColors(containerColor = EarthyLiquidTokens.glassBg()),
        border = CardDefaults.outlinedCardBorder().copy(brush = EarthyLiquidTokens.glassBorderBrush(), width = 0.8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (hasActiveTurn) {
                // Maneuver Glyph Pod (Terracotta Clay / Ember)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(EarthyLiquidTokens.accent().copy(alpha = 0.2f), CircleShape)
                        .border(1.dp, EarthyLiquidTokens.accent().copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = resolveManeuverIcon(nextTurnIcon),
                        contentDescription = "Next Turn",
                        tint = EarthyLiquidTokens.accent(),
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (bottomLabel.isNotBlank()) {
                    Column(
                        modifier = Modifier.widthIn(max = 140.dp)
                    ) {
                        Text(
                            text = bottomLabel,
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = EarthyLiquidTokens.textPrimary()
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Next maneuver",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 10.sp,
                                color = EarthyLiquidTokens.textMuted()
                            )
                        )
                    }

                    // Vertical Hairline Divider
                    Box(
                        modifier = Modifier
                            .width(0.8.dp)
                            .height(28.dp)
                            .background(EarthyLiquidTokens.glassBorder())
                    )
                }
            }

            // Speedometer Pod
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = displaySpeed,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Black,
                        fontSize = 32.sp,
                        letterSpacing = (-1.0).sp,
                        color = EarthyLiquidTokens.textPrimary()
                    )
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = displayUnit,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = EarthyLiquidTokens.accent()
                    ),
                    modifier = Modifier.padding(bottom = 5.dp)
                )
            }
        }
    }
}

/**
 * Minimalist Earthy Liquid Glass Telemetry Capsule
 */
@Composable
fun FloatingTelemetryPill(
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .shadow(elevation = 8.dp, shape = EarthyLiquidTokens.PillShape),
        shape = EarthyLiquidTokens.PillShape,
        colors = CardDefaults.cardColors(containerColor = EarthyLiquidTokens.glassBg()),
        border = CardDefaults.outlinedCardBorder().copy(brush = EarthyLiquidTokens.glassBorderBrush(), width = 0.8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = value,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = EarthyLiquidTokens.textPrimary()
                )
            )
            Text(
                text = unit,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    color = EarthyLiquidTokens.accent()
                )
            )
        }
    }
}

/**
 * Map Layer Speed Dial
 */
@Composable
fun MapStyleSpeedDial(
    currentMode: FusedMapMode,
    onSelectMode: (FusedMapMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    val options = listOf(
        Triple(FusedMapMode.STREET, GabayIcons.LayerVector, "Vector Street"),
        Triple(FusedMapMode.HYBRID_3D, GabayIcons.Layer3d, "3D Buildings"),
        Triple(FusedMapMode.SATELLITE, GabayIcons.LayerSatellite, "Satellite"),
        Triple(FusedMapMode.TOPO, GabayIcons.LayerTopo, "Topography")
    )

    Box(
        modifier = modifier.size(48.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        AnimatedVisibility(
            visible = isExpanded,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 56.dp)
                .wrapContentSize(Alignment.BottomEnd, unbounded = true),
            enter = fadeIn(tween(180)) + slideInVertically(initialOffsetY = { it / 3 }),
            exit = fadeOut(tween(140)) + slideOutVertically(targetOffsetY = { it / 3 })
        ) {
            Card(
                shape = EarthyLiquidTokens.CardShape,
                colors = CardDefaults.cardColors(containerColor = EarthyLiquidTokens.glassBg()),
                border = CardDefaults.outlinedCardBorder().copy(brush = EarthyLiquidTokens.glassBorderBrush(), width = 0.8.dp),
                modifier = Modifier
                    .shadow(16.dp, EarthyLiquidTokens.CardShape)
                    .width(168.dp)
            ) {
                Column(
                    modifier = Modifier.padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    options.forEach { (mode, icon, label) ->
                        val isSelected = (mode == currentMode)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) EarthyLiquidTokens.SageGreen.copy(alpha = 0.25f) else Color.Transparent)
                                .clickable {
                                    onSelectMode(mode)
                                    isExpanded = false
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isSelected) EarthyLiquidTokens.accent() else EarthyLiquidTokens.textPrimary(),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = label,
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = if (isSelected) EarthyLiquidTokens.accent() else EarthyLiquidTokens.textPrimary()
                                )
                            )
                        }
                    }
                }
            }
        }

        CircularFloatingButton(
            icon = GabayIcons.LayerStack,
            onClick = { isExpanded = !isExpanded },
            isActive = isExpanded,
            contentDescription = "Map Style",
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}

/**
 * 2D / 3D Perspective Tilt Button
 */
@Composable
fun PerspectiveToggleButton(
    is3d: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .shadow(elevation = 8.dp, shape = CircleShape)
            .background(if (is3d) EarthyLiquidTokens.accent().copy(alpha = 0.22f) else EarthyLiquidTokens.glassBg(), CircleShape)
            .border(
                0.8.dp,
                if (is3d) EarthyLiquidTokens.accent() else EarthyLiquidTokens.glassBorder(),
                CircleShape
            )
            .clickable { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (is3d) "3D" else "2D",
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                color = if (is3d) EarthyLiquidTokens.accent() else EarthyLiquidTokens.textPrimary()
            )
        )
    }
}

/**
 * Compass Reset Button (Dynamic Rotating Needle)
 */
@Composable
fun CompassResetButton(
    cameraBearing: Float,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRotated = Math.abs(cameraBearing) > 2.0f

    AnimatedVisibility(
        visible = isRotated,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        Box(
            modifier = modifier
                .size(48.dp)
                .shadow(elevation = 8.dp, shape = CircleShape)
                .background(EarthyLiquidTokens.glassBg(), CircleShape)
                .border(0.8.dp, EarthyLiquidTokens.glassBorder(), CircleShape)
                .clickable { onReset() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.rotate(-cameraBearing)
            ) {
                // North Needle Tip (Terracotta Ember)
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(10.dp)
                        .background(EarthyLiquidTokens.accent(), RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                )
                Spacer(modifier = Modifier.height(1.5.dp))
                // South Needle Base (Warm Sand)
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(10.dp)
                        .background(EarthyLiquidTokens.WarmSand, RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp))
                )
            }
        }
    }
}

/**
 * Circular Glass Floating Action Button
 */
@Composable
fun CircularFloatingButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    activeColor: Color = EarthyLiquidTokens.accent(),
    contentDescription: String? = null
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .shadow(elevation = 8.dp, shape = CircleShape)
            .background(if (isActive) activeColor.copy(alpha = 0.22f) else EarthyLiquidTokens.glassBg(), CircleShape)
            .border(
                0.8.dp,
                if (isActive) activeColor else EarthyLiquidTokens.glassBorder(),
                CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) activeColor else EarthyLiquidTokens.textPrimary(),
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Earthy Liquid Glass Route Preview Modal Sheet
 */
@Composable
fun RoutePreviewSheet(
    route: RouteResult,
    selectedMode: VehicleMode,
    onSelectMode: (VehicleMode) -> Unit,
    avoidTolls: Boolean,
    onToggleAvoidTolls: () -> Unit,
    avoidHighways: Boolean,
    onToggleAvoidHighways: () -> Unit,
    onStartNavigation: () -> Unit,
    onCancelRoute: () -> Unit,
    routeAdvisory: String? = null,
    isCommuteMode: Boolean = false,
    onSelectCommute: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val distKm = route.totalDistanceMeters / 1000.0
    val durMin = (route.estimatedDurationSeconds / 60u).coerceAtLeast(1u)
    val viaRoad = route.maneuvers.firstOrNull()?.roadName?.ifBlank { "Optimal Highway" } ?: "Optimal Highway"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .shadow(elevation = 20.dp, shape = EarthyLiquidTokens.CardShape),
        shape = EarthyLiquidTokens.CardShape,
        colors = CardDefaults.cardColors(containerColor = EarthyLiquidTokens.glassBg()),
        border = CardDefaults.outlinedCardBorder().copy(brush = EarthyLiquidTokens.glassBorderBrush(), width = 0.8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .background(EarthyLiquidTokens.textMuted().copy(alpha = 0.4f), CircleShape)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Vehicle Mode Segmented Control
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (GabayColors.isSunlightLightMode) Color(0xFFF1F5F9) else Color(0x33000000), EarthyLiquidTokens.SmallPillShape)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val modes = listOf(
                    Triple(VehicleMode.SCOOTER, "Scooter", GabayIcons.ModeScooter),
                    Triple(VehicleMode.CAR, "Car", GabayIcons.ModeCar),
                    Triple(VehicleMode.WALK, "Walk", GabayIcons.ModeWalk)
                )

                modes.forEach { (mode, label, icon) ->
                    val isSelected = selectedMode == mode && !isCommuteMode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(EarthyLiquidTokens.SmallPillShape)
                            .background(if (isSelected) EarthyLiquidTokens.glassBg() else Color.Transparent)
                            .border(if (isSelected) 0.8.dp else 0.dp, if (isSelected) EarthyLiquidTokens.glassBorder() else Color.Transparent, EarthyLiquidTokens.SmallPillShape)
                            .clickable { onSelectMode(mode) }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isSelected) EarthyLiquidTokens.accent() else EarthyLiquidTokens.textSecondary(),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = label,
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.5.sp,
                                    color = if (isSelected) EarthyLiquidTokens.textPrimary() else EarthyLiquidTokens.textSecondary()
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Duration & Distance Hero Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "$durMin min",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp,
                                color = EarthyLiquidTokens.accent()
                            )
                        )
                        Text(
                            text = "(${String.format(Locale.US, "%.1f", distKm)} km)",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Normal,
                                fontSize = 15.sp,
                                color = EarthyLiquidTokens.textSecondary()
                            ),
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                    Text(
                        text = "Fastest route via $viaRoad",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = EarthyLiquidTokens.textSecondary()
                        )
                    )
                }

                // Avoidance Options Chips
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(EarthyLiquidTokens.SmallPillShape)
                            .background(if (avoidTolls) EarthyLiquidTokens.accent().copy(alpha = 0.2f) else Color.Transparent)
                            .border(0.8.dp, if (avoidTolls) EarthyLiquidTokens.accent() else EarthyLiquidTokens.glassBorder(), EarthyLiquidTokens.SmallPillShape)
                            .clickable { onToggleAvoidTolls() }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "No Tolls",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = if (avoidTolls) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp,
                                color = if (avoidTolls) EarthyLiquidTokens.accent() else EarthyLiquidTokens.textSecondary()
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(EarthyLiquidTokens.SmallPillShape)
                            .background(if (avoidHighways) EarthyLiquidTokens.accent().copy(alpha = 0.2f) else Color.Transparent)
                            .border(0.8.dp, if (avoidHighways) EarthyLiquidTokens.accent() else EarthyLiquidTokens.glassBorder(), EarthyLiquidTokens.SmallPillShape)
                            .clickable { onToggleAvoidHighways() }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "No Highways",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = if (avoidHighways) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp,
                                color = if (avoidHighways) EarthyLiquidTokens.accent() else EarthyLiquidTokens.textSecondary()
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Primary Actions: "START NAVIGATION" & Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onStartNavigation,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EarthyLiquidTokens.accent(),
                        contentColor = Color.White
                    ),
                    shape = EarthyLiquidTokens.PillShape,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = GabayIcons.NavCursor,
                            contentDescription = "Start",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "START NAVIGATION",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (GabayColors.isSunlightLightMode) Color(0xFFFEE2E2) else Color(0x33FF3B30))
                        .border(0.8.dp, Color(0xFFB91C1C).copy(alpha = 0.5f), CircleShape)
                        .clickable { onCancelRoute() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = GabayIcons.ActionClose,
                        contentDescription = "Cancel",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Earthy Liquid Co-Pilot Assistant Sheet
 */
@Composable
fun AgentCoPilotSheet(
    messages: List<ChatMessage>,
    isListening: Boolean = false,
    audioRmsDb: Float = 0f,
    partialSpeechText: String = "",
    onToggleListening: () -> Unit = {},
    onSendPrompt: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    var offsetY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(partialSpeechText) {
        if (partialSpeechText.isNotBlank()) {
            inputText = partialSpeechText
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.70f)
            .offset { IntOffset(0, offsetY.roundToInt()) }
            .shadow(24.dp, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        colors = CardDefaults.cardColors(containerColor = EarthyLiquidTokens.glassBg()),
        border = CardDefaults.outlinedCardBorder().copy(brush = EarthyLiquidTokens.glassBorderBrush(), width = 0.8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (offsetY > 120f) onClose() else offsetY = 0f
                            },
                            onVerticalDrag = { _, dragAmount ->
                                offsetY = (offsetY + dragAmount).coerceAtLeast(0f)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(4.dp)
                        .background(EarthyLiquidTokens.textMuted().copy(alpha = 0.4f), CircleShape)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(if (isListening) Color(0xFFEF4444) else EarthyLiquidTokens.accent(), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isListening) "Listening..." else "Gabay Co-Pilot",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = EarthyLiquidTokens.textPrimary()
                        )
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = GabayIcons.ActionClose,
                        contentDescription = "Close",
                        tint = EarthyLiquidTokens.textSecondary(),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (isListening) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (GabayColors.isSunlightLightMode) Color(0xFFF1F5F9) else Color(0x33000000), EarthyLiquidTokens.SmallPillShape)
                        .border(0.8.dp, Color(0xFFEF4444).copy(alpha = 0.4f), EarthyLiquidTokens.SmallPillShape)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val normalizedDb = ((audioRmsDb + 2f) / 14f).coerceIn(0.15f, 1.0f)
                    val bars = listOf(0.45f, 0.75f, 1.0f, 0.90f, 0.65f, 0.85f, 0.50f)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        bars.forEach { factor ->
                            val h = (4.dp + (16.dp * normalizedDb * factor))
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(h)
                                    .background(Color(0xFFEF4444), RoundedCornerShape(1.5.dp))
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    val speechPrompt = if (partialSpeechText.isNotBlank()) "\"$partialSpeechText\"" else "Speak your question or destination..."
                    Text(
                        text = speechPrompt,
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 12.sp,
                            color = EarthyLiquidTokens.textPrimary()
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "Route to NEUST San Isidro",
                    "Change my campus",
                    "Where am I?",
                    "Nearest Gas Station",
                    "3D Perspective",
                    "Satellite Mode",
                    "Cancel Route"
                ).forEach { prompt ->
                    Box(
                        modifier = Modifier
                            .clip(EarthyLiquidTokens.SmallPillShape)
                            .background(if (GabayColors.isSunlightLightMode) Color(0xFFF1F5F9) else Color(0x33FFFFFF))
                            .border(0.8.dp, EarthyLiquidTokens.glassBorder(), EarthyLiquidTokens.SmallPillShape)
                            .clickable { onSendPrompt(prompt) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = prompt,
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 12.sp,
                                color = EarthyLiquidTokens.textPrimary()
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val listState = rememberLazyListState()
            LaunchedEffect(messages.size) {
                if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Ask Gabay anything about your route, POIs,\nor traffic conditions.",
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 13.sp,
                                    color = EarthyLiquidTokens.textMuted(),
                                    textAlign = TextAlign.Center
                                )
                            )
                        }
                    }
                } else {
                    items(messages) { msg ->
                        val isUser = msg.sender.equals("RIDER", ignoreCase = true)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            Card(
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isUser) 16.dp else 4.dp,
                                    bottomEnd = if (isUser) 4.dp else 16.dp
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isUser) EarthyLiquidTokens.ForestSlate else (if (GabayColors.isSunlightLightMode) Color(0xFFF1F5F9) else Color(0x33FFFFFF))
                                ),
                                border = if (isUser) null else CardDefaults.outlinedCardBorder().copy(brush = EarthyLiquidTokens.glassBorderBrush(), width = 0.8.dp),
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    Text(
                                        text = msg.text,
                                        style = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontSize = 13.sp,
                                            color = if (isUser) Color.White else EarthyLiquidTokens.textPrimary()
                                        )
                                    )
                                    Text(
                                        text = msg.timestamp,
                                        style = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontSize = 9.sp,
                                            color = if (isUser) Color.White.copy(alpha = 0.7f) else EarthyLiquidTokens.textMuted()
                                        ),
                                        modifier = Modifier.align(Alignment.End)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Card(
                shape = EarthyLiquidTokens.PillShape,
                colors = CardDefaults.cardColors(containerColor = if (GabayColors.isSunlightLightMode) Color(0xFFF1F5F9) else Color(0x33FFFFFF)),
                border = CardDefaults.outlinedCardBorder().copy(brush = EarthyLiquidTokens.glassBorderBrush(), width = 0.8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        if (inputText.isEmpty()) {
                            Text(
                                text = "Ask Gabay...",
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 14.sp,
                                    color = EarthyLiquidTokens.textMuted()
                                )
                            )
                        }
                        BasicTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            textStyle = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 14.sp,
                                color = EarthyLiquidTokens.textPrimary()
                            ),
                            cursorBrush = SolidColor(EarthyLiquidTokens.accent()),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    IconButton(
                        onClick = onToggleListening,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = GabayIcons.CopilotMic,
                            contentDescription = "Mic",
                            tint = if (isListening) Color(0xFFEF4444) else EarthyLiquidTokens.accent(),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                onSendPrompt(inputText)
                                inputText = ""
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = GabayIcons.Send,
                            contentDescription = "Send",
                            tint = if (inputText.isNotBlank()) EarthyLiquidTokens.accent() else EarthyLiquidTokens.textMuted(),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Earthy Liquid Glass Settings Drawer (with Modern IosLiquidToggle switches)
 */
@Composable
fun TopMenuSheet(
    driveHistory: List<DriveHistoryItem>,
    selectedMode: VehicleMode,
    onModeSelect: (VehicleMode) -> Unit,
    audioController: dev.gabay.app.audio.AudioController,
    onThemeToggle: (Boolean) -> Unit,
    currentMapMode: FusedMapMode = FusedMapMode.STREET,
    onMapModeSelect: (FusedMapMode) -> Unit = {},
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var currentVoiceEngine by remember { mutableStateOf(audioController.voiceEngine) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.80f)
            .offset { IntOffset(0, offsetY.roundToInt()) }
            .shadow(24.dp, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        colors = CardDefaults.cardColors(containerColor = EarthyLiquidTokens.glassBg()),
        border = CardDefaults.outlinedCardBorder().copy(brush = EarthyLiquidTokens.glassBorderBrush(), width = 0.8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = { if (offsetY > 120f) onClose() else offsetY = 0f },
                            onVerticalDrag = { _, dragAmount -> offsetY = (offsetY + dragAmount).coerceAtLeast(0f) }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(4.dp)
                        .background(EarthyLiquidTokens.textMuted().copy(alpha = 0.4f), CircleShape)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Preferences & Vault",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = EarthyLiquidTokens.textPrimary()
                    )
                )

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = GabayIcons.ActionClose,
                        contentDescription = "Close",
                        tint = EarthyLiquidTokens.textSecondary(),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // iOS Segmented Control Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (GabayColors.isSunlightLightMode) Color(0xFFF1F5F9) else Color(0x33000000), EarthyLiquidTokens.SmallPillShape)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Preferences", "Drives (${driveHistory.size})", "Offline Vault").forEachIndexed { index, label ->
                    val isSel = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(EarthyLiquidTokens.SmallPillShape)
                            .background(if (isSel) EarthyLiquidTokens.glassBg() else Color.Transparent)
                            .border(if (isSel) 0.8.dp else 0.dp, if (isSel) EarthyLiquidTokens.glassBorder() else Color.Transparent, EarthyLiquidTokens.SmallPillShape)
                            .clickable { selectedTab = index }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 12.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) EarthyLiquidTokens.textPrimary() else EarthyLiquidTokens.textMuted()
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedTab) {
                0 -> {
                    // TAB 0: Preferences
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Vehicle Mode Selector
                        item {
                            Text("DEFAULT VEHICLE MODE", style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = EarthyLiquidTokens.textMuted()))
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    Triple(VehicleMode.SCOOTER, "Scooter", GabayIcons.ModeScooter),
                                    Triple(VehicleMode.CAR, "Car", GabayIcons.ModeCar),
                                    Triple(VehicleMode.WALK, "Walk", GabayIcons.ModeWalk)
                                ).forEach { (mode, label, icon) ->
                                    val isSel = selectedMode == mode
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(EarthyLiquidTokens.SmallPillShape)
                                            .background(if (isSel) EarthyLiquidTokens.accent().copy(alpha = 0.2f) else (if (GabayColors.isSunlightLightMode) Color(0xFFF1F5F9) else Color(0x33FFFFFF)))
                                            .border(0.8.dp, if (isSel) EarthyLiquidTokens.accent() else EarthyLiquidTokens.glassBorder(), EarthyLiquidTokens.SmallPillShape)
                                            .clickable { onModeSelect(mode) }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(imageVector = icon, contentDescription = null, tint = if (isSel) EarthyLiquidTokens.accent() else EarthyLiquidTokens.textSecondary(), modifier = Modifier.size(16.dp))
                                            Text(text = label, style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = if (isSel) EarthyLiquidTokens.accent() else EarthyLiquidTokens.textPrimary()))
                                        }
                                    }
                                }
                            }
                        }

                        // Map View Style Selector
                        item {
                            Text("MAP VIEW STYLE", style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = EarthyLiquidTokens.textMuted()))
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    Triple(FusedMapMode.STREET, "Street", GabayIcons.LayerVector),
                                    Triple(FusedMapMode.HYBRID_3D, "3D", GabayIcons.Layer3d),
                                    Triple(FusedMapMode.SATELLITE, "Satellite", GabayIcons.LayerSatellite),
                                    Triple(FusedMapMode.TOPO, "Topo", GabayIcons.LayerTopo)
                                ).forEach { (mode, label, icon) ->
                                    val isSel = currentMapMode == mode
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(EarthyLiquidTokens.SmallPillShape)
                                            .background(if (isSel) EarthyLiquidTokens.accent().copy(alpha = 0.2f) else (if (GabayColors.isSunlightLightMode) Color(0xFFF1F5F9) else Color(0x33FFFFFF)))
                                            .border(0.8.dp, if (isSel) EarthyLiquidTokens.accent() else EarthyLiquidTokens.glassBorder(), EarthyLiquidTokens.SmallPillShape)
                                            .clickable { onMapModeSelect(mode) }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Icon(imageVector = icon, contentDescription = null, tint = if (isSel) EarthyLiquidTokens.accent() else EarthyLiquidTokens.textSecondary(), modifier = Modifier.size(16.dp))
                                            Text(text = label, style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = if (isSel) EarthyLiquidTokens.accent() else EarthyLiquidTokens.textPrimary()))
                                        }
                                    }
                                }
                            }
                        }

                        // Modern iOS Liquid Toggles Section
                        item {
                            Text("COCKPIT CONTROLS & HUD", style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = EarthyLiquidTokens.textMuted()))
                            Spacer(modifier = Modifier.height(6.dp))
                            Card(
                                shape = EarthyLiquidTokens.SmallPillShape,
                                colors = CardDefaults.cardColors(containerColor = if (GabayColors.isSunlightLightMode) Color(0xFFF1F5F9) else Color(0x33FFFFFF)),
                                border = CardDefaults.outlinedCardBorder().copy(brush = EarthyLiquidTokens.glassBorderBrush(), width = 0.8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                                    // Sunlight Bright Mode Toggle
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Sunlight Bright Mode", style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = EarthyLiquidTokens.textPrimary()))
                                            Text("High contrast alabaster theme for direct tropical sunlight", style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 11.sp, color = EarthyLiquidTokens.textMuted()))
                                        }
                                        IosLiquidToggle(
                                            checked = GabayColors.isSunlightLightMode,
                                            onCheckedChange = { onThemeToggle(it) }
                                        )
                                    }
                                    HorizontalDivider(color = EarthyLiquidTokens.glassBorder(), thickness = 0.5.dp)

                                    // Avoid Expressways Toggle
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Avoid Expressways & Tolls", style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = EarthyLiquidTokens.textPrimary()))
                                            Text("Route via local service roads (<400cc friendly)", style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 11.sp, color = EarthyLiquidTokens.textMuted()))
                                        }
                                        IosLiquidToggle(
                                            checked = GabayHudSettings.avoidExpressways,
                                            onCheckedChange = {
                                                GabayHudSettings.avoidExpressways = it
                                            }
                                        )
                                    }
                                    HorizontalDivider(color = EarthyLiquidTokens.glassBorder(), thickness = 0.5.dp)

                                    // Turn Guidance Arrows Toggle
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Turn Guidance Arrows", style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = EarthyLiquidTokens.textPrimary()))
                                            Text("Display active maneuver indicators in speedometer pod", style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 11.sp, color = EarthyLiquidTokens.textMuted()))
                                        }
                                        IosLiquidToggle(
                                            checked = GabayHudSettings.showTurnArrows,
                                            onCheckedChange = {
                                                GabayHudSettings.showTurnArrows = it
                                            }
                                        )
                                    }
                                    HorizontalDivider(color = EarthyLiquidTokens.glassBorder(), thickness = 0.5.dp)

                                    // Speed Units Toggle (MPH vs KM/H)
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Use MPH Units", style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = EarthyLiquidTokens.textPrimary()))
                                            Text("Switch speed readout between KM/H and MPH", style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 11.sp, color = EarthyLiquidTokens.textMuted()))
                                        }
                                        IosLiquidToggle(
                                            checked = GabayHudSettings.useMph,
                                            onCheckedChange = {
                                                GabayHudSettings.useMph = it
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Convx Appearance & Liquid Glass Design System Section
                        item {
                            val context = LocalContext.current
                            Text("APPEARANCE & LIQUID GLASS", style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = EarthyLiquidTokens.textMuted()))
                            Spacer(modifier = Modifier.height(6.dp))
                            Card(
                                shape = EarthyLiquidTokens.SmallPillShape,
                                colors = CardDefaults.cardColors(containerColor = EarthyLiquidTokens.glassBg()),
                                border = CardDefaults.outlinedCardBorder().copy(brush = EarthyLiquidTokens.glassBorderBrush(), width = 0.8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                                    // Master Glass Toggle
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Enable Liquid Glass", style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = EarthyLiquidTokens.textPrimary()))
                                            Text("Hardware background blur, specular reflection, and translucent iOS materials", style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 11.sp, color = EarthyLiquidTokens.textMuted()))
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        GlassSwitch(
                                            checked = GabayGlassSettings.globalEnabled,
                                            onCheckedChange = {
                                                GabayGlassSettings.globalEnabled = it
                                                GabayGlassSettings.save(context)
                                            }
                                        )
                                    }

                                    if (GabayGlassSettings.globalEnabled) {
                                        HorizontalDivider(color = EarthyLiquidTokens.glassBorder(), thickness = 0.5.dp)

                                        // Glass Rendering Style Selector (LIQUID, BLUR, TRANSLUCENT)
                                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                            Text("Glass Material Style", style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = EarthyLiquidTokens.textPrimary()))
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                listOf(
                                                    Triple(GlassStyle.LIQUID, "Liquid", "Refractive + Rim"),
                                                    Triple(GlassStyle.BLUR, "Frosted", "Blur Only"),
                                                    Triple(GlassStyle.TRANSLUCENT, "Minimal", "Low GPU")
                                                ).forEach { (st, label, _) ->
                                                    val isSel = GabayGlassSettings.style == st
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .background(if (isSel) EarthyLiquidTokens.accent().copy(alpha = 0.22f) else (if (GabayColors.isSunlightLightMode) Color(0xFFF1F5F9) else Color(0x33FFFFFF)))
                                                            .border(0.8.dp, if (isSel) EarthyLiquidTokens.accent() else EarthyLiquidTokens.glassBorder(), RoundedCornerShape(10.dp))
                                                            .clickable {
                                                                GabayGlassSettings.style = st
                                                                GabayGlassSettings.save(context)
                                                            }
                                                            .padding(vertical = 8.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(label, style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal, fontSize = 11.sp, color = if (isSel) EarthyLiquidTokens.accent() else EarthyLiquidTokens.textPrimary()))
                                                    }
                                                }
                                            }
                                        }

                                        HorizontalDivider(color = EarthyLiquidTokens.glassBorder(), thickness = 0.5.dp)

                                        // Specular Rim Highlight Toggle
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("Specular Rim Highlight", style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = EarthyLiquidTokens.textPrimary()))
                                                Text("Top edge specular arc and physical light refraction", style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 11.sp, color = EarthyLiquidTokens.textMuted()))
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            GlassSwitch(
                                                checked = GabayGlassSettings.specularRimEnabled,
                                                onCheckedChange = {
                                                    GabayGlassSettings.specularRimEnabled = it
                                                    GabayGlassSettings.save(context)
                                                }
                                            )
                                        }

                                        HorizontalDivider(color = EarthyLiquidTokens.glassBorder(), thickness = 0.5.dp)

                                        // Surface Opacity Slider
                                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Surface Translucency", style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = EarthyLiquidTokens.textPrimary()))
                                                Text("${(GabayGlassSettings.surfaceOpacity * 100).roundToInt()}%", style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = EarthyLiquidTokens.accent()))
                                            }
                                            Slider(
                                                value = GabayGlassSettings.surfaceOpacity,
                                                onValueChange = {
                                                    GabayGlassSettings.surfaceOpacity = it
                                                },
                                                onValueChangeFinished = {
                                                    GabayGlassSettings.save(context)
                                                },
                                                valueRange = 0.30f..0.95f,
                                                colors = SliderDefaults.colors(
                                                    thumbColor = EarthyLiquidTokens.accent(),
                                                    activeTrackColor = EarthyLiquidTokens.accent()
                                                )
                                            )
                                        }

                                        HorizontalDivider(color = EarthyLiquidTokens.glassBorder(), thickness = 0.5.dp)

                                        // Blur Strength Slider
                                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Background Blur Radius", style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = EarthyLiquidTokens.textPrimary()))
                                                Text("${GabayGlassSettings.blurRadiusDp.roundToInt()} dp", style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = EarthyLiquidTokens.accent()))
                                            }
                                            Slider(
                                                value = GabayGlassSettings.blurRadiusDp,
                                                onValueChange = {
                                                    GabayGlassSettings.blurRadiusDp = it
                                                },
                                                onValueChangeFinished = {
                                                    GabayGlassSettings.save(context)
                                                },
                                                valueRange = 4f..28f,
                                                colors = SliderDefaults.colors(
                                                    thumbColor = EarthyLiquidTokens.accent(),
                                                    activeTrackColor = EarthyLiquidTokens.accent()
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Voice Engine Selection
                        item {
                            Text("VOICE GUIDANCE ENGINE", style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = EarthyLiquidTokens.textMuted()))
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    Pair(VoiceEngineType.SYSTEM_TTS, "System TTS"),
                                    Pair(VoiceEngineType.TACTICAL_EXTERNAL, "Tactical Cues")
                                ).forEach { (engine, label) ->
                                    val isSel = currentVoiceEngine == engine
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(EarthyLiquidTokens.SmallPillShape)
                                            .background(if (isSel) EarthyLiquidTokens.accent().copy(alpha = 0.2f) else (if (GabayColors.isSunlightLightMode) Color(0xFFF1F5F9) else Color(0x33FFFFFF)))
                                            .border(0.8.dp, if (isSel) EarthyLiquidTokens.accent() else EarthyLiquidTokens.glassBorder(), EarthyLiquidTokens.SmallPillShape)
                                            .clickable {
                                                currentVoiceEngine = engine
                                                audioController.voiceEngine = engine
                                            }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(label, style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = if (isSel) EarthyLiquidTokens.accent() else EarthyLiquidTokens.textPrimary()))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    val sample = if (currentVoiceEngine == VoiceEngineType.TACTICAL_EXTERNAL) "turn left" else "Turn left in 200 meters"
                                    audioController.testVoiceCue(sample)
                                },
                                shape = EarthyLiquidTokens.SmallPillShape,
                                colors = ButtonDefaults.buttonColors(containerColor = if (GabayColors.isSunlightLightMode) Color(0xFFE2E8F0) else Color(0x33FFFFFF), contentColor = EarthyLiquidTokens.textPrimary()),
                                modifier = Modifier.fillMaxWidth().height(40.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(imageVector = GabayIcons.AudioTts, contentDescription = null, tint = EarthyLiquidTokens.accent(), modifier = Modifier.size(16.dp))
                                    Text("Test Voice Audio Cue", style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 12.sp))
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // TAB 1: Drives
                    if (driveHistory.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No recorded drives yet.\nCompleted navigation trips will appear here.", style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 13.sp, color = EarthyLiquidTokens.textMuted(), textAlign = TextAlign.Center))
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(driveHistory) { drive ->
                                Card(
                                    shape = EarthyLiquidTokens.SmallPillShape,
                                    colors = CardDefaults.cardColors(containerColor = if (GabayColors.isSunlightLightMode) Color(0xFFF1F5F9) else Color(0x33FFFFFF)),
                                    border = CardDefaults.outlinedCardBorder().copy(brush = EarthyLiquidTokens.glassBorderBrush(), width = 0.8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(text = drive.title, style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = EarthyLiquidTokens.textPrimary()))
                                            Text(text = drive.date, style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 11.sp, color = EarthyLiquidTokens.textMuted()))
                                        }
                                        Text(text = "${drive.distanceKm} km • ${drive.durationMin} min", style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = EarthyLiquidTokens.accent()))
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // TAB 2: Vault Diagnostics
                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            val graphFile = File("/sdcard/Documents/Gabay/ph.gabay")
                            val graphMb = if (graphFile.exists()) "${graphFile.length() / (1024 * 1024)} MB" else "MISSING"
                            Card(
                                shape = EarthyLiquidTokens.SmallPillShape,
                                colors = CardDefaults.cardColors(containerColor = if (GabayColors.isSunlightLightMode) Color(0xFFF1F5F9) else Color(0x33FFFFFF)),
                                border = CardDefaults.outlinedCardBorder().copy(brush = EarthyLiquidTokens.glassBorderBrush(), width = 0.8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("OSM Graph Router (ph.gabay)", style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = EarthyLiquidTokens.textPrimary()))
                                        Text(if (graphFile.exists()) "READY ($graphMb)" else "MISSING", style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (graphFile.exists()) EarthyLiquidTokens.SageGreen else Color(0xFFEF4444)))
                                    }
                                    Text("A* Canonical Highway Graph for Offline Routing", style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 11.sp, color = EarthyLiquidTokens.textMuted()))
                                }
                            }
                        }
                        item {
                            val dbFile = File("/sdcard/Documents/Gabay/pois_fts.sqlite")
                            val dbMb = if (dbFile.exists()) "${dbFile.length() / (1024 * 1024)} MB" else "MISSING"
                            Card(
                                shape = EarthyLiquidTokens.SmallPillShape,
                                colors = CardDefaults.cardColors(containerColor = if (GabayColors.isSunlightLightMode) Color(0xFFF1F5F9) else Color(0x33FFFFFF)),
                                border = CardDefaults.outlinedCardBorder().copy(brush = EarthyLiquidTokens.glassBorderBrush(), width = 0.8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("SQLite Spatial FTS5 POIs", style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = EarthyLiquidTokens.textPrimary()))
                                        Text(if (dbFile.exists()) "READY ($dbMb)" else "MISSING", style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (dbFile.exists()) EarthyLiquidTokens.SageGreen else Color(0xFFEF4444)))
                                    }
                                    Text("Offline search over nationwide landmarks & barangays", style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 11.sp, color = EarthyLiquidTokens.textMuted()))
                                }
                            }
                        }
                        item {
                            val pmtilesFile = File("/sdcard/Documents/Gabay/philippines_vector.pmtiles")
                            val pmtilesMb = if (pmtilesFile.exists()) "${pmtilesFile.length() / (1024 * 1024)} MB" else "MISSING"
                            Card(
                                shape = EarthyLiquidTokens.SmallPillShape,
                                colors = CardDefaults.cardColors(containerColor = if (GabayColors.isSunlightLightMode) Color(0xFFF1F5F9) else Color(0x33FFFFFF)),
                                border = CardDefaults.outlinedCardBorder().copy(brush = EarthyLiquidTokens.glassBorderBrush(), width = 0.8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("PMTiles Offline Vector Map", style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = EarthyLiquidTokens.textPrimary()))
                                        Text(if (pmtilesFile.exists()) "BOUND ($pmtilesMb)" else "LOCAL", style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (pmtilesFile.exists()) EarthyLiquidTokens.SageGreen else EarthyLiquidTokens.textMuted()))
                                    }
                                    Text("Offline vector basemap with 3D extrusions", style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 11.sp, color = EarthyLiquidTokens.textMuted()))
                                }
                            }
                        }
                        item {
                            val modelFile = File("/sdcard/Documents/Gabay/model.gguf")
                            val modelMb = if (modelFile.exists()) "${modelFile.length() / (1024 * 1024)} MB" else "MISSING"
                            Card(
                                shape = EarthyLiquidTokens.SmallPillShape,
                                colors = CardDefaults.cardColors(containerColor = if (GabayColors.isSunlightLightMode) Color(0xFFF1F5F9) else Color(0x33FFFFFF)),
                                border = CardDefaults.outlinedCardBorder().copy(brush = EarthyLiquidTokens.glassBorderBrush(), width = 0.8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Neural Engine (model.gguf)", style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = EarthyLiquidTokens.textPrimary()))
                                        Text(if (modelFile.exists()) "READY ($modelMb)" else "MISSING", style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (modelFile.exists()) EarthyLiquidTokens.SageGreen else Color(0xFFEF4444)))
                                    }
                                    Text("On-device offline neural intelligence for voice intents & co-pilot dialogue", style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 11.sp, color = EarthyLiquidTokens.textMuted()))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
