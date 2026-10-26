package io.github.jessesaarinen.distractionkiller

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
}
