package io.github.jessesaarinen.distractionkiller

import android.app.Application
import io.github.jessesaarinen.distractionkiller.data.EventLog
import io.github.jessesaarinen.distractionkiller.data.SettingsStore
import io.github.jessesaarinen.distractionkiller.detection.InspectorLog

/**
 * Starts before any screen and before the scroll watcher service, and loads
 * the saved settings, event log and inspector log once.
 */
class DistractionKillerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SettingsStore.init(this)
        EventLog.init(this)
        InspectorLog.init(this)
    }
}
