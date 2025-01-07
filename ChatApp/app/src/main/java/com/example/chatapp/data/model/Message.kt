package com.example.chatapp.data.model

/**
 * Data class representing a message in the application.
 *
 * @property id Unique identifier for the message.
 * @property text The content of the message.
 * @property senderEmail The email address of the user who sent the message.
 * @property recipientEmail The email address of the user who received the message.
 * @property timestamp The timestamp when the message was sent.
 * @property chatId The unique identifier of the chat the message belongs to.
 * @property imageUrls A list of URLs for any images attached to the message.
 * @property edited Indicates whether the message has been edited.
 * @property editedAt The timestamp when the message was last edited.
 * @property readBy A list of email addresses of users who have read the message.
 * @property groupMessage Indicates whether the message is part of a group chat.
 * @property groupId The unique identifier of the group chat the message belongs to.
 */
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