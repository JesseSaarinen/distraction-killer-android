package io.github.jessesaarinen.distractionkiller.detection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/** Each app's daily time, and when the reminders come after it (task N21). */
class UsageTimerTest {

    @Before
    fun startFresh() {
        UsageTimer.seed(DAY + 100, emptyMap())
        UsageTimer.reset(0L)
    }

    /** Ticks the app for this many seconds and returns the seconds (1-based) where a level came. */
    private fun use(seconds: Int, app: String = TIKTOK, allowance: Int = 10, day: Long = DAY): Map<Int, Int> {
        val levels = mutableMapOf<Int, Int>()
        for (second in 1..seconds) {
            UsageTimer.tick(day, app, allowance, INTERVAL, LEVEL2_DELAY)?.let { levels[second] = it }
        }
        return levels
    }

    @Test
    fun nothingWhileTheDailyTimeLasts() {
        assertEquals(emptyMap<Int, Int>(), use(9))
        assertEquals(9, UsageTimer.state.value.usage(TIKTOK).usedSeconds)
    }

    @Test
    fun level1RightWhenTheTimeIsUsedThenLevel2ThenEveryInterval() {
        // Level 1 at 10 s, level 2 five seconds later, then level 1 every 20 s and level 2 after each.
        assertEquals(mapOf(10 to 1, 15 to 2, 35 to 1, 40 to 2, 60 to 1), use(60))
    }

    @Test
    fun zeroDailyTimeRemindsOnTheFirstSecond() {
        assertEquals(mapOf(1 to 1), use(1, allowance = 0))
    }

    @Test
    fun stoppingAtLevel1SkipsLevel2() {
        use(10)
        UsageTimer.stoppedAtLevel1(TIKTOK)
        // The next level 1 one interval later, no level 2 in between.
        assertEquals(mapOf(20 to 1), use(20))
    }

    @Test
    fun appsHaveTheirOwnTime() {
        use(9, app = TIKTOK)
        assertEquals(emptyMap<Int, Int>(), use(9, app = READER))
        assertEquals(mapOf(1 to 1), use(1, app = TIKTOK))
    }

    @Test
    fun aNewDayStartsFromZero() {
        use(30)
        assertEquals(emptyMap<Int, Int>(), use(9, day = DAY + 1))
        assertEquals(9, UsageTimer.state.value.usage(TIKTOK).usedSeconds)
    }

    @Test
    fun moreDailyTimeStopsTheReminders() {
        use(12)
        assertEquals(emptyMap<Int, Int>(), use(10, allowance = 60))
        assertEquals(UsagePhase.ALLOWANCE, UsageTimer.state.value.usage(TIKTOK).phase)
    }

    @Test
    fun seededTimeCountsAndAnAppOverItsTimeGetsLevel1() {
        UsageTimer.seed(DAY, mapOf(TIKTOK to 600))
        assertEquals(mapOf(1 to 1), use(1, allowance = 300))
    }

    @Test
    fun seedingAgainKeepsThePhaseAndNeverLowersTheTime() {
        use(12)
        UsageTimer.seed(DAY, mapOf(TIKTOK to 5, READER to 7))
        val tiktok = UsageTimer.state.value.usage(TIKTOK)
        assertEquals(12, tiktok.usedSeconds)
        assertEquals(UsagePhase.AFTER_LEVEL1, tiktok.phase)
        assertEquals(7, UsageTimer.state.value.usage(READER).usedSeconds)
    }

    @Test
    fun resetClearsTodayAndRemembersWhen() {
        use(12)
        UsageTimer.reset(1234L)
        assertEquals(0, UsageTimer.state.value.usage(TIKTOK).usedSeconds)
        assertEquals(1234L, UsageTimer.countFromMillis)
        assertNull(UsageTimer.tick(DAY, TIKTOK, 10, INTERVAL, LEVEL2_DELAY))
    }

    private companion object {
        const val DAY = 20_000L
        const val TIKTOK = "com.zhiliaoapp.musically"
        const val READER = "com.example.reader"
        const val INTERVAL = 20
        const val LEVEL2_DELAY = 5
    }
}
