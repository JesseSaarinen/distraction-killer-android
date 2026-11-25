package io.github.jessesaarinen.distractionkiller.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.jessesaarinen.distractionkiller.Config
import io.github.jessesaarinen.distractionkiller.R
import io.github.jessesaarinen.distractionkiller.TaskType
import io.github.jessesaarinen.distractionkiller.data.SettingsStore
import io.github.jessesaarinen.distractionkiller.data.WatchedApp
import io.github.jessesaarinen.distractionkiller.detection.UsageTimer
import io.github.jessesaarinen.distractionkiller.ui.components.Hint
import io.github.jessesaarinen.distractionkiller.ui.components.ScreenScaffold
import io.github.jessesaarinen.distractionkiller.ui.components.SecondaryButton
import io.github.jessesaarinen.distractionkiller.ui.components.SectionCard
import io.github.jessesaarinen.distractionkiller.ui.components.SectionLabel
import io.github.jessesaarinen.distractionkiller.ui.components.SegmentedChoice
import io.github.jessesaarinen.distractionkiller.ui.components.SelectRow
import io.github.jessesaarinen.distractionkiller.ui.components.SwitchRow

// Settings (tasks N10 and N22), like views/settings.js in the web demo.
// Every change is saved right away. The back arrow and back gesture return home.
@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenPermissions: () -> Unit, onNewDemo: () -> Unit) {
    val settings by SettingsStore.settings.collectAsState()
    var tasksError by remember { mutableStateOf(false) }
    var pickingApp by remember { mutableStateOf(false) }
    var editingApp by remember { mutableStateOf<WatchedApp?>(null) }
    var editingRepeat by remember { mutableStateOf(false) }
    val usage by UsageTimer.state.collectAsState()

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
            // Demo mode overrides the daily times below, so say it where the times are.
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Text(
                    text = stringResource(R.string.settings_demo_active, settings.demoAllowanceSec),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                )
            }
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
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            // Task N22: how often the reminder repeats, under the apps it is about.
            ValueRow(
                title = stringResource(R.string.repeat_title),
                value = durationText(settings.repeatSeconds),
                onClick = { editingRepeat = true },
            )
            Hint(stringResource(R.string.settings_repeat_hint), modifier = Modifier.padding(bottom = 4.dp))
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
            Hint(stringResource(R.string.settings_alarm_hint, Config.ALARM_SECONDS))
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
            if (settings.sound) {
                Text(
                    text = stringResource(R.string.settings_sound_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            SoundChoice(
                selected = settings.alarmSound,
                onSelect = { sound -> SettingsStore.update { it.copy(alarmSound = sound) } },
                soundOn = settings.sound,
                vibrationOn = settings.vibration,
            )
        }

        SectionLabel(stringResource(R.string.settings_section_demo))
        SectionCard {
            SwitchRow(
                title = stringResource(R.string.settings_demo),
                hint = stringResource(R.string.settings_demo_hint),
                checked = settings.demoMode,
                onCheckedChange = { on -> SettingsStore.update { it.copy(demoMode = on) } },
            )
            if (settings.demoMode) {
                Text(text = stringResource(R.string.demo_allowance_title), style = MaterialTheme.typography.titleMedium)
                DemoAllowanceChoice(
                    selected = settings.demoAllowanceSec,
                    onSelect = { seconds -> SettingsStore.update { it.copy(demoAllowanceSec = seconds) } },
                )
            }
            SecondaryButton(
                text = stringResource(R.string.settings_new_demo),
                onClick = onNewDemo,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )
            Hint(stringResource(R.string.settings_new_demo_hint), modifier = Modifier.padding(bottom = 4.dp))
        }

        // Task N22: the permission steps live here, so they do not show on the home screen during a demo.
        SectionLabel(stringResource(R.string.home_setup_title))
        SectionCard {
            ValueRow(
                title = stringResource(R.string.settings_permissions),
                value = stringResource(R.string.settings_permissions_open),
                onClick = onOpenPermissions,
            )
            Hint(stringResource(R.string.settings_permissions_hint), modifier = Modifier.padding(bottom = 4.dp))
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
    if (editingRepeat) {
        RepeatDialog(
            seconds = settings.repeatSeconds,
            onSave = { seconds ->
                SettingsStore.update { it.copy(repeatSeconds = seconds) }
                editingRepeat = false
            },
            onDismiss = { editingRepeat = false },
        )
    }
}

/** The demo mode's daily time: 10 s, 15 s, 30 s or 1 min. */
@Composable
fun DemoAllowanceChoice(selected: Int, onSelect: (Int) -> Unit) {
    SegmentedChoice(
        options = Config.DEMO_ALLOWANCE_OPTIONS_SECONDS,
        selected = selected,
        label = { seconds -> durationText(seconds) },
        onSelect = onSelect,
    )
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

/** A setting that opens a dialog or a screen: its name on the left, the current value on the right. */
@Composable
private fun ValueRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Text(text = value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}

/** One line under the task's name: what the task asks the user to do. */
@Composable
private fun taskDescription(task: TaskType): String = when (task) {
    TaskType.BREATHING -> stringResource(R.string.task_breathing_desc, Config.BREATHING_SECONDS)
    TaskType.QUESTION -> stringResource(R.string.task_question_desc)
    TaskType.PHYSICAL -> stringResource(R.string.task_physical_desc, Config.PHYSICAL_SECONDS)
}
