package com.doctalk.app.presentation.screens.chat.components

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Typing indicator component for showing AI is typing
 */
@Composable
fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition()
    
    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, delayMillis = 0),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, delayMillis = 200),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    val alpha3 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, delayMillis = 400),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TypingDot(alpha = alpha1)
        Spacer(modifier = Modifier.width(4.dp))
        TypingDot(alpha = alpha2)
        Spacer(modifier = Modifier.width(4.dp))
        TypingDot(alpha = alpha3)
    }
}

/**
 * Individual typing dot
 */
@Composable
private fun TypingDot(alpha: Float) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .background(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                shape = CircleShape
            )
    )
}
