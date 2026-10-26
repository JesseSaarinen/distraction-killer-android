package io.github.jessesaarinen.distractionkiller.detection

import io.github.jessesaarinen.distractionkiller.Config
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/** The 60 s swipe rule, the two levels and the long break (task N6). */
class ScrollTimerTest {

    @Before
    fun startFromZero() {
        ScrollTimer.reset()
    }

    private fun swipeAt(second: Int) = ScrollTimer.onSwipe(START + second * 1000L)

    private fun tickAt(second: Int, inFront: Boolean = true, level1: Int = LEVEL1, level2: Int = LEVEL2): Int? =
        ScrollTimer.tick(START + second * 1000L, inFront, level1Seconds = level1, level2DelaySeconds = level2)

    private val seconds get() = ScrollTimer.state.value.activeSeconds
    private val phase get() = ScrollTimer.state.value.phase

    @Test
    fun nothingCountsBeforeTheFirstSwipe() {
        for (s in 1..5) assertNull(tickAt(s))
        assertEquals(0, seconds)
    }

    @Test
    fun countsOnlyWhileShortVideosAreInFront() {
        swipeAt(0)
        tickAt(1)
        tickAt(2, inFront = false)
        tickAt(3)
        assertEquals(2, seconds)
    }

    @Test
    fun stopsCountingWhenTheLastSwipeIsOlderThanTheWindow() {
        swipeAt(0)
        val window = Config.SWIPE_WINDOW_SECONDS
        for (s in 1..window + 5) tickAt(s, level1 = 1000)
        // Seconds 1 to 60 count, 61 and later do not.
        assertEquals(window, seconds)
        swipeAt(window + 5)
        tickAt(window + 6, level1 = 1000)
        assertEquals(window + 1, seconds)
    }

    @Test
    fun level1ComesAfterItsTime() {
        swipeAt(0)
        for (s in 1 until LEVEL1) assertNull(tickAt(s))
        assertEquals(1, tickAt(LEVEL1))
        assertEquals(TimerPhase.AFTER_LEVEL1, phase)
        assertEquals(LEVEL1, seconds)
    }

    @Test
    fun level2NeedsNewSwipingAfterLevel1() {
        reachLevel1()
        // The banner is up and the user only watches: nothing counts.
        for (s in LEVEL1 + 1..LEVEL1 + 20) assertNull(tickAt(s))
        assertEquals(LEVEL1, seconds)

        // The user swipes on: level 2 comes after LEVEL2 more active seconds.
        val restart = LEVEL1 + 20
        swipeAt(restart)
        for (s in restart + 1 until restart + LEVEL2) assertNull(tickAt(s))
        assertEquals(2, tickAt(restart + LEVEL2))
        assertEquals(TimerPhase.PAUSED, phase)
    }

    @Test
    fun pauseWaitsForReset() {
        reachLevel2()
        val after = LEVEL1 + LEVEL2
        swipeAt(after)
        for (s in after + 1..after + 30) assertNull(tickAt(s))
        assertEquals(TimerPhase.PAUSED, phase)

        ScrollTimer.reset()
        assertEquals(0, seconds)
        assertEquals(TimerPhase.COUNTING, phase)
    }

    @Test
    fun longBreakStartsOver() {
        swipeAt(0)
        for (s in 1..5) tickAt(s)
        assertEquals(5, seconds)

        val back = 5 + Config.SESSION_BREAK_SECONDS
        assertNull(tickAt(back))
        assertEquals(0, seconds)
        // The swipe from before the break is too old to count.
        assertNull(tickAt(back + 1))
        assertEquals(0, seconds)
    }

    @Test
    fun swipeAfterALongBreakCountsRightAway() {
        swipeAt(0)
        for (s in 1..5) tickAt(s)

        val back = 5 + Config.SESSION_BREAK_SECONDS
        swipeAt(back)
        tickAt(back)
        assertEquals(0, seconds)
        tickAt(back + 1)
        assertEquals(1, seconds)
    }

    @Test
    fun longBreakEndsAPauseThatNeverGotReset() {
        // For example if level 2 could not open its screen.
        reachLevel2()
        val back = LEVEL1 + LEVEL2 + Config.SESSION_BREAK_SECONDS
        tickAt(back)
        assertEquals(TimerPhase.COUNTING, phase)
        assertEquals(0, seconds)
    }

    // Swipes once a second, so the swipe window never runs out.
    private fun reachLevel1() {
        for (s in 0 until LEVEL1) {
            swipeAt(s)
            tickAt(s + 1)
        }
        assertEquals(TimerPhase.AFTER_LEVEL1, phase)
    }

    private fun reachLevel2() {
        reachLevel1()
        for (s in LEVEL1 until LEVEL1 + LEVEL2) {
            swipeAt(s)
            tickAt(s + 1)
        }
        assertEquals(TimerPhase.PAUSED, phase)
    }

    private companion object {
        const val START = 1_761_508_800_000L
        const val LEVEL1 = 15
        const val LEVEL2 = 10
    }
}
