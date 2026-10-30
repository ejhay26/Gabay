package dev.gabay.app.ui

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import java.util.Locale

data class ChatMessage(
    val sender: String, // "RIDER" or "GABAY AGENT"
    val text: String,
    val timestamp: String
)

data class DriveHistoryItem(
    val title: String,
    val distanceKm: Float,
    val durationMin: Int,
    val date: String
)

/**
 * Two-Way Origin to Destination Header (Google Maps Style + Apple OS Fluidity)
 * Features:
 * - Origin location row with Cyan indicator ring
 * - 3 vertical connector dots linking origin to destination
 * - Destination pin row with Red dropped pin indicator
 * - Dynamic Swap button [⇅] using Phosphor Bold icon to reverse directions
 * - Cancel Route button [✕]
 * - Elevated high-contrast card styling with subtle mint border
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
            .padding(horizontal = 16.dp, vertical = 2.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = GabayColors.UiCardElevated),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(if (isLoading) GabayColors.AccentPrimary.copy(alpha = 0.6f) else GabayColors.UiBorder)
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Column: Origin Dot, 3 Vertical Dots Connector, Destination Pin
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.width(22.dp)
                ) {
                    // Origin location ring
                    Box(
                        modifier = Modifier
                            .size(13.dp)
                            .background(Color.Transparent, CircleShape)
                            .border(2.5.dp, GabayColors.AccentPrimary, CircleShape)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    // 3 subtle vertical connector dots
                    Box(modifier = Modifier.size(2.dp).background(Color(0xFF64748B), CircleShape))
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(modifier = Modifier.size(2.dp).background(Color(0xFF64748B), CircleShape))
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(modifier = Modifier.size(2.dp).background(Color(0xFF64748B), CircleShape))
                    Spacer(modifier = Modifier.height(2.dp))
                    // Destination pin
                    Icon(
                        imageVector = GabayIcons.DestinationPin,
                        contentDescription = "Destination Pin",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Middle Column: Origin & Destination Names (Clickable to edit Point A or Point B)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (onPickOrigin != null && !isNavigating) {
                                    Modifier.clickable { onPickOrigin() }
                                } else Modifier
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = originName,
                            style = TextStyle(
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = GabayColors.TextPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (onPickOrigin != null && !isNavigating) {
                            Text(
                                text = "CHANGE",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GabayColors.TextMuted
                                ),
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(GabayColors.UiBorder.copy(alpha = 0.5f))
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (onPickDestination != null && !isNavigating) {
                                    Modifier.clickable { onPickDestination() }
                                } else Modifier
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = destinationName,
                            style = TextStyle(
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = GabayColors.AccentPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (onPickDestination != null && !isNavigating) {
                            Text(
                                text = "CHANGE",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GabayColors.AccentPrimary.copy(alpha = 0.7f)
                                ),
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                    if (isLoading) {
                        Text(
                            text = "Finding best route via road network...",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Normal,
                                fontSize = 10.sp,
                                color = GabayColors.TextSecondary
                            ),
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Right Column: Swap Action [⇅] (Only in Preview) and Close Action [✕]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!isNavigating) {
                        // Reverse origin <-> destination (Hidden during active navigation to prevent button collision)
                        IconButton(
                            onClick = onSwap,
                            enabled = !isLoading,
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (isLoading) GabayColors.UiInput.copy(alpha = 0.4f) else GabayColors.UiInput,
                                    CircleShape
                                )
                                .border(1.dp, GabayColors.UiBorder, CircleShape)
                        ) {
                            Icon(
                                imageVector = GabayIcons.ModeSwap,
                                contentDescription = "Swap Origin and Destination",
                                tint = if (isLoading) GabayColors.AccentPrimary.copy(alpha = 0.4f) else GabayColors.AccentPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Close / Cancel route / Stop navigation
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (GabayColors.isSunlightLightMode) Color(0xFFFEE2E2) else Color(0x33FF3B30),
                                CircleShape
                            )
                            .border(
                                1.dp,
                                if (GabayColors.isSunlightLightMode) Color(0xFFEF4444) else Color(0xFFFF3B30).copy(alpha = 0.5f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = GabayIcons.ActionClose,
                            contentDescription = if (isNavigating) "Stop Navigation" else "Cancel Route",
                            tint = if (GabayColors.isSunlightLightMode) Color(0xFFB91C1C) else Color(0xFFFF3B30),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Indeterminate sleek cyan loading progress line
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = GabayColors.AccentPrimary,
                    trackColor = GabayColors.UiCardElevated
                )
            }
        }
    }
}

/**
 * Non-blocking Route Calculation Loading Indicator
 * Displays a fluid HUD status banner with spinning mint indicator during background pathfinding.
 */
@Composable
fun RouteCalculatingIndicator(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = GabayColors.UiCardElevated),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(GabayColors.AccentAmber.copy(alpha = 0.8f)))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = GabayColors.AccentAmber,
                strokeWidth = 2.5.dp
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "OPTIMIZING NAVIGATION ROUTE...",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = GabayColors.AccentAmber
                    )
                )
                Text(
                    text = "Snapping to nearest road centerline & computing trajectory",
                    style = TextStyle(
                        fontFamily = FontFamily.Default,
                        fontSize = 11.sp,
                        color = GabayColors.TextSecondary
                    )
                )
            }
        }
    }
}
/**
 * Top Search Bar (Google Maps Style + Apple OS Fluidity)
 * Includes Menu icon, live search text field, quick category chips,
 * Google Maps style Recent Searches dropdown, and SQLite FTS5 live suggestions.
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
            .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 4.dp)
    ) {
        // Floating Rounded Search Bar Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(GabayColors.UiCardGlass, RoundedCornerShape(26.dp))
                .border(1.dp, GabayColors.UiBorder, RoundedCornerShape(26.dp))
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Back Button (if custom search) or Circular Menu Button
            if (onCancelSearch != null) {
                IconButton(
                    onClick = onCancelSearch,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = GabayIcons.ChevronLeft,
                        contentDescription = "Back",
                        tint = GabayColors.TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                // ── Hexagonal tactical menu button ─────────────────────────────────
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(GabayShapes.Hexagon)
                        .background(GabayColors.UiCardElevated)
                        .border(1.dp, GabayColors.AccentAmber.copy(alpha = 0.7f), GabayShapes.Hexagon)
                        .clickable { onMenuClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = GabayIcons.Menu,
                        contentDescription = "Menu",
                        tint = GabayColors.AccentAmber,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Middle: Search Input Field
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = hint,
                        style = GabayTypography.SearchPlaceholder,
                        maxLines = 1
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = {
                        onQueryChange(it)
                        isSearchFocused = true
                    },
                    singleLine = true,
                    textStyle = GabayTypography.SearchText,
                    cursorBrush = SolidColor(GabayColors.AccentAmber),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Right: Clear Button or HUD Audio Guidance Mode Toggle
            if (query.isNotEmpty() || isSearchFocused) {
                IconButton(
                    onClick = {
                        onQueryChange("")
                        isSearchFocused = false
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = GabayIcons.ActionClose,
                        contentDescription = "Clear",
                        tint = GabayColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                IconButton(
                    onClick = onToggleAudioMode,
                    modifier = Modifier.size(36.dp)
                ) {
                    val (icon, tint, desc) = when (audioGuidanceMode) {
                        dev.gabay.app.audio.AudioGuidanceMode.FULL_VOICE -> Triple(ApexAlonIcons.SpeakerWaves, GabayColors.AccentPrimary, "Full Voice Guidance")
                        dev.gabay.app.audio.AudioGuidanceMode.ALERTS_ONLY -> Triple(ApexAlonIcons.HazardTriangle, GabayColors.AccentAmber, "Alerts Only")
                        dev.gabay.app.audio.AudioGuidanceMode.MUTED -> Triple(GabayIcons.AudioMute, Color(0xFFEF4444), "Muted Guidance")
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = desc,
                        tint = tint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Quick Category Chips (Google Maps Style horizontal chips)
        if (query.isEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val categories = listOf(
                    Triple("Gas", "Gas", ApexAlonIcons.FuelPump),
                    Triple("Hospital", "Hospital", ApexAlonIcons.HospitalCross),
                    Triple("School", "School", ApexAlonIcons.GraduationCap),
                    Triple("Food", "Food", ApexAlonIcons.WaypointPin),
                    Triple("Civic", "Civic", ApexAlonIcons.CivicBuilding),
                    Triple("Market", "Market", ApexAlonIcons.WaypointPin)
                )
                categories.forEach { (catKey, label, icon) ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(GabayColors.UiCardGlass)
                            .border(1.dp, GabayColors.AccentAmber.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                            .clickable {
                                isSearchFocused = false
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                onCategoryClick(catKey)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = GabayColors.AccentAmber,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = label,
                            style = TextStyle(
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = GabayColors.TextPrimary
                            )
                        )
                    }
                }
            }
        }

        // Use Current Location Card (When picking Point A)
        if (onUseCurrentLocation != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GabayColors.UiCardElevated),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(GabayColors.AccentAmber.copy(alpha = 0.6f)))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onUseCurrentLocation()
                            isSearchFocused = false
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(GabayColors.AccentAmber.copy(alpha = 0.2f), CircleShape)
                            .border(1.dp, GabayColors.AccentAmber, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = GabayIcons.Crosshairs,
                            contentDescription = null,
                            tint = GabayColors.AccentAmber,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Your current location",
                            style = TextStyle(
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = GabayColors.AccentAmber
                            )
                        )
                        Text(
                            text = "GPS real-time position as start point",
                            style = TextStyle(
                                fontFamily = FontFamily.Default,
                                fontSize = 11.5.sp,
                                color = GabayColors.TextSecondary
                            )
                        )
                    }
                }
            }
        }

        // Google Maps Style Recently Viewed / Searches Dropdown
        AnimatedVisibility(
            visible = query.isEmpty() && isSearchFocused && recentDestinations.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = GabayColors.UiCardElevated),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(GabayColors.UiBorder))
            ) {
                Column(modifier = Modifier.padding(vertical = 10.dp)) {
                    // Header row: Recent Searches with Clear All
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = ApexAlonIcons.ClockHistory,
                                contentDescription = null,
                                tint = GabayColors.TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "RECENT SEARCHES",
                                style = TextStyle(
                                    fontFamily = FontFamily.Default,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.5.sp,
                                    color = GabayColors.TextSecondary
                                )
                            )
                        }
                        Text(
                            text = "CLEAR ALL",
                            style = TextStyle(
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = GabayColors.AccentPrimary
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onClearRecents() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(GabayColors.UiBorder.copy(alpha = 0.5f))
                            .padding(top = 4.dp, bottom = 4.dp)
                    )

                    recentDestinations.take(5).forEach { recent ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectRecent(recent)
                                    isSearchFocused = false
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(GabayColors.UiInput, CircleShape)
                                    .border(1.dp, GabayColors.UiBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (recent.category.lowercase(Locale.ROOT)) {
                                        "hospital" -> ApexAlonIcons.HospitalCross
                                        "school" -> ApexAlonIcons.GraduationCap
                                        "gas" -> ApexAlonIcons.FuelPump
                                        "government", "civic" -> ApexAlonIcons.CivicBuilding
                                        else -> ApexAlonIcons.ClockHistory
                                    },
                                    contentDescription = null,
                                    tint = GabayColors.AccentPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = recent.name,
                                    style = TextStyle(
                                        fontFamily = FontFamily.Default,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = GabayColors.TextPrimary
                                    ),
                                    maxLines = 1
                                )
                                Text(
                                    text = recent.subtitle,
                                    style = TextStyle(
                                        fontFamily = FontFamily.Default,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 11.5.sp,
                                        color = GabayColors.TextSecondary
                                    ),
                                    maxLines = 1
                                )
                            }
                            Icon(
                                imageVector = ApexAlonIcons.RoadCenterline,
                                contentDescription = "Route",
                                tint = GabayColors.TextSecondary.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Live Search Results Dropdown (SQLite FTS5 Results)
        AnimatedVisibility(
            visible = query.isNotBlank() && searchResults.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = GabayColors.UiCardElevated),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(GabayColors.UiBorder))
            ) {
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    searchResults.take(6).forEach { place ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectPlace(place)
                                    isSearchFocused = false
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(GabayColors.UiInput, CircleShape)
                                    .border(1.dp, GabayColors.UiBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (place.category.lowercase(Locale.ROOT)) {
                                        "hospital" -> ApexAlonIcons.HospitalCross
                                        "school", "university", "college" -> ApexAlonIcons.GraduationCap
                                        "gas" -> ApexAlonIcons.FuelPump
                                        "government", "administrative" -> ApexAlonIcons.CivicBuilding
                                        else -> ApexAlonIcons.WaypointPin
                                    },
                                    contentDescription = null,
                                    tint = GabayColors.AccentPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = place.name,
                                    style = GabayTypography.StreetName.copy(fontSize = 14.sp),
                                    color = GabayColors.TextPrimary,
                                    maxLines = 1
                                )
                                Text(
                                    text = place.category,
                                    style = GabayTypography.MonospaceLabel.copy(fontSize = 11.sp),
                                    color = GabayColors.TextMuted,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Redesigned Middle Bottom Speedometer (Number Only + MPH + Turn Arrows)
 * Matches the user requirement with rock-solid fixed geometry:
 * - Fixed 140dp width with zero horizontal layout shift or expansion.
 * - Flanking turn arrows reside in fixed 22dp bays so container size NEVER changes.
 * - Live speed number centered with high-contrast amber/white styling.
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

    val displayPrimary = if (GabayHudSettings.useMph) "$speedMph" else "$speedInt"
    val displayPrimaryUnit = if (GabayHudSettings.useMph) "MPH" else "KM/H"
    val displaySecondary = if (GabayHudSettings.useMph) "$speedInt km/h" else "$speedMph mph"

    val isTurnLeft = GabayHudSettings.showTurnArrows && nextTurnIcon.contains("left")
    val isTurnRight = GabayHudSettings.showTurnArrows && nextTurnIcon.contains("right")

    // Infinite transition for turn arrow pulse
    val infiniteTransition = rememberInfiniteTransition(label = "TurnArrowPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    )

    Column(
        modifier = modifier
            .requiredWidth(140.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(GabayColors.UiCardGlass, RoundedCornerShape(28.dp))
            .border(1.2.dp, GabayColors.AccentAmber.copy(alpha = 0.55f), RoundedCornerShape(28.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Amber top accent pill
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(2.5.dp)
                .background(GabayColors.AccentAmber.copy(alpha = 0.7f), RoundedCornerShape(1.5.dp))
        )
        Spacer(modifier = Modifier.height(2.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Turn Bay (Fixed 20dp width, zero layout displacement)
            Box(
                modifier = Modifier.requiredSize(20.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isTurnLeft) {
                    Icon(
                        imageVector = GabayIcons.TurnArrowLeft,
                        contentDescription = "Turn Left",
                        tint = GabayColors.AccentAmber.copy(alpha = pulseAlpha),
                        modifier = Modifier.requiredSize(18.dp)
                    )
                }
            }

            // Central Speed Readout
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = displayPrimary,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 32.sp,
                        letterSpacing = (-1.5).sp,
                        color = GabayColors.TextPrimary
                    )
                )
                Spacer(modifier = Modifier.width(3.dp))
                Column(
                    modifier = Modifier.padding(bottom = 4.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = displayPrimaryUnit,
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = GabayColors.AccentAmber
                        )
                    )
                    Text(
                        text = displaySecondary,
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Normal,
                            fontSize = 7.5.sp,
                            color = GabayColors.TextMuted
                        )
                    )
                }
            }

            // Right Turn Bay (Fixed 20dp width, zero layout displacement)
            Box(
                modifier = Modifier.requiredSize(20.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isTurnRight) {
                    Icon(
                        imageVector = GabayIcons.TurnArrowRight,
                        contentDescription = "Turn Right",
                        tint = GabayColors.AccentAmber.copy(alpha = pulseAlpha),
                        modifier = Modifier.requiredSize(18.dp)
                    )
                }
            }
        }

        // Bottom Street Label
        if (bottomLabel.isNotBlank()) {
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = bottomLabel.uppercase(),
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 9.sp,
                    color = GabayColors.AccentAmber.copy(alpha = 0.9f)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Animated Map Style Speed-Dial
 * Extends upward in a floating overlay without displacing adjacent cockpit UI controls.
 * Highlights the active/selected map style with unified glass capsule design.
 */
@Composable
fun MapStyleSpeedDial(
    currentMode: FusedMapMode,
    onSelectMode: (FusedMapMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    val options = listOf(
        Triple(FusedMapMode.SATELLITE, GabayIcons.LayerSatellite, "SATELLITE"),
        Triple(FusedMapMode.TOPO, GabayIcons.LayerTopo, "TOPOGRAPHY"),
        Triple(FusedMapMode.STREET, GabayIcons.LayerVector, "STREET (VECTOR)")
    )

    Box(
        modifier = modifier.size(48.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        // Floating options popup above the anchor button (Zero horizontal layout impact)
        AnimatedVisibility(
            visible = isExpanded,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 58.dp)
                .wrapContentSize(Alignment.BottomStart, unbounded = true),
            enter = fadeIn(tween(180)) + slideInVertically(
                initialOffsetY = { it / 3 },
                animationSpec = tween(220)
            ),
            exit = fadeOut(tween(140)) + slideOutVertically(
                targetOffsetY = { it / 3 },
                animationSpec = tween(180)
            )
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEach { (mode, icon, label) ->
                    val isSelected = (mode == currentMode)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(GabayColors.UiCardGlass, RoundedCornerShape(24.dp))
                            .border(
                                1.2.dp,
                                if (isSelected) GabayColors.AccentAmber else GabayColors.UiBorder,
                                RoundedCornerShape(24.dp)
                            )
                            .clickable {
                                onSelectMode(mode)
                                isExpanded = false
                            }
                            .padding(end = 14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    if (isSelected) GabayColors.AccentAmber.copy(alpha = 0.25f) else Color.Transparent,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isSelected) GabayColors.AccentAmber else GabayColors.TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = label,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp,
                                color = if (isSelected) GabayColors.AccentAmber else GabayColors.TextPrimary
                            )
                        )
                    }
                }
            }
        }

        // Main Anchor Floating Button
        val mainIcon = when (currentMode) {
            FusedMapMode.STREET, FusedMapMode.HYBRID_3D -> GabayIcons.LayerVector
            FusedMapMode.TOPO -> GabayIcons.LayerTopo
            FusedMapMode.SATELLITE -> GabayIcons.LayerSatellite
        }
        CircularFloatingButton(
            icon = mainIcon,
            onClick = { isExpanded = !isExpanded },
            isActive = isExpanded,
            contentDescription = "Map Style Options",
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}

/**
 * Dedicated 2D / 3D Perspective Toggle Button
 * Works across all map styles (Street, Topography, Satellite).
 */
@Composable
fun PerspectiveToggleButton(
    is3d: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .requiredSize(48.dp)
            .background(
                if (is3d) GabayColors.AccentAmber.copy(alpha = 0.2f) else GabayColors.UiCardGlass,
                CircleShape
            )
            .border(
                if (is3d) 1.5.dp else 1.dp,
                if (is3d) GabayColors.AccentAmber else GabayColors.AccentAmber.copy(alpha = 0.35f),
                CircleShape
            )
            .clickable { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (is3d) "3D" else "2D",
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                color = if (is3d) GabayColors.AccentAmber else GabayColors.TextPrimary
            )
        )
    }
}

/**
 * Reset Rotation Button (Compass Needle)
 * Visible whenever the map is rotated from True North. Needle points to North.
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
                .requiredSize(48.dp)
                .background(GabayColors.UiCardGlass, CircleShape)
                .border(1.dp, GabayColors.UiBorder, CircleShape)
                .clickable { onReset() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.rotate(-cameraBearing)
            ) {
                // Red North needle tip
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(10.dp)
                        .background(Color(0xFFFF3B30), RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                )
                Spacer(modifier = Modifier.height(2.dp))
                // White South needle base
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
 * Floating Circular Action Button (Apple OS Fluid Roundness)
 */
@Composable
fun CircularFloatingButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    activeColor: Color = GabayColors.AccentAmber,
    contentDescription: String? = null
) {
    val bgColor = if (isActive) activeColor.copy(alpha = 0.2f) else GabayColors.UiCardGlass
    val borderColor = if (isActive) activeColor else GabayColors.AccentAmber.copy(alpha = 0.45f)
    val iconColor = if (isActive) activeColor else GabayColors.TextPrimary

    Box(
        modifier = modifier
            .requiredSize(48.dp)
            .background(bgColor, CircleShape)
            .border(1.2.dp, borderColor, CircleShape)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier.requiredSize(22.dp)
        )
    }
}

/**
 * Floating Distance & Pace Pills (Visible Only During Active Route)
 */
@Composable
fun FloatingTelemetryPill(
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(GabayColors.UiCardGlass, RoundedCornerShape(18.dp))
            .border(1.dp, GabayColors.AccentAmber.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = value,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = GabayColors.AccentAmber
            )
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = unit,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = GabayColors.TextMuted
            )
        )
    }
}

/**
 * Apple-style Live Agent Co-Pilot Sheet with Conversation Transcript History
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

    // Sync live voice partial transcription into active input field
    LaunchedEffect(partialSpeechText) {
        if (partialSpeechText.isNotBlank()) {
            inputText = partialSpeechText
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.70f)
            .offset { IntOffset(0, offsetY.roundToInt()) },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        colors = CardDefaults.cardColors(containerColor = GabayColors.UiCard),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(GabayColors.UiBorder))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Interactive Apple-style Draggable Handle Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (offsetY > 120f) {
                                    onClose()
                                } else {
                                    offsetY = 0f
                                }
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
                        .height(5.dp)
                        .background(GabayColors.TextMuted.copy(alpha = 0.6f), CircleShape)
                )
            }

            // Sheet Header with Active Pulse Dot
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(if (isListening) Color(0xFFFF3B30) else GabayColors.AccentAmber, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isListening) "LISTENING TO VOICE..." else "GABAY CO-PILOT AGENT",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 1.1.sp,
                            color = if (isListening) Color(0xFFFF3B30) else GabayColors.TextPrimary
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
                        tint = GabayColors.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Dynamic Neon Audio Waveform Visualizer (Appears when voice recognition is active)
            if (isListening) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GabayColors.UiCardElevated, RoundedCornerShape(12.dp))
                        .border(1.dp, GabayColors.AccentAmber.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val normalizedDb = ((audioRmsDb + 2f) / 14f).coerceIn(0.15f, 1.0f)
                    val barFactors = listOf(0.45f, 0.75f, 1.0f, 0.90f, 0.65f, 0.85f, 0.50f)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        barFactors.forEach { factor ->
                            val h = (6.dp + (18.dp * normalizedDb * factor))
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(h)
                                    .background(GabayColors.AccentAmber, RoundedCornerShape(2.dp))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = if (partialSpeechText.isNotBlank()) "\"$partialSpeechText\"" else "LISTENING... Speak your command",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = GabayColors.AccentAmber
                        ),
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Contextual Quick Action Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "Where am I?",
                    "Nearest Gas Station",
                    "Satellite Mode",
                    "3D Perspective",
                    "Cancel Route"
                ).forEach { prompt ->
                    Box(
                        modifier = Modifier
                            .background(GabayColors.UiInput, RoundedCornerShape(16.dp))
                            .border(1.dp, GabayColors.UiBorder, RoundedCornerShape(16.dp))
                            .clickable { onSendPrompt(prompt) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = prompt,
                            style = TextStyle(
                                fontFamily = FontFamily.Default,
                                fontSize = 12.sp,
                                color = GabayColors.TextPrimary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val listState = rememberLazyListState()
            LaunchedEffect(messages.size) {
                if (messages.isNotEmpty()) {
                    listState.animateScrollToItem(messages.size - 1)
                }
            }

            // Conversation History Transcript List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages) { msg ->
                    val isAgent = msg.sender.contains("AGENT", ignoreCase = true) || msg.sender.contains("GABAY", ignoreCase = true)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = if (isAgent) Alignment.Start else Alignment.End
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = msg.sender,
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAgent) GabayColors.AccentAmber else GabayColors.TextMuted
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = msg.timestamp,
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = GabayColors.TextMuted
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isAgent) GabayColors.UiCardAmber else GabayColors.UiCardElevated,
                                    RoundedCornerShape(14.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isAgent) GabayColors.AccentAmber.copy(alpha = 0.5f) else GabayColors.UiBorder,
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = msg.text,
                                style = TextStyle(
                                    fontFamily = if (isAgent) FontFamily.Monospace else FontFamily.Default,
                                    fontSize = 13.sp,
                                    color = GabayColors.TextPrimary
                                )
                            )
                        }
                    }
                }
            }

            // Real Interactive Command Bar (Voice Mic + Text Input + Send)
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GabayColors.UiInput, RoundedCornerShape(24.dp))
                    .border(
                        1.dp,
                        if (isListening) Color(0xFFFF3B30) else GabayColors.UiBorder,
                        RoundedCornerShape(24.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Speech-to-Text Mic Trigger
                IconButton(
                    onClick = onToggleListening,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = GabayIcons.CopilotMic,
                        contentDescription = "Voice Input",
                        tint = if (isListening) Color(0xFFFF3B30) else GabayColors.AccentAmber,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                BasicTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Default,
                        fontSize = 13.sp,
                        color = Color.White
                    ),
                    cursorBrush = SolidColor(GabayColors.AccentAmber),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (inputText.isEmpty()) {
                            Text(
                                text = if (isListening) "Listening to speech..." else "Ask co-pilot or command an action...",
                                style = TextStyle(
                                    fontFamily = FontFamily.Default,
                                    fontSize = 13.sp,
                                    color = if (isListening) Color(0xFFFF3B30) else GabayColors.TextMuted
                                )
                            )
                        }
                        innerTextField()
                    }
                )

                // Send Message Button
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSendPrompt(inputText.trim())
                            inputText = ""
                        }
                    },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = GabayIcons.Send,
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank()) GabayColors.AccentAmber else GabayColors.TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

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
 * Top Menu Drawer Sheet (Cockpit Settings, Drive History & Offline Vault Stats)
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
    var currentAudioMode by remember { mutableStateOf(audioController.guidanceMode) }
    var currentVoiceEngine by remember { mutableStateOf(audioController.voiceEngine) }
    val context = androidx.compose.ui.platform.LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.78f)
            .offset { IntOffset(0, offsetY.roundToInt()) },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        colors = CardDefaults.cardColors(containerColor = GabayColors.UiCard),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(GabayColors.UiBorder))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
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
                        .background(GabayColors.TextMuted.copy(alpha = 0.45f), CircleShape)
                )
            }

            // Header Title + Close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "GABAY COCKPIT HUB",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 1.1.sp,
                        color = GabayColors.TextPrimary
                    )
                )

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = GabayIcons.ActionClose,
                        contentDescription = "Close",
                        tint = GabayColors.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Navigation Tabs: [SETTINGS] [DRIVES] [VAULT]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GabayColors.UiInput, RoundedCornerShape(12.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("SETTINGS", "DRIVES (${driveHistory.size})", "OFFLINE VAULT").forEachIndexed { index, label ->
                    val isSel = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .background(
                                if (isSel) GabayColors.UiCardElevated else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedTab = index },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) GabayColors.AccentAmber else GabayColors.TextMuted
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (selectedTab) {
                0 -> {
                    // TAB 0: SETTINGS & HUD CUSTOMIZATION
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Section: THEME & DISPLAY
                        item {
                            Text(
                                text = "THEME & CONTRAST",
                                style = GabayTypography.MonospaceLabel
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val isSunlight = GabayColors.isSunlightLightMode
                                // Tactical Dark Button
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .background(
                                            if (!isSunlight) GabayColors.AccentAmber.copy(alpha = 0.2f) else GabayColors.UiInput,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (!isSunlight) GabayColors.AccentAmber else GabayColors.UiBorder,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { onThemeToggle(false) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "TACTICAL DARK",
                                        style = TextStyle(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (!isSunlight) GabayColors.AccentAmber else GabayColors.TextSecondary
                                        )
                                    )
                                }
                                // Sunlight Daylight Button
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .background(
                                            if (isSunlight) GabayColors.AccentAmber.copy(alpha = 0.2f) else GabayColors.UiInput,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSunlight) GabayColors.AccentAmber else GabayColors.UiBorder,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { onThemeToggle(true) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "SUNLIGHT LIGHT",
                                        style = TextStyle(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSunlight) GabayColors.AccentAmber else GabayColors.TextSecondary
                                        )
                                    )
                                }
                            }
                        }

                        // Section: HUD TELEMETRY WIDGETS
                        item {
                            Text(
                                text = "HUD TELEMETRY WIDGETS",
                                style = GabayTypography.MonospaceLabel
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(GabayColors.UiInput, RoundedCornerShape(14.dp))
                                    .border(1.dp, GabayColors.UiBorder, RoundedCornerShape(14.dp))
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Unit Toggle
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Speedometer Unit",
                                        style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = GabayColors.TextPrimary)
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .background(if (!GabayHudSettings.useMph) GabayColors.AccentAmber else Color.Transparent, RoundedCornerShape(6.dp))
                                                .clickable { GabayHudSettings.useMph = false; GabayHudSettings.save(context) }
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text("KM/H", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (!GabayHudSettings.useMph) Color(0xFF0F172A) else GabayColors.TextMuted))
                                        }
                                        Box(
                                            modifier = Modifier
                                                .background(if (GabayHudSettings.useMph) GabayColors.AccentAmber else Color.Transparent, RoundedCornerShape(6.dp))
                                                .clickable { GabayHudSettings.useMph = true; GabayHudSettings.save(context) }
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text("MPH", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (GabayHudSettings.useMph) Color(0xFF0F172A) else GabayColors.TextMuted))
                                        }
                                    }
                                }

                                HorizontalDivider(color = GabayColors.UiBorder, thickness = 0.5.dp)

                                // G-Force Toggle
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Lateral G-Force Meter", style = TextStyle(fontSize = 13.sp, color = GabayColors.TextPrimary))
                                    Switch(
                                        checked = GabayHudSettings.showGForce,
                                        onCheckedChange = { GabayHudSettings.showGForce = it; GabayHudSettings.save(context) },
                                        colors = SwitchDefaults.colors(checkedThumbColor = GabayColors.AccentAmber, checkedTrackColor = GabayColors.AccentAmber.copy(alpha = 0.3f))
                                    )
                                }

                                HorizontalDivider(color = GabayColors.UiBorder, thickness = 0.5.dp)

                                // Trip Pace Pill Toggle
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Trip Distance & Pace Pill", style = TextStyle(fontSize = 13.sp, color = GabayColors.TextPrimary))
                                    Switch(
                                        checked = GabayHudSettings.showPacePill,
                                        onCheckedChange = { GabayHudSettings.showPacePill = it; GabayHudSettings.save(context) },
                                        colors = SwitchDefaults.colors(checkedThumbColor = GabayColors.AccentAmber, checkedTrackColor = GabayColors.AccentAmber.copy(alpha = 0.3f))
                                    )
                                }

                                HorizontalDivider(color = GabayColors.UiBorder, thickness = 0.5.dp)

                                // Centerline Magnet Snapping
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Road Centerline Magnet", style = TextStyle(fontSize = 13.sp, color = GabayColors.TextPrimary))
                                    Switch(
                                        checked = GabayHudSettings.snapToCenterline,
                                        onCheckedChange = { GabayHudSettings.snapToCenterline = it; GabayHudSettings.save(context) },
                                        colors = SwitchDefaults.colors(checkedThumbColor = GabayColors.AccentAmber, checkedTrackColor = GabayColors.AccentAmber.copy(alpha = 0.3f))
                                    )
                                }
                            }
                        }

                        // Section: AUDIO & OFFLINE VOICE ENGINE
                        item {
                            Text(
                                text = "AUDIO & VOICE GUIDANCE",
                                style = GabayTypography.MonospaceLabel
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(GabayColors.UiInput, RoundedCornerShape(14.dp))
                                    .border(1.dp, GabayColors.UiBorder, RoundedCornerShape(14.dp))
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("Guidance Mode", style = TextStyle(fontSize = 12.sp, color = GabayColors.TextSecondary))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        AudioGuidanceMode.FULL_VOICE to "FULL VOICE",
                                        AudioGuidanceMode.ALERTS_ONLY to "ALERTS ONLY",
                                        AudioGuidanceMode.MUTED to "MUTED"
                                    ).forEach { (mode, label) ->
                                        val isSel = currentAudioMode == mode
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(36.dp)
                                                .background(
                                                    if (isSel) GabayColors.AccentAmber.copy(alpha = 0.2f) else GabayColors.UiCardElevated,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .border(1.dp, if (isSel) GabayColors.AccentAmber else Color.Transparent, RoundedCornerShape(8.dp))
                                                .clickable {
                                                    currentAudioMode = mode
                                                    audioController.guidanceMode = mode
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSel) GabayColors.AccentAmber else GabayColors.TextMuted)
                                            )
                                        }
                                    }
                                }

                                Text("Voice Engine", style = TextStyle(fontSize = 12.sp, color = GabayColors.TextSecondary))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        dev.gabay.app.audio.VoiceEngineType.TACTICAL_EXTERNAL to "EXTERNAL (WAV)",
                                        dev.gabay.app.audio.VoiceEngineType.SYSTEM_TTS to "SYSTEM TTS"
                                    ).forEach { (engine, label) ->
                                        val isSel = currentVoiceEngine == engine
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(36.dp)
                                                .background(
                                                    if (isSel) GabayColors.AccentAmber.copy(alpha = 0.2f) else GabayColors.UiCardElevated,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .border(1.dp, if (isSel) GabayColors.AccentAmber else Color.Transparent, RoundedCornerShape(8.dp))
                                                .clickable {
                                                    currentVoiceEngine = engine
                                                    audioController.voiceEngine = engine
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSel) GabayColors.AccentAmber else GabayColors.TextMuted)
                                            )
                                        }
                                    }
                                }

                                // Test Cue Button
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                        .background(GabayColors.UiCardElevated, RoundedCornerShape(10.dp))
                                        .border(1.dp, GabayColors.UiBorder, RoundedCornerShape(10.dp))
                                        .clickable {
                                            val sample = if (currentVoiceEngine == dev.gabay.app.audio.VoiceEngineType.TACTICAL_EXTERNAL) "turn left" else "Turn left in 200 meters"; audioController.testVoiceCue(sample)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = GabayIcons.AudioTts, contentDescription = null, tint = GabayColors.AccentAmber, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("PLAY SAMPLE VOICE CUE", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GabayColors.AccentAmber))
                                    }
                                }
                            }
                        }

                        // Section: ROUTING PREFERENCES
                        item {
                            Text(
                                text = "ROUTING & VEHICLE PREFERENCES",
                                style = GabayTypography.MonospaceLabel
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    Triple(VehicleMode.SCOOTER, "SCOOTER (<400cc)", GabayIcons.ModeScooter),
                                    Triple(VehicleMode.CAR, "CAR", GabayIcons.ModeCar),
                                    Triple(VehicleMode.WALK, "WALK", GabayIcons.ModeWalk)
                                ).forEach { (mode, label, icon) ->
                                    val isSel = selectedMode == mode
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .background(
                                                if (isSel) GabayColors.AccentAmber.copy(alpha = 0.2f) else GabayColors.UiInput,
                                                RoundedCornerShape(12.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSel) GabayColors.AccentAmber else GabayColors.UiBorder,
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable { onModeSelect(mode) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = if (isSel) GabayColors.AccentAmber else GabayColors.TextSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (mode == VehicleMode.SCOOTER) "SCOOTER" else label,
                                                style = TextStyle(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSel) GabayColors.AccentAmber else GabayColors.TextSecondary
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // TAB 1: DRIVES HISTORY
                    if (driveHistory.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No recorded drives yet.\nCompleted navigation trips will appear here.", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = GabayColors.TextMuted, textAlign = androidx.compose.ui.text.style.TextAlign.Center))
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(driveHistory) { drive ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(GabayColors.UiInput, RoundedCornerShape(12.dp))
                                        .border(1.dp, GabayColors.UiBorder, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(text = drive.title, style = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = GabayColors.TextPrimary))
                                        Text(text = drive.date, style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = GabayColors.TextMuted))
                                    }
                                    Text(text = "${drive.distanceKm} KM • ${drive.durationMin} MIN", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GabayColors.AccentAmber))
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // TAB 2: OFFLINE VAULT & REAL SYSTEM DIAGNOSTICS
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            val graphFile = java.io.File("/sdcard/Documents/Gabay/ph.gabay")
                            val graphMb = if (graphFile.exists()) "${graphFile.length() / (1024 * 1024)} MB" else "MISSING"
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(GabayColors.UiInput, RoundedCornerShape(12.dp))
                                    .border(1.dp, GabayColors.UiBorder, RoundedCornerShape(12.dp))
                                    .padding(14.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("OSM Graph Router (ph.gabay)", style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GabayColors.TextPrimary))
                                    Text(if (graphFile.exists()) "READY ($graphMb)" else "MISSING", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = if (graphFile.exists()) GabayColors.AccentEmerald else Color(0xFFFF3B30)))
                                }
                                Text("A* Canonical Highway Graph (Bicycles, Scooters, Cars)", style = TextStyle(fontSize = 11.sp, color = GabayColors.TextMuted))
                            }
                        }
                        item {
                            val dbFile = java.io.File("/sdcard/Documents/Gabay/pois_fts.sqlite")
                            val dbMb = if (dbFile.exists()) "${dbFile.length() / (1024 * 1024)} MB" else "MISSING"
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(GabayColors.UiInput, RoundedCornerShape(12.dp))
                                    .border(1.dp, GabayColors.UiBorder, RoundedCornerShape(12.dp))
                                    .padding(14.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("SQLite Spatial FTS5 Router", style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GabayColors.TextPrimary))
                                    Text(if (dbFile.exists()) "READY ($dbMb)" else "MISSING", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = if (dbFile.exists()) GabayColors.AccentEmerald else Color(0xFFFF3B30)))
                                }
                                Text("Full-text offline search over POIs, barangays, and road names", style = TextStyle(fontSize = 11.sp, color = GabayColors.TextMuted))
                            }
                        }
                        item {
                            val pmtilesFile = java.io.File("/sdcard/Documents/Gabay/philippines_vector.pmtiles")
                            val pmtilesMb = if (pmtilesFile.exists()) "${pmtilesFile.length() / (1024 * 1024)} MB" else "MISSING"
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(GabayColors.UiInput, RoundedCornerShape(12.dp))
                                    .border(1.dp, GabayColors.UiBorder, RoundedCornerShape(12.dp))
                                    .padding(14.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("PMTiles Offline Vector Map", style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GabayColors.TextPrimary))
                                    Text(if (pmtilesFile.exists()) "BOUND ($pmtilesMb)" else "LOCAL", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = if (pmtilesFile.exists()) GabayColors.AccentEmerald else GabayColors.TextMuted))
                                }
                                Text("Protomaps HTTP Localhost :8765 Zero Data Consumption", style = TextStyle(fontSize = 11.sp, color = GabayColors.TextMuted))
                            }
                        }
                        item {
                            val modelFile = java.io.File("/sdcard/Documents/Gabay/model.gguf")
                            val modelMb = if (modelFile.exists()) "${modelFile.length() / (1024 * 1024)} MB" else "MISSING"
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(GabayColors.UiInput, RoundedCornerShape(12.dp))
                                    .border(1.dp, GabayColors.UiBorder, RoundedCornerShape(12.dp))
                                    .padding(14.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Edge LLM Model (model.gguf)", style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GabayColors.TextPrimary))
                                    Text(if (modelFile.exists()) "READY ($modelMb)" else "OPTIONAL", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = if (modelFile.exists()) GabayColors.AccentEmerald else GabayColors.TextMuted))
                                }
                                Text("Edge llama.cpp daemon with Live Rider Telemetry injection", style = TextStyle(fontSize = 11.sp, color = GabayColors.TextMuted))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Google Maps Style Route Preview Sheet
 * Displays travel mode tabs (Two-wheeler / Car / Walk), route duration & distance,
 * route subtitle, avoidance chips, and prominent "START" button.
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
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = GabayColors.UiCardElevated),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(GabayColors.UiBorder))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag Handle Bar
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .background(Color(0xFF64748B), CircleShape)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Mode Selector Tabs (Google Maps Style: Two-wheeler, Car, Commute, Walk)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Two-wheeler (Scooter)
                val isScooterSelected = selectedMode == VehicleMode.SCOOTER && !isCommuteMode
                val scooterMins = (route.totalDistanceMeters / 11.67 / 60.0).toLong().toULong().coerceAtLeast(1u)
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectMode(VehicleMode.SCOOTER) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = ApexAlonIcons.Scooter,
                            contentDescription = "Two-wheeler",
                            tint = if (isScooterSelected) GabayColors.AccentPrimary else GabayColors.TextSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = "$scooterMins m",
                            style = TextStyle(
                                fontWeight = if (isScooterSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                color = if (isScooterSelected) GabayColors.AccentPrimary else GabayColors.TextSecondary
                            )
                        )
                    }
                    if (isScooterSelected) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Box(modifier = Modifier.width(26.dp).height(2.5.dp).background(GabayColors.AccentPrimary, CircleShape))
                    }
                }

                // Car
                val isCarSelected = selectedMode == VehicleMode.CAR && !isCommuteMode
                val carMins = (route.totalDistanceMeters / 9.72 / 60.0).toLong().toULong().coerceAtLeast(1u)
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectMode(VehicleMode.CAR) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = GabayIcons.ModeCar,
                            contentDescription = "Car",
                            tint = if (isCarSelected) GabayColors.AccentPrimary else GabayColors.TextSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = "$carMins m",
                            style = TextStyle(
                                fontWeight = if (isCarSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                color = if (isCarSelected) GabayColors.AccentPrimary else GabayColors.TextSecondary
                            )
                        )
                    }
                    if (isCarSelected) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Box(modifier = Modifier.width(26.dp).height(2.5.dp).background(GabayColors.AccentPrimary, CircleShape))
                    }
                }

                // Commute (Transit Bus / Train / Ferry)
                val isCommuteSelected = isCommuteMode
                val commuteMins = (route.totalDistanceMeters / 6.5 / 60.0).toLong().toULong().coerceAtLeast(1u)
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectCommute?.invoke() }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = GabayIcons.NavCursor,
                            contentDescription = "Commute",
                            tint = if (isCommuteSelected) GabayColors.AccentPrimary else GabayColors.TextSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = "$commuteMins m",
                            style = TextStyle(
                                fontWeight = if (isCommuteSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                color = if (isCommuteSelected) GabayColors.AccentPrimary else GabayColors.TextSecondary
                            )
                        )
                    }
                    if (isCommuteSelected) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Box(modifier = Modifier.width(26.dp).height(2.5.dp).background(GabayColors.AccentPrimary, CircleShape))
                    }
                }

                // Walk
                val isWalkSelected = selectedMode == VehicleMode.WALK && !isCommuteMode
                val walkMins = (route.totalDistanceMeters / 1.33 / 60.0).toLong().toULong().coerceAtLeast(1u)
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectMode(VehicleMode.WALK) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = GabayIcons.ModeWalk,
                            contentDescription = "Walk",
                            tint = if (isWalkSelected) GabayColors.AccentPrimary else GabayColors.TextSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = "$walkMins m",
                            style = TextStyle(
                                fontWeight = if (isWalkSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                color = if (isWalkSelected) GabayColors.AccentPrimary else GabayColors.TextSecondary
                            )
                        )
                    }
                    if (isWalkSelected) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Box(modifier = Modifier.width(26.dp).height(2.5.dp).background(GabayColors.AccentPrimary, CircleShape))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Avoidances row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avoid tolls chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (avoidTolls) GabayColors.AccentPrimary.copy(alpha = 0.15f) else Color.Transparent)
                        .border(
                            1.dp,
                            if (avoidTolls) GabayColors.AccentPrimary else GabayColors.UiBorder,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { onToggleAvoidTolls() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (avoidTolls) {
                            Icon(
                                imageVector = GabayIcons.ActionCheck,
                                contentDescription = null,
                                tint = GabayColors.AccentPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Text(
                            text = "Avoid tolls",
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = if (avoidTolls) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (avoidTolls) GabayColors.AccentPrimary else GabayColors.TextSecondary
                            )
                        )
                    }
                }

                // Avoid highways chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (avoidHighways) GabayColors.AccentPrimary.copy(alpha = 0.15f) else Color.Transparent)
                        .border(
                            1.dp,
                            if (avoidHighways) GabayColors.AccentPrimary else GabayColors.UiBorder,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { onToggleAvoidHighways() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (avoidHighways) {
                            Icon(
                                imageVector = GabayIcons.ActionCheck,
                                contentDescription = null,
                                tint = GabayColors.AccentPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Text(
                            text = "Avoid highways",
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = if (avoidHighways) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (avoidHighways) GabayColors.AccentPrimary else GabayColors.TextSecondary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Duration & Distance Hero Telemetry
            val distKm = route.totalDistanceMeters / 1000.0
            val durMin = route.estimatedDurationSeconds / 60u
            val viaRoad = route.maneuvers.firstOrNull()?.roadName ?: "Road Network"

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
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp,
                                color = GabayColors.AccentPrimary
                            )
                        )
                        Text(
                            text = "(${String.format(Locale.US, "%.1f", distKm)} km)",
                            style = TextStyle(
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.Normal,
                                fontSize = 15.sp,
                                color = GabayColors.TextSecondary
                            )
                        )
                    }
                    Text(
                        text = "Fastest route via $viaRoad",
                        style = TextStyle(
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = GabayColors.TextSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: Start Navigation [▶ START] and Cancel [✕]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onStartNavigation,
                    colors = ButtonDefaults.buttonColors(containerColor = GabayColors.AccentAmber),
                    shape = RoundedCornerShape(24.dp),
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
                            contentDescription = "Start Navigation",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "START NAVIGATION",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        )
                    }
                }

                IconButton(
                    onClick = onCancelRoute,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (GabayColors.isSunlightLightMode) Color(0xFFFEE2E2) else Color(0x33FF3B30),
                            CircleShape
                        )
                        .border(
                            1.dp,
                            if (GabayColors.isSunlightLightMode) Color(0xFFEF4444) else Color(0xFFFF3B30).copy(alpha = 0.5f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = GabayIcons.ActionClose,
                        contentDescription = "Cancel Route",
                        tint = if (GabayColors.isSunlightLightMode) Color(0xFFB91C1C) else Color(0xFFFF3B30),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
