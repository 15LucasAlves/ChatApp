package com.example.chatapp.data.model

data class User(
    val email: String = "",
    val password: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val username: String? = null,
    val photoUrl: String? = null
) 