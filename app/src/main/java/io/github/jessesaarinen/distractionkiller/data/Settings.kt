package io.github.jessesaarinen.distractionkiller.data

import io.github.jessesaarinen.distractionkiller.AlarmMode
import io.github.jessesaarinen.distractionkiller.Config
import io.github.jessesaarinen.distractionkiller.TaskType

/**
 * An app whose time is counted, and how many minutes a day it may be used
 * before the reminders start. 0 = reminders from the first second.
 */
data class WatchedApp(val packageName: String, val label: String, val allowanceMin: Int)

/**
 * The user's choices on the settings screen, like DEFAULT_SETTINGS in the web
 * demo. apps are the watched apps, sorted by name.
 *
 * intervalMin: after an app's daily time is used up, how often level 1 comes
 * again, one of Config.INTERVAL_OPTIONS_MIN.
 *
 * level2AfterSec: more seconds in the app after level 1 before level 2, one of
 * Config.LEVEL2_DELAY_OPTIONS_SECONDS.
 *
 * interruptionsOn: false pauses the app. Nothing is counted and nothing interrupts.
 *
 * tester: the RT8 tester whose test is running (task N19). Every event gets
 * this number, so the testers' results stay apart. 0 = no test running.
 */
data class Settings(
    val intervalMin: Int = Config.DEFAULT_INTERVAL_MIN,
    val level2AfterSec: Int = Config.DEFAULT_LEVEL2_DELAY_SECONDS,
    val tasks: Set<TaskType> = Config.DEFAULT_TASKS,
    val alarmMode: AlarmMode = Config.DEFAULT_ALARM_MODE,
    val sound: Boolean = Config.DEFAULT_SOUND,
    val vibration: Boolean = Config.DEFAULT_VIBRATION,
    val demoMode: Boolean = Config.DEFAULT_DEMO_MODE,
    val apps: List<WatchedApp> = Config.DEFAULT_APPS.sortedBy { it.label.lowercase() },
    val interruptionsOn: Boolean = Config.DEFAULT_INTERRUPTIONS_ON,
    val tester: Int = 0,
) {
    /** Time between reminders after the daily time is used up. Demo mode uses seconds instead of minutes. */
    val intervalSeconds: Int
        get() = if (demoMode) Config.DEMO_INTERVAL_SECONDS else intervalMin * 60

    /** More time in the app after level 1 before level 2. Demo mode has its own short time. */
    val level2DelaySeconds: Int
        get() = if (demoMode) Config.DEMO_LEVEL2_DELAY_SECONDS else level2AfterSec

    /** The names of every watched app. */
    val watchedLabels: List<String>
        get() = apps.map { it.label }

    /** The watched app with this package name, or null if it is not watched. */
    fun watchedApp(packageName: String): WatchedApp? = apps.firstOrNull { it.packageName == packageName }

    /** How long the app may be used today before the reminders start. Demo mode uses seconds. */
    fun allowanceSeconds(app: WatchedApp): Int =
        if (demoMode) Config.DEMO_ALLOWANCE_SECONDS else app.allowanceMin * 60

    /** Adds an app to the watched apps, keeping the list sorted by name. */
    fun addApp(app: WatchedApp): Settings =
        copy(apps = (apps.filter { it.packageName != app.packageName } + app).sortedBy { it.label.lowercase() })

    fun removeApp(packageName: String): Settings =
        copy(apps = apps.filter { it.packageName != packageName })

    fun setAllowance(packageName: String, minutes: Int): Settings =
        copy(apps = apps.map { if (it.packageName == packageName) it.copy(allowanceMin = minutes) else it })

    /** The app's name for the logs, or the package name for an app that is not watched. */
    fun appLabel(packageName: String): String = watchedApp(packageName)?.label ?: packageName
}
