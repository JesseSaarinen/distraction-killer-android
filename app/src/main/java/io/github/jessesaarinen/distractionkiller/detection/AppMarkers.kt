package io.github.jessesaarinen.distractionkiller.detection

/**
 * One app the service watches, and how to recognise its short-video feed.
 * View IDs are the part after ":id/".
 *
 * swipeIds: views whose scroll events are swipes in the short-video feed.
 *
 * playerIds: views that are on screen only while the short-video player is
 * open. If the app has them, the service checks every second whether one is
 * visible, and that decides whether the short videos are in front. It also
 * notes which item of the swipe feed is on screen: a new item is a swipe, even
 * if the app sent no scroll event. Without player views, the last scroll
 * decides: a swipe in the feed, or a scroll somewhere else.
 *
 * reportsDistance: false for an app whose lists always report dy=0. Then a
 * dy=0 event can be a real swipe, and every scroll event in the feed counts.
 */
data class WatchedApp(
    val packageName: String,
    val label: String,
    val swipeIds: Set<String>,
    val playerIds: Set<String> = emptySet(),
    val reportsDistance: Boolean = true,
)

/**
 * The apps we watch and their markers (task N5). The IDs come from the
 * inspector on the test phone (Galaxy S21, 26.10.). An app update can change
 * them, so they are all in this one file.
 */
object AppMarkers {
    val apps = listOf(
        // The video feed is a vertical ViewPager. Most other TikTok IDs are scrambled.
        WatchedApp("com.zhiliaoapp.musically", "TikTok", swipeIds = setOf("viewpager")),
        WatchedApp("com.ss.android.ugc.trill", "TikTok", swipeIds = setOf("viewpager")),
        // Reels is the viewer's vertical pager. The home feed is "list" and does not count.
        WatchedApp("com.instagram.android", "Instagram", swipeIds = setOf("clips_viewer_view_pager")),
        // Shorts is reel_recycler. YouTube's lists report dy=0 even when they move,
        // so the Shorts player on screen tells when Shorts is in front.
        WatchedApp(
            "com.google.android.youtube",
            "YouTube",
            swipeIds = setOf("reel_recycler"),
            playerIds = setOf("reel_recycler", "reel_watch_fragment_root"),
            reportsDistance = false,
        ),
    )

    /** One name per app for the settings ("TikTok" has two packages). */
    val labels: List<String> = apps.map { it.label }.distinct()

    fun find(packageName: String): WatchedApp? = apps.firstOrNull { it.packageName == packageName }

    /** The app's name for the event log, or the package name for an unknown app. */
    fun label(packageName: String): String = find(packageName)?.label ?: packageName
}
