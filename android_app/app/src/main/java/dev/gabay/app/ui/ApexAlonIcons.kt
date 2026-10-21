package dev.gabay.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Apex Alon Modular Icon Font Pack for Jetpack Compose.
 * High-contrast, zero-slop automotive vector geometry.
 */
object ApexAlonIcons {
    val Scooter: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "ApexAlon.Scooter",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M6 18a3 3 0 1 0 0-6 3 3 0 0 0 0 6z M18 18a3 3 0 1 0 0-6 3 3 0 0 0 0 6z M6 15h4l3-6h4 M12 12l2 6h3 M7 12h5 M17 6l2-3 M15 6h4 M17 9l1-3").toNodes(),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    val CompassStar: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "ApexAlon.CompassStar",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M12 2l3 7 7 3-7 3-3 7-3-7-7-3 7-3 3-7z M12 10a2 2 0 1 0 0 4 2 2 0 0 0 0-4z").toNodes(),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    val WaypointPin: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "ApexAlon.WaypointPin",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M12 21c-4.5-5-7-8.5-7-12a7 7 0 1 1 14 0c0 3.5-2.5 7-7 12z M12 6l3 5-3-1-3 1z").toNodes(),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    val FuelPump: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "ApexAlon.FuelPump",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M4 4h10v16H4z M6 7h6v4H6z M4 17h10 M14 9h2a2 2 0 0 1 2 2v5a1.5 1.5 0 0 0 3 0V9l-2-2").toNodes(),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    val RoadCenterline: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "ApexAlon.RoadCenterline",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M4 21L8 3 M20 21L16 3 M12 5v3 M12 11v3 M12 17v3").toNodes(),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    val PerspectiveCube: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "ApexAlon.PerspectiveCube",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M12 2l9 5v10l-9 5-9-5V7z M12 12l9-5 M12 12v10 M12 12L3 7").toNodes(),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    val WrenchTool: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "ApexAlon.WrenchTool",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z").toNodes(),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    val SpeakerWaves: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "ApexAlon.SpeakerWaves",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M11 5L6 9H2v6h4l5 4V5z M15.54 8.46a5 5 0 0 1 0 7.07 M19.07 4.93a10 10 0 0 1 0 14.14").toNodes(),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    val SpeedometerDial: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "ApexAlon.SpeedometerDial",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M12 4a9 9 0 0 0-9 9c0 2.5 1 4.7 2.6 6.4L7 18 M21 13a9 9 0 0 0-9-9 M17 18l1.4 1.4A9 9 0 0 0 21 13 M12 13l4-4").toNodes(),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    val ShieldCrest: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "ApexAlon.ShieldCrest",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z M12 6v12").toNodes(),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    val BatteryBolt: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "ApexAlon.BatteryBolt",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M2 6h18v12H2z M22 10v4 M12 8l-4 5h3l-1 3 4-5h-3z").toNodes(),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    val GearSettings: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "ApexAlon.GearSettings",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z").toNodes(),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    val HelmetSafety: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "ApexAlon.HelmetSafety",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M12 3a9 9 0 0 0-9 9c0 3.5 2 6.5 5 8h8a6 6 0 0 0 6-6c0-6.08-4.92-11-10-11z M8 12h8a2 2 0 0 1 2 2v2a2 2 0 0 1-2 2H8").toNodes(),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    val HazardTriangle: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "ApexAlon.HazardTriangle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z M12 9v4 M12 17h.01").toNodes(),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    val GforceMeter: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "ApexAlon.GforceMeter",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z M12 3v18 M3 12h18 M14 10a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5z").toNodes(),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    val SatelliteDish: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "ApexAlon.SatelliteDish",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M4 10a7.5 7.5 0 0 0 10 10 M4 6a11.5 11.5 0 0 0 14 14 M4 2a15.5 15.5 0 0 0 18 18 M4 20a2 2 0 1 0 0-4 2 2 0 0 0 0 4z").toNodes(),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    val ClockHistory: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "ApexAlon.ClockHistory",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M12 4a8 8 0 1 0 8 8 8 8 0 0 0-8-8zm0 14a6 6 0 1 1 6-6 6 6 0 0 1-6 6zm.5-10H11v5l4.2 2.5.8-1.3-3.5-2.1z").toNodes(),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    val HospitalCross: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "ApexAlon.HospitalCross",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M3 5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2zm8 3v3H8v2h3v3h2v-3h3v-2h-3V8z").toNodes(),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    val GraduationCap: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "ApexAlon.GraduationCap",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M12 3L1 9l11 6 9-4.91V17h2V9z M5 13.18v4L12 21l7-3.82v-4L12 17z").toNodes(),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }

    val CivicBuilding: ImageVector by lazy {
        val builder = ImageVector.Builder(
            name = "ApexAlon.CivicBuilding",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        builder.addPath(
            pathData = PathParser().parsePathString("M12 2L2 7v2h20V7z M4 11v8h3v-8zm5 0v8h3v-8zm5 0v8h3v-8zm5 0v8h3v-8z M2 21h20v2H2z").toNodes(),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        builder.build()
    }
}