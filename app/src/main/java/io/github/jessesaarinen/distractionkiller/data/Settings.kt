package io.github.jessesaarinen.distractionkiller.data

import io.github.jessesaarinen.distractionkiller.AlarmSound
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
 * repeatSeconds: after an app's daily time is used up, the reminder repeats
 * every this many seconds of use, and every other one brings a level 2 task
 * (task N22). Between Config.REPEAT_MIN_SECONDS and Config.REPEAT_MAX_MINUTES.
 *
 * alarmSound: what the reminder sounds like when sound is on.
 *
 * demoAllowanceSec: in demo mode, every app's daily time in seconds, one of
 * Config.DEMO_ALLOWANCE_OPTIONS_SECONDS.
 *
 * setupDone: false shows the setup steps when the app opens (first start, or
 * "Aloita uusi demo").
 *
 * interruptionsOn: false pauses the app. Nothing is counted and nothing interrupts.
 *
 * tester: the RT8 tester whose test is running (task N19). Every event gets
 * this number, so the testers' results stay apart. 0 = no test running.
 */
data class Settings(
    val repeatSeconds: Int = Config.DEFAULT_REPEAT_SECONDS,
    val tasks: Set<TaskType> = Config.DEFAULT_TASKS,
    val alarmSound: AlarmSound = Config.DEFAULT_ALARM_SOUND,
    val sound: Boolean = Config.DEFAULT_SOUND,
    val vibration: Boolean = Config.DEFAULT_VIBRATION,
    val demoMode: Boolean = Config.DEFAULT_DEMO_MODE,
    val demoAllowanceSec: Int = Config.DEFAULT_DEMO_ALLOWANCE_SECONDS,
    val apps: List<WatchedApp> = Config.DEFAULT_APPS.sortedBy { it.label.lowercase() },
    val interruptionsOn: Boolean = Config.DEFAULT_INTERRUPTIONS_ON,
    val setupDone: Boolean = false,
    val tester: Int = 0,
) {
    /** The names of every watched app. */
    val watchedLabels: List<String>
        get() = apps.map { it.label }

    /** The watched app with this package name, or null if it is not watched. */
    fun watchedApp(packageName: String): WatchedApp? = apps.firstOrNull { it.packageName == packageName }

    /** How long the app may be used today before the reminders start. Demo mode uses seconds. */
    fun allowanceSeconds(app: WatchedApp): Int =
        if (demoMode) demoAllowanceSec else app.allowanceMin * 60

    /** Adds an app to the watched apps, keeping the list sorted by name. */
    fun addApp(app: WatchedApp): Settings =
        copy(apps = (apps.filter { it.packageName != app.packageName } + app).sortedBy { it.label.lowercase() })

    fun removeApp(packageName: String): Settings =
        copy(apps = apps.filter { it.packageName != packageName })

    fun setAllowance(packageName: String, minutes: Int): Settings =
        copy(apps = apps.map { if (it.packageName == packageName) it.copy(allowanceMin = minutes) else it })

    /** The app's name for the logs, or the package name for an app that is not watched. */
    fun appLabel(packageName: String): String = watchedApp(packageName)?.label ?: packageName

    /**
     * "Aloita uusi demo": demo mode with the default apps and short times, and
     * the setup steps again, so the next visitor starts from the beginning.
     * The stats and the sound choices stay.
     */
    fun newDemo(): Settings = copy(
        demoMode = true,
        demoAllowanceSec = Config.DEFAULT_DEMO_ALLOWANCE_SECONDS,
        repeatSeconds = Config.DEMO_REPEAT_SECONDS,
        apps = Config.DEFAULT_APPS.sortedBy { it.label.lowercase() },
        interruptionsOn = true,
        setupDone = false,
        tester = 0,
    )

    companion object {
        /** The longest repeat time the picker offers. */
        const val REPEAT_MAX_SECONDS = Config.REPEAT_MAX_MINUTES * 60 + 60 - Config.REPEAT_STEP_SECONDS

        /** A repeat time the picker can show: within the limits, never 0. */
        fun clampRepeat(seconds: Int): Int = seconds.coerceIn(Config.REPEAT_MIN_SECONDS, REPEAT_MAX_SECONDS)
    }
}
