package io.github.jessesaarinen.distractionkiller.ui

import androidx.annotation.DrawableRes
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

/** A decision time on the stats screen: "1,5 s" under 10 seconds, otherwise like durationText. */
@Composable
fun decisionText(millis: Long): String {
    val tenths = ((millis + 50) / 100).toInt()
    return if (tenths < 100) {
        stringResource(R.string.duration_seconds_decimal, tenths / 10, tenths % 10)
    } else {
        durationText(((millis + 500) / 1000).toInt())
    }
}

/** The task's name on the settings and stats screens. */
@StringRes
fun taskLabel(task: TaskType): Int = when (task) {
    TaskType.BREATHING -> R.string.task_breathing
    TaskType.QUESTION -> R.string.task_question
    TaskType.PHYSICAL -> R.string.task_physical
}

/** The task's icon on the settings and stats screens. */
@DrawableRes
fun taskIcon(task: TaskType): Int = when (task) {
    TaskType.BREATHING -> R.drawable.ic_breath
    TaskType.QUESTION -> R.drawable.ic_question
    TaskType.PHYSICAL -> R.drawable.ic_move
}
