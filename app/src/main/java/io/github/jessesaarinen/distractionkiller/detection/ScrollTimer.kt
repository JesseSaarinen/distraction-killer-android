package io.github.jessesaarinen.distractionkiller.detection

import io.github.jessesaarinen.distractionkiller.Config
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Where the timer is in the interruption flow. */
enum class TimerPhase {
    /** Counting toward level 1. */
    COUNTING,

    /** Level 1 was shown. Counting toward level 2. */
    AFTER_LEVEL1,

    /** Level 2 was shown. Waits for reset(). */
    PAUSED,
}

data class TimerState(
    val activeSeconds: Int = 0,
    val phase: TimerPhase = TimerPhase.COUNTING,
    val secondsAfterLevel1: Int = 0,
)

/**
 * Counts active short-video seconds, like scrollTimer.js in the web demo.
 *
 * A second is active when a watched short-video screen is in front, the screen
 * is on, and the user swiped within the last Config.SWIPE_WINDOW_SECONDS.
 * Level 1 comes after level1Seconds. After level 1 a new swipe is needed, and
 * level 2 comes after level2DelaySeconds more active seconds. Then the timer
 * pauses until reset(). A long break (Config.SESSION_BREAK_SECONDS) starts over.
 *
 * Times are passed in, so the logic does not depend on the real clock.
 */
object ScrollTimer {
    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState> = _state.asStateFlow()

    private var lastSwipeAtMillis: Long? = null
    private var lastActiveAtMillis: Long? = null

    fun onSwipe(nowMillis: Long) {
        lastSwipeAtMillis = nowMillis
    }

    /**
     * Call once a second. Returns 1 or 2 when that level is reached on this tick,
     * otherwise null.
     */
    fun tick(nowMillis: Long, shortVideoInFront: Boolean, level1Seconds: Int, level2DelaySeconds: Int): Int? {
        val current = _state.value

        // A long break starts over. This also ends a pause whose level 2 screen
        // never got to call reset(), so the timer cannot get stuck. A swipe that
        // came with the return is kept, so the new session starts right away.
        val lastActive = lastActiveAtMillis
        if (lastActive != null && nowMillis - lastActive >= Config.SESSION_BREAK_SECONDS * 1000L) {
            val lastSwipe = lastSwipeAtMillis
            reset()
            lastSwipeAtMillis = lastSwipe
            return null
        }
        if (current.phase == TimerPhase.PAUSED) return null

        val lastSwipe = lastSwipeAtMillis
        val recentlySwiped = lastSwipe != null && nowMillis - lastSwipe <= Config.SWIPE_WINDOW_SECONDS * 1000L
        if (!shortVideoInFront || !recentlySwiped) return null

        lastActiveAtMillis = nowMillis
        val seconds = current.activeSeconds + 1
        return when (current.phase) {
            TimerPhase.COUNTING -> {
                if (seconds >= level1Seconds) {
                    _state.value = TimerState(seconds, TimerPhase.AFTER_LEVEL1, 0)
                    // Like the web demo: level 2 needs new swiping after the banner.
                    lastSwipeAtMillis = null
                    1
                } else {
                    _state.value = current.copy(activeSeconds = seconds)
                    null
                }
            }
            TimerPhase.AFTER_LEVEL1 -> {
                val after = current.secondsAfterLevel1 + 1
                if (after >= level2DelaySeconds) {
                    _state.value = TimerState(seconds, TimerPhase.PAUSED, after)
                    2
                } else {
                    _state.value = TimerState(seconds, TimerPhase.AFTER_LEVEL1, after)
                    null
                }
            }
            TimerPhase.PAUSED -> null
        }
    }

    /** Back to zero: after stopping, after level 2, or after a long break. */
    fun reset() {
        _state.value = TimerState()
        lastSwipeAtMillis = null
        lastActiveAtMillis = null
    }
}
