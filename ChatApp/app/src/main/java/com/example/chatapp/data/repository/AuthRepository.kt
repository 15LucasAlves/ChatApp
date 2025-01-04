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

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val userRepository = UserRepository()


    suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun register(email: String, password: String): Result<Unit> {
        return try {
            auth.createUserWithEmailAndPassword(email, password).await()

            // Create user document in Firestore
            val user = User(
                email = email,
                createdAt = Timestamp.now()
            )
            userRepository.createUser(user)

            try {
                db.collection("users")
                    .document(email)
                    .set(user)
                    .await()
            } catch (e: Exception) {
                android.util.Log.e("FirestoreError", "Could not create user doc", e)
                // or update stateFlow with error
            }

            val userDocument = db.collection("users").document(auth.currentUser?.uid ?: "")
            userDocument.set(user).await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    fun logout() {
        auth.signOut()
    }

    fun getCurrentUser() = auth.currentUser
}