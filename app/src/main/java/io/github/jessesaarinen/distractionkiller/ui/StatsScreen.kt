package io.github.jessesaarinen.distractionkiller.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.jessesaarinen.distractionkiller.R
import io.github.jessesaarinen.distractionkiller.data.EventLog
import io.github.jessesaarinen.distractionkiller.data.Summary
import io.github.jessesaarinen.distractionkiller.data.summarize
import io.github.jessesaarinen.distractionkiller.ui.components.Hint
import io.github.jessesaarinen.distractionkiller.ui.components.PrimaryButton
import io.github.jessesaarinen.distractionkiller.ui.components.ProgressBar
import io.github.jessesaarinen.distractionkiller.ui.components.ScreenScaffold
import io.github.jessesaarinen.distractionkiller.ui.components.SectionCard
import io.github.jessesaarinen.distractionkiller.ui.components.SectionLabel
import io.github.jessesaarinen.distractionkiller.ui.components.SideBySide
import io.github.jessesaarinen.distractionkiller.ui.components.SunMark

// Stats (task N11), like views/stats.js in the web demo: numbers from the
// event log, and the log export for the RT8 test. Back arrow and gesture return home.
@Composable
fun StatsScreen(onShare: () -> Unit, onBack: () -> Unit) {
    val events by EventLog.events.collectAsState()
    val summary = remember(events) { summarize(events) }
    var confirmClear by remember { mutableStateOf(false) }

    ScreenScaffold(title = stringResource(R.string.stats_title), onBack = onBack) {
        if (summary.rounds == 0) {
            EmptyState()
        } else {
            Numbers(summary)
        }

        SectionLabel(stringResource(R.string.stats_testers_title))
        SectionCard {
            Hint(stringResource(R.string.stats_testers_hint))
            PrimaryButton(
                text = stringResource(R.string.stats_share),
                onClick = onShare,
                icon = R.drawable.ic_share,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )
            TextButton(
                onClick = { confirmClear = true },
                enabled = events.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(
                    text = stringResource(R.string.stats_clear),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (events.isNotEmpty()) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    },
                )
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            text = { Text(stringResource(R.string.stats_clear_confirm), style = MaterialTheme.typography.bodyLarge) },
            confirmButton = {
                TextButton(onClick = {
                    EventLog.clear()
                    confirmClear = false
                }) {
                    Text(stringResource(R.string.stats_clear_yes), color = MaterialTheme.colorScheme.error)
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
private fun EmptyState() {
    SectionCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SunMark(modifier = Modifier.size(96.dp), rise = 0.15f)
            Text(
                text = stringResource(R.string.stats_empty_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.stats_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun Numbers(summary: Summary) {
    // The main number: how often an interruption ended the scrolling.
    val onHero = MaterialTheme.colorScheme.onTertiary
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.tertiary,
        contentColor = onHero,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.stats_stop_rate_label).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = onHero.copy(alpha = 0.8f),
            )
            Text(
                text = stringResource(R.string.stats_percent, summary.stopRate),
                style = MaterialTheme.typography.displayLarge,
            )
            Text(
                text = stringResource(R.string.stats_stop_rate),
                style = MaterialTheme.typography.bodyLarge,
                color = onHero.copy(alpha = 0.9f),
            )
            ProgressBar(
                fraction = summary.stopRate / 100f,
                color = MaterialTheme.colorScheme.secondary,
                track = onHero.copy(alpha = 0.2f),
                height = 10.dp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }

    // The labels are plurals, so one reads "1 keskeytys" and "1 kerran lopetit" (#27).
    TileRow(
        summary.rounds.toString() to pluralStringResource(R.plurals.stats_rounds, summary.rounds),
        summary.stopped.toString() to pluralStringResource(R.plurals.stats_stopped, summary.stopped),
    )
    TileRow(
        summary.continued.toString() to pluralStringResource(R.plurals.stats_continued, summary.continued),
        summary.skipped.toString() to pluralStringResource(R.plurals.stats_skipped, summary.skipped),
    )
    TileRow(
        durationText(summary.avgScrollBeforeStop) to stringResource(R.string.stats_avg_before_stop),
        summary.dismissed.toString() to pluralStringResource(R.plurals.stats_dismissed, summary.dismissed),
    )

    SectionLabel(stringResource(R.string.stats_where_title))
    SectionCard {
        BarRow(
            label = stringResource(R.string.stats_where_level1),
            value = summary.stoppedAtLevel1.toString(),
            fraction = share(summary.stoppedAtLevel1, summary.stopped),
        )
        BarRow(
            label = stringResource(R.string.stats_where_level2),
            value = summary.stoppedAtLevel2.toString(),
            fraction = share(summary.stoppedAtLevel2, summary.stopped),
        )
    }

    SectionLabel(stringResource(R.string.stats_tasks_title))
    SectionCard {
        Hint(stringResource(R.string.stats_bar_legend))
        for (task in summary.byTask) {
            BarRow(
                icon = taskIcon(task.task),
                label = stringResource(taskLabel(task.task)),
                value = stringResource(R.string.stats_of, task.stopped, task.total),
                fraction = share(task.stopped, task.total),
            )
        }
    }

    if (summary.byApp.isNotEmpty()) {
        SectionLabel(stringResource(R.string.stats_apps_title))
        SectionCard {
            Hint(stringResource(R.string.stats_bar_legend))
            for (app in summary.byApp) {
                BarRow(
                    label = app.app,
                    value = stringResource(R.string.stats_of, app.stopped, app.rounds),
                    fraction = share(app.stopped, app.rounds),
                )
            }
        }
    }

    if (summary.answers.isNotEmpty()) {
        SectionLabel(stringResource(R.string.stats_answers_title))
        SectionCard {
            for (answer in summary.answers) {
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .size(8.dp)
                            .background(MaterialTheme.colorScheme.secondary, CircleShape),
                    )
                    Text(text = answer, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

private fun share(part: Int, whole: Int): Float = if (whole > 0) part / whole.toFloat() else 0f

/** Two number tiles side by side. Each pair is the number and its label. */
@Composable
private fun TileRow(first: Pair<String, String>, second: Pair<String, String>) {
    SideBySide {
        for ((value, label) in listOf(first, second)) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(text = value, style = MaterialTheme.typography.headlineMedium)
                    Hint(label)
                }
            }
        }
    }
}

/** A label, its number on the right, and a bar under them. */
@Composable
private fun BarRow(
    label: String,
    value: String,
    fraction: Float,
    @DrawableRes icon: Int? = null,
) {
    Column(
        modifier = Modifier.padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .size(20.dp),
                )
            }
            Text(text = label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(text = value, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        }
        ProgressBar(fraction = fraction)
    }
}
