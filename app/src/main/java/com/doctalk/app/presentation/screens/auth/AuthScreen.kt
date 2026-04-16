package com.doctalk.app.presentation.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.doctalk.app.R
import com.doctalk.app.presentation.components.DocTalkButton
import com.doctalk.app.presentation.components.DocTalkOutlinedButton
import com.doctalk.app.presentation.theme.DocTalkTextStyles

/**
 * Authentication welcome screen
 */
@Composable
fun AuthScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo
            androidx.compose.foundation.Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "DocTalk Logo",
                modifier = Modifier.size(120.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // App Name
            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Tagline
            Text(
                text = "Chat with your documents using AI",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Sign Up Button
            DocTalkButton(
                text = stringResource(id = R.string.sign_up),
                onClick = onNavigateToSignUp,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Sign In Button
            DocTalkOutlinedButton(
                text = stringResource(id = R.string.sign_in),
                onClick = onNavigateToLogin,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Terms and Privacy
            Text(
                text = "By continuing, you agree to our Terms of Service and Privacy Policy",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
