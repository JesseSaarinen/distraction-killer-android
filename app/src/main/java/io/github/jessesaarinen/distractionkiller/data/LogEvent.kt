package io.github.jessesaarinen.distractionkiller.data

/**
 * One row of the event log, like logEvent() in the web demo's state.js.
 *
 * - type: "level1", "level2" or "outcome"
 * - app: the watched app's name, like "TikTok"
 * - scrollSeconds: the app's time in front today when this happened. The name
 *   is from the old scroll counting, kept so old and new logs read the same way.
 * - level: 1 or 2, only on outcome rows
 * - task: "breathing", "question" or "physical", on level 2 rows
 * - outcome: "stopped", "continued", "skipped" or "dismissed" (left the app some other way)
 * - answer: what the user answered in the question task, if anything
 * - tester: the RT8 tester number (task N19), 0 when no test is running
 */
data class LogEvent(
    val type: String,
    val app: String,
    val scrollSeconds: Int,
    val level: Int? = null,
    val task: String? = null,
    val outcome: String? = null,
    val answer: String? = null,
    val tester: Int = 0,
    val ts: Long = System.currentTimeMillis(),
)
