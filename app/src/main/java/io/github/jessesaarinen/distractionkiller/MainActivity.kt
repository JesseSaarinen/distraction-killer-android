package io.github.jessesaarinen.distractionkiller

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
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
import io.github.jessesaarinen.distractionkiller.detection.ScrollWatcherService
import io.github.jessesaarinen.distractionkiller.ui.HomeScreen
import io.github.jessesaarinen.distractionkiller.ui.InspectorScreen
import io.github.jessesaarinen.distractionkiller.ui.SettingsScreen
import io.github.jessesaarinen.distractionkiller.ui.StatsScreen
import io.github.jessesaarinen.distractionkiller.ui.theme.DistractionKillerTheme

/** The app's screens. Switching is done here, no navigation library. */
enum class Screen { HOME, SETTINGS, STATS, INSPECTOR }

class MainActivity : ComponentActivity() {

    // Checked again every time the app comes back from the phone's settings.
    private var serviceEnabled by mutableStateOf(false)
    private var batteryUnrestricted by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DistractionKillerTheme {
                var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
                when (screen) {
                    Screen.HOME -> HomeScreen(
                        serviceEnabled = serviceEnabled,
                        batteryUnrestricted = batteryUnrestricted,
                        onOpenAccessibilitySettings = ::openAccessibilitySettings,
                        onOpenAppInfo = ::openAppInfo,
                        onOpenSettings = { screen = Screen.SETTINGS },
                        onOpenStats = { screen = Screen.STATS },
                        onOpenInspector = { screen = Screen.INSPECTOR },
                    )
                    Screen.SETTINGS -> {
                        BackHandler { screen = Screen.HOME }
                        SettingsScreen()
                    }
                    Screen.STATS -> {
                        BackHandler { screen = Screen.HOME }
                        StatsScreen(onShare = ::shareEventLog)
                    }
                    Screen.INSPECTOR -> {
                        BackHandler { screen = Screen.HOME }
                        InspectorScreen(
                            serviceEnabled = serviceEnabled,
                            onShare = ::shareInspectorLog,
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        serviceEnabled = isServiceEnabled()
        batteryUnrestricted = isBatteryUnrestricted()
    }

    private fun isServiceEnabled(): Boolean {
        val manager = getSystemService(AccessibilityManager::class.java) ?: return false
        return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK).any {
            val info = it.resolveInfo.serviceInfo
            info.packageName == packageName && info.name == ScrollWatcherService::class.java.name
        }
    }

    // "Unrestricted" battery use, so Samsung does not stop the service in the background.
    private fun isBatteryUnrestricted(): Boolean {
        val power = getSystemService(PowerManager::class.java) ?: return false
        return power.isIgnoringBatteryOptimizations(packageName)
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    // "Allow restricted settings" (Android 13+) and the battery setting live in the app info.
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
