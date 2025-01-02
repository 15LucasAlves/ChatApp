package com.example.chatapp.ui.users

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chatapp.data.model.User
import com.example.chatapp.ui.chat.ChatViewModel
import  androidx.compose.material.icons.filled.AccountCircle

@Composable
fun UserSelectionScreen(
    modifier: Modifier = Modifier,
    currentUserEmail: String,
    viewModel: UserSelectionViewModel = viewModel(),
    onUserSelected: (String) -> Unit,
    chatViewModel: ChatViewModel = viewModel(),
    onNavigateToProfile: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val users by viewModel.users.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadUsers(currentUserEmail)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        TextField(
            value = searchQuery,
            onValueChange = { 
                searchQuery = it
                viewModel.searchUsers(it)
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search users...") }
        )

        if (error.isNotEmpty()) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(users) { user ->
                UserItem(user = user, onClick = { onUserSelected(user.email)
                    chatViewModel.setRecipientEmail(user.email)
                chatViewModel.setUserEmail(currentUserEmail)})
            }
        }
    }
    //replaces top button for view profile with more lowkey button on left side of the screen on the bottom
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        FloatingActionButton(
            onClick = onNavigateToProfile,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ) {
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = "User Profile Icon"
            )
        }
    }
}

@Composable
private fun UserItem(user: User, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Text(
                text = user.username ?: user.email,
                style = MaterialTheme.typography.titleMedium
            )
            if (user.username != null) {
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
} 