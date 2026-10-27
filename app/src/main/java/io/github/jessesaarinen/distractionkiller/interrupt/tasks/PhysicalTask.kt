package io.github.jessesaarinen.distractionkiller.interrupt.tasks

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.jessesaarinen.distractionkiller.Config
import io.github.jessesaarinen.distractionkiller.R
import io.github.jessesaarinen.distractionkiller.ui.components.IconBubble
import io.github.jessesaarinen.distractionkiller.ui.components.PrimaryButton
import kotlinx.coroutines.delay

// @param: puts the annotation on the constructor's parameters, as Kotlin asks.
private data class Challenge(@param:DrawableRes val icon: Int, @param:StringRes val text: Int)

private val CHALLENGES = listOf(
    Challenge(R.drawable.ic_window, R.string.physical_window),
    Challenge(R.drawable.ic_move, R.string.physical_stretch),
    Challenge(R.drawable.ic_water, R.string.physical_water),
    Challenge(R.drawable.ic_eye, R.string.physical_eyes),
)

/**
 * A small physical challenge, like tasks/physical.js in the web demo:
 * 10 seconds, then the user marks it done.
 */
@Composable
fun PhysicalTask(onDone: () -> Unit) {
    val challenge = remember { CHALLENGES.random() }
    var left by remember { mutableIntStateOf(Config.PHYSICAL_SECONDS) }
    var done by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (left > 0) {
            delay(1000)
            left--
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        IconBubble(
            icon = challenge.icon,
            size = 96.dp,
            container = MaterialTheme.colorScheme.secondaryContainer,
            content = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Text(
            text = stringResource(challenge.text),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        CountdownRing(left = left, total = Config.PHYSICAL_SECONDS)
        if (!done) {
            PrimaryButton(
                text = stringResource(R.string.physical_done),
                onClick = {
                    done = true
                    onDone()
                },
                enabled = left == 0,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Seconds left in a ring that fills up. A check mark when the time is over. */
@Composable
private fun CountdownRing(left: Int, total: Int) {
    val progress by animateFloatAsState(
        targetValue = (total - left) / total.toFloat(),
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        label = "countdown",
    )
    val ring = MaterialTheme.colorScheme.primary
    Box(modifier = Modifier.size(104.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 4.dp.toPx()
            val radius = size.minDimension / 2f - stroke
            drawCircle(color = ring.copy(alpha = 0.15f), radius = radius, style = Stroke(stroke))
            drawArc(
                color = ring,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        if (left > 0) {
            Text(
                text = "$left",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp),
            )
        }
    }
}
