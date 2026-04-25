package com.doctalk.app.presentation.screens.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.doctalk.app.R
import com.doctalk.app.presentation.navigation.Screen
import kotlinx.coroutines.delay

/**
 * Splash screen that shows app branding and handles initial navigation
 */
@Composable
fun SplashScreen(
    onNavigationComplete: (Screen) -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }
    val alphaAnimation by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000)
    )
    
    LaunchedEffect(key1 = true) {
        startAnimation = true
        delay(2000) // Show splash for 2 seconds
        
        // Determine where to navigate based on auth state
        // This will be handled by the navigation component based on AuthViewModel state
        onNavigationComplete(Screen.Auth) // Will be overridden by auth state logic
    }
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.mipmap.ic_launcher),
            contentDescription = "DocTalk Logo",
            modifier = Modifier
                .size(120.dp)
                .alpha(alphaAnimation)
        )
    }
}
