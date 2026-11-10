package io.github.jessesaarinen.distractionkiller

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.github.jessesaarinen.distractionkiller.data.EventLog
import io.github.jessesaarinen.distractionkiller.data.SettingsStore
import io.github.jessesaarinen.distractionkiller.detection.InspectorLog
import io.github.jessesaarinen.distractionkiller.detection.Permissions
import io.github.jessesaarinen.distractionkiller.detection.UsageWatcherService
import io.github.jessesaarinen.distractionkiller.ui.HomeScreen
import io.github.jessesaarinen.distractionkiller.ui.InspectorScreen
import io.github.jessesaarinen.distractionkiller.ui.SettingsScreen
import io.github.jessesaarinen.distractionkiller.ui.StatsScreen
import io.github.jessesaarinen.distractionkiller.ui.theme.DistractionKillerTheme

/** The app's screens. Switching is done here, no navigation library. */
enum class Screen { HOME, SETTINGS, STATS, INSPECTOR }

class MainActivity : ComponentActivity() {

    // Checked again every time the app comes back from the phone's settings.
    private var usageAccess by mutableStateOf(false)
    private var overlayAllowed by mutableStateOf(false)
    private var batteryUnrestricted by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DistractionKillerTheme {
                var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
                when (screen) {
                    Screen.HOME -> HomeScreen(
                        usageAccess = usageAccess,
                        overlayAllowed = overlayAllowed,
                        batteryUnrestricted = batteryUnrestricted,
                        onOpenUsageAccess = ::openUsageAccessSettings,
                        onOpenOverlay = ::openOverlaySettings,
                        onOpenAppInfo = ::openAppInfo,
                        onOpenSettings = { screen = Screen.SETTINGS },
                        onOpenStats = { screen = Screen.STATS },
                        onOpenInspector = { screen = Screen.INSPECTOR },
                    )
                    Screen.SETTINGS -> {
                        BackHandler { screen = Screen.HOME }
                        SettingsScreen(onBack = { screen = Screen.HOME })
                    }
                    Screen.STATS -> {
                        BackHandler { screen = Screen.HOME }
                        StatsScreen(onShare = ::shareEventLog, onBack = { screen = Screen.HOME })
                    }
                    Screen.INSPECTOR -> {
                        BackHandler { screen = Screen.HOME }
                        InspectorScreen(
                            serviceEnabled = usageAccess,
                            onShare = ::shareInspectorLog,
                            onBack = { screen = Screen.HOME },
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        usageAccess = Permissions.hasUsageAccess(this)
        overlayAllowed = Permissions.canDrawOverlays(this)
        batteryUnrestricted = Permissions.isBatteryUnrestricted(this)
        // Starts the watcher once usage access is granted. Does nothing while it already runs.
        UsageWatcherService.start(this)
    }

    private fun openUsageAccessSettings() {
        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    private fun openOverlaySettings() {
        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.fromParts("package", packageName, null)))
    }

    // The battery setting, and "Allow restricted settings" if the phone asks for it, live in the app info.
    private fun openAppInfo() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
    }

    private fun shareEventLog() {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.stats_share_subject))
            putExtra(Intent.EXTRA_TEXT, EventLog.exportJson(SettingsStore.settings.value))
        }
        startActivity(Intent.createChooser(send, getString(R.string.stats_share_title)))
    }

    private fun shareInspectorLog() {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, InspectorLog.toShareText(SettingsStore.settings.value))
        }
        startActivity(Intent.createChooser(send, getString(R.string.inspector_share_title)))
    }
}
