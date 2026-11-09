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
import uniffi.gabay_core.PoiSearchResult
import uniffi.gabay_core.RouteResult

@Composable
fun RouteSheet(
    poi: PoiSearchResult,
    route: RouteResult?,
    isRouting: Boolean,
    onStartNavigation: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 12.dp,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = poi.name,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "${poi.city}, ${poi.province}".trim().removePrefix(",").removeSuffix(","),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedText
                    )
                }
                TextButton(onClick = onClose) {
                    Text("Close")
                }
            }

            if (isRouting) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = TerracottaAccent
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Calculating optimal route...")
                }
            } else if (route != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format("%.1f km", route.totalDistanceKm),
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(text = "Distance", style = MaterialTheme.typography.bodyMedium, color = MutedText)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${route.estimatedDurationMin} min",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(text = "Travel Time", style = MaterialTheme.typography.bodyMedium, color = MutedText)
                    }
                }

                Button(
                    onClick = onStartNavigation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TerracottaAccent)
                ) {
                    Text("Start Navigation", style = MaterialTheme.typography.labelLarge)
                }
            } else {
                Text(
                    text = "Could not calculate a road route to this destination.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
