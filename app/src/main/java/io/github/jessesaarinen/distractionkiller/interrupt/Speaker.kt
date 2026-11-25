package io.github.jessesaarinen.distractionkiller.interrupt

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import io.github.jessesaarinen.distractionkiller.detection.InspectorLog
import java.util.Locale

/**
 * Says a spoken reminder in Finnish with the phone's own text-to-speech
 * (task N22). The voice comes from the phone's speech engine, so the app needs
 * no audio files and no internet.
 *
 * Getting ready takes a moment after the start. Until then, or if the phone
 * has no Finnish voice, speak() returns false and the alarm plays the chime
 * instead. The inspector tells why.
 */
class Speaker(context: Context) {
    private var engine: TextToSpeech? = null
    private var ready = false

    init {
        try {
            // Google's engine has a Finnish voice; Samsung's own engine may not. If Google's
            // is not installed, Android uses the phone's default engine instead.
            engine = TextToSpeech(context.applicationContext, { status -> onInit(status) }, GOOGLE_ENGINE)
        } catch (e: RuntimeException) {
            InspectorLog.error("speech", e)
        }
    }

    private fun onInit(status: Int) {
        val tts = engine
        if (tts == null || status != TextToSpeech.SUCCESS) {
            InspectorLog.note("speech: no text-to-speech engine on the phone")
            return
        }
        val result = tts.setLanguage(FINNISH)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            InspectorLog.note("speech: no Finnish voice, the chime plays instead")
            return
        }
        // Alarm volume, like the chime, so it is heard over the video.
        tts.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
        ready = true
    }

    /** Says the text. false = not ready or no Finnish voice. */
    fun speak(text: String): Boolean {
        val tts = engine ?: return false
        if (!ready) return false
        return tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID) == TextToSpeech.SUCCESS
    }

    fun stop() {
        engine?.stop()
    }

    fun release() {
        engine?.shutdown()
        engine = null
        ready = false
    }

    private companion object {
        val FINNISH: Locale = Locale.forLanguageTag("fi-FI")
        const val UTTERANCE_ID = "reminder"
        const val GOOGLE_ENGINE = "com.google.android.tts"
    }
}
