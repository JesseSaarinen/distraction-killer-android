package io.github.jessesaarinen.distractionkiller.interrupt

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.jessesaarinen.distractionkiller.Config
import io.github.jessesaarinen.distractionkiller.R
import io.github.jessesaarinen.distractionkiller.TaskType
import io.github.jessesaarinen.distractionkiller.interrupt.tasks.BreathingTask
import io.github.jessesaarinen.distractionkiller.interrupt.tasks.PhysicalTask
import io.github.jessesaarinen.distractionkiller.interrupt.tasks.QuestionTask
import io.github.jessesaarinen.distractionkiller.ui.durationText
import kotlinx.coroutines.delay

/**
 * Level 2: a calm full-screen break with one task, like showLevel2() in the
 * web demo. Ohita appears after 5 s. After the task the user chooses to stop
 * or to keep scrolling. outcome is "stopped", "continued" or "skipped".
 */
@Composable
fun Level2Screen(
    task: TaskType,
    scrollSeconds: Int,
    onFinish: (outcome: String, answer: String?) -> Unit,
) {
    // rememberSaveable keeps these when the phone turns and Android builds the screen again.
    var taskDone by rememberSaveable { mutableStateOf(false) }
    var answer by rememberSaveable { mutableStateOf<String?>(null) }
    var skipVisible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(Config.SKIP_DELAY_SECONDS * 1000L)
        skipVisible = true
    }
    // The back gesture works like Ohita, but only once Ohita is shown.
    BackHandler {
        if (skipVisible && !taskDone) onFinish("skipped", null)
    }

    // safeDrawing includes the keyboard, so the answer field in the question
    // task stays above it.
    Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.level2_scrolled, durationText(scrollSeconds)),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.level2_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )

            when (task) {
                TaskType.BREATHING -> BreathingTask(onDone = { taskDone = true })
                TaskType.QUESTION -> QuestionTask(onDone = {
                    answer = it
                    taskDone = true
                })
                TaskType.PHYSICAL -> PhysicalTask(onDone = { taskDone = true })
            }

            if (taskDone) {
                Text(
                    text = stringResource(R.string.level2_prompt),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Button(
                    onClick = { onFinish("stopped", answer) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) {
                    Text(stringResource(R.string.level2_stop))
                }
                OutlinedButton(
                    onClick = { onFinish("continued", answer) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.level2_continue))
                }
            } else if (skipVisible) {
                TextButton(
                    onClick = { onFinish("skipped", null) },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.level2_skip))
                }
            } else {
                Text(
                    text = stringResource(R.string.level2_skip_wait),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
