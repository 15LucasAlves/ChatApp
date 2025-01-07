@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
package com.example.chatapp.ui.chat

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.*
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

// The main composable function that represents the chat screen
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    userEmail: String,
    recipientEmail: String,
    onNavigateBack: () -> Unit,
    viewModel: ChatViewModel = viewModel(),
    userRepository: UserRepository = UserRepository()
) {
    // Holds the text that appears in the TextField
    var messageText by remember { mutableStateOf("") }

    // Holds the message object that the user long-pressed (to edit/delete)
    var selectedMessage by remember { mutableStateOf<Message?>(null) }

    // Indicates whether we are currently editing (instead of sending a new message)
    var isEditing by remember { mutableStateOf(false) }

    // Controls whether the bottom sheet is shown for "Edit / Delete / Copy"
    var showActionsSheet by remember { mutableStateOf(false) }

    // Observing data from the ViewModel
    val messages by viewModel.messages.collectAsState()
    val isSending by viewModel.isSending.collectAsState() // Lock state for sending

    // For searching messages
    var searchQuery by remember { mutableStateOf("") }

    // Used to fetch the recipient's user info
    var recipientUser by remember { mutableStateOf<User?>(null) }
    val scope = rememberCoroutineScope()

    // A lazy list to display messages
    val listState = rememberLazyListState()

    // Multiple images picking
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.forEach { uri -> viewModel.addImageUri(uri) }
    }

    // Fetch recipient info (like their username/photo)
    LaunchedEffect(recipientEmail) {
        scope.launch {
            recipientUser = userRepository.getUser(recipientEmail)
        }
    }

    // Search filtering
    val filteredMessages = if (searchQuery.isEmpty()) {
        messages
    } else {
        messages.filter { it.text.contains(searchQuery, ignoreCase = true) }
    }

    // Group messages by date
    val groupedMessages = filteredMessages.groupBy { message ->
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(message.timestamp))
    }

    Scaffold(
        topBar = {
            ChatTopBar(
                recipientUser = recipientUser,
                onNavigateBack = onNavigateBack,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onClearSearch = { searchQuery = "" }
            )
        }
    ) { padding ->
        // The main content of the chat screen
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Main list of messages
            LazyColumn(
                modifier = Modifier.weight(1f),
                reverseLayout = true,
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                groupedMessages.forEach { (date, dailyMessages) ->
                    items(dailyMessages) { message ->
                        ChatMessageItem(
                            message = message,
                            isOwnMessage = (message.senderEmail == userEmail),
                            onLongClick = {
                                selectedMessage = message
                                showActionsSheet = true // open bottom sheet
                            }
                        )
                    }
                    item {
                        // Date header
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

            // Show a row of selected images (thumbnails) JUST ABOVE the input
            val selectedUris by viewModel.selectedImageUris.collectAsState()
            if (selectedUris.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    selectedUris.forEach { uri ->
                        Box(
                            modifier = Modifier
                                .padding(8.dp)
                                .size(80.dp)
                        ) {
                            AsyncImage(
                                model = uri,
                                contentDescription = "Selected image preview",
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { viewModel.removeImageUri(uri) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove image",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Message input at the bottom
            MessageInput(
                messageText = messageText,
                isEditing = isEditing,
                onMessageChange = { messageText = it },
                onSendClick = {
                    if (messageText.isNotEmpty() || viewModel.selectedImageUris.value.isNotEmpty()) {
                        if (isEditing && selectedMessage != null) {
                            // EDIT existing message
                            viewModel.editMessage(selectedMessage!!.id, messageText)
                            isEditing = false
                            selectedMessage = null
                        } else {
                            // SEND new message
                            viewModel.sendMessage(messageText, userEmail, recipientEmail)
                        }
                        messageText = ""
                    }
                },
                onAttachClick = {
                    imagePickerLauncher.launch("image/*")
                },
                isSending = isSending
            )
        }

        // Show bottom sheet if showActionsSheet == true and we have a selectedMessage
        if (showActionsSheet && selectedMessage != null) {
            MessageActionsBottomSheet(
                message = selectedMessage!!,
                isOwnMessage = (selectedMessage?.senderEmail == userEmail),
                onDismiss = {
                    showActionsSheet = false
                },
                onDelete = {
                    viewModel.deleteMessage(selectedMessage!!.id)
                    selectedMessage = null
                    showActionsSheet = false
                },
                onEdit = {
                    // Switch to "edit mode"
                    isEditing = true
                    messageText = selectedMessage!!.text
                    // Close the sheet
                    showActionsSheet = false
                }
            )
        }
    }
}

// The top bar of the chat screen, including the search functionality
@Composable
fun ChatTopBar(
    recipientUser: User?,
    onNavigateBack: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit
) {
    var isSearchActive by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            if (isSearchActive) {
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search messages...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 16.dp),
                    maxLines = 1,
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent
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

// Helper function to format the date header for the message list
fun formatDateHeader(date: String): String {
    val parsedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)
    return SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(parsedDate ?: Date())
}

// Composable function for a single chat message item
@Composable
fun ChatMessageItem(
    message: Message,
    isOwnMessage: Boolean,
    onLongClick: () -> Unit
) {
    // Check if there's at least one image
    val hasImages = message.imageUrls?.isNotEmpty() == true

    // Decide the bubble's modifier:
    // If images exist, fill the available space (up to 300.dp).
    // Otherwise, let the text bubble wrap its content (also up to 300.dp).
    val bubbleModifier = if (hasImages) {
        Modifier
            .widthIn(max = 300.dp)  // never exceed 300.dp
            .fillMaxWidth()         // fill bubble width if images are present
    } else {
        Modifier.widthIn(max = 300.dp)  // text-only messages can wrap up to 300.dp
    }

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
            modifier = bubbleModifier
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
                // If there are images, stretch each image to fill the bubble width
                if (hasImages) {
                    message.imageUrls?.forEach { singleUrl ->
                        Spacer(modifier = Modifier.height(4.dp))
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(singleUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Message image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // Show text (if any). If there's images, fill the bubble width;
                // if no images, just wrap content within max 300.dp.
                if (message.text.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val textModifier = if (hasImages) {
                        Modifier.fillMaxWidth()
                    } else {
                        Modifier.wrapContentWidth() // text alone can wrap naturally
                    }
                    Text(
                        text = message.text,
                        color = Color.White,
                        textAlign = if (isOwnMessage) TextAlign.End else TextAlign.Start,
                        modifier = textModifier
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Timestamp + read indicators
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
                            imageVector = when {
                                message.readBy.size > 1 -> Icons.Default.DoneAll
                                else -> Icons.Default.Done
                            },
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

// This function is a Composable function that creates a bottom sheet for message actions.
// It takes in a message, a boolean indicating if the message is the user's own message,
// and three callback functions for dismissing the bottom sheet, deleting the message, and editing the message.
@Composable
fun MessageActionsBottomSheet(
    message: Message,
    isOwnMessage: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    // Get the current context and the clipboard manager
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Create a modal bottom sheet with the provided onDismissRequest callback
    ModalBottomSheet(onDismissRequest = onDismiss) {
        // Create a column to hold the list items
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // If the message is the user's own message and the message text is not empty, show the "Edit" option
            if (isOwnMessage && message.text.isNotEmpty()) {
                ListItem(
                    headlineContent = { Text("Edit Message") },
                    leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) },
                    modifier = Modifier.clickable {
                        onEdit()
                    }
                )
            }

            // If the message is the user's own message, show the "Delete" option
            if (isOwnMessage) {
                ListItem(
                    headlineContent = { Text("Delete Message") },
                    leadingContent = { Icon(Icons.Default.Delete, contentDescription = null) },
                    modifier = Modifier.clickable {
                        onDelete()
                    }
                )
            }

            // If the message text is not empty, show the "Copy" option
            if (message.text.isNotEmpty()) {
                ListItem(
                    headlineContent = { Text("Copy Text") },
                    leadingContent = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                    modifier = Modifier.clickable {
                        // Copy the message text to the clipboard and show a toast
                        clipboardManager.setText(AnnotatedString(message.text))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        onDismiss() // close the bottom sheet after copying
                    }
                )
            }
        }
    }
}

// This function is a Composable function that creates a message input UI.
// It takes in the current message text, a boolean indicating if the user is editing the message,
// and callback functions for changing the message text, sending the message, attaching a file, and the sending state.
@Composable
fun MessageInput(
    messageText: String,
    isEditing: Boolean,
    onMessageChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onAttachClick: () -> Unit,
    isSending: Boolean
) {
    // Create a row to hold the input UI elements
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Create an icon button for attaching a file, disabled while sending
        IconButton(onClick = onAttachClick, enabled = !isSending) {
            Icon(Icons.Default.AttachFile, contentDescription = "Attach")
        }

        // Create a box to hold the text input field
        Box(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 56.dp, max = 150.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 8.dp)
        ) {
            // Create a scrollable column to hold the text input field
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                // Create a text field for the message input, disabled while sending
                TextField(
                    value = messageText,
                    onValueChange = onMessageChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(if (isEditing) "Edit message..." else "Type a message...")
                    },
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    enabled = !isSending
                )
            }
        }

        // Create a spacer between the text input and the send button
        Spacer(modifier = Modifier.width(8.dp))

        // Create an icon button for sending the message, disabled while sending
        IconButton(
            onClick = onSendClick,
            enabled = !isSending // disable while sending to avoid duplicates
        ) {
            Icon(
                imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Send,
                contentDescription = if (isEditing) "Save" else "Send"
            )
        }
    }
}