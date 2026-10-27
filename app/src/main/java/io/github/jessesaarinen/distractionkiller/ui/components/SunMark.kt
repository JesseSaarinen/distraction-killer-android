package io.github.jessesaarinen.distractionkiller.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect

/**
 * The app's mark: a sun rising over the horizon, "look up from the phone".
 * rise goes from 0 (the sun just peeks over the horizon) to 1 (fully up).
 * Draw it in a square, for example Modifier.size(48.dp). Decorative only.
 */
@Composable
fun SunMark(
    modifier: Modifier = Modifier,
    rise: Float = 0.5f,
    sun: Color = MaterialTheme.colorScheme.secondary,
    horizon: Color = MaterialTheme.colorScheme.primary,
    glow: Boolean = true,
) {
    Canvas(modifier = modifier) {
        val unit = size.minDimension
        val horizonY = size.height * 0.68f
        val radius = unit * 0.22f
        val sunY = horizonY + radius * 0.6f - rise.coerceIn(0f, 1f) * radius * 1.85f
        val center = Offset(size.width / 2f, sunY)

        // Only the part above the horizon is visible.
        clipRect(bottom = horizonY) {
            if (glow) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(sun.copy(alpha = 0.45f), sun.copy(alpha = 0f)),
                        center = center,
                        radius = radius * 2f,
                    ),
                    radius = radius * 2f,
                    center = center,
                )
            }
            drawCircle(color = sun, radius = radius, center = center)
        }
        drawLine(
            color = horizon,
            start = Offset(size.width * 0.14f, horizonY),
            end = Offset(size.width * 0.86f, horizonY),
            strokeWidth = unit * 0.06f,
            cap = StrokeCap.Round,
        )
    }
}
