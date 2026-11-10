package io.github.jessesaarinen.distractionkiller.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.jessesaarinen.distractionkiller.AlarmMode
import io.github.jessesaarinen.distractionkiller.Config
import io.github.jessesaarinen.distractionkiller.R
import io.github.jessesaarinen.distractionkiller.TaskType
import io.github.jessesaarinen.distractionkiller.data.SettingsStore
import io.github.jessesaarinen.distractionkiller.data.WatchedApp
import io.github.jessesaarinen.distractionkiller.detection.UsageTimer
import io.github.jessesaarinen.distractionkiller.interrupt.Alarm
import io.github.jessesaarinen.distractionkiller.ui.components.Hint
import io.github.jessesaarinen.distractionkiller.ui.components.ScreenScaffold
import io.github.jessesaarinen.distractionkiller.ui.components.SecondaryButton
import io.github.jessesaarinen.distractionkiller.ui.components.SectionCard
import io.github.jessesaarinen.distractionkiller.ui.components.SectionLabel
import io.github.jessesaarinen.distractionkiller.ui.components.SegmentedChoice
import io.github.jessesaarinen.distractionkiller.ui.components.SelectRow
import io.github.jessesaarinen.distractionkiller.ui.components.SwitchRow

// Settings (task N10), like views/settings.js in the web demo.
// Every change is saved right away. The back arrow and back gesture return home.
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val settings by SettingsStore.settings.collectAsState()
    var tasksError by remember { mutableStateOf(false) }
    var pickingApp by remember { mutableStateOf(false) }
    var editingApp by remember { mutableStateOf<WatchedApp?>(null) }
    val usage by UsageTimer.state.collectAsState()

    // For the test button. Released when the user leaves the screen.
    val context = LocalContext.current
    val alarm = remember { Alarm(context) }
    DisposableEffect(alarm) {
        onDispose { alarm.release() }
    }

    ScreenScaffold(title = stringResource(R.string.settings_title), onBack = onBack) {
        // The main switch, in petrol while the app is on.
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = if (settings.interruptionsOn) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ) {
            SwitchRow(
                title = stringResource(R.string.settings_interruptions_on),
                hint = stringResource(R.string.settings_interruptions_hint),
                checked = settings.interruptionsOn,
                onCheckedChange = { on -> SettingsStore.update { it.copy(interruptionsOn = on) } },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }

        if (settings.demoMode) {
            // Demo mode overrides the times below, so say it where the times are.
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Text(
                    text = stringResource(
                        R.string.settings_demo_active,
                        Config.DEMO_ALLOWANCE_SECONDS,
                        Config.DEMO_LEVEL2_DELAY_SECONDS,
                        Config.DEMO_INTERVAL_SECONDS,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                )
            }
        }

        SectionLabel(stringResource(R.string.settings_section_timing))
        SectionCard {
            Text(text = stringResource(R.string.settings_interval_title), style = MaterialTheme.typography.titleMedium)
            SegmentedChoice(
                options = Config.INTERVAL_OPTIONS_MIN,
                selected = settings.intervalMin,
                label = { minutes -> stringResource(R.string.duration_minutes, minutes) },
                onSelect = { minutes -> SettingsStore.update { it.copy(intervalMin = minutes) } },
            )
            Hint(stringResource(R.string.settings_interval_hint))

            Text(
                text = stringResource(R.string.settings_level2_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp),
            )
            SegmentedChoice(
                options = Config.LEVEL2_DELAY_OPTIONS_SECONDS,
                selected = settings.level2AfterSec,
                label = { seconds -> durationText(seconds) },
                onSelect = { seconds -> SettingsStore.update { it.copy(level2AfterSec = seconds) } },
            )
            Hint(stringResource(R.string.settings_level2_hint))
        }

        SectionLabel(stringResource(R.string.settings_tasks_title))
        SectionCard {
            for (task in TaskType.entries) {
                SelectRow(
                    icon = taskIcon(task),
                    title = stringResource(taskLabel(task)),
                    description = taskDescription(task),
                    checked = task in settings.tasks,
                    onCheckedChange = { checked ->
                        val tasks = if (checked) settings.tasks + task else settings.tasks - task
                        // At least one task must stay chosen, like in the web demo.
                        tasksError = tasks.isEmpty()
                        if (tasks.isNotEmpty()) SettingsStore.update { it.copy(tasks = tasks) }
                    },
                )
            }
            if (tasksError) {
                Hint(stringResource(R.string.settings_tasks_min_one), color = MaterialTheme.colorScheme.error)
            } else {
                Hint(stringResource(R.string.settings_tasks_hint))
            }
        }

        SectionLabel(stringResource(R.string.settings_section_alarm))
        SectionCard {
            Text(text = stringResource(R.string.settings_alarm_title), style = MaterialTheme.typography.titleMedium)
            SegmentedChoice(
                options = AlarmMode.entries,
                selected = settings.alarmMode,
                label = { mode ->
                    when (mode) {
                        AlarmMode.UNTIL_DISMISSED -> stringResource(R.string.alarm_until_dismissed)
                        AlarmMode.TIMED -> stringResource(R.string.alarm_timed, Config.ALARM_TIMED_SECONDS)
                    }
                },
                onSelect = { mode -> SettingsStore.update { it.copy(alarmMode = mode) } },
            )
            Hint(stringResource(R.string.settings_alarm_hint))
            SwitchRow(
                title = stringResource(R.string.settings_sound),
                checked = settings.sound,
                onCheckedChange = { on -> SettingsStore.update { it.copy(sound = on) } },
                modifier = Modifier.padding(top = 4.dp),
            )
            SwitchRow(
                title = stringResource(R.string.settings_vibration),
                checked = settings.vibration,
                onCheckedChange = { on -> SettingsStore.update { it.copy(vibration = on) } },
            )
            SecondaryButton(
                text = stringResource(R.string.settings_test_alarm),
                onClick = { alarm.test(sound = settings.sound, vibration = settings.vibration) },
                icon = R.drawable.ic_bell,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 4.dp),
            )
        }

        SectionLabel(stringResource(R.string.settings_apps_title))
        SectionCard {
            for (app in settings.apps) {
                AppRow(
                    app = app,
                    usedSeconds = usage.usage(app.packageName).usedSeconds,
                    onEdit = { editingApp = app },
                    onRemove = { SettingsStore.update { it.removeApp(app.packageName) } },
                )
            }
            if (settings.apps.isEmpty()) Hint(stringResource(R.string.home_no_apps))
            SecondaryButton(
                text = stringResource(R.string.settings_apps_add),
                onClick = { pickingApp = true },
                icon = R.drawable.ic_plus,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )
            Hint(stringResource(R.string.settings_apps_hint), modifier = Modifier.padding(bottom = 4.dp))
        }

        SectionLabel(stringResource(R.string.settings_section_demo))
        SectionCard {
            SwitchRow(
                title = stringResource(R.string.settings_demo),
                hint = stringResource(
                    R.string.settings_demo_hint,
                    Config.DEMO_ALLOWANCE_SECONDS,
                    Config.DEMO_LEVEL2_DELAY_SECONDS,
                    Config.DEMO_INTERVAL_SECONDS,
                ),
                checked = settings.demoMode,
                onCheckedChange = { on -> SettingsStore.update { it.copy(demoMode = on) } },
            )
        }
    }

    if (pickingApp) {
        AppPickerDialog(
            exclude = settings.apps.map { it.packageName }.toSet(),
            onPick = { app ->
                SettingsStore.update { it.addApp(app) }
                pickingApp = false
                // Right away to its daily time, so the user sees it can be changed.
                editingApp = app
            },
            onDismiss = { pickingApp = false },
        )
    }
    editingApp?.let { app ->
        AllowanceDialog(
            app = app,
            onSave = { minutes ->
                SettingsStore.update { it.setAllowance(app.packageName, minutes) }
                editingApp = null
            },
            onDismiss = { editingApp = null },
        )
    }
}

/**
 * A watched app: its name, its daily time and how much of it is used today.
 * Tapping the row opens the daily time carousels, the cross removes the app.
 */
@Composable
private fun AppRow(app: WatchedApp, usedSeconds: Int, onEdit: () -> Unit, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(MaterialTheme.shapes.small)
                .clickable(onClickLabel = stringResource(R.string.settings_apps_edit), onClick = onEdit)
                .heightIn(min = 48.dp)
                .padding(end = 16.dp, top = 6.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = app.label, style = MaterialTheme.typography.titleMedium)
            Hint(
                stringResource(
                    R.string.settings_apps_allowance,
                    allowanceText(app.allowanceMin),
                    allowanceText(usedSeconds / 60),
                )
            )
        }
        IconButton(onClick = onRemove) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.settings_apps_remove, app.label),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** One line under the task's name: what the task asks the user to do. */
@Composable
private fun taskDescription(task: TaskType): String = when (task) {
    TaskType.BREATHING -> stringResource(R.string.task_breathing_desc, Config.BREATHING_SECONDS)
    TaskType.QUESTION -> stringResource(R.string.task_question_desc)
    TaskType.PHYSICAL -> stringResource(R.string.task_physical_desc, Config.PHYSICAL_SECONDS)
}
