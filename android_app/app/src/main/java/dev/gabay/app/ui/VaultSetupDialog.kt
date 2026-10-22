package dev.gabay.app.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.gabay.app.data.VaultFileItem
import dev.gabay.app.data.VaultIntegrityManager
import dev.gabay.app.data.VaultStatus
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun VaultSetupDialog(
    vaultStatus: VaultStatus,
    onDismiss: () -> Unit,
    onVaultReady: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isDownloading by remember { mutableStateOf(false) }
    var currentDownloadingFile by remember { mutableStateOf<String?>(null) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadedBytesText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = { if (!isDownloading) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = !isDownloading, dismissOnClickOutside = !isDownloading)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(GabayColors.UiSurfaceElevated)
                .border(1.dp, GabayColors.UiBorder, RoundedCornerShape(20.dp))
                .padding(22.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "OFFLINE VAULT SETUP",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = GabayColors.AccentSolar
                        )
                    )
                    Text(
                        text = "Zero-Data Navigation Repository",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = GabayColors.TextMuted
                        )
                    )
                }

                if (!isDownloading) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(GabayColors.UiInput)
                            .clickable { onDismiss() }
                            .padding(6.dp)
                    ) {
                        Icon(
                            imageVector = GabayIcons.ActionClose,
                            contentDescription = "Close",
                            tint = GabayColors.TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Gabay runs completely offline without cellular reception. To enable nationwide turn-by-turn routing, vector maps, and search, the following binary vault files are required:",
                style = TextStyle(fontSize = 12.sp, color = GabayColors.TextPrimary, lineHeight = 17.sp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // File items list
            vaultStatus.files.forEach { fileItem ->
                VaultFileRow(item = fileItem)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Error display
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = errorMessage ?: "",
                    style = TextStyle(color = GabayColors.AccentFire, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Download Progress Bar or Action Buttons
            if (isDownloading) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GabayColors.UiInput, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = currentDownloadingFile ?: "Downloading...",
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = GabayColors.TextPrimary, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${(downloadProgress * 100).toInt()}%",
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = GabayColors.AccentSolar, fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { downloadProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = GabayColors.AccentSolar,
                        trackColor = GabayColors.UiBorder
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = downloadedBytesText,
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = GabayColors.TextMuted)
                    )
                }
            } else {
                val totalMb = (vaultStatus.totalMissingBytes / (1024.0 * 1024.0)).toInt()

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(GabayColors.AccentSolar)
                            .clickable {
                                isDownloading = true
                                errorMessage = null
                                coroutineScope.launch {
                                    var allSuccess = true
                                    val missing = vaultStatus.missingFiles
                                    for ((index, fileItem) in missing.withIndex()) {
                                        currentDownloadingFile = "Fetching ${fileItem.filename} (${index + 1}/${missing.size})"
                                        val success = VaultIntegrityManager.streamDownloadFile(fileItem) { downloaded, total ->
                                            downloadProgress = (downloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                                            val downMb = String.format(Locale.US, "%.1f", downloaded / (1024.0 * 1024.0))
                                            val totMb = String.format(Locale.US, "%.1f", total / (1024.0 * 1024.0))
                                            downloadedBytesText = "$downMb MB of $totMb MB"
                                        }
                                        if (!success) {
                                            allSuccess = false
                                            errorMessage = "Failed to download ${fileItem.filename}. Please check your connection."
                                            break
                                        }
                                    }
                                    isDownloading = false
                                    if (allSuccess) {
                                        onVaultReady()
                                    }
                                }
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "DOWNLOAD OFFLINE VAULT ($totalMb MB)",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = GabayColors.TextInverse
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(GabayColors.UiInput)
                            .clickable { onDismiss() }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "SKIP / USE MANUAL ADB PUSH",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                color = GabayColors.TextMuted
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VaultFileRow(item: VaultFileItem) {
    val sizeMb = String.format(Locale.US, "%.1f MB", item.expectedSizeBytes / (1024.0 * 1024.0))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GabayColors.UiInput, RoundedCornerShape(8.dp))
            .border(
                1.dp,
                if (item.isPresent) GabayColors.AccentMint.copy(alpha = 0.4f) else GabayColors.UiBorder,
                RoundedCornerShape(8.dp)
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.filename,
                style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = GabayColors.TextPrimary)
            )
            Text(
                text = item.description,
                style = TextStyle(fontSize = 10.sp, color = GabayColors.TextMuted),
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (item.isPresent) {
            Box(
                modifier = Modifier
                    .background(GabayColors.AccentMint.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "READY",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = GabayColors.AccentMint)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .background(GabayColors.AccentSolar.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = sizeMb,
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = GabayColors.AccentSolar)
                )
            }
        }
    }
}
