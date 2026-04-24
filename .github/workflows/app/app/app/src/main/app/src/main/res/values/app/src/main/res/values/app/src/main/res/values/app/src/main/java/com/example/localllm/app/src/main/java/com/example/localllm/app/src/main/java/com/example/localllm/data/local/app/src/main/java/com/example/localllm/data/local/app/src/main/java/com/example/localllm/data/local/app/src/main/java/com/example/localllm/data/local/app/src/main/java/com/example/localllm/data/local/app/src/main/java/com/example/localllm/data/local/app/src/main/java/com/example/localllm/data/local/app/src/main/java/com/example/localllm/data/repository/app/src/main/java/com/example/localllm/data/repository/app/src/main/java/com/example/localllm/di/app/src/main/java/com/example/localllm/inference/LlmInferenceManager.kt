package com.example.localllm.inference

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LlmInferenceManager @Inject constructor(
    private val context: Context
) {
    private var llmInference: LlmInference? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _inferenceState = MutableStateFlow<InferenceState>(InferenceState.Idle)
    val inferenceState: StateFlow<InferenceState> = _inferenceState.asStateFlow()
    
    private val _partialResult = MutableStateFlow("")
    val partialResult: StateFlow<String> = _partialResult.asStateFlow()

    fun loadModel(modelPath: String, onComplete: (Boolean) -> Unit) {
        scope.launch {
            try {
                _inferenceState.value = InferenceState.Loading
                llmInference?.close()
                
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelPath)
                    .setMaxTokens(1024)
                    .setTopK(40)
                    .setTemperature(0.8f)
                    .setRandomSeed(101)
                    .setResultListener { partialResult, done ->
                        _partialResult.value = partialResult
                        if (done) {
                            _inferenceState.value = InferenceState.Complete(partialResult)
                        }
                    }
                    .setErrorListener { error ->
                        _inferenceState.value = InferenceState.Error(error.message ?: "Unknown error")
                    }
                    .build()

                llmInference = LlmInference.createFromOptions(context, options)
                _inferenceState.value = InferenceState.Ready
                onComplete(true)
            } catch (e: Exception) {
                _inferenceState.value = InferenceState.Error(e.message ?: "Failed to load model")
                onComplete(false)
            }
        }
    }

    fun generateResponse(prompt: String): Flow<String> = callbackFlow {
        val inference = llmInference ?: run {
            trySend("Error: Model not loaded")
            close()
            return@callbackFlow
        }

        _inferenceState.value = InferenceState.Generating
        _partialResult.value = ""

        try {
            inference.generateResponseAsync(prompt)
            
            val job = launch {
                partialResult.collect { result ->
                    if (result.isNotEmpty()) {
                        trySend(result)
                    }
                }
            }

            inferenceState.filter { it is InferenceState.Complete || it is InferenceState.Error }
                .first()
            
            job.cancel()
            close()
        } catch (e: Exception) {
            _inferenceState.value = InferenceState.Error(e.message ?: "Generation failed")
            trySend("Error: ${e.message}")
            close()
        }
    }.flowOn(Dispatchers.Default)

    fun unloadModel() {
        llmInference?.close()
        llmInference = null
        _inferenceState.value = InferenceState.Idle
        _partialResult.value = ""
    }

    sealed class InferenceState {
        object Idle : InferenceState()
        object Loading : InferenceState()
        object Ready : InferenceState()
        object Generating : InferenceState()
        data class Complete(val result: String) : InferenceState()
        data class Error(val message: String) : InferenceState()
    }
}
