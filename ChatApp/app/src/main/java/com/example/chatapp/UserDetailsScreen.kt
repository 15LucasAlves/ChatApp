package com.example.chatapp

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun UserDetailsScreen(email: String) {
    Surface {
        Column {
            Text(text = "User Details", style = MaterialTheme.typography.headlineMedium)
            Text(text = "Email: $email", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewUserDetailsScreen() {
    UserDetailsScreen(email = "test@example.com")
}