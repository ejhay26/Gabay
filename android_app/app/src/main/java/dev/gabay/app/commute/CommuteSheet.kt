package dev.gabay.app.commute

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gabay.app.ui.GabayColors
import dev.gabay.app.ui.GabayIcons

@Composable
fun CommuteSheet(
    itineraries: List<CommuteItinerary>,
    selectedItineraryIndex: Int,
    onSelectItinerary: (Int) -> Unit,
    onStartCommute: (CommuteItinerary) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (itineraries.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(GabayColors.UiSurfaceElevated, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .border(1.dp, GabayColors.UiBorder, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "NO TRANSIT ROUTE FOUND",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = GabayColors.TextPrimary, fontSize = 14.sp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "No public transit stations (MRT, LRT, Busway, Ferry) within walking distance of origin or destination.",
                    style = TextStyle(color = GabayColors.TextMuted, fontSize = 12.sp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        return
    }

    val activeItinerary = itineraries.getOrNull(selectedItineraryIndex) ?: itineraries.first()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(GabayColors.UiSurfaceElevated, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .border(1.dp, GabayColors.UiBorder, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .padding(16.dp)
    ) {
        // Drag handle
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(GabayColors.UiBorder)
                .align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Header: Title, Duration, Fare
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "COMMUTE ITINERARY",
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = GabayColors.AccentAmber)
                    )
                    Text("•", color = GabayColors.TextMuted)
                    Text(
                        text = activeItinerary.title,
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = GabayColors.TextPrimary)
                    )
                }
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${activeItinerary.totalDurationMinutes} MIN",
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 24.sp, color = GabayColors.TextPrimary)
                    )
                    if (activeItinerary.totalFarePhp > 0) {
                        Text(
                            text = "Est. ₱${activeItinerary.totalFarePhp}",
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GabayColors.AccentSolar)
                        )
                    } else {
                        Text(
                            text = "FREE",
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GabayColors.AccentEmerald)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(GabayColors.UiInput)
                    .clickable { onClose() }
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = GabayIcons.ActionClose,
                    contentDescription = "Close",
                    tint = GabayColors.TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Visual Step Timeline Bar (Google Maps style pills)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(GabayColors.UiInput, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            activeItinerary.summaryPills.forEachIndexed { idx, pill ->
                if (idx > 0) {
                    Text("➔", color = GabayColors.TextMuted, fontSize = 10.sp)
                }

                val pillColor = try {
                    Color(android.graphics.Color.parseColor(pill.hexColor))
                } catch (e: Exception) {
                    GabayColors.AccentSky
                }

                Row(
                    modifier = Modifier
                        .background(pillColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .border(1.dp, pillColor.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val iconVector = when (pill.vehicleType) {
                        TransitVehicleType.TRAIN -> GabayIcons.NavCursor
                        TransitVehicleType.BUS -> GabayIcons.ModeCar
                        TransitVehicleType.FERRY -> GabayIcons.ModeSwap
                        TransitVehicleType.JEEPNEY -> GabayIcons.ModeCar
                        TransitVehicleType.WALK -> GabayIcons.ModeWalk
                    }
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = pillColor,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = pill.label,
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = pillColor)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Detailed Step-by-Step Directions List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .height(240.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(activeItinerary.legs) { _, leg ->
                when (leg) {
                    is CommuteLeg.Walk -> {
                        CommuteWalkCard(leg = leg)
                    }
                    is CommuteLeg.TransitRide -> {
                        CommuteTransitRideCard(leg = leg)
                    }
                    is CommuteLeg.Transfer -> {
                        CommuteTransferCard(leg = leg)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Start Commute Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(GabayColors.AccentSolar)
                    .clickable { onStartCommute(activeItinerary) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "START COMMUTE GUIDANCE",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = GabayColors.TextInverse
                    )
                )
            }
        }
    }
}

@Composable
private fun CommuteWalkCard(leg: CommuteLeg.Walk) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GabayColors.UiInput, RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(0xFF334155)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = GabayIcons.ModeWalk,
                contentDescription = "Walk",
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(15.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = leg.instruction,
                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = GabayColors.TextPrimary)
            )
            Text(
                text = "~${leg.durationMinutes} min walk",
                style = TextStyle(fontSize = 10.sp, color = GabayColors.TextMuted)
            )
        }
    }
}

@Composable
private fun CommuteTransitRideCard(leg: CommuteLeg.TransitRide) {
    var expanded by remember { mutableStateOf(false) }

    val lineColor = try {
        Color(android.graphics.Color.parseColor(leg.lineHexColor))
    } catch (e: Exception) {
        GabayColors.AccentSky
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GabayColors.UiInput, RoundedCornerShape(8.dp))
            .border(1.dp, lineColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(lineColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = GabayIcons.NavCursor,
                    contentDescription = "Transit",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = leg.lineName,
                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = lineColor)
                    )
                    Text(
                        text = "Toward ${leg.headsign}",
                        style = TextStyle(fontSize = 10.sp, color = GabayColors.TextMuted)
                    )
                }
                Text(
                    text = "Board at ${leg.departureStation} ➔ Alight at ${leg.arrivalStation}",
                    style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = GabayColors.TextPrimary)
                )
                Text(
                    text = "${leg.durationMinutes} min (${leg.stopsCount} stops) • Every ${leg.headwayMinutes} min • Est. ₱${leg.fareEstimatePhp}",
                    style = TextStyle(fontSize = 10.sp, color = GabayColors.AccentSolar)
                )
            }
        }

        // Expandable intermediate stops
        if (leg.intermediateStops.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (expanded) "Hide intermediate stops" else "View ${leg.intermediateStops.size} stops",
                    style = TextStyle(fontSize = 10.sp, color = GabayColors.TextMuted, fontWeight = FontWeight.Medium)
                )
                Text(
                    text = if (expanded) "▲" else "▼",
                    style = TextStyle(fontSize = 8.sp, color = GabayColors.TextMuted)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 38.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    leg.intermediateStops.forEach { stName ->
                        Text(
                            text = "• $stName",
                            style = TextStyle(fontSize = 10.sp, color = GabayColors.TextMuted)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommuteTransferCard(leg: CommuteLeg.Transfer) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GabayColors.UiInput.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
            .border(1.dp, GabayColors.UiBorder, RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Color(0xFF475569)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = GabayIcons.ModeSwap,
                contentDescription = "Transfer",
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = leg.instruction,
                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = GabayColors.AccentSky)
            )
            Text(
                text = "Transfer walk (~${leg.durationMinutes} min)",
                style = TextStyle(fontSize = 10.sp, color = GabayColors.TextMuted)
            )
        }
    }
}
