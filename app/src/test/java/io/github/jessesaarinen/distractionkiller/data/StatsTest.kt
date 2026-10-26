package io.github.jessesaarinen.distractionkiller.data

import io.github.jessesaarinen.distractionkiller.TaskType
import org.junit.Assert.assertEquals
import org.junit.Test

/** The numbers on the stats screen (task N11). */
class StatsTest {

    @Test
    fun emptyLogGivesZeros() {
        val summary = summarize(emptyList())
        assertEquals(0, summary.rounds)
        assertEquals(0, summary.stopRate)
        assertEquals(0, summary.avgScrollBeforeStop)
        assertEquals(emptyList<AppStats>(), summary.byApp)
    }

    @Test
    fun countsRoundsOutcomesTasksAndApps() {
        val events = listOf(
            // TikTok: stopped at the banner.
            LogEvent(type = "level1", app = "TikTok", scrollSeconds = 60),
            LogEvent(type = "outcome", app = "TikTok", scrollSeconds = 62, level = 1, outcome = "stopped"),
            // Instagram: went on, stopped at the breathing task.
            LogEvent(type = "level1", app = "Instagram", scrollSeconds = 60),
            LogEvent(type = "outcome", app = "Instagram", scrollSeconds = 61, level = 1, outcome = "continued"),
            LogEvent(type = "level2", app = "Instagram", scrollSeconds = 90, task = "breathing"),
            LogEvent(type = "outcome", app = "Instagram", scrollSeconds = 96, level = 2, task = "breathing", outcome = "stopped"),
            // YouTube: left the app while the banner was up.
            LogEvent(type = "level1", app = "YouTube", scrollSeconds = 60),
            LogEvent(type = "outcome", app = "YouTube", scrollSeconds = 60, level = 1, outcome = "dismissed"),
            // TikTok again: answered the question and went on.
            LogEvent(type = "level1", app = "TikTok", scrollSeconds = 60),
            LogEvent(type = "level2", app = "TikTok", scrollSeconds = 90, task = "question"),
            LogEvent(
                type = "outcome", app = "TikTok", scrollSeconds = 95, level = 2, task = "question",
                outcome = "continued", answer = "Tylsistyin",
            ),
        )
        val summary = summarize(events)

        assertEquals(4, summary.rounds)
        assertEquals(2, summary.stopped)
        assertEquals(1, summary.stoppedAtLevel1)
        assertEquals(1, summary.stoppedAtLevel2)
        assertEquals(1, summary.continued)
        assertEquals(0, summary.skipped)
        assertEquals(1, summary.dismissed)
        assertEquals(50, summary.stopRate)
        assertEquals(79, summary.avgScrollBeforeStop)
        assertEquals(listOf("Tylsistyin"), summary.answers)
        assertEquals(
            listOf(
                TaskStats(TaskType.BREATHING, total = 1, stopped = 1),
                TaskStats(TaskType.QUESTION, total = 1, stopped = 0),
                TaskStats(TaskType.PHYSICAL, total = 0, stopped = 0),
            ),
            summary.byTask,
        )
        assertEquals(
            listOf(
                AppStats("TikTok", rounds = 2, stopped = 1),
                AppStats("Instagram", rounds = 1, stopped = 1),
                AppStats("YouTube", rounds = 1, stopped = 0),
            ),
            summary.byApp,
        )
    }

    @Test
    fun stopRateIsAtMostHundred() {
        val events = listOf(
            LogEvent(type = "level1", app = "TikTok", scrollSeconds = 60),
            LogEvent(type = "outcome", app = "TikTok", scrollSeconds = 60, level = 1, outcome = "stopped"),
            LogEvent(type = "outcome", app = "TikTok", scrollSeconds = 70, level = 2, outcome = "stopped"),
        )
        assertEquals(100, summarize(events).stopRate)
    }
}
