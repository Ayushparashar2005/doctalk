package com.doctalk.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.doctalk.app.presentation.screens.auth.AuthScreen
import com.doctalk.app.presentation.screens.auth.LoginScreen
import com.doctalk.app.presentation.screens.auth.SignUpScreen
import com.doctalk.app.presentation.screens.chat.ChatScreen
import com.doctalk.app.presentation.screens.home.HomeScreen
import com.doctalk.app.presentation.screens.settings.GroqSettingsScreen
import com.doctalk.app.presentation.screens.splash.SplashScreen
import com.doctalk.app.presentation.screens.upload.UploadScreen
import com.doctalk.app.viewmodel.AuthViewModel
import com.doctalk.app.viewmodel.AuthState

/**
 * Main navigation component for the DocTalk application
 */
@Composable
fun DocTalkNavigation(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel
) {
    val authState by authViewModel.authState.collectAsState()
    
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        // Splash Screen
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigationComplete = { destination ->
                    navController.navigate(destination.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        
        // Authentication Screens
        composable(Screen.Auth.route) {
            AuthScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route)
                },
                onNavigateToSignUp = {
                    navController.navigate(Screen.SignUp.route)
                }
            )
        }
        
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToSignUp = {
                    navController.navigate(Screen.SignUp.route) {
                        popUpTo(Screen.Auth.route)
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.SignUp.route) {
            SignUpScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Auth.route)
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Main App Screens
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToUpload = {
                    navController.navigate(Screen.Upload.route)
                },
                onNavigateToChat = { documentId, documentName ->
                    navController.navigate(Screen.Chat.createRoute(documentId, documentName))
                },
                onNavigateToAuth = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigateToGroqSettings = {
                    navController.navigate(Screen.GroqSettings.route)
                }
            )
        }
        
        composable(Screen.Upload.route) {
            UploadScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToChat = { documentId, documentName ->
                    navController.navigate(Screen.Chat.createRoute(documentId, documentName)) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }
        
        composable(Screen.Chat.route) { backStackEntry ->
            val documentId = backStackEntry.arguments?.getString("documentId") ?: ""
            val documentName = backStackEntry.arguments?.getString("documentName") ?: ""
            
            ChatScreen(
                documentId = documentId,
                documentName = documentName,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Groq Settings Screen
        composable(Screen.GroqSettings.route) {
            GroqSettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
    
    // Handle authentication state changes
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                if (navController.currentDestination?.route != Screen.Home.route &&
                    navController.currentDestination?.route != Screen.Chat.route &&
                    navController.currentDestination?.route != Screen.Upload.route) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            }
            is AuthState.NotAuthenticated -> {
                if (navController.currentDestination?.route == Screen.Home.route ||
                    navController.currentDestination?.route == Screen.Chat.route ||
                    navController.currentDestination?.route == Screen.Upload.route) {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            }
            else -> {
                // Handle other states as needed
            }
        }
    }
}

/**
 * Sealed class defining app screens
 */
sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Auth : Screen("auth")
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object Home : Screen("home")
    object Upload : Screen("upload")
    object Chat : Screen("chat/{documentId}/{documentName}") {
        
        fun createRoute(documentId: String, documentName: String): String {
            return "chat/$documentId/$documentName"
        }
    }
    object GroqSettings : Screen("groq_settings")
}
