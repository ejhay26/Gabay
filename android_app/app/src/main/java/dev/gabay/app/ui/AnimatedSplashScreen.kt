package dev.gabay.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * Convx & Apple Design Inspired Liquid-Glass Animated Splash Screen
 * Features:
 * - Geometric Philippine Sun 8-ray Hexagonal Compass Mark
 * - Liquid Glass blur & Radial Solar Amber luminous aura
 * - Physics-backed spring scale & fade-in (critically damped)
 * - Seamless morph/scale expansion dissolve into live Cockpit
 */
@Composable
fun AnimatedSplashScreen(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {}
) {
    var isVisible by remember { mutableStateOf(true) }

    // Spring animation progression from 0f to 1f
    val animScale = remember { Animatable(0.7f) }
    val animAlpha = remember { Animatable(0f) }
    val pulseGlow = rememberInfiniteTransition(label = "pulse")
    val glowRadius by pulseGlow.animateFloat(
        initialValue = 180f,
        targetValue = 260f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    LaunchedEffect(Unit) {
        // Fast, direct spring response (Apple Design principle 1 & 4)
        animAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing)
        )
        animScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        // Hold for brief, polished brand impact
        delay(1300L)
        // Fluid liquid dissolve
        isVisible = false
        delay(550L)
        onDismiss()
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(animationSpec = tween(500, easing = FastOutSlowInEasing)) +
                scaleOut(targetScale = 1.15f, animationSpec = tween(500, easing = FastOutSlowInEasing)),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GabayColors.BgObsidian),
            contentAlignment = Alignment.Center
        ) {
            // LAYER 1: Ambient Solar Amber Radial Liquid Glass Halo
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GabayColors.AccentAmberGlow.copy(alpha = 0.22f),
                            GabayColors.AccentAmber.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = glowRadius
                    ),
                    radius = glowRadius,
                    center = center
                )
            }

            // LAYER 2: Frosted Liquid Glass Central Card
            Column(
                modifier = Modifier
                    .scale(animScale.value)
                    .alpha(animAlpha.value)
                    .clip(RoundedCornerShape(32.dp))
                    .background(GabayColors.UiCardElevated.copy(alpha = 0.75f))
                    .blur(0.5.dp)
                    .padding(horizontal = 48.dp, vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Tactical Philippine Sun & Compass Geometric Brand Mark (Logo Design)
                Canvas(modifier = Modifier.size(110.dp)) {
                    val c = Offset(size.width / 2f, size.height / 2f)
                    val r = size.minDimension / 2f

                    // 1. Outer Hexagonal Shield
                    val hexPath = Path()
                    for (i in 0 until 6) {
                        val angle = Math.toRadians((60.0 * i) - 30.0)
                        val x = c.x + (r * 0.95f * cos(angle)).toFloat()
                        val y = c.y + (r * 0.95f * sin(angle)).toFloat()
                        if (i == 0) hexPath.moveTo(x, y) else hexPath.lineTo(x, y)
                    }
                    hexPath.close()

                    drawPath(
                        path = hexPath,
                        color = GabayColors.AccentAmber,
                        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    // 2. Eight Radiant Sun Rays (Philippine Flag Sun Heritage)
                    for (i in 0 until 8) {
                        val angle = Math.toRadians(45.0 * i)
                        val startR = r * 0.48f
                        val endR = r * 0.78f
                        val p1 = Offset(
                            c.x + (startR * cos(angle)).toFloat(),
                            c.y + (startR * sin(angle)).toFloat()
                        )
                        val p2 = Offset(
                            c.x + (endR * cos(angle)).toFloat(),
                            c.y + (endR * sin(angle)).toFloat()
                        )
                        drawLine(
                            color = GabayColors.AccentAmber.copy(alpha = 0.85f),
                            start = p1,
                            end = p2,
                            strokeWidth = 2.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    // 3. Central Compass Iris Circle
                    drawCircle(
                        color = GabayColors.AccentAmber,
                        radius = r * 0.32f,
                        center = c,
                        style = Stroke(width = 2.5.dp.toPx())
                    )

                    // 4. North Arrow Tip in White
                    val northPath = Path().apply {
                        moveTo(c.x, c.y - r * 0.38f)
                        lineTo(c.x - 7.dp.toPx(), c.y - r * 0.12f)
                        lineTo(c.x + 7.dp.toPx(), c.y - r * 0.12f)
                        close()
                    }
                    drawPath(northPath, color = Color.White)

                    // 5. Center Golden Hub
                    drawCircle(
                        color = GabayColors.AccentAmberGlow,
                        radius = 4.dp.toPx(),
                        center = c
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Brand Title with Wide Optical Tracking
                Text(
                    text = "G A B A Y",
                    style = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp,
                        letterSpacing = 8.sp,
                        color = GabayColors.AccentAmber
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Subtitle
                Text(
                    text = "OFFLINE TACTICAL COCKPIT",
                    style = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 3.sp,
                        color = GabayColors.TextSecondary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Readiness Pulse Indicator Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(GabayColors.AccentAmber)
                    )
                    Text(
                        text = "READY",
                        style = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 9.sp,
                            letterSpacing = 2.sp,
                            color = GabayColors.AccentAmber
                        )
                    )
                }
            }
        }
    }
}
