@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
package com.example.chatapp.ui.chat

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.chatapp.data.model.Message
import com.example.chatapp.data.model.User
import com.example.chatapp.data.repository.UserRepository
import com.example.chatapp.util.DateFormatter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*


@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    userEmail: String,
    recipientEmail: String,
    onNavigateBack: () -> Unit,
    viewModel: ChatViewModel = viewModel(),
    userRepository: UserRepository = UserRepository()
) {
    var messageText by remember { mutableStateOf("") }
    var selectedMessage by remember { mutableStateOf<Message?>(null) }
    var isEditing by remember { mutableStateOf(false) }
    val messages by viewModel.messages.collectAsState()
    val listState = rememberLazyListState()
    var recipientUser by remember { mutableStateOf<User?>(null) }
    var searchQuery by remember { mutableStateOf("") } // State for search query

    val scope = rememberCoroutineScope()

    // Fetch recipient user info
    LaunchedEffect(recipientEmail) {
        scope.launch {
            recipientUser = userRepository.getUser(recipientEmail)
        }
    }

    // Filter messages based on search query
    val filteredMessages = if (searchQuery.isEmpty()) {
        messages
    } else {
        messages.filter { it.text.contains(searchQuery, ignoreCase = true) }
    }

    // Group filtered messages by day
    val groupedMessages = filteredMessages.groupBy { message ->
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(message.timestamp))
    }

    Scaffold(
        topBar = {
            ChatTopBar(
                recipientUser = recipientUser,
                onNavigateBack = onNavigateBack,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it }, // Update the query dynamically
                onClearSearch = { searchQuery = "" } // Clear search query
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // LazyColumn with grouped messages
            LazyColumn(
                modifier = Modifier.weight(1f),
                reverseLayout = true,
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                groupedMessages.forEach { (date, messagesForDay) ->
                    items(messagesForDay) { message ->
                        ChatMessageItem(
                            message = message,
                            isOwnMessage = message.senderEmail == userEmail,
                            onLongClick = { selectedMessage = message }
                        )
                    }
                    item {
                        // Display date header
                        Text(
                            text = formatDateHeader(date),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Input box sticks to the bottom
            MessageInput(
                messageText = messageText,
                isEditing = isEditing,
                onMessageChange = { messageText = it },
                onSendClick = {
                    if (messageText.isNotEmpty()) {
                        if (isEditing && selectedMessage != null) {
                            viewModel.editMessage(selectedMessage!!.id, messageText)
                            isEditing = false
                            selectedMessage = null
                        } else {
                            viewModel.sendMessage(messageText, userEmail, recipientEmail)
                        }
                        messageText = ""
                    }
                },
                onAttachClick = {}
            )
        }

        // Bottom Sheet for actions
        if (selectedMessage != null) {
            MessageActionsBottomSheet(
                message = selectedMessage!!,
                isOwnMessage = selectedMessage?.senderEmail == userEmail,
                onDismiss = { selectedMessage = null },
                onDelete = {
                    viewModel.deleteMessage(selectedMessage!!.id)
                    selectedMessage = null
                },
                onEdit = {
                    isEditing = true
                    messageText = selectedMessage!!.text
                }
            )
        }
    }
}

fun formatDateHeader(date: String): String {
    val parsedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)
    val formattedDate = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(parsedDate)
    return formattedDate
}
@Composable
fun ChatTopBar(
    recipientUser: User?,
    onNavigateBack: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit, // Callback to update query
    onClearSearch: () -> Unit
) {
    var isSearchActive by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            if (isSearchActive) {
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange, // Dynamically updates the query
                    placeholder = { Text("Search messages...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 16.dp),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    recipientUser?.photoUrl?.let { imageUrl ->
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "User profile picture",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(text = recipientUser?.username ?: "Unknown User")
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Navigate Back")
            }
        },
        actions = {
            IconButton(onClick = {
                if (isSearchActive) {
                    onClearSearch()
                }
                isSearchActive = !isSearchActive
            }) {
                Crossfade(targetState = isSearchActive) { state ->
                    if (state) {
                        Icon(Icons.Default.Close, contentDescription = "Close search")
                    } else {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            }
        }
    )
}

@Composable
fun ChatMessageItem(
    message: Message,
    isOwnMessage: Boolean,
    onLongClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick
            ),
        horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = if (isOwnMessage)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.secondary,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column {
                if (message.imageUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(message.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Message image",
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                    text = message.text,
                    color = Color.White,
                    textAlign = if (isOwnMessage) TextAlign.End else TextAlign.Start
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = DateFormatter.formatMessageTimestamp(message.timestamp),
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (isOwnMessage) {
                        Icon(
                            imageVector = if (message.readBy.size > 1)
                                Icons.Default.DoneAll
                            else
                                Icons.Default.Done,
                            contentDescription = if (message.readBy.size > 1) "Read" else "Sent",
                            tint = if (message.readBy.size > 1)
                                Color.Blue.copy(alpha = 0.7f)
                            else
                                Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageActionsBottomSheet(
    message: Message,
    isOwnMessage: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (isOwnMessage) {
                ListItem(
                    headlineContent = { Text("Edit Message") },
                    leadingContent = {
                        Icon(Icons.Default.Edit, contentDescription = null)
                    },
                    modifier = Modifier.clickable(onClick = onEdit)
                )
                ListItem(
                    headlineContent = { Text("Delete Message") },
                    leadingContent = {
                        Icon(Icons.Default.Delete, contentDescription = null)
                    },
                    modifier = Modifier.clickable(onClick = onDelete)
                )
            }
            ListItem(
                headlineContent = { Text("Copy Text") },
                leadingContent = {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                },
                modifier = Modifier.clickable(onClick ={
                    val copiedtext = message.text
                    clipboardManager.setText(AnnotatedString(copiedtext))
                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_LONG).show()
                })
            )
        }
    }
}

@Composable
fun MessageInput(
    messageText: String,
    isEditing: Boolean,
    onMessageChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onAttachClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onAttachClick) {
            Icon(Icons.Default.AttachFile, contentDescription = "Attach")
        }

        TextField(
            value = messageText,
            onValueChange = onMessageChange,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text(if (isEditing) "Edit message..." else "Type a message")
            },
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(onClick = onSendClick) {
            Icon(
                imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Send,
                contentDescription = if (isEditing) "Save" else "Send"
            )
        }
    }
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}