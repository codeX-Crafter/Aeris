package com.runanywhere.kotlin_starter_example.services

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder

suspend fun playWavBytes(wavData: ByteArray) = withContext(Dispatchers.IO) {
    if (wavData.size < 44) return@withContext

    val buffer = ByteBuffer.wrap(wavData).order(ByteOrder.LITTLE_ENDIAN)
    buffer.position(22)
    val numChannels = buffer.short.toInt()
    val sampleRate = buffer.int
    buffer.int  // byteRate
    buffer.short  // blockAlign
    val bitsPerSample = buffer.short.toInt()

    // Find data chunk
    var dataOffset = 36
    while (dataOffset < wavData.size - 8) {
        if (wavData[dataOffset] == 'd'.code.toByte() &&
            wavData[dataOffset + 1] == 'a'.code.toByte() &&
            wavData[dataOffset + 2] == 't'.code.toByte() &&
            wavData[dataOffset + 3] == 'a'.code.toByte()
        ) break
        dataOffset++
    }
    dataOffset += 8
    if (dataOffset >= wavData.size) return@withContext

    val pcmData = wavData.copyOfRange(dataOffset, wavData.size)

    val channelConfig = if (numChannels == 1)
        AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO
    val audioFormat = if (bitsPerSample == 16)
        AudioFormat.ENCODING_PCM_16BIT else AudioFormat.ENCODING_PCM_8BIT

    val minBuffer = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    if (minBuffer <= 0 || pcmData.isEmpty()) return@withContext

    val track = AudioTrack.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
        .setAudioFormat(
            AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(audioFormat)
                .setChannelMask(channelConfig)
                .build()
        )
        .setBufferSizeInBytes(maxOf(minBuffer, pcmData.size))
        .setTransferMode(AudioTrack.MODE_STATIC)
        .build()

    track.write(pcmData, 0, pcmData.size)
    track.play()

    val durationMs = (pcmData.size.toLong() * 1000) /
            (sampleRate * numChannels * (bitsPerSample / 8))
    delay(durationMs + 200)

    track.stop()
    track.release()
}