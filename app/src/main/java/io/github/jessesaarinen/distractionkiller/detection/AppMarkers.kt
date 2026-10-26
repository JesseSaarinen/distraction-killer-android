package io.github.jessesaarinen.distractionkiller.detection

/**
 * One app the service watches.
 *
 * shortVideoIds are the view IDs (the part after ":id/") of the app's
 * short-video feed. Only swipes there count. Empty: the whole app counts.
 */
data class WatchedApp(
    val packageName: String,
    val label: String,
    val shortVideoIds: Set<String> = emptySet(),
)

/**
 * The apps we watch, and how we recognise their short-video feed (task N5).
 * The IDs come from the inspector on the test phone. An app update can change
 * them, so they are all in this one file.
 */
object AppMarkers {
    val apps = listOf(
        // Not checked yet, so the whole app counts.
        WatchedApp("com.zhiliaoapp.musically", "TikTok"),
        WatchedApp("com.ss.android.ugc.trill", "TikTok"),
        // Reels, checked 26.10.: the viewer's vertical pager. The home feed is "list".
        WatchedApp("com.instagram.android", "Instagram", shortVideoIds = setOf("clips_viewer_view_pager")),
        // Not checked yet, so the whole app counts.
        WatchedApp("com.google.android.youtube", "YouTube"),
    )

    /** One name per app for the settings ("TikTok" has two packages). */
    val labels: List<String> = apps.map { it.label }.distinct()

    fun find(packageName: String): WatchedApp? = apps.firstOrNull { it.packageName == packageName }

    /** The app's name for the event log, or the package name for an unknown app. */
    fun label(packageName: String): String = find(packageName)?.label ?: packageName

    /** True when a scroll in this view (short ID, if any) is in the app's short-video feed. */
    fun isShortVideo(packageName: String, viewId: String?): Boolean {
        val app = find(packageName) ?: return false
        return app.shortVideoIds.isEmpty() || (viewId != null && viewId in app.shortVideoIds)
    }
}
