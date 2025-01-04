package com.example.chatapp.data.model

import com.google.firebase.Timestamp // Ensure you have the correct import

data class User(
    val email: String = "",
    val username: String = "",
    val photoUrl: String? = null,
    val fcmToken: String? = null,
    val createdAt: Timestamp = Timestamp.now() // Added field with default value
)