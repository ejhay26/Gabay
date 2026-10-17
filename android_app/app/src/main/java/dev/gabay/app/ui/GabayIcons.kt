package dev.gabay.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Gabay Global Icon Registry
 * Pixel-aligned Lucide vector icons with 1.75dp stroke width.
 * Adheres strictly to 01_ANTI_SLOP_AND_UI_DESIGN.md (Zero emojis, zero bloated gradients).
 */
object GabayIcons {
    private const val DefaultStroke = 1.75f

    // ic_action_check
    val ActionCheckRes: Int = dev.gabay.app.R.drawable.ic_action_check
    val ActionCheck: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "ActionCheck",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M20 6 9 17l-5-5").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    // ic_action_close
    val ActionCloseRes: Int = dev.gabay.app.R.drawable.ic_action_close
    val ActionClose: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "ActionClose",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M18 6 6 18").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("m6 6 12 12").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    // ic_action_copy
    val ActionCopyRes: Int = dev.gabay.app.R.drawable.ic_action_copy
    val ActionCopy: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "ActionCopy",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M 10.0 8.0 h 10.0 a 2.0 2.0 0 0 1 2.0 2.0 v 10.0 a 2.0 2.0 0 0 1 -2.0 2.0 h -10.0 a 2.0 2.0 0 0 1 -2.0 -2.0 v -10.0 a 2.0 2.0 0 0 1 2.0 -2.0 Z").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    // ic_audio_mute
    val AudioMuteRes: Int = dev.gabay.app.R.drawable.ic_audio_mute
    val AudioMute: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "AudioMute",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M11 4.702a.7.7 0 0 0-1.203-.498L6.413 7.587A1.4 1.4 0 0 1 5.416 8H3a1 1 0 0 0-1 1v6a1 1 0 0 0 1 1h2.416a1.4 1.4 0 0 1 .997.413l3.383 3.384A.7.7 0 0 0 11 19.298z").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("m16.5 14.5 5-5").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("m16.5 9.5 5 5").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    // ic_audio_tts
    val AudioTtsRes: Int = dev.gabay.app.R.drawable.ic_audio_tts
    val AudioTts: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "AudioTts",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M11 4.702a.705.705 0 0 0-1.203-.498L6.413 7.587A1.4 1.4 0 0 1 5.416 8H3a1 1 0 0 0-1 1v6a1 1 0 0 0 1 1h2.416a1.4 1.4 0 0 1 .997.413l3.383 3.384A.705.705 0 0 0 11 19.298z").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M16 9a5 5 0 0 1 0 6").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M19.364 18.364a9 9 0 0 0 0-12.728").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    // ic_chevron_down
    val ChevronDownRes: Int = dev.gabay.app.R.drawable.ic_chevron_down
    val ChevronDown: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "ChevronDown",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("m6 9 6 6 6-6").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    // ic_chevron_left
    val ChevronLeftRes: Int = dev.gabay.app.R.drawable.ic_chevron_left
    val ChevronLeft: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "ChevronLeft",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("m15 18-6-6 6-6").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    // ic_chevron_up
    val ChevronUpRes: Int = dev.gabay.app.R.drawable.ic_chevron_up
    val ChevronUp: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "ChevronUp",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("m18 15-6-6-6 6").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    // ic_compass
    val CompassRes: Int = dev.gabay.app.R.drawable.ic_compass
    val Compass: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "Compass",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M 2.000 12.000 a 10.000 10.000 0 1 0 20.000 0 a 10.000 10.000 0 1 0 -20.000 0 Z").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("m16.24 7.76-1.804 5.411a2 2 0 0 1-1.265 1.265L7.76 16.24l1.804-5.411a2 2 0 0 1 1.265-1.265z").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    // ic_compass_heading
    val CompassHeadingRes: Int = dev.gabay.app.R.drawable.ic_compass_heading
    val CompassHeading: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "CompassHeading",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M 2.000 12.000 a 10.000 10.000 0 1 0 20.000 0 a 10.000 10.000 0 1 0 -20.000 0 Z").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("m16.24 7.76-1.804 5.411a2 2 0 0 1-1.265 1.265L7.76 16.24l1.804-5.411a2 2 0 0 1 1.265-1.265z").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    // ic_copilot_mic
    val CopilotMicRes: Int = dev.gabay.app.R.drawable.ic_copilot_mic
    val CopilotMic: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "CopilotMic",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M12 19v3").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M19 10v2a7 7 0 0 1-14 0v-2").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M 12.0 2.0 h 0.0 a 3.0 3.0 0 0 1 3.0 3.0 v 7.0 a 3.0 3.0 0 0 1 -3.0 3.0 h -0.0 a 3.0 3.0 0 0 1 -3.0 -3.0 v -7.0 a 3.0 3.0 0 0 1 3.0 -3.0 Z").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    // ic_crosshairs
    val CrosshairsRes: Int = dev.gabay.app.R.drawable.ic_crosshairs
    val Crosshairs: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "Crosshairs",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M 2.000 12.000 a 10.000 10.000 0 1 0 20.000 0 a 10.000 10.000 0 1 0 -20.000 0 Z").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M 22 12 L 18 12").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M 6 12 L 2 12").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M 12 6 L 12 2").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M 12 22 L 12 18").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    // ic_destination_pin
    val DestinationPinRes: Int = dev.gabay.app.R.drawable.ic_destination_pin
    val DestinationPin: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "DestinationPin",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M20 10c0 4.993-5.539 10.193-7.399 11.799a1 1 0 0 1-1.202 0C9.539 20.193 4 14.993 4 10a8 8 0 0 1 16 0").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M 9.000 10.000 a 3.000 3.000 0 1 0 6.000 0 a 3.000 3.000 0 1 0 -6.000 0 Z").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    // ic_finish_flag
    val FinishFlagRes: Int = dev.gabay.app.R.drawable.ic_finish_flag
    val FinishFlag: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "FinishFlag",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M4 22V4a1 1 0 0 1 .4-.8A6 6 0 0 1 8 2c3 0 5 2 7.333 2q2 0 3.067-.8A1 1 0 0 1 20 4v10a1 1 0 0 1-.4.8A6 6 0 0 1 16 16c-3 0-5-2-8-2a6 6 0 0 0-4 1.528").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    // ic_layer3d
    val Layer3dRes: Int = dev.gabay.app.R.drawable.ic_layer3d
    val Layer3d: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "Layer3d",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M21 8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16Z").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("m3.3 7 8.7 5 8.7-5").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M12 22V12").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    // ic_layer_satellite
    val LayerSatelliteRes: Int = dev.gabay.app.R.drawable.ic_layer_satellite
    val LayerSatellite: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "LayerSatellite",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M 2.000 12.000 a 10.000 10.000 0 1 0 20.000 0 a 10.000 10.000 0 1 0 -20.000 0 Z").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M12 2a14.5 14.5 0 0 0 0 20 14.5 14.5 0 0 0 0-20").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M2 12h20").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    // ic_layer_stack
    val LayerStackRes: Int = dev.gabay.app.R.drawable.ic_layer_stack
    val LayerStack: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "LayerStack",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M12.83 2.18a2 2 0 0 0-1.66 0L2.6 6.08a1 1 0 0 0 0 1.83l8.58 3.91a2 2 0 0 0 1.66 0l8.58-3.9a1 1 0 0 0 0-1.83z").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M2 12a1 1 0 0 0 .58.91l8.6 3.91a2 2 0 0 0 1.65 0l8.58-3.9A1 1 0 0 0 22 12").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M2 17a1 1 0 0 0 .58.91l8.6 3.91a2 2 0 0 0 1.65 0l8.58-3.9A1 1 0 0 0 22 17").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    // ic_layer_topo
    val LayerTopoRes: Int = dev.gabay.app.R.drawable.ic_layer_topo
    val LayerTopo: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "LayerTopo",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("m8 3 4 8 5-5 5 15H2L8 3z").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    // ic_layer_vector
    val LayerVectorRes: Int = dev.gabay.app.R.drawable.ic_layer_vector
    val LayerVector: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "LayerVector",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M14.106 5.553a2 2 0 0 0 1.788 0l3.659-1.83A1 1 0 0 1 21 4.619v12.764a1 1 0 0 1-.553.894l-4.553 2.277a2 2 0 0 1-1.788 0l-4.212-2.106a2 2 0 0 0-1.788 0l-3.659 1.83A1 1 0 0 1 3 19.381V6.618a1 1 0 0 1 .553-.894l4.553-2.277a2 2 0 0 1 1.788 0z").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M15 5.764v15").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M9 3.236v15").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    // ic_maneuver_fork
    val ManeuverForkRes: Int = dev.gabay.app.R.drawable.ic_maneuver_fork
    val ManeuverFork: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "ManeuverFork",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M15 6a9 9 0 0 0-9 9V3").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M 15.000 6.000 a 3.000 3.000 0 1 0 6.000 0 a 3.000 3.000 0 1 0 -6.000 0 Z").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M 3.000 18.000 a 3.000 3.000 0 1 0 6.000 0 a 3.000 3.000 0 1 0 -6.000 0 Z").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    // ic_maneuver_left
    val ManeuverLeftRes: Int = dev.gabay.app.R.drawable.ic_maneuver_left
    val ManeuverLeft: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "ManeuverLeft",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M20 20v-7a4 4 0 0 0-4-4H4").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M9 14 4 9l5-5").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    // ic_maneuver_merge
    val ManeuverMergeRes: Int = dev.gabay.app.R.drawable.ic_maneuver_merge
    val ManeuverMerge: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "ManeuverMerge",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M 15.000 18.000 a 3.000 3.000 0 1 0 6.000 0 a 3.000 3.000 0 1 0 -6.000 0 Z").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M 3.000 6.000 a 3.000 3.000 0 1 0 6.000 0 a 3.000 3.000 0 1 0 -6.000 0 Z").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M6 21V9a9 9 0 0 0 9 9").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    // ic_maneuver_right
    val ManeuverRightRes: Int = dev.gabay.app.R.drawable.ic_maneuver_right
    val ManeuverRight: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "ManeuverRight",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("m15 14 5-5-5-5").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M4 20v-7a4 4 0 0 1 4-4h12").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    // ic_maneuver_slight_left
    val ManeuverSlightLeftRes: Int = dev.gabay.app.R.drawable.ic_maneuver_slight_left
    val ManeuverSlightLeft: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "ManeuverSlightLeft",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M7 17V7h10").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M17 17 7 7").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    // ic_maneuver_slight_right
    val ManeuverSlightRightRes: Int = dev.gabay.app.R.drawable.ic_maneuver_slight_right
    val ManeuverSlightRight: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "ManeuverSlightRight",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M7 7h10v10").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M7 17 17 7").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    // ic_maneuver_straight
    val ManeuverStraightRes: Int = dev.gabay.app.R.drawable.ic_maneuver_straight
    val ManeuverStraight: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "ManeuverStraight",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("m5 12 7-7 7 7").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M12 19V5").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    // ic_maneuver_u_turn
    val ManeuverUTurnRes: Int = dev.gabay.app.R.drawable.ic_maneuver_u_turn
    val ManeuverUTurn: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "ManeuverUTurn",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M9 14 4 9l5-5").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M4 9h10.5a5.5 5.5 0 0 1 5.5 5.5a5.5 5.5 0 0 1-5.5 5.5H11").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    // ic_menu
    val MenuRes: Int = dev.gabay.app.R.drawable.ic_menu
    val Menu: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "Menu",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M4 5h16").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M4 12h16").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M4 19h16").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    // ic_mode_car
    val ModeCarRes: Int = dev.gabay.app.R.drawable.ic_mode_car
    val ModeCar: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "ModeCar",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M19 17h2c.6 0 1-.4 1-1v-3c0-.9-.7-1.7-1.5-1.9C18.7 10.6 16 10 16 10s-1.3-1.4-2.2-2.3c-.5-.4-1.1-.7-1.8-.7H5c-.6 0-1.1.4-1.4.9l-1.4 2.9A3.7 3.7 0 0 0 2 12v4c0 .6.4 1 1 1h2").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M 5.000 17.000 a 2.000 2.000 0 1 0 4.000 0 a 2.000 2.000 0 1 0 -4.000 0 Z").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M9 17h6").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M 15.000 17.000 a 2.000 2.000 0 1 0 4.000 0 a 2.000 2.000 0 1 0 -4.000 0 Z").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    // ic_mode_scooter
    val ModeScooterRes: Int = dev.gabay.app.R.drawable.ic_mode_scooter
    val ModeScooter: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "ModeScooter",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M 15.000 17.500 a 3.500 3.500 0 1 0 7.000 0 a 3.500 3.500 0 1 0 -7.000 0 Z").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M 2.000 17.500 a 3.500 3.500 0 1 0 7.000 0 a 3.500 3.500 0 1 0 -7.000 0 Z").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M 14.000 5.000 a 1.000 1.000 0 1 0 2.000 0 a 1.000 1.000 0 1 0 -2.000 0 Z").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M12 17.5V14l-3-3 4-3 2 3h2").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    // ic_mode_walk
    val ModeWalkRes: Int = dev.gabay.app.R.drawable.ic_mode_walk
    val ModeWalk: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "ModeWalk",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M4 16v-2.38C4 11.5 2.97 10.5 3 8c.03-2.72 1.49-6 4.5-6C9.37 2 10 3.8 10 5.5c0 3.11-2 5.66-2 8.68V16a2 2 0 1 1-4 0Z").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M20 20v-2.38c0-2.12 1.03-3.12 1-5.62-.03-2.72-1.49-6-4.5-6C14.63 6 14 7.8 14 9.5c0 3.11 2 5.66 2 8.68V20a2 2 0 1 0 4 0Z").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M16 17h4").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M4 13h4").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    // ic_nav_cursor
    val NavCursorRes: Int = dev.gabay.app.R.drawable.ic_nav_cursor
    val NavCursor: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "NavCursor",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M 3 11 L 22 2 L 13 21 L 11 13 L 3 11 Z").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    // ic_recenter_gps
    val RecenterGpsRes: Int = dev.gabay.app.R.drawable.ic_recenter_gps
    val RecenterGps: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "RecenterGps",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M 2.000 12.000 a 10.000 10.000 0 1 0 20.000 0 a 10.000 10.000 0 1 0 -20.000 0 Z").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M 22 12 L 18 12").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M 6 12 L 2 12").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M 12 6 L 12 2").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M 12 22 L 12 18").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    // ic_reroute_active
    val RerouteActiveRes: Int = dev.gabay.app.R.drawable.ic_reroute_active
    val RerouteActive: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "RerouteActive",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M 3.000 19.000 a 3.000 3.000 0 1 0 6.000 0 a 3.000 3.000 0 1 0 -6.000 0 Z").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M9 19h8.5a3.5 3.5 0 0 0 0-7h-11a3.5 3.5 0 0 1 0-7H15").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M 15.000 5.000 a 3.000 3.000 0 1 0 6.000 0 a 3.000 3.000 0 1 0 -6.000 0 Z").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    // ic_road_prohibited
    val RoadProhibitedRes: Int = dev.gabay.app.R.drawable.ic_road_prohibited
    val RoadProhibited: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "RoadProhibited",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M 2.000 12.000 a 10.000 10.000 0 1 0 20.000 0 a 10.000 10.000 0 1 0 -20.000 0 Z").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M4.929 4.929 19.07 19.071").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    // ic_rotate_ccw
    val RotateCcwRes: Int = dev.gabay.app.R.drawable.ic_rotate_ccw
    val RotateCcw: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "RotateCcw",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M3 3v5h5").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    // ic_route_dist
    val RouteDistRes: Int = dev.gabay.app.R.drawable.ic_route_dist
    val RouteDist: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "RouteDist",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M12 13v8").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M12 3v3").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M18.172 6a2 2 0 0 1 1.414.586l2.06 2.06a1.207 1.207 0 0 1 0 1.708l-2.06 2.06a2 2 0 0 1-1.414.586H4a1 1 0 0 1-1-1V7a1 1 0 0 1 1-1z").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    // ic_search_poi
    val SearchPoiRes: Int = dev.gabay.app.R.drawable.ic_search_poi
    val SearchPoi: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "SearchPoi",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("m21 21-4.34-4.34").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M 3.000 11.000 a 8.000 8.000 0 1 0 16.000 0 a 8.000 8.000 0 1 0 -16.000 0 Z").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    // ic_send
    val SendRes: Int = dev.gabay.app.R.drawable.ic_send
    val Send: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "Send",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M14.536 21.686a.5.5 0 0 0 .937-.024l6.5-19a.496.496 0 0 0-.635-.635l-19 6.5a.5.5 0 0 0-.024.937l7.93 3.18a2 2 0 0 1 1.112 1.11z").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("m21.854 2.147-10.94 10.939").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    // ic_settings_tuning
    val SettingsTuningRes: Int = dev.gabay.app.R.drawable.ic_settings_tuning
    val SettingsTuning: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "SettingsTuning",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M10 5H3").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M12 19H3").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M14 3v4").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M16 17v4").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M21 12h-9").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M21 19h-5").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M21 5h-7").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M8 10v4").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M8 12H3").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    // ic_speed_gauge
    val SpeedGaugeRes: Int = dev.gabay.app.R.drawable.ic_speed_gauge
    val SpeedGauge: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "SpeedGauge",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("m12 14 4-4").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M3.34 19a10 10 0 1 1 17.32 0").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    // ic_time_pace
    val TimePaceRes: Int = dev.gabay.app.R.drawable.ic_time_pace
    val TimePace: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "TimePace",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M 10 2 L 14 2").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M 12 14 L 15 11").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M 4.000 14.000 a 8.000 8.000 0 1 0 16.000 0 a 8.000 8.000 0 1 0 -16.000 0 Z").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    // ic_turn_arrow_left
    val TurnArrowLeftRes: Int = dev.gabay.app.R.drawable.ic_turn_arrow_left
    val TurnArrowLeft: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "TurnArrowLeft",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("m12 19-7-7 7-7").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M19 12H5").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    // ic_turn_arrow_right
    val TurnArrowRightRes: Int = dev.gabay.app.R.drawable.ic_turn_arrow_right
    val TurnArrowRight: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "TurnArrowRight",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M5 12h14").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("m12 5 7 7-7 7").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    // ic_warning_hazard
    val WarningHazardRes: Int = dev.gabay.app.R.drawable.ic_warning_hazard
    val WarningHazard: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "WarningHazard",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M12 9v4").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M12 17h.01").toNodes(),
            stroke = SolidColor(Color(0xFFF0F4F8)),
            strokeLineWidth = DefaultStroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

}
