package com.conzola.kate

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** The few icons Material's core set doesn't have. */
object KateIcons {
    private fun icon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply(block).build()

    private fun PathBuilder.square(x: Float, y: Float) {
        moveTo(x, y); lineTo(x + 7f, y); lineTo(x + 7f, y + 7f); lineTo(x, y + 7f); close()
    }

    val Bluetooth: ImageVector by lazy {
        icon("bluetooth") {
            path(stroke = SolidColor(Color.White), strokeLineWidth = 1.9f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(6.5f, 7.5f); lineTo(17f, 16.5f); lineTo(12f, 21f); lineTo(12f, 3f); lineTo(17f, 7.5f); lineTo(6.5f, 16.5f)
            }
        }
    }
    val Bolt: ImageVector by lazy {
        icon("bolt") {
            path(fill = SolidColor(Color.White)) {
                moveTo(13.5f, 2f); lineTo(5f, 13.5f); lineTo(11f, 13.5f); lineTo(10f, 22f); lineTo(19f, 10f); lineTo(13f, 10f); close()
            }
        }
    }
    val Grid: ImageVector by lazy {
        icon("grid") { path(fill = SolidColor(Color.White)) { square(4f, 4f); square(13f, 4f); square(4f, 13f); square(13f, 13f) } }
    }
}
