package com.example.chatapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.chatapp.data.local.UserPreferences
import com.example.chatapp.ui.auth.LoginScreen
import com.example.chatapp.ui.auth.RegisterScreen
import com.example.chatapp.ui.profile.ProfileScreen
import com.example.chatapp.ui.users.UserSelectionScreen
import com.example.chatapp.ui.theme.ChatAppTheme
import com.example.chatapp.ui.chat.ChatScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userPreferences = UserPreferences(applicationContext)

        setContent {
            ChatAppTheme {
                var currentScreen by remember { mutableStateOf("splash") }
                var userEmail by remember { mutableStateOf("") }
                var recipientEmail by remember { mutableStateOf("") }

                // Check for saved credentials
                LaunchedEffect(Unit) {
                    userPreferences.userEmail.collect { savedEmail ->
                        if (savedEmail != null) {
                            userEmail = savedEmail
                            currentScreen = "userSelection"
                        } else {
                            currentScreen = "login"
                        }
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when (currentScreen) {
                        "login" -> {
                            LoginScreen(
                                onLogin = { email, password ->
                                    userEmail = email
                                    // Save credentials
                                    lifecycleScope.launch {
                                        userPreferences.saveUserCredentials(email, password)
                                    }
                                    currentScreen = "userSelection"
                                },
                                onNavigateToRegister = { currentScreen = "register" }
                            )
                        }
                        "profile" -> {
                            ProfileScreen(
                                email = userEmail,
                                onNavigateBack = { currentScreen = "userSelection" },
                                isInitialSetup = false,
                                onLogout = {
                                    lifecycleScope.launch {
                                        userPreferences.clearUserCredentials()
                                        userEmail = ""
                                        currentScreen = "login"
                                    }
                                }
                            )
                        }
                        "userSelection" -> {
                            UserSelectionScreen(
                                currentUserEmail = userEmail,
                                onUserSelected = { selectedUserEmail ->
                                    recipientEmail = selectedUserEmail
                                    currentScreen = "chat"
                                },
                                onNavigateToProfile = { currentScreen = "profile" }
                            )
                        }
                        "chat" -> {
                            ChatScreen(
                                userEmail = userEmail,
                                recipientEmail = recipientEmail,
                                onNavigateBack = { currentScreen = "userSelection" }
                            )
                        }
                        "register" -> {
                            RegisterScreen(
                                onRegister = { email, _ ->
                                    userEmail = email
                                    currentScreen = "userDetails"
                                },
                                onNavigateToLogin = { currentScreen = "login" }
                            )
                        }
                        "userDetails" -> {
                            ProfileScreen(
                                email = userEmail,
                                onNavigateBack = { currentScreen = "login" },
                                isInitialSetup = true
                            )
                        }

                    }
                }
            }
        }
    }
}