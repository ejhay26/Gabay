package dev.gabay.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import android.media.SoundPool
import kotlin.math.sin

enum class AudioGuidanceMode {
    FULL_VOICE,   // Speaks turn maneuvers, distance cues, road names, and co-pilot responses
    ALERTS_ONLY,  // Only plays chime and speaks critical hazard/turn alerts (<80m), silences chat
    MUTED         // Completely silent
}

enum class VoiceEngineType {
    TACTICAL_EXTERNAL, // External offline tactical voice & synthesized cockpit chimes
    SYSTEM_TTS         // Android system TTS engine
}

/**
 * Offline Audio Controller with Audio Focus Ducking.
 * Prioritizes terse rally-cockpit voice navigation cues while smoothly ducking background audio.
 * Supports external offline voice audio pack (WAV assets), synthesized chimes, and system TTS.
 */
class AudioController(private val context: Context) : TextToSpeech.OnInitListener {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var audioFocusRequest: AudioFocusRequest? = null

    // External Offline Voice Pack Engine (SoundPool)
    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
        .build()

    private val voiceSoundMap = mutableMapOf<String, Int>()
    private var isVoicePackLoaded = false

    private val prefs = context.getSharedPreferences("gabay_audio_prefs", Context.MODE_PRIVATE)

    var guidanceMode: AudioGuidanceMode = AudioGuidanceMode.valueOf(
        prefs.getString("guidance_mode", AudioGuidanceMode.FULL_VOICE.name) ?: AudioGuidanceMode.FULL_VOICE.name
    )
        set(value) {
            field = value
            prefs.edit().putString("guidance_mode", value.name).apply()
            if (value == AudioGuidanceMode.MUTED) {
                tts?.stop()
                abandonAudioFocus()
            }
        }

    var voiceEngine: VoiceEngineType = VoiceEngineType.valueOf(
        prefs.getString("voice_engine", VoiceEngineType.TACTICAL_EXTERNAL.name) ?: VoiceEngineType.TACTICAL_EXTERNAL.name
    )
        set(value) {
            field = value
            prefs.edit().putString("voice_engine", value.name).apply()
        }

    // Debounce state to eliminate spamming
    private var lastCueTimeMs = 0L
    private var lastCueText = ""
    private val MIN_REPEAT_INTERVAL_MS = 10000L // 10s repeat suppression

    init {
        tts = TextToSpeech(context.applicationContext, this)
        loadExternalVoicePack()
    }

    private fun loadExternalVoicePack() {
        Thread {
            try {
                val voiceFiles = listOf(
                    "turn_left.wav", "turn_right.wav", "sharp_left.wav", "sharp_right.wav",
                    "slight_left.wav", "slight_right.wav", "u_turn.wav", "straight.wav",
                    "arrived.wav", "rerouting.wav", "route_ready.wav", "hazard_ahead.wav",
                    "speed_warning.wav", "in_50m.wav", "in_100m.wav", "in_200m.wav",
                    "in_500m.wav", "in_1km.wav", "gps_acquired.wav", "gps_lost.wav", "where_am_i.wav"
                )
                val am = context.assets
                for (fileName in voiceFiles) {
                    try {
                        val afd = am.openFd("voice/$fileName")
                        val soundId = soundPool.load(afd, 1)
                        val key = fileName.removeSuffix(".wav")
                        voiceSoundMap[key] = soundId
                    } catch (e: Exception) {
                        Log.w("GabayAudio", "Could not load voice asset $fileName: ${e.message}")
                    }
                }
                isVoicePackLoaded = true
                Log.d("GabayAudio", "External offline voice pack loaded: ${voiceSoundMap.size} cues.")
            } catch (e: Exception) {
                Log.w("GabayAudio", "Error loading voice pack: ${e.message}")
            }
        }.start()
    }

    fun playVoiceAsset(key: String): Boolean {
        val soundId = voiceSoundMap[key] ?: return false
        requestAudioFocus()
        soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f)
        // Automatically release audio focus after sound completes
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            abandonAudioFocus()
        }, 2200L)
        return true
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isTtsReady = true
                tts?.setSpeechRate(1.05f) // Crisp, slightly brisk cockpit pace
                tts?.setPitch(1.0f)

                // Select highest quality available local voice
                try {
                    val voices = tts?.voices
                    val bestVoice = voices?.filter {
                        it.locale.language == "en" && !it.isNetworkConnectionRequired
                    }?.maxByOrNull { it.quality }
                    if (bestVoice != null) {
                        tts?.voice = bestVoice
                    }
                } catch (e: Exception) {}

                setupUtteranceListener()
                Log.d("GabayAudio", "Offline AudioController initialized.")
            }
        }
    }

    private fun setupUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                abandonAudioFocus()
            }
            override fun onError(utteranceId: String?) {
                abandonAudioFocus()
            }
        })
    }

    private fun requestAudioFocus(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(false)
                .build()

            audioFocusRequest = request
            return audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            return audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_NOTIFICATION,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    /**
     * Synthesizes and plays a zero-latency PCM tactical audio chime.
     * 100% offline, zero disk assets required.
     */
    fun playTacticalChime(freq1: Double = 880.0, freq2: Double = 1760.0, durationMs: Int = 180) {
        if (guidanceMode == AudioGuidanceMode.MUTED) return

        Thread {
            try {
                val sampleRate = 44100
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                val buffer = ShortArray(numSamples)
                val half = numSamples / 2

                for (i in 0 until numSamples) {
                    val freq = if (i < half) freq1 else freq2
                    val angle = 2.0 * Math.PI * i / (sampleRate / freq)
                    // Apply linear attack and decay envelope to prevent popping clicks
                    val envelope = if (i < 200) i / 200.0 else if (i > numSamples - 200) (numSamples - i) / 200.0 else 1.0
                    buffer[i] = (sin(angle) * 32767.0 * 0.7 * envelope).toInt().toShort()
                }

                requestAudioFocus()
                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()
                Thread.sleep(durationMs.toLong() + 50L)
                audioTrack.stop()
                audioTrack.release()
                if (guidanceMode != AudioGuidanceMode.FULL_VOICE) {
                    abandonAudioFocus()
                }
            } catch (e: Exception) {
                Log.w("GabayAudio", "Tactical chime error: ${e.message}")
            }
        }.start()
    }

    private fun matchVoiceKey(cue: String): String? {
        val lower = cue.lowercase()
        return when {
            "sharp left" in lower -> "sharp_left"
            "sharp right" in lower -> "sharp_right"
            "slight left" in lower || "keep left" in lower -> "slight_left"
            "slight right" in lower || "keep right" in lower -> "slight_right"
            "turn left" in lower -> "turn_left"
            "turn right" in lower -> "turn_right"
            "u-turn" in lower || "uturn" in lower -> "u_turn"
            "straight" in lower || "continue" in lower -> "straight"
            "arrived" in lower || "destination" in lower -> "arrived"
            "recalculat" in lower || "off-route" in lower || "off route" in lower -> "rerouting"
            "route calculated" in lower || "route ready" in lower || "route verified" in lower -> "route_ready"
            "hazard" in lower || "caution" in lower -> "hazard_ahead"
            "speed" in lower -> "speed_warning"
            "50 meter" in lower || "50m" in lower -> "in_50m"
            "100 meter" in lower || "100m" in lower -> "in_100m"
            "200 meter" in lower || "200m" in lower -> "in_200m"
            "500 meter" in lower || "500m" in lower -> "in_500m"
            "1 kilometer" in lower || "1 km" in lower || "1km" in lower -> "in_1km"
            "where am i" in lower || "location" in lower -> "where_am_i"
            "gps acquired" in lower || "gps lock" in lower || "gps connected" in lower -> "gps_acquired"
            "gps lost" in lower || "gps weak" in lower -> "gps_lost"
            else -> null
        }
    }

    /**
     * Speaks a terse verbal advisory cue with background audio ducking and intelligent spam debounce.
     */
    fun speakCue(cue: String, isCriticalAlert: Boolean = false) {
        if (guidanceMode == AudioGuidanceMode.MUTED || cue.isBlank()) return

        // In ALERTS_ONLY mode, only critical alerts (<80m turn maneuver, hazard) are allowed
        if (guidanceMode == AudioGuidanceMode.ALERTS_ONLY && !isCriticalAlert) {
            return
        }

        val now = SystemClock.elapsedRealtime()
        if (cue.equals(lastCueText, ignoreCase = true) && (now - lastCueTimeMs) < MIN_REPEAT_INTERVAL_MS) {
            return // Debounce spamming of identical cues
        }

        lastCueText = cue
        lastCueTimeMs = now

        // Check if external offline voice engine handles this cue
        if (voiceEngine == VoiceEngineType.TACTICAL_EXTERNAL) {
            if (isCriticalAlert) {
                playTacticalChime(880.0, 1320.0, 120)
            }
            val voiceKey = matchVoiceKey(cue)
            if (voiceKey != null && playVoiceAsset(voiceKey)) {
                Log.d("GabayAudio", "Played external offline voice cue: $voiceKey")
                return
            }
        }

        if (guidanceMode == AudioGuidanceMode.FULL_VOICE) {
            if (!isTtsReady) return
            requestAudioFocus()
            val utteranceId = "cue_${System.currentTimeMillis()}"
            tts?.speak(cue, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }
    }

    /**
     * Backward-compatible speak method.
     */
    fun speakTerseCue(cue: String) {
        val isCritical = cue.contains("TURN", ignoreCase = true) ||
            cue.contains("METERS", ignoreCase = true) ||
            cue.contains("HAZARD", ignoreCase = true) ||
            cue.contains("OFF-ROUTE", ignoreCase = true)
        speakCue(cue, isCriticalAlert = isCritical)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        soundPool.release()
        abandonAudioFocus()
    }
}
