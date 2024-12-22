package com.example.chatapp

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.TextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.runtime.remember
import kotlinx.coroutines.launch
import com.example.chatapp.data.DatabaseManager

@Composable
fun LoginScreen(onLogin: (String, String) -> Unit, onNavigateToRegister: () -> Unit) {
    val scope = rememberCoroutineScope()
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        TextField(
            value = email.value,
            onValueChange = { email.value = it },
            label = { Text("Email") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = password.value,
            onValueChange = { password.value = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            scope.launch {
                errorMessage = ""
                if (email.value.isEmpty() || password.value.isEmpty()) {
                    errorMessage = "All fields are required"
                } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.value).matches()) {
                    errorMessage = "Invalid email format"
                } else {
                    if (DatabaseManager.loginUser(email.value, password.value)) {
                        onLogin(email.value, password.value)
                    } else {
                        errorMessage = "Invalid email or password"
                    }
                }
            }
        }) {
            Text("Login")
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (errorMessage.isNotEmpty()) {
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onNavigateToRegister) {
            Text("Don't have an account? Register")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewLoginScreen() {
    LoginScreen(onLogin = { _, _ -> }, onNavigateToRegister = {})
}