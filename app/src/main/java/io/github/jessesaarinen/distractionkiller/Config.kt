package io.github.jessesaarinen.distractionkiller

/** How long the level 1 alarm rings. */
enum class AlarmMode {
    /** "Kunnes sammutan": until the user taps a banner button. */
    UNTIL_DISMISSED,

    /** "15 sekuntia": stops by itself after Config.ALARM_TIMED_SECONDS. */
    TIMED,
}

/** The level 2 tasks. The id is what the event log stores, same as in the web demo. */
enum class TaskType(val id: String) {
    BREATHING("breathing"),
    QUESTION("question"),
    PHYSICAL("physical");

    companion object {
        fun fromId(id: String?): TaskType = entries.firstOrNull { it.id == id } ?: BREATHING
    }
}

/** All timings and choices in one place, like config.js in the web demo. */
object Config {
    /** Interval choices for level 1, in minutes. */
    val INTERVAL_OPTIONS_MIN = listOf(1, 3, 5, 10, 15)
    const val DEFAULT_INTERVAL_MIN = 1

    /** A second counts as active scrolling if the user swiped within this time. */
    const val SWIPE_WINDOW_SECONDS = 60

    /** After level 1, this many more active seconds bring level 2. */
    const val LEVEL2_DELAY_SECONDS = 30

    /** This long without active scrolling starts a new session from zero. */
    const val SESSION_BREAK_SECONDS = 10 * 60

    // Demo mode: fast timings for showing the app at the fair.
    const val DEMO_LEVEL1_SECONDS = 15
    const val DEMO_LEVEL2_DELAY_SECONDS = 10

    /** Level 1 alarm: a chime and a short vibration every ALARM_BEAT_MS. */
    const val ALARM_BEAT_MS = 1_500L
    const val ALARM_VIBRATION_MS = 200L
    const val ALARM_TIMED_SECONDS = 15

    /** Level 2 starts with one chime and this vibration pattern (pause, buzz, pause, buzz). */
    val LEVEL2_VIBRATION_PATTERN = longArrayOf(0, 300, 150, 300)

    /** The Ohita button appears after this long. */
    const val SKIP_DELAY_SECONDS = 5

    const val BREATHING_SECONDS = 15
    const val BREATHING_PHASE_SECONDS = 4
    const val PHYSICAL_SECONDS = 10
    const val ANSWER_MAX_LENGTH = 120

    // Defaults until the settings screen exists (N10).
    val DEFAULT_ALARM_MODE = AlarmMode.UNTIL_DISMISSED
    const val DEFAULT_SOUND = true
    const val DEFAULT_VIBRATION = true
    val DEFAULT_TASKS = TaskType.entries.toList()
}
