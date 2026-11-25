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
import io.github.jessesaarinen.distractionkiller.AlarmSound
import io.github.jessesaarinen.distractionkiller.Config
import io.github.jessesaarinen.distractionkiller.R

/**
 * The reminder alarm on levels 1 and 2 (task N22). It rings for
 * Config.ALARM_SECONDS and then stops by itself; the banner stays until the
 * user taps it. The chime and the beep repeat with a short vibration every
 * Config.ALARM_BEAT_MS. Speech says one random spoken reminder once, and the
 * vibration repeats the same way. stop() ends it early.
 */
class Alarm(context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            context.getSystemService(Vibrator::class.java)
        }
    private val speechLines: List<String> = context.resources.getStringArray(R.array.speech_lines).toList()

    // Made right away, so the voice is ready when the first reminder comes.
    private val speaker = Speaker(context)
    private val chimes = mutableMapOf<ChimeStyle, Chime>()

    /** The sound repeated on every beat, or null when the beat only vibrates. */
    private var beatChime: Chime? = null
    private var vibration = false

    private val beat = object : Runnable {
        override fun run() {
            if (vibration) vibrateOnce()
            beatChime?.play()
            handler.postDelayed(this, Config.ALARM_BEAT_MS)
        }
    }

    // Stops the beats, but lets a spoken reminder finish its sentence.
    private val timeout = Runnable { stopBeats() }

    /** sound = null: vibration only. Nothing at all if both are off. */
    fun start(sound: AlarmSound?, vibration: Boolean) {
        stop()
        this.vibration = vibration
        beatChime = when (sound) {
            null -> null
            AlarmSound.CHIME -> chime(ChimeStyle.SOFT)
            AlarmSound.BEEP -> chime(ChimeStyle.BEEP)
            // No Finnish voice (or not ready yet): the chime instead, so the reminder is never silent.
            AlarmSound.SPEECH -> if (speaker.speak(speechLines.random())) null else chime(ChimeStyle.SOFT)
        }
        if (beatChime == null && !vibration) return
        beat.run()
        handler.postDelayed(timeout, Config.ALARM_SECONDS * 1000L)
    }

    fun stop() {
        stopBeats()
        speaker.stop()
    }

    fun release() {
        stop()
        chimes.values.forEach { it.release() }
        chimes.clear()
        speaker.release()
    }

    private fun stopBeats() {
        handler.removeCallbacks(beat)
        handler.removeCallbacks(timeout)
    }

    private fun chime(style: ChimeStyle): Chime = chimes.getOrPut(style) { Chime(style) }

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
