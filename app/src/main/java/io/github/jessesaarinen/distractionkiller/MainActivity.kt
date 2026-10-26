package io.github.jessesaarinen.distractionkiller

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import io.github.jessesaarinen.distractionkiller.detection.InspectorLog
import io.github.jessesaarinen.distractionkiller.detection.ScrollWatcherService
import io.github.jessesaarinen.distractionkiller.ui.HomeScreen
import io.github.jessesaarinen.distractionkiller.ui.InspectorScreen
import io.github.jessesaarinen.distractionkiller.ui.theme.DistractionKillerTheme

/** The app's screens. Switching is done here, no navigation library. */
enum class Screen { HOME, INSPECTOR }

class MainActivity : ComponentActivity() {

    // Checked again every time the app comes back from the settings.
    private var serviceEnabled by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DistractionKillerTheme {
                var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
                when (screen) {
                    Screen.HOME -> HomeScreen(
                        serviceEnabled = serviceEnabled,
                        onOpenAccessibilitySettings = ::openAccessibilitySettings,
                        onOpenAppInfo = ::openAppInfo,
                        onOpenInspector = { screen = Screen.INSPECTOR },
                    )
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
    }

    private fun isServiceEnabled(): Boolean {
        val manager = getSystemService(AccessibilityManager::class.java) ?: return false
        return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK).any {
            val info = it.resolveInfo.serviceInfo
            info.packageName == packageName && info.name == ScrollWatcherService::class.java.name
        }
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    // "Allow restricted settings" lives in the app info menu (Android 13+).
    private fun openAppInfo() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
    }

    private fun shareInspectorLog() {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, InspectorLog.toShareText())
        }
        startActivity(Intent.createChooser(send, getString(R.string.inspector_share_title)))
    }
}
