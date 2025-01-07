package com.example.chatapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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

// The main activity of the chat app
class MainActivity : ComponentActivity() {
    // Declare a UserPreferences object to manage user credentials
    private lateinit var userPreferences: UserPreferences

    // Override the onCreate method to set up the activity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize the UserPreferences object with the application context
        userPreferences = UserPreferences(applicationContext)

        // Set the content of the activity using Compose
        setContent {
            // Use the ChatAppTheme to style the app
            ChatAppTheme {
                // Declare mutable state variables to track the current screen and user/recipient emails
                var currentScreen by remember { mutableStateOf("splash") }
                var userEmail by remember { mutableStateOf("") }
                var recipientEmail by remember { mutableStateOf("") }

                // Check for saved user credentials when the app starts
                LaunchedEffect(Unit) {
                    userPreferences.userEmail.collect { savedEmail ->
                        if (savedEmail != null) {
                            // If a saved email is found, set the user email and navigate to the user selection screen
                            userEmail = savedEmail
                            currentScreen = "userSelection"
                        } else {
                            // If no saved email is found, navigate to the login screen
                            currentScreen = "login"
                        }
                    }
                }

                // Use the Scaffold composable to create the app's layout
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Render the appropriate screen based on the current screen state
                    when (currentScreen) {
                        "login" -> {
                            // Render the LoginScreen
                            LoginScreen(
                                modifier = Modifier.padding(innerPadding),
                                onLogin = { email, password, loginResult ->
                                    // Update the user email and save the credentials if the login is successful
                                    userEmail = email
                                    if (loginResult) {
                                        Log.e("Main", "passed")
                                        lifecycleScope.launch {
                                            userPreferences.saveUserCredentials(email, password)
                                        }
                                        currentScreen = "userSelection"
                                    } else {
                                        Log.e("Main", "failed")
                                        currentScreen = "login"
                                    }
                                },
                                onNavigateToRegister = { currentScreen = "register" }
                            )
                        }
                        "profile" -> {
                            // Render the ProfileScreen
                            ProfileScreen(
                                modifier = Modifier.padding(innerPadding),
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
                            // Render the UserSelectionScreen
                            UserSelectionScreen(
                                modifier = Modifier.padding(innerPadding),
                                currentUserEmail = userEmail,
                                onUserSelected = { selectedUserEmail ->
                                    recipientEmail = selectedUserEmail
                                    currentScreen = "chat"
                                },
                                onNavigateToProfile = { currentScreen = "profile" }
                            )
                        }
                        "chat" -> {
                            // Render the ChatScreen
                            ChatScreen(
                                modifier = Modifier.padding(innerPadding),
                                userEmail = userEmail,
                                recipientEmail = recipientEmail,
                                onNavigateBack = { currentScreen = "userSelection" }
                            )
                        }
                        "register" -> {
                            // Render the RegisterScreen
                            RegisterScreen(
                                modifier = Modifier.padding(innerPadding),
                                onRegister = { email, _ ->
                                    userEmail = email
                                    currentScreen = "userDetails"
                                },
                                onNavigateToLogin = { currentScreen = "login" }
                            )
                        }
                        "userDetails" -> {
                            // Render the ProfileScreen in initial setup mode
                            ProfileScreen(
                                modifier = Modifier.padding(innerPadding),
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