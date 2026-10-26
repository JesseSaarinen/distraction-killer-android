package io.github.jessesaarinen.distractionkiller.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.jessesaarinen.distractionkiller.R

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
