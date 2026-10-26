package io.github.jessesaarinen.distractionkiller.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.jessesaarinen.distractionkiller.R
import io.github.jessesaarinen.distractionkiller.data.EventLog
import io.github.jessesaarinen.distractionkiller.data.Summary
import io.github.jessesaarinen.distractionkiller.data.summarize

// Stats (task N11), like views/stats.js in the web demo: numbers from the
// event log, and the log export for the RT8 test. Back gesture returns home.
@Composable
fun StatsScreen(onShare: () -> Unit) {
    val events by EventLog.events.collectAsState()
    val summary = remember(events) { summarize(events) }
    var confirmClear by remember { mutableStateOf(false) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.stats_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            if (summary.rounds == 0) {
                Text(
                    text = stringResource(R.string.stats_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Numbers(summary)
            }

            SectionTitle(stringResource(R.string.stats_testers_title))
            Button(
                onClick = onShare,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.stats_share))
            }
            OutlinedButton(
                onClick = { confirmClear = true },
                enabled = events.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.stats_clear))
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            text = { Text(stringResource(R.string.stats_clear_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    EventLog.clear()
                    confirmClear = false
                }) {
                    Text(stringResource(R.string.stats_clear_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text(stringResource(R.string.stats_clear_no))
                }
            },
        )
    }
}

@Composable
private fun Numbers(summary: Summary) {
    // The main number: how often an interruption ended the scrolling.
    StatBox(
        value = stringResource(R.string.stats_percent, summary.stopRate),
        label = stringResource(R.string.stats_stop_rate),
    ) {
        ProgressBar(fraction = summary.stopRate / 100f)
    }
    StatRow(
        summary.rounds to R.string.stats_rounds,
        summary.stopped to R.string.stats_stopped,
    )
    StatRow(
        summary.continued to R.string.stats_continued,
        summary.skipped to R.string.stats_skipped,
    )
    StatBox(value = summary.dismissed.toString(), label = stringResource(R.string.stats_dismissed))
    StatBox(
        value = durationText(summary.avgScrollBeforeStop),
        label = stringResource(R.string.stats_avg_before_stop),
    )

    SectionTitle(stringResource(R.string.stats_where_title))
    TableRow(stringResource(R.string.stats_where_level1), listOf(summary.stoppedAtLevel1.toString()))
    TableRow(stringResource(R.string.stats_where_level2), listOf(summary.stoppedAtLevel2.toString()))

    SectionTitle(stringResource(R.string.stats_tasks_title))
    TableRow(
        label = stringResource(R.string.stats_col_task),
        numbers = listOf(stringResource(R.string.stats_col_times), stringResource(R.string.stats_col_stopped)),
        header = true,
    )
    for (task in summary.byTask) {
        TableRow(stringResource(taskLabel(task.task)), listOf(task.total.toString(), task.stopped.toString()))
    }

    SectionTitle(stringResource(R.string.stats_apps_title))
    TableRow(
        label = stringResource(R.string.stats_col_app),
        numbers = listOf(stringResource(R.string.stats_col_times), stringResource(R.string.stats_col_stopped)),
        header = true,
    )
    for (app in summary.byApp) {
        TableRow(app.app, listOf(app.rounds.toString(), app.stopped.toString()))
    }

    if (summary.answers.isNotEmpty()) {
        SectionTitle(stringResource(R.string.stats_answers_title))
        for (answer in summary.answers) {
            Text(text = stringResource(R.string.stats_answer, answer), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/** Two numbers side by side, each with its label. */
@Composable
private fun StatRow(first: Pair<Int, Int>, second: Pair<Int, Int>) {
    Row(
        modifier = Modifier.height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        for ((number, label) in listOf(first, second)) {
            StatBox(
                value = number.toString(),
                label = stringResource(label),
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun StatBox(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    extra: @Composable () -> Unit = {},
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            extra()
        }
    }
}

@Composable
private fun ProgressBar(fraction: Float) {
    Box(
        modifier = Modifier
            .padding(top = 4.dp)
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.background),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 12.dp),
    )
}

/** A label and one or more numbers in fixed-width columns on the right. */
@Composable
private fun TableRow(label: String, numbers: List<String>, header: Boolean = false) {
    val style = if (header) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyLarge
    val color = if (header) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, style = style, color = color, modifier = Modifier.weight(1f))
        for (number in numbers) {
            Text(
                text = number,
                style = style,
                color = color,
                textAlign = TextAlign.End,
                modifier = Modifier.width(72.dp),
            )
        }
    }
}
