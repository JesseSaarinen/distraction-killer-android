package io.github.jessesaarinen.distractionkiller.data

import android.content.Context
import android.content.SharedPreferences
import io.github.jessesaarinen.distractionkiller.AlarmMode
import io.github.jessesaarinen.distractionkiller.Config
import io.github.jessesaarinen.distractionkiller.TaskType
import io.github.jessesaarinen.distractionkiller.detection.AppMarkers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Saves the settings on the phone, like getSettings() and saveSettings() in
 * the web demo's state.js. This is the only place that reads or writes them.
 * The service and the screens share one StateFlow, so a change works right away.
 */
object SettingsStore {
    private const val FILE_NAME = "settings"
    private const val KEY_INTERVAL_MIN = "intervalMin"
    private const val KEY_LEVEL2_AFTER_SEC = "level2AfterSec"
    private const val KEY_TASKS = "tasks"
    private const val KEY_ALARM_MODE = "alarmMode"
    private const val KEY_SOUND = "sound"
    private const val KEY_VIBRATION = "vibration"
    private const val KEY_DEMO_MODE = "demoMode"
    private const val KEY_WATCHED_APPS = "watchedApps"
    private const val KEY_INTERRUPTIONS_ON = "interruptionsOn"
    private const val KEY_TESTER = "tester"

    private var prefs: SharedPreferences? = null
    private val _settings = MutableStateFlow(Settings())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    /** Reads the saved settings once. DistractionKillerApp calls this when the app starts. */
    fun init(context: Context) {
        if (prefs != null) return
        val loaded = context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
        prefs = loaded
        _settings.value = read(loaded)
    }

    /** Changes and saves the settings, for example update { it.copy(sound = false) }. */
    fun update(change: (Settings) -> Settings) {
        val new = change(_settings.value)
        _settings.value = new
        val p = prefs ?: return
        p.edit()
            .putInt(KEY_INTERVAL_MIN, new.intervalMin)
            .putInt(KEY_LEVEL2_AFTER_SEC, new.level2AfterSec)
            .putStringSet(KEY_TASKS, new.tasks.map { it.id }.toSet())
            .putString(KEY_ALARM_MODE, new.alarmMode.id)
            .putBoolean(KEY_SOUND, new.sound)
            .putBoolean(KEY_VIBRATION, new.vibration)
            .putBoolean(KEY_DEMO_MODE, new.demoMode)
            .putStringSet(KEY_WATCHED_APPS, new.watchedApps)
            .putBoolean(KEY_INTERRUPTIONS_ON, new.interruptionsOn)
            .putInt(KEY_TESTER, new.tester)
            .apply()
    }

    // Missing or unknown values fall back to the defaults, so settings saved by
    // an older version never break the app.
    private fun read(p: SharedPreferences): Settings {
        val defaults = Settings()
        val interval = p.getInt(KEY_INTERVAL_MIN, defaults.intervalMin)
        val level2After = p.getInt(KEY_LEVEL2_AFTER_SEC, defaults.level2AfterSec)
        val taskIds = p.getStringSet(KEY_TASKS, null).orEmpty()
        val tasks = TaskType.entries.filter { it.id in taskIds }.toSet()
        // null = never saved, so every app is watched. An empty set means the user turned all off.
        val apps = p.getStringSet(KEY_WATCHED_APPS, null)
        return Settings(
            intervalMin = if (interval in Config.INTERVAL_OPTIONS_MIN) interval else defaults.intervalMin,
            level2AfterSec = if (level2After in Config.LEVEL2_DELAY_OPTIONS_SECONDS) level2After else defaults.level2AfterSec,
            tasks = tasks.ifEmpty { defaults.tasks },
            alarmMode = AlarmMode.fromId(p.getString(KEY_ALARM_MODE, null)),
            sound = p.getBoolean(KEY_SOUND, defaults.sound),
            vibration = p.getBoolean(KEY_VIBRATION, defaults.vibration),
            demoMode = p.getBoolean(KEY_DEMO_MODE, defaults.demoMode),
            watchedApps = if (apps == null) defaults.watchedApps else AppMarkers.labels.filter { it in apps }.toSet(),
            interruptionsOn = p.getBoolean(KEY_INTERRUPTIONS_ON, defaults.interruptionsOn),
            tester = p.getInt(KEY_TESTER, defaults.tester).coerceAtLeast(0),
        )
    }
}
