package io.github.jessesaarinen.distractionkiller.interrupt.tasks

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.jessesaarinen.distractionkiller.Config
import io.github.jessesaarinen.distractionkiller.R
import kotlinx.coroutines.delay

/**
 * 15-second breathing exercise, like tasks/breathing.js in the web demo:
 * the circle grows (breathe in) and shrinks (breathe out) every 4 seconds.
 */
@Composable
fun BreathingTask(onDone: () -> Unit) {
    val currentOnDone by rememberUpdatedState(onDone)
    // Saved, so turning the phone does not start the exercise over.
    var elapsed by rememberSaveable { mutableIntStateOf(0) }
    var started by remember { mutableStateOf(false) }

    val total = Config.BREATHING_SECONDS
    val phase = Config.BREATHING_PHASE_SECONDS
    val finished = elapsed >= total
    val inhale = !finished && (elapsed / phase) % 2 == 0
    val scale by animateFloatAsState(
        targetValue = if (started && inhale) 1f else 0.55f,
        animationSpec = tween(durationMillis = phase * 1000, easing = FastOutSlowInEasing),
        label = "breathing",
    )

    LaunchedEffect(Unit) {
        started = true
        while (elapsed < total) {
            delay(1000)
            elapsed++
        }
        currentOnDone()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.breathing_intro),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .scale(scale)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), CircleShape),
            )
            Text(
                text = if (finished) stringResource(R.string.task_done_mark) else "${total - elapsed}",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = stringResource(
                when {
                    finished -> R.string.breathing_done
                    inhale -> R.string.breathing_in
                    else -> R.string.breathing_out
                }
            ),
            style = MaterialTheme.typography.titleLarge,
        )
    }
}
