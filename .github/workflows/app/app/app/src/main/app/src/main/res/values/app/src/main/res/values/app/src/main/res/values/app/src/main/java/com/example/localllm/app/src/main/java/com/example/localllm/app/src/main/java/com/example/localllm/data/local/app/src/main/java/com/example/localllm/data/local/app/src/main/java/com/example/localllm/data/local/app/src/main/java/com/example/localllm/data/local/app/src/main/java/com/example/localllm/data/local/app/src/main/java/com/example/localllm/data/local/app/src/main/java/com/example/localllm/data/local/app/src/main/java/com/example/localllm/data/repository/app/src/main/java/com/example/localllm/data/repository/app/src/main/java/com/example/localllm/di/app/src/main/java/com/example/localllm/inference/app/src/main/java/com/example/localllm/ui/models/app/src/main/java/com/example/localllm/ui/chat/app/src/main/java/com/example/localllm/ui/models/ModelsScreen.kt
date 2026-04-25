package com.example.localllm.ui.models

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.localllm.data.local.LocalModel
import com.example.localllm.data.local.ModelType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelsScreen(
    onModelSelected: (String) -> Unit,
    viewModel: ModelsViewModel = hiltViewModel()
) {
    val models by viewModel.models.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var showImportDialog by remember { mutableStateOf(false) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { 
            pendingUri = it
            showImportDialog = true 
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Local LLM") },
                actions = {
                    IconButton(onClick = { filePicker.launch("*/*") }) {
                        Icon(Icons.Default.Add, contentDescription = "Import Model")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Your Models",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (models.isEmpty()) {
                item {
                    EmptyState(onImportClick = { filePicker.launch("*/*") })
                }
            } else {
                items(models, key = { it.id }) { model ->
                    ModelCard(
                        model = model,
                        onClick = { onModelSelected(model.id) },
                        onDelete = { viewModel.deleteModel(model) }
                    )
                }
            }
        }

        if (showImportDialog && pendingUri != null) {
            ImportDialog(
                uri = pendingUri!!,
                onDismiss = { 
                    showImportDialog = false 
                    pendingUri = null
                },
                onConfirm = { metadata ->
                    viewModel.importModel(pendingUri!!, metadata)
                    showImportDialog = false
                    pendingUri = null
                }
            )
        }

        when (val state = uiState) {
            is ModelsViewModel.ModelsUiState.Importing -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is ModelsViewModel.ModelsUiState.ImportSuccess -> {
                LaunchedEffect(state) {
                    viewModel.dismissMessage()
                }
            }
            is ModelsViewModel.ModelsUiState.Error -> {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissMessage() },
                    title = { Text("Error") },
                    text = { Text(state.message) },
                    confirmButton = {
                        TextButton(onClick = { viewModel.dismissMessage() }) {
                            Text("OK")
                        }
                    }
                )
            }
            else -> {}
        }
    }
}

@Composable
private fun ImportDialog(
    uri: Uri,
    onDismiss: () -> Unit,
    onConfirm: (ModelsViewModel.ModelMetadata) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var parameters by remember { mutableStateOf("2B") }
    var description by remember { mutableStateOf("") }
    var modelType by remember { mutableStateOf(ModelType.GEMMA) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Model") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("File Name (no spaces)") }
                )
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") }
                )
                OutlinedTextField(
                    value = parameters,
                    onValueChange = { parameters = it },
                    label = { Text("Parameters (e.g., 2B, 7B)") }
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        ModelsViewModel.ModelMetadata(
                            name = name,
                            displayName = displayName,
                            parameters = parameters,
                            modelType = modelType,
                            description = description
                        )
                    )
                },
                enabled = name.isNotBlank() && displayName.isNotBlank()
            ) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelCard(
    model: LocalModel,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = model.displayName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${model.parameters} • ${formatFileSize(model.fileSize)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (model.description.isNotBlank()) {
                    Text(
                        text = model.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            onDelete()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(onImportClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Memory,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No Models Found",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            "Import a .bin model file to get started",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onImportClick) {
            Icon(Icons.Default.FileUpload, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Import Model")
        }
    }
}

private fun formatFileSize(size: Long): String {
    return when {
        size > 1_000_000_000 -> String.format("%.1f GB", size / 1_000_000_000.0)
        size > 1_000_000 -> String.format("%.1f MB", size / 1_000_000.0)
        else -> "$size B"
    }
}
