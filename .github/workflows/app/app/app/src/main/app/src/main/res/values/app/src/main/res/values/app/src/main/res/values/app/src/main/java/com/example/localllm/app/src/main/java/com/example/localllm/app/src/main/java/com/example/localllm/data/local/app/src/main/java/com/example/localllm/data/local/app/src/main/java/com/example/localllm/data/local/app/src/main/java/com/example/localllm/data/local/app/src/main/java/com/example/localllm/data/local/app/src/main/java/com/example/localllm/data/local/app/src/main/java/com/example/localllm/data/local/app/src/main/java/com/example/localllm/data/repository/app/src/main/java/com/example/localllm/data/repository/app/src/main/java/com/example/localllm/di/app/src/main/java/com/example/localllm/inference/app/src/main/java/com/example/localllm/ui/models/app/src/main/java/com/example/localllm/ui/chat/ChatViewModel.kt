package com.example.localllm.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.localllm.data.local.MessageRole
import com.example.localllm.data.repository.ChatRepository
import com.example.localllm.data.repository.ModelRepository
import com.example.localllm.inference.LlmInferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val inferenceManager: LlmInferenceManager,
    private val chatRepository: ChatRepository,
    private val modelRepository: ModelRepository
) : ViewModel() {

    private val modelId: String = checkNotNull(savedStateHandle["modelId"])
    private var sessionId: String? = null

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    val inferenceState = inferenceManager.inferenceState

    init {
        viewModelScope.launch {
            modelRepository.updateLastUsed(modelId)
            val model = modelRepository.getModel(modelId)
            model?.let {
                _uiState.value = _uiState.value.copy(
                    modelName = it.displayName,
                    isLoading = true
                )
                inferenceManager.loadModel(it.filePath) { success ->
                    _uiState.value = _uiState.value.copy(
                        isModelReady = success,
                        isLoading = false,
                        error = if (!success) "Failed to load model" else null
                    )
                }
            }
        }
    }

    fun createSession() {
        viewModelScope.launch {
            sessionId = chatRepository.createSession(modelId)
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank() || !_uiState.value.isModelReady) return
        
        val currentSession = sessionId ?: run {
            createSession()
            sessionId
        } ?: return

        viewModelScope.launch {
            chatRepository.sendMessage(currentSession, content, MessageRole.USER)
            
            val currentMessages = _messages.value.toMutableList()
            currentMessages.add(ChatMessage(MessageRole.USER, content))
            _messages.value = currentMessages
            
            _uiState.value = _uiState.value.copy(isGenerating = true)
            
            val prompt = buildPrompt(content)
            
            var fullResponse = ""
            inferenceManager.generateResponse(prompt).collect { partial ->
                fullResponse = partial
                _uiState.value = _uiState.value.copy(currentResponse = partial)
            }
            
            chatRepository.sendMessage(currentSession, fullResponse, MessageRole.MODEL)
            
            currentMessages.add(ChatMessage(MessageRole.MODEL, fullResponse))
            _messages.value = currentMessages
            
            if (currentMessages.size <= 2) {
                chatRepository.updateSessionTitle(
                    currentSession, 
                    content.take(30) + if (content.length > 30) "..." else ""
                )
            }
            
            _uiState.value = _uiState.value.copy(
                isGenerating = false,
                currentResponse = ""
            )
        }
    }

    private fun buildPrompt(userMessage: String): String {
        return """<start_of_turn>user
$userMessage<end_of_turn>
<start_of_turn>model
"""
    }

    fun stopGeneration() {
        _uiState.value = _uiState.value.copy(isGenerating = false)
    }

    fun clearChat() {
        sessionId?.let { id ->
            viewModelScope.launch {
                chatRepository.clearHistory(id)
                _messages.value = emptyList()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        inferenceManager.unloadModel()
    }

    data class ChatUiState(
        val modelName: String = "",
        val isModelReady: Boolean = false,
        val isLoading: Boolean = false,
        val isGenerating: Boolean = false,
        val currentResponse: String = "",
        val error: String? = null
    )

    data class ChatMessage(
        val role: MessageRole,
        val content: String,
        val isGenerating: Boolean = false
    )
}
