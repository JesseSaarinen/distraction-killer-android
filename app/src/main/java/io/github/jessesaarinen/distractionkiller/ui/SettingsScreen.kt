package io.github.jessesaarinen.distractionkiller.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.jessesaarinen.distractionkiller.AlarmMode
import io.github.jessesaarinen.distractionkiller.Config
import io.github.jessesaarinen.distractionkiller.R
import io.github.jessesaarinen.distractionkiller.TaskType
import io.github.jessesaarinen.distractionkiller.data.SettingsStore
import io.github.jessesaarinen.distractionkiller.detection.AppMarkers
import io.github.jessesaarinen.distractionkiller.interrupt.Alarm

// Settings (task N10), like views/settings.js in the web demo.
// Every change is saved right away. Back gesture returns home.
@Composable
fun SettingsScreen() {
    val settings by SettingsStore.settings.collectAsState()
    var tasksError by remember { mutableStateOf(false) }

    // For the test button. Released when the user leaves the screen.
    val context = LocalContext.current
    val alarm = remember { Alarm(context) }
    DisposableEffect(alarm) {
        onDispose { alarm.release() }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            SectionTitle(stringResource(R.string.settings_interval_title))
            Column(Modifier.selectableGroup()) {
                for (minutes in Config.INTERVAL_OPTIONS_MIN) {
                    RadioRow(
                        text = stringResource(R.string.duration_minutes, minutes),
                        selected = minutes == settings.intervalMin,
                        onClick = { SettingsStore.update { it.copy(intervalMin = minutes) } },
                    )
                }
            }
            Hint(stringResource(R.string.settings_interval_hint))

            SectionTitle(stringResource(R.string.settings_tasks_title))
            for (task in TaskType.entries) {
                CheckboxRow(
                    text = stringResource(taskLabel(task)),
                    checked = task in settings.tasks,
                    onCheckedChange = { checked ->
                        val tasks = if (checked) settings.tasks + task else settings.tasks - task
                        // At least one task must stay chosen, like in the web demo.
                        tasksError = tasks.isEmpty()
                        if (tasks.isNotEmpty()) SettingsStore.update { it.copy(tasks = tasks) }
                    },
                )
            }
            Hint(stringResource(if (tasksError) R.string.settings_tasks_min_one else R.string.settings_tasks_hint))

            SectionTitle(stringResource(R.string.settings_alarm_title))
            Column(Modifier.selectableGroup()) {
                RadioRow(
                    text = stringResource(R.string.alarm_until_dismissed),
                    selected = settings.alarmMode == AlarmMode.UNTIL_DISMISSED,
                    onClick = { SettingsStore.update { it.copy(alarmMode = AlarmMode.UNTIL_DISMISSED) } },
                )
                RadioRow(
                    text = stringResource(R.string.alarm_timed, Config.ALARM_TIMED_SECONDS),
                    selected = settings.alarmMode == AlarmMode.TIMED,
                    onClick = { SettingsStore.update { it.copy(alarmMode = AlarmMode.TIMED) } },
                )
            }
            Hint(stringResource(R.string.settings_alarm_hint))

            SectionTitle(stringResource(R.string.settings_apps_title))
            for (label in AppMarkers.labels) {
                SwitchRow(
                    title = label,
                    checked = label in settings.watchedApps,
                    onCheckedChange = { on ->
                        SettingsStore.update {
                            it.copy(watchedApps = if (on) it.watchedApps + label else it.watchedApps - label)
                        }
                    },
                )
            }
            Hint(stringResource(R.string.settings_apps_hint))

            SectionTitle(stringResource(R.string.settings_other_title))
            SwitchRow(
                title = stringResource(R.string.settings_sound),
                checked = settings.sound,
                onCheckedChange = { on -> SettingsStore.update { it.copy(sound = on) } },
            )
            SwitchRow(
                title = stringResource(R.string.settings_vibration),
                checked = settings.vibration,
                onCheckedChange = { on -> SettingsStore.update { it.copy(vibration = on) } },
            )
            SwitchRow(
                title = stringResource(R.string.settings_demo),
                hint = stringResource(
                    R.string.settings_demo_hint,
                    Config.DEMO_LEVEL1_SECONDS,
                    Config.DEMO_LEVEL2_DELAY_SECONDS,
                ),
                checked = settings.demoMode,
                onCheckedChange = { on -> SettingsStore.update { it.copy(demoMode = on) } },
            )
            OutlinedButton(
                onClick = { alarm.test(sound = settings.sound, vibration = settings.vibration) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.settings_test_alarm))
            }
        }
    }
}

@StringRes
private fun taskLabel(task: TaskType): Int = when (task) {
    TaskType.BREATHING -> R.string.task_breathing
    TaskType.QUESTION -> R.string.task_question
    TaskType.PHYSICAL -> R.string.task_physical
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 20.dp),
    )
}

@Composable
private fun Hint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// The whole row is the touch target (at least 48 dp), not just the small control.

@Composable
private fun RadioRow(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(text = text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun CheckboxRow(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Checkbox),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Text(text = text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, hint: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Switch),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (hint != null) Hint(hint)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}
