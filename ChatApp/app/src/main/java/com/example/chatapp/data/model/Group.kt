package com.example.chatapp.data.model

/**
 * A data class representing a group in the application.
 *
 * @property id The unique identifier of the group.
 * @property name The name of the group.
 * @property createdBy The user ID of the person who created the group.
 * @property createdAt The timestamp when the group was created.
 * @property members A list of user IDs representing the members of the group.
 * @property photoUrl An optional URL for the group's profile picture.
 */
data class Group(
    val id: String = "",
    val name: String = "",
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val members: List<String> = emptyList(),
    val photoUrl: String? = null
)