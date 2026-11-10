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
    var hours by remember { mutableIntStateOf(app.allowanceMin / 60) }
    // Rounded to the carousel's steps.
    var minutes by remember { mutableIntStateOf(app.allowanceMin % 60 / Config.ALLOWANCE_STEP_MIN * Config.ALLOWANCE_STEP_MIN) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(app.label, style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.allowance_question), style = MaterialTheme.typography.bodyLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    WheelPicker(
                        values = (0..Config.ALLOWANCE_MAX_HOURS).toList(),
                        current = hours,
                        onSelect = { hours = it },
                        label = { stringResource(R.string.duration_hours, it) },
                        modifier = Modifier.weight(1f),
                    )
                    WheelPicker(
                        values = (0 until 60 step Config.ALLOWANCE_STEP_MIN).toList(),
                        current = minutes,
                        onSelect = { minutes = it },
                        label = { stringResource(R.string.duration_minutes, it) },
                        modifier = Modifier.weight(1f),
                    )
                }
                Hint(stringResource(R.string.allowance_hint))
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(hours * 60 + minutes) }) {
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
