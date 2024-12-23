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

class ProfileViewModel : ViewModel() {
    private val repository = UserRepository()
    private val storage = FirebaseStorage.getInstance()

    private val _userState = MutableStateFlow<User?>(null)
    val userState: StateFlow<User?> = _userState

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow("")
    val error: StateFlow<String> = _error

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

                val photoUrl = profileImageUri?.let { uri ->
                    uploadProfileImage(email, uri)
                }

                repository.updateUserDetails(
                    email = email,
                    username = username,
                    photoUrl = photoUrl
                )

                loadUserProfile(email)
                onComplete()
            } catch (e: Exception) {
                _error.value = "Failed to update profile: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun uploadProfileImage(email: String, imageUri: Uri): String {
        val imageRef = storage.reference.child("profile_images/${email}_${System.currentTimeMillis()}")
        imageRef.putFile(imageUri).await()
        return imageRef.downloadUrl.await().toString()
    }
} 