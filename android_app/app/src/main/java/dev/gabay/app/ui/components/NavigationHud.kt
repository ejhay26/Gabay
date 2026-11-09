package dev.gabay.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.gabay.app.ui.theme.MutedText
import dev.gabay.app.ui.theme.TerracottaAccent
import uniffi.gabay_core.HudTelemetry

@Composable
fun NavigationHud(
    telemetry: HudTelemetry?,
    onStopNavigation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 12.dp,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val nextRoad = telemetry?.nextTurnRoad ?: "Navigating..."
            val distMeters = telemetry?.nextTurnDistanceM?.toInt() ?: 0
            val speedKmh = telemetry?.liveSpeedKmh?.toInt() ?: 0
            val remKm = telemetry?.remainingDistanceKm ?: 0.0

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = nextRoad,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = if (distMeters > 0) "In $distMeters meters" else "Follow route",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TerracottaAccent
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$speedKmh",
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Text(
                        text = "km/h",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedText
                    )
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format("Remaining: %.1f km", remKm),
                    style = MaterialTheme.typography.bodyMedium
                )

                Button(
                    onClick = onStopNavigation,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Exit")
                }
            }
        }
    }
}
