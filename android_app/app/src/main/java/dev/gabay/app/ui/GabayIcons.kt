package dev.gabay.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Gabay Global Tactical Icon Registry (Phosphor Icons Bold)
 * Pure vector outline geometry tailored for high-contrast motorcycle/scooter cockpit HUDs.
 * Replaces generic icon sets with high-legibility tactical icons.
 */
object GabayIcons {
    // ic_action_check
    val ActionCheckRes: Int = dev.gabay.app.R.drawable.ic_action_check
    val ActionCheck: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "ActionCheck",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 256f,
            viewportHeight = 256f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M232.49,80.49l-128,128a12,12,0,0,1-17,0l-56-56a12,12,0,1,1,17-17L96,183,215.51,63.51a12,12,0,0,1,17,17Z").toNodes(),
            fill = SolidColor(Color(0xFFF0F4F8))
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
            viewportWidth = 256f,
            viewportHeight = 256f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M208.49,191.51a12,12,0,0,1-17,17L128,145,64.49,208.49a12,12,0,0,1-17-17L111,128,47.51,64.49a12,12,0,0,1,17-17L128,111l63.51-63.52a12,12,0,0,1,17,17L145,128Z").toNodes(),
            fill = SolidColor(Color(0xFFF0F4F8))
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
            viewportWidth = 256f,
            viewportHeight = 256f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M216,28H88A12,12,0,0,0,76,40V76H40A12,12,0,0,0,28,88V216a12,12,0,0,0,12,12H168a12,12,0,0,0,12-12V180h36a12,12,0,0,0,12-12V40A12,12,0,0,0,216,28ZM156,204H52V100H156Zm48-48H180V88a12,12,0,0,0-12-12H100V52H204Z").toNodes(),
            fill = SolidColor(Color(0xFFF0F4F8))
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
            viewportWidth = 256f,
            viewportHeight = 256f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M56.88,31.93A12,12,0,1,0,39.12,48.07L64.51,76H32A20,20,0,0,0,12,96v64a20,20,0,0,0,20,20H75.88l68.75,53.47A12,12,0,0,0,164,224V185.44l35.12,38.63a12,12,0,0,0,17.76-16.14ZM36,100H68v56H36Zm104,99.46L92,162.13V106.24L140,159Zm-31-134a12,12,0,0,1,2.11-16.84l33.51-26.07A12,12,0,0,1,164,32V94.94a12,12,0,0,1-24,0V56.54l-14.15,11A12,12,0,0,1,109,65.44Zm74,49.35a12,12,0,0,1,18-15.85,44,44,0,0,1,5.55,50.21,12,12,0,0,1-21-11.55A19.67,19.67,0,0,0,188,128,20,20,0,0,0,183,114.79ZM252,128a84.18,84.18,0,0,1-19.11,53.35,12,12,0,1,1-18.53-15.25A60,60,0,0,0,212.73,88a12,12,0,1,1,17.88-16A83.87,83.87,0,0,1,252,128Z").toNodes(),
            fill = SolidColor(Color(0xFFF0F4F8))
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
            viewportWidth = 256f,
            viewportHeight = 256f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M157.27,21.22a12,12,0,0,0-12.64,1.31L75.88,76H32A20,20,0,0,0,12,96v64a20,20,0,0,0,20,20H75.88l68.75,53.47A12,12,0,0,0,164,224V32A12,12,0,0,0,157.27,21.22ZM36,100H68v56H36Zm104,99.46L92,162.13V93.87l48-37.33ZM212,128a44,44,0,0,1-11,29.11,12,12,0,1,1-18-15.88,20,20,0,0,0,0-26.43,12,12,0,0,1,18-15.86A43.94,43.94,0,0,1,212,128Zm40,0a83.87,83.87,0,0,1-21.39,56,12,12,0,0,1-17.89-16,60,60,0,0,0,0-80,12,12,0,1,1,17.88-16A83.87,83.87,0,0,1,252,128Z").toNodes(),
            fill = SolidColor(Color(0xFFF0F4F8))
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
            viewportWidth = 256f,
            viewportHeight = 256f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M216.49,104.49l-80,80a12,12,0,0,1-17,0l-80-80a12,12,0,0,1,17-17L128,159l71.51-71.52a12,12,0,0,1,17,17Z").toNodes(),
            fill = SolidColor(Color(0xFFF0F4F8))
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
            viewportWidth = 256f,
            viewportHeight = 256f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M168.49,199.51a12,12,0,0,1-17,17l-80-80a12,12,0,0,1,0-17l80-80a12,12,0,0,1,17,17L97,128Z").toNodes(),
            fill = SolidColor(Color(0xFFF0F4F8))
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
            viewportWidth = 256f,
            viewportHeight = 256f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M216.49,168.49a12,12,0,0,1-17,0L128,97,56.49,168.49a12,12,0,0,1-17-17l80-80a12,12,0,0,1,17,0l80,80A12,12,0,0,1,216.49,168.49Z").toNodes(),
            fill = SolidColor(Color(0xFFF0F4F8))
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
            viewportWidth = 256f,
            viewportHeight = 256f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M128,20A108,108,0,1,0,236,128,108.12,108.12,0,0,0,128,20Zm0,192a84,84,0,1,1,84-84A84.09,84.09,0,0,1,128,212ZM163.27,77l-56,24a12,12,0,0,0-6.3,6.3l-24,56A12,12,0,0,0,92.73,179l56-24a12,12,0,0,0,6.3-6.3l24-56A12,12,0,0,0,163.27,77Zm-28.41,57.89-24,10.29,10.29-24,24-10.29Z").toNodes(),
            fill = SolidColor(Color(0xFFF0F4F8))
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
            viewportWidth = 256f,
            viewportHeight = 256f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M128,20A108,108,0,1,0,236,128,108.12,108.12,0,0,0,128,20Zm0,192a84,84,0,1,1,84-84A84.09,84.09,0,0,1,128,212ZM163.27,77l-56,24a12,12,0,0,0-6.3,6.3l-24,56A12,12,0,0,0,92.73,179l56-24a12,12,0,0,0,6.3-6.3l24-56A12,12,0,0,0,163.27,77Zm-28.41,57.89-24,10.29,10.29-24,24-10.29Z").toNodes(),
            fill = SolidColor(Color(0xFFF0F4F8))
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
            viewportWidth = 256f,
            viewportHeight = 256f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M128,180a52.06,52.06,0,0,0,52-52V64A52,52,0,0,0,76,64v64A52.06,52.06,0,0,0,128,180ZM100,64a28,28,0,0,1,56,0v64a28,28,0,0,1-56,0Zm40,155.22V240a12,12,0,0,1-24,0V219.22A92.14,92.14,0,0,1,36,128a12,12,0,0,1,24,0,68,68,0,0,0,136,0,12,12,0,0,1,24,0A92.14,92.14,0,0,1,140,219.22Z").toNodes(),
            fill = SolidColor(Color(0xFFF0F4F8))
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
            viewportWidth = 256f,
            viewportHeight = 256f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M232,116h-4.72A100.21,100.21,0,0,0,140,28.72V24a12,12,0,0,0-24,0v4.72A100.21,100.21,0,0,0,28.72,116H24a12,12,0,0,0,0,24h4.72A100.21,100.21,0,0,0,116,227.28V232a12,12,0,0,0,24,0v-4.72A100.21,100.21,0,0,0,227.28,140H232a12,12,0,0,0,0-24Zm-92,87v-3a12,12,0,0,0-24,0v3a76.15,76.15,0,0,1-63-63h3a12,12,0,0,0,0-24H53a76.15,76.15,0,0,1,63-63v3a12,12,0,0,0,24,0V53a76.15,76.15,0,0,1,63,63h-3a12,12,0,0,0,0,24h3A76.15,76.15,0,0,1,140,203ZM128,84a44,44,0,1,0,44,44A44.05,44.05,0,0,0,128,84Zm0,64a20,20,0,1,1,20-20A20,20,0,0,1,128,148Z").toNodes(),
            fill = SolidColor(Color(0xFFF0F4F8))
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
            viewportWidth = 256f,
            viewportHeight = 256f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M128,60a44,44,0,1,0,44,44A44.05,44.05,0,0,0,128,60Zm0,64a20,20,0,1,1,20-20A20,20,0,0,1,128,124Zm0-112a92.1,92.1,0,0,0-92,92c0,77.36,81.64,135.4,85.12,137.83a12,12,0,0,0,13.76,0,259,259,0,0,0,42.18-39C205.15,170.57,220,136.37,220,104A92.1,92.1,0,0,0,128,12Zm31.3,174.71A249.35,249.35,0,0,1,128,216.89a249.35,249.35,0,0,1-31.3-30.18C80,167.37,60,137.31,60,104a68,68,0,0,1,136,0C196,137.31,176,167.37,159.3,186.71Z").toNodes(),
            fill = SolidColor(Color(0xFFF0F4F8))
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
            viewportWidth = 256f,
            viewportHeight = 256f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M242.15,49.59A12,12,0,0,0,232,44H40a12,12,0,0,0-8.48,20.49L71,104,31.52,143.51A12,12,0,0,0,40,164H167.28l-26.11,54.84a12,12,0,1,0,21.66,10.32l80-168A12,12,0,0,0,242.15,49.59ZM178.71,140H69l27.52-27.52a12,12,0,0,0,0-17L69,68H213Z").toNodes(),
            fill = SolidColor(Color(0xFFF0F4F8))
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
            viewportWidth = 256f,
            viewportHeight = 256f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M225.6,62.64l-88-48.17a19.91,19.91,0,0,0-19.2,0l-88,48.17A20,20,0,0,0,20,80.19v95.62a20,20,0,0,0,10.4,17.55l88,48.17a19.89,19.89,0,0,0,19.2,0l88-48.17A20,20,0,0,0,236,175.81V80.19A20,20,0,0,0,225.6,62.64ZM128,36.57,200,76,128,115.4,56,76ZM44,96.79l72,39.4v76.67L44,173.44Zm96,116.07V136.19l72-39.4v76.65Z").toNodes(),
            fill = SolidColor(Color(0xFFF0F4F8))
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
            viewportWidth = 256f,
            viewportHeight = 256f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M128,20A108,108,0,1,0,236,128,108.12,108.12,0,0,0,128,20Zm84,108a83.64,83.64,0,0,1-4.47,27L167,130a19.65,19.65,0,0,0-7.8-2.78l-22.82-3.08A20.14,20.14,0,0,0,117.72,132h-4.07l-2.71-5.6a19.88,19.88,0,0,0-13.8-10.84L94.46,115l4-7h14.39a20,20,0,0,0,9.66-2.49l12.25-6.76a20.57,20.57,0,0,0,3.74-2.68l26.92-24.33A20,20,0,0,0,172,56.49,84,84,0,0,1,212,128ZM140.76,45l6.2,11.1L122.75,78l-10.93,6H96.14A20.05,20.05,0,0,0,78.78,94.06l-4.49,7.85L67.68,84.28l9.91-23.42A83.91,83.91,0,0,1,140.76,45ZM44,128a83.52,83.52,0,0,1,4.4-26.77l7.74,20.65a19.89,19.89,0,0,0,14.52,12.53l19.53,4.2,3,6.1a20.11,20.11,0,0,0,13.55,10.77l-5,11.12a20,20,0,0,0,3.58,21.71l.21.22,18.16,18.7-.89,4.59A84.09,84.09,0,0,1,44,128Zm103.65,81.66a20.11,20.11,0,0,0-5-17.3l-.21-.22-17.72-18.25,11.37-25.52,19,2.56,41.43,25.48A84.2,84.2,0,0,1,147.65,209.66Z").toNodes(),
            fill = SolidColor(Color(0xFFF0F4F8))
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
            viewportWidth = 256f,
            viewportHeight = 256f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M234.36,170A12,12,0,0,1,230,186.37l-96,56a12,12,0,0,1-12.1,0l-96-56a12,12,0,0,1,12.09-20.74l90,52.48L218,165.63A12,12,0,0,1,234.36,170ZM218,117.63,128,170.11,38.05,117.63A12,12,0,0,0,26,138.37l96,56a12,12,0,0,0,12.1,0l96-56A12,12,0,0,0,218,117.63ZM20,80a12,12,0,0,1,6-10.37l96-56a12.06,12.06,0,0,1,12.1,0l96,56a12,12,0,0,1,0,20.74l-96,56a12,12,0,0,1-12.1,0l-96-56A12,12,0,0,1,20,80Zm35.82,0L128,122.11,200.18,80,128,37.89Z").toNodes(),
            fill = SolidColor(Color(0xFFF0F4F8))
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
            viewportWidth = 256f,
            viewportHeight = 256f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M160,80a32,32,0,1,0-32-32A32,32,0,0,0,160,80Zm0-40a8,8,0,1,1-8,8A8,8,0,0,1,160,40Zm94.32,153.88L199.76,101.8A19.85,19.85,0,0,0,182.55,92h0a19.83,19.83,0,0,0-17.2,9.8l-18.7,31.55-37.42-63.5a20,20,0,0,0-34.46,0L1.66,193.91A12,12,0,0,0,12,212H244a12,12,0,0,0,10.32-18.12ZM92,87.87,108.57,116H75.43ZM33,188l28.28-48h61.44L151,188Zm145.86,0L160.56,157l22-37.1L222.94,188Z").toNodes(),
            fill = SolidColor(Color(0xFFF0F4F8))
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
            viewportWidth = 256f,
            viewportHeight = 256f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M231.38,46.54a12,12,0,0,0-10.29-2.18L161.4,59.28l-60-30a12,12,0,0,0-8.28-.91l-64,16A12,12,0,0,0,20,56V200a12,12,0,0,0,14.91,11.64L94.6,196.72l60,30a12,12,0,0,0,8.28.91l64-16A12,12,0,0,0,236,200V56A12,12,0,0,0,231.38,46.54ZM108,59.42l40,20V196.58l-40-20Zm-64,6,40-10V174.63l-40,10ZM212,190.63l-40,10V81.37l40-10Z").toNodes(),
            fill = SolidColor(Color(0xFFF0F4F8))
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
            viewportWidth = 256f,
            viewportHeight = 256f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M228,64a36,36,0,1,0-48,33.94V112a4,4,0,0,1-4,4H80a4,4,0,0,1-4-4V97.94a36,36,0,1,0-24,0V112a28,28,0,0,0,28,28h36v18.06a36,36,0,1,0,24,0V140h36a28,28,0,0,0,28-28V97.94A36.07,36.07,0,0,0,228,64ZM64,52A12,12,0,1,1,52,64,12,12,0,0,1,64,52Zm64,152a12,12,0,1,1,12-12A12,12,0,0,1,128,204ZM192,76a12,12,0,1,1,12-12A12,12,0,0,1,192,76Z").toNodes(),
            fill = SolidColor(Color(0xFFF0F4F8))
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
            viewportWidth = 256f,
            viewportHeight = 256f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M236,200a12,12,0,0,1-24,0,84.09,84.09,0,0,0-84-84H61l27.52,27.51a12,12,0,0,1-17,17l-48-48a12,12,0,0,1,0-17l48-48a12,12,0,0,1,17,17L61,92h67A108.12,108.12,0,0,1,236,200Z").toNodes(),
            fill = SolidColor(Color(0xFFF0F4F8))
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
            viewportWidth = 256f,
            viewportHeight = 256f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M208,108a36.05,36.05,0,0,0-33.38,22.54l-39-5.57a4,4,0,0,1-2.47-1.35L100.55,85.53A36,36,0,1,0,68,89.94v76.12a36,36,0,1,0,24,0V112.44l23,26.8a28,28,0,0,0,17.3,9.49l41.34,5.91A36,36,0,1,0,208,108ZM80,44A12,12,0,1,1,68,56,12,12,0,0,1,80,44Zm0,168a12,12,0,1,1,12-12A12,12,0,0,1,80,212Zm128-56a12,12,0,1,1,12-12A12,12,0,0,1,208,156Z").toNodes(),
            fill = SolidColor(Color(0xFFF0F4F8))
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
            viewportWidth = 256f,
            viewportHeight = 256f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M232.49,112.49l-48,48a12,12,0,0,1-17-17L195,116H128a84.09,84.09,0,0,0-84,84,12,12,0,0,1-24,0A108.12,108.12,0,0,1,128,92h67L167.51,64.48a12,12,0,0,1,17-17l48,48A12,12,0,0,1,232.49,112.49Z").toNodes(),
            fill = SolidColor(Color(0xFFF0F4F8))
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
            viewportWidth = 256f,
            viewportHeight = 256f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M200.49,200.49a12,12,0,0,1-17,0L76,93v75a12,12,0,0,1-24,0V64A12,12,0,0,1,64,52H168a12,12,0,0,1,0,24H93L200.49,183.51A12,12,0,0,1,200.49,200.49Z").toNodes(),
            fill = SolidColor(Color(0xFFF0F4F8))
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
            viewportWidth = 256f,
            viewportHeight = 256f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M204,64V168a12,12,0,0,1-24,0V93L72.49,200.49a12,12,0,0,1-17-17L163,76H88a12,12,0,0,1,0-24H192A12,12,0,0,1,204,64Z").toNodes(),
            fill = SolidColor(Color(0xFFF0F4F8))
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
            viewportWidth = 256f,
            viewportHeight = 256f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M208.49,120.49a12,12,0,0,1-17,0L140,69V216a12,12,0,0,1-24,0V69L64.49,120.49a12,12,0,0,1-17-17l72-72a12,12,0,0,1,17,0l72,72A12,12,0,0,1,208.49,120.49Z").toNodes(),
            fill = SolidColor(Color(0xFFF0F4F8))
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
            viewportWidth = 256f,
            viewportHeight = 256f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M236,112a68.07,68.07,0,0,1-68,68H61l27.52,27.51a12,12,0,0,1-17,17l-48-48a12,12,0,0,1,0-17l48-48a12,12,0,1,1,17,17L61,156H168a44,44,0,0,0,0-88H80a12,12,0,0,1,0-24h88A68.07,68.07,0,0,1,236,112Z").toNodes(),
            fill = SolidColor(Color(0xFFF0F4F8))
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
            viewportWidth = 256f,
            viewportHeight = 256f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M228,128a12,12,0,0,1-12,12H40a12,12,0,0,1,0-24H216A12,12,0,0,1,228,128ZM40,76H216a12,12,0,0,0,0-24H40a12,12,0,0,0,0,24ZM216,180H40a12,12,0,0,0,0,24H216a12,12,0,0,0,0-24Z").toNodes(),
            fill = SolidColor(Color(0xFFF0F4F8))
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
            viewportWidth = 256f,
            viewportHeight = 256f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M236,108H213L170.83,65.86A19.86,19.86,0,0,0,156.69,60H48.28a20,20,0,0,0-16.64,8.91L2,113.34A12,12,0,0,0,0,120v48a20,20,0,0,0,20,20H33.5a34,34,0,0,0,65,0h59a34,34,0,0,0,65,0H236a20,20,0,0,0,20-20V128A20,20,0,0,0,236,108ZM50.42,84H155l24,24H34.42ZM66,188a10,10,0,1,1,10-10A10,10,0,0,1,66,188Zm124,0a10,10,0,1,1,10-10A10,10,0,0,1,190,188Zm42-24H221A34,34,0,0,0,159,164H97A34,34,0,0,0,35,164H24V132H232Z").toNodes(),
            fill = SolidColor(Color(0xFFF0F4F8))
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
            viewportWidth = 256f,
            viewportHeight = 256f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M212,124c-1.22,0-2.43.06-3.62.16L175.24,35.79A12,12,0,0,0,164,28H132a12,12,0,0,0,0,24h23.68l30.23,80.6A44.28,44.28,0,0,0,171,152h-29.2L119.24,91.79A12,12,0,0,0,108,84H28a12,12,0,0,0,0,24h4v2.92A60.14,60.14,0,0,0,0,164a12,12,0,0,0,12,12h4.74a44,44,0,0,0,86.52,0h65.48A44,44,0,1,0,212,124ZM48,130.05a12,12,0,0,0,8-11.32V108H99.68l16.5,44H26.06A36,36,0,0,1,48,130.05ZM60,188a20,20,0,0,1-18.32-12H78.32A20,20,0,0,1,60,188Zm152,0a20,20,0,1,1,20-20A20,20,0,0,1,212,188Z").toNodes(),
            fill = SolidColor(Color(0xFFF0F4F8))
        )
        builder.build()
    }

    // ic_mode_swap / arrows_down_up
    val ModeSwap: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "ModeSwap",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 256f,
            viewportHeight = 256f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M88.49,43.51a12,12,0,0,0-17,0l-48,48a12,12,0,0,0,17,17L64,85V208a12,12,0,0,0,24,0V85l23.51,23.52a12,12,0,0,0,17-17Zm144,80a12,12,0,0,0-17,0L192,147V48a12,12,0,0,0-24,0V147l-23.51-23.51a12,12,0,0,0-17,17l48,48a12,12,0,0,0,17,0l48-48A12,12,0,0,0,232.49,123.51Z").toNodes(),
            fill = SolidColor(Color(0xFFF0F4F8))
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
            viewportWidth = 256f,
            viewportHeight = 256f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M152,84a36,36,0,1,0-36-36A36,36,0,0,0,152,84Zm0-48a12,12,0,1,1-12,12A12,12,0,0,1,152,36Zm68,112a12,12,0,0,1-12,12c-37,0-55.27-18.47-70-33.3-1.71-1.72-3.36-3.4-5-5l-8.63,19.85L159,166.23a12,12,0,0,1,5,9.77v56a12,12,0,0,1-24,0V182.17l-25.37-18.12L83,236.78a12,12,0,1,1-22-9.57l50.06-115.13q-10.64.75-25,8.4a159.78,159.78,0,0,0-29.83,21.23,12,12,0,0,1-16.43-17.5c2.61-2.45,64.36-59.67,104.09-25.18,3.94,3.42,7.64,7.16,11.22,10.78C168.43,123.28,181,136,208,136A12,12,0,0,1,220,148Z").toNodes(),
            fill = SolidColor(Color(0xFFF0F4F8))
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
            viewportWidth = 256f,
            viewportHeight = 256f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M238.7,102.46,62.81,37.21l-.25-.09A20,20,0,0,0,37.12,62.56l.09.25L102.46,238.7A20,20,0,0,0,121.3,252h.35a20,20,0,0,0,18.77-14.12l.09-.29,21.23-75.85,75.85-21.23.29-.09a20,20,0,0,0,.82-38Zm-89.93,38a12,12,0,0,0-8.32,8.32l-19.68,70.29L62.8,62.8l156.26,58Z").toNodes(),
            fill = SolidColor(Color(0xFFF0F4F8))
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
            viewportWidth = 256f,
            viewportHeight = 256f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M232,116h-4.72A100.21,100.21,0,0,0,140,28.72V24a12,12,0,0,0-24,0v4.72A100.21,100.21,0,0,0,28.72,116H24a12,12,0,0,0,0,24h4.72A100.21,100.21,0,0,0,116,227.28V232a12,12,0,0,0,24,0v-4.72A100.21,100.21,0,0,0,227.28,140H232a12,12,0,0,0,0-24Zm-92,87v-3a12,12,0,0,0-24,0v3a76.15,76.15,0,0,1-63-63h3a12,12,0,0,0,0-24H53a76.15,76.15,0,0,1,63-63v3a12,12,0,0,0,24,0V53a76.15,76.15,0,0,1,63,63h-3a12,12,0,0,0,0,24h3A76.15,76.15,0,0,1,140,203ZM128,84a44,44,0,1,0,44,44A44.05,44.05,0,0,0,128,84Zm0,64a20,20,0,1,1,20-20A20,20,0,0,1,128,148Z").toNodes(),
            fill = SolidColor(Color(0xFFF0F4F8))
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
            viewportWidth = 256f,
            viewportHeight = 256f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M228,48V96a12,12,0,0,1-12,12H168a12,12,0,0,1,0-24h19l-7.8-7.8a75.55,75.55,0,0,0-53.32-22.26h-.43A75.49,75.49,0,0,0,72.39,75.57,12,12,0,1,1,55.61,58.41a99.38,99.38,0,0,1,69.87-28.47H126A99.42,99.42,0,0,1,196.2,59.23L204,67V48a12,12,0,0,1,24,0ZM183.61,180.43a75.49,75.49,0,0,1-53.09,21.63h-.43A75.55,75.55,0,0,1,76.77,179.8L69,172H88a12,12,0,0,0,0-24H40a12,12,0,0,0-12,12v48a12,12,0,0,0,24,0V189l7.8,7.8A99.42,99.42,0,0,0,130,226.06h.56a99.38,99.38,0,0,0,69.87-28.47,12,12,0,0,0-16.78-17.16Z").toNodes(),
            fill = SolidColor(Color(0xFFF0F4F8))
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
            viewportWidth = 256f,
            viewportHeight = 256f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M128,20A108,108,0,1,0,236,128,108.12,108.12,0,0,0,128,20Zm84,108a83.6,83.6,0,0,1-16.75,50.28L77.72,60.75A84,84,0,0,1,212,128ZM44,128A83.6,83.6,0,0,1,60.75,77.72L178.28,195.25A84,84,0,0,1,44,128Z").toNodes(),
            fill = SolidColor(Color(0xFFF0F4F8))
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
            viewportWidth = 256f,
            viewportHeight = 256f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M228,128a100,100,0,0,1-98.66,100H128a99.39,99.39,0,0,1-68.62-27.29,12,12,0,0,1,16.48-17.45,76,76,0,1,0-1.57-109c-.13.13-.25.25-.39.37L54.89,92H72a12,12,0,0,1,0,24H24a12,12,0,0,1-12-12V56a12,12,0,0,1,24,0V76.72L57.48,57.06A100,100,0,0,1,228,128Z").toNodes(),
            fill = SolidColor(Color(0xFFF0F4F8))
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
            viewportWidth = 256f,
            viewportHeight = 256f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M248.92,104,215.3,66.62A20,20,0,0,0,200.44,60H140V32a12,12,0,0,0-24,0V60H40A20,20,0,0,0,20,80v64a20,20,0,0,0,20,20h76v60a12,12,0,0,0,24,0V164h60.44a20,20,0,0,0,14.86-6.62L248.92,120A12,12,0,0,0,248.92,104Zm-50.26,36H44V84H198.66l25.2,28Z").toNodes(),
            fill = SolidColor(Color(0xFFF0F4F8))
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
            viewportWidth = 256f,
            viewportHeight = 256f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M232.49,215.51,185,168a92.12,92.12,0,1,0-17,17l47.53,47.54a12,12,0,0,0,17-17ZM44,112a68,68,0,1,1,68,68A68.07,68.07,0,0,1,44,112Z").toNodes(),
            fill = SolidColor(Color(0xFFF0F4F8))
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
            viewportWidth = 256f,
            viewportHeight = 256f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M230.14,25.86a20,20,0,0,0-19.57-5.11l-.22.07L18.44,79a20,20,0,0,0-3.06,37.25L99,157l40.71,83.65a19.81,19.81,0,0,0,18,11.38c.57,0,1.15,0,1.73-.07A19.82,19.82,0,0,0,177,237.56L235.18,45.65a1.42,1.42,0,0,0,.07-.22A20,20,0,0,0,230.14,25.86ZM156.91,221.07l-34.37-70.64,46-45.95a12,12,0,0,0-17-17l-46,46L34.93,99.09,210,46Z").toNodes(),
            fill = SolidColor(Color(0xFFF0F4F8))
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
            viewportWidth = 256f,
            viewportHeight = 256f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M140,124v92a12,12,0,0,1-24,0V124a12,12,0,0,1,24,0Zm60,68a12,12,0,0,0-12,12v12a12,12,0,0,0,24,0V204A12,12,0,0,0,200,192Zm24-40H212V40a12,12,0,0,0-24,0V152H176a12,12,0,0,0,0,24h48a12,12,0,0,0,0-24ZM56,160a12,12,0,0,0-12,12v44a12,12,0,0,0,24,0V172A12,12,0,0,0,56,160Zm24-40H68V40a12,12,0,0,0-24,0v80H32a12,12,0,0,0,0,24H80a12,12,0,0,0,0-24Zm72-48H140V40a12,12,0,0,0-24,0V72H104a12,12,0,0,0,0,24h48a12,12,0,0,0,0-24Z").toNodes(),
            fill = SolidColor(Color(0xFFF0F4F8))
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
            viewportWidth = 256f,
            viewportHeight = 256f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M119.51,143.51l88-88a12,12,0,1,1,17,17l-88,88a12,12,0,1,1-17-17Zm14.23-43.2a12,12,0,1,0,2.62-23.85A75.15,75.15,0,0,0,128,76a76.08,76.08,0,0,0-76,76,12,12,0,0,0,24,0,52.06,52.06,0,0,1,52-52A54.75,54.75,0,0,1,133.74,100.31Zm101.54,7.5A12,12,0,0,0,213.09,117a92.47,92.47,0,0,1,2.58,63H40.34A92.23,92.23,0,0,1,128,60h.84a91.43,91.43,0,0,1,34.2,6.91,12,12,0,0,0,9.14-22.19A116.07,116.07,0,0,0,18.57,190.58,20.09,20.09,0,0,0,37.46,204H218.53a20.06,20.06,0,0,0,18.88-13.38,116.39,116.39,0,0,0-2.13-82.81Z").toNodes(),
            fill = SolidColor(Color(0xFFF0F4F8))
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
            viewportWidth = 256f,
            viewportHeight = 256f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M128,44a96,96,0,1,0,96,96A96.11,96.11,0,0,0,128,44Zm0,168a72,72,0,1,1,72-72A72.08,72.08,0,0,1,128,212ZM164.49,99.51a12,12,0,0,1,0,17l-28,28a12,12,0,0,1-17-17l28-28A12,12,0,0,1,164.49,99.51ZM92,16A12,12,0,0,1,104,4h48a12,12,0,0,1,0,24H104A12,12,0,0,1,92,16Z").toNodes(),
            fill = SolidColor(Color(0xFFF0F4F8))
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
            viewportWidth = 256f,
            viewportHeight = 256f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M228,128a12,12,0,0,1-12,12H69l51.52,51.51a12,12,0,0,1-17,17l-72-72a12,12,0,0,1,0-17l72-72a12,12,0,0,1,17,17L69,116H216A12,12,0,0,1,228,128Z").toNodes(),
            fill = SolidColor(Color(0xFFF0F4F8))
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
            viewportWidth = 256f,
            viewportHeight = 256f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M224.49,136.49l-72,72a12,12,0,0,1-17-17L187,140H40a12,12,0,0,1,0-24H187L135.51,64.48a12,12,0,0,1,17-17l72,72A12,12,0,0,1,224.49,136.49Z").toNodes(),
            fill = SolidColor(Color(0xFFF0F4F8))
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
            viewportWidth = 256f,
            viewportHeight = 256f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M240.26,186.1,152.81,34.23h0a28.74,28.74,0,0,0-49.62,0L15.74,186.1a27.45,27.45,0,0,0,0,27.71A28.31,28.31,0,0,0,40.55,228h174.9a28.31,28.31,0,0,0,24.79-14.19A27.45,27.45,0,0,0,240.26,186.1Zm-20.8,15.7a4.46,4.46,0,0,1-4,2.2H40.55a4.46,4.46,0,0,1-4-2.2,3.56,3.56,0,0,1,0-3.73L124,46.2a4.77,4.77,0,0,1,8,0l87.44,151.87A3.56,3.56,0,0,1,219.46,201.8ZM116,136V104a12,12,0,0,1,24,0v32a12,12,0,0,1-24,0Zm28,40a16,16,0,1,1-16-16A16,16,0,0,1,144,176Z").toNodes(),
            fill = SolidColor(Color(0xFFF0F4F8))
        )
        builder.build()
    }

}
