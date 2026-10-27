package io.github.jessesaarinen.distractionkiller.data

import io.github.jessesaarinen.distractionkiller.Config
import io.github.jessesaarinen.distractionkiller.TaskType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

data class TaskStats(val task: TaskType, val total: Int, val stopped: Int)

data class AppStats(val app: String, val rounds: Int, val stopped: Int)

/** One day in the stats chart (N16): its interruptions, and how many of them ended the scrolling. */
data class DayStats(val date: LocalDate, val rounds: Int, val stopped: Int)

/** The numbers on the stats screen. */
data class Summary(
    val rounds: Int,
    val stopped: Int,
    val stoppedAtLevel1: Int,
    val stoppedAtLevel2: Int,
    val continued: Int,
    val skipped: Int,
    val dismissed: Int,
    val stopRate: Int,
    val avgScrollBeforeStop: Int,
    val answers: List<String>,
    val byTask: List<TaskStats>,
    val byApp: List<AppStats>,
    val speed: DecisionSpeed,
)

/**
 * How quickly the user chose (task N19), for the RT8 question "is the
 * friction the right size". Times are medians in milliseconds, null when
 * nobody made that choice yet.
 *
 * - continueMillis, stopMillis: from the banner to Jatka or Lopeta tähän
 * - level2Breaks: how many level 2 breaks there were
 * - quickSkips: how often Ohita was tapped within Config.QUICK_SKIP_SECONDS
 *   after it appeared
 */
data class DecisionSpeed(
    val continueMillis: Long?,
    val stopMillis: Long?,
    val level2Breaks: Int,
    val quickSkips: Int,
)

/**
 * Counts the numbers from the log, like summarize() in the web demo's
 * stats.js. One "round" starts at every level 1 interruption.
 */
fun summarize(events: List<LogEvent>): Summary {
    val level1Events = events.filter { it.type == "level1" }
    val outcomes = events.filter { it.type == "outcome" }
    val stops = outcomes.filter { it.outcome == "stopped" }
    val level2Outcomes = outcomes.filter { it.level == 2 }
    val rounds = level1Events.size

    return Summary(
        rounds = rounds,
        stopped = stops.size,
        stoppedAtLevel1 = stops.count { it.level == 1 },
        stoppedAtLevel2 = stops.count { it.level == 2 },
        continued = level2Outcomes.count { it.outcome == "continued" },
        skipped = level2Outcomes.count { it.outcome == "skipped" },
        dismissed = outcomes.count { it.outcome == "dismissed" },
        stopRate = if (rounds > 0) (stops.size * 100.0 / rounds).roundToInt().coerceAtMost(100) else 0,
        avgScrollBeforeStop = if (stops.isNotEmpty()) stops.map { it.scrollSeconds }.average().roundToInt() else 0,
        answers = outcomes.mapNotNull { it.answer },
        byTask = TaskType.entries.map { task ->
            val own = level2Outcomes.filter { it.task == task.id }
            TaskStats(task, total = own.size, stopped = own.count { it.outcome == "stopped" })
        },
        byApp = level1Events.map { it.app }.distinct().map { app ->
            AppStats(app, rounds = level1Events.count { it.app == app }, stopped = stops.count { it.app == app })
        },
        speed = decisionSpeed(events),
    )
}

/**
 * Pairs each choice with the interruption before it: a level 1 outcome with
 * the level1 row, a level 2 outcome with the level2 row. The log is in time order.
 */
fun decisionSpeed(events: List<LogEvent>): DecisionSpeed {
    val continueTimes = mutableListOf<Long>()
    val stopTimes = mutableListOf<Long>()
    // Ohita appears after SKIP_DELAY_SECONDS, so "right away" is up to this long after the break opened.
    val quickSkipLimit = (Config.SKIP_DELAY_SECONDS + Config.QUICK_SKIP_SECONDS) * 1000L
    var level1At: Long? = null
    var level2At: Long? = null
    var level2Breaks = 0
    var quickSkips = 0
    for (event in events) {
        when (event.type) {
            "level1" -> {
                level1At = event.ts
                level2At = null
            }
            "level2" -> {
                level2At = event.ts
                level2Breaks++
            }
            "outcome" -> {
                val shownAt = if (event.level == 1) level1At else level2At
                if (shownAt != null) {
                    val millis = event.ts - shownAt
                    when {
                        event.level == 1 && event.outcome == "continued" -> continueTimes += millis
                        event.level == 1 && event.outcome == "stopped" -> stopTimes += millis
                        event.level == 2 && event.outcome == "skipped" && millis <= quickSkipLimit -> quickSkips++
                    }
                }
            }
        }
    }
    return DecisionSpeed(
        continueMillis = median(continueTimes),
        stopMillis = median(stopTimes),
        level2Breaks = level2Breaks,
        quickSkips = quickSkips,
    )
}

/** The middle value, so one long pause (phone left on the table) does not skew it. */
private fun median(values: List<Long>): Long? {
    if (values.isEmpty()) return null
    val sorted = values.sorted()
    val middle = sorted.size / 2
    return if (sorted.size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2
}

/**
 * The number for the next RT8 tester (task N19): one more than any tester so
 * far, so the numbers in the log never repeat.
 */
fun nextTester(events: List<LogEvent>, current: Int): Int =
    maxOf(current, events.maxOfOrNull { it.tester } ?: 0) + 1

/**
 * The last [days] days up to [today] for the stats chart (N16), oldest first.
 * A day changes at midnight in [zone], and a day without interruptions has zeros.
 */
fun lastDays(events: List<LogEvent>, today: LocalDate, zone: ZoneId, days: Int = 7): List<DayStats> {
    val byDate = events.groupBy { Instant.ofEpochMilli(it.ts).atZone(zone).toLocalDate() }
    return (days - 1 downTo 0).map { daysAgo ->
        val date = today.minusDays(daysAgo.toLong())
        val sameDay = byDate[date].orEmpty()
        DayStats(
            date = date,
            rounds = sameDay.count { it.type == "level1" },
            stopped = sameDay.count { it.type == "outcome" && it.outcome == "stopped" },
        )
    }
}
