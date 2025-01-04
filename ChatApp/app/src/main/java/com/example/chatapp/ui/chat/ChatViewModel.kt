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

    fun addImageUri(uri: Uri) {
        _selectedImageUris.value = _selectedImageUris.value + uri
    }

    fun removeImageUri(uri: Uri) {
        _selectedImageUris.value = _selectedImageUris.value - uri
    }

    fun clearImageUris() {
        _selectedImageUris.value = emptyList()
    }

    // --------------------- User Email Management ---------------------
    private val _currentUserEmail = mutableStateOf("")
    val currentUserEmail: String get() = _currentUserEmail.value

    fun setUserEmail(email: String) {
        _currentUserEmail.value = email
        Log.d(TAG, "User email updated: $email")
    }

    private val _currentRecipientEmail = mutableStateOf("")
    val recipientEmail: String get() = _currentRecipientEmail.value

    fun setRecipientEmail(email: String) {
        _currentRecipientEmail.value = email
        Log.d(TAG, "Recipient email updated: $email")
    }

    // --------------------- Chat Initialization ---------------------
    fun initChat(currentUserEmail: String, recipientEmail: String) {
        setUserEmail(currentUserEmail)
        setRecipientEmail(recipientEmail)
        currentChatId = createChatId(currentUserEmail, recipientEmail)
        loadMessages(initial = true)
    }

    fun initGroupChat(currentUserEmail: String, groupId: String) {
        setUserEmail(currentUserEmail)
        currentGroupId = groupId
        loadGroupMessages(initial = true)
    }

    fun clearMessageList() {
        _messages.value = emptyList()
    }

    private fun createChatId(currentUserEmail: String, recipientEmail: String): String {
        val sortedEmails = listOf(currentUserEmail, recipientEmail).sorted()
        return "${sortedEmails[0]}-${sortedEmails[1]}"
    }

    // --------------------- Message Loading ---------------------
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

    fun loadMoreMessages() {
        loadMessages(initial = false)
    }

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

    fun loadMoreGroupMessages() {
        loadGroupMessages(initial = false)
    }

    // --------------------- Message Sending ---------------------
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
    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            try {
                repository.deleteMessage(messageId)
            } catch (e: Exception) {
                _error.value = "Failed to delete message: ${e.message}"
            }
        }
    }

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
    fun setVisibility(visible: Boolean) {
        isVisible = visible
        if (visible) markUnreadMessagesAsRead()
    }

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