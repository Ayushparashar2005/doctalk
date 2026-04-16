package com.doctalk.app.presentation.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.doctalk.app.network.groq.GroqConfig
import com.doctalk.app.network.groq.GroqRepository
import com.doctalk.app.presentation.components.DocTalkButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.TextField
import com.doctalk.app.presentation.components.ErrorMessage
import com.doctalk.app.presentation.components.SuccessMessage
import com.doctalk.app.presentation.components.TopAppBar

/**
 * Groq API settings screen for configuration
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroqSettingsScreen(
    onNavigateBack: () -> Unit,
    groqRepository: GroqRepository = hiltViewModel()
) {
    var apiKey by remember { mutableStateOf("") }
    var selectedModel by remember { mutableStateOf(GroqConfig.DEFAULT_MODEL) }
    var apiKeyVisible by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }
    
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Top App Bar
        TopAppBar(
            title = "Groq API Settings",
            onBackClick = onNavigateBack
        )
        
        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // API Key Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "API Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Get your API key from console.groq.com",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { 
                            apiKey = it
                            errorMessage = null
                            successMessage = null
                        },
                        label = { Text("Groq API Key") },
                        placeholder = { Text("gsk_...") },
                        visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                                Icon(
                                    imageVector = if (apiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (apiKeyVisible) "Hide API key" else "Show API key"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (apiKey.isNotBlank() && !groqRepository.validateApiKey(apiKey)) {
                        Text(
                            text = "Invalid API key format. Should start with 'gsk_'",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Model Selection Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Model Selection",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    ExposedDropdownMenuBox(
                        expanded = isExpanded,
                        onExpandedChange = { isExpanded = it }
                    ) {
                        TextField(
                            value = groqRepository.getModelDisplayName(selectedModel),
                            onValueChange = { },
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        
                        DropdownMenu(
                            expanded = isExpanded,
                            onDismissRequest = { isExpanded = false }
                        ) {
                            groqRepository.getAvailableModels().forEach { model ->
                                DropdownMenuItem(
                                    text = { Text(groqRepository.getModelDisplayName(model)) },
                                    onClick = {
                                        selectedModel = model
                                        isExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Model: ${groqRepository.getModelDisplayName(selectedModel)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Save Button
            DocTalkButton(
                text = "Save Configuration",
                onClick = {
                    if (apiKey.isBlank()) {
                        errorMessage = "API key cannot be empty"
                        return@DocTalkButton
                    }
                    
                    if (!groqRepository.validateApiKey(apiKey)) {
                        errorMessage = "Invalid API key format"
                        return@DocTalkButton
                    }
                    
                    if (groqRepository.setGroqApiKey(apiKey)) {
                        successMessage = "Groq API configuration saved successfully!"
                        errorMessage = null
                    } else {
                        errorMessage = "Failed to save API configuration"
                        successMessage = null
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = apiKey.isNotBlank() && groqRepository.validateApiKey(apiKey)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Info Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "About Groq",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "Groq provides fast inference for open-source models including Llama 3, Mixtral, and Gemma. Your API key is stored securely and used only for processing your document queries.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
            
            // Messages
            errorMessage?.let {
                Spacer(modifier = Modifier.height(16.dp))
                ErrorMessage(
                    message = it,
                    onDismiss = { errorMessage = null }
                )
            }
            
            successMessage?.let {
                Spacer(modifier = Modifier.height(16.dp))
                SuccessMessage(
                    message = it,
                    onDismiss = { successMessage = null }
                )
            }
        }
    }
}
