package io.github.jessesaarinen.distractionkiller.interrupt.tasks

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.jessesaarinen.distractionkiller.Config
import io.github.jessesaarinen.distractionkiller.R
import kotlinx.coroutines.delay

/**
 * 15-second breathing exercise, like tasks/breathing.js in the web demo:
 * the sun grows (breathe in) and shrinks (breathe out) every 4 seconds, and
 * the ring around it fills up until the time is over.
 */
@Composable
fun BreathingTask(onDone: () -> Unit) {
    val currentOnDone by rememberUpdatedState(onDone)
    var elapsed by remember { mutableIntStateOf(0) }
    var started by remember { mutableStateOf(false) }

    val total = Config.BREATHING_SECONDS
    val phase = Config.BREATHING_PHASE_SECONDS
    val finished = elapsed >= total
    val inhale = !finished && (elapsed / phase) % 2 == 0
    val scale by animateFloatAsState(
        targetValue = if (started && inhale) 1f else 0.62f,
        animationSpec = tween(durationMillis = phase * 1000, easing = FastOutSlowInEasing),
        label = "breathing",
    )
    val progress by animateFloatAsState(
        targetValue = elapsed / total.toFloat(),
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        label = "progress",
    )

    LaunchedEffect(Unit) {
        started = true
        while (elapsed < total) {
            delay(1000)
            elapsed++
        }
        currentOnDone()
    }

    val sun = MaterialTheme.colorScheme.secondary
    val ring = MaterialTheme.colorScheme.primary
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.breathing_title),
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.breathing_intro),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Box(modifier = Modifier.size(248.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 4.dp.toPx()
                val ringRadius = size.minDimension / 2f - stroke
                val topLeft = Offset(center.x - ringRadius, center.y - ringRadius)
                drawCircle(color = ring.copy(alpha = 0.15f), radius = ringRadius, style = Stroke(stroke))
                drawArc(
                    color = ring,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(ringRadius * 2f, ringRadius * 2f),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
                val sunRadius = ringRadius * 0.7f * scale
                // A soft glow that starts at the sun's edge and fades out.
                val glowRadius = sunRadius * 1.4f
                drawCircle(
                    brush = Brush.radialGradient(
                        0.7f to sun.copy(alpha = 0.35f),
                        1f to sun.copy(alpha = 0f),
                        center = center,
                        radius = glowRadius,
                    ),
                    radius = glowRadius,
                )
                drawCircle(color = sun, radius = sunRadius)
            }
            if (finished) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.size(48.dp),
                )
            } else {
                Text(
                    text = "${total - elapsed}",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSecondary,
                )
            }
        }
        Text(
            text = stringResource(
                when {
                    finished -> R.string.breathing_done
                    inhale -> R.string.breathing_in
                    else -> R.string.breathing_out
                }
            ),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
