package dev.gabay.app.ui

import android.view.HapticFeedbackConstants
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.gabay.app.R
import kotlin.math.hypot
import kotlin.math.pow

/**
 * Convx-Engineered Circular Ripple & Rotating Logo Splash Transition
 *
 * Architecture:
 * 1. Single clock frame-driven animation via withFrameNanos (TOTAL_MS = 1500f).
 * 2. Bold logo entrance rotating along round direction: starts at -140° and spins into True North 0°.
 * 3. Directional heading arrow inside pulses forward with vibrant terracotta ember glow.
 * 4. Dual physical rhythm beats (at 450ms and 750ms) firing tactile CLOCK_TICK haptics.
 * 5. Thick, bold ripple wavefront graphics radiating outward (Stroke width 10dp and 6dp).
 * 6. Smooth circular aperture clip reveal transitioning seamlessly into the live Cockpit HUD.
 */
@Composable
fun AnimatedSplashScreen(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {}
) {
    val view = LocalView.current
    var isDismissed by remember { mutableStateOf(false) }
    var timeMs by remember { mutableFloatStateOf(0f) }

    var beat1Fired by remember { mutableStateOf(false) }
    var beat2Fired by remember { mutableStateOf(false) }

    val totalDurationMs = 1500f
    val zoomStartMs = 950f
    val revealStartMs = 1200f

    // Frame-driven single clock loop
    LaunchedEffect(Unit) {
        val startNano = withFrameNanos { it }
        while (timeMs < totalDurationMs) {
            withFrameNanos { nowNano ->
                timeMs = ((nowNano - startNano) / 1_000_000f).coerceAtMost(totalDurationMs)
            }
        }
        isDismissed = true
        onDismiss()
    }

    // Trigger physical rhythm beats with tactile haptic ticks
    if (timeMs >= 450f && !beat1Fired) {
        beat1Fired = true
        try {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        } catch (_: Exception) {}
    }
    if (timeMs >= 750f && !beat2Fired) {
        beat2Fired = true
        try {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        } catch (_: Exception) {}
    }

    if (isDismissed) return

    // Convx easing math
    fun outCubic(t: Float): Float = 1f - (1f - t).pow(3)
    fun outBack(t: Float): Float {
        val c1 = 1.70158f
        val c3 = c1 + 1f
        val m = t - 1f
        return 1f + c3 * m.pow(3) + c1 * m.pow(2)
    }

    // Logo entrance & round direction spin (0..650ms)
    val glyphProgress = (timeMs / 650f).coerceIn(0f, 1f)
    val glyphRotation = -140f * (1f - outCubic(glyphProgress))
    val glyphScale = 0.65f + 0.35f * outBack(glyphProgress)
    val glyphAlpha = if (timeMs >= zoomStartMs) {
        (1f - ((timeMs - zoomStartMs) / 300f)).coerceIn(0f, 1f)
    } else {
        (timeMs / 200f).coerceIn(0f, 1f)
    }

    // Zoom tunnel scale at conclusion
    val zoomProgress = ((timeMs - zoomStartMs) / (totalDurationMs - zoomStartMs)).coerceIn(0f, 1f)
    val finalZoomScale = 1f + 14f * zoomProgress.pow(1.6f)

    // Aperture reveal progress
    val revealProgress = ((timeMs - revealStartMs) / (totalDurationMs - revealStartMs)).coerceIn(0f, 1f)
    val scrimAlpha = (1f - revealProgress).coerceIn(0f, 1f)

    // Ripple wave 1 progress (starts at 450ms)
    val ripple1Progress = if (timeMs > 450f) ((timeMs - 450f) / 750f).coerceIn(0f, 1f) else 0f
    // Ripple wave 2 progress (starts at 750ms)
    val ripple2Progress = if (timeMs > 750f) ((timeMs - 750f) / 600f).coerceIn(0f, 1f) else 0f

    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(scrimAlpha),
        contentAlignment = Alignment.Center
    ) {
        // Thick ripple graphics canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = hypot(size.width / 2f, size.height / 2f) * 1.25f

            // Solid obsidian background (#141A15)
            drawRect(color = Color(0xFF141A15))

            // Aperture reveal cutout
            if (revealProgress > 0f) {
                val apertureRadius = outCubic(revealProgress) * maxRadius
                drawCircle(
                    color = Color.Transparent,
                    radius = apertureRadius,
                    center = center,
                    blendMode = BlendMode.Clear
                )
            }

            // Wave 1: Thick Terracotta Clay Shockwave
            if (ripple1Progress > 0f) {
                val r1 = outCubic(ripple1Progress) * maxRadius
                val alpha1 = (1f - ripple1Progress * 0.85f).coerceIn(0f, 1f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFFE87A54).copy(alpha = alpha1),
                            Color(0xFFC96F4F).copy(alpha = alpha1 * 0.6f)
                        ),
                        center = center,
                        radius = r1 + 30f
                    ),
                    radius = r1,
                    center = center,
                    style = Stroke(width = 10.dp.toPx())
                )
            }

            // Wave 2: Bold Warm Sand Echo Wave
            if (ripple2Progress > 0f) {
                val r2 = outCubic(ripple2Progress) * maxRadius
                val alpha2 = (1f - ripple2Progress * 0.9f).coerceIn(0f, 1f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFFD9C9B2).copy(alpha = alpha2 * 0.8f),
                            Color(0xFF6B7F5B).copy(alpha = alpha2 * 0.4f)
                        ),
                        center = center,
                        radius = r2 + 20f
                    ),
                    radius = r2,
                    center = center,
                    style = Stroke(width = 6.dp.toPx())
                )
            }
        }

        // Animated Gabay App Logo with Round Direction Spin & Forward Heading Pulse
        Box(
            modifier = Modifier
                .size(86.dp)
                .scale(glyphScale * finalZoomScale)
                .rotate(glyphRotation)
                .alpha(glyphAlpha)
                .clip(CircleShape)
                .background(Color(0xFF141A15)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_gabay_launcher_foreground),
                contentDescription = "Gabay Monogram Logo",
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
