package io.github.jessesaarinen.distractionkiller.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.jessesaarinen.distractionkiller.R
import io.github.jessesaarinen.distractionkiller.data.Settings
import io.github.jessesaarinen.distractionkiller.data.SettingsStore
import io.github.jessesaarinen.distractionkiller.detection.AppMarkers
import io.github.jessesaarinen.distractionkiller.detection.InspectorLog
import io.github.jessesaarinen.distractionkiller.detection.ScrollTimer
import io.github.jessesaarinen.distractionkiller.detection.TimerPhase
import io.github.jessesaarinen.distractionkiller.detection.TimerState

// Inspector (tasks N4 and N5): the scroll watcher's state right now, and a list
// of what it sees, so the short-video markers can be found and checked.
// Back gesture returns home.
@Composable
fun InspectorScreen(
    serviceEnabled: Boolean,
    onShare: () -> Unit,
) {
    val entries by InspectorLog.entries.collectAsState()
    val counts by InspectorLog.counts.collectAsState()
    val timer by ScrollTimer.state.collectAsState()
    val status by InspectorLog.status.collectAsState()
    val settings by SettingsStore.settings.collectAsState()

    Scaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.inspector_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
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
                    Button(onClick = onShare, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.inspector_share))
                    }
                    OutlinedButton(onClick = { InspectorLog.clear() }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.inspector_clear))
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val started = status.serviceStartedAt
                    StatusLine(
                        if (started != null) {
                            stringResource(R.string.inspector_status_running, InspectorLog.formatClock(started))
                        } else {
                            stringResource(R.string.inspector_status_stopped)
                        }
                    )
                    val front = status.inFrontApp
                    StatusLine(
                        if (front != null) {
                            stringResource(R.string.inspector_in_front, AppMarkers.label(front))
                        } else {
                            stringResource(R.string.inspector_not_in_front)
                        }
                    )
                    StatusLine(timerText(timer, settings))
                    if (!settings.interruptionsOn) StatusLine(stringResource(R.string.inspector_paused))
                    val lastAt = status.lastEventAt
                    StatusLine(
                        if (lastAt != null) {
                            stringResource(
                                R.string.inspector_last_event,
                                InspectorLog.formatClock(lastAt),
                                AppMarkers.label(status.lastEventApp.orEmpty()),
                            )
                        } else {
                            stringResource(R.string.inspector_last_event_none)
                        }
                    )
                    status.lastScroll?.let { StatusLine(stringResource(R.string.inspector_last_scroll, InspectorLog.formatEntry(it))) }
                }
            }
            item {
                OutlinedButton(onClick = { ScrollTimer.reset() }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.inspector_reset_timer))
                }
            }
            item {
                Text(
                    text = stringResource(R.string.inspector_counts_title),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            items(counts.entries.toList()) { (packageName, kinds) ->
                Text(
                    text = "$packageName ${InspectorLog.formatCounts(kinds)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
            }
            item {
                Text(
                    text = stringResource(R.string.inspector_events_title),
                    style = MaterialTheme.typography.titleMedium,
                )
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

/** How far the timer is, and when the next level comes. */
@Composable
private fun timerText(timer: TimerState, settings: Settings): String = when (timer.phase) {
    TimerPhase.COUNTING ->
        stringResource(R.string.inspector_timer_level1, timer.activeSeconds, settings.level1Seconds)
    TimerPhase.AFTER_LEVEL1 -> {
        // Level 2 comes level2DelaySeconds after the second where level 1 came.
        val level2At = timer.activeSeconds - timer.secondsAfterLevel1 + settings.level2DelaySeconds
        stringResource(R.string.inspector_timer_level2, timer.activeSeconds, level2At)
    }
    TimerPhase.PAUSED -> stringResource(R.string.inspector_timer_paused, timer.activeSeconds)
}

@Composable
private fun StatusLine(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium)
}
