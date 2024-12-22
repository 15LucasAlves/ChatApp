package com.example.chatapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.chatapp.ui.theme.ChatAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChatAppTheme {
                var isLoginScreen by remember { mutableStateOf(true) }
                var userEmail by remember { mutableStateOf("") }
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (isLoginScreen) {
                        LoginScreen(onLogin = { email, _ ->
                            userEmail = email
                            isLoginScreen = false // Navigate to UserDetailsScreen
                        }, onNavigateToRegister = { isLoginScreen = false })
                    } else {
                        if (userEmail.isNotEmpty()) {
                            UserDetailsScreen(email = userEmail)
                        } else {
                            RegisterScreen(onRegister = { email, _ ->
                                userEmail = email
                                isLoginScreen = true // Navigate back to LoginScreen
                            }, onNavigateToLogin = { isLoginScreen = true })
                        }
                    }
                }
            }
        }
    }
}