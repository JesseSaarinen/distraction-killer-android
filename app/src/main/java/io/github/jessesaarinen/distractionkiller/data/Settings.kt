package io.github.jessesaarinen.distractionkiller.data

import io.github.jessesaarinen.distractionkiller.AlarmMode
import io.github.jessesaarinen.distractionkiller.Config
import io.github.jessesaarinen.distractionkiller.TaskType
import io.github.jessesaarinen.distractionkiller.detection.AppMarkers

/**
 * The user's choices on the settings screen, like DEFAULT_SETTINGS in the web
 * demo. watchedApps holds app names from AppMarkers.labels ("TikTok", ...).
 */
data class Settings(
    val intervalMin: Int = Config.DEFAULT_INTERVAL_MIN,
    val tasks: Set<TaskType> = Config.DEFAULT_TASKS,
    val alarmMode: AlarmMode = Config.DEFAULT_ALARM_MODE,
    val sound: Boolean = Config.DEFAULT_SOUND,
    val vibration: Boolean = Config.DEFAULT_VIBRATION,
    val demoMode: Boolean = Config.DEFAULT_DEMO_MODE,
    val watchedApps: Set<String> = AppMarkers.labels.toSet(),
) {
    /** Active scrolling before level 1. Demo mode uses seconds instead of minutes. */
    val level1Seconds: Int
        get() = if (demoMode) Config.DEMO_LEVEL1_SECONDS else intervalMin * 60

    /** More active scrolling after level 1 before level 2. */
    val level2DelaySeconds: Int
        get() = if (demoMode) Config.DEMO_LEVEL2_DELAY_SECONDS else Config.LEVEL2_DELAY_SECONDS
}
