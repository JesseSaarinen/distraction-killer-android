package io.github.jessesaarinen.distractionkiller.detection

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings

/** The special permissions the user grants in the phone's settings. */
object Permissions {
    /** "Usage access": lets the service see which app is in front. */
    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** "Display over other apps": the banner, and opening the break screen from the background. */
    fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)

    /** "Unrestricted" battery use, so Samsung does not stop the service in the background. */
    fun isBatteryUnrestricted(context: Context): Boolean {
        val power = context.getSystemService(PowerManager::class.java) ?: return false
        return power.isIgnoringBatteryOptimizations(context.packageName)
    }
}
