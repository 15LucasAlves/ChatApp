package com.example.chatapp.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.chatapp.data.Result
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chatapp.ui.chat.ChatViewModel
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.materialIcon
import androidx.compose.ui.text.input.VisualTransformation

// This is the LoginScreen composable function that represents the login screen UI.
@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    onLogin: (email: String, password: String, loginResult: Boolean) -> Unit,
    loginViewModel: LoginViewModel = viewModel(),
    onNavigateToRegister: () -> Unit
) {
    // Declare mutable state variables to hold the email, password, error, login result, and error message.
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var loginResult by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isPasswordVisible by remember { mutableStateOf(false) }

    // Compose the login screen UI.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Display the "Login" text.
        Text(
            text = "Login",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Display the email input field.
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(Icons.Default.Email, contentDescription = "email")
            },
            maxLines = 1,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Display the password input field with a toggle for password visibility.
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = if(isPasswordVisible){
                VisualTransformation.None
            }else{
                PasswordVisualTransformation()
            },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(Icons.Default.Key, contentDescription = "pass")
            },
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    if(isPasswordVisible){
                        Icon(Icons.Default.Visibility, contentDescription = "Password Visible")
                    }else{
                        Icon(Icons.Default.VisibilityOff, contentDescription = "Password Not Visible")
                    }
                }
            }
        )

        // Display the error message, if any.
        if (error.isNotEmpty()) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Display the login button.
        Button(
            onClick = { onLogin(email, password, loginResult)
                loginViewModel.login(email, password, onSuccess = {
                    loginResult = true
                }, onFailure = {
                    errorMessage = "Authentication failed, incorrect email or password. Try Again."
                    loginResult = false
                })
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login")
        }

        // Display the error message, if any.
        if (errorMessage?.isNotEmpty() == true) {
            Text(
                text = errorMessage!!,
                color = Color.Red,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Display the "Don't have an account? Register" button.
        TextButton(
            onClick = onNavigateToRegister,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Don't have an account? Register")
        }
    }
}