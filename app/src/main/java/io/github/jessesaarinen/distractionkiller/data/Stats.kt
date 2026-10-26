package io.github.jessesaarinen.distractionkiller.data

import io.github.jessesaarinen.distractionkiller.TaskType
import kotlin.math.roundToInt

data class TaskStats(val task: TaskType, val total: Int, val stopped: Int)

data class AppStats(val app: String, val rounds: Int, val stopped: Int)

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
    )
}
