package com.example.chatapp.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatapp.data.model.User
import com.example.chatapp.data.repository.UserRepository
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * ViewModel for the profile screen in the chat app.
 * It handles the logic for loading, updating, and managing the user's profile information.
 */
class ProfileViewModel : ViewModel() {
    // Repository for interacting with user data
    private val repository = UserRepository()
    // Firebase Storage instance for uploading profile images
    private val storage = FirebaseStorage.getInstance()

    // MutableStateFlow to hold the current user's profile information
    private val _userState = MutableStateFlow<User?>(null)
    val userState: StateFlow<User?> = _userState

    // MutableStateFlow to indicate the loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // MutableStateFlow to hold any error messages
    private val _error = MutableStateFlow("")
    val error: StateFlow<String> = _error

    /**
     * Loads the user's profile information from the repository.
     * @param email The email address of the user whose profile to load.
     */
    fun loadUserProfile(email: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = ""
                val user = repository.getUser(email)
                _userState.value = user
            } catch (e: Exception) {
                _error.value = "Failed to load profile: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Updates the user's profile information, including the username and profile image.
     * @param email The email address of the user whose profile to update.
     * @param username The new username to set.
     * @param profileImageUri The new profile image URI, or null if no image is provided.
     * @param onComplete A callback function to be called when the update is complete.
     */
    fun updateProfile(
        email: String,
        username: String,
        profileImageUri: Uri?,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = ""

                // Upload the new profile image, if provided
                val photoUrl = profileImageUri?.let { uri ->
                    uploadProfileImage(email, uri)
                }

                // Update the user's details in the repository
                repository.updateUserDetails(
                    email = email,
                    username = username,
                    photoUrl = photoUrl
                )

                // Reload the user's profile to update the UI
                loadUserProfile(email)
                onComplete()
            } catch (e: Exception) {
                _error.value = "Failed to update profile: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Uploads a profile image to Firebase Storage.
     * @param email The email address of the user whose profile image to upload.
     * @param imageUri The URI of the image to upload.
     * @return The download URL of the uploaded image.
     */
    private suspend fun uploadProfileImage(email: String, imageUri: Uri): String {
        val imageRef = storage.reference.child("profile_images/${email}_${System.currentTimeMillis()}")
        imageRef.putFile(imageUri).await()
        return imageRef.downloadUrl.await().toString()
    }
}