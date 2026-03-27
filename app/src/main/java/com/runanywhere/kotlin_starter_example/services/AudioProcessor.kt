package com.runanywhere.kotlin_starter_example.services

import android.content.Context
import android.content.pm.PackageManager
import android.media.*
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*

class AudioProcessor(private val context: Context) {

    companion object {
        const val TAG = "AudioProcessor"
        const val SAMPLE_RATE = 16000
    }

    private val sampleRate = SAMPLE_RATE
    private var isRecording = true
    private var audioRecord: AudioRecord? = null

    suspend fun start(onAudio: (FloatArray) -> Unit) = withContext(Dispatchers.IO) {

        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "RECORD_AUDIO permission not granted — aborting")
            return@withContext
        }

        val bufferSize = maxOf(
            AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ),
            sampleRate
        )

        if (bufferSize <= 0) {
            Log.e(TAG, "Invalid buffer size: $bufferSize")
            return@withContext
        }

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize")
                audioRecord?.release()
                audioRecord = null
                return@withContext
            }

            val buffer = ShortArray(bufferSize)
            audioRecord?.startRecording()
            Log.d(TAG, "Recording started")

            while (isActive && isRecording) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    val floatBuffer = FloatArray(read) { buffer[it] / 32768f }
                    onAudio(floatBuffer)
                }
            }

        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException — mic permission denied at OS level: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "AudioRecord error: ${e.message}")
        } finally {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            Log.d(TAG, "Recording stopped and released")
        }
    }

    fun stop() {
        isRecording = false
    }
}
