package com.example.chatapp.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatapp.data.Result
import com.example.chatapp.data.repository.AuthRepository
import com.example.chatapp.data.repository.UserRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the login screen in the chat app.
 * It handles the login process and updates the user's FCM token in Firestore.
 */
class LoginViewModel : ViewModel() {
    // Repositories for authentication and user data
    private val repository = AuthRepository()
    private val userRepository = UserRepository()

    // Firebase authentication instance
    val auth = Firebase.auth

    // MutableStateFlow to track the loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // MutableStateFlow to track any errors that occur during the login process
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    /**
     * Logs in the user with the provided email and password.
     * @param email The email of the user.
     * @param password The password of the user.
     * @param onSuccess A callback function to be executed on successful login.
     * @param onFailure A callback function to be executed on failed login.
     */
    fun onlogins(email: String, password: String, onSuccess: () -> Unit, onFailure: () -> Unit) {
        // Use the Firebase Authentication API to sign in the user
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sign-in successful
                    Log.d("LoginView", "signInWithEmail:success")
                    val user = auth.currentUser

                    // Fetch the user's FCM token and update it in Firestore
                    user?.email?.let { currentEmail ->
                        FirebaseMessaging.getInstance().token
                            .addOnCompleteListener { tokenTask ->
                                if (!tokenTask.isSuccessful) {
                                    Log.w("LoginView", "Fetching FCM registration token failed", tokenTask.exception)
                                    // Not critical if it fails, user is still logged in
                                } else {
                                    val token = tokenTask.result
                                    // Update Firestore with the user's token
                                    viewModelScope.launch {
                                        userRepository.updateFcmToken(currentEmail, token)
                                    }
                                }
                            }
                    }

                    // Execute the onSuccess callback
                    onSuccess()
                } else {
                    // Sign-in failed
                    Log.w("LoginView", "signInWithEmail:failure", task.exception)
                    // Execute the onFailure callback
                    onFailure()
                }
            }
    }

    /**
     * Logs in the user with the provided email and password using the AuthRepository.
     * @param email The email of the user.
     * @param password The password of the user.
     * @param onSuccess A callback function to be executed on successful login.
     * @param onFailure A callback function to be executed on failed login.
     */
    fun login(email: String, password: String, onSuccess: () -> Unit, onFailure: () -> Unit) {
        viewModelScope.launch {
            try {
                // Set the loading state to true
                _isLoading.value = true
                // Reset the error state
                _error.value = null
                Log.d("loginviewmodel", "passed repo")

                // Use the AuthRepository to log in the user
                when (val result = repository.login(email, password)) {
                    is Result.Success -> {
                        // After successful sign-in, fetch the user's FCM token and update it in Firestore
                        FirebaseMessaging.getInstance().token
                            .addOnCompleteListener { tokenTask ->
                                if (tokenTask.isSuccessful) {
                                    val token = tokenTask.result
                                    viewModelScope.launch {
                                        userRepository.updateFcmToken(email, token)
                                    }
                                }
                                // Execute the onSuccess callback
                                onSuccess()
                            }
                    }
                    is Result.Error -> {
                        // Set the error state with the exception message
                        _error.value = result.exception.message
                        // Execute the onFailure callback
                        onFailure()
                    }
                    is Result.Loading -> Log.d("loginview", "loading")
                }
            } finally {
                // Set the loading state to false
                _isLoading.value = false
            }
        }
    }
}