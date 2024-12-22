package com.example.chatapp.data

data class User(
    val email: String = "",
    val password: String = "",
    val createdAt: Long = System.currentTimeMillis()
)