package io.github.jessesaarinen.distractionkiller.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.jessesaarinen.distractionkiller.Config
import io.github.jessesaarinen.distractionkiller.R
import io.github.jessesaarinen.distractionkiller.data.Settings
import io.github.jessesaarinen.distractionkiller.ui.components.Hint
import io.github.jessesaarinen.distractionkiller.ui.components.WheelPicker

/**
 * Sets how often the reminder repeats once the daily time is used up (task
 * N22): minutes, and seconds in steps of Config.REPEAT_STEP_SECONDS, like the
 * daily time carousels. 0 min 0 s becomes Config.REPEAT_MIN_SECONDS.
 */
@Composable
fun RepeatDialog(seconds: Int, onSave: (Int) -> Unit, onDismiss: () -> Unit) {
    var chosen by remember { mutableIntStateOf(seconds) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.repeat_title), style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                RepeatWheels(seconds = chosen, onChange = { chosen = it })
                Hint(stringResource(R.string.repeat_hint, Config.REPEAT_MIN_SECONDS))
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(Settings.clampRepeat(chosen)) }) {
                Text(stringResource(R.string.allowance_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.app_picker_cancel))
            }
        },
    )
}

/** The minute and second carousels for the repeat time. onChange gets the seconds, not yet clamped. */
@Composable
fun RepeatWheels(seconds: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val minutes = seconds / 60
    // Rounded to the carousel's steps.
    val rest = seconds % 60 / Config.REPEAT_STEP_SECONDS * Config.REPEAT_STEP_SECONDS
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        WheelPicker(
            values = (0..Config.REPEAT_MAX_MINUTES).toList(),
            current = minutes,
            onSelect = { onChange(it * 60 + rest) },
            label = { stringResource(R.string.duration_minutes, it) },
            modifier = Modifier.weight(1f),
        )
        WheelPicker(
            values = (0 until 60 step Config.REPEAT_STEP_SECONDS).toList(),
            current = rest,
            onSelect = { onChange(minutes * 60 + it) },
            label = { stringResource(R.string.duration_seconds, it) },
            modifier = Modifier.weight(1f),
        )
    }
}
