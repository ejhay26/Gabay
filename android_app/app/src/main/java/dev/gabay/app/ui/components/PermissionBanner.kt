package dev.gabay.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.gabay.app.core.AppPermissionsState
import dev.gabay.app.ui.theme.TerracottaAccent

@Composable
fun PermissionBanner(
    permissions: AppPermissionsState,
    onRequestLocation: () -> Unit,
    onRequestStorage: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (permissions.allEssentialGranted) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Permissions Required",
                style = MaterialTheme.typography.titleLarge
            )

            if (!permissions.hasLocationPermission) {
                Text(
                    text = "Location access is needed for GPS positioning and route navigation.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onRequestLocation,
                    colors = ButtonDefaults.buttonColors(containerColor = TerracottaAccent)
                ) {
                    Text("Grant Location Access")
                }
            }

            if (!permissions.hasStoragePermission) {
                Text(
                    text = "Storage access is needed to read offline map tiles (PMTiles) and road graphs from Documents/Gabay.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = onRequestStorage
                ) {
                    Text("Grant All Files Access")
                }
            }
        }
    }
}
