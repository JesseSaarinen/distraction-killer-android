package io.github.jessesaarinen.distractionkiller.detection

import android.os.Build
import io.github.jessesaarinen.distractionkiller.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** What kind of accessibility event an inspector entry is about. */
enum class EventKind { SCROLLED, WINDOW, CONTENT, SNAPSHOT }

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
)

/**
 * In-memory log for the inspector screen (task N4). Nothing is saved to disk,
 * so the log is gone when the app process ends.
 */
object InspectorLog {
    private const val MAX_ENTRIES = 300
    private const val MAX_SHARE_CHARS = 100_000

    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss")

    private val _entries = MutableStateFlow<List<InspectorEntry>>(emptyList())

    /** Newest first. */
    val entries: StateFlow<List<InspectorEntry>> = _entries.asStateFlow()

    private val _counts = MutableStateFlow<Map<String, Map<EventKind, Int>>>(emptyMap())

    /** How many events of each kind each app has sent. */
    val counts: StateFlow<Map<String, Map<EventKind, Int>>> = _counts.asStateFlow()

    private val lastSnapshot = mutableMapOf<String, List<String>>()

    fun add(entry: InspectorEntry) {
        _entries.update { (listOf(entry) + it).take(MAX_ENTRIES) }
    }

    fun count(packageName: String, kind: EventKind) {
        _counts.update { all ->
            val forApp = all[packageName].orEmpty()
            val newCount = (forApp[kind] ?: 0) + 1
            all + (packageName to (forApp + (kind to newCount)))
        }
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
        lastSnapshot.clear()
    }

    fun formatEntry(entry: InspectorEntry): String {
        val time = Instant.ofEpochMilli(entry.timeMillis).atZone(ZoneId.systemDefault()).format(timeFormat)
        val parts = mutableListOf(time, entry.packageName, entry.kind.name)
        entry.viewId?.let { parts.add("id=$it") }
        entry.className?.let { parts.add("class=$it") }
        entry.detail?.let { parts.add(it) }
        return parts.joinToString(" ")
    }

    fun formatCounts(kinds: Map<EventKind, Int>): String =
        kinds.entries.joinToString(" ") { "${it.key.name.lowercase()}=${it.value}" }

    /** Plain text for the share sheet, oldest entry first. */
    fun toShareText(): String = buildString {
        appendLine("Distraction Killer – tarkastelu")
        appendLine(
            "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}), " +
                "${Build.MANUFACTURER} ${Build.MODEL}, versio ${BuildConfig.VERSION_NAME}"
        )
        appendLine()
        appendLine("Laskurit:")
        for ((packageName, kinds) in counts.value) {
            appendLine("$packageName ${formatCounts(kinds)}")
        }
        appendLine()
        appendLine("Tapahtumat:")
        for (entry in entries.value.asReversed()) {
            appendLine(formatEntry(entry))
        }
    }.take(MAX_SHARE_CHARS)
}
