package com.doctalk.app.presentation.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.doctalk.app.R
import com.doctalk.app.data.model.Document
import com.doctalk.app.data.model.DocumentStatus
import com.doctalk.app.presentation.components.DocTalkButton
import com.doctalk.app.presentation.components.ErrorMessage
import com.doctalk.app.presentation.components.SuccessMessage
import com.doctalk.app.presentation.components.TopAppBar
import com.doctalk.app.presentation.screens.home.components.DocumentCard
import com.doctalk.app.presentation.screens.home.components.StatsCard
import com.doctalk.app.presentation.navigation.Screen
import com.doctalk.app.viewmodel.HomeViewModel

/**
 * Home screen showing user's documents and statistics
 */
@Composable
fun HomeScreen(
    onNavigateToUpload: () -> Unit,
    onNavigateToChat: (String, String) -> Unit,
    onNavigateToAuth: () -> Unit,
    onNavigateToGroqSettings: () -> Unit,
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    val currentUser by homeViewModel.currentUser.collectAsState()
    val documents by homeViewModel.documents.collectAsState()
    val stats by homeViewModel.stats.collectAsState()
    val isLoading by homeViewModel.isLoading.collectAsState()
    val errorMessage by homeViewModel.errorMessage.collectAsState()
    val successMessage by homeViewModel.successMessage.collectAsState()
    
    var showUserMenu by remember { mutableStateOf(false) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            TopAppBar(
                title = stringResource(id = R.string.my_documents),
                actions = {
                    IconButton(onClick = { showUserMenu = !showUserMenu }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu"
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showUserMenu,
                        onDismissRequest = { showUserMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Groq Settings") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Groq Settings"
                                )
                            },
                            onClick = {
                                showUserMenu = false
                                onNavigateToGroqSettings()
                            }
                        )
                        
                        DropdownMenuItem(
                            text = { Text("Sign Out") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = "Sign Out"
                                )
                            },
                            onClick = {
                                showUserMenu = false
                                homeViewModel.signOut()
                            }
                        )
                    }
                }
            )
            
            // Content
            if (isLoading && documents.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // User Info Section
                    item {
                        currentUser?.let { user ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = "Welcome back, ${user.displayName}!",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = user.email,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Stats Section
                    item {
                        StatsCard(
                            totalDocuments = stats.totalDocuments,
                            processedDocuments = stats.processedDocuments,
                            processingDocuments = stats.processingDocuments,
                            failedDocuments = stats.failedDocuments
                        )
                    }
                    
                    // Quick Actions
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DocTalkButton(
                                text = "Upload Document",
                                onClick = onNavigateToUpload,
                                modifier = Modifier.weight(1f),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                            
                            if (stats.processedDocuments > 0) {
                                DocTalkButton(
                                    text = "Start Chat",
                                    onClick = { 
                                        val firstDoc = homeViewModel.getProcessedDocuments().firstOrNull()
                                        firstDoc?.let { doc ->
                                            onNavigateToChat(doc.id, doc.fileName)
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Chat,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                    
                    // Documents Section
                    item {
                        Text(
                            text = "Recent Documents",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    if (documents.isEmpty()) {
                        item {
                            EmptyState(
                                onUploadClick = onNavigateToUpload
                            )
                        }
                    } else {
                        items(documents.take(10)) { document ->
                            DocumentCard(
                                document = document,
                                onChatClick = { 
                                    if (document.status == DocumentStatus.PROCESSED) {
                                        onNavigateToChat(document.id, document.fileName)
                                    }
                                },
                                onDeleteClick = { /* TODO: Implement delete */ }
                            )
                        }
                    }
                }
            }
        }
        
        // Floating Action Button
        FloatingActionButton(
            onClick = onNavigateToUpload,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Upload Document"
            )
        }
        
        // Error and Success Messages
        errorMessage?.let {
            ErrorMessage(
                message = it,
                onDismiss = { homeViewModel.clearError() },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            )
        }
        
        successMessage?.let {
            SuccessMessage(
                message = it,
                onDismiss = { homeViewModel.clearSuccessMessage() },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            )
        }
    }
}

/**
 * Empty state component when no documents are available
 */
@Composable
private fun EmptyState(
    onUploadClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.DocumentScanner,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource(id = R.string.no_documents),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = stringResource(id = R.string.upload_first_document),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            DocTalkButton(
                text = stringResource(id = R.string.upload_document),
                onClick = onUploadClick
            )
        }
    }
}
