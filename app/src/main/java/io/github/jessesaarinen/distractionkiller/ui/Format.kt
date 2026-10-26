package io.github.jessesaarinen.distractionkiller.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.jessesaarinen.distractionkiller.R
import io.github.jessesaarinen.distractionkiller.TaskType

/** "45 s", "1 min" or "1 min 30 s", like formatDuration() in the web demo. */
@Composable
fun durationText(seconds: Int): String {
    val minutes = seconds / 60
    val rest = seconds % 60
    return when {
        minutes == 0 -> stringResource(R.string.duration_seconds, seconds)
        rest == 0 -> stringResource(R.string.duration_minutes, minutes)
        else -> stringResource(R.string.duration_minutes_seconds, minutes, rest)
    }
}

/** The task's name on the settings and stats screens. */
@StringRes
fun taskLabel(task: TaskType): Int = when (task) {
    TaskType.BREATHING -> R.string.task_breathing
    TaskType.QUESTION -> R.string.task_question
    TaskType.PHYSICAL -> R.string.task_physical
}
