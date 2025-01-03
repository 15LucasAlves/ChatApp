package com.example.chatapp.ui.users

import android.media.Image
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
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
                UserItem(user = user, currentUserEmail, onClick = { onUserSelected(user.email)
                    chatViewModel.initChat(currentUserEmail, user.email)
                    chatViewModel.setRecipientEmail(user.email)
                chatViewModel.setUserEmail(currentUserEmail) })
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
private fun UserItem(
    user: User,
    currentUserEmail: String,
    onClick: () -> Unit,
    viewModel: UserSelectionViewModel = viewModel(),
    ) {

    val repository = MessageRepository()
    val chatId = viewModel.chatId(currentUserEmail,user.email)
    Log.d("userslec", "$chatId")
    val messages = repository.fetchLastMessage(chatId).collectAsState(initial = Result.Loading)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.CenterHorizontally),
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
        Column(
            modifier = Modifier
                .absolutePadding(0.dp,0.dp,50.dp,0.dp)
        ){
            user.photoUrl?.let { imageUrl ->
                Image(
                    painter = rememberAsyncImagePainter(imageUrl),
                    contentDescription = "Userpic",
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RectangleShape) //so it's a rectangle pic
                        .align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}