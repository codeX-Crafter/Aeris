package com.runanywhere.kotlin_starter_example.services

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.runanywhere.kotlin_starter_example.data.SoundRepository
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
 * SoundClassifier using YAMNet TFLite.
 * Optimized for robustness and verbose logging to diagnose detection issues.
 */
class SoundClassifier(private val context: Context) {

    companion object {
        const val TAG = "SoundClassifier"
        const val MODEL_PATH = "yamnet.tflite"
        const val LABEL_PATH = "yamnet_class_map.csv"
        const val SAMPLE_RATE = 16000
        const val REQUIRED_SAMPLES = 15600 
        const val INTERNAL_THRESHOLD = 0.1f // Lowered to capture more candidates
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
            
            val outputCount = interpreter?.outputTensorCount ?: 0
            Log.d(TAG, "YAMNet initialized. Labels: ${labels.size}, Outputs: $outputCount")
            
            for (i in 0 until outputCount) {
                val tensor = interpreter?.getOutputTensor(i)
                Log.d(TAG, "Output $i: Name=${tensor?.name()}, Shape=${tensor?.shape()?.contentToString()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Initialization Error: ${e.message}", e)
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
            reader.readLine() 
            reader.forEachLine { line ->
                val parts = line.split(",")
                if (parts.size >= 3) {
                    val displayName = parts.drop(2).joinToString(",").replace("\"", "").trim()
                    labels.add(displayName)
                }
            }
            reader.close()
            Log.d(TAG, "Loaded ${labels.size} labels")
        } catch (e: Exception) {
            Log.e(TAG, "Label Loading Error: ${e.message}")
        }
    }

    suspend fun classify(audio: FloatArray): Map<SoundType, Float> = withContext(Dispatchers.Default) {
        audioBuffer.addAll(audio.toList())

        // Trim buffer if it gets too large to avoid memory issues
        if (audioBuffer.size > REQUIRED_SAMPLES * 4) {
            repeat(audioBuffer.size - REQUIRED_SAMPLES * 2) { audioBuffer.removeAt(0) }
        }

        if (audioBuffer.size < REQUIRED_SAMPLES) return@withContext emptyMap<SoundType, Float>()

        val input = audioBuffer.take(REQUIRED_SAMPLES).toFloatArray()
        
        // 50% overlap
        repeat(REQUIRED_SAMPLES / 2) {
            if (audioBuffer.isNotEmpty()) audioBuffer.removeAt(0)
        }

        // Removed RMS check to ensure we process even quiet sounds for debugging
        
        return@withContext try {
            val outputCount = interpreter?.outputTensorCount ?: 0
            val outputScores = Array(1) { FloatArray(labels.size) }
            
            if (outputCount >= 3) {
                // Multi-output model (likely the official YAMNet TFLite)
                val outputEmbeddings = Array(1) { FloatArray(1024) }
                val outputSpectrogram = Array(1) { Array(96) { FloatArray(64) } }

                val outputs = mutableMapOf<Int, Any>(
                    0 to outputScores,
                    1 to outputEmbeddings,
                    2 to outputSpectrogram
                )

                interpreter?.runForMultipleInputsOutputs(arrayOf(input), outputs)
                SoundRepository.updateSpectrogram(outputSpectrogram[0])
            } else {
                // Simplified model with only scores output
                interpreter?.run(input, outputScores)
            }

            val probabilities = outputScores[0]
            val results = mutableMapOf<SoundType, Float>()

            // Debug: Find top 3 labels regardless of mapping
            val topIndices = probabilities.indices.sortedByDescending { probabilities[it] }.take(3)
            val debugMsg = topIndices.joinToString { "${labels[it]}: ${"%.2f".format(probabilities[it])}" }
            Log.v(TAG, "Top Detections: $debugMsg")

            probabilities.indices.forEach { i ->
                if (probabilities[i] > INTERNAL_THRESHOLD) {
                    val label = labels[i].lowercase()
                    val type = mapLabelToSoundType(label)
                    if (type != null) {
                        results[type] = maxOf(results[type] ?: 0f, probabilities[i])
                    }
                }
            }
            
            if (results.isNotEmpty()) {
                Log.d(TAG, "Mapped Results: ${results.map { "${it.key}: ${it.value}" }}")
            }
            results
        } catch (e: Exception) {
            Log.e(TAG, "Classification Loop Error: ${e.message}")
            emptyMap<SoundType, Float>()
        }
    }

    private fun mapLabelToSoundType(label: String): SoundType? {
        val l = label.lowercase()
        return when {
            l.contains("siren") || l.contains("emergency vehicle") || 
            l.contains("ambulance") || l.contains("police car") || 
            l.contains("fire engine") || l.contains("fire truck") -> SoundType.SIREN
            
            l.contains("horn") || l.contains("honk") || 
            l.contains("vehicle horn") || l.contains("car horn") ||
            l.contains("toot") -> SoundType.HORN
            
            l.contains("alarm") || l.contains("smoke detector") || 
            l.contains("fire alarm") || l.contains("buzzer") || 
            l.contains("beeper") || l.contains("smoke alarm") -> SoundType.ALARM
            
            l.contains("doorbell") || l.contains("ding-dong") || 
            l.contains("chime") -> SoundType.DOORBELL
            
            l.contains("speech") || l.contains("shout") || 
            l.contains("yell") || l.contains("conversation") || 
            l.contains("laughter") || l.contains("crying") || 
            l.contains("baby cry") || l.contains("child speech") ||
            l.contains("narration") || l.contains("monologue") ||
            l.contains("babbling") -> SoundType.VOICE

            else -> null
        }
    }

    fun close() {
        interpreter?.close()
        interpreter = null
        audioBuffer.clear()
    }
}
