package io.github.jessesaarinen.distractionkiller.detection

/** The usage events that matter for which app is in front. */
enum class AppEventType {
    /** An activity of the app came to the front. */
    RESUMED,

    /** An activity of the app left the front. */
    PAUSED,

    /** The screen turned off or locked: no app is in use. */
    SCREEN_OFF,
}

/**
 * One event from Android's usage stats (UsageStatsManager), reduced to what
 * this app needs. UsageWatcherService turns the Android events into these,
 * so the logic below has no Android classes and can be unit tested.
 */
data class AppEvent(val timeMillis: Long, val packageName: String, val type: AppEventType)

/**
 * Follows the events and knows which app is in front. Android sends the old
 * app's PAUSED and the new app's RESUMED, so a PAUSED of another app (one
 * already in the background) changes nothing.
 */
class ForegroundTracker {
    var app: String? = null
        private set

    /** When the app in front came there. */
    var sinceMillis: Long = 0L
        private set

    /** For secondsInFront: the time up to here is counted already. */
    internal fun restartAt(timeMillis: Long) {
        sinceMillis = timeMillis
    }

    fun onEvent(event: AppEvent) {
        when (event.type) {
            AppEventType.RESUMED -> if (event.packageName != app) {
                app = event.packageName
                sinceMillis = event.timeMillis
            }
            AppEventType.PAUSED -> if (event.packageName == app) app = null
            AppEventType.SCREEN_OFF -> app = null
        }
    }
}

/**
 * How many seconds each app was in front between fromMillis and untilMillis,
 * counted from the events in time order. The events should start a little
 * before fromMillis, so the app that was already in front at fromMillis is
 * known. Used to know how much of the daily time is used when the service starts.
 */
fun secondsInFront(events: List<AppEvent>, fromMillis: Long, untilMillis: Long): Map<String, Int> {
    val millis = mutableMapOf<String, Long>()
    val tracker = ForegroundTracker()

    // Adds the time of the app in front, up to endMillis, inside fromMillis..untilMillis.
    fun addUntil(endMillis: Long) {
        val app = tracker.app ?: return
        val start = maxOf(tracker.sinceMillis, fromMillis)
        val end = minOf(endMillis, untilMillis)
        if (end > start) millis[app] = (millis[app] ?: 0L) + (end - start)
    }

    for (event in events) {
        if (event.timeMillis > untilMillis) break
        // Close the current stretch first. If the event changes nothing, the
        // stretch goes on from here and no time is lost or counted twice.
        addUntil(event.timeMillis)
        val before = tracker.app
        tracker.onEvent(event)
        if (tracker.app != null && tracker.app == before) tracker.restartAt(event.timeMillis)
    }
    addUntil(untilMillis)
    return millis.mapValues { (it.value / 1000L).toInt() }
}
