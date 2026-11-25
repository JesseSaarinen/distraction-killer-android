package io.github.jessesaarinen.distractionkiller.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.jessesaarinen.distractionkiller.Config
import io.github.jessesaarinen.distractionkiller.R
import io.github.jessesaarinen.distractionkiller.data.Settings
import io.github.jessesaarinen.distractionkiller.data.SettingsStore
import io.github.jessesaarinen.distractionkiller.data.WatchedApp
import io.github.jessesaarinen.distractionkiller.ui.components.CheckCircle
import io.github.jessesaarinen.distractionkiller.ui.components.Hint
import io.github.jessesaarinen.distractionkiller.ui.components.PrimaryButton
import io.github.jessesaarinen.distractionkiller.ui.components.ProgressBar
import io.github.jessesaarinen.distractionkiller.ui.components.SecondaryButton
import io.github.jessesaarinen.distractionkiller.ui.components.SectionCard
import io.github.jessesaarinen.distractionkiller.ui.components.SunMark

private const val STEP_APPS = 0
private const val STEP_TIME = 1
private const val STEP_REPEAT = 2
private const val STEP_SOUND = 3
private const val STEP_COUNT = 4

/**
 * The setup steps (task N22), shown when the app opens for the first time and
 * after "Aloita uusi demo": which apps are watched, after how much use the
 * first reminder comes, how often it repeats, and what it sounds like. Nothing
 * is saved before Valmis. The back gesture goes one step back.
 */
@Composable
fun SetupWizard(onDone: () -> Unit) {
    val start = remember { SettingsStore.settings.value }
    var step by rememberSaveable { mutableIntStateOf(STEP_APPS) }
    // The default apps are always offered, plus any app the user added before.
    var candidates by remember {
        mutableStateOf((Config.DEFAULT_APPS + start.apps).distinctBy { it.packageName }.sortedBy { it.label.lowercase() })
    }
    var chosen by remember { mutableStateOf(start.apps.map { it.packageName }.toSet()) }
    var allowanceMin by remember { mutableIntStateOf(start.apps.firstOrNull()?.allowanceMin ?: Config.DEFAULT_ALLOWANCE_MIN) }
    var demoAllowanceSec by remember { mutableIntStateOf(start.demoAllowanceSec) }
    var repeatSeconds by remember { mutableIntStateOf(start.repeatSeconds) }
    var sound by remember { mutableStateOf(start.alarmSound) }
    var pickingApp by remember { mutableStateOf(false) }

    BackHandler(enabled = step > STEP_APPS) { step-- }

    fun finish() {
        SettingsStore.update { s ->
            s.copy(
                // In demo mode the daily time is the demo's seconds, so the apps keep their minutes.
                apps = candidates
                    .filter { it.packageName in chosen }
                    .map { if (s.demoMode) it else it.copy(allowanceMin = allowanceMin) },
                demoAllowanceSec = demoAllowanceSec,
                repeatSeconds = Settings.clampRepeat(repeatSeconds),
                alarmSound = sound,
                sound = true,
                interruptionsOn = true,
                setupDone = true,
            )
        }
        onDone()
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SunMark(modifier = Modifier.size(32.dp), rise = 0.55f)
                Text(
                    text = stringResource(R.string.wizard_step, step + 1, STEP_COUNT),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            ProgressBar(fraction = (step + 1) / STEP_COUNT.toFloat())

            when (step) {
                STEP_APPS -> {
                    StepTitle(stringResource(R.string.wizard_apps_title), stringResource(R.string.wizard_apps_text))
                    SectionCard {
                        for (app in candidates) {
                            AppCheckRow(
                                label = app.label,
                                checked = app.packageName in chosen,
                                onCheckedChange = { on ->
                                    chosen = if (on) chosen + app.packageName else chosen - app.packageName
                                },
                            )
                        }
                        SecondaryButton(
                            text = stringResource(R.string.settings_apps_add),
                            onClick = { pickingApp = true },
                            icon = R.drawable.ic_plus,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 4.dp),
                        )
                    }
                    if (chosen.isEmpty()) {
                        Hint(stringResource(R.string.wizard_apps_min_one), color = MaterialTheme.colorScheme.error)
                    }
                }
                STEP_TIME -> {
                    StepTitle(stringResource(R.string.wizard_time_title), stringResource(R.string.wizard_time_text))
                    SectionCard {
                        if (start.demoMode) {
                            DemoAllowanceChoice(selected = demoAllowanceSec, onSelect = { demoAllowanceSec = it })
                            Hint(stringResource(R.string.wizard_time_demo_hint))
                        } else {
                            AllowanceWheels(minutes = allowanceMin, onChange = { allowanceMin = it })
                            Hint(stringResource(R.string.allowance_hint))
                        }
                    }
                }
                STEP_REPEAT -> {
                    StepTitle(stringResource(R.string.repeat_title), stringResource(R.string.wizard_repeat_text))
                    SectionCard {
                        RepeatWheels(seconds = repeatSeconds, onChange = { repeatSeconds = it })
                        Hint(stringResource(R.string.repeat_hint, Config.REPEAT_MIN_SECONDS))
                    }
                }
                else -> {
                    StepTitle(stringResource(R.string.wizard_sound_title), stringResource(R.string.wizard_sound_text))
                    SectionCard {
                        SoundChoice(
                            selected = sound,
                            onSelect = { sound = it },
                            soundOn = true,
                            vibrationOn = start.vibration,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (step > STEP_APPS) {
                    SecondaryButton(
                        text = stringResource(R.string.back),
                        onClick = { step-- },
                        modifier = Modifier.weight(1f),
                    )
                }
                PrimaryButton(
                    text = stringResource(if (step == STEP_SOUND) R.string.wizard_done else R.string.wizard_next),
                    onClick = { if (step == STEP_SOUND) finish() else step++ },
                    enabled = chosen.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    if (pickingApp) {
        AppPickerDialog(
            exclude = candidates.map { it.packageName }.toSet(),
            onPick = { app: WatchedApp ->
                candidates = (candidates + app).sortedBy { it.label.lowercase() }
                chosen = chosen + app.packageName
                pickingApp = false
            },
            onDismiss = { pickingApp = false },
        )
    }
}

@Composable
private fun StepTitle(title: String, text: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.semantics { heading() },
    )
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** An app that can be picked on and off: its name and a round check box. */
@Composable
private fun AppCheckRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(MaterialTheme.shapes.medium)
            .toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Checkbox)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        CheckCircle(checked)
    }
}
