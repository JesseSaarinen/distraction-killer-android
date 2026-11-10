package io.github.jessesaarinen.distractionkiller.detection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Starts UsageWatcherService again after the phone restarts or this app is
 * updated, so the user does not have to open the app first.
 */
class StartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            UsageWatcherService.start(context)
        }
    }
}
