package io.github.jessesaarinen.distractionkiller.data

import android.content.Context
import android.os.Build
import io.github.jessesaarinen.distractionkiller.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.time.Instant

/**
 * The event log, saved in the app's own storage as events.jsonl (one JSON
 * object per line). This is the only place that reads or writes it. Nothing
 * leaves the phone unless the user shares the export.
 */
object EventLog {
    private const val FILE_NAME = "events.jsonl"

    private var file: File? = null
    private val _events = MutableStateFlow<List<LogEvent>>(emptyList())
    val events: StateFlow<List<LogEvent>> = _events.asStateFlow()

    /** Reads the saved log once. DistractionKillerApp calls this when the app starts. */
    fun init(context: Context) {
        if (file != null) return
        val loaded = File(context.filesDir, FILE_NAME)
        file = loaded
        _events.value = read(loaded)
    }

    fun add(event: LogEvent) {
        _events.value = _events.value + event
        try {
            file?.appendText(toJson(event).toString() + "\n")
        } catch (e: IOException) {
            // Storage full: the event stays in memory until the app closes.
        }
    }

    fun clear() {
        _events.value = emptyList()
        file?.delete()
    }

    /** Settings and all events for the RT8 test, like exportJson() in the web demo. */
    fun exportJson(settings: Settings): String {
        val json = JSONObject()
            .put("exportedAt", Instant.now().toString())
            .put("appVersion", BuildConfig.VERSION_NAME)
            .put("device", "${Build.MANUFACTURER} ${Build.MODEL}")
            .put("android", Build.VERSION.RELEASE)
            .put("settings", settingsJson(settings))
            .put("events", JSONArray(_events.value.map(::toJson)))
        return json.toString(2)
    }

    // A broken line (for example if the phone died while writing) is skipped.
    private fun read(file: File): List<LogEvent> {
        if (!file.exists()) return emptyList()
        val lines = try {
            file.readLines()
        } catch (e: IOException) {
            return emptyList()
        }
        return lines.mapNotNull { line ->
            try {
                fromJson(JSONObject(line))
            } catch (e: JSONException) {
                null
            }
        }
    }

    private fun toJson(event: LogEvent): JSONObject = JSONObject()
        .put("ts", event.ts)
        .put("app", event.app)
        .put("type", event.type)
        .put("level", event.level ?: JSONObject.NULL)
        .put("task", event.task ?: JSONObject.NULL)
        .put("outcome", event.outcome ?: JSONObject.NULL)
        .put("scrollSeconds", event.scrollSeconds)
        .put("answer", event.answer ?: JSONObject.NULL)

    private fun fromJson(json: JSONObject): LogEvent = LogEvent(
        ts = json.getLong("ts"),
        app = json.getString("app"),
        type = json.getString("type"),
        level = if (json.isNull("level")) null else json.getInt("level"),
        task = json.stringOrNull("task"),
        outcome = json.stringOrNull("outcome"),
        scrollSeconds = json.getInt("scrollSeconds"),
        answer = json.stringOrNull("answer"),
    )

    private fun JSONObject.stringOrNull(key: String): String? = if (isNull(key)) null else getString(key)

    private fun settingsJson(settings: Settings): JSONObject = JSONObject()
        .put("intervalMin", settings.intervalMin)
        .put("level2AfterSec", settings.level2AfterSec)
        .put("tasks", JSONArray(settings.tasks.map { it.id }))
        .put("alarmMode", settings.alarmMode.id)
        .put("sound", settings.sound)
        .put("vibration", settings.vibration)
        .put("demoMode", settings.demoMode)
        .put("watchedApps", JSONArray(settings.watchedApps.toList()))
        .put("interruptionsOn", settings.interruptionsOn)
}
