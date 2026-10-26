package io.github.jessesaarinen.distractionkiller.interrupt.tasks

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jessesaarinen.distractionkiller.Config
import io.github.jessesaarinen.distractionkiller.R
import kotlinx.coroutines.delay

private data class Challenge(@StringRes val icon: Int, @StringRes val text: Int)

private val CHALLENGES = listOf(
    Challenge(R.string.physical_window_icon, R.string.physical_window),
    Challenge(R.string.physical_stretch_icon, R.string.physical_stretch),
    Challenge(R.string.physical_water_icon, R.string.physical_water),
    Challenge(R.string.physical_eyes_icon, R.string.physical_eyes),
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
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = stringResource(challenge.icon), fontSize = 72.sp)
        Text(
            text = stringResource(challenge.text),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = if (left > 0) "$left" else stringResource(R.string.task_done_mark),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        if (!done) {
            Button(
                onClick = {
                    done = true
                    onDone()
                },
                enabled = left == 0,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.physical_done))
            }
        }
    }
}
