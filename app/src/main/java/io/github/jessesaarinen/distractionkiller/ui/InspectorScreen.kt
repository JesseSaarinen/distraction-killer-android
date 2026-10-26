package io.github.jessesaarinen.distractionkiller.ui

import androidx.compose.foundation.layout.Arrangement
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
import io.github.jessesaarinen.distractionkiller.detection.InspectorLog
import io.github.jessesaarinen.distractionkiller.detection.ScrollTimer

// Inspector (task N4): lists what the scroll watcher sees, so we can find
// the view IDs of the short-video screens (task N5). Back gesture returns home.
@Composable
fun InspectorScreen(
    serviceEnabled: Boolean,
    onShare: () -> Unit,
) {
    val entries by InspectorLog.entries.collectAsState()
    val counts by InspectorLog.counts.collectAsState()
    val timer by ScrollTimer.state.collectAsState()

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
                Text(
                    text = stringResource(R.string.inspector_timer, timer.activeSeconds, timer.phase.name),
                    style = MaterialTheme.typography.bodyMedium,
                )
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
