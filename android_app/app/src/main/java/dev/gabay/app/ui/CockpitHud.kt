package dev.gabay.app.ui

import android.content.Context
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
 * Apple Design System & Fluid Glass Surfaces
 */
private object AppleUiTokens {
    val GlassDark = Color(0xDD181B24)
    val GlassLight = Color(0xF4FFFFFF)
    val GlassBorderDark = Color(0x2EFFFFFF)
    val GlassBorderLight = Color(0x1F000000)
    val AppleBlue = Color(0xFF0A84FF)
    val AppleGreen = Color(0xFF30D158)
    val AppleGreenDark = Color(0xFF249D42)
    val AppleRed = Color(0xFFFF453A)
    val AppleAmber = Color(0xFFFF9F0A)
    val TextPrimaryDark = Color(0xFFFFFFFF)
    val TextSecondaryDark = Color(0x99EBEBF5)
    val TextMutedDark = Color(0x66EBEBF5)
    val CardShape = RoundedCornerShape(22.dp)
    val PillShape = RoundedCornerShape(26.dp)
    val SmallPillShape = RoundedCornerShape(16.dp)

    fun glassBg(): Color = if (GabayColors.isSunlightLightMode) GlassLight else GlassDark
    fun glassBorder(): Color = if (GabayColors.isSunlightLightMode) GlassBorderLight else GlassBorderDark
    fun textPrimary(): Color = if (GabayColors.isSunlightLightMode) Color(0xFF0F172A) else TextPrimaryDark
    fun textSecondary(): Color = if (GabayColors.isSunlightLightMode) Color(0xFF475569) else TextSecondaryDark
    fun textMuted(): Color = if (GabayColors.isSunlightLightMode) Color(0xFF94A3B8) else TextMutedDark
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
 * Apple Maps Two-Way Origin to Destination Header
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
            .shadow(elevation = 10.dp, shape = AppleUiTokens.CardShape),
        shape = AppleUiTokens.CardShape,
        colors = CardDefaults.cardColors(containerColor = AppleUiTokens.glassBg()),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(if (isLoading) AppleUiTokens.AppleAmber.copy(alpha = 0.8f) else AppleUiTokens.glassBorder()),
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
                // Left Route Visual Indicator (Apple Maps Origin Ring -> Vertical Connector -> Destination Pin)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 2.dp, end = 12.dp)
                ) {
                    // Origin Teal Ring
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(Color.Transparent, CircleShape)
                            .border(2.5.dp, Color(0xFF00C7BE), CircleShape)
                    )
                    // Sleek connector dots
                    Box(
                        modifier = Modifier
                            .padding(vertical = 3.dp)
                            .width(1.5.dp)
                            .height(16.dp)
                            .background(AppleUiTokens.textMuted().copy(alpha = 0.35f))
                    )
                    // Destination Coral Red Dot
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(AppleUiTokens.AppleRed, CircleShape)
                    )
                }

                // Center Origin & Destination Names
                Column(modifier = Modifier.weight(1f)) {
                    // Origin Row
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
                                color = AppleUiTokens.textPrimary()
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Destination Row
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
                                color = AppleUiTokens.textPrimary()
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Right Action Buttons: Swap [⇅] and Close [✕]
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
                                tint = AppleUiTokens.textPrimary(),
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
                            tint = AppleUiTokens.textPrimary(),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Animated Loading Progress Bar
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp),
                    color = AppleUiTokens.AppleAmber,
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
            .shadow(8.dp, AppleUiTokens.PillShape),
        shape = AppleUiTokens.PillShape,
        colors = CardDefaults.cardColors(containerColor = AppleUiTokens.glassBg()),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(AppleUiTokens.glassBorder()), width = 0.8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = AppleUiTokens.AppleAmber
            )
            Text(
                text = "Calculating fastest route...",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = AppleUiTokens.textPrimary()
                )
            )
        }
    }
}

/**
 * Apple Maps / CarPlay Fluid Search Bar Capsule
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
    hint: String = "Search places, roads, landmarks...",
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
                .shadow(elevation = 12.dp, shape = AppleUiTokens.PillShape),
            shape = AppleUiTokens.PillShape,
            colors = CardDefaults.cardColors(containerColor = AppleUiTokens.glassBg()),
            border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(AppleUiTokens.glassBorder()), width = 0.8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Button: Back or Menu
                if (onCancelSearch != null) {
                    IconButton(
                        onClick = onCancelSearch,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = GabayIcons.ChevronLeft,
                            contentDescription = "Back",
                            tint = AppleUiTokens.textPrimary(),
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
                            tint = AppleUiTokens.textPrimary(),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Search Input Field
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
                                color = AppleUiTokens.textMuted()
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
                            color = AppleUiTokens.textPrimary()
                        ),
                        cursorBrush = SolidColor(AppleUiTokens.AppleAmber),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Clear Query Action
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
                            contentDescription = "Clear search",
                            tint = AppleUiTokens.textMuted(),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Audio Guidance Toggle Button
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
                        AudioGuidanceMode.FULL_VOICE -> AppleUiTokens.AppleGreen
                        AudioGuidanceMode.ALERTS_ONLY -> AppleUiTokens.AppleAmber
                        AudioGuidanceMode.MUTED -> AppleUiTokens.AppleRed
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

        // Quick Category Pills Row (Apple Maps Style)
        if (query.isEmpty() && searchResults.isEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val categories = listOf(
                    "⛽ Gas" to "gas station",
                    "☕ Coffee" to "coffee",
                    "🍔 Food" to "restaurant",
                    "🅿️ Parking" to "parking",
                    "🛣️ Tollway" to "expressway toll",
                    "🏥 Hospital" to "hospital",
                    "🏪 7-Eleven" to "convenience store"
                )
                categories.forEach { (label, keyword) ->
                    Box(
                        modifier = Modifier
                            .clip(AppleUiTokens.SmallPillShape)
                            .background(AppleUiTokens.glassBg())
                            .border(0.8.dp, AppleUiTokens.glassBorder(), AppleUiTokens.SmallPillShape)
                            .clickable {
                                onQueryChange(keyword)
                                onCategoryClick(keyword)
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = label,
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = AppleUiTokens.textPrimary()
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
                    .shadow(16.dp, AppleUiTokens.CardShape),
                shape = AppleUiTokens.CardShape,
                colors = CardDefaults.cardColors(containerColor = AppleUiTokens.glassBg()),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(AppleUiTokens.glassBorder()), width = 0.8.dp)
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
                                tint = AppleUiTokens.AppleAmber,
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
                                        color = AppleUiTokens.textPrimary()
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
                                        color = AppleUiTokens.textSecondary()
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        HorizontalDivider(color = AppleUiTokens.glassBorder(), thickness = 0.5.dp)
                    }
                }
            }
        } else if (query.isEmpty() && recentDestinations.isNotEmpty() && isSearchFocused) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .shadow(16.dp, AppleUiTokens.CardShape),
                shape = AppleUiTokens.CardShape,
                colors = CardDefaults.cardColors(containerColor = AppleUiTokens.glassBg()),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(AppleUiTokens.glassBorder()), width = 0.8.dp)
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
                                color = AppleUiTokens.textMuted()
                            )
                        )
                        Text(
                            text = "Clear",
                            modifier = Modifier.clickable { onClearRecents() },
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = AppleUiTokens.AppleBlue
                            )
                        )
                    }
                    HorizontalDivider(color = AppleUiTokens.glassBorder(), thickness = 0.5.dp)
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
                                    tint = AppleUiTokens.textMuted(),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = recent.name,
                                    style = TextStyle(
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = AppleUiTokens.textPrimary()
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            HorizontalDivider(color = AppleUiTokens.glassBorder(), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Apple CarPlay Bottom Telemetry & Speedometer Deck
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
            .shadow(elevation = 12.dp, shape = AppleUiTokens.PillShape),
        shape = AppleUiTokens.PillShape,
        colors = CardDefaults.cardColors(containerColor = AppleUiTokens.glassBg()),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(AppleUiTokens.glassBorder()), width = 0.8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (hasActiveTurn) {
                // Maneuver Glyph Pod
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(AppleUiTokens.AppleGreen.copy(alpha = 0.18f), CircleShape)
                        .border(1.dp, AppleUiTokens.AppleGreen.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = resolveManeuverIcon(nextTurnIcon),
                        contentDescription = "Next Turn",
                        tint = AppleUiTokens.AppleGreen,
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
                                color = AppleUiTokens.textPrimary()
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Next maneuver",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 10.sp,
                                color = AppleUiTokens.textMuted()
                            )
                        )
                    }

                    // Vertical Hairline Divider
                    Box(
                        modifier = Modifier
                            .width(0.8.dp)
                            .height(28.dp)
                            .background(AppleUiTokens.glassBorder())
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
                        color = AppleUiTokens.textPrimary()
                    )
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = displayUnit,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = AppleUiTokens.AppleAmber
                    ),
                    modifier = Modifier.padding(bottom = 5.dp)
                )
            }
        }
    }
}

/**
 * Minimalist Apple Glass Telemetry Capsule
 */
@Composable
fun FloatingTelemetryPill(
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .shadow(elevation = 8.dp, shape = AppleUiTokens.PillShape),
        shape = AppleUiTokens.PillShape,
        colors = CardDefaults.cardColors(containerColor = AppleUiTokens.glassBg()),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(AppleUiTokens.glassBorder()), width = 0.8.dp)
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
                    color = AppleUiTokens.textPrimary()
                )
            )
            Text(
                text = unit,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    color = AppleUiTokens.AppleAmber
                )
            )
        }
    }
}

/**
 * Apple-style Map Layer Selector
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
        contentAlignment = Alignment.BottomStart
    ) {
        AnimatedVisibility(
            visible = isExpanded,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 56.dp)
                .wrapContentSize(Alignment.BottomStart, unbounded = true),
            enter = fadeIn(tween(180)) + slideInVertically(initialOffsetY = { it / 3 }),
            exit = fadeOut(tween(140)) + slideOutVertically(targetOffsetY = { it / 3 })
        ) {
            Card(
                shape = AppleUiTokens.CardShape,
                colors = CardDefaults.cardColors(containerColor = AppleUiTokens.glassBg()),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(AppleUiTokens.glassBorder()), width = 0.8.dp),
                modifier = Modifier.shadow(16.dp, AppleUiTokens.CardShape)
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
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) AppleUiTokens.AppleBlue.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable {
                                    onSelectMode(mode)
                                    isExpanded = false
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isSelected) AppleUiTokens.AppleBlue else AppleUiTokens.textPrimary(),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = label,
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = if (isSelected) AppleUiTokens.AppleBlue else AppleUiTokens.textPrimary()
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
            modifier = Modifier.align(Alignment.BottomStart)
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
            .background(if (is3d) AppleUiTokens.AppleAmber.copy(alpha = 0.22f) else AppleUiTokens.glassBg(), CircleShape)
            .border(
                0.8.dp,
                if (is3d) AppleUiTokens.AppleAmber else AppleUiTokens.glassBorder(),
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
                color = if (is3d) AppleUiTokens.AppleAmber else AppleUiTokens.textPrimary()
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
                .background(AppleUiTokens.glassBg(), CircleShape)
                .border(0.8.dp, AppleUiTokens.glassBorder(), CircleShape)
                .clickable { onReset() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.rotate(-cameraBearing)
            ) {
                // North Needle Tip
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(10.dp)
                        .background(AppleUiTokens.AppleRed, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                )
                Spacer(modifier = Modifier.height(1.5.dp))
                // South Needle Base
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(10.dp)
                        .background(Color(0xFFE2E8F0), RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp))
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
    activeColor: Color = AppleUiTokens.AppleAmber,
    contentDescription: String? = null
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .shadow(elevation = 8.dp, shape = CircleShape)
            .background(if (isActive) activeColor.copy(alpha = 0.2f) else AppleUiTokens.glassBg(), CircleShape)
            .border(
                0.8.dp,
                if (isActive) activeColor else AppleUiTokens.glassBorder(),
                CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) activeColor else AppleUiTokens.textPrimary(),
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Apple Maps Style Route Preview Modal Sheet
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
    val viaRoad = route.maneuvers.firstOrNull()?.roadName?.ifBlank { "Optimal Route" } ?: "Optimal Route"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .shadow(elevation = 20.dp, shape = AppleUiTokens.CardShape),
        shape = AppleUiTokens.CardShape,
        colors = CardDefaults.cardColors(containerColor = AppleUiTokens.glassBg()),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(AppleUiTokens.glassBorder()), width = 0.8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag Handle Bar
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .background(AppleUiTokens.textMuted().copy(alpha = 0.4f), CircleShape)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Vehicle Mode Segmented Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (GabayColors.isSunlightLightMode) Color(0xFFF1F5F9) else Color(0x33000000), AppleUiTokens.SmallPillShape)
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
                            .clip(AppleUiTokens.SmallPillShape)
                            .background(if (isSelected) AppleUiTokens.glassBg() else Color.Transparent)
                            .border(if (isSelected) 0.8.dp else 0.dp, if (isSelected) AppleUiTokens.glassBorder() else Color.Transparent, AppleUiTokens.SmallPillShape)
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
                                tint = if (isSelected) AppleUiTokens.AppleGreen else AppleUiTokens.textSecondary(),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = label,
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.5.sp,
                                    color = if (isSelected) AppleUiTokens.textPrimary() else AppleUiTokens.textSecondary()
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
                                color = AppleUiTokens.AppleGreen
                            )
                        )
                        Text(
                            text = "(${String.format(Locale.US, "%.1f", distKm)} km)",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Normal,
                                fontSize = 15.sp,
                                color = AppleUiTokens.textSecondary()
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
                            color = AppleUiTokens.textSecondary()
                        )
                    )
                }

                // Avoidance Options Chips
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(AppleUiTokens.SmallPillShape)
                            .background(if (avoidTolls) AppleUiTokens.AppleAmber.copy(alpha = 0.2f) else Color.Transparent)
                            .border(0.8.dp, if (avoidTolls) AppleUiTokens.AppleAmber else AppleUiTokens.glassBorder(), AppleUiTokens.SmallPillShape)
                            .clickable { onToggleAvoidTolls() }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "No Tolls",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = if (avoidTolls) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp,
                                color = if (avoidTolls) AppleUiTokens.AppleAmber else AppleUiTokens.textSecondary()
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(AppleUiTokens.SmallPillShape)
                            .background(if (avoidHighways) AppleUiTokens.AppleAmber.copy(alpha = 0.2f) else Color.Transparent)
                            .border(0.8.dp, if (avoidHighways) AppleUiTokens.AppleAmber else AppleUiTokens.glassBorder(), AppleUiTokens.SmallPillShape)
                            .clickable { onToggleAvoidHighways() }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "No Highways",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = if (avoidHighways) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp,
                                color = if (avoidHighways) AppleUiTokens.AppleAmber else AppleUiTokens.textSecondary()
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Primary Actions: "START NAVIGATION" [GO] & Close [✕]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onStartNavigation,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppleUiTokens.AppleGreen,
                        contentColor = Color.White
                    ),
                    shape = AppleUiTokens.PillShape,
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
                        .border(0.8.dp, AppleUiTokens.AppleRed.copy(alpha = 0.5f), CircleShape)
                        .clickable { onCancelRoute() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = GabayIcons.ActionClose,
                        contentDescription = "Cancel",
                        tint = AppleUiTokens.AppleRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Siri / Dynamic Island-inspired Voice Co-Pilot Sheet
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
        colors = CardDefaults.cardColors(containerColor = AppleUiTokens.glassBg()),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(AppleUiTokens.glassBorder()), width = 0.8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 8.dp)
        ) {
            // Drag handle
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
                        .background(AppleUiTokens.textMuted().copy(alpha = 0.4f), CircleShape)
                )
            }

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(if (isListening) AppleUiTokens.AppleRed else AppleUiTokens.AppleAmber, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isListening) "Listening..." else "Gabay Co-Pilot",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = AppleUiTokens.textPrimary()
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
                        tint = AppleUiTokens.textSecondary(),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Real-time Waveform Meter
            if (isListening) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (GabayColors.isSunlightLightMode) Color(0xFFF1F5F9) else Color(0x33000000), AppleUiTokens.SmallPillShape)
                        .border(0.8.dp, AppleUiTokens.AppleRed.copy(alpha = 0.4f), AppleUiTokens.SmallPillShape)
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
                                    .background(AppleUiTokens.AppleRed, RoundedCornerShape(1.5.dp))
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
                            color = AppleUiTokens.textPrimary()
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Context Quick Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "Where am I?",
                    "Nearest Gas Station",
                    "3D Perspective",
                    "Satellite Mode",
                    "Cancel Route"
                ).forEach { prompt ->
                    Box(
                        modifier = Modifier
                            .clip(AppleUiTokens.SmallPillShape)
                            .background(if (GabayColors.isSunlightLightMode) Color(0xFFF1F5F9) else Color(0x33FFFFFF))
                            .border(0.8.dp, AppleUiTokens.glassBorder(), AppleUiTokens.SmallPillShape)
                            .clickable { onSendPrompt(prompt) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = prompt,
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 12.sp,
                                color = AppleUiTokens.textPrimary()
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Chat Message List
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
                                    color = AppleUiTokens.textMuted(),
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
                                    containerColor = if (isUser) AppleUiTokens.AppleBlue else (if (GabayColors.isSunlightLightMode) Color(0xFFF1F5F9) else Color(0x33FFFFFF))
                                ),
                                border = if (isUser) null else CardDefaults.outlinedCardBorder().copy(brush = SolidColor(AppleUiTokens.glassBorder()), width = 0.8.dp),
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    Text(
                                        text = msg.text,
                                        style = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontSize = 13.sp,
                                            color = if (isUser) Color.White else AppleUiTokens.textPrimary()
                                        )
                                    )
                                    Text(
                                        text = msg.timestamp,
                                        style = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontSize = 9.sp,
                                            color = if (isUser) Color.White.copy(alpha = 0.7f) else AppleUiTokens.textMuted()
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

            // Bottom Input Capsule
            Card(
                shape = AppleUiTokens.PillShape,
                colors = CardDefaults.cardColors(containerColor = if (GabayColors.isSunlightLightMode) Color(0xFFF1F5F9) else Color(0x33FFFFFF)),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(AppleUiTokens.glassBorder()), width = 0.8.dp),
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
                                    color = AppleUiTokens.textMuted()
                                )
                            )
                        }
                        BasicTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            textStyle = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 14.sp,
                                color = AppleUiTokens.textPrimary()
                            ),
                            cursorBrush = SolidColor(AppleUiTokens.AppleAmber),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Mic Button
                    IconButton(
                        onClick = onToggleListening,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = GabayIcons.CopilotMic,
                            contentDescription = "Mic",
                            tint = if (isListening) AppleUiTokens.AppleRed else AppleUiTokens.AppleAmber,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Send Button
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
                            tint = if (inputText.isNotBlank()) AppleUiTokens.AppleBlue else AppleUiTokens.textMuted(),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * iOS Settings Style Drawer
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
            .fillMaxHeight(0.78f)
            .offset { IntOffset(0, offsetY.roundToInt()) }
            .shadow(24.dp, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        colors = CardDefaults.cardColors(containerColor = AppleUiTokens.glassBg()),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(AppleUiTokens.glassBorder()), width = 0.8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 8.dp)
        ) {
            // Drag handle
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
                        .background(AppleUiTokens.textMuted().copy(alpha = 0.4f), CircleShape)
                )
            }

            // Header
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
                        color = AppleUiTokens.textPrimary()
                    )
                )

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = GabayIcons.ActionClose,
                        contentDescription = "Close",
                        tint = AppleUiTokens.textSecondary(),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // iOS Segmented Control Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (GabayColors.isSunlightLightMode) Color(0xFFF1F5F9) else Color(0x33000000), AppleUiTokens.SmallPillShape)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Settings", "Drives (${driveHistory.size})", "Vault").forEachIndexed { index, label ->
                    val isSel = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(AppleUiTokens.SmallPillShape)
                            .background(if (isSel) AppleUiTokens.glassBg() else Color.Transparent)
                            .border(if (isSel) 0.8.dp else 0.dp, if (isSel) AppleUiTokens.glassBorder() else Color.Transparent, AppleUiTokens.SmallPillShape)
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
                                color = if (isSel) AppleUiTokens.textPrimary() else AppleUiTokens.textMuted()
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedTab) {
                0 -> {
                    // TAB 0: Settings
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Section: Vehicle Mode
                        item {
                            Text("DEFAULT VEHICLE MODE", style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = AppleUiTokens.textMuted()))
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
                                            .clip(AppleUiTokens.SmallPillShape)
                                            .background(if (isSel) AppleUiTokens.AppleGreen.copy(alpha = 0.2f) else (if (GabayColors.isSunlightLightMode) Color(0xFFF1F5F9) else Color(0x33FFFFFF)))
                                            .border(0.8.dp, if (isSel) AppleUiTokens.AppleGreen else AppleUiTokens.glassBorder(), AppleUiTokens.SmallPillShape)
                                            .clickable { onModeSelect(mode) }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(imageVector = icon, contentDescription = null, tint = if (isSel) AppleUiTokens.AppleGreen else AppleUiTokens.textSecondary(), modifier = Modifier.size(16.dp))
                                            Text(text = label, style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = if (isSel) AppleUiTokens.AppleGreen else AppleUiTokens.textPrimary()))
                                        }
                                    }
                                }
                            }
                        }

                        // Section: Theme & Contrast
                        item {
                            Text("DISPLAY THEME", style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = AppleUiTokens.textMuted()))
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val isSunlight = GabayColors.isSunlightLightMode
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(AppleUiTokens.SmallPillShape)
                                        .background(if (!isSunlight) AppleUiTokens.AppleAmber.copy(alpha = 0.2f) else (if (GabayColors.isSunlightLightMode) Color(0xFFF1F5F9) else Color(0x33FFFFFF)))
                                        .border(0.8.dp, if (!isSunlight) AppleUiTokens.AppleAmber else AppleUiTokens.glassBorder(), AppleUiTokens.SmallPillShape)
                                        .clickable { onThemeToggle(false) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Tactical Dark", style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = if (!isSunlight) AppleUiTokens.AppleAmber else AppleUiTokens.textPrimary()))
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(AppleUiTokens.SmallPillShape)
                                        .background(if (isSunlight) AppleUiTokens.AppleAmber.copy(alpha = 0.2f) else (if (GabayColors.isSunlightLightMode) Color(0xFFF1F5F9) else Color(0x33FFFFFF)))
                                        .border(0.8.dp, if (isSunlight) AppleUiTokens.AppleAmber else AppleUiTokens.glassBorder(), AppleUiTokens.SmallPillShape)
                                        .clickable { onThemeToggle(true) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Sunlight Bright", style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = if (isSunlight) AppleUiTokens.AppleAmber else AppleUiTokens.textPrimary()))
                                }
                            }
                        }

                        // Section: Voice Engine
                        item {
                            Text("VOICE GUIDANCE ENGINE", style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = AppleUiTokens.textMuted()))
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
                                            .clip(AppleUiTokens.SmallPillShape)
                                            .background(if (isSel) AppleUiTokens.AppleAmber.copy(alpha = 0.2f) else (if (GabayColors.isSunlightLightMode) Color(0xFFF1F5F9) else Color(0x33FFFFFF)))
                                            .border(0.8.dp, if (isSel) AppleUiTokens.AppleAmber else AppleUiTokens.glassBorder(), AppleUiTokens.SmallPillShape)
                                            .clickable {
                                                currentVoiceEngine = engine
                                                audioController.voiceEngine = engine
                                            }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(label, style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = if (isSel) AppleUiTokens.AppleAmber else AppleUiTokens.textPrimary()))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    val sample = if (currentVoiceEngine == VoiceEngineType.TACTICAL_EXTERNAL) "turn left" else "Turn left in 200 meters"
                                    audioController.testVoiceCue(sample)
                                },
                                shape = AppleUiTokens.SmallPillShape,
                                colors = ButtonDefaults.buttonColors(containerColor = if (GabayColors.isSunlightLightMode) Color(0xFFE2E8F0) else Color(0x33FFFFFF), contentColor = AppleUiTokens.textPrimary()),
                                modifier = Modifier.fillMaxWidth().height(40.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(imageVector = GabayIcons.AudioTts, contentDescription = null, tint = AppleUiTokens.AppleAmber, modifier = Modifier.size(16.dp))
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
                            Text("No recorded drives yet.\nCompleted navigation trips will appear here.", style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 13.sp, color = AppleUiTokens.textMuted(), textAlign = TextAlign.Center))
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(driveHistory) { drive ->
                                Card(
                                    shape = AppleUiTokens.SmallPillShape,
                                    colors = CardDefaults.cardColors(containerColor = if (GabayColors.isSunlightLightMode) Color(0xFFF1F5F9) else Color(0x33FFFFFF)),
                                    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(AppleUiTokens.glassBorder()), width = 0.8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(text = drive.title, style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = AppleUiTokens.textPrimary()))
                                            Text(text = drive.date, style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 11.sp, color = AppleUiTokens.textMuted()))
                                        }
                                        Text(text = "${drive.distanceKm} km • ${drive.durationMin} min", style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AppleUiTokens.AppleGreen))
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
                                shape = AppleUiTokens.SmallPillShape,
                                colors = CardDefaults.cardColors(containerColor = if (GabayColors.isSunlightLightMode) Color(0xFFF1F5F9) else Color(0x33FFFFFF)),
                                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(AppleUiTokens.glassBorder()), width = 0.8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("OSM Graph Router (ph.gabay)", style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AppleUiTokens.textPrimary()))
                                        Text(if (graphFile.exists()) "READY ($graphMb)" else "MISSING", style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (graphFile.exists()) AppleUiTokens.AppleGreen else AppleUiTokens.AppleRed))
                                    }
                                    Text("A* Canonical Highway Graph for Offline Routing", style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 11.sp, color = AppleUiTokens.textMuted()))
                                }
                            }
                        }
                        item {
                            val dbFile = File("/sdcard/Documents/Gabay/pois_fts.sqlite")
                            val dbMb = if (dbFile.exists()) "${dbFile.length() / (1024 * 1024)} MB" else "MISSING"
                            Card(
                                shape = AppleUiTokens.SmallPillShape,
                                colors = CardDefaults.cardColors(containerColor = if (GabayColors.isSunlightLightMode) Color(0xFFF1F5F9) else Color(0x33FFFFFF)),
                                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(AppleUiTokens.glassBorder()), width = 0.8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("SQLite Spatial FTS5 POIs", style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AppleUiTokens.textPrimary()))
                                        Text(if (dbFile.exists()) "READY ($dbMb)" else "MISSING", style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (dbFile.exists()) AppleUiTokens.AppleGreen else AppleUiTokens.AppleRed))
                                    }
                                    Text("Offline search over nationwide landmarks & barangays", style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 11.sp, color = AppleUiTokens.textMuted()))
                                }
                            }
                        }
                        item {
                            val pmtilesFile = File("/sdcard/Documents/Gabay/philippines_vector.pmtiles")
                            val pmtilesMb = if (pmtilesFile.exists()) "${pmtilesFile.length() / (1024 * 1024)} MB" else "MISSING"
                            Card(
                                shape = AppleUiTokens.SmallPillShape,
                                colors = CardDefaults.cardColors(containerColor = if (GabayColors.isSunlightLightMode) Color(0xFFF1F5F9) else Color(0x33FFFFFF)),
                                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(AppleUiTokens.glassBorder()), width = 0.8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("PMTiles Offline Vector Map", style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AppleUiTokens.textPrimary()))
                                        Text(if (pmtilesFile.exists()) "BOUND ($pmtilesMb)" else "LOCAL", style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (pmtilesFile.exists()) AppleUiTokens.AppleGreen else AppleUiTokens.textMuted()))
                                    }
                                    Text("Offline vector basemap with 3D extrusions", style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 11.sp, color = AppleUiTokens.textMuted()))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
