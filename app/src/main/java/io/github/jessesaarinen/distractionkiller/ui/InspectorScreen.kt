package io.github.jessesaarinen.distractionkiller.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.jessesaarinen.distractionkiller.R
import io.github.jessesaarinen.distractionkiller.data.SettingsStore
import io.github.jessesaarinen.distractionkiller.detection.InspectorLog
import io.github.jessesaarinen.distractionkiller.detection.UsagePhase
import io.github.jessesaarinen.distractionkiller.detection.UsageTimer
import io.github.jessesaarinen.distractionkiller.ui.components.PrimaryButton
import io.github.jessesaarinen.distractionkiller.ui.components.SecondaryButton
import io.github.jessesaarinen.distractionkiller.ui.components.SectionCard
import io.github.jessesaarinen.distractionkiller.ui.components.SectionLabel

// Inspector (task N4, simplified in N21): the watcher's state right now, each
// watched app's time today, and a list of what happened, for checking on a phone.
// Back arrow and back gesture return home.
@Composable
fun InspectorScreen(
    serviceEnabled: Boolean,
    onShare: () -> Unit,
    onBack: () -> Unit,
) {
    val entries by InspectorLog.entries.collectAsState()
    val usage by UsageTimer.state.collectAsState()
    val status by InspectorLog.status.collectAsState()
    val settings by SettingsStore.settings.collectAsState()

    Scaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                IconButton(onClick = onBack, modifier = Modifier.offset(x = (-12).dp)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_back),
                        contentDescription = stringResource(R.string.back),
                    )
                }
            }
            item {
                Text(
                    text = stringResource(R.string.inspector_title),
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.semantics { heading() },
                )
            }
            item {
                Text(
                    text = stringResource(R.string.inspector_intro),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (!serviceEnabled) {
                item {
                    Text(
                        text = stringResource(R.string.inspector_service_off),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrimaryButton(
                        text = stringResource(R.string.inspector_share),
                        onClick = onShare,
                        icon = R.drawable.ic_share,
                        modifier = Modifier.weight(1f),
                    )
                    SecondaryButton(
                        text = stringResource(R.string.inspector_clear),
                        onClick = { InspectorLog.clear() },
                        modifier = Modifier
                            .weight(1f)
                            .align(Alignment.CenterVertically),
                    )
                }
            }
            item {
                SectionCard {
                    val started = status.serviceStartedAt
                    StatusLine(
                        if (started != null) {
                            stringResource(R.string.inspector_status_running, InspectorLog.formatClock(started))
                        } else {
                            stringResource(R.string.inspector_status_stopped)
                        }
                    )
                    StatusLine(stringResource(R.string.inspector_in_front, status.inFrontApp ?: "–"))
                    for (app in settings.apps) {
                        val day = usage.usage(app.packageName)
                        StatusLine(
                            stringResource(
                                R.string.inspector_app_today,
                                app.label,
                                durationText(day.usedSeconds),
                                durationText(settings.allowanceSeconds(app)),
                                phaseText(day.phase),
                            )
                        )
                    }
                    if (!settings.interruptionsOn) StatusLine(stringResource(R.string.inspector_paused))
                }
            }
            item {
                SecondaryButton(
                    text = stringResource(R.string.inspector_reset_timer),
                    onClick = { UsageTimer.reset(System.currentTimeMillis()) },
                )
            }
            item {
                SectionLabel(stringResource(R.string.inspector_events_title))
            }
            if (entries.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.inspector_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(entries) { entry ->
                Text(
                    text = InspectorLog.formatEntry(entry),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

/** Where the app is in its day, in a few words. */
@Composable
private fun phaseText(phase: UsagePhase): String = when (phase) {
    UsagePhase.ALLOWANCE -> stringResource(R.string.inspector_phase_allowance)
    UsagePhase.AFTER_LEVEL1 -> stringResource(R.string.inspector_phase_after_level1)
    UsagePhase.BETWEEN_REMINDERS -> stringResource(R.string.inspector_phase_between)
}

@Composable
private fun StatusLine(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium)
}
