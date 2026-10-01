package com.autumn.nyaclash.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * A tiny built-in "server" icon, so we do not have to depend on the huge
 * `material-icons-extended` artifact (whose core set has no fitting glyph).
 */
val NyaNodesIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "NyaNodes",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(3f, 4f)
            lineTo(21f, 4f)
            lineTo(21f, 10f)
            lineTo(3f, 10f)
            close()
        }
        path(fill = SolidColor(Color.Black)) {
            moveTo(3f, 14f)
            lineTo(21f, 14f)
            lineTo(21f, 20f)
            lineTo(3f, 20f)
            close()
        }
    }.build()
}
