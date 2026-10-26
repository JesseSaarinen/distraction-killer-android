package io.github.jessesaarinen.distractionkiller.detection

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import io.github.jessesaarinen.distractionkiller.Config

/**
 * Watches TikTok, Instagram and YouTube through Android's accessibility API.
 * Reads only the app's package name, view IDs, class names and scroll positions.
 * Never reads text, messages or video content.
 *
 * Swipes feed the ScrollTimer (N6). Reached levels are only logged to the
 * inspector for now; the banner and the calm overlay come in N7 and N9.
 */
class ScrollWatcherService : AccessibilityService() {

    private val lastSnapshotAt = mutableMapOf<String, Long>()

    private val handler = Handler(Looper.getMainLooper())
    private val tickRunnable = object : Runnable {
        override fun run() {
            onTick()
            handler.postDelayed(this, TICK_MS)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        handler.postDelayed(tickRunnable, TICK_MS)
    }

    override fun onDestroy() {
        handler.removeCallbacks(tickRunnable)
        super.onDestroy()
    }

    private fun onTick() {
        val now = System.currentTimeMillis()
        val foreground = foregroundWatchedApp()
        // Settings come in N10. Until then: default interval, no demo mode.
        val level = ScrollTimer.tick(
            nowMillis = now,
            shortVideoInFront = foreground != null,
            level1Seconds = Config.DEFAULT_INTERVAL_MIN * 60,
            level2DelaySeconds = Config.LEVEL2_DELAY_SECONDS,
        )
        if (level != null && foreground != null) {
            val kind = if (level == 1) EventKind.LEVEL1 else EventKind.LEVEL2
            val seconds = ScrollTimer.state.value.activeSeconds
            InspectorLog.add(InspectorEntry(now, foreground, kind, detail = "active=${seconds}s"))
        }
    }

    /**
     * The watched app in front, if the screen is on. For now the whole app
     * counts; N5 narrows this to the short-video screens.
     */
    private fun foregroundWatchedApp(): String? {
        val power = getSystemService(PowerManager::class.java)
        if (power == null || !power.isInteractive) return null
        val packageName = rootInActiveWindow?.packageName?.toString() ?: return null
        return if (AppMarkers.find(packageName) != null) packageName else null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        if (AppMarkers.find(packageName) == null) return

        val kind = when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> EventKind.SCROLLED
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> EventKind.WINDOW
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> EventKind.CONTENT
            else -> return
        }
        InspectorLog.count(packageName, kind)
        // Content changes arrive many times a second while a video plays,
        // so they are only counted, not listed.
        if (kind == EventKind.CONTENT) return

        val now = System.currentTimeMillis()
        if (kind == EventKind.SCROLLED) ScrollTimer.onSwipe(now)
        InspectorLog.add(
            InspectorEntry(
                timeMillis = now,
                packageName = packageName,
                kind = kind,
                viewId = event.source?.viewIdResourceName?.let(::shortId),
                className = event.className?.toString(),
                detail = if (kind == EventKind.SCROLLED) scrollDetail(event) else null,
            )
        )
        takeSnapshot(now, packageName)
    }

    override fun onInterrupt() {
        // Nothing to stop: this service gives no spoken or haptic feedback.
    }

    /** Lists the view IDs on screen, at most once every 2 seconds per app. */
    private fun takeSnapshot(now: Long, packageName: String) {
        if (now - (lastSnapshotAt[packageName] ?: 0L) < SNAPSHOT_INTERVAL_MS) return
        lastSnapshotAt[packageName] = now

        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() != packageName) return
        InspectorLog.addSnapshotIfChanged(now, packageName, collectViewIds(root))
    }

    private fun collectViewIds(root: AccessibilityNodeInfo): List<String> {
        val ids = sortedSetOf<String>()
        val toVisit = mutableListOf(root)
        var visited = 0
        while (toVisit.isNotEmpty() && visited < MAX_NODES) {
            val node = toVisit.removeAt(toVisit.lastIndex)
            visited++
            node.viewIdResourceName?.let { ids.add(shortId(it)) }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { toVisit.add(it) }
            }
        }
        return ids.toList()
    }

    /** "com.instagram.android:id/clips_viewer" → "clips_viewer" */
    private fun shortId(viewId: String): String = viewId.substringAfter(":id/")

    /** Which items are visible in the scrolled list, and how far it moved. */
    private fun scrollDetail(event: AccessibilityEvent): String {
        val parts = mutableListOf<String>()
        if (event.itemCount >= 0) {
            parts.add("items=${event.fromIndex}-${event.toIndex}/${event.itemCount}")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            parts.add("dy=${event.scrollDeltaY}")
        }
        return parts.joinToString(" ")
    }

    private companion object {
        const val TICK_MS = 1_000L
        const val SNAPSHOT_INTERVAL_MS = 2_000L
        const val MAX_NODES = 1_000
    }
}
