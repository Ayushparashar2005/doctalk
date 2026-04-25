package com.doctalk.app.presentation.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.doctalk.app.data.model.Document
import com.doctalk.app.data.model.DocumentStatus
import com.doctalk.app.data.model.getDisplayName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Card component for displaying a document
 */
@Composable
fun DocumentCard(
    document: Document,
    onChatClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with file info and menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // File icon and name
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = getFileIcon(document.fileType),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = getFileIconColor(document.fileType)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = document.fileName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Text(
                            text = formatFileSize(document.fileSize),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                
                // Menu button
                IconButton(
                    onClick = { showMenu = !showMenu }
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options"
                    )
                }
            }
            
            // Dropdown menu
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                if (document.status == DocumentStatus.PROCESSED) {
                    DropdownMenuItem(
                        text = { Text("Start Chat") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            showMenu = false
                            onChatClick()
                        }
                    )
                }
                
                DropdownMenuItem(
                    text = { Text("Delete") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = {
                        showMenu = false
                        onDeleteClick()
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Status section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status indicator and text
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusIndicator(status = document.status)
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = document.status.getDisplayName(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = getStatusColor(document.status)
                    )
                }
                
                // Upload date
                Text(
                    text = formatDate(document.uploadedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            // Progress indicator for uploading/processing
            if (document.status == DocumentStatus.UPLOADING || document.status == DocumentStatus.PROCESSING) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = getStatusColor(document.status)
                )
            }
            
            // Error message if failed
            if (document.status == DocumentStatus.FAILED && document.errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = document.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            // Additional info for processed documents
            if (document.status == DocumentStatus.PROCESSED) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    document.pageCount?.let {
                        Text(
                            text = "$it pages",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    
                    document.wordCount?.let {
                        Text(
                            text = "$it words",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Status indicator component
 */
@Composable
private fun StatusIndicator(status: DocumentStatus) {
    val color = getStatusColor(status)
    
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(RoundedCornerShape(50))
            .background(color)
    )
}

/**
 * Gets the appropriate icon for file type
 */
private fun getFileIcon(fileType: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (fileType.lowercase()) {
        "pdf" -> Icons.Default.Description
        "txt" -> Icons.Default.Description
        else -> Icons.Default.Description
    }
}

/**
 * Gets the color for file icon
 */
private fun getFileIconColor(fileType: String): Color {
    return when (fileType.lowercase()) {
        "pdf" -> Color(0xFFE53935) // Red for PDF
        "txt" -> Color(0xFF43A047) // Green for TXT
        else -> Color(0xFF757575) // Gray for others
    }
}

/**
 * Gets the color for document status
 */
private fun getStatusColor(status: DocumentStatus): Color {
    return when (status) {
        DocumentStatus.UPLOADING -> Color(0xFF1976D2) // Blue
        DocumentStatus.PROCESSING -> Color(0xFFFB8C00) // Orange
        DocumentStatus.PROCESSED -> Color(0xFF43A047) // Green
        DocumentStatus.FAILED -> Color(0xFFE53935) // Red
        DocumentStatus.DELETED -> Color(0xFF757575) // Gray
    }
}

/**
 * Formats file size in human readable format
 */
private fun formatFileSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    
    return when {
        mb >= 1 -> String.format("%.1f MB", mb)
        kb >= 1 -> String.format("%.1f KB", kb)
        else -> "$bytes B"
    }
}

/**
 * Formats date in human readable format
 */
private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val format = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return format.format(date)
}
