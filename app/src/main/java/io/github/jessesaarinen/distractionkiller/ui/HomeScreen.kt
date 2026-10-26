package io.github.jessesaarinen.distractionkiller.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.jessesaarinen.distractionkiller.BuildConfig
import io.github.jessesaarinen.distractionkiller.Config
import io.github.jessesaarinen.distractionkiller.R
import io.github.jessesaarinen.distractionkiller.data.EventLog
import io.github.jessesaarinen.distractionkiller.data.SettingsStore
import io.github.jessesaarinen.distractionkiller.ui.theme.DistractionKillerTheme

// Home screen (task N12): whether the scroll watcher is on, and a setup guide
// whose steps get a check mark when they are done.
@Composable
fun HomeScreen(
    serviceEnabled: Boolean,
    batteryUnrestricted: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenInspector: () -> Unit,
) {
    val settings by SettingsStore.settings.collectAsState()
    val events by EventLog.events.collectAsState()
    // The whole chain works once the first interruption has happened.
    val firstInterruptionSeen = events.any { it.type == "level1" }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.home_intro),
                style = MaterialTheme.typography.bodyLarge,
            )
            if (serviceEnabled && !settings.interruptionsOn) {
                Text(
                    text = stringResource(R.string.home_paused),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Button(
                    onClick = { SettingsStore.update { it.copy(interruptionsOn = true) } },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.home_resume))
                }
            } else if (serviceEnabled) {
                Text(
                    text = stringResource(R.string.home_service_on, durationText(settings.level1Seconds)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Text(
                    text = stringResource(R.string.home_service_off),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            Text(
                text = stringResource(R.string.home_setup_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            SetupStep(
                number = 1,
                done = serviceEnabled,
                title = stringResource(R.string.setup_accessibility_title),
                text = stringResource(R.string.setup_accessibility_text),
            ) {
                Button(
                    onClick = onOpenAccessibilitySettings,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.home_open_accessibility))
                }
                // Android 13+ blocks this switch for apps installed from an APK file.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Text(
                        text = stringResource(R.string.setup_restricted_text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        onClick = onOpenAppInfo,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(R.string.home_open_app_info))
                    }
                }
            }
            SetupStep(
                number = 2,
                done = batteryUnrestricted,
                title = stringResource(R.string.setup_battery_title),
                text = stringResource(R.string.setup_battery_text),
            ) {
                OutlinedButton(
                    onClick = onOpenAppInfo,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.home_open_app_info))
                }
            }
            SetupStep(
                number = 3,
                done = firstInterruptionSeen,
                title = stringResource(R.string.setup_try_title),
                text = stringResource(R.string.setup_try_text, Config.DEMO_LEVEL1_SECONDS),
            )

            OutlinedButton(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.home_open_settings))
            }
            OutlinedButton(
                onClick = onOpenStats,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.home_open_stats))
            }
            OutlinedButton(
                onClick = onOpenInspector,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.home_open_inspector))
            }
            Text(
                text = stringResource(R.string.home_privacy),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.home_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** One setup step: a number, or a check mark once done. A done step shows only its title. */
@Composable
private fun SetupStep(
    number: Int,
    done: Boolean,
    title: String,
    text: String,
    actions: @Composable () -> Unit = {},
) {
    // A screen reader says "Vaihe 1" or "Vaihe 1, tehty" instead of "1" or "✓".
    val stepLabel = stringResource(if (done) R.string.setup_step_done else R.string.setup_step, number)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    color = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape,
                )
                .clearAndSetSemantics { contentDescription = stepLabel },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (done) stringResource(R.string.task_done_mark) else number.toString(),
                style = MaterialTheme.typography.titleSmall,
                color = if (done) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp).semantics { heading() },
            )
            if (!done) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                actions()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    DistractionKillerTheme {
        HomeScreen(
            serviceEnabled = false,
            batteryUnrestricted = false,
            onOpenAccessibilitySettings = {},
            onOpenAppInfo = {},
            onOpenSettings = {},
            onOpenStats = {},
            onOpenInspector = {},
        )
    }
}
