package io.github.jessesaarinen.distractionkiller

import io.github.jessesaarinen.distractionkiller.data.WatchedApp

/**
 * What the reminder sounds like (task N22). The id is what the settings store.
 * SPEECH says one of the spoken reminders (strings.xml, speech_lines) with the
 * phone's text-to-speech. If the phone has no Finnish voice, the chime plays instead.
 */
enum class AlarmSound(val id: String) {
    CHIME("chime"),
    BEEP("beep"),
    SPEECH("speech");

    companion object {
        fun fromId(id: String?): AlarmSound = entries.firstOrNull { it.id == id } ?: Config.DEFAULT_ALARM_SOUND
    }
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
    /** The apps watched before the user changes anything, with their daily time in minutes. */
    val DEFAULT_APPS = listOf(
        WatchedApp("com.zhiliaoapp.musically", "TikTok", 30),
        WatchedApp("com.instagram.android", "Instagram", 30),
        WatchedApp("com.google.android.youtube", "YouTube", 30),
    )

    /** Daily time for an app the user adds. */
    const val DEFAULT_ALLOWANCE_MIN = 30

    /** The daily time pickers: 0 to ALLOWANCE_MAX_HOURS hours, minutes in steps of ALLOWANCE_STEP_MIN. */
    const val ALLOWANCE_MAX_HOURS = 12
    const val ALLOWANCE_STEP_MIN = 5

    /**
     * After the daily time is used up, the reminder repeats every this many
     * seconds of use (task N22). Every other reminder brings a level 2 task.
     * The picker: 0 to REPEAT_MAX_MINUTES minutes, seconds in steps of REPEAT_STEP_SECONDS.
     */
    const val DEFAULT_REPEAT_SECONDS = 60
    const val REPEAT_MIN_SECONDS = 5
    const val REPEAT_MAX_MINUTES = 30
    const val REPEAT_STEP_SECONDS = 5

    // Demo mode: the daily time is seconds, so the reminder comes fast at the fair.
    val DEMO_ALLOWANCE_OPTIONS_SECONDS = listOf(10, 15, 30, 60)
    const val DEFAULT_DEMO_ALLOWANCE_SECONDS = 15

    /** "Aloita uusi demo" sets this repeat time. The visitor can change it in the setup. */
    const val DEMO_REPEAT_SECONDS = 15

    /** The alarm rings this long, then stops by itself (task N22). The banner stays until a tap. */
    const val ALARM_SECONDS = 5

    /** Chime and beep: one sound and a short vibration every ALARM_BEAT_MS while the alarm rings. */
    const val ALARM_BEAT_MS = 1_500L
    const val ALARM_VIBRATION_MS = 200L

    /** The Ohita button appears after this long. */
    const val SKIP_DELAY_SECONDS = 5

    /** Stats (task N19): Ohita counts as tapped right away within this long after it appeared. */
    const val QUICK_SKIP_SECONDS = 3

    const val BREATHING_SECONDS = 15
    const val BREATHING_PHASE_SECONDS = 4
    const val PHYSICAL_SECONDS = 10
    const val ANSWER_MAX_LENGTH = 120

    // Default settings, like DEFAULT_SETTINGS in the web demo.
    // Speech, because the RT8 tester found a spoken reminder the most effective.
    val DEFAULT_ALARM_SOUND = AlarmSound.SPEECH
    const val DEFAULT_SOUND = true
    const val DEFAULT_VIBRATION = true
    const val DEFAULT_DEMO_MODE = false
    const val DEFAULT_INTERRUPTIONS_ON = true
    val DEFAULT_TASKS = TaskType.entries.toSet()
}
