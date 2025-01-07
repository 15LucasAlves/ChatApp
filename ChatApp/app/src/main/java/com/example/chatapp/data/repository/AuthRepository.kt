package com.example.chatapp.data.repository

import android.util.Log
import com.example.chatapp.data.Result
import com.example.chatapp.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.Timestamp

/**
 * AuthRepository is a class that handles authentication-related operations in the chat app.
 * It uses Firebase Authentication and Firestore to manage user authentication and user data.
 */
class AuthRepository {
    // Instances of Firebase Authentication and Firestore
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val userRepository = UserRepository()

    /**
     * Logs in a user with the provided email and password.
     * @param email The email of the user.
     * @param password The password of the user.
     * @return A Result object containing either a success or an error.
     */
    suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            // Sign in the user with the provided email and password
            auth.signInWithEmailAndPassword(email, password).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            // Return an error result if the login fails
            Result.Error(e)
        }
    }

    /**
     * Registers a new user with the provided email and password.
     * @param email The email of the new user.
     * @param password The password of the new user.
     * @return A Result object containing either a success or an error.
     */
    suspend fun register(email: String, password: String): Result<Unit> {
        return try {
            // Create a new user with the provided email and password
            auth.createUserWithEmailAndPassword(email, password).await()

            // Create a new user document in Firestore
            val user = User(
                email = email,
                createdAt = Timestamp.now()
            )
            userRepository.createUser(user)

            try {
                // Set the user document in Firestore
                db.collection("users")
                    .document(email)
                    .set(user)
                    .await()
            } catch (e: Exception) {
                // Log the error if the user document could not be created
                android.util.Log.e("FirestoreError", "Could not create user doc", e)
            }

            // Set the user document in Firestore using the user's UID
            val userDocument = db.collection("users").document(auth.currentUser?.uid ?: "")
            userDocument.set(user).await()

            Result.Success(Unit)
        } catch (e: Exception) {
            // Return an error result if the registration fails
            Result.Error(e)
        }
    }

    /**
     * Logs out the current user.
     */
    fun logout() {
        // Sign out the current user
        auth.signOut()
    }

    /**
     * Returns the current user.
     * @return The current user, or null if no user is signed in.
     */
    fun getCurrentUser() = auth.currentUser
}