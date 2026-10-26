package io.github.jessesaarinen.distractionkiller.detection

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import io.github.jessesaarinen.distractionkiller.Config
import io.github.jessesaarinen.distractionkiller.interrupt.InterruptController

/**
 * Watches TikTok, Instagram and YouTube through Android's accessibility API.
 * Reads only the app's package name, view IDs, class names and scroll positions.
 * Never reads text, messages or video content.
 *
 * Swipes feed the ScrollTimer (N6). Level 1 shows the banner (N7); the calm
 * level 2 overlay comes in N9.
 */
class ScrollWatcherService : AccessibilityService() {

    private val lastSnapshotAt = mutableMapOf<String, Long>()
    private var interrupts: InterruptController? = null

    /** Ticks in a row with no watched app in front. */
    private var awayTicks = 0

    private val handler = Handler(Looper.getMainLooper())
    private val tickRunnable = object : Runnable {
        override fun run() {
            onTick()
            handler.postDelayed(this, TICK_MS)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        interrupts = InterruptController(this)
        handler.postDelayed(tickRunnable, TICK_MS)
    }

    override fun onDestroy() {
        handler.removeCallbacks(tickRunnable)
        interrupts?.release()
        interrupts = null
        super.onDestroy()
    }

    private fun onTick() {
        val now = System.currentTimeMillis()
        val foreground = foregroundWatchedApp()
        // Like the web demo: the banner and alarm go away when the user leaves the app.
        // Two ticks in a row, so a short gap while the app changes screens does not count.
        awayTicks = if (foreground == null) awayTicks + 1 else 0
        if (awayTicks >= 2) interrupts?.dismissLevel1()

        // Settings come in N10. Until then: default interval, no demo mode.
        val level = ScrollTimer.tick(
            nowMillis = now,
            shortVideoInFront = foreground != null,
            level1Seconds = Config.DEFAULT_INTERVAL_MIN * 60,
            level2DelaySeconds = Config.LEVEL2_DELAY_SECONDS,
        )
        if (level == null || foreground == null) return

        val seconds = ScrollTimer.state.value.activeSeconds
        if (level == 1) {
            InspectorLog.add(InspectorEntry(now, foreground, EventKind.LEVEL1, detail = "active=${seconds}s"))
            interrupts?.showLevel1(foreground, seconds)
        } else {
            InspectorLog.add(InspectorEntry(now, foreground, EventKind.LEVEL2, detail = "active=${seconds}s"))
            interrupts?.dismissLevel1()
            // Placeholder until the calm overlay (N9): start over.
            ScrollTimer.reset()
        }
    }

    /**
     * The watched app in front, if the screen is on. For now the whole app
     * counts; N5 narrows this to the short-video screens.
     */
    private fun foregroundWatchedApp(): String? {
        val power = getSystemService(PowerManager::class.java)
        if (power == null || !power.isInteractive) return null
        val active = rootInActiveWindow?.packageName?.toString() ?: return null
        // The user is touching our own banner: the video app is still right behind it.
        if (active == packageName) return interrupts?.bannerApp
        return if (AppMarkers.find(active) != null) active else null
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
