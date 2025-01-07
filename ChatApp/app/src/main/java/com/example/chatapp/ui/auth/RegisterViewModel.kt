package com.example.chatapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * RegisterViewModel is a ViewModel class that handles the registration process for the chat app.
 * It uses Firebase Authentication and Firestore to create a new user account and store user data.
 */
class RegisterViewModel : ViewModel() {
    // Get an instance of the Firebase Authentication service
    private val auth = FirebaseAuth.getInstance()

    // MutableStateFlow to track the loading state of the registration process
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // MutableStateFlow to track any errors that occur during the registration process
    private val _error = MutableStateFlow("")
    val error: StateFlow<String> = _error

    /**
     * Registers a new user with the provided email and password.
     * @param email The email address of the new user.
     * @param password The password for the new user.
     * @param onComplete A callback function that is called when the registration is complete, with a boolean indicating success or failure.
     */
    fun register(email: String, password: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                // Set the loading state to true
                _isLoading.value = true
                _error.value = ""

                // Log a message to indicate that the register user function has been called
                android.util.Log.e("Register View Model", "Inside register user function")

                // Create a new user account using the provided email and password
                auth.createUserWithEmailAndPassword(email, password).await()
                onComplete(true)

                // Get an instance of the Firestore database
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

                // Create a new user data map with the provided email and a default profile image URL
                val newUserData = mapOf(
                    "email" to email,
                    "photoUrl" to "https://firebasestorage.googleapis.com/v0/b/chatapp-e94d4.firebasestorage.app/o/profile_images%2Fdefault_user.png?alt=media&token=65586b82-8a16-4000-94ed-ff95c4026c80",
                    "username" to "",      // or "No username yet"
                    //"password" to password
                )

                // Store the new user data in the Firestore "users" collection
                val set = db.collection("users")
                    .document(email)
                    .set(newUserData)

            } catch (e: Exception) {
                // If an exception occurs, set the error message and call the onComplete callback with false
                _error.value = e.message ?: "Registration failed"
                onComplete(false)
            } finally {
                // Set the loading state to false
                _isLoading.value = false
            }
        }
    }

    /**
     * Sets the error message in the _error MutableStateFlow.
     * @param message The error message to be set.
     */
    fun setError(message: String) {
        _error.value = message
    }
}