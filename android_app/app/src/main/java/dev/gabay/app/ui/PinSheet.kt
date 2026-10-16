package dev.gabay.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import uniffi.gabay_core.LatLng
import uniffi.gabay_core.PinInfo

@Composable
fun PinInspectorSheet(
    pinInfo: PinInfo,
    onStartRoute: (LatLng) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(GabayColors.BgCard, RoundedCornerShape(12.dp))
            .border(1.dp, GabayColors.BorderSubtle, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Road name and Close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = GabayIcons.DestinationPin,
                        contentDescription = "Pin",
                        tint = GabayColors.AccentEmerald,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = pinInfo.nearestRoadName.uppercase(),
                        style = GabayTypography.StreetName,
                        color = GabayColors.TextPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(GabayColors.BgElevated, RoundedCornerShape(6.dp))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = GabayIcons.ActionClose,
                        contentDescription = "Close",
                        tint = GabayColors.TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Barangay and City
            val subText = listOfNotNull(pinInfo.barangay, pinInfo.cityOrMunicipality).joinToString(", ")
            if (subText.isNotBlank()) {
                Text(
                    text = subText,
                    style = GabayTypography.MonospaceLabel,
                    color = GabayColors.TextSecondary
                )
            }

            // Coordinates Box with Copy Button
            val coordString = String.format(
                "%.5f° N, %.5f° E",
                pinInfo.coordinate.latitude,
                pinInfo.coordinate.longitude
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GabayColors.BgInput, RoundedCornerShape(6.dp))
                    .border(1.dp, GabayColors.BorderSubtle, RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = coordString,
                    style = GabayTypography.CoordinateDisplay,
                    color = GabayColors.AccentCyan
                )

                Box(
                    modifier = Modifier
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Coordinates", "${pinInfo.coordinate.latitude}, ${pinInfo.coordinate.longitude}")
                            clipboard.setPrimaryClip(clip)
                        }
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = GabayIcons.ActionCopy,
                        contentDescription = "Copy",
                        tint = GabayColors.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Action: Route Here Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .background(GabayColors.AccentEmerald, RoundedCornerShape(6.dp))
                    .clickable { onStartRoute(pinInfo.coordinate) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = GabayIcons.RerouteActive,
                        contentDescription = "Route Here",
                        tint = GabayColors.BgObsidian,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "NAVIGATE TO PIN",
                        style = GabayTypography.PillText.copy(color = GabayColors.BgObsidian)
                    )
                }
            }
        }
    }
}
