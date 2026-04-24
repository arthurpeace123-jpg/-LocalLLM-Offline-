package com.example.localllm.ui.models

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.localllm.data.local.LocalModel
import com.example.localllm.data.local.ModelType
import com.example.localllm.data.repository.ModelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ModelsViewModel @Inject constructor(
    private val repository: ModelRepository
) : ViewModel() {

    val models: StateFlow<List<LocalModel>> = repository.allModels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow<ModelsUiState>(ModelsUiState.Idle)
    val uiState: StateFlow<ModelsUiState> = _uiState.asStateFlow()

    fun importModel(uri: Uri, metadata: ModelMetadata) {
        viewModelScope.launch {
            _uiState.value = ModelsUiState.Importing
            repository.importModel(
                uri = uri,
                name = metadata.name,
                displayName = metadata.displayName,
                parameters = metadata.parameters,
                modelType = metadata.modelType,
                description = metadata.description
            ).onSuccess {
                _uiState.value = ModelsUiState.ImportSuccess(it)
            }.onFailure {
                _uiState.value = ModelsUiState.Error(it.message ?: "Import failed")
            }
        }
    }

    fun deleteModel(model: LocalModel) {
        viewModelScope.launch {
            repository.deleteModel(model)
        }
    }

    fun dismissMessage() {
        _uiState.value = ModelsUiState.Idle
    }

    data class ModelMetadata(
        val name: String,
        val displayName: String,
        val parameters: String,
        val modelType: ModelType,
        val description: String
    )

    sealed class ModelsUiState {
        object Idle : ModelsUiState()
        object Importing : ModelsUiState()
        data class ImportSuccess(val model: LocalModel) : ModelsUiState()
        data class Error(val message: String) : ModelsUiState()
    }
}
