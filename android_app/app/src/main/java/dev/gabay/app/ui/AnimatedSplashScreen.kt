package dev.gabay.app.ui

import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.toPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import dev.gabay.app.R
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin

/**
 * Convx-Engineered Cold-Start Animated Splash Transition for Gabay.
 *
 * Architecture:
 * 1. Monotonic clock frame-driven animation via withFrameNanos.
 *    Uses actual elapsed monotonic wall-clock time so startup stalls or initial
 *    MapLibre GPU texture initialization do NOT artificially prolong the splash
 *    screen beyond TOTAL_MS.
 * 2. Instant tap-to-skip gesture so the user can immediately enter the app at any millisecond.
 * 3. Vector paths extracted directly from R.drawable.ic_gabay_launcher_foreground and rendered
 *    as pure GPU geometry inside Canvas, avoiding VectorPainter/Image re-rasterization memory spikes
 *    during 17x zoom tunnel.
 * 4. Hardware aperture hole punched via clipPath(hole, ClipOp.Difference), revealing the live
 *    app underneath with zero offscreen buffer allocation and no BlendMode.Clear glitches.
 * 5. Dual physical rhythm beats (at 450ms and 750ms) firing tactile CLOCK_TICK haptics.
 * 6. Accessibility-aware: aborts immediately if system ANIMATOR_DURATION_SCALE == 0f.
 */
@Composable
fun AnimatedSplashScreen(
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    val view = LocalView.current
    val reducedMotion = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val clock = remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        if (!reducedMotion) {
            val startNanos = withFrameNanos { it }
            var prevClock = 0f
            while (prevClock < TOTAL_MS) {
                val nowNanos = withFrameNanos { it }
                val elapsedMs = ((nowNanos - startNanos) / 1_000_000f).coerceAtMost(TOTAL_MS)
                val before = prevClock
                val after = elapsedMs
                prevClock = after
                clock.floatValue = after
                if (BEATS_MS.any { it in before..after && it != before }) {
                    try {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    } catch (_: Exception) {}
                }
            }
        }
        onDismiss()
    }

    if (reducedMotion) return

    val logoVector = ImageVector.vectorResource(R.drawable.ic_gabay_launcher_foreground)
    val vectorPaths = remember(logoVector) {
        logoVector.root.filterIsInstance<VectorPath>().map { it.pathData.toPath() }
    }
    val logoViewport = logoVector.viewportWidth.coerceAtLeast(1f)
    val hole = remember { Path() }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onDismiss() },
                    onPress = { onDismiss() }
                )
            }
    ) {
        val t = clock.floatValue
        val c = center
        val r0 = 52.dp.toPx()
        val cover = hypot(size.width, size.height) / 2f * 1.08f

        // Hardware aperture hole cutout revealing live Cockpit underneath
        val holeR = cover * outCubic(phase(t, REVEAL_START, TOTAL_MS))
        hole.reset()
        if (holeR > 0f) {
            hole.addOval(Rect(c, holeR))
        }

        clipPath(hole, ClipOp.Difference) {
            // Obsidian background (#141A15)
            drawRect(Color(0xFF141A15))

            // Ripples radiating beneath the central disc
            BEATS_MS.forEachIndexed { index, beat ->
                val x = (t - beat) / RIPPLE_MS
                if (x in 0f..1f) {
                    val rippleColor = if (index == 0) Color(0xFFE87A54) else Color(0xFFD9C9B2)
                    drawCircle(
                        color = rippleColor.copy(alpha = 0.50f * (1f - x)),
                        radius = r0 * (1f + 2.5f * outCubic(x)),
                        center = c,
                        style = Stroke(width = 2.5.dp.toPx() * (1f - 0.5f * x))
                    )
                }
            }

            // Central Disc Entrance & Zoom Tunnel
            val beatPulse = BEATS_MS.sumOf { pulse(t - it).toDouble() }.toFloat()
            val zoom = inCubic(phase(t, ZOOM_START, ZOOM_END))
            val discIn = outBack(phase(t, 0f, 450f))
            val discR = r0 * discIn * (1f + 0.08f * beatPulse) + (cover - r0) * zoom

            // Deep forest obsidian disc carrying the emblem
            drawCircle(
                color = Color(0xFF1E2721),
                radius = discR.coerceAtLeast(0f),
                center = c
            )
            // Subtle rim on disc
            drawCircle(
                color = Color(0xFF6B7F5B).copy(alpha = 0.35f * (1f - zoom)),
                radius = discR.coerceAtLeast(0f),
                center = c,
                style = Stroke(width = 1.dp.toPx())
            )

            // Gabay App Icon Vectors:
            // Grows faster than the disc while zooming, so terracotta and linen geometry sweep past the camera
            val glyphIn = phase(t, 80f, 550f)
            val glyphSize = r0 * 2f * 0.72f * outBack(glyphIn) * (1f + 0.08f * beatPulse) * (1f + 16f * zoom.pow(1.6f))
            val spin = -140f * (1f - outCubic(glyphIn)) + 35f * zoom

            if (glyphSize > 0f && vectorPaths.isNotEmpty()) {
                rotate(spin, pivot = c) {
                    translate(c.x - glyphSize / 2f, c.y - glyphSize / 2f) {
                        scale(glyphSize / logoViewport, pivot = Offset.Zero) {
                            if (vectorPaths.size >= 3) {
                                // Path 0: Outer Halo
                                drawPath(
                                    path = vectorPaths[0],
                                    color = Color(0xFF6B7F5B).copy(alpha = 0.45f),
                                    style = Stroke(width = 1.5f)
                                )
                                // Path 1: Master Gabay 'G' Monogram (Warm Terracotta Clay)
                                drawPath(
                                    path = vectorPaths[1],
                                    color = Color(0xFFC96F4F)
                                )
                                // Path 2: Directional Heading Chevron (Warm Alabaster Linen)
                                drawPath(
                                    path = vectorPaths[2],
                                    color = Color(0xFFF8F6EE)
                                )
                            } else {
                                // Fallback: draw all paths in Terracotta
                                vectorPaths.forEach { p ->
                                    drawPath(path = p, color = Color(0xFFC96F4F))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val TOTAL_MS = 1350f
private const val ZOOM_START = 800f
private const val ZOOM_END = 1200f
private const val REVEAL_START = 1120f
private const val RIPPLE_MS = 500f
private const val PULSE_MS = 180f
private val BEATS_MS = listOf(350f, 650f)

private fun phase(t: Float, start: Float, end: Float) = ((t - start) / (end - start)).coerceIn(0f, 1f)
private fun pulse(dt: Float) = if (dt in 0f..PULSE_MS) sin(PI.toFloat() * dt / PULSE_MS) else 0f
private fun outCubic(x: Float) = 1f - (1f - x).pow(3)
private fun inCubic(x: Float) = x * x * x
private fun outBack(x: Float): Float {
    val c1 = 1.70158f
    val c3 = c1 + 1f
    return 1f + c3 * (x - 1f).pow(3) + c1 * (x - 1f).pow(2)
}
