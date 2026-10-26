package io.github.jessesaarinen.distractionkiller.interrupt

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sin

/**
 * A soft two-note "ding" made in code, like playChime() in the web demo:
 * 660 Hz, then 880 Hz 0.18 s later. No audio files.
 *
 * Plays on the alarm volume, so it is heard over the video.
 */
class Chime {
    private val track: AudioTrack

    init {
        val samples = generate()
        track = AudioTrack.Builder()
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
        track.write(samples, 0, samples.size)
    }

    fun play() {
        if (track.state != AudioTrack.STATE_INITIALIZED) return
        // A static track replays from the start after stop() + reloadStaticData().
        track.stop()
        track.reloadStaticData()
        track.play()
    }

    fun release() {
        track.release()
    }

    private fun generate(): ShortArray {
        val buffer = FloatArray((SAMPLE_RATE * 0.8).toInt())
        addTone(buffer, frequency = 660.0, startSeconds = 0.0, durationSeconds = 0.4)
        addTone(buffer, frequency = 880.0, startSeconds = 0.18, durationSeconds = 0.6)
        return ShortArray(buffer.size) { i -> (buffer[i].coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort() }
    }

    /** A sine tone with a quick fade-in and an exponential fade-out, like the web demo's gain ramps. */
    private fun addTone(buffer: FloatArray, frequency: Double, startSeconds: Double, durationSeconds: Double) {
        val start = (startSeconds * SAMPLE_RATE).toInt()
        val length = (durationSeconds * SAMPLE_RATE).toInt()
        val attack = (0.02 * SAMPLE_RATE).toInt()
        val decayRate = ln(0.001 / PEAK) / (length - attack)
        for (n in 0 until length) {
            val i = start + n
            if (i >= buffer.size) break
            val envelope = if (n < attack) PEAK * n / attack else PEAK * exp(decayRate * (n - attack))
            buffer[i] += (envelope * sin(2 * PI * frequency * n / SAMPLE_RATE)).toFloat()
        }
    }

    private companion object {
        const val SAMPLE_RATE = 44_100
        const val PEAK = 0.4
    }
}
