package com.doctalk.app.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.doctalk.app.presentation.navigation.DocTalkNavigation
import com.doctalk.app.presentation.theme.DocTalkTheme
import com.doctalk.app.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity for the DocTalk application
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            DocTalkTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val authViewModel: AuthViewModel = hiltViewModel()
                    
                    DocTalkNavigation(
                        navController = navController,
                        authViewModel = authViewModel
                    )
                }
            }
        }
    }
}
