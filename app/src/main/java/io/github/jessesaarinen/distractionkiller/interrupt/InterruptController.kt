package io.github.jessesaarinen.distractionkiller.interrupt

import android.accessibilityservice.AccessibilityService
import android.content.Intent
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
        val settings = SettingsStore.settings.value
        EventLog.add(
            LogEvent(
                type = "level1",
                app = AppMarkers.label(packageName),
                scrollSeconds = scrollSeconds,
                tester = settings.tester,
            )
        )
        val shown = banner.show {
            Level1Banner(
                scrollSeconds = scrollSeconds,
                onStop = { finishLevel1("stopped") },
                onContinue = { finishLevel1("continued") },
            )
        }
        // Without the banner there is no button to stop the alarm, so no alarm
        // either. Level 2 still comes if the scrolling goes on.
        if (!shown) return
        bannerApp = packageName
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
        val settings = SettingsStore.settings.value
        alarm.notifyLevel2(sound = settings.sound, vibration = settings.vibration)
        // Like pickTask() in the web demo: one of the chosen tasks at random.
        val task = settings.tasks.ifEmpty { Config.DEFAULT_TASKS }.random()
        EventLog.add(
            LogEvent(
                type = "level2",
                app = AppMarkers.label(packageName),
                scrollSeconds = scrollSeconds,
                task = task.id,
                tester = settings.tester,
            )
        )
        openCalmScreen(CalmActivity.level2Intent(service, packageName, task, scrollSeconds))
        hideBanner()
    }

    fun release() {
        alarm.release()
        banner.hide()
        bannerApp = null
    }

    private fun finishLevel1(outcome: String) {
        val packageName = bannerApp ?: return
        val seconds = ScrollTimer.state.value.activeSeconds
        if (outcome == "stopped") {
            // "Lopeta tähän" leaves the video app for the thank-you screen.
            ScrollTimer.reset()
            openCalmScreen(CalmActivity.doneIntent(service, packageName, seconds))
        }
        hideBanner()
        EventLog.add(
            LogEvent(
                type = "outcome",
                app = AppMarkers.label(packageName),
                scrollSeconds = seconds,
                level = 1,
                outcome = outcome,
                tester = SettingsStore.settings.value.tester,
            )
        )
        InspectorLog.add(
            InspectorEntry(System.currentTimeMillis(), packageName, EventKind.OUTCOME, detail = "level1 $outcome")
        )
    }

    /**
     * Opens CalmActivity. Called while the banner is still up, so the video
     * does not show on its own for a moment in between.
     */
    private fun openCalmScreen(intent: Intent) {
        try {
            service.startActivity(intent)
        } catch (e: RuntimeException) {
            InspectorLog.error("calm screen", e)
        }
    }

    private fun hideBanner() {
        bannerApp = null
        alarm.stop()
        banner.hide()
    }
}
