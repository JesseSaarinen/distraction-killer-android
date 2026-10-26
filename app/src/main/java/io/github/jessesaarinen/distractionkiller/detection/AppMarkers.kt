package io.github.jessesaarinen.distractionkiller.detection

/** One app the service watches. Short-video markers are added in task N5. */
data class WatchedApp(
    val packageName: String,
    val label: String,
)

/** The apps we watch, and (from N5 on) how we recognise their short-video part. */
object AppMarkers {
    val apps = listOf(
        WatchedApp("com.zhiliaoapp.musically", "TikTok"),
        WatchedApp("com.ss.android.ugc.trill", "TikTok"),
        WatchedApp("com.instagram.android", "Instagram"),
        WatchedApp("com.google.android.youtube", "YouTube"),
    )

    /** One name per app for the settings ("TikTok" has two packages). */
    val labels: List<String> = apps.map { it.label }.distinct()

    fun find(packageName: String): WatchedApp? = apps.firstOrNull { it.packageName == packageName }
}
