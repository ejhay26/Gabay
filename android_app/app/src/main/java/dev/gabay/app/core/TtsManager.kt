package dev.gabay.app.core

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class TtsManager(context: Context) : TextToSpeech.OnInitListener {

    private val tag = "GabayTTS"
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var pendingUtterance: String? = null

    init {
        try {
            // Prefer Google TTS engine if installed, otherwise system default
            tts = TextToSpeech(context.applicationContext, this, "com.google.android.tts")
        } catch (e: Exception) {
            Log.w(tag, "Google TTS unavailable, falling back to default engine: ${e.message}")
            tts = TextToSpeech(context.applicationContext, this)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let { engine ->
                // Try en-PH first, fallback to en-US or default
                val phLocale = Locale("en", "PH")
                val result = engine.setLanguage(phLocale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    engine.language = Locale.US
                }
                engine.setSpeechRate(1.0f)
                engine.setPitch(1.0f)

                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        Log.d(tag, "TTS started: $utteranceId")
                    }

                    override fun onDone(utteranceId: String?) {
                        Log.d(tag, "TTS finished: $utteranceId")
                    }

                    override fun onError(utteranceId: String?) {
                        Log.e(tag, "TTS error on utterance: $utteranceId")
                    }
                })

                isInitialized = true
                Log.i(tag, "Google Built-in TTS initialized successfully.")

                pendingUtterance?.let {
                    speak(it, flush = true)
                    pendingUtterance = null
                }
            }
        } else {
            Log.e(tag, "TextToSpeech init failed with code: $status")
        }
    }

    /**
     * Speaks the given instruction or advisory text.
     * @param text The text to vocalize
     * @param flush If true, interrupts current speech for high-priority navigation instructions
     */
    fun speak(text: String, flush: Boolean = true) {
        if (!isInitialized) {
            pendingUtterance = text
            return
        }

        val queueMode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        val utteranceId = "gabay_${System.currentTimeMillis()}"
        tts?.speak(text, queueMode, null, utteranceId)
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
        } catch (e: Exception) {
            Log.w(tag, "Error shutting down TTS: ${e.message}")
        }
    }
}
