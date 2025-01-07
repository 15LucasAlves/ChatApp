package com.example.chatapp.ui.chat

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatapp.data.Result
import com.example.chatapp.data.model.Message
import com.example.chatapp.data.repository.MessageRepository
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * ViewModel for the chat screen, responsible for managing the state and logic of the chat.
 */
class ChatViewModel : ViewModel() {
    private val TAG = "ChatViewModel"

    private val repository = MessageRepository()
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _error = MutableStateFlow("")
    val error: StateFlow<String> = _error

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // --------------------- Prevent multiple sends ---------------------
    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending

    private var currentChatId: String? = null
    private var currentGroupId: String? = null
    private var lastMessageTimestamp: Long? = null
    private val pageSize = 20

    private val storage = FirebaseStorage.getInstance()

    private var isVisible = false

    // --------------------- Image Selection ---------------------
    private val _selectedImageUris = MutableStateFlow<List<Uri>>(emptyList())
    val selectedImageUris: StateFlow<List<Uri>> = _selectedImageUris

    /**
     * Adds a new image URI to the list of selected image URIs.
     * @param uri The URI of the image to be added.
     */
    fun addImageUri(uri: Uri) {
        _selectedImageUris.value = _selectedImageUris.value + uri
    }

    /**
     * Removes an image URI from the list of selected image URIs.
     * @param uri The URI of the image to be removed.
     */
    fun removeImageUri(uri: Uri) {
        _selectedImageUris.value = _selectedImageUris.value - uri
    }

    /**
     * Clears the list of selected image URIs.
     */
    fun clearImageUris() {
        _selectedImageUris.value = emptyList()
    }

    // --------------------- User Email Management ---------------------
    private val _currentUserEmail = mutableStateOf("")
    val currentUserEmail: String get() = _currentUserEmail.value

    /**
     * Sets the current user's email address.
     * @param email The email address of the current user.
     */
    fun setUserEmail(email: String) {
        _currentUserEmail.value = email
        Log.d(TAG, "User email updated: $email")
    }

    private val _currentRecipientEmail = mutableStateOf("")
    val recipientEmail: String get() = _currentRecipientEmail.value

    /**
     * Sets the recipient's email address.
     * @param email The email address of the recipient.
     */
    fun setRecipientEmail(email: String) {
        _currentRecipientEmail.value = email
        Log.d(TAG, "Recipient email updated: $email")
    }

    // --------------------- Chat Initialization ---------------------
    /**
     * Initializes the chat by setting the current user's email, the recipient's email, and the current chat ID.
     * Then, it loads the messages for the current chat.
     * @param currentUserEmail The email address of the current user.
     * @param recipientEmail The email address of the recipient.
     */
    fun initChat(currentUserEmail: String, recipientEmail: String) {
        setUserEmail(currentUserEmail)
        setRecipientEmail(recipientEmail)
        currentChatId = createChatId(currentUserEmail, recipientEmail)
        loadMessages(initial = true)
    }

    /**
     * Initializes the group chat by setting the current user's email and the current group ID.
     * Then, it loads the messages for the current group.
     * @param currentUserEmail The email address of the current user.
     * @param groupId The ID of the group.
     */
    fun initGroupChat(currentUserEmail: String, groupId: String) {
        setUserEmail(currentUserEmail)
        currentGroupId = groupId
        loadGroupMessages(initial = true)
    }

    /**
     * Clears the list of messages.
     */
    fun clearMessageList() {
        _messages.value = emptyList()
    }

    /**
     * Creates a unique chat ID based on the current user's email and the recipient's email.
     * @param currentUserEmail The email address of the current user.
     * @param recipientEmail The email address of the recipient.
     * @return The unique chat ID.
     */
    private fun createChatId(currentUserEmail: String, recipientEmail: String): String {
        val sortedEmails = listOf(currentUserEmail, recipientEmail).sorted()
        return "${sortedEmails[0]}-${sortedEmails[1]}"
    }

    // --------------------- Message Loading ---------------------
    /**
     * Loads the messages for the current chat.
     * @param initial If true, it will load the initial set of messages. Otherwise, it will load more messages.
     */
    fun loadMessages(initial: Boolean = false) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = ""
                if (initial) {
                    lastMessageTimestamp = null
                    _messages.value = emptyList()
                }
                currentChatId?.let { chatId ->
                    repository.getMessagesForChat(
                        chatId = chatId,
                        pageSize = pageSize,
                        lastTimestamp = lastMessageTimestamp
                    ).collect { result ->
                        when (result) {
                            is Result.Success -> {
                                if (result.data.isNotEmpty()) {
                                    lastMessageTimestamp = result.data.last().timestamp
                                    _messages.value = if (initial) {
                                        result.data
                                    } else {
                                        _messages.value + result.data
                                    }
                                    if (isVisible) markUnreadMessagesAsRead()
                                }
                            }
                            is Result.Error -> {
                                _error.value = result.exception.message ?: "Unknown error"
                            }
                            is Result.Loading -> {
                                // No-op
                            }
                        }
                    }
                } ?: run {
                    _error.value = "Chat ID is not set."
                }
            } catch (e: Exception) {
                _error.value = "Failed to load messages: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Loads more messages for the current chat.
     */
    fun loadMoreMessages() {
        loadMessages(initial = false)
    }

    /**
     * Loads the messages for the current group.
     * @param initial If true, it will load the initial set of messages. Otherwise, it will load more messages.
     */
    fun loadGroupMessages(initial: Boolean = false) {
        if (_isLoading.value) return
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = ""
                if (initial) {
                    lastMessageTimestamp = null
                    _messages.value = emptyList()
                }
                currentGroupId?.let { groupId ->
                    repository.getGroupMessages(
                        groupId = groupId,
                        pageSize = pageSize,
                        lastTimestamp = lastMessageTimestamp
                    ).collect { result ->
                        when (result) {
                            is Result.Success -> {
                                if (result.data.isNotEmpty()) {
                                    lastMessageTimestamp = result.data.last().timestamp
                                    _messages.value = if (initial) {
                                        result.data
                                    } else {
                                        _messages.value + result.data
                                    }
                                    if (isVisible) markUnreadMessagesAsRead()
                                }
                            }
                            is Result.Error -> {
                                _error.value = result.exception.message ?: "Unknown error"
                            }
                            is Result.Loading -> {
                                // No-op
                            }
                        }
                    }
                } ?: run {
                    _error.value = "Group ID is not set."
                }
            } catch (e: Exception) {
                _error.value = "Failed to load group messages: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Loads more messages for the current group.
     */
    fun loadMoreGroupMessages() {
        loadGroupMessages(initial = false)
    }

    // --------------------- Message Sending ---------------------
    /**
     * Sends a message to the current chat.
     * @param text The text of the message.
     * @param senderEmail The email address of the sender.
     * @param receiverEmail The email address of the receiver.
     */
    fun sendMessage(text: String, senderEmail: String, receiverEmail: String) {
        // If we are already sending, do nothing
        if (_isSending.value) return

        viewModelScope.launch {
            _isSending.value = true
            try {
                _error.value = ""
                currentChatId?.let { chatId ->
                    // 1) Upload any selected images
                    val imageUris = _selectedImageUris.value
                    val imageUrls = if (imageUris.isNotEmpty()) {
                        uploadImages(imageUris, senderEmail)
                    } else emptyList()

                    // 2) Build the message
                    val message = Message(
                        text = text,
                        senderEmail = senderEmail,
                        recipientEmail = receiverEmail,
                        timestamp = System.currentTimeMillis(),
                        chatId = chatId,
                        groupMessage = false,
                        imageUrls = if (imageUrls.isEmpty()) null else imageUrls
                    )

                    // 3) Send
                    repository.sendMessage(message)

                    // 4) Clear the local list of selected URIs after sending
                    clearImageUris()
                } ?: run {
                    _error.value = "Chat ID is not set."
                }
            } catch (e: Exception) {
                _error.value = "Failed to send message: ${e.message}"
            } finally {
                // Re-enable the send button after the process finishes
                _isSending.value = false
            }
        }
    }

    /**
     * Uploads the selected images to Firebase Storage and returns the download URLs.
     * @param uris The list of image URIs to be uploaded.
     * @param senderEmail The email address of the sender.
     * @return The list of download URLs for the uploaded images.
     */
    private suspend fun uploadImages(uris: List<Uri>, senderEmail: String): List<String> {
        val imageUrls = mutableListOf<String>()
        for (uri in uris) {
            val imageRef = storage.reference.child(
                "chat_images/${senderEmail}_${System.currentTimeMillis()}_${uri.lastPathSegment}"
            )
            imageRef.putFile(uri).await()
            val downloadUrl = imageRef.downloadUrl.await().toString()
            imageUrls.add(downloadUrl)
        }
        return imageUrls
    }

    // --------------------- Message Editing & Deletion ---------------------
    /**
     * Deletes a message from the chat.
     * @param messageId The ID of the message to be deleted.
     */
    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            try {
                repository.deleteMessage(messageId)
            } catch (e: Exception) {
                _error.value = "Failed to delete message: ${e.message}"
            }
        }
    }

    /**
     * Edits the text of a message in the chat.
     * @param messageId The ID of the message to be edited.
     * @param newText The new text for the message.
     */
    fun editMessage(messageId: String, newText: String) {
        // No need to lock send for editing, but we do need to ensure it overwrites the text
        viewModelScope.launch {
            try {
                repository.editMessage(messageId, newText)
            } catch (e: Exception) {
                _error.value = "Failed to edit message: ${e.message}"
            }
        }
    }

    // --------------------- Visibility & Read Receipts ---------------------
    /**
     * Sets the visibility of the chat screen.
     * If the screen becomes visible, it will mark all unread messages as read.
     * @param visible True if the chat screen is visible, false otherwise.
     */
    fun setVisibility(visible: Boolean) {
        isVisible = visible
        if (visible) markUnreadMessagesAsRead()
    }

    /**
     * Marks all unread messages as read for the current user.
     */
    private fun markUnreadMessagesAsRead() {
        viewModelScope.launch {
            try {
                val unreadMessages = _messages.value.filter { msg ->
                    msg.senderEmail != currentUserEmail && !msg.readBy.contains(currentUserEmail)
                }
                if (unreadMessages.isNotEmpty()) {
                    repository.markMessagesAsRead(unreadMessages, currentUserEmail)
                }
            } catch (e: Exception) {
                _error.value = "Failed to mark messages as read: ${e.message}"
            }
        }
    }
}