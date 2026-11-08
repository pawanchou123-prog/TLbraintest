package com.example.core.audio

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Central Sound Manager for the TL Brain Test application (Owner: PRITAM CHOUDHARY).
 *
 * Features:
 * - Thread-safe SoundPool engine for low-latency game and UI playback.
 * - Global ON/OFF toggle and master volume slider (0.0 to 1.0) with persistent SharedPreferences storage.
 * - StateFlow integration for live Compose UI reactivity.
 * - Graceful degradation: never throws exceptions if audio hardware is absent, muted, or failing.
 * - 100% Free & Royalty-Free procedural audio generated locally on-device. Zero cloud cost.
 */
object SoundManager {

    private const val TAG = "TlSoundManager"
    private const val PREFS_NAME = "tl_brain_audio_prefs"
    private const val KEY_SOUND_ENABLED = "sound_effects_enabled"
    private const val KEY_SOUND_VOLUME = "sound_effects_volume"

    private var appContext: Context? = null
    private var prefs: SharedPreferences? = null

    private var soundPool: SoundPool? = null
    private val soundIdMap = ConcurrentHashMap<SoundEffect, Int>()
    private val loadedSoundSet = ConcurrentHashMap.newKeySet<Int>()

    private val _soundEnabledFlow = MutableStateFlow(true)
    val soundEnabledFlow: StateFlow<Boolean> = _soundEnabledFlow.asStateFlow()

    private val _soundVolumeFlow = MutableStateFlow(0.85f)
    val soundVolumeFlow: StateFlow<Float> = _soundVolumeFlow.asStateFlow()

    private var isInitialized = false
    private val initLock = Any()

    /**
     * Initializes the SoundManager with application context and loads persisted audio settings.
     */
    fun init(context: Context) {
        if (isInitialized) return
        synchronized(initLock) {
            if (isInitialized) return
            try {
                val app = context.applicationContext
                appContext = app
                val sp = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs = sp

                val savedEnabled = sp.getBoolean(KEY_SOUND_ENABLED, true)
                val savedVolume = sp.getFloat(KEY_SOUND_VOLUME, 0.85f).coerceIn(0.0f, 1.0f)

                _soundEnabledFlow.value = savedEnabled
                _soundVolumeFlow.value = savedVolume

                setupSoundPool(app)
                isInitialized = true
            } catch (t: Throwable) {
                Log.w(TAG, "Audio subsystem initialization fallback: ${t.message}")
            }
        }
    }

    private fun setupSoundPool(context: Context) {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val pool = SoundPool.Builder()
                .setMaxStreams(8)
                .setAudioAttributes(audioAttributes)
                .build()

            pool.setOnLoadCompleteListener { _, sampleId, status ->
                if (status == 0) {
                    loadedSoundSet.add(sampleId)
                }
            }

            soundPool = pool

            // Synthesize and load audio files asynchronously on IO dispatcher to avoid blocking UI
            CoroutineScope(Dispatchers.IO).launch {
                preloadAllSounds(context, pool)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "SoundPool unavailable (expected in certain testing environments): ${t.message}")
        }
    }

    private fun preloadAllSounds(context: Context, pool: SoundPool) {
        try {
            val soundDir = File(context.cacheDir, "tl_brain_sounds").apply { mkdirs() }

            SoundEffect.entries.forEach { effect ->
                try {
                    val soundFile = File(soundDir, "${effect.id}.wav")
                    if (!soundFile.exists() || soundFile.length() < 44) {
                        val wavBytes = PcmSoundSynthesizer.synthesizeWav(effect)
                        FileOutputStream(soundFile).use { it.write(wavBytes) }
                    }
                    val soundId = pool.load(soundFile.absolutePath, 1)
                    if (soundId > 0) {
                        soundIdMap[effect] = soundId
                    }
                } catch (e: Exception) {
                    Log.v(TAG, "Failed loading sound: ${effect.id} (${e.message})")
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Error generating local sound cache: ${t.message}")
        }
    }

    /**
     * Plays a sound effect with master volume multiplier and concurrency management.
     * Guaranteed never to crash the calling thread if audio is unavailable or failing.
     */
    fun play(effect: SoundEffect, volumeMultiplier: Float = 1.0f) {
        if (!_soundEnabledFlow.value) return
        val masterVol = _soundVolumeFlow.value
        if (masterVol <= 0.001f) return

        val pool = soundPool ?: return
        val soundId = soundIdMap[effect] ?: return

        try {
            val vol = (masterVol * volumeMultiplier).coerceIn(0.0f, 1.0f)
            val priority = when (effect) {
                SoundEffect.CORRECT, SoundEffect.WRONG, SoundEffect.QUIZ_COMPLETE,
                SoundEffect.HIGH_SCORE, SoundEffect.LEVEL_UP, SoundEffect.GAME_OVER -> 2
                else -> 1
            }
            pool.play(soundId, vol, vol, priority, 0, 1.0f)
        } catch (t: Throwable) {
            // Graceful degradation: never let audio exceptions affect user actions
            Log.v(TAG, "Playback bypassed for ${effect.id}: ${t.message}")
        }
    }

    /**
     * Toggles global sound effects ON or OFF and persists to SharedPreferences.
     */
    fun setSoundEnabled(enabled: Boolean) {
        _soundEnabledFlow.value = enabled
        prefs?.edit()?.putBoolean(KEY_SOUND_ENABLED, enabled)?.apply()
    }

    /**
     * Returns true if sound effects are globally enabled.
     */
    fun isSoundEnabled(): Boolean = _soundEnabledFlow.value

    /**
     * Sets master sound volume (clamped 0.0f to 1.0f) and persists to SharedPreferences.
     */
    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0.0f, 1.0f)
        _soundVolumeFlow.value = clamped
        prefs?.edit()?.putFloat(KEY_SOUND_VOLUME, clamped)?.apply()
    }

    /**
     * Returns the current master sound volume (0.0f to 1.0f).
     */
    fun getVolume(): Float = _soundVolumeFlow.value

    /**
     * Releases audio resources.
     */
    fun release() {
        try {
            soundPool?.release()
            soundPool = null
            soundIdMap.clear()
            loadedSoundSet.clear()
            isInitialized = false
        } catch (t: Throwable) {
            Log.w(TAG, "Error releasing audio resources: ${t.message}")
        }
    }
}
