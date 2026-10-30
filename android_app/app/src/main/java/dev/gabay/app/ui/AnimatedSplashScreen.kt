package dev.gabay.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.gabay.app.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot

/**
 * Convx-Style Circular Ripple Engulfing Splash Transition
 * Directly uses the app launcher icon seamlessly with zero secondary text cards.
 *
 * Sequence:
 * 1. Matches exact position, scale, and background of Android OS system splash.
 * 2. Radiates an expanding circular liquid ripple wave from the icon center.
 * 3. Engulfs the entire screen with an amber-edged wavefront, revealing the live Cockpit.
 * 4. Dismisses cleanly from the composition tree.
 */
@Composable
fun AnimatedSplashScreen(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {}
) {
    var isDismissed by remember { mutableStateOf(false) }

    // Liquid ripple wave progression: 0f (center icon) -> 1f (engulfs entire diagonal)
    val rippleProgress = remember { Animatable(0f) }
    // Icon scale: 1.0f -> 1.12f as the ripple blasts outward
    val iconScale = remember { Animatable(1.0f) }
    // Icon alpha: 1.0f -> 0f as it dissolves into the live map center
    val iconAlpha = remember { Animatable(1.0f) }
    // Background obsidian scrim alpha
    val bgAlpha = remember { Animatable(1.0f) }

    LaunchedEffect(Unit) {
        // Brief 150ms hold to seamlessly anchor against the system window transition
        delay(150L)

        // Emil Kowalski high-fluidity expansion curve (0.16, 1, 0.3, 1)
        val fluidCurve = CubicBezierEasing(0.16f, 1.0f, 0.3f, 1.0f)

        // Launch simultaneous ripple blast & icon dissolve
        launch {
            iconScale.animateTo(
                targetValue = 1.15f,
                animationSpec = tween(durationMillis = 420, easing = fluidCurve)
            )
        }
        launch {
            delay(120L)
            iconAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing)
            )
        }
        launch {
            delay(200L)
            bgAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
            )
        }

        rippleProgress.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 550, easing = fluidCurve)
        )

        isDismissed = true
        onDismiss()
    }

    if (!isDismissed) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .alpha(bgAlpha.value),
            contentAlignment = Alignment.Center
        ) {
            // Ripple wave canvas that expands outward from center
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = hypot(size.width / 2f, size.height / 2f) * 1.15f
                val currentRadius = rippleProgress.value * maxRadius

                // Solid obsidian background
                drawRect(color = Color(0xFF0A0D10))

                if (currentRadius > 0f) {
                    // Expanding clear aperture revealing live cockpit underneath
                    drawCircle(
                        color = Color.Transparent,
                        radius = currentRadius,
                        center = center,
                        blendMode = BlendMode.Clear
                    )

                    // Glowing Solar Amber wavefront boundary
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFFFFB300).copy(alpha = (1f - rippleProgress.value * 0.7f).coerceIn(0f, 1f)),
                                Color(0xFFFFD54F).copy(alpha = (1f - rippleProgress.value).coerceIn(0f, 0.9f))
                            ),
                            center = center,
                            radius = currentRadius + 30f
                        ),
                        radius = currentRadius,
                        center = center,
                        style = Stroke(width = 8.dp.toPx())
                    )

                    // Secondary trailing echo wave
                    if (currentRadius > 40f) {
                        drawCircle(
                            color = Color(0xFFFFB300).copy(alpha = ((1f - rippleProgress.value) * 0.35f).coerceIn(0f, 1f)),
                            radius = currentRadius * 0.85f,
                            center = center,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            }

            // Direct App Launcher Icon (Identical to system splash icon)
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .scale(iconScale.value)
                    .alpha(iconAlpha.value)
                    .clip(CircleShape)
                    .background(Color(0xFF0A0D10)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_gabay_launcher_foreground),
                    contentDescription = "Gabay Icon",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
