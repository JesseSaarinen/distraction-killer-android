package io.github.jessesaarinen.distractionkiller.interrupt

import android.accessibilityservice.AccessibilityService
import io.github.jessesaarinen.distractionkiller.Config
import io.github.jessesaarinen.distractionkiller.detection.EventKind
import io.github.jessesaarinen.distractionkiller.detection.InspectorEntry
import io.github.jessesaarinen.distractionkiller.detection.InspectorLog
import io.github.jessesaarinen.distractionkiller.detection.ScrollTimer

/**
 * Shows the interruptions on top of the watched app and acts on the user's
 * choice, like the feed session code in the web demo's app.js.
 *
 * Choices go to the inspector log for now; the real event log comes in N11.
 */
class InterruptController(private val service: AccessibilityService) {
    private val banner = OverlayWindow(service, fullScreen = false)
    private val alarm = Alarm(service)

    /** The app the banner is shown on, or null when there is no banner. */
    var bannerApp: String? = null
        private set

    /** Level 1: banner + alarm. */
    fun showLevel1(packageName: String, scrollSeconds: Int) {
        bannerApp = packageName
        banner.show {
            Level1Banner(
                scrollSeconds = scrollSeconds,
                onStop = { finishLevel1("stopped") },
                onContinue = { finishLevel1("continued") },
            )
        }
        // Settings come in N10. Until then the defaults from Config.
        alarm.start(
            sound = Config.DEFAULT_SOUND,
            vibration = Config.DEFAULT_VIBRATION,
            mode = Config.DEFAULT_ALARM_MODE,
        )
    }

    /** Hides the banner without a choice: the user left the app, or level 2 replaced it. */
    fun dismissLevel1() {
        if (bannerApp != null) finishLevel1("dismissed")
    }

    fun release() {
        alarm.release()
        banner.hide()
        bannerApp = null
    }

    private fun finishLevel1(outcome: String) {
        val packageName = bannerApp ?: return
        bannerApp = null
        alarm.stop()
        banner.hide()
        if (outcome == "stopped") {
            // "Lopeta tähän" really leaves the video app.
            ScrollTimer.reset()
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
        }
        InspectorLog.add(
            InspectorEntry(System.currentTimeMillis(), packageName, EventKind.OUTCOME, detail = "level1 $outcome")
        )
    }
}
