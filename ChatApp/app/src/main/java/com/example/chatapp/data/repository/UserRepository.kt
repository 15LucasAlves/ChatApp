package com.example.chatapp.data.repository

import android.util.Log
import com.example.chatapp.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.SetOptions

/**
 * The UserRepository class is responsible for managing user-related data operations,
 * such as retrieving all users, getting a specific user, creating a new user,
 * updating user details, and updating the user's FCM token.
 */
class UserRepository {
    // Get an instance of the Firebase Firestore database
    private val firestore = FirebaseFirestore.getInstance()
    // Get a reference to the "users" collection in the Firestore database
    private val usersCollection = firestore.collection("users")

    /**
     * Retrieves a flow of all users from the Firestore database.
     * @return A flow of a list of [User] objects.
     */
    fun getAllUsers(): Flow<List<User>> = callbackFlow {
        // Add a snapshot listener to the "users" collection
        val subscription = usersCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Handle any errors that occur during the snapshot listener
                    return@addSnapshotListener
                }

                // Convert the Firestore documents to a list of User objects
                val users = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(User::class.java)
                } ?: emptyList()

                // Send the list of users through the flow
                trySend(users)
            }

        // Clean up the snapshot listener when the flow is closed
        awaitClose { subscription.remove() }
    }

    /**
     * Retrieves a specific user from the Firestore database based on their email.
     * @param email The email of the user to retrieve.
     * @return The [User] object if found, or null if not found.
     */
    suspend fun getUser(email: String): User? {
        return try {
            // Get the Firestore document for the user with the given email
            val document = usersCollection.document(email).get().await()
            // Convert the document to a User object
            document.toObject(User::class.java)
        } catch (e: Exception) {
            // Return null if an exception occurs
            null
        }
    }

    /**
     * Creates a new user in the Firestore database.
     * @param user The [User] object to be created.
     * @throws Exception if the user creation fails.
     */
    suspend fun createUser(user: User) {
        Log.e("Create user", "Inside create user function")
        try {
            // Create a new document in the "users" collection with the user's email as the document ID
            usersCollection.document(user.email)
                .set(user)
                .await()
        } catch (e: Exception) {
            // Throw an exception if the user creation fails
            throw Exception("Failed to create user: ${e.message}")
        }
    }

    /**
     * Updates the details of an existing user in the Firestore database.
     * @param email The email of the user to update.
     * @param username The new username for the user.
     * @param photoUrl The new profile photo URL for the user (optional).
     * @throws Exception if the user update fails.
     */
    suspend fun updateUserDetails(email: String, username: String, photoUrl: String?) {
        try {
            // Create a map of the updates to be made
            val updates = mutableMapOf<String, Any>()
            updates["username"] = username
            if (photoUrl != null) {
                updates["photoUrl"] = photoUrl
            }

            // Update the user document in the "users" collection with the new details
            usersCollection.document(email)
                .update(updates)
                .await()
        } catch (e: Exception) {
            // Throw an exception if the user update fails
            throw Exception("Failed to update user details: ${e.message}")
        }
    }

    /**
     * Updates the FCM token for a user in the Firestore database.
     * @param email The email of the user to update.
     * @param token The new FCM token for the user.
     */
    suspend fun updateFcmToken(email: String, token: String) {
        try {
            // Get a reference to the user document in the "users" collection
            val docRef = usersCollection.document(email)
            // Update the "fcmToken" field in the user document with the new token
            docRef.set(
                mapOf("fcmToken" to token),
                SetOptions.merge() // Ensures doc is created/merged if nonexistent
            ).await()
        } catch (e: Exception) {
            // Log an error message if the FCM token update fails
            Log.e("UserRepository", "Failed to update FCM token: ${e.message}")
        }
    }
}