package com.example.core.audio

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Procedural Audio Synthesizer for TL Brain Test (Owner: PRITAM CHOUDHARY).
 *
 * Synthesizes 100% Royalty-Free, Public Domain (CC0 equivalent) audio waveforms directly
 * in code using pure mathematical algorithms. Zero external network requests, zero cloud cost,
 * zero copyright risk, and ultra-low file footprint (~1KB to 15KB per sound effect).
 */
object PcmSoundSynthesizer {

    private const val SAMPLE_RATE = 22050

    /**
     * Synthesizes a complete RIFF WAV byte array for the specified [SoundEffect].
     */
    fun synthesizeWav(effect: SoundEffect): ByteArray {
        val pcm = when (effect) {
            // General UI
            SoundEffect.CLICK -> clickWave(durationMs = 25, freq = 1200.0)
            SoundEffect.SELECT -> clickWave(durationMs = 35, freq = 900.0)
            SoundEffect.MENU_OPEN -> slideTone(startFreq = 440.0, endFreq = 750.0, durationMs = 80)
            SoundEffect.MENU_CLOSE -> slideTone(startFreq = 650.0, endFreq = 380.0, durationMs = 70)
            SoundEffect.TAB_CHANGE -> multiToneChord(listOf(600.0 to 0.7), durationMs = 40)
            SoundEffect.PAGE_TRANSITION -> slideTone(startFreq = 300.0, endFreq = 520.0, durationMs = 75)
            SoundEffect.TOGGLE_ON -> sequenceNotes(listOf(523.25 to 50, 783.99 to 70))
            SoundEffect.TOGGLE_OFF -> sequenceNotes(listOf(783.99 to 50, 523.25 to 70))
            SoundEffect.CHECKBOX -> clickWave(durationMs = 30, freq = 1400.0)
            SoundEffect.NOTIFICATION -> sequenceNotes(listOf(659.25 to 80, 987.77 to 140))
            SoundEffect.POPUP_OPEN -> slideTone(startFreq = 420.0, endFreq = 680.0, durationMs = 85)
            SoundEffect.POPUP_CLOSE -> slideTone(startFreq = 620.0, endFreq = 360.0, durationMs = 75)
            SoundEffect.ERROR -> buzzerWave(freq = 180.0, durationMs = 170)
            SoundEffect.WARNING -> sequenceNotes(listOf(440.0 to 70, 440.0 to 110))
            SoundEffect.SUCCESS -> multiToneChord(listOf(523.25 to 0.5, 659.25 to 0.5, 783.99 to 0.6), durationMs = 220)
            SoundEffect.SAVE -> sequenceNotes(listOf(587.33 to 60, 880.0 to 120))
            SoundEffect.DELETE -> sequenceNotes(listOf(440.0 to 60, 330.0 to 100))
            SoundEffect.LOGOUT -> sequenceNotes(listOf(659.25 to 70, 523.25 to 70, 392.0 to 120))

            // Quiz
            SoundEffect.QUIZ_START -> sequenceNotes(listOf(440.0 to 60, 554.37 to 60, 659.25 to 60, 880.0 to 150))
            SoundEffect.QUESTION_APPEAR -> multiToneChord(listOf(1046.5 to 0.6, 2093.0 to 0.2), durationMs = 75)
            SoundEffect.OPTION_SELECT -> clickWave(durationMs = 35, freq = 850.0)
            SoundEffect.CORRECT -> multiToneChord(listOf(783.99 to 0.6, 1046.5 to 0.8), durationMs = 260)
            SoundEffect.WRONG -> buzzerWave(freq = 150.0, durationMs = 220)
            SoundEffect.TIMER_WARN -> multiToneChord(listOf(1200.0 to 0.8), durationMs = 50)
            SoundEffect.TIMER_END -> sequenceNotes(listOf(220.0 to 90, 180.0 to 140))
            SoundEffect.NEXT_QUESTION -> slideTone(startFreq = 500.0, endFreq = 900.0, durationMs = 60)
            SoundEffect.QUIZ_COMPLETE -> sequenceNotes(listOf(523.25 to 70, 659.25 to 70, 783.99 to 70, 1046.5 to 220))
            SoundEffect.SCORE_DISPLAY -> sequenceNotes(listOf(783.99 to 50, 987.77 to 50, 1174.66 to 120))
            SoundEffect.HIGH_SCORE -> sequenceNotes(listOf(523.25 to 80, 659.25 to 80, 783.99 to 80, 1046.5 to 100, 1318.51 to 260))
            SoundEffect.XP_EARNED -> sequenceNotes(listOf(987.77 to 40, 1318.51 to 90))
            SoundEffect.LEVEL_UP -> sequenceNotes(listOf(440.0 to 70, 554.37 to 70, 659.25 to 70, 880.0 to 180))
            SoundEffect.ACHIEVEMENT_UNLOCKED -> sequenceNotes(listOf(587.33 to 80, 739.99 to 80, 880.0 to 80, 1174.66 to 220))

            // Brain Games
            SoundEffect.GAME_START -> sequenceNotes(listOf(523.25 to 70, 659.25 to 70, 783.99 to 130))
            SoundEffect.GAME_CORRECT_MOVE -> multiToneChord(listOf(880.0 to 0.6, 1320.0 to 0.3), durationMs = 80)
            SoundEffect.GAME_WRONG_MOVE -> slideTone(startFreq = 300.0, endFreq = 180.0, durationMs = 120)
            SoundEffect.MATCH_FOUND -> sequenceNotes(listOf(659.25 to 60, 987.77 to 130))
            SoundEffect.MATCH_COMPLETED -> sequenceNotes(listOf(523.25 to 60, 659.25 to 60, 783.99 to 60, 1046.5 to 140))
            SoundEffect.COUNTDOWN_TICK -> clickWave(durationMs = 28, freq = 1400.0)
            SoundEffect.GAME_OVER -> sequenceNotes(listOf(440.0 to 90, 370.0 to 90, 293.66 to 160))
            SoundEffect.NEW_RECORD -> sequenceNotes(listOf(659.25 to 70, 783.99 to 70, 987.77 to 70, 1318.51 to 240))
            SoundEffect.GAME_COMPLETED -> sequenceNotes(listOf(523.25 to 80, 783.99 to 80, 1046.5 to 200))
            SoundEffect.POINTS_EARNED -> sequenceNotes(listOf(1046.5 to 35, 1318.51 to 75))

            // Leaderboard
            SoundEffect.LEADERBOARD_OPEN -> sequenceNotes(listOf(523.25 to 60, 659.25 to 60, 783.99 to 100))
            SoundEffect.RANKING_CHANGED -> sequenceNotes(listOf(587.33 to 70, 880.0 to 140))

            // Profile
            SoundEffect.AVATAR_SELECT -> slideTone(startFreq = 480.0, endFreq = 820.0, durationMs = 60)
            SoundEffect.SETTINGS_SAVED -> sequenceNotes(listOf(523.25 to 50, 659.25 to 90))

            // Account
            SoundEffect.SUBSCRIPTION_ACTIVATED -> sequenceNotes(listOf(440.0 to 80, 554.37 to 80, 659.25 to 80, 880.0 to 220))
            SoundEffect.REQUEST_SUBMITTED -> sequenceNotes(listOf(600.0 to 60, 900.0 to 100))

            // Support Chat
            SoundEffect.CHAT_SENT -> slideTone(startFreq = 500.0, endFreq = 950.0, durationMs = 50)
            SoundEffect.CHAT_RECEIVED -> sequenceNotes(listOf(880.0 to 45, 1174.66 to 90))
        }

        return wrapInWav(pcm)
    }

    private fun clickWave(durationMs: Int, freq: Double): ByteArray {
        val totalSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt().coerceAtLeast(1)
        val pcm = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / totalSamples
            val envelope = exp(-progress * 8.0) // fast exponential decay
            val sample = sin(2.0 * PI * freq * t) * envelope
            pcm[i] = (sample * 24000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        return shortsToBytes(pcm)
    }

    private fun slideTone(startFreq: Double, endFreq: Double, durationMs: Int): ByteArray {
        val totalSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt().coerceAtLeast(1)
        val pcm = ShortArray(totalSamples)
        var phase = 0.0
        for (i in 0 until totalSamples) {
            val progress = i.toDouble() / totalSamples
            val currentFreq = startFreq + (endFreq - startFreq) * progress
            phase += 2.0 * PI * currentFreq / SAMPLE_RATE
            val envelope = sin(PI * progress) // smooth bell envelope
            val sample = sin(phase) * envelope
            pcm[i] = (sample * 22000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        return shortsToBytes(pcm)
    }

    private fun buzzerWave(freq: Double, durationMs: Int): ByteArray {
        val totalSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt().coerceAtLeast(1)
        val pcm = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / totalSamples
            val envelope = 1.0 - progress
            // Mix fundamental with 2nd and 3rd harmonics for rich buzzer sound
            val s1 = sin(2.0 * PI * freq * t)
            val s2 = sin(2.0 * PI * freq * 2.0 * t) * 0.4
            val s3 = sin(2.0 * PI * freq * 3.0 * t) * 0.2
            val sample = (s1 + s2 + s3) * envelope * 0.6
            pcm[i] = (sample * 24000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        return shortsToBytes(pcm)
    }

    private fun multiToneChord(tones: List<Pair<Double, Double>>, durationMs: Int): ByteArray {
        val totalSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt().coerceAtLeast(1)
        val pcm = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / totalSamples
            val envelope = exp(-progress * 3.5)
            var sample = 0.0
            for ((freq, weight) in tones) {
                sample += sin(2.0 * PI * freq * t) * weight
            }
            pcm[i] = (sample * envelope * 18000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        return shortsToBytes(pcm)
    }

    private fun sequenceNotes(notes: List<Pair<Double, Int>>): ByteArray {
        val totalSamples = notes.sumOf { (SAMPLE_RATE * (it.second / 1000.0)).toInt().coerceAtLeast(1) }
        val pcm = ShortArray(totalSamples)
        var offset = 0
        for ((freq, durationMs) in notes) {
            val noteSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt().coerceAtLeast(1)
            for (i in 0 until noteSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val progress = i.toDouble() / noteSamples
                val envelope = exp(-progress * 4.0)
                val sample = sin(2.0 * PI * freq * t) * envelope
                val destIdx = offset + i
                if (destIdx < totalSamples) {
                    pcm[destIdx] = (sample * 22000.0).toInt().coerceIn(-32767, 32767).toShort()
                }
            }
            offset += noteSamples
        }
        return shortsToBytes(pcm)
    }

    private fun shortsToBytes(shorts: ShortArray): ByteArray {
        val bytes = ByteArray(shorts.size * 2)
        for (i in shorts.indices) {
            val s = shorts[i].toInt()
            bytes[i * 2] = (s and 0xff).toByte()
            bytes[i * 2 + 1] = ((s shr 8) and 0xff).toByte()
        }
        return bytes
    }

    private fun wrapInWav(pcmData: ByteArray): ByteArray {
        val out = ByteArrayOutputStream(44 + pcmData.size)
        val dataOut = DataOutputStream(out)

        val totalDataLen = pcmData.size + 36
        val byteRate = SAMPLE_RATE * 1 * 2

        fun writeLeShort(v: Int) {
            dataOut.writeByte(v and 0xff)
            dataOut.writeByte((v shr 8) and 0xff)
        }
        fun writeLeInt(v: Int) {
            dataOut.writeByte(v and 0xff)
            dataOut.writeByte((v shr 8) and 0xff)
            dataOut.writeByte((v shr 16) and 0xff)
            dataOut.writeByte((v shr 24) and 0xff)
        }

        // RIFF chunk
        dataOut.writeBytes("RIFF")
        writeLeInt(totalDataLen)
        dataOut.writeBytes("WAVE")

        // fmt chunk
        dataOut.writeBytes("fmt ")
        writeLeInt(16) // subchunk1 size (16 for PCM)
        writeLeShort(1) // audio format 1 = PCM
        writeLeShort(1) // mono channel
        writeLeInt(SAMPLE_RATE)
        writeLeInt(byteRate)
        writeLeShort(2) // block align (channels * bitsPerSample / 8)
        writeLeShort(16) // bits per sample

        // data chunk
        dataOut.writeBytes("data")
        writeLeInt(pcmData.size)
        dataOut.write(pcmData)

        dataOut.flush()
        return out.toByteArray()
    }
}
