package com.example.chatapp.ui.chat

import android.net.Uri
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
    private val repository = MessageRepository()
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _error = MutableStateFlow<String>("")
    val error: StateFlow<String> = _error

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var currentChatId: String? = null
    private var lastMessageTimestamp: Long? = null
    private val pageSize = 20

    private val storage = FirebaseStorage.getInstance()

    private var isVisible = false
    private var lastReadMessageId: String? = null

    fun initChat(currentUserEmail: String, recipientEmail: String) {
        currentChatId = repository.createChatId(currentUserEmail, recipientEmail)
        loadMessages(initial = true)
    }

    fun loadMessages(initial: Boolean = false) {
        if (_isLoading.value) return

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
                                    if (isVisible) {
                                        markUnreadMessagesAsRead()
                                    }
                                }
                            }
                            is Result.Error -> {
                                _error.value = result.exception.message ?: "Unknown error occurred"
                            }
                        }
                    }
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

    fun sendMessage(text: String, senderEmail: String) {
        viewModelScope.launch {
            try {
                _error.value = ""
                currentChatId?.let { chatId ->
                    val message = Message(
                        text = text,
                        senderEmail = senderEmail,
                        timestamp = System.currentTimeMillis(),
                        chatId = chatId
                    )
                    repository.sendMessage(message)
                }
            } catch (e: Exception) {
                _error.value = "Failed to send message: ${e.message}"
            }
        }
    }

    fun markMessageAsRead(messageId: String) {
        viewModelScope.launch {
            try {
                repository.markMessageAsRead(messageId)
            } catch (e: Exception) {
                // Silently fail for read receipts
            }
        }
    }

    fun sendImage(imageUri: Uri, senderEmail: String) {
        viewModelScope.launch {
            try {
                _error.value = ""
                currentChatId?.let { chatId ->
                    // Upload image to Firebase Storage
                    val imageRef = storage.reference.child(
                        "chat_images/${senderEmail}_${System.currentTimeMillis()}"
                    )
                    imageRef.putFile(imageUri).await()
                    val imageUrl = imageRef.downloadUrl.await().toString()

                    // Create message with image
                    val message = Message(
                        text = "📷 Image",
                        senderEmail = senderEmail,
                        timestamp = System.currentTimeMillis(),
                        chatId = chatId,
                        imageUrl = imageUrl
                    )
                    repository.sendMessage(message)
                }
            } catch (e: Exception) {
                _error.value = "Failed to send image: ${e.message}"
            }
        }
    }

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
        viewModelScope.launch {
            try {
                repository.editMessage(messageId, newText)
            } catch (e: Exception) {
                _error.value = "Failed to edit message: ${e.message}"
            }
        }
    }

    fun setVisibility(visible: Boolean) {
        isVisible = visible
        if (visible) {
            markUnreadMessagesAsRead()
        }
    }

    private fun markUnreadMessagesAsRead() {
        viewModelScope.launch {
            try {
                val unreadMessages = _messages.value.filter { message ->
                    message.senderEmail != currentUserEmail && !message.readBy.contains(currentUserEmail)
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