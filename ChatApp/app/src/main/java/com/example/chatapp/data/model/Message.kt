package com.example.chatapp.data.model

data class Message(
    val id: String = "",
    val text: String = "",
    val senderEmail: String = "",
    val recipientEmail: String = "",
    val timestamp: Long = 0,
    val chatId: String = "",
    val imageUrl: String? = null,
    val isEdited: Boolean = false,
    val editedAt: Long? = null,
    val readBy: List<String> = emptyList(),
    val isGroupMessage: Boolean = false,
    val groupId: String? = null
)
