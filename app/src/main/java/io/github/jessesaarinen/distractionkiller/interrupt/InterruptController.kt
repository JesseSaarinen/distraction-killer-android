package io.github.jessesaarinen.distractionkiller.interrupt

import android.accessibilityservice.AccessibilityService
import io.github.jessesaarinen.distractionkiller.Config
import io.github.jessesaarinen.distractionkiller.data.EventLog
import io.github.jessesaarinen.distractionkiller.data.LogEvent
import io.github.jessesaarinen.distractionkiller.data.SettingsStore
import io.github.jessesaarinen.distractionkiller.detection.AppMarkers
import io.github.jessesaarinen.distractionkiller.detection.EventKind
import io.github.jessesaarinen.distractionkiller.detection.InspectorEntry
import io.github.jessesaarinen.distractionkiller.detection.InspectorLog
import io.github.jessesaarinen.distractionkiller.detection.ScrollTimer

/**
 * Shows the interruptions on top of the watched app and acts on the user's
 * choice, like the feed session code in the web demo's app.js.
 *
 * Every interruption and choice goes to the event log, and to the inspector
 * for debugging.
 */
class InterruptController(private val service: AccessibilityService) {
    private val banner = OverlayWindow(service, fullScreen = false)
    private val alarm = Alarm(service)

    /** The app the banner is shown on, or null when there is no banner. */
    var bannerApp: String? = null
        private set

    /** Level 1: banner + alarm. */
    fun showLevel1(packageName: String, scrollSeconds: Int) {
        EventLog.add(LogEvent(type = "level1", app = AppMarkers.label(packageName), scrollSeconds = scrollSeconds))
        bannerApp = packageName
        banner.show {
            Level1Banner(
                scrollSeconds = scrollSeconds,
                onStop = { finishLevel1("stopped") },
                onContinue = { finishLevel1("continued") },
            )
        }
        val settings = SettingsStore.settings.value
        alarm.start(sound = settings.sound, vibration = settings.vibration, mode = settings.alarmMode)
    }

    /** Hides the banner without a choice: the user left the app. */
    fun dismissLevel1() {
        if (bannerApp != null) finishLevel1("dismissed")
    }

    /**
     * Level 2: the calm full screen with one random task. It replaces the
     * banner, and like in the web demo level 1 then gets no outcome.
     * CalmActivity resets the timer when the break ends.
     */
    fun showLevel2(packageName: String, scrollSeconds: Int) {
        hideBanner()
        val settings = SettingsStore.settings.value
        alarm.notifyLevel2(sound = settings.sound, vibration = settings.vibration)
        // Like pickTask() in the web demo: one of the chosen tasks at random.
        val task = settings.tasks.ifEmpty { Config.DEFAULT_TASKS }.random()
        EventLog.add(
            LogEvent(type = "level2", app = AppMarkers.label(packageName), scrollSeconds = scrollSeconds, task = task.id)
        )
        service.startActivity(CalmActivity.level2Intent(service, packageName, task, scrollSeconds))
    }

    fun release() {
        alarm.release()
        banner.hide()
        bannerApp = null
    }

    private fun finishLevel1(outcome: String) {
        val packageName = bannerApp ?: return
        val seconds = ScrollTimer.state.value.activeSeconds
        hideBanner()
        EventLog.add(
            LogEvent(
                type = "outcome",
                app = AppMarkers.label(packageName),
                scrollSeconds = seconds,
                level = 1,
                outcome = outcome,
            )
        )
        if (outcome == "stopped") {
            // "Lopeta tähän" leaves the video app for the thank-you screen.
            ScrollTimer.reset()
            service.startActivity(CalmActivity.doneIntent(service, packageName, seconds))
        }
        InspectorLog.add(
            InspectorEntry(System.currentTimeMillis(), packageName, EventKind.OUTCOME, detail = "level1 $outcome")
        )
    }

    private fun hideBanner() {
        bannerApp = null
        alarm.stop()
        banner.hide()
    }
}
