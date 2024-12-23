package com.example.chatapp.ui.groups

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatapp.data.model.Group
import com.example.chatapp.data.repository.MessageRepository
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class GroupViewModel : ViewModel() {
    private val repository = MessageRepository()
    private val storage = FirebaseStorage.getInstance()

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow("")
    val error: StateFlow<String> = _error

    fun loadUserGroups(userEmail: String) {
        viewModelScope.launch {
            try {
                repository.getUserGroups(userEmail).collect { result ->
                    when (result) {
                        is Result.Success -> _groups.value = result.data
                        is Result.Error -> _error.value = result.exception.message ?: "Unknown error"
                    }
                }
            } catch (e: Exception) {
                _error.value = "Failed to load groups: ${e.message}"
            }
        }
    }

    fun createGroup(
        name: String,
        creatorEmail: String,
        members: List<String>,
        groupImage: Uri?
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = ""

                val photoUrl = groupImage?.let { uri ->
                    val imageRef = storage.reference.child("group_images/${System.currentTimeMillis()}")
                    imageRef.putFile(uri).await()
                    imageRef.downloadUrl.await().toString()
                }

                val group = Group(
                    name = name,
                    createdBy = creatorEmail,
                    members = members + creatorEmail,
                    photoUrl = photoUrl
                )

                repository.createGroup(group)
            } catch (e: Exception) {
                _error.value = "Failed to create group: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addMember(groupId: String, email: String) {
        viewModelScope.launch {
            try {
                repository.addMemberToGroup(groupId, email)
            } catch (e: Exception) {
                _error.value = "Failed to add member: ${e.message}"
            }
        }
    }

    fun removeMember(groupId: String, email: String) {
        viewModelScope.launch {
            try {
                repository.removeMemberFromGroup(groupId, email)
            } catch (e: Exception) {
                _error.value = "Failed to remove member: ${e.message}"
            }
        }
    }
} 