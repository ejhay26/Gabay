package dev.gabay.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gabay.app.map.FusedMapMode
import uniffi.gabay_core.HudTelemetry
import uniffi.gabay_core.PoiSearchResult
import uniffi.gabay_core.VehicleMode

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
 * Top Search Bar (Google Maps Style + Apple OS Fluidity)
 * Includes Menu icon, live search text field, and SQLite FTS5 live suggestions dropdown.
 */
@Composable
fun TopSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    searchResults: List<PoiSearchResult>,
    onSelectPlace: (PoiSearchResult) -> Unit,
    onMenuClick: () -> Unit,
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
            // Left: Circular Menu Button
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            ) {
                Icon(
                    imageVector = GabayIcons.Menu,
                    contentDescription = "Menu",
                    tint = GabayColors.TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
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
                        text = "Search places, roads, landmarks...",
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
                    cursorBrush = SolidColor(GabayColors.AccentMint),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Right: Clear or Search Icon
            if (query.isNotEmpty()) {
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
                    onClick = {},
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = GabayIcons.SearchPoi,
                        contentDescription = "Search",
                        tint = GabayColors.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Search Results Dropdown (SQLite FTS5 Results)
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
                    searchResults.take(5).forEach { place ->
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
                                    .size(32.dp)
                                    .background(GabayColors.UiInput, CircleShape)
                                    .border(1.dp, GabayColors.UiBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = GabayIcons.DestinationPin,
                                    contentDescription = null,
                                    tint = GabayColors.AccentMint,
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
 * Matches the user requirement: "Live speed (no text) is in the middle bottom in km/h and a very little one on the bottom right to also show miles/h then it will show or blink arrows on either sides whenever we need to turn"
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

    val isTurnLeft = nextTurnIcon.contains("left")
    val isTurnRight = nextTurnIcon.contains("right")

    // Infinite transition for turn arrow pulse
    val infiniteTransition = rememberInfiniteTransition(label = "TurnArrowPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    )

    Column(
        modifier = modifier
            .background(GabayColors.UiCardGlass, RoundedCornerShape(32.dp))
            .border(1.dp, GabayColors.UiBorder, RoundedCornerShape(32.dp))
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Left Flanking Turn Arrow - ONLY VISIBLE WHEN TURNING LEFT
            if (isTurnLeft) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(GabayColors.AccentMint.copy(alpha = 0.2f * pulseAlpha), CircleShape)
                        .border(1.dp, GabayColors.AccentMint.copy(alpha = pulseAlpha), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = GabayIcons.TurnArrowLeft,
                        contentDescription = "Turn Left",
                        tint = GabayColors.AccentMint,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
            }

            // Large Speed Readout (NUMBER ONLY, NO "LIVE SPEED" LABEL)
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "$speedInt",
                    style = GabayTypography.SpeedHuge,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(4.dp))
                Column(
                    modifier = Modifier.padding(bottom = 6.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "KM/H",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = GabayColors.TextSecondary
                        )
                    )
                    Text(
                        text = "$speedMph mph",
                        style = GabayTypography.SpeedMph
                    )
                }
            }

            // Right Flanking Turn Arrow - ONLY VISIBLE WHEN TURNING RIGHT
            if (isTurnRight) {
                Spacer(modifier = Modifier.width(14.dp))
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(GabayColors.AccentMint.copy(alpha = 0.2f * pulseAlpha), CircleShape)
                        .border(1.dp, GabayColors.AccentMint.copy(alpha = pulseAlpha), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = GabayIcons.TurnArrowRight,
                        contentDescription = "Turn Right",
                        tint = GabayColors.AccentMint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Bottom label (Street name or Maneuver instruction)
        if (bottomLabel.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = bottomLabel.uppercase(),
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    color = GabayColors.AccentMint.copy(alpha = 0.9f)
                ),
                maxLines = 1
            )
        }
    }
}

/**
 * Animated Map Style Speed-Dial
 * Extends icons upwards on tap, then delayed text labels slide sidewards to the right.
 * Highlights the active/selected map style.
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

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Bottom
    ) {
        // Extended items upwards
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
        ) {
            Column(
                modifier = Modifier.padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                options.forEachIndexed { index, (mode, icon, label) ->
                    val isSelected = (mode == currentMode)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            onSelectMode(mode)
                            isExpanded = false
                        }
                    ) {
                        // Circular Icon Button
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .background(
                                    if (isSelected) GabayColors.AccentMint.copy(alpha = 0.25f) else GabayColors.UiCardGlass,
                                    CircleShape
                                )
                                .border(
                                    if (isSelected) 1.5.dp else 1.dp,
                                    if (isSelected) GabayColors.AccentMint else GabayColors.UiBorder,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isSelected) GabayColors.AccentMint else GabayColors.TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Delayed text label extending sidewards to the right
                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = fadeIn(animationSpec = tween(200, delayMillis = (index + 1) * 80)) +
                                    slideInHorizontally(
                                        initialOffsetX = { -30 },
                                        animationSpec = tween(250, delayMillis = (index + 1) * 80)
                                    ),
                            exit = fadeOut() + slideOutHorizontally()
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(GabayColors.UiCardGlass, RoundedCornerShape(16.dp))
                                    .border(
                                        1.dp,
                                        if (isSelected) GabayColors.AccentMint else GabayColors.UiBorder,
                                        RoundedCornerShape(16.dp)
                                    )
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (isSelected) GabayColors.AccentMint else GabayColors.TextPrimary
                                    )
                                )
                            }
                        }
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
            contentDescription = "Map Style Options"
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
            .size(48.dp)
            .background(
                if (is3d) GabayColors.AccentMint.copy(alpha = 0.25f) else GabayColors.UiCardGlass,
                CircleShape
            )
            .border(
                if (is3d) 1.5.dp else 1.dp,
                if (is3d) GabayColors.AccentMint else GabayColors.UiBorder,
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
                color = if (is3d) GabayColors.AccentMint else GabayColors.TextPrimary
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
                .size(48.dp)
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
    activeColor: Color = GabayColors.AccentMint,
    contentDescription: String? = null
) {
    val bgColor = if (isActive) activeColor.copy(alpha = 0.25f) else GabayColors.UiCardGlass
    val borderColor = if (isActive) activeColor else GabayColors.UiBorder
    val iconColor = if (isActive) activeColor else GabayColors.TextPrimary

    Box(
        modifier = modifier
            .size(48.dp)
            .background(bgColor, CircleShape)
            .border(1.dp, borderColor, CircleShape)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier.size(22.dp)
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
            .border(1.dp, GabayColors.UiBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = value,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = GabayColors.AccentMint
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
                            .background(if (isListening) Color(0xFFFF3B30) else GabayColors.AccentMint, CircleShape)
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
                        .border(1.dp, GabayColors.AccentMint.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
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
                                    .background(GabayColors.AccentMint, RoundedCornerShape(2.dp))
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
                            color = GabayColors.AccentMint
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

            // Conversation History Transcript List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages) { msg ->
                    val isAgent = msg.sender.contains("AGENT")
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
                                    color = if (isAgent) GabayColors.AccentMint else GabayColors.TextMuted
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
                                    if (isAgent) GabayColors.UiInput else GabayColors.UiCardElevated,
                                    RoundedCornerShape(14.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isAgent) GabayColors.AccentMint.copy(alpha = 0.4f) else GabayColors.UiBorder,
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = msg.text,
                                style = TextStyle(
                                    fontFamily = if (isAgent) FontFamily.Monospace else FontFamily.Default,
                                    fontSize = 13.sp,
                                    color = Color.White
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
                        tint = if (isListening) Color(0xFFFF3B30) else GabayColors.AccentMint,
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
                    cursorBrush = SolidColor(GabayColors.AccentMint),
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
                        tint = if (inputText.isNotBlank()) GabayColors.AccentMint else GabayColors.TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Top Menu Drawer Sheet (Drive History & Offline Vault Stats)
 */
@Composable
fun TopMenuSheet(
    driveHistory: List<DriveHistoryItem>,
    selectedMode: VehicleMode,
    onModeSelect: (VehicleMode) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.65f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        colors = CardDefaults.cardColors(containerColor = GabayColors.UiCard),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(GabayColors.UiBorder))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Apple-style Centered Drag Handle Pill
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 14.dp)
                    .width(38.dp)
                    .height(4.dp)
                    .background(GabayColors.TextMuted.copy(alpha = 0.45f), CircleShape)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "GABAY COCKPIT & HISTORY",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
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

            Spacer(modifier = Modifier.height(14.dp))

            // Vehicle Mode Pills
            Text(
                text = "VEHICLE MODE",
                style = GabayTypography.MonospaceLabel
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple(VehicleMode.SCOOTER, "SCOOTER", GabayIcons.ModeScooter),
                    Triple(VehicleMode.CAR, "CAR", GabayIcons.ModeCar),
                    Triple(VehicleMode.WALK, "WALK", GabayIcons.ModeWalk)
                ).forEach { (mode, label, icon) ->
                    val isSel = selectedMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .background(
                                if (isSel) GabayColors.AccentMint.copy(alpha = 0.2f) else GabayColors.UiInput,
                                RoundedCornerShape(12.dp)
                            )
                            .border(
                                1.dp,
                                if (isSel) GabayColors.AccentMint else GabayColors.UiBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onModeSelect(mode) },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSel) GabayColors.AccentMint else GabayColors.TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = label,
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) GabayColors.AccentMint else GabayColors.TextSecondary
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Successful Drive History List
            Text(
                text = "SUCCESSFUL DRIVES HISTORY",
                style = GabayTypography.MonospaceLabel
            )
            Spacer(modifier = Modifier.height(6.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
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
                            Text(
                                text = drive.title,
                                style = TextStyle(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = GabayColors.TextPrimary
                                )
                            )
                            Text(
                                text = drive.date,
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = GabayColors.TextMuted
                                )
                            )
                        }
                        Text(
                            text = "${drive.distanceKm} KM • ${drive.durationMin} MIN",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GabayColors.AccentMint
                            )
                        )
                    }
                }
            }
        }
    }
}
