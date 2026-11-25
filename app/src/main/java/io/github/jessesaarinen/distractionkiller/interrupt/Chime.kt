package io.github.jessesaarinen.distractionkiller.interrupt

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import io.github.jessesaarinen.distractionkiller.detection.InspectorLog
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sin

/** SOFT: the two-note "ding". BEEP: three short, sharper beeps (task N22). */
enum class ChimeStyle { SOFT, BEEP }

/**
 * A sound made in code, no audio files. SOFT is the two-note "ding" like
 * playChime() in the web demo: 660 Hz, then 880 Hz 0.18 s later. BEEP is
 * three 1 kHz beeps with an overtone, closer to an alarm clock.
 *
 * Plays on the alarm volume, so it is heard over the video. If the phone gives
 * no audio track, the chime stays silent and the alarm only vibrates.
 */
class Chime(private val style: ChimeStyle = ChimeStyle.SOFT) {
    private val track: AudioTrack? = try {
        createTrack()
    } catch (e: RuntimeException) {
        InspectorLog.error("chime", e)
        null
    }

    fun play() {
        if (track == null || track.state != AudioTrack.STATE_INITIALIZED) return
        try {
            // A static track replays from the start after stop() + reloadStaticData().
            track.stop()
            track.reloadStaticData()
            track.play()
        } catch (e: IllegalStateException) {
            InspectorLog.error("chime", e)
        }
    }

    fun release() {
        track?.release()
    }

    private fun createTrack(): AudioTrack {
        val samples = generate()
        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(samples.size * 2)
            .build()
            .apply { write(samples, 0, samples.size) }
    }

    private fun generate(): ShortArray {
        val buffer = FloatArray((SAMPLE_RATE * 0.8).toInt())
        when (style) {
            ChimeStyle.SOFT -> {
                addTone(buffer, frequency = 660.0, startSeconds = 0.0, durationSeconds = 0.4)
                addTone(buffer, frequency = 880.0, startSeconds = 0.18, durationSeconds = 0.6)
            }
            ChimeStyle.BEEP -> for (i in 0 until 3) {
                val start = i * 0.2
                addTone(buffer, frequency = 1000.0, startSeconds = start, durationSeconds = 0.12)
                addTone(buffer, frequency = 3000.0, startSeconds = start, durationSeconds = 0.12, peak = PEAK / 3)
            }
        }
        return ShortArray(buffer.size) { i -> (buffer[i].coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort() }
    }

    /** A sine tone with a quick fade-in and an exponential fade-out, like the web demo's gain ramps. */
    private fun addTone(
        buffer: FloatArray,
        frequency: Double,
        startSeconds: Double,
        durationSeconds: Double,
        peak: Double = PEAK,
    ) {
        val start = (startSeconds * SAMPLE_RATE).toInt()
        val length = (durationSeconds * SAMPLE_RATE).toInt()
        val attack = (0.02 * SAMPLE_RATE).toInt()
        val decayRate = ln(0.001 / peak) / (length - attack)
        for (n in 0 until length) {
            val i = start + n
            if (i >= buffer.size) break
            val envelope = if (n < attack) peak * n / attack else peak * exp(decayRate * (n - attack))
            buffer[i] += (envelope * sin(2 * PI * frequency * n / SAMPLE_RATE)).toFloat()
        }
    }

    private companion object {
        const val SAMPLE_RATE = 44_100
        const val PEAK = 0.4
    }
}
