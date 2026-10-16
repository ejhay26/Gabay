package dev.gabay.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uniffi.gabay_core.HudTelemetry
import uniffi.gabay_core.VehicleMode

@Composable
fun TopManeuverBanner(
    telemetry: HudTelemetry,
    vehicleMode: VehicleMode,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .background(GabayColors.BgCard, RoundedCornerShape(8.dp))
            .border(1.dp, GabayColors.BorderSubtle, RoundedCornerShape(8.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Maneuver Icon
            val maneuverIcon: ImageVector = when (telemetry.nextTurnIcon) {
                "maneuver_left" -> GabayIcons.ManeuverLeft
                "maneuver_right" -> GabayIcons.ManeuverRight
                "maneuver_slight_left" -> GabayIcons.ManeuverSlightLeft
                "maneuver_slight_right" -> GabayIcons.ManeuverSlightRight
                "maneuver_u_turn" -> GabayIcons.ManeuverUTurn
                "maneuver_fork" -> GabayIcons.ManeuverFork
                else -> GabayIcons.ManeuverStraight
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(GabayColors.BgInput, RoundedCornerShape(6.dp))
                    .border(1.dp, GabayColors.BorderSubtle, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = maneuverIcon,
                    contentDescription = "Maneuver",
                    tint = if (telemetry.isOffRoute) GabayColors.AccentRed else GabayColors.AccentEmerald,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Direction text and distance
            Column(modifier = Modifier.weight(1f)) {
                val distText = if (telemetry.nextTurnDistanceM >= 1000f) {
                    String.format("%.1f KM", telemetry.nextTurnDistanceM / 1000f)
                } else {
                    "${telemetry.nextTurnDistanceM.toInt()} M"
                }

                Text(
                    text = distText,
                    style = GabayTypography.ManeuverDistance,
                    color = GabayColors.TextPrimary
                )

                Text(
                    text = telemetry.nextTurnRoad.uppercase(),
                    style = GabayTypography.StreetName,
                    color = GabayColors.TextSecondary,
                    maxLines = 1
                )
            }

            // Avoidance badges
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (vehicleMode == VehicleMode.SCOOTER) {
                    AvoidanceBadge(text = "NO TOLL")
                    AvoidanceBadge(text = "NO SKYWAY")
                } else {
                    AvoidanceBadge(text = "OPTIMAL")
                }
            }
        }
    }
}

@Composable
fun AvoidanceBadge(text: String) {
    Box(
        modifier = Modifier
            .background(GabayColors.BgElevated, RoundedCornerShape(4.dp))
            .border(1.dp, GabayColors.BorderSubtle, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = GabayTypography.BadgeText,
            color = GabayColors.AccentAmber
        )
    }
}

@Composable
fun BottomCockpitDock(
    telemetry: HudTelemetry,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(GabayColors.BgCard, RoundedCornerShape(12.dp))
            .border(1.dp, GabayColors.BorderSubtle, RoundedCornerShape(12.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Speed Column
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "LIVE SPEED",
                    style = GabayTypography.MonospaceLabel,
                    color = GabayColors.TextMuted
                )
                Text(
                    text = telemetry.liveSpeedKmh.toInt().toString(),
                    style = GabayTypography.SpeedNumber,
                    color = if (telemetry.liveSpeedKmh > 60f) GabayColors.AccentAmber else GabayColors.TextPrimary
                )
                Text(
                    text = "KM/H",
                    style = GabayTypography.TelemetryUnit,
                    color = GabayColors.TextSecondary
                )
            }

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(48.dp)
                    .background(GabayColors.BorderSubtle)
            )

            // Heading Column
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "HEADING",
                    style = GabayTypography.MonospaceLabel,
                    color = GabayColors.TextMuted
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = GabayIcons.CompassHeading,
                        contentDescription = "Compass",
                        tint = GabayColors.AccentCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${telemetry.headingCardinal} ${telemetry.headingDegrees.toInt()}°",
                        style = GabayTypography.TelemetryValue,
                        color = GabayColors.TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (telemetry.isOffRoute) "OFF-ROUTE" else "ON-TRACK",
                    style = GabayTypography.BadgeText,
                    color = if (telemetry.isOffRoute) GabayColors.AccentRed else GabayColors.AccentEmerald
                )
            }

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(48.dp)
                    .background(GabayColors.BorderSubtle)
            )

            // Distance & Pace Column
            Column(
                modifier = Modifier.weight(1.2f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "REMAINING / PACE",
                    style = GabayTypography.MonospaceLabel,
                    color = GabayColors.TextMuted
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = String.format("%.1f KM", telemetry.remainingDistanceKm),
                    style = GabayTypography.TelemetryValue,
                    color = GabayColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = telemetry.paceWindowMin,
                    style = GabayTypography.MonospaceLabel.copy(fontSize = 12.sp),
                    color = GabayColors.AccentEmerald
                )
            }
        }
    }
}

@Composable
fun GloveModeBar(
    selectedMode: VehicleMode,
    isSatelliteActive: Boolean,
    isTopoActive: Boolean,
    is3dActive: Boolean,
    onModeSelect: (VehicleMode) -> Unit,
    onToggleSat: () -> Unit,
    onToggleTopo: () -> Unit,
    onToggle3d: () -> Unit,
    onRecenterGps: () -> Unit,
    onVoiceCoPilot: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mode: Scooter
        ModePill(
            label = "SCOOTER",
            icon = GabayIcons.ModeScooter,
            isActive = selectedMode == VehicleMode.SCOOTER,
            onClick = { onModeSelect(VehicleMode.SCOOTER) }
        )

        // Mode: Car
        ModePill(
            label = "CAR",
            icon = GabayIcons.ModeCar,
            isActive = selectedMode == VehicleMode.CAR,
            onClick = { onModeSelect(VehicleMode.CAR) }
        )

        // Mode: Walk
        ModePill(
            label = "WALK",
            icon = GabayIcons.ModeWalk,
            isActive = selectedMode == VehicleMode.WALK,
            onClick = { onModeSelect(VehicleMode.WALK) }
        )

        // Layer: Macro Satellite Peek
        ModePill(
            label = "SAT",
            icon = GabayIcons.LayerSatellite,
            isActive = isSatelliteActive,
            onClick = onToggleSat
        )

        // Layer: Topography Contours
        ModePill(
            label = "TOPO",
            icon = GabayIcons.LayerTopo,
            isActive = isTopoActive,
            onClick = onToggleTopo
        )

        // Layer: 3D Extrusions
        ModePill(
            label = "3D",
            icon = GabayIcons.Layer3d,
            isActive = is3dActive,
            onClick = onToggle3d
        )

        // Recenter GPS
        ModePill(
            label = "RECENTER",
            icon = GabayIcons.RecenterGps,
            isActive = false,
            onClick = onRecenterGps
        )

        // Co-Pilot Voice Query
        ModePill(
            label = "GABAY VOICE",
            icon = GabayIcons.CopilotMic,
            isActive = true,
            activeColor = GabayColors.AccentCyan,
            onClick = onVoiceCoPilot
        )
    }
}

@Composable
fun ModePill(
    label: String,
    icon: ImageVector,
    isActive: Boolean,
    activeColor: Color = GabayColors.AccentEmerald,
    onClick: () -> Unit
) {
    val bgColor = if (isActive) activeColor.copy(alpha = 0.15f) else GabayColors.BgElevated
    val borderColor = if (isActive) activeColor else GabayColors.BorderSubtle
    val tintColor = if (isActive) activeColor else GabayColors.TextSecondary

    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 72.dp, minHeight = 48.dp)
            .background(bgColor, RoundedCornerShape(6.dp))
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tintColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = GabayTypography.PillText,
                color = tintColor
            )
        }
    }
}
