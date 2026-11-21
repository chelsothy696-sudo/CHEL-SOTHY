package com.example.media

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

data class BuiltInBeatInfo(
    val id: String,
    val titleKm: String,
    val titleEn: String,
    val bpm: Int,
    val durationMs: Long
)

object BuiltInAudioGenerator {

    val catalog = listOf(
        BuiltInBeatInfo(
            id = "beat_lofi",
            titleKm = "ភ្លេង Lo-Fi ស្ងប់ស្ងាត់",
            titleEn = "Phnom Penh Lo-Fi",
            bpm = 85,
            durationMs = 12_000L
        ),
        BuiltInBeatInfo(
            id = "beat_cyber",
            titleKm = "ចង្វាក់ Cyber Bass",
            titleEn = "Cyber Bass Pulse",
            bpm = 120,
            durationMs = 10_000L
        ),
        BuiltInBeatInfo(
            id = "beat_breeze",
            titleKm = "ភ្លេងឆ្នេរសមុទ្រ",
            titleEn = "Golden Breeze",
            bpm = 96,
            durationMs = 12_000L
        )
    )

    suspend fun ensureBeatFile(context: Context, beatId: String): Pair<String, Long> =
        withContext(Dispatchers.IO) {
            val info = catalog.find { it.id == beatId } ?: catalog.first()
            val dir = File(context.filesDir, "built_in_audio").apply { mkdirs() }
            val wavFile = File(dir, "${info.id}.wav")
            if (!wavFile.exists() || wavFile.length() < 1024L) {
                writeSynthesizedBeatWav(wavFile, info)
            }
            Uri.fromFile(wavFile).toString() to info.durationMs
        }

    private fun writeSynthesizedBeatWav(file: File, info: BuiltInBeatInfo) {
        val sampleRate = 44100
        val channels = 1
        val bitsPerSample = 16
        val totalSamples = ((info.durationMs / 1000.0) * sampleRate).toInt()
        val pcmData = ByteArray(totalSamples * 2)
        val buffer = ByteBuffer.wrap(pcmData).order(ByteOrder.LITTLE_ENDIAN)

        val secondsPerBeat = 60.0 / info.bpm
        val chordRoots = when (info.id) {
            "beat_cyber" -> doubleArrayOf(110.0, 130.81, 146.83, 98.0)
            "beat_breeze" -> doubleArrayOf(261.63, 293.66, 329.63, 392.0)
            else -> doubleArrayOf(220.0, 196.0, 174.61, 164.81)
        }

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val beatIndex = (t / secondsPerBeat).toInt()
            val beatPos = t - (beatIndex * secondsPerBeat)

            // Soft kick drum on every beat
            val kickEnv = exp(-beatPos * 14.0)
            val kickFreq = 52.0 + 95.0 * exp(-beatPos * 28.0)
            val kick = sin(2.0 * PI * kickFreq * beatPos) * kickEnv * 0.42

            // Warm pad / arpeggio chord cycling every 2 beats
            val chordFreq = chordRoots[(beatIndex / 2) % chordRoots.size]
            val pad = (
                sin(2.0 * PI * chordFreq * t) * 0.16 +
                    sin(2.0 * PI * (chordFreq * 1.5) * t) * 0.10 +
                    sin(2.0 * PI * (chordFreq * 2.0) * t) * 0.06
                )

            // Crisp offbeat hi-hat pulse
            val halfBeatPos = t % (secondsPerBeat / 2.0)
            val hatEnv = if (info.id == "beat_cyber") exp(-halfBeatPos * 40.0) else exp(-halfBeatPos * 55.0)
            val hatTone = sin(2.0 * PI * 6800.0 * t) * sin(2.0 * PI * 4300.0 * t)
            val hat = hatTone * hatEnv * 0.12

            val mixed = (kick + pad + hat).coerceIn(-0.95, 0.95)
            val sample16 = (mixed * Short.MAX_VALUE).toInt().toShort()
            buffer.putShort(sample16)
        }

        FileOutputStream(file).use { out ->
            val byteRate = sampleRate * channels * bitsPerSample / 8
            val dataSize = pcmData.size
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
                put("RIFF".toByteArray(Charsets.US_ASCII))
                putInt(36 + dataSize)
                put("WAVE".toByteArray(Charsets.US_ASCII))
                put("fmt ".toByteArray(Charsets.US_ASCII))
                putInt(16) // PCM chunk size
                putShort(1) // PCM format
                putShort(channels.toShort())
                putInt(sampleRate)
                putInt(byteRate)
                putShort((channels * bitsPerSample / 8).toShort())
                putShort(bitsPerSample.toShort())
                put("data".toByteArray(Charsets.US_ASCII))
                putInt(dataSize)
            }
            out.write(header.array())
            out.write(pcmData)
        }
    }
}
