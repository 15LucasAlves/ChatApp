package com.example.chatapp.data.model

import com.google.firebase.Timestamp

/**
 * Data class representing a user in the chat application.
 *
 * @property email The email address of the user.
 * @property username The username of the user.
 * @property photoUrl The URL of the user's profile photo (nullable).
 * @property fcmToken The Firebase Cloud Messaging token of the user (nullable).
 * @property createdAt The timestamp when the user account was created.
 */
data class User(
    val email: String = "",
    val username: String = "",
    val photoUrl: String? = null,
    val fcmToken: String? = null,
    val createdAt: Timestamp = Timestamp.now() // Added field with default value
)