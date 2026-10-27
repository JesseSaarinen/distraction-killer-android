package io.github.jessesaarinen.distractionkiller.data

import io.github.jessesaarinen.distractionkiller.TaskType
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The numbers on the stats screen (tasks N11 and N19) and the chart of the last days (N16). */
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

    @Test
    fun decisionSpeedPairsEachChoiceWithItsInterruption() {
        val events = listOf(
            // Banner at 0 s, Jatka after 1.5 s, then the break: Ohita right after it appeared.
            LogEvent(type = "level1", app = "TikTok", scrollSeconds = 60, ts = 0),
            LogEvent(type = "outcome", app = "TikTok", scrollSeconds = 60, level = 1, outcome = "continued", ts = 1_500),
            LogEvent(type = "level2", app = "TikTok", scrollSeconds = 90, task = "breathing", ts = 30_000),
            LogEvent(type = "outcome", app = "TikTok", scrollSeconds = 90, level = 2, outcome = "skipped", ts = 36_000),
            // Banner, Lopeta after 4 s.
            LogEvent(type = "level1", app = "TikTok", scrollSeconds = 60, ts = 100_000),
            LogEvent(type = "outcome", app = "TikTok", scrollSeconds = 60, level = 1, outcome = "stopped", ts = 104_000),
            // Banner ignored, the break replaced it. Ohita only after 20 s.
            LogEvent(type = "level1", app = "TikTok", scrollSeconds = 60, ts = 200_000),
            LogEvent(type = "level2", app = "TikTok", scrollSeconds = 90, task = "question", ts = 230_000),
            LogEvent(type = "outcome", app = "TikTok", scrollSeconds = 90, level = 2, outcome = "skipped", ts = 250_000),
            // Banner, Jatka after 2.5 s. Left the app while the banner was up does not count.
            LogEvent(type = "level1", app = "TikTok", scrollSeconds = 60, ts = 300_000),
            LogEvent(type = "outcome", app = "TikTok", scrollSeconds = 60, level = 1, outcome = "continued", ts = 302_500),
            LogEvent(type = "level1", app = "TikTok", scrollSeconds = 60, ts = 400_000),
            LogEvent(type = "outcome", app = "TikTok", scrollSeconds = 60, level = 1, outcome = "dismissed", ts = 400_500),
        )
        val speed = summarize(events).speed

        assertEquals(2_000L, speed.continueMillis) // median of 1.5 s and 2.5 s
        assertEquals(4_000L, speed.stopMillis)
        assertEquals(2, speed.level2Breaks)
        assertEquals(1, speed.quickSkips)
    }

    @Test
    fun decisionSpeedIsEmptyWithoutChoices() {
        val speed = summarize(emptyList()).speed
        assertNull(speed.continueMillis)
        assertNull(speed.stopMillis)
        assertEquals(0, speed.level2Breaks)
        assertEquals(0, speed.quickSkips)
    }

    @Test
    fun nextTesterNeverRepeatsANumber() {
        assertEquals(1, nextTester(emptyList(), current = 0))
        val events = listOf(
            LogEvent(type = "level1", app = "TikTok", scrollSeconds = 60, tester = 1),
            LogEvent(type = "level1", app = "TikTok", scrollSeconds = 60, tester = 2),
        )
        assertEquals(3, nextTester(events, current = 2))
        // The test was ended (0), but testers 1 and 2 are still in the log.
        assertEquals(3, nextTester(events, current = 0))
        // The log was cleared during the test: go on from the current tester.
        assertEquals(4, nextTester(emptyList(), current = 3))
    }

    @Test
    fun lastDaysCountsEachDayOldestFirst() {
        val zone = ZoneId.of("Europe/Helsinki")
        val today = LocalDate.of(2026, 11, 5)
        fun at(date: LocalDate, hour: Int, minute: Int = 0) =
            date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
        val events = listOf(
            // Today: two interruptions, one of them stopped.
            LogEvent(type = "level1", app = "TikTok", scrollSeconds = 60, ts = at(today, 9)),
            LogEvent(type = "outcome", app = "TikTok", scrollSeconds = 60, level = 1, outcome = "stopped", ts = at(today, 9)),
            LogEvent(type = "level1", app = "TikTok", scrollSeconds = 60, ts = at(today, 21)),
            LogEvent(type = "outcome", app = "TikTok", scrollSeconds = 70, level = 1, outcome = "continued", ts = at(today, 21)),
            // Two days ago, just after midnight.
            LogEvent(type = "level1", app = "YouTube", scrollSeconds = 60, ts = at(today.minusDays(2), 0, 5)),
            // Eight days ago: too old for the chart.
            LogEvent(type = "level1", app = "Instagram", scrollSeconds = 60, ts = at(today.minusDays(8), 12)),
        )
        val days = lastDays(events, today, zone)

        assertEquals(7, days.size)
        assertEquals(today.minusDays(6), days.first().date)
        assertEquals(DayStats(today, rounds = 2, stopped = 1), days.last())
        assertEquals(DayStats(today.minusDays(2), rounds = 1, stopped = 0), days[4])
        assertEquals(3, days.sumOf { it.rounds })
    }

    @Test
    fun aDayChangesAtLocalMidnight() {
        val helsinki = ZoneId.of("Europe/Helsinki")
        val today = LocalDate.of(2026, 11, 5)
        // 00:30 in Helsinki is still 4.11. in UTC.
        val ts = today.atTime(0, 30).atZone(helsinki).toInstant().toEpochMilli()
        val events = listOf(LogEvent(type = "level1", app = "TikTok", scrollSeconds = 60, ts = ts))

        assertEquals(1, lastDays(events, today, helsinki).last().rounds)
        assertEquals(0, lastDays(events, today, ZoneId.of("UTC")).last().rounds)
    }
}
