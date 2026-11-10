package io.github.jessesaarinen.distractionkiller.detection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Which app is in front, from Android's usage events (task N21). */
class ForegroundTest {

    private fun resumed(second: Int, app: String) = AppEvent(second * 1000L, app, AppEventType.RESUMED)
    private fun paused(second: Int, app: String) = AppEvent(second * 1000L, app, AppEventType.PAUSED)
    private fun screenOff(second: Int) = AppEvent(second * 1000L, "android", AppEventType.SCREEN_OFF)

    @Test
    fun theLastResumedAppIsInFront() {
        val tracker = ForegroundTracker()
        listOf(resumed(1, TIKTOK), paused(5, TIKTOK), resumed(5, LAUNCHER)).forEach(tracker::onEvent)
        assertEquals(LAUNCHER, tracker.app)
    }

    @Test
    fun aLatePauseOfAnotherAppChangesNothing() {
        val tracker = ForegroundTracker()
        listOf(resumed(1, TIKTOK), resumed(2, LAUNCHER), paused(2, TIKTOK)).forEach(tracker::onEvent)
        assertEquals(LAUNCHER, tracker.app)
    }

    @Test
    fun screenOffMeansNoAppInFront() {
        val tracker = ForegroundTracker()
        listOf(resumed(1, TIKTOK), screenOff(3)).forEach(tracker::onEvent)
        assertNull(tracker.app)
    }

    @Test
    fun secondsInFrontAddUpEachStretch() {
        val events = listOf(
            resumed(0, TIKTOK), paused(10, TIKTOK), resumed(10, LAUNCHER),
            paused(15, LAUNCHER), resumed(15, TIKTOK), screenOff(20),
            resumed(100, TIKTOK),
        )
        assertEquals(mapOf(TIKTOK to 25, LAUNCHER to 5), secondsInFront(events, 0L, 110_000L))
    }

    @Test
    fun onlyTimeInsideTheRangeCounts() {
        // In front since before midnight (here second 50) and still in front at second 80.
        val events = listOf(resumed(0, TIKTOK))
        assertEquals(mapOf(TIKTOK to 30), secondsInFront(events, 50_000L, 80_000L))
    }

    @Test
    fun theSameAppResumedAgainIsNotCountedTwice() {
        // Another activity of the same app comes to the front.
        val events = listOf(resumed(0, TIKTOK), resumed(10, TIKTOK), paused(20, TIKTOK))
        assertEquals(mapOf(TIKTOK to 20), secondsInFront(events, 0L, 60_000L))
    }

    private companion object {
        const val TIKTOK = "com.zhiliaoapp.musically"
        const val LAUNCHER = "com.android.launcher"
    }
}
