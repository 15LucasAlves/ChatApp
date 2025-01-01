package com.example.chatapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RegisterViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow("")
    val error: StateFlow<String> = _error

    fun register(email: String, password: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = ""

                android.util.Log.e("Register View Model", "Inside register user function")

                auth.createUserWithEmailAndPassword(email, password).await()
                onComplete(true)

                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

                val newUserData = mapOf(
                    "email" to email,
                    "photoUrl" to "https://firebasestorage.googleapis.com/v0/b/chatapp-e94d4.firebasestorage.app/o/profile_images%2Fdefault_user_profile_image.png?alt=media&token=f19d8620-dacc-438b-aa53-b47199d89640",
                    "username" to ""      // or "No username yet"
                )

                val set = db.collection("users")
                    .document(email)
                    .set(newUserData)


            } catch (e: Exception) {
                _error.value = e.message ?: "Registration failed"
                onComplete(false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setError(message: String) {
        _error.value = message
    }
} 