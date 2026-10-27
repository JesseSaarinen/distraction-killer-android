package io.github.jessesaarinen.distractionkiller.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.jessesaarinen.distractionkiller.R
import io.github.jessesaarinen.distractionkiller.data.DayStats
import io.github.jessesaarinen.distractionkiller.ui.components.Hint

// The stats chart (task N16): a column for each of the last days, today on the
// right. A column is as high as the day's interruptions (the busiest day fills
// the height). Its petrol lower part is the interruptions that ended the
// scrolling, the grey upper part the rest. Tapping a day shows its numbers
// under the chart, today's at first. A screen reader reads each day in words.

/** The height of the busiest day's column. */
private val PLOT_HEIGHT = 96.dp

/** Thin columns with air between them, so the chart stays calm. */
private val BAR_WIDTH = 24.dp

@Composable
fun DayChart(days: List<DayStats>) {
    if (days.isEmpty()) return
    val most = days.maxOf { it.rounds }.coerceAtLeast(1)
    // Today (the last day) is chosen at first.
    var selected by rememberSaveable { mutableIntStateOf(days.lastIndex) }
    val shortNames = stringArrayResource(R.array.weekdays_short)
    val longNames = stringArrayResource(R.array.weekdays)
    val texts = days.mapIndexed { i, day ->
        val name = when (i) {
            days.lastIndex -> stringResource(R.string.stats_days_today)
            days.lastIndex - 1 -> stringResource(R.string.stats_days_yesterday)
            else -> stringResource(
                R.string.stats_days_date,
                longNames[day.date.dayOfWeek.value - 1],
                day.date.dayOfMonth,
                day.date.monthValue,
            )
        }
        stringResource(R.string.stats_day, name, day.rounds, day.stopped.coerceAtMost(day.rounds))
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // The columns share the whole width, so a tap anywhere on the chart picks a day.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
        ) {
            days.forEachIndexed { i, day ->
                DayColumn(
                    day = day,
                    most = most,
                    weekday = shortNames[day.date.dayOfWeek.value - 1],
                    description = texts[i],
                    selected = i == selected,
                    onSelect = { selected = i },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Text(text = texts[selected.coerceIn(days.indices)], style = MaterialTheme.typography.titleSmall)
        // FlowRow puts the second item on its own line when the font is large.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            LegendItem(color = MaterialTheme.colorScheme.primary, text = stringResource(R.string.stats_days_stopped))
            LegendItem(color = otherColor(), text = stringResource(R.string.stats_days_other))
        }
        Hint(stringResource(R.string.stats_days_hint))
    }
}

/** One day: its column on the baseline and the weekday under it. The chosen day has a sand background. */
@Composable
private fun DayColumn(
    day: DayStats,
    most: Int,
    weekday: String,
    description: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val highlight by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
        label = "day",
    )
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(highlight)
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .semantics { contentDescription = description }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(PLOT_HEIGHT),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Bar(day = day, most = most)
        }
        // The baseline. The columns' pieces join into one hairline.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
        Text(
            text = weekday,
            style = MaterialTheme.typography.bodySmall,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            // The column's description already says the day.
            modifier = Modifier
                .padding(top = 6.dp)
                .clearAndSetSemantics { },
        )
    }
}

/** The column: rounded at the top, square at the baseline, with a 2 dp gap between its two parts. */
@Composable
private fun Bar(day: DayStats, most: Int) {
    if (day.rounds == 0) return
    val stopped = day.stopped.coerceAtMost(day.rounds)
    val other = day.rounds - stopped
    val roundedTop = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
    Column(
        modifier = Modifier
            .width(BAR_WIDTH)
            .height(PLOT_HEIGHT * (day.rounds / most.toFloat())),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (other > 0) {
            Box(
                modifier = Modifier
                    .weight(other.toFloat())
                    .fillMaxWidth()
                    .clip(roundedTop)
                    .background(otherColor()),
            )
        }
        if (stopped > 0) {
            Box(
                modifier = Modifier
                    .weight(stopped.toFloat())
                    .fillMaxWidth()
                    .clip(if (other > 0) RectangleShape else roundedTop)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

/**
 * The grey of the other interruptions: the quiet text colour at 70 %. It is at
 * least 3:1 against the card in both themes (outline is only 2.7:1 in the dark).
 */
@Composable
private fun otherColor(): Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

@Composable
private fun LegendItem(color: Color, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color),
        )
        Hint(text)
    }
}
