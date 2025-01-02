package com.example.chatapp.ui.chat

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatapp.data.model.Message
import com.example.chatapp.data.repository.MessageRepository
import com.example.chatapp.data.Result
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChatViewModel : ViewModel() {
    private val TAG = "ChatViewModel"

    private val repository = MessageRepository()
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _error = MutableStateFlow<String>("")
    val error: StateFlow<String> = _error

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var currentChatId: String? = null
    private var currentGroupId: String? = null
    private var lastMessageTimestamp: Long? = null
    private val pageSize = 20

    private val storage = FirebaseStorage.getInstance()

    private var isVisible = false
    private var lastReadMessageId: String? = null

    // --------------------- User Email Management ---------------------

    /**
     * Holds the current user's email.
     */
    private val _currentUserEmail = mutableStateOf("")
    val currentUserEmail: String get() = _currentUserEmail.value

    fun setUserEmail(email: String) {
        _currentUserEmail.value = email
        Log.d(TAG, "User email updated: $email")
    }

    /**
     * Holds the recipient's email for individual chats.
     */
    private val _currentRecipientEmail = mutableStateOf("")
    val recipientEmail: String get() = _currentRecipientEmail.value

    fun setRecipientEmail(email: String) {
        _currentRecipientEmail.value = email
        Log.d(TAG, "Recipient email updated: $email")
    }

    // --------------------- Chat Initialization ---------------------

    /**
     * Initializes an individual chat between two users.
     */
    fun initChat(currentUserEmail: String, recipientEmail: String) {
        setUserEmail(currentUserEmail)
        setRecipientEmail(recipientEmail)
        currentChatId = createChatId(currentUserEmail, recipientEmail)
        Log.d(TAG, "Chat initialized with chatId: $currentChatId")
        loadMessages(initial = true)
    }

    /**
     * Initializes a group chat.
     */
    fun initGroupChat(currentUserEmail: String, groupId: String) {
        setUserEmail(currentUserEmail)
        currentGroupId = groupId
        Log.d(TAG, "Group chat initialized with groupId: $groupId")
        loadGroupMessages(initial = true)
    }

    /**
     * Creates a unique chatId by sorting and concatenating user emails.
     */
    private fun createChatId(currentUserEmail: String, recipientEmail: String): String {
        val sortedEmails = listOf(currentUserEmail, recipientEmail).sorted()
        val chatId = "${sortedEmails[0]}-${sortedEmails[1]}"
        Log.d(TAG, "Generated chatId: $chatId")
        return chatId
    }

    // --------------------- Message Loading ---------------------

    /**
     * Loads messages for an individual chat.
     */
    fun loadMessages(initial: Boolean = false) {
        if (_isLoading.value) {
            Log.d(TAG, "loadMessages called but already loading. Exiting.")
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = ""
                Log.d(TAG, "Loading messages. Initial: $initial")

                if (initial) {
                    lastMessageTimestamp = null
                    _messages.value = emptyList()
                    Log.d(TAG, "Initial load: Resetting messages and timestamp.")
                }

                currentChatId?.let { chatId ->
                    Log.d(TAG, "Fetching messages for chatId: $chatId")
                    repository.getMessagesForChat(
                        chatId = chatId,
                        pageSize = pageSize,
                        lastTimestamp = lastMessageTimestamp
                    ).collect { result ->
                        when (result) {
                            is Result.Success -> {
                                Log.d(TAG, "Successfully fetched ${result.data.size} messages.")
                                if (result.data.isNotEmpty()) {
                                    lastMessageTimestamp = result.data.last().timestamp
                                    _messages.value = if (initial) {
                                        result.data
                                    } else {
                                        _messages.value + result.data
                                    }
                                    Log.d(TAG, "Updated messages list. Total messages: ${_messages.value.size}")
                                    if (isVisible) {
                                        markUnreadMessagesAsRead()
                                    }
                                } else {
                                    Log.d(TAG, "No more messages to load.")
                                }
                            }
                            is Result.Error -> {
                                Log.e(TAG, "Error fetching messages: ${result.exception.message}")
                                _error.value = result.exception.message ?: "Unknown error occurred"
                            }
                        }
                    }
                } ?: run {
                    Log.e(TAG, "currentChatId is null. Cannot load messages.")
                    _error.value = "Chat ID is not set."
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in loadMessages: ${e.message}", e)
                _error.value = "Failed to load messages: ${e.message}"
            } finally {
                _isLoading.value = false
                Log.d(TAG, "Loading messages completed. isLoading set to false.")
            }
        }
    }

    /**
     * Loads messages for a group chat.
     */
    fun loadGroupMessages(initial: Boolean = false) {
        if (_isLoading.value) {
            Log.d(TAG, "loadGroupMessages called but already loading. Exiting.")
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = ""
                Log.d(TAG, "Loading group messages. Initial: $initial")

                if (initial) {
                    lastMessageTimestamp = null
                    _messages.value = emptyList()
                    Log.d(TAG, "Initial group load: Resetting messages and timestamp.")
                }

                currentGroupId?.let { groupId ->
                    Log.d(TAG, "Fetching group messages for groupId: $groupId")
                    repository.getGroupMessages(
                        groupId = groupId,
                        pageSize = pageSize,
                        lastTimestamp = lastMessageTimestamp
                    ).collect { result ->
                        when (result) {
                            is Result.Success -> {
                                Log.d(TAG, "Successfully fetched ${result.data.size} group messages.")
                                if (result.data.isNotEmpty()) {
                                    lastMessageTimestamp = result.data.last().timestamp
                                    _messages.value = if (initial) {
                                        result.data
                                    } else {
                                        _messages.value + result.data
                                    }
                                    Log.d(TAG, "Updated group messages list. Total messages: ${_messages.value.size}")
                                    if (isVisible) {
                                        markUnreadMessagesAsRead()
                                    }
                                } else {
                                    Log.d(TAG, "No more group messages to load.")
                                }
                            }
                            is Result.Error -> {
                                Log.e(TAG, "Error fetching group messages: ${result.exception.message}")
                                _error.value = result.exception.message ?: "Unknown error occurred"
                            }
                        }
                    }
                } ?: run {
                    Log.e(TAG, "currentGroupId is null. Cannot load group messages.")
                    _error.value = "Group ID is not set."
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in loadGroupMessages: ${e.message}", e)
                _error.value = "Failed to load group messages: ${e.message}"
            } finally {
                _isLoading.value = false
                Log.d(TAG, "Loading group messages completed. isLoading set to false.")
            }
        }
    }

    /**
     * Loads more messages for pagination in individual chats.
     */
    fun loadMoreMessages() {
        Log.d(TAG, "loadMoreMessages called.")
        loadMessages(initial = false)
    }

    /**
     * Loads more messages for pagination in group chats.
     */
    fun loadMoreGroupMessages() {
        Log.d(TAG, "loadMoreGroupMessages called.")
        loadGroupMessages(initial = false)
    }

    // --------------------- Message Sending ---------------------

    /**
     * Sends a text message in an individual chat.
     */
    fun sendMessage(text: String, senderEmail: String, receiverEmail: String) {
        Log.d(TAG, "sendMessage called with text: \"$text\", sender: $senderEmail, receiver: $receiverEmail")
        viewModelScope.launch {
            try {
                _error.value = ""
                currentChatId?.let { chatId ->
                    val message = Message(
                        text = text,
                        senderEmail = senderEmail,
                        recipientEmail = receiverEmail,
                        timestamp = System.currentTimeMillis(),
                        chatId = chatId,
                        groupMessage = false
                    )
                    Log.d(TAG, "Prepared message: $message")
                    repository.sendMessage(message)
                    Log.d(TAG, "Message sent successfully.")
                    // No manual addition to _messages; snapshot listener handles it
                } ?: run {
                    Log.e(TAG, "currentChatId is null. Cannot send message.")
                    _error.value = "Chat ID is not set."
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in sendMessage: ${e.message}", e)
                _error.value = "Failed to send message: ${e.message}"
            }
        }
    }

    /**
     * Sends an image message in an individual chat.
     */
    fun sendImage(imageUri: Uri, senderEmail: String) {
        Log.d(TAG, "sendImage called with URI: $imageUri, sender: $senderEmail")
        viewModelScope.launch {
            try {
                _error.value = ""
                currentChatId?.let { chatId ->
                    // Upload image to Firebase Storage
                    val imageRef = storage.reference.child(
                        "chat_images/${senderEmail}_${System.currentTimeMillis()}"
                    )
                    Log.d(TAG, "Uploading image to: ${imageRef.path}")
                    imageRef.putFile(imageUri).await()
                    val imageUrl = imageRef.downloadUrl.await().toString()
                    Log.d(TAG, "Image uploaded. URL: $imageUrl")

                    // Create message with image
                    val message = Message(
                        text = "📷 Image",
                        senderEmail = senderEmail,
                        recipientEmail = recipientEmail, // Ensure recipientEmail is correctly set
                        timestamp = System.currentTimeMillis(),
                        chatId = chatId,
                        imageUrl = imageUrl,
                        groupMessage = false
                    )
                    Log.d(TAG, "Prepared image message: $message")
                    repository.sendMessage(message)
                    Log.d(TAG, "Image message sent successfully.")
                    // No manual addition to _messages; snapshot listener handles it
                } ?: run {
                    Log.e(TAG, "currentChatId is null. Cannot send image.")
                    _error.value = "Chat ID is not set."
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in sendImage: ${e.message}", e)
                _error.value = "Failed to send image: ${e.message}"
            }
        }
    }

    // --------------------- Message Editing and Deletion ---------------------

    /**
     * Deletes a message.
     */
    fun deleteMessage(messageId: String) {
        Log.d(TAG, "deleteMessage called with messageId: $messageId")
        viewModelScope.launch {
            try {
                repository.deleteMessage(messageId)
                Log.d(TAG, "Message deleted successfully.")
            } catch (e: Exception) {
                Log.e(TAG, "Exception in deleteMessage: ${e.message}", e)
                _error.value = "Failed to delete message: ${e.message}"
            }
        }
    }

    /**
     * Edits a message's text.
     */
    fun editMessage(messageId: String, newText: String) {
        Log.d(TAG, "editMessage called with messageId: $messageId, newText: \"$newText\"")
        viewModelScope.launch {
            try {
                repository.editMessage(messageId, newText)
                Log.d(TAG, "Message edited successfully.")
            } catch (e: Exception) {
                Log.e(TAG, "Exception in editMessage: ${e.message}", e)
                _error.value = "Failed to edit message: ${e.message}"
            }
        }
    }

    // --------------------- Visibility and Read Receipts ---------------------

    /**
     * Sets the visibility of the chat (e.g., when the chat screen is visible).
     * If visible, marks unread messages as read.
     */
    fun setVisibility(visible: Boolean) {
        Log.d(TAG, "setVisibility called with visible: $visible")
        isVisible = visible
        if (visible) {
            markUnreadMessagesAsRead()
        }
    }

    /**
     * Marks unread messages as read by the current user.
     */
    private fun markUnreadMessagesAsRead() {
        Log.d(TAG, "markUnreadMessagesAsRead called.")
        viewModelScope.launch {
            try {
                val unreadMessages = _messages.value.filter { message ->
                    message.senderEmail != currentUserEmail && !message.readBy.contains(currentUserEmail)
                }
                Log.d(TAG, "Found ${unreadMessages.size} unread messages.")
                if (unreadMessages.isNotEmpty()) {
                    repository.markMessagesAsRead(unreadMessages, currentUserEmail)
                    Log.d(TAG, "Marked unread messages as read.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in markUnreadMessagesAsRead: ${e.message}", e)
                _error.value = "Failed to mark messages as read: ${e.message}"
            }
        }
    }
}