package com.example.chatapp.data.model

data class Group(
    val id: String = "",
    val name: String = "",
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val members: List<String> = emptyList(),
    val photoUrl: String? = null
) 