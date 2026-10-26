package io.github.jessesaarinen.distractionkiller.detection

/** What one scroll event means for the timer. */
enum class ScrollVerdict {
    /** A swipe in the short-video feed. The timer counts it. */
    SWIPE,

    /** The user scrolled something else in the app, so the short videos are not in front. */
    OTHER,

    /** Nothing moved. A list sends this when it only lays itself out. */
    LAYOUT,

    /** Not one of the watched apps. */
    IGNORED,
}

/** A verdict and a short reason, shown in the inspector. */
data class Classification(val verdict: ScrollVerdict, val reason: String)

/**
 * Decides what a scroll event in a watched app means, using the markers in
 * AppMarkers (task N5). Plain Kotlin without Android classes, so it has unit tests.
 */
object ShortVideoClassifier {

    /**
     * viewId is the short ID of the view that scrolled, if it has one. dx and
     * dy are how far it moved, or null on Android 8, which does not tell.
     */
    fun classifyScroll(packageName: String, viewId: String?, dx: Int?, dy: Int?): Classification {
        val app = AppMarkers.find(packageName) ?: return Classification(ScrollVerdict.IGNORED, "not watched")
        val inFeed = viewId != null && viewId in app.swipeIds
        if (dx == null || dy == null || !app.reportsDistance) {
            // No distance to tell a swipe from a layout pass, so the view alone decides.
            return if (inFeed) {
                Classification(ScrollVerdict.SWIPE, "in the feed, no distance")
            } else {
                Classification(ScrollVerdict.OTHER, "not the feed")
            }
        }
        if (dx == 0 && dy == 0) return Classification(ScrollVerdict.LAYOUT, "did not move")
        if (!inFeed) return Classification(ScrollVerdict.OTHER, "not the feed")
        // The feeds scroll up and down. A sideways scroll is something else with the same ID, like a tab pager.
        if (dy == 0) return Classification(ScrollVerdict.OTHER, "sideways")
        return Classification(ScrollVerdict.SWIPE, "in the feed")
    }

    /**
     * Whether the app's short-video player is on screen, from the player views
     * that are visible: true or false, or null when the app has no player views.
     */
    fun isPlayerOnScreen(packageName: String, visibleIds: Collection<String>): Boolean? {
        val app = AppMarkers.find(packageName) ?: return false
        if (app.playerIds.isEmpty()) return null
        return visibleIds.any { it in app.playerIds }
    }
}
