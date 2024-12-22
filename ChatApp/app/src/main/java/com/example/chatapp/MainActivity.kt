package com.example.chatapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.chatapp.ui.theme.ChatAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChatAppTheme {
                var isLoginScreen by remember { mutableStateOf(true) }
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (isLoginScreen) {
                        LoginScreen(onLogin = { email, password ->
                            // Handle login logic here
                        }, onNavigateToRegister = { isLoginScreen = false })
                    } else {
                        RegisterScreen(onRegister = { email, password ->
                            // Handle registration logic here
                        }, onNavigateToLogin = { isLoginScreen = true })
                    }
                }
            }
        }
    }
}