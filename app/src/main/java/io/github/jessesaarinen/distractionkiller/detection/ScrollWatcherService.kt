package io.github.jessesaarinen.distractionkiller.detection

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import io.github.jessesaarinen.distractionkiller.data.SettingsStore
import io.github.jessesaarinen.distractionkiller.interrupt.InterruptController

/**
 * Watches TikTok, Instagram and YouTube through Android's accessibility API.
 * Reads only the app's package name, view IDs, class names and scroll positions.
 * Never reads text, messages or video content.
 *
 * ShortVideoClassifier (N5) decides which scrolls are swipes in the
 * short-video feed. They feed the ScrollTimer (N6). Level 1 shows the banner
 * (N7) and level 2 the calm full screen (N9).
 */
class ScrollWatcherService : AccessibilityService() {

    private val lastSnapshotAt = mutableMapOf<String, Long>()
    private var interrupts: InterruptController? = null

    /** Ticks in a row with no watched app in front. */
    private var awayTicks = 0

    /**
     * The app whose short videos are in front. A swipe in the feed or the
     * player on screen sets it. A scroll somewhere else in that app, like
     * Instagram's home feed, or the player leaving the screen clears it.
     */
    private var shortVideoApp: String? = null

    /** The feed item on screen at the last player check, like "item 12", and when that was. */
    private var feedItem: String? = null
    private var feedItemAt = 0L

    /** How the last player check found the feed items. A change shows in the inspector. */
    private var feedHow: String? = null

    private val handler = Handler(Looper.getMainLooper())
    private val tickRunnable = object : Runnable {
        override fun run() {
            try {
                onTick()
            } catch (e: Exception) {
                // A bug must not stop the service. The error shows in the inspector.
                InspectorLog.error("tick", e)
            }
            handler.postDelayed(this, TICK_MS)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        InspectorLog.setServiceRunning(true)
        InspectorLog.note("service started")
        // If Android connects the service again, start over instead of running two tickers.
        handler.removeCallbacks(tickRunnable)
        interrupts?.release()
        interrupts = InterruptController(this)
        handler.postDelayed(tickRunnable, TICK_MS)
    }

    override fun onDestroy() {
        handler.removeCallbacks(tickRunnable)
        interrupts?.release()
        interrupts = null
        InspectorLog.setServiceRunning(false)
        InspectorLog.note("service stopped")
        super.onDestroy()
    }

    private fun onTick() {
        val now = System.currentTimeMillis()
        val root = activeRoot()
        val foreground = foregroundWatchedApp(root)
        // Like the web demo: the banner and alarm go away when the user leaves the app.
        // Two ticks in a row, so a short gap while the app changes screens does not count.
        awayTicks = if (foreground == null) awayTicks + 1 else 0
        if (awayTicks >= 2) interrupts?.dismissLevel1()
        if (root != null && foreground != null) checkPlayer(now, root, foreground)

        val settings = SettingsStore.settings.value
        if (!settings.interruptionsOn) {
            // Paused in the settings: nothing counts, and turning it back on starts from zero.
            ScrollTimer.reset()
            return
        }
        val level = ScrollTimer.tick(
            nowMillis = now,
            shortVideoInFront = foreground != null && foreground == shortVideoApp,
            level1Seconds = settings.level1Seconds,
            level2DelaySeconds = settings.level2DelaySeconds,
        )
        if (level == null || foreground == null) return

        val seconds = ScrollTimer.state.value.activeSeconds
        if (level == 1) {
            InspectorLog.add(InspectorEntry(now, foreground, EventKind.LEVEL1, detail = "active=${seconds}s"))
            interrupts?.showLevel1(foreground, seconds)
        } else {
            InspectorLog.add(InspectorEntry(now, foreground, EventKind.LEVEL2, detail = "active=${seconds}s"))
            interrupts?.showLevel2(foreground, seconds)
        }
    }

    /** The root of the window the user is in, or null when the screen is off. */
    private fun activeRoot(): AccessibilityNodeInfo? {
        val power = getSystemService(PowerManager::class.java)
        if (power == null || !power.isInteractive) return null
        return rootInActiveWindow
    }

    /** The watched app in front. */
    private fun foregroundWatchedApp(root: AccessibilityNodeInfo?): String? {
        val active = root?.packageName?.toString() ?: return null
        // The user is touching our own banner: the video app is still right behind it.
        if (active == packageName) return interrupts?.bannerApp
        return if (isWatched(active)) active else null
    }

    /**
     * For an app with player views (YouTube Shorts), the player on screen
     * decides whether the short videos are in front.
     */
    private fun checkPlayer(now: Long, root: AccessibilityNodeInfo, packageName: String) {
        val app = AppMarkers.find(packageName) ?: return
        if (app.playerIds.isEmpty()) return
        // While the user touches our banner, the app's own window is not the active one.
        if (root.packageName?.toString() != packageName) return
        val visibleIds = app.playerIds.filter { id ->
            root.findAccessibilityNodeInfosByViewId("$packageName:id/$id").any { it.isVisibleToUser }
        }
        val onScreen = ShortVideoClassifier.isPlayerOnScreen(packageName, visibleIds) == true
        setInFront(now, packageName, onScreen, if (onScreen) "player on screen" else "player not on screen")
        checkFeedItem(now, root, app, onScreen)
    }

    /**
     * Notes when a new item of the feed is on screen. That is a swipe even if
     * the app sent no scroll event for it, which YouTube's lists may not do.
     */
    private fun checkFeedItem(now: Long, root: AccessibilityNodeInfo, app: WatchedApp, playerOnScreen: Boolean) {
        val look = if (playerOnScreen) lookAtFeed(root, app) else null
        // Once per visit to the player, so a log without ITEM swipes still tells why.
        if (look != null && look.how != feedHow) {
            InspectorLog.add(InspectorEntry(now, app.packageName, EventKind.ITEM, detail = "feed: ${look.how}"))
        }
        feedHow = look?.how
        val item = look?.item
        // After a gap (another app, screen off, our banner) there is nothing to compare with.
        val previous = if (now - feedItemAt <= 2 * TICK_MS) feedItem else null
        feedItem = item
        feedItemAt = now
        if (!ShortVideoClassifier.isNewItem(previous, item)) return
        ScrollTimer.onSwipe(now)
        InspectorLog.add(
            InspectorEntry(now, app.packageName, EventKind.ITEM, detail = "$item → swipe (new item on screen)")
        )
    }

    /** The feed item on screen at a player check, or null, and how the check found it. */
    private class FeedLook(val item: String?, val how: String)

    /**
     * The feed item that fills most of the screen, as a key that changes with
     * every swipe: its place in the list if the list tells it, otherwise the view.
     */
    private fun lookAtFeed(root: AccessibilityNodeInfo, app: WatchedApp): FeedLook {
        val feed = app.swipeIds
            .flatMap { root.findAccessibilityNodeInfosByViewId("${app.packageName}:id/$it") }
            .firstOrNull { it.isVisibleToUser }
            ?: return FeedLook(null, "${app.swipeIds.joinToString()} not on screen")
        if (feed.childCount == 0) return FeedLook(null, "the list shows no items")
        val feedBounds = Rect()
        feed.getBoundsInScreen(feedBounds)
        var best: AccessibilityNodeInfo? = null
        var bestHeight = 0
        for (i in 0 until feed.childCount) {
            val child = feed.getChild(i) ?: continue
            if (!child.isVisibleToUser) continue
            val bounds = Rect()
            child.getBoundsInScreen(bounds)
            if (!bounds.intersect(feedBounds) || bounds.height() <= bestHeight) continue
            best = child
            bestHeight = bounds.height()
        }
        val item = best ?: return FeedLook(null, "no item on screen")
        val row = item.collectionItemInfo?.rowIndex ?: -1
        return if (row >= 0) {
            FeedLook("item $row", "items by their place in the list")
        } else {
            FeedLook("view ${item.hashCode()}", "items by view, the list does not tell their place")
        }
    }

    /** Notes whether this app's short videos are in front. Changes show in the inspector. */
    private fun setInFront(now: Long, packageName: String, inFront: Boolean, reason: String) {
        val new = when {
            inFront -> packageName
            shortVideoApp == packageName -> null
            else -> shortVideoApp
        }
        if (new == shortVideoApp) return
        shortVideoApp = new
        InspectorLog.setInFront(new)
        val state = if (inFront) "short videos in front" else "short videos not in front"
        InspectorLog.add(InspectorEntry(now, packageName, EventKind.FRONT, detail = "$state ($reason)"))
    }

    /** A watched app that the user has not turned off in the settings. */
    private fun isWatched(packageName: String): Boolean {
        val app = AppMarkers.find(packageName) ?: return false
        return app.label in SettingsStore.settings.value.watchedApps
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        try {
            handleEvent(event)
        } catch (e: Exception) {
            // A bug must not stop the service. The error shows in the inspector.
            InspectorLog.error("event", e)
        }
    }

    private fun handleEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        if (!isWatched(packageName)) return

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
        val viewId = event.source?.viewIdResourceName?.let(::shortId)
        var detail: String? = null
        if (kind == EventKind.SCROLLED) {
            // The verdict shows which scrolls count, so the markers can be checked in the inspector.
            val result = onScroll(now, packageName, viewId, event)
            detail = scrollDetail(event) + " → ${result.verdict.name.lowercase()} (${result.reason})"
        }
        InspectorLog.add(
            InspectorEntry(
                timeMillis = now,
                packageName = packageName,
                kind = kind,
                viewId = viewId,
                className = event.className?.toString(),
                detail = detail,
            )
        )
        takeSnapshot(now, packageName)
    }

    override fun onInterrupt() {
        // Nothing to stop: this service gives no spoken or haptic feedback.
    }

    /** Asks the classifier what the scroll means, and tells the timer about swipes. */
    private fun onScroll(now: Long, packageName: String, viewId: String?, event: AccessibilityEvent): Classification {
        // Before Android 9 the event does not tell how far the view moved.
        val result = ShortVideoClassifier.classifyScroll(
            packageName = packageName,
            viewId = viewId,
            dx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) event.scrollDeltaX else null,
            dy = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) event.scrollDeltaY else null,
        )
        when (result.verdict) {
            ScrollVerdict.SWIPE -> {
                setInFront(now, packageName, inFront = true, reason = "swipe")
                ScrollTimer.onSwipe(now)
            }
            ScrollVerdict.OTHER -> {
                // In an app with player views, the player check decides instead.
                if (AppMarkers.find(packageName)?.playerIds.isNullOrEmpty()) {
                    setInFront(now, packageName, inFront = false, reason = "scrolled ${viewId ?: "a view without an ID"}")
                }
            }
            ScrollVerdict.LAYOUT, ScrollVerdict.IGNORED -> Unit
        }
        return result
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
            if (event.scrollDeltaX != 0) parts.add("dx=${event.scrollDeltaX}")
        }
        return parts.joinToString(" ")
    }

    private companion object {
        const val TICK_MS = 1_000L
        const val SNAPSHOT_INTERVAL_MS = 2_000L
        const val MAX_NODES = 1_000
    }
}
