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
import io.github.jessesaarinen.distractionkiller.data.WatchedApp
import io.github.jessesaarinen.distractionkiller.ui.components.Hint
import io.github.jessesaarinen.distractionkiller.ui.components.WheelPicker

/**
 * Sets an app's daily time with two carousels: hours, and minutes in steps of
 * Config.ALLOWANCE_STEP_MIN (task N21). 0 h 0 min = reminders from the first second.
 */
@Composable
fun AllowanceDialog(app: WatchedApp, onSave: (Int) -> Unit, onDismiss: () -> Unit) {
    var minutes by remember { mutableIntStateOf(app.allowanceMin) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(app.label, style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.allowance_question), style = MaterialTheme.typography.bodyLarge)
                AllowanceWheels(minutes = minutes, onChange = { minutes = it })
                Hint(stringResource(R.string.allowance_hint))
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(minutes) }) {
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

/** The hour and minute carousels for a daily time. Also used in the setup steps (task N22). */
@Composable
fun AllowanceWheels(minutes: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val hours = minutes / 60
    // Rounded to the carousel's steps.
    val rest = minutes % 60 / Config.ALLOWANCE_STEP_MIN * Config.ALLOWANCE_STEP_MIN
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        WheelPicker(
            values = (0..Config.ALLOWANCE_MAX_HOURS).toList(),
            current = hours,
            onSelect = { onChange(it * 60 + rest) },
            label = { stringResource(R.string.duration_hours, it) },
            modifier = Modifier.weight(1f),
        )
        WheelPicker(
            values = (0 until 60 step Config.ALLOWANCE_STEP_MIN).toList(),
            current = rest,
            onSelect = { onChange(hours * 60 + it) },
            label = { stringResource(R.string.duration_minutes, it) },
            modifier = Modifier.weight(1f),
        )
    }
}
