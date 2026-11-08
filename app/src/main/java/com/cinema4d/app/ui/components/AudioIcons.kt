package com.cinema4d.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Speaker with two sound waves — used by the trailer audio toggle. */
val SpeakerOnIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "SpeakerOn",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(3f, 9f)
            lineTo(7f, 9f)
            lineTo(12f, 4f)
            lineTo(12f, 20f)
            lineTo(7f, 15f)
            lineTo(3f, 15f)
            close()
        }
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.9f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(15f, 9f)
            lineTo(16.8f, 12f)
            lineTo(15f, 15f)
            moveTo(18f, 6.6f)
            lineTo(20.2f, 12f)
            lineTo(18f, 17.4f)
        }
    }.build()
}

/** Speaker with a slash — muted state. */
val SpeakerOffIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "SpeakerOff",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(3f, 9f)
            lineTo(7f, 9f)
            lineTo(12f, 4f)
            lineTo(12f, 20f)
            lineTo(7f, 15f)
            lineTo(3f, 15f)
            close()
        }
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.9f,
            strokeLineCap = StrokeCap.Round,
        ) {
            moveTo(15.2f, 9.4f)
            lineTo(20.2f, 14.6f)
            moveTo(20.2f, 9.4f)
            lineTo(15.2f, 14.6f)
        }
    }.build()
}
