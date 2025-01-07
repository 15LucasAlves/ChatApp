package com.example.chatapp.ui.users

import android.media.Image
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.chatapp.data.Result
import com.example.chatapp.data.repository.MessageRepository
import  androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import coil.compose.rememberAsyncImagePainter
import coil.compose.rememberImagePainter
import com.example.chatapp.data.model.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
@Composable
fun UserSelectionScreen(
    modifier: Modifier = Modifier,
    currentUserEmail: String,
    viewModel: UserSelectionViewModel = viewModel(),
    onUserSelected: (String) -> Unit,
    chatViewModel: ChatViewModel = viewModel(),
    onNavigateToProfile: () -> Unit
) {
    // Declare a mutable state variable to store the search query
    var searchQuery by remember { mutableStateOf("") }
    // Collect the users and error state from the view model
    val users by viewModel.users.collectAsState()
    val error by viewModel.error.collectAsState()

    // Load the users when the screen is first composed
    LaunchedEffect(Unit) {
        viewModel.loadUsers(currentUserEmail)
    }

    // Compose the main content of the screen
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Render a text field for searching users
        TextField(
            shape = MaterialTheme.shapes.medium,
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                viewModel.searchUsers(it)
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search users...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "search")
            },
            maxLines = 1,
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        // Display an error message if there is one
        if (error.isNotEmpty()) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // Render a lazy column to display the list of users
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(users) { user ->
                // Render a user item for each user
                UserItem(user = user, currentUserEmail, onClick = {
                    onUserSelected(user.email)
                    chatViewModel.initChat(currentUserEmail, user.email)
                    chatViewModel.setRecipientEmail(user.email)
                    chatViewModel.setUserEmail(currentUserEmail)
                })
            }
        }
    }

    // Render a floating action button to navigate to the user's profile
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
private fun UserItem(
    user: User,
    currentUserEmail: String,
    onClick: () -> Unit,
    viewModel: UserSelectionViewModel = viewModel(),
) {
    // Create a MessageRepository instance
    val repository = MessageRepository()
    // Get the chat ID for the current user and the given user
    val chatId = viewModel.chatId(currentUserEmail, user.email)
    // Fetch the last message for the chat ID and collect the result as a state
    val messages = repository.fetchLastMessage(chatId).collectAsState(initial = Result.Loading)

    // Render a card for the user item
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Render the user's profile image
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                user.photoUrl?.let { imageUrl ->
                    Image(
                        painter = rememberAsyncImagePainter(imageUrl),
                        contentDescription = "Userpic",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } ?: Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Default User Icon",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Render the user's username and email, as well as the last message in the chat
            Column(
                modifier = Modifier.fillMaxWidth()
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
                when (val result = messages.value) {
                    is Result.Loading -> {
                        Text(text = "Loading...", style = MaterialTheme.typography.bodySmall)
                    }
                    is Result.Error -> {
                        Text(text = "Error fetching message", style = MaterialTheme.typography.bodySmall)
                    }
                    is Result.Success -> {
                        val lastMessage = result.data.firstOrNull()
                        Text(
                            text = lastMessage?.text ?: "No messages yet",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}