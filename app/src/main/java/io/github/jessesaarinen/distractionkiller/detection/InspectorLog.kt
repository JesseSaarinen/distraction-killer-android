package io.github.jessesaarinen.distractionkiller.detection

import android.content.Context
import android.os.Build
import io.github.jessesaarinen.distractionkiller.BuildConfig
import io.github.jessesaarinen.distractionkiller.data.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * What an inspector entry is about: an accessibility event, a snapshot, the
 * short videos coming to or leaving the front, a reached level, an outcome,
 * a note about the app itself (started, log cleared) or a caught error.
 */
enum class EventKind { SCROLLED, WINDOW, CONTENT, SNAPSHOT, FRONT, LEVEL1, LEVEL2, OUTCOME, NOTE, ERROR }

/**
 * One line in the inspector. Only technical data: app package, event kind,
 * view ID, class name and scroll numbers. Never any text from the screen.
 */
data class InspectorEntry(
    val timeMillis: Long,
    val packageName: String,
    val kind: EventKind,
    val viewId: String? = null,
    val className: String? = null,
    val detail: String? = null,
) {
    /** One line of the saved file: the fields separated by tabs, empty for none. */
    fun toLine(): String =
        listOf(timeMillis.toString(), packageName, kind.name, viewId.orEmpty(), className.orEmpty(), detail.orEmpty())
            .joinToString(SEPARATOR) { it.replace(LINE_BREAKS, " ") }

    companion object {
        private const val SEPARATOR = "\t"
        private val LINE_BREAKS = Regex("[\\t\\r\\n]")

        /** Reads a line written by toLine(), or null for a broken line. */
        fun fromLine(line: String): InspectorEntry? {
            val parts = line.split(SEPARATOR, limit = 6)
            if (parts.size != 6) return null
            val time = parts[0].toLongOrNull() ?: return null
            val kind = EventKind.entries.firstOrNull { it.name == parts[2] } ?: return null
            return InspectorEntry(
                timeMillis = time,
                packageName = parts[1],
                kind = kind,
                viewId = parts[3].ifEmpty { null },
                className = parts[4].ifEmpty { null },
                detail = parts[5].ifEmpty { null },
            )
        }
    }
}

/** The scroll watcher's state right now, for the top of the inspector and the shared text. */
data class InspectorStatus(
    /** When the scroll watcher started, or null when it is not running. */
    val serviceStartedAt: Long? = null,
    /** The app whose short videos are in front, or null. */
    val inFrontApp: String? = null,
    /** The newest event of any kind from a watched app. */
    val lastEventAt: Long? = null,
    val lastEventApp: String? = null,
    /** The newest scroll line, with its verdict. */
    val lastScroll: InspectorEntry? = null,
)

/**
 * The inspector's log (tasks N4 and N5). Saved in the app's own storage as
 * inspector.log, so it is still there after the phone closes the app. At most
 * MAX_ENTRIES lines. The event counters count only while the app runs.
 */
object InspectorLog {
    private const val MAX_ENTRIES = 300
    private const val MAX_SHARE_CHARS = 100_000
    private const val FILE_NAME = "inspector.log"
    private const val SAME_ERROR_PAUSE_MS = 60_000L

    private val clockFormat = DateTimeFormatter.ofPattern("HH:mm:ss")
    private val dateFormat = DateTimeFormatter.ofPattern("d.M.")
    private val dateTimeFormat = DateTimeFormatter.ofPattern("d.M. 'klo' HH:mm:ss")

    private var file: File? = null
    private var linesInFile = 0
    private var appPackage = "app"
    private var startedAt = System.currentTimeMillis()
    private var lastError: String? = null
    private var lastErrorAt = 0L

    private val _entries = MutableStateFlow<List<InspectorEntry>>(emptyList())

    /** Newest first. */
    val entries: StateFlow<List<InspectorEntry>> = _entries.asStateFlow()

    private val _counts = MutableStateFlow<Map<String, Map<EventKind, Int>>>(emptyMap())

    /** How many events of each kind each app has sent since the app started. */
    val counts: StateFlow<Map<String, Map<EventKind, Int>>> = _counts.asStateFlow()

    private val _status = MutableStateFlow(InspectorStatus())
    val status: StateFlow<InspectorStatus> = _status.asStateFlow()

    private val lastSnapshot = mutableMapOf<String, List<String>>()

    /** Reads the saved log once. DistractionKillerApp calls this when the app starts. */
    fun init(context: Context) {
        if (file != null) return
        val saved = File(context.filesDir, FILE_NAME)
        file = saved
        appPackage = context.packageName
        startedAt = System.currentTimeMillis()
        val lines = read(saved)
        linesInFile = lines.size
        _entries.value = lines.asReversed().take(MAX_ENTRIES)
        note("app started")
    }

    fun add(entry: InspectorEntry) {
        _entries.update { (listOf(entry) + it).take(MAX_ENTRIES) }
        if (entry.kind == EventKind.SCROLLED) _status.update { it.copy(lastScroll = entry) }
        save(entry)
    }

    /** A note about the app itself, like "service started". */
    fun note(text: String) {
        add(InspectorEntry(System.currentTimeMillis(), appPackage, EventKind.NOTE, detail = text))
    }

    /**
     * An error that the service caught instead of crashing. The same error
     * again within a minute is not listed again.
     */
    fun error(where: String, e: Throwable) {
        val frame = e.stackTrace.firstOrNull()?.let { " (${it.fileName}:${it.lineNumber})" }.orEmpty()
        val text = "$where: ${e.javaClass.simpleName}: ${e.message}$frame"
        val now = System.currentTimeMillis()
        if (text == lastError && now - lastErrorAt < SAME_ERROR_PAUSE_MS) return
        lastError = text
        lastErrorAt = now
        add(InspectorEntry(now, appPackage, EventKind.ERROR, detail = text))
    }

    fun count(packageName: String, kind: EventKind) {
        _counts.update { all ->
            val forApp = all[packageName].orEmpty()
            val newCount = (forApp[kind] ?: 0) + 1
            all + (packageName to (forApp + (kind to newCount)))
        }
        _status.update { it.copy(lastEventAt = System.currentTimeMillis(), lastEventApp = packageName) }
    }

    /** The service calls this when it starts and stops. */
    fun setServiceRunning(running: Boolean) {
        _status.update {
            it.copy(serviceStartedAt = if (running) System.currentTimeMillis() else null, inFrontApp = null)
        }
    }

    /** The service calls this when the short videos come to or leave the front. */
    fun setInFront(packageName: String?) {
        _status.update { it.copy(inFrontApp = packageName) }
    }

    /** Adds a snapshot entry only when the app's view IDs changed since its last snapshot. */
    fun addSnapshotIfChanged(timeMillis: Long, packageName: String, viewIds: List<String>) {
        if (lastSnapshot[packageName] == viewIds) return
        lastSnapshot[packageName] = viewIds
        add(InspectorEntry(timeMillis, packageName, EventKind.SNAPSHOT, detail = viewIds.joinToString(" ")))
    }

    fun clear() {
        _entries.value = emptyList()
        _counts.value = emptyMap()
        _status.update { it.copy(lastEventAt = null, lastEventApp = null, lastScroll = null) }
        lastSnapshot.clear()
        file?.delete()
        linesInFile = 0
        note("log cleared")
    }

    /** "21:05:13" */
    fun formatClock(millis: Long): String = format(millis, clockFormat)

    fun formatEntry(entry: InspectorEntry): String {
        val parts = mutableListOf(formatClock(entry.timeMillis), entry.packageName, entry.kind.name)
        entry.viewId?.let { parts.add("id=$it") }
        entry.className?.let { parts.add("class=$it") }
        entry.detail?.let { parts.add(it) }
        return parts.joinToString(" ")
    }

    fun formatCounts(kinds: Map<EventKind, Int>): String =
        kinds.entries.joinToString(" ") { "${it.key.name.lowercase()}=${it.value}" }

    /**
     * Plain text for the share sheet: first the state, so that even an empty
     * log tells why it is empty, then the counters and the entries, oldest first.
     */
    fun toShareText(settings: Settings): String = buildString {
        val now = System.currentTimeMillis()
        val state = status.value
        val timer = ScrollTimer.state.value
        appendLine("Distraction Killer – tarkastelu")
        appendLine(
            "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}), " +
                "${Build.MANUFACTURER} ${Build.MODEL}, versio ${BuildConfig.VERSION_NAME}"
        )
        appendLine("Jaettu ${format(now, dateTimeFormat)}")
        val started = state.serviceStartedAt
        appendLine(
            if (started != null) {
                "Tunnistus: käynnissä ${format(started, dateTimeFormat)} alkaen"
            } else {
                "Tunnistus: EI käynnissä (esteettömyysasetus on pois, tai puhelin pysäytti palvelun)"
            }
        )
        appendLine("Edessä: " + (state.inFrontApp?.let { "lyhytvideot (${AppMarkers.label(it)})" } ?: "ei lyhytvideoita"))
        appendLine("Ajastin: ${timer.activeSeconds} s aktiivista selausta (${timer.phase.name})")
        val lastEventAt = state.lastEventAt
        appendLine(
            "Viimeisin tapahtuma: " +
                if (lastEventAt != null) "${format(lastEventAt, dateTimeFormat)} ${state.lastEventApp}" else "ei yhtään"
        )
        appendLine("Viimeisin vieritys: " + (state.lastScroll?.let(::formatEntry) ?: "ei yhtään"))
        appendLine("Asetukset: ${settingsSummary(settings)}")
        appendLine(
            "Sovellus käynnistyi ${format(startedAt, dateTimeFormat)}. " +
                "Laskurit ovat siitä alkaen, tapahtumat säilyvät uudelleenkäynnistyksen yli."
        )
        appendLine()
        appendLine("Laskurit:")
        if (counts.value.isEmpty()) appendLine("(ei tapahtumia)")
        for ((packageName, kinds) in counts.value) {
            appendLine("$packageName ${formatCounts(kinds)}")
        }
        appendLine()
        appendLine("Tapahtumat:")
        // The newest lines that fit, so a long log loses its oldest lines, not its newest.
        val fitting = mutableListOf<InspectorEntry>()
        var room = MAX_SHARE_CHARS - length
        for (entry in entries.value) {
            room -= formatEntry(entry).length + 1
            if (room < 0) break
            fitting.add(entry)
        }
        val left = entries.value.size - fitting.size
        if (left > 0) appendLine("($left vanhinta riviä jätetty pois)")
        var day = ""
        for (entry in fitting.asReversed()) {
            val entryDay = format(entry.timeMillis, dateFormat)
            if (entryDay != day) {
                appendLine("— $entryDay —")
                day = entryDay
            }
            appendLine(formatEntry(entry))
        }
    }

    private fun settingsSummary(settings: Settings): String {
        fun onOff(on: Boolean) = if (on) "päällä" else "pois"
        val watched = AppMarkers.labels.filter { it in settings.watchedApps }
        return listOf(
            "keskeytykset ${if (settings.interruptionsOn) "päällä" else "tauolla"}",
            "väli ${settings.intervalMin} min",
            "demotila ${onOff(settings.demoMode)}",
            "taso 1 ${settings.level1Seconds} s",
            "taso 2 +${settings.level2DelaySeconds} s",
            "hälytys ${settings.alarmMode.id}",
            "ääni ${onOff(settings.sound)}",
            "värinä ${onOff(settings.vibration)}",
            "tehtävät ${settings.tasks.joinToString("+") { it.id }}",
            "valvotut ${watched.joinToString("+").ifEmpty { "ei yhtään" }}",
        ).joinToString(", ")
    }

    private fun format(millis: Long, formatter: DateTimeFormatter): String =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(formatter)

    // Broken lines (for example if the phone died while writing) are skipped.
    private fun read(saved: File): List<InspectorEntry> {
        if (!saved.exists()) return emptyList()
        return try {
            saved.readLines().mapNotNull(InspectorEntry::fromLine)
        } catch (e: IOException) {
            emptyList()
        }
    }

    // Appends one line. When the file has twice as many lines as the log keeps,
    // it is written again with only the kept lines. If storage is full, the
    // line stays in memory only.
    private fun save(entry: InspectorEntry) {
        val saved = file ?: return
        try {
            if (linesInFile >= 2 * MAX_ENTRIES) {
                val kept = _entries.value.asReversed()
                val temp = File(saved.parentFile, "$FILE_NAME.tmp")
                temp.writeText(kept.joinToString("") { it.toLine() + "\n" })
                if (temp.renameTo(saved)) linesInFile = kept.size
            } else {
                saved.appendText(entry.toLine() + "\n")
                linesInFile++
            }
        } catch (e: IOException) {
            // Nothing to do: the log in memory is still complete.
        }
    }
}
