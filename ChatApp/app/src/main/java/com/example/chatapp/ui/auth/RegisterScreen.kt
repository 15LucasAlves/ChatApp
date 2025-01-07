package com.example.chatapp.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * The RegisterScreen composable function is responsible for rendering the registration screen
 * of the chat app. It includes input fields for email, password, and confirm password, as well
 * as buttons for registration and navigating to the login screen.
 *
 * @param modifier The modifier to be applied to the screen.
 * @param onRegister A callback function that is called when the user successfully registers.
 * @param onNavigateToLogin A callback function that is called when the user wants to navigate to the login screen.
 * @param viewModel The RegisterViewModel instance to be used for the registration logic.
 */
@Composable
fun RegisterScreen(
    modifier: Modifier = Modifier,
    onRegister: (email: String, password: String) -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: RegisterViewModel = viewModel()
) {
    // Declare mutable state variables for email, password, and confirm password
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Collect the isLoading and error states from the view model
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    // Declare a mutable state variable to track the visibility of the password
    var isPasswordVisible by remember { mutableStateOf(false) }

    // Render the registration screen
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Display the "Register" title
        Text(
            text = "Register",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Render the email input field
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

        // Render the password input field with a toggle for password visibility
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

        Spacer(modifier = Modifier.height(16.dp))

        // Render the confirm password input field with the same password visibility toggle
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            visualTransformation = if(isPasswordVisible){
                VisualTransformation.None
            }else{
                PasswordVisualTransformation()
            },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(Icons.Default.Key, contentDescription = "pass")
            }
        )

        // Display any error messages
        if (error.isNotEmpty()) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Render the registration button
        Button(
            onClick = {
                // Check if the passwords match
                if (password == confirmPassword) {
                    // Call the register function from the view model
                    viewModel.register(email, password) { success ->
                        if (success) {
                            // Call the onRegister callback if the registration is successful
                            onRegister(email, password)
                        }
                    }
                } else {
                    // Set an error message if the passwords don't match
                    viewModel.setError("Passwords do not match")
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            // Display a loading indicator if the registration is in progress
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Register")
            }
        }

        // Render the button to navigate to the login screen
        TextButton(
            onClick = onNavigateToLogin,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Already have an account? Login")
        }
    }
}