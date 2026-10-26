package io.github.jessesaarinen.distractionkiller.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import io.github.jessesaarinen.distractionkiller.R
import io.github.jessesaarinen.distractionkiller.data.DayStats

/**
 * The stats chart (N16): a bar for each of the last days. The bar is as high
 * as the day's interruptions, and its dark lower part shows how many of them
 * ended the scrolling. The busiest day fills the whole height.
 */
@Composable
fun DayChart(days: List<DayStats>) {
    val most = days.maxOf { it.rounds }.coerceAtLeast(1)
    val weekdays = stringArrayResource(R.array.weekdays_short)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (day in days) {
                val weekday = weekdays[day.date.dayOfWeek.value - 1]
                // A screen reader reads the day in words instead of the bar.
                val description = stringResource(R.string.stats_day, weekday, day.rounds, day.stopped)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clearAndSetSemantics { contentDescription = description },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = day.rounds.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Bar(rounds = day.rounds, stopped = day.stopped, most = most)
                    Text(text = weekday, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendItem(color = MaterialTheme.colorScheme.secondary, text = stringResource(R.string.stats_days_rounds))
            LegendItem(color = MaterialTheme.colorScheme.primary, text = stringResource(R.string.stats_days_stopped))
        }
    }
}

@Composable
private fun Bar(rounds: Int, stopped: Int, most: Int) {
    // Every day has the same space, and the bar grows up from the bottom.
    Box(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        if (rounds > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(rounds / most.toFloat())
                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                    .background(MaterialTheme.colorScheme.secondary),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(stopped.coerceAtMost(rounds) / rounds.toFloat())
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
