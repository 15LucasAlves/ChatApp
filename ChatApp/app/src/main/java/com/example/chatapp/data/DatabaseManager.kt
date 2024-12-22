package com.example.chatapp.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

object DatabaseManager {
    private val auth: FirebaseAuth = Firebase.auth

    suspend fun registerUser(email: String, password: String): Boolean {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user != null
        } catch (e: Exception) {
            Log.e("DatabaseManager", "Error registering user", e)
            false
        }
    }

    suspend fun loginUser(email: String, password: String): Boolean {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user != null
        } catch (e: Exception) {
            Log.e("DatabaseManager", "Error logging in user", e)
            false
        }
    }

    fun getCurrentUser() = auth.currentUser
} 