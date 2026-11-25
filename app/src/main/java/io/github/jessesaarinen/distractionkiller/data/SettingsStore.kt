package io.github.jessesaarinen.distractionkiller.data

import android.content.Context
import android.content.SharedPreferences
import io.github.jessesaarinen.distractionkiller.AlarmSound
import io.github.jessesaarinen.distractionkiller.Config
import io.github.jessesaarinen.distractionkiller.TaskType
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
    // "intervalMin", "level2AfterSec" and "alarmMode" held the timing and the alarm
    // length before task N22. They are not read anymore.
    private const val KEY_REPEAT_SECONDS = "repeatSeconds"
    private const val KEY_TASKS = "tasks"
    private const val KEY_ALARM_SOUND = "alarmSound"
    private const val KEY_SOUND = "sound"
    private const val KEY_VIBRATION = "vibration"
    private const val KEY_DEMO_MODE = "demoMode"
    private const val KEY_DEMO_ALLOWANCE_SEC = "demoAllowanceSec"
    // "watchedApps" and "customApps" held the apps before the daily times (N21). They are not read anymore.
    private const val KEY_APPS = "apps"
    private const val SEPARATOR = "\t"
    private const val KEY_INTERRUPTIONS_ON = "interruptionsOn"
    private const val KEY_SETUP_DONE = "setupDone"
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
            .putInt(KEY_REPEAT_SECONDS, new.repeatSeconds)
            .putStringSet(KEY_TASKS, new.tasks.map { it.id }.toSet())
            .putString(KEY_ALARM_SOUND, new.alarmSound.id)
            .putBoolean(KEY_SOUND, new.sound)
            .putBoolean(KEY_VIBRATION, new.vibration)
            .putBoolean(KEY_DEMO_MODE, new.demoMode)
            .putInt(KEY_DEMO_ALLOWANCE_SEC, new.demoAllowanceSec)
            .putStringSet(KEY_APPS, new.apps.map { it.packageName + SEPARATOR + it.allowanceMin + SEPARATOR + it.label }.toSet())
            .putBoolean(KEY_INTERRUPTIONS_ON, new.interruptionsOn)
            .putBoolean(KEY_SETUP_DONE, new.setupDone)
            .putInt(KEY_TESTER, new.tester)
            .apply()
    }

    // Missing or unknown values fall back to the defaults, so settings saved by
    // an older version never break the app.
    private fun read(p: SharedPreferences): Settings {
        val defaults = Settings()
        val taskIds = p.getStringSet(KEY_TASKS, null).orEmpty()
        val tasks = TaskType.entries.filter { it.id in taskIds }.toSet()
        val demoAllowance = p.getInt(KEY_DEMO_ALLOWANCE_SEC, defaults.demoAllowanceSec)
        return Settings(
            repeatSeconds = Settings.clampRepeat(p.getInt(KEY_REPEAT_SECONDS, defaults.repeatSeconds)),
            tasks = tasks.ifEmpty { defaults.tasks },
            alarmSound = AlarmSound.fromId(p.getString(KEY_ALARM_SOUND, null)),
            sound = p.getBoolean(KEY_SOUND, defaults.sound),
            vibration = p.getBoolean(KEY_VIBRATION, defaults.vibration),
            demoMode = p.getBoolean(KEY_DEMO_MODE, defaults.demoMode),
            demoAllowanceSec = if (demoAllowance in Config.DEMO_ALLOWANCE_OPTIONS_SECONDS) demoAllowance else defaults.demoAllowanceSec,
            apps = readApps(p) ?: defaults.apps,
            interruptionsOn = p.getBoolean(KEY_INTERRUPTIONS_ON, defaults.interruptionsOn),
            setupDone = p.getBoolean(KEY_SETUP_DONE, defaults.setupDone),
            tester = p.getInt(KEY_TESTER, defaults.tester).coerceAtLeast(0),
        )
    }

    // Saved as "package<TAB>minutes<TAB>name". A package name cannot hold a tab, and the name
    // comes last, so it may hold anything. null = never saved, so the default apps are watched.
    // An empty list means the user removed them all.
    private fun readApps(p: SharedPreferences): List<WatchedApp>? =
        p.getStringSet(KEY_APPS, null)
            ?.mapNotNull { entry ->
                val parts = entry.split(SEPARATOR, limit = 3)
                val minutes = parts.getOrNull(1)?.toIntOrNull()
                val label = parts.getOrNull(2).orEmpty()
                if (parts[0].isEmpty() || minutes == null || minutes < 0 || label.isEmpty()) {
                    null
                } else {
                    WatchedApp(parts[0], label, minutes)
                }
            }
            ?.sortedBy { it.label.lowercase() }
}
