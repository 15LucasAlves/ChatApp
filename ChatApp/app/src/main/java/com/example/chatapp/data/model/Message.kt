package com.example.chatapp.data.model

data class Message(
    val id: String = "",
    val text: String = "",
    val senderEmail: String = "",
    val recipientEmail: String = "",
    val timestamp: Long = 0L,
    val chatId: String = "",
    val imageUrls: List<String>? = null,  // <-- Replaces single String? with a list
    val edited: Boolean = false,
    val editedAt: Long? = null,
    val readBy: List<String> = emptyList(),
    val groupMessage: Boolean = false,
    val groupId: String? = null
)