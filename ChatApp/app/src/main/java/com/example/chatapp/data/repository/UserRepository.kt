package com.example.chatapp.data.repository

import com.example.chatapp.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    fun getAllUsers(): Flow<List<User>> = callbackFlow {
        val subscription = usersCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Handle error
                    return@addSnapshotListener
                }

                val users = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(User::class.java)
                } ?: emptyList()

                trySend(users)
            }

        awaitClose { subscription.remove() }
    }

    suspend fun getUser(email: String): User? {
        return try {
            val document = usersCollection.document(email).get().await()
            document.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun createUser(user: User) {
        try {
            usersCollection.document(user.email)
                .set(user)
                .await()
        } catch (e: Exception) {
            throw Exception("Failed to create user: ${e.message}")
        }
    }

    suspend fun updateUserDetails(email: String, username: String, photoUrl: String?) {
        try {
            val updates = mutableMapOf<String, Any>()
            updates["username"] = username
            if (photoUrl != null) {
                updates["photoUrl"] = photoUrl
            }
            
            usersCollection.document(email)
                .update(updates)
                .await()
        } catch (e: Exception) {
            throw Exception("Failed to update user details: ${e.message}")
        }
    }
}