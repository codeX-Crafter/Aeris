package com.runanywhere.kotlin_starter_example.services

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runanywhere.sdk.core.types.InferenceFramework
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.sdk.public.extensions.Models.ModelCategory
import com.runanywhere.sdk.public.extensions.Models.ModelFileDescriptor
import com.runanywhere.sdk.public.extensions.registerModel
import com.runanywhere.sdk.public.extensions.registerMultiFileModel
import com.runanywhere.sdk.public.extensions.downloadModel
import com.runanywhere.sdk.public.extensions.loadLLMModel
import com.runanywhere.sdk.public.extensions.loadSTTModel
import com.runanywhere.sdk.public.extensions.loadTTSVoice
import com.runanywhere.sdk.public.extensions.loadVLMModel
import com.runanywhere.sdk.public.extensions.unloadLLMModel
import com.runanywhere.sdk.public.extensions.unloadSTTModel
import com.runanywhere.sdk.public.extensions.unloadTTSVoice
import com.runanywhere.sdk.public.extensions.unloadVLMModel
import com.runanywhere.sdk.public.extensions.isLLMModelLoaded
import com.runanywhere.sdk.public.extensions.isSTTModelLoaded
import com.runanywhere.sdk.public.extensions.isTTSVoiceLoaded
import com.runanywhere.sdk.public.extensions.isVLMModelLoaded
import com.runanywhere.sdk.public.extensions.isVoiceAgentReady
import com.runanywhere.sdk.public.extensions.availableModels
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ModelService : ViewModel() {

    private val TAG = "ModelService"

    // ── LLM state ─────────────────────────────────────────────
    var isLLMDownloading by mutableStateOf(false)
        private set
    var llmDownloadProgress by mutableStateOf(0f)
        private set
    var isLLMLoading by mutableStateOf(false)
        private set
    var isLLMLoaded by mutableStateOf(false)
        private set

    // ── STT state ─────────────────────────────────────────────
    var isSTTDownloading by mutableStateOf(false)
        private set
    var sttDownloadProgress by mutableStateOf(0f)
        private set
    var isSTTLoading by mutableStateOf(false)
        private set
    var isSTTLoaded by mutableStateOf(false)
        private set

    // ── TTS state ─────────────────────────────────────────────
    var isTTSDownloading by mutableStateOf(false)
        private set
    var ttsDownloadProgress by mutableStateOf(0f)
        private set
    var isTTSLoading by mutableStateOf(false)
        private set
    var isTTSLoaded by mutableStateOf(false)
        private set

    // ── VLM state ─────────────────────────────────────────────
    var isVLMDownloading by mutableStateOf(false)
        private set
    var vlmDownloadProgress by mutableStateOf(0f)
        private set
    var isVLMLoading by mutableStateOf(false)
        private set
    var isVLMLoaded by mutableStateOf(false)
        private set

    var isVoiceAgentReady by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    companion object {
        const val LLM_MODEL_ID = "smollm2-360m-instruct-q8_0"
        const val STT_MODEL_ID = "sherpa-onnx-whisper-tiny.en"
        const val TTS_MODEL_ID = "vits-piper-en_US-lessac-medium"
        const val VLM_MODEL_ID = "smolvlm-256m-instruct"

        fun registerDefaultModels() {
            RunAnywhere.registerModel(
                id = LLM_MODEL_ID,
                name = "SmolLM2 360M Instruct Q8_0",
                url = "https://huggingface.co/HuggingFaceTB/SmolLM2-360M-Instruct-GGUF/resolve/main/smollm2-360m-instruct-q8_0.gguf",
                framework = InferenceFramework.LLAMA_CPP,
                modality = ModelCategory.LANGUAGE,
                memoryRequirement = 400_000_000
            )
            RunAnywhere.registerModel(
                id = STT_MODEL_ID,
                name = "Sherpa Whisper Tiny (ONNX)",
                url = "https://github.com/RunanywhereAI/sherpa-onnx/releases/download/runanywhere-models-v1/sherpa-onnx-whisper-tiny.en.tar.gz",
                framework = InferenceFramework.ONNX,
                modality = ModelCategory.SPEECH_RECOGNITION
            )
            RunAnywhere.registerModel(
                id = TTS_MODEL_ID,
                name = "Piper TTS (US English - Medium)",
                url = "https://github.com/RunanywhereAI/sherpa-onnx/releases/download/runanywhere-models-v1/vits-piper-en_US-lessac-medium.tar.gz",
                framework = InferenceFramework.ONNX,
                modality = ModelCategory.SPEECH_SYNTHESIS
            )
            RunAnywhere.registerMultiFileModel(
                id = VLM_MODEL_ID,
                name = "SmolVLM 256M Instruct (Q8)",
                files = listOf(
                    ModelFileDescriptor(
                        url = "https://huggingface.co/ggml-org/SmolVLM-256M-Instruct-GGUF/resolve/main/SmolVLM-256M-Instruct-Q8_0.gguf",
                        filename = "SmolVLM-256M-Instruct-Q8_0.gguf"
                    ),
                    ModelFileDescriptor(
                        url = "https://huggingface.co/ggml-org/SmolVLM-256M-Instruct-GGUF/resolve/main/mmproj-SmolVLM-256M-Instruct-f16.gguf",
                        filename = "mmproj-SmolVLM-256M-Instruct-f16.gguf"
                    )
                ),
                framework = InferenceFramework.LLAMA_CPP,
                modality = ModelCategory.MULTIMODAL,
                memoryRequirement = 365_000_000
            )
        }
    }

    init {
        viewModelScope.launch {
            refreshModelState()
        }
    }

    private suspend fun refreshModelState() {
        Log.d(TAG, "Refreshing all model states")
        isLLMLoaded = RunAnywhere.isLLMModelLoaded()
        isSTTLoaded = RunAnywhere.isSTTModelLoaded()
        isTTSLoaded = RunAnywhere.isTTSVoiceLoaded()
        isVLMLoaded = RunAnywhere.isVLMModelLoaded
        isVoiceAgentReady = RunAnywhere.isVoiceAgentReady()
        Log.d(TAG, "Sync complete: STT=$isSTTLoaded, TTS=$isTTSLoaded, LLM=$isLLMLoaded")
    }

    private suspend fun isModelDownloaded(modelId: String): Boolean {
        val models = RunAnywhere.availableModels()
        val model = models.find { it.id == modelId }
        val exists = model?.localPath != null
        Log.d(TAG, "Checking disk for $modelId: exists=$exists")
        return exists
    }

    fun downloadAndLoadLLM() {
        if (isLLMDownloading || isLLMLoading) return
        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting LLM process")
                errorMessage = null
                refreshModelState()
                
                if (!isModelDownloaded(LLM_MODEL_ID)) {
                    isLLMDownloading = true
                    RunAnywhere.downloadModel(LLM_MODEL_ID)
                        .catch { e ->
                            Log.e(TAG, "LLM Download Catch: ${e.message}")
                            errorMessage = "LLM Download Error: ${e.message}"
                            isLLMDownloading = false
                        }
                        .collect { progress ->
                            llmDownloadProgress = progress.progress
                        }
                    isLLMDownloading = false
                }
                
                isLLMLoading = true
                Log.d(TAG, "Invoking loadLLMModel")
                RunAnywhere.loadLLMModel(LLM_MODEL_ID)
                refreshModelState()
            } catch (e: Exception) {
                Log.e(TAG, "LLM Critical Error: ${e.message}")
                errorMessage = "LLM Load Failed: ${e.message}"
            } finally {
                isLLMDownloading = false
                isLLMLoading = false
            }
        }
    }

    fun downloadAndLoadSTT() {
        if (isSTTDownloading || isSTTLoading) return
        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting STT process")
                errorMessage = null
                refreshModelState()

                if (!isModelDownloaded(STT_MODEL_ID)) {
                    isSTTDownloading = true
                    RunAnywhere.downloadModel(STT_MODEL_ID)
                        .catch { e ->
                            Log.e(TAG, "STT Download Catch: ${e.message}")
                            errorMessage = "STT Download Error: ${e.message}"
                            isSTTDownloading = false
                        }
                        .collect { progress ->
                            sttDownloadProgress = progress.progress
                        }
                    isSTTDownloading = false
                }
                
                isSTTLoading = true
                Log.d(TAG, "Invoking loadSTTModel")
                RunAnywhere.loadSTTModel(STT_MODEL_ID)
                refreshModelState()
            } catch (e: Exception) {
                Log.e(TAG, "STT Critical Error: ${e.message}")
                errorMessage = "STT Load Failed: ${e.message}"
            } finally {
                isSTTDownloading = false
                isSTTLoading = false
            }
        }
    }

    fun downloadAndLoadTTS() {
        if (isTTSDownloading || isTTSLoading) return
        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting TTS process")
                errorMessage = null
                refreshModelState()

                if (!isModelDownloaded(TTS_MODEL_ID)) {
                    isTTSDownloading = true
                    RunAnywhere.downloadModel(TTS_MODEL_ID)
                        .catch { e ->
                            Log.e(TAG, "TTS Download Catch: ${e.message}")
                            errorMessage = "TTS Download Error: ${e.message}"
                            isTTSDownloading = false
                        }
                        .collect { progress ->
                            ttsDownloadProgress = progress.progress
                        }
                    isTTSDownloading = false
                }
                
                isTTSLoading = true
                Log.d(TAG, "Invoking loadTTSVoice")
                RunAnywhere.loadTTSVoice(TTS_MODEL_ID)
                refreshModelState()
            } catch (e: Exception) {
                Log.e(TAG, "TTS Critical Error: ${e.message}")
                errorMessage = "TTS Load Failed: ${e.message}"
            } finally {
                isTTSDownloading = false
                isTTSLoading = false
            }
        }
    }

    fun downloadAndLoadVLM() {
        if (isVLMDownloading || isVLMLoading) return
        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting VLM process")
                errorMessage = null
                refreshModelState()

                if (!isModelDownloaded(VLM_MODEL_ID)) {
                    isVLMDownloading = true
                    RunAnywhere.downloadModel(VLM_MODEL_ID)
                        .catch { e ->
                            Log.e(TAG, "VLM Download Catch: ${e.message}")
                            errorMessage = "VLM Download Error: ${e.message}"
                            isVLMDownloading = false
                        }
                        .collect { progress ->
                            vlmDownloadProgress = progress.progress
                        }
                    isVLMDownloading = false
                }
                
                isVLMLoading = true
                Log.d(TAG, "Invoking loadVLMModel")
                RunAnywhere.loadVLMModel(VLM_MODEL_ID)
                refreshModelState()
            } catch (e: Exception) {
                Log.e(TAG, "VLM Critical Error: ${e.message}")
                errorMessage = "VLM Load Failed: ${e.message}"
            } finally {
                isVLMDownloading = false
                isVLMLoading = false
            }
        }
    }

    fun downloadAndLoadAllModels() {
        viewModelScope.launch {
            Log.d(TAG, "Full Download All Sequence Initiated")
            downloadAndLoadSTT()
            downloadAndLoadTTS()
            downloadAndLoadLLM()
        }
    }

    fun unloadAllModels() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Unloading all models from memory")
                RunAnywhere.unloadLLMModel()
                RunAnywhere.unloadSTTModel()
                RunAnywhere.unloadTTSVoice()
                try { RunAnywhere.unloadVLMModel() } catch (_: Exception) {}
                refreshModelState()
            } catch (e: Exception) {
                Log.e(TAG, "Unload Failure: ${e.message}")
                errorMessage = "Unload Failed: ${e.message}"
            }
        }
    }

    fun clearError() {
        errorMessage = null
    }
}
