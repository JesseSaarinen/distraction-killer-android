package io.github.jessesaarinen.distractionkiller.detection

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Where an app is in its day. */
enum class UsagePhase {
    /** The daily time is not used up yet. No reminders. */
    ALLOWANCE,

    /** Level 1 was shown. Counting toward level 2. */
    AFTER_LEVEL1,

    /** Level 2 was shown or the user stopped. Counting toward the next level 1. */
    BETWEEN_REMINDERS,
}

/** One app's day: seconds in front today, and the seconds counted in the current phase. */
data class AppUsage(
    val usedSeconds: Int = 0,
    val phase: UsagePhase = UsagePhase.ALLOWANCE,
    val phaseSeconds: Int = 0,
)

/** Every watched app's day. day is the date as days since 1970 (LocalDate.toEpochDay). */
data class UsageState(val day: Long = 0L, val apps: Map<String, AppUsage> = emptyMap()) {
    fun usage(packageName: String): AppUsage = apps[packageName] ?: AppUsage()
}

/**
 * Counts each watched app's time in front, one second per tick, and says when
 * a reminder is due. Every app has its own daily time (allowance). When it is
 * used up, level 1 comes right away. If the user stays in the app, level 2
 * comes level2DelaySeconds later. After level 2, or after the user stops on
 * level 1, level 1 comes again every intervalSeconds of more use. At midnight
 * every app starts the new day from zero.
 *
 * Times are passed in, so the logic does not depend on the real clock.
 */
object UsageTimer {
    private val _state = MutableStateFlow(UsageState())
    val state: StateFlow<UsageState> = _state.asStateFlow()

    /**
     * Sets today's used seconds from Android's usage stats, so the daily time
     * stays right when the service starts again in the middle of the day.
     * An app that is over its time gets level 1 on its next second in front.
     */
    fun seed(day: Long, usedSeconds: Map<String, Int>) {
        // Seeding again on the same day (after a pause, or when an app was added)
        // keeps where each app is, and never lowers the count.
        val current = _state.value.let { if (it.day == day) it else UsageState(day) }
        val merged = current.apps.toMutableMap()
        for ((packageName, seconds) in usedSeconds) {
            val app = merged[packageName] ?: AppUsage()
            merged[packageName] = app.copy(usedSeconds = maxOf(app.usedSeconds, seconds))
        }
        _state.value = current.copy(apps = merged)
    }

    /**
     * Call once a second while the app is in front and the screen is on.
     * Returns 1 or 2 when that level is due on this tick, otherwise null.
     */
    fun tick(day: Long, packageName: String, allowanceSeconds: Int, intervalSeconds: Int, level2DelaySeconds: Int): Int? {
        // A new day starts every app from zero.
        val current = _state.value.let { if (it.day == day) it else UsageState(day) }
        val app = current.usage(packageName)
        val used = app.usedSeconds + 1
        var level: Int? = null
        val next = when {
            // The user gave the app more time, or this is the first second over it.
            used < allowanceSeconds -> AppUsage(used)
            app.phase == UsagePhase.ALLOWANCE -> {
                level = 1
                AppUsage(used, UsagePhase.AFTER_LEVEL1, 0)
            }
            app.phase == UsagePhase.AFTER_LEVEL1 && app.phaseSeconds + 1 >= level2DelaySeconds -> {
                level = 2
                AppUsage(used, UsagePhase.BETWEEN_REMINDERS, 0)
            }
            app.phase == UsagePhase.BETWEEN_REMINDERS && app.phaseSeconds + 1 >= intervalSeconds -> {
                level = 1
                AppUsage(used, UsagePhase.AFTER_LEVEL1, 0)
            }
            else -> app.copy(usedSeconds = used, phaseSeconds = app.phaseSeconds + 1)
        }
        _state.value = current.copy(apps = current.apps + (packageName to next))
        return level
    }

    /** The user stopped on level 1: no level 2, the next reminder after the interval. */
    fun stoppedAtLevel1(packageName: String) {
        val current = _state.value
        val app = current.apps[packageName] ?: return
        if (app.phase != UsagePhase.AFTER_LEVEL1) return
        val next = app.copy(phase = UsagePhase.BETWEEN_REMINDERS, phaseSeconds = 0)
        _state.value = current.copy(apps = current.apps + (packageName to next))
    }

    /**
     * Time before this does not count toward today. Set by reset(), so seeding
     * from Android's usage stats does not bring back the time that was cleared.
     */
    var countFromMillis: Long = 0L
        private set

    /** Every app back to zero for today: a new tester, or the inspector's button. */
    fun reset(nowMillis: Long) {
        countFromMillis = nowMillis
        _state.value = UsageState(_state.value.day)
    }
}
