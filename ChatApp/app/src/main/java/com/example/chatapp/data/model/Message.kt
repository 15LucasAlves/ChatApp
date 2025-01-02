package com.example.chatapp.data.model

data class Message(
    val id: String = "",
    val text: String = "",
    val senderEmail: String = "",
    val recipientEmail: String = "",
    val timestamp: Long = 0L,
    val chatId: String = "",
    val imageUrl: String? = null,
    val edited: Boolean = false, // Renamed from isEdited to match Firestore field
    val editedAt: Long? = null,
    val readBy: List<String> = emptyList(),
    val groupMessage: Boolean = false, // Renamed from isGroupMessage to match Firestore field
    val groupId: String? = null
)