package io.github.jessesaarinen.distractionkiller.interrupt

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import io.github.jessesaarinen.distractionkiller.AlarmMode
import io.github.jessesaarinen.distractionkiller.Config

/**
 * The level 1 alarm, like startAlarm()/stopAlarm() in the web demo: a chime and
 * a short vibration every Config.ALARM_BEAT_MS until stop(), or for
 * Config.ALARM_TIMED_SECONDS in timed mode. Level 2 gets a single
 * notifyLevel2(), like notify(2).
 */
class Alarm(context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            context.getSystemService(Vibrator::class.java)
        }
    private var chime: Chime? = null
    private var sound = false
    private var vibration = false

    private val beat = object : Runnable {
        override fun run() {
            if (vibration) vibrateOnce()
            if (sound) chime?.play()
            handler.postDelayed(this, Config.ALARM_BEAT_MS)
        }
    }
    private val timeout = Runnable { stop() }

    fun start(sound: Boolean, vibration: Boolean, mode: AlarmMode) {
        stop()
        if (!sound && !vibration) return
        this.sound = sound
        this.vibration = vibration
        if (sound && chime == null) chime = Chime()
        beat.run()
        if (mode == AlarmMode.TIMED) {
            handler.postDelayed(timeout, Config.ALARM_TIMED_SECONDS * 1000L)
        }
    }

    /** One chime and a stronger double buzz when level 2 starts. */
    fun notifyLevel2(sound: Boolean, vibration: Boolean) {
        stop()
        if (vibration) vibrate(VibrationEffect.createWaveform(Config.LEVEL2_VIBRATION_PATTERN, -1))
        if (sound) {
            if (chime == null) chime = Chime()
            chime?.play()
        }
    }

    fun stop() {
        handler.removeCallbacks(beat)
        handler.removeCallbacks(timeout)
    }

    fun release() {
        stop()
        chime?.release()
        chime = null
    }

    private fun vibrateOnce() {
        vibrate(VibrationEffect.createOneShot(Config.ALARM_VIBRATION_MS, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun vibrate(effect: VibrationEffect) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        // Alarm usage, so Android does not drop the vibration while another app is in front.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            v.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(effect, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
        }
    }
}
