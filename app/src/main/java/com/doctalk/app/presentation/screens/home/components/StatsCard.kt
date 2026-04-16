package com.doctalk.app.presentation.screens.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Statistics card showing document counts
 */
@Composable
fun StatsCard(
    totalDocuments: Int,
    processedDocuments: Int,
    processingDocuments: Int,
    failedDocuments: Int
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
                .padding(20.dp)
        ) {
            Text(
                text = "Document Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Total Documents
                StatItem(
                    icon = Icons.Default.DocumentScanner,
                    label = "Total",
                    value = totalDocuments.toString(),
                    color = MaterialTheme.colorScheme.primary
                )
                
                // Processed Documents
                StatItem(
                    icon = Icons.Default.CheckCircle,
                    label = "Processed",
                    value = processedDocuments.toString(),
                    color = MaterialTheme.colorScheme.primary
                )
                
                // Processing Documents
                StatItem(
                    icon = Icons.Default.Schedule,
                    label = "Processing",
                    value = processingDocuments.toString(),
                    color = MaterialTheme.colorScheme.secondary
                )
                
                // Failed Documents
                StatItem(
                    icon = Icons.Default.Error,
                    label = "Failed",
                    value = failedDocuments.toString(),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Individual statistic item
 */
@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = androidx.compose.ui.Modifier.size(24.dp)
        )
        
        Spacer(modifier = androidx.compose.ui.Modifier.height(4.dp))
        
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}
