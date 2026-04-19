package com.doctalk.app.presentation.screens.chat.components

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.doctalk.app.data.model.Message
import com.doctalk.app.data.model.MessageType
import com.doctalk.app.data.model.getFormattedTime
import com.doctalk.app.data.model.isFromAI
import com.doctalk.app.data.model.isFromUser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Chat message bubble component
 */
@Composable
fun ChatMessageBubble(
    message: Message
) {
    val isUserMessage = message.isFromUser()
    val isAIMessage = message.isFromAI()
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUserMessage) Arrangement.End else Arrangement.Start
    ) {
        if (isAIMessage) {
            // AI Avatar
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AI",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        Column(
            horizontalAlignment = if (isUserMessage) Alignment.End else Alignment.Start,
            modifier = Modifier.weight(1f)
        ) {
            // Message bubble
            Card(
                shape = RoundedCornerShape(
                    topStart = if (isUserMessage) 16.dp else 4.dp,
                    topEnd = if (isUserMessage) 4.dp else 16.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isUserMessage) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    )
                ) {
                    // Message content
                    if (message.isTyping) {
                        TypingIndicator()
                    } else {
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isUserMessage) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = if (isAIMessage) FontWeight.Normal else FontWeight.Medium
                        )
                    }
                    
                    // Timestamp
                    if (!message.isTyping) {
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = message.getFormattedTime(),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isUserMessage) {
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            },
                            textAlign = if (isUserMessage) TextAlign.End else TextAlign.Start
                        )
                    }
                }
            }
            
            // Message metadata (for AI messages)
            if (isAIMessage && message.metadata != null && !message.isTyping) {
                Spacer(modifier = Modifier.height(4.dp))
                
                message.metadata?.let { metadata ->
                    Row(
                        modifier = Modifier.padding(start = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (metadata.contextUsed) {
                            Text(
                                text = "Context used",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                        }
                        
                        if (metadata.responseTime > 0) {
                            Text(
                                text = "  ·  ${metadata.responseTime}ms",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
        
        if (isUserMessage) {
            Spacer(modifier = Modifier.width(8.dp))
            
            // User Avatar placeholder
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.secondary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "U",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
