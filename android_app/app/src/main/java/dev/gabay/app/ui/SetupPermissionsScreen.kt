package dev.gabay.app.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Apple Design (/apple-design) First-Launch Onboarding & Permission Setup.
 *
 * Provides a fluid, transparent, and purposeful setup flow before the rider enters the cockpit.
 * Eliminates the cold-start race condition where map tiles and the routing graph fail to load
 * because storage permission was requested too late.
 */
@Composable
fun SetupPermissionsScreen(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Reactive Permission States
    var isLocationGranted by remember { mutableStateOf(checkLocationGranted(context)) }
    var isStorageGranted by remember { mutableStateOf(checkStorageGranted(context)) }
    var isAudioGranted by remember { mutableStateOf(checkAudioGranted(context)) }
    var isNotificationGranted by remember { mutableStateOf(checkNotificationGranted(context)) }

    // Re-check permissions automatically on resume (e.g. returning from System Settings)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isLocationGranted = checkLocationGranted(context)
                isStorageGranted = checkStorageGranted(context)
                isAudioGranted = checkAudioGranted(context)
                isNotificationGranted = checkNotificationGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Permission Launchers
    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        isLocationGranted = checkLocationGranted(context)
    }

    val audioLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        isAudioGranted = checkAudioGranted(context)
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        isNotificationGranted = checkNotificationGranted(context)
    }

    fun requestStorageAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    context.startActivity(intent)
                } catch (e2: Exception) {}
            }
        } else {
            locationLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    val allEssentialGranted = isLocationGranted && isStorageGranted
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        animationSpec = spring(dampingRatio = 1.0f, stiffness = 400f),
        label = "ButtonSpringScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0E14))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Apple Brand Header: Cyber Chevron Icon Badge with Ambient Glow
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF232938), Color(0xFF141824))
                        )
                    )
                    .border(1.5.dp, Color(0xFF38435E), RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = GabayIcons.Compass,
                    contentDescription = "Gabay Logo",
                    tint = GabayColors.AccentSolar,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Headline with Apple Optical Sizing (-0.02em tracking, tight leading)
            Text(
                text = "Welcome to Gabay",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    letterSpacing = (-0.5).sp,
                    color = Color(0xFFF8FAFC)
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Set up essential permissions so your offline maps, vehicular telemetry, and voice co-pilot can run seamlessly.",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.5.sp,
                    lineHeight = 21.sp,
                    color = Color(0xFF94A3B8)
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 1. Precise Location Card
            ApplePermissionCard(
                icon = GabayIcons.RecenterGps,
                iconTint = Color(0xFF38BDF8),
                title = "Precise Location",
                badge = "REQUIRED",
                badgeColor = Color(0xFF38BDF8),
                description = "Enables live GPS speedometer, turn-by-turn routing, and vehicular orientation on Philippine roads.",
                isGranted = isLocationGranted,
                onGrantClick = {
                    locationLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Offline Map Vault Access Card
            ApplePermissionCard(
                icon = GabayIcons.LayerVector,
                iconTint = GabayColors.AccentSolar,
                title = "Offline Map Vault Access",
                badge = "REQUIRED",
                badgeColor = GabayColors.AccentSolar,
                description = "Directly accesses /sdcard/Documents/Gabay to read nationwide vector maps, SQLite POIs, and route graphs without mobile data.",
                isGranted = isStorageGranted,
                onGrantClick = { requestStorageAccess() }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Voice Co-Pilot Card
            ApplePermissionCard(
                icon = GabayIcons.CopilotMic,
                iconTint = Color(0xFFA855F7),
                title = "Voice Co-Pilot Mic",
                badge = "RECOMMENDED",
                badgeColor = Color(0xFFA855F7),
                description = "Enables hands-free driving voice commands and local road advisories while riding.",
                isGranted = isAudioGranted,
                onGrantClick = {
                    audioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Spacer(modifier = Modifier.height(14.dp))

                // 4. Background Guidance Notifications Card
                ApplePermissionCard(
                    icon = GabayIcons.NavCursor,
                    iconTint = Color(0xFF10B981),
                    title = "Background Guidance",
                    badge = "OPTIONAL",
                    badgeColor = Color(0xFF10B981),
                    description = "Maintains turn directions and telemetry notifications when your phone is locked or screen is off.",
                    isGranted = isNotificationGranted,
                    onGrantClick = {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Apple Call-To-Action Button: Fluid Spring Press
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(buttonScale)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (allEssentialGranted) {
                            Brush.horizontalGradient(
                                listOf(Color(0xFFFFB300), Color(0xFFFFA000))
                            )
                        } else {
                            Brush.horizontalGradient(
                                listOf(Color(0xFF232A3B), Color(0xFF1E2433))
                            )
                        }
                    )
                    .border(
                        1.dp,
                        if (allEssentialGranted) Color(0xFFFFD54F).copy(alpha = 0.5f) else Color(0xFF333E56),
                        RoundedCornerShape(18.dp)
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        onContinue()
                    }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (allEssentialGranted) "Continue to Navigation" else "Grant Permissions to Continue",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        letterSpacing = (-0.2).sp,
                        color = if (allEssentialGranted) Color(0xFF0B0E14) else Color(0xFF94A3B8)
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Permissions can be modified anytime in Android Settings.",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 11.5.sp,
                    color = Color(0xFF64748B)
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ApplePermissionCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    badge: String,
    badgeColor: Color,
    description: String,
    isGranted: Boolean,
    onGrantClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1.0f,
        animationSpec = spring(dampingRatio = 1.0f, stiffness = 400f),
        label = "CardSpringScale"
    )

    val cardBorderColor by animateColorAsState(
        targetValue = if (isGranted) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFF283044).copy(alpha = 0.7f),
        label = "BorderColor"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(cardScale)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF141824).copy(alpha = 0.85f))
            .border(1.dp, cardBorderColor, RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                if (!isGranted) onGrantClick()
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // SF-Style Rounded Icon Tile
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(iconTint.copy(alpha = 0.12f))
                .border(1.dp, iconTint.copy(alpha = 0.25f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Center: Title, Badge, Description
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        letterSpacing = (-0.2).sp,
                        color = Color(0xFFF8FAFC)
                    )
                )

                Spacer(modifier = Modifier.width(6.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badge,
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.5.sp,
                            color = badgeColor
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = description,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = Color(0xFF94A3B8)
                )
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Right Action Pill: Allowed Checkmark vs Grant Button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isGranted) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFF1E293B)
                )
                .border(
                    1.dp,
                    if (isGranted) Color(0xFF10B981).copy(alpha = 0.35f) else Color(0xFF475569),
                    RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isGranted) "✓ Active" else "Allow",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    color = if (isGranted) Color(0xFF34D399) else Color(0xFFF1F5F9)
                )
            )
        }
    }
}

// ----------------------------------------------------------------------------
// Permission Checking Helpers
// ----------------------------------------------------------------------------

private fun checkLocationGranted(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

private fun checkStorageGranted(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
}

private fun checkAudioGranted(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED
}

private fun checkNotificationGranted(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}
