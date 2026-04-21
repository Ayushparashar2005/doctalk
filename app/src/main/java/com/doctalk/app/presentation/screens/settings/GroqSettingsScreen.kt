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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.doctalk.app.network.groq.GroqModels
import com.doctalk.app.presentation.components.DocTalkButton
import com.doctalk.app.presentation.components.ErrorMessage
import com.doctalk.app.presentation.components.SuccessMessage
import com.doctalk.app.presentation.components.TopAppBar
import com.doctalk.app.utils.AppPreferences

@Composable
fun GroqSettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedModel by remember {
        mutableStateOf(AppPreferences.getSelectedGroqModel(context))
    }
    var isExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title = "Groq Model Settings",
            onBackClick = onNavigateBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(16.dp))

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

                    OutlinedTextField(
                        value = GroqModels.getModelDisplayName(selectedModel),
                        onValueChange = { },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            Text(
                                text = if (isExpanded) "▴" else "▾",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    )

                    DropdownMenu(
                        expanded = isExpanded,
                        onDismissRequest = { isExpanded = false }
                    ) {
                        GroqModels.ALL_MODELS.forEach { model ->
                            DropdownMenuItem(
                                text = { Text(GroqModels.getModelDisplayName(model)) },
                                onClick = {
                                    selectedModel = model
                                    isExpanded = false
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    DocTalkButton(
                        text = if (isExpanded) "Hide Models" else "Choose Model",
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Selected model: ${GroqModels.getModelDisplayName(selectedModel)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            DocTalkButton(
                text = "Save Model",
                onClick = {
                    AppPreferences.setSelectedGroqModel(context, selectedModel)
                    errorMessage = null
                    successMessage = "Saved. The backend will use ${GroqModels.getModelDisplayName(selectedModel)}."
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedModel.isNotBlank()
            )

            Spacer(modifier = Modifier.height(32.dp))

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
                        text = "Backend Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "DocTalk now uses the local backend for chat and SQLite for storage. The selected Groq model is forwarded to the backend, which handles the Groq API call.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

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
