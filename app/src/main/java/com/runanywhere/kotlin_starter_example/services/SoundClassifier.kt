package com.runanywhere.kotlin_starter_example.services

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.runanywhere.kotlin_starter_example.data.SoundType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/**
 * SoundClassifier uses the YAMNet TFLite model to classify audio chunks into sound categories.
 * It replaces the previous keyword-matching transcription approach.
 */
class SoundClassifier(private val context: Context) {

    companion object {
        const val TAG = "SoundClassifier"
        const val MODEL_PATH = "yamnet.tflite"
        const val LABEL_PATH = "yamnet_class_map.csv"
        const val SAMPLE_RATE = 16000
        const val REQUIRED_SAMPLES = 15600 // YAMNet expects 0.975s of audio (15600 samples @ 16kHz)
        const val CLASSIFICATION_THRESHOLD = 0.3f
    }

    private var interpreter: Interpreter? = null
    private val labels = mutableListOf<String>()
    private val audioBuffer = mutableListOf<Float>()

    init {
        try {
            val model = loadModelFile(context.assets, MODEL_PATH)
            val options = Interpreter.Options().apply {
                setNumThreads(2)
            }
            interpreter = Interpreter(model, options)
            loadLabels()
            Log.d(TAG, "YAMNet initialized with ${labels.size} labels")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize YAMNet: ${e.message}")
        }
    }

    private fun loadModelFile(assetManager: AssetManager, modelPath: String): ByteBuffer {
        val fileDescriptor = assetManager.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun loadLabels() {
        try {
            val reader = BufferedReader(InputStreamReader(context.assets.open(LABEL_PATH)))
            reader.readLine() // Skip CSV header
            reader.forEachLine { line ->
                val parts = line.split(",")
                if (parts.size >= 3) {
                    // Extract display_name, which might be quoted and contain commas
                    val displayName = parts.drop(2).joinToString(",").replace("\"", "").trim()
                    labels.add(displayName)
                }
            }
            reader.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading labels: ${e.message}")
        }
    }

    suspend fun classify(audio: FloatArray): Map<SoundType, Float> = withContext(Dispatchers.Default) {
        audioBuffer.addAll(audio.toList())

        // Ensure we have enough samples for one YAMNet window
        if (audioBuffer.size < REQUIRED_SAMPLES) return@withContext emptyMap<SoundType, Float>()

        val input = audioBuffer.take(REQUIRED_SAMPLES).toFloatArray()
        
        // Remove processed chunk with 50% overlap for better detection continuity
        repeat(REQUIRED_SAMPLES / 2) {
            if (audioBuffer.isNotEmpty()) audioBuffer.removeAt(0)
        }

        // Quick RMS check to skip silence and save computation
        val rms = Math.sqrt(input.map { it * it }.average()).toFloat()
        if (rms < 0.001f) return@withContext emptyMap<SoundType, Float>()

        return@withContext try {
            val output = Array(1) { FloatArray(labels.size) }
            interpreter?.run(input, output)

            val probabilities = output[0]
            val results = mutableMapOf<SoundType, Float>()

            // Find all labels above threshold and map them to our internal SoundType
            probabilities.indices.forEach { i ->
                if (probabilities[i] > CLASSIFICATION_THRESHOLD) {
                    val label = labels[i].lowercase()
                    val type = mapLabelToSoundType(label)
                    if (type != null) {
                        // Keep the highest confidence for each SoundType
                        results[type] = maxOf(results[type] ?: 0f, probabilities[i])
                    }
                }
            }
            
            if (results.isNotEmpty()) {
                Log.d(TAG, "Detections: ${results.map { "${it.key}: ${it.value}" }}")
            }
            results
        } catch (e: Exception) {
            Log.e(TAG, "Classification failed: ${e.message}")
            emptyMap<SoundType, Float>()
        }
    }

    private fun mapLabelToSoundType(label: String): SoundType? {
        return when {
            label.contains("siren") || label.contains("emergency vehicle") || 
            label.contains("ambulance") || label.contains("police car") || 
            label.contains("fire engine") -> SoundType.SIREN
            
            label.contains("horn") || label.contains("honk") || 
            label.contains("vehicle horn") || label.contains("car horn") -> SoundType.HORN
            
            label.contains("alarm") || label.contains("smoke detector") || 
            label.contains("fire alarm") || label.contains("buzzer") || 
            label.contains("beeper") -> SoundType.ALARM
            
            label.contains("doorbell") || label.contains("ding-dong") || 
            label.contains("chime") -> SoundType.DOORBELL
            
            label.contains("speech") || label.contains("shout") || 
            label.contains("yell") || label.contains("conversation") || 
            label.contains("laughter") || label.contains("crying") || 
            label.contains("baby cry") -> SoundType.VOICE

            else -> null
        }
    }

    fun close() {
        interpreter?.close()
        interpreter = null
        audioBuffer.clear()
    }
}
