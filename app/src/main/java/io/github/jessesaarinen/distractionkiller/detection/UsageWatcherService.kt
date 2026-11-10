package io.github.jessesaarinen.distractionkiller.detection

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import io.github.jessesaarinen.distractionkiller.MainActivity
import io.github.jessesaarinen.distractionkiller.R
import io.github.jessesaarinen.distractionkiller.data.SettingsStore
import io.github.jessesaarinen.distractionkiller.interrupt.InterruptController
import java.time.LocalDate
import java.time.ZoneId

/**
 * Counts the time in the watched apps. Once a second, while the screen is on
 * and unlocked, it reads Android's usage events (UsageStatsManager) to know
 * which app is in front, and gives that second to UsageTimer. When an app's
 * daily time is used up, InterruptController shows the reminders.
 *
 * Needs the "usage access" permission. It sees only which app is in front and
 * when, never what is on the screen. A foreground service, so Android keeps it
 * running. That is why the phone shows a notification while it runs.
 */
class UsageWatcherService : Service() {

    private var interrupts: InterruptController? = null
    private val tracker = ForegroundTracker()

    /** The events up to here are read already. */
    private var readUntilMillis = 0L

    /** Ticks in a row with no watched app in front. */
    private var awayTicks = 0

    /** What the inspector last showed as the app in front. */
    private var shownFront: String? = "start"

    /** The watched apps when today's times were last read from Android, null before the first time. */
    private var seededApps: Set<String>? = null
    private var seededWhileOn = false

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

    override fun onCreate() {
        super.onCreate()
        startInForeground()
        InspectorLog.setServiceRunning(true)
        InspectorLog.note("service started")
        interrupts = InterruptController(this)
        handler.post(tickRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

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
        val settings = SettingsStore.settings.value
        val watchedNow = settings.apps.map { it.packageName }.toSet()
        // Read today's time again from Android when the service starts, when an
        // app is added, and when the user turns the interruptions back on.
        if (watchedNow != seededApps || (settings.interruptionsOn && !seededWhileOn)) {
            seed(now, watchedNow)
            seededApps = watchedNow
            seededWhileOn = settings.interruptionsOn
        } else {
            readEvents(readUntilMillis, now).forEach(tracker::onEvent)
            readUntilMillis = now
        }
        if (!settings.interruptionsOn) seededWhileOn = false

        val front = tracker.app.takeIf { screenInUse() }
        val watched = front?.let { settings.watchedApp(it) }
        showFront(front, watched?.label)
        // Like the web demo: the banner and alarm go away when the user leaves the app.
        // Two ticks in a row, so a short gap while the app changes screens does not count.
        awayTicks = if (watched == null) awayTicks + 1 else 0
        if (awayTicks >= 2) interrupts?.dismissLevel1()
        if (watched == null || !settings.interruptionsOn) return

        val level = UsageTimer.tick(
            day = LocalDate.now().toEpochDay(),
            packageName = watched.packageName,
            allowanceSeconds = settings.allowanceSeconds(watched),
            intervalSeconds = settings.intervalSeconds,
            level2DelaySeconds = settings.level2DelaySeconds,
        ) ?: return

        val used = UsageTimer.state.value.usage(watched.packageName).usedSeconds
        if (level == 1) {
            InspectorLog.add(InspectorEntry(now, watched.label, EventKind.LEVEL1, "used today ${used}s"))
            interrupts?.showLevel1(watched.packageName, used)
        } else {
            InspectorLog.add(InspectorEntry(now, watched.label, EventKind.LEVEL2, "used today ${used}s"))
            interrupts?.showLevel2(watched.packageName, used)
        }
    }

    /**
     * Reads today's events again: who is in front now, and how long each
     * watched app has been in front since midnight (or since the last reset).
     */
    private fun seed(now: Long, watched: Set<String>) {
        val midnight = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val from = maxOf(midnight, UsageTimer.countFromMillis)
        // A little before, so the app that was already in front at the start is known.
        val events = readEvents(from - SEED_LOOKBACK_MS, now)
        events.forEach(tracker::onEvent)
        readUntilMillis = now
        val used = secondsInFront(events, from, now).filterKeys { it in watched }
        UsageTimer.seed(LocalDate.now().toEpochDay(), used)
    }

    // ACTIVITY_RESUMED and ACTIVITY_PAUSED are Android 10 names for the same numbers
    // as the older MOVE_TO_FOREGROUND and MOVE_TO_BACKGROUND, so they work on every version.
    @SuppressLint("InlinedApi")
    private fun readEvents(fromMillis: Long, untilMillis: Long): List<AppEvent> {
        val usage = getSystemService(UsageStatsManager::class.java) ?: return emptyList()
        // Without usage access this returns nothing, and nothing is counted.
        val events = usage.queryEvents(fromMillis, untilMillis) ?: return emptyList()
        val list = mutableListOf<AppEvent>()
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val type = when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> AppEventType.RESUMED
                UsageEvents.Event.ACTIVITY_PAUSED -> AppEventType.PAUSED
                UsageEvents.Event.SCREEN_NON_INTERACTIVE, UsageEvents.Event.KEYGUARD_SHOWN -> AppEventType.SCREEN_OFF
                else -> continue
            }
            list.add(AppEvent(event.timeStamp, event.packageName.orEmpty(), type))
        }
        return list
    }

    /** The screen is on and unlocked. */
    private fun screenInUse(): Boolean {
        val power = getSystemService(PowerManager::class.java) ?: return false
        val keyguard = getSystemService(KeyguardManager::class.java)
        return power.isInteractive && keyguard?.isKeyguardLocked != true
    }

    /** Lists a change of the app in front. Only watched apps by name, others as "another app". */
    private fun showFront(front: String?, watchedLabel: String?) {
        val shown = when {
            front == null -> null
            front == packageName -> "Distraction Killer"
            else -> watchedLabel ?: OTHER_APP
        }
        if (shown == shownFront) return
        shownFront = shown
        InspectorLog.setInFront(shown)
        val detail = if (shown == null) "no app in front (screen off, locked or switching)" else "in front"
        InspectorLog.add(InspectorEntry(System.currentTimeMillis(), shown ?: "-", EventKind.FRONT, detail))
    }

    private fun startInForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.notification_channel), NotificationManager.IMPORTANCE_MIN)
        )
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_eye)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setContentIntent(open)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    companion object {
        private const val TICK_MS = 1_000L
        private const val SEED_LOOKBACK_MS = 60 * 60 * 1000L
        private const val CHANNEL_ID = "watcher"
        private const val NOTIFICATION_ID = 1
        private const val OTHER_APP = "another app"

        /** Starts the service if usage access is granted. Safe to call again while it runs. */
        fun start(context: Context) {
            if (!Permissions.hasUsageAccess(context)) return
            try {
                ContextCompat.startForegroundService(context, Intent(context, UsageWatcherService::class.java))
            } catch (e: RuntimeException) {
                // Android 12+ refuses this from the background in some cases. The app tries again when opened.
                InspectorLog.error("start service", e)
            }
        }
    }
}
