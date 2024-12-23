package com.example.chatapp.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatapp.data.model.Group
import com.example.chatapp.data.model.User
import com.example.chatapp.data.repository.MessageRepository
import com.example.chatapp.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GroupSettingsViewModel : ViewModel() {
    private val messageRepository = MessageRepository()
    private val userRepository = UserRepository()

    private val _group = MutableStateFlow<Group?>(null)
    val group: StateFlow<Group?> = _group

    private val _members = MutableStateFlow<List<User>>(emptyList())
    val members: StateFlow<List<User>> = _members

    private val _error = MutableStateFlow("")
    val error: StateFlow<String> = _error

    fun loadGroup(groupId: String) {
        viewModelScope.launch {
            try {
                messageRepository.getGroup(groupId).collect { result ->
                    when (result) {
                        is Result.Success -> {
                            _group.value = result.data
                            loadMembers(result.data.members)
                        }
                        is Result.Error -> _error.value = result.exception.message ?: "Unknown error"
                    }
                }
            } catch (e: Exception) {
                _error.value = "Failed to load group: ${e.message}"
            }
        }
    }

    private fun loadMembers(memberEmails: List<String>) {
        viewModelScope.launch {
            try {
                val membersList = memberEmails.mapNotNull { email ->
                    userRepository.getUser(email)
                }
                _members.value = membersList
            } catch (e: Exception) {
                _error.value = "Failed to load members: ${e.message}"
            }
        }
    }

    fun addMember(groupId: String, email: String) {
        viewModelScope.launch {
            try {
                messageRepository.addMemberToGroup(groupId, email)
            } catch (e: Exception) {
                _error.value = "Failed to add member: ${e.message}"
            }
        }
    }

    fun removeMember(groupId: String, email: String) {
        viewModelScope.launch {
            try {
                messageRepository.removeMemberFromGroup(groupId, email)
            } catch (e: Exception) {
                _error.value = "Failed to remove member: ${e.message}"
            }
        }
    }

    fun updateGroupName(groupId: String, newName: String) {
        viewModelScope.launch {
            try {
                messageRepository.updateGroupName(groupId, newName)
            } catch (e: Exception) {
                _error.value = "Failed to update group name: ${e.message}"
            }
        }
    }

    fun leaveGroup(groupId: String, email: String) {
        viewModelScope.launch {
            try {
                messageRepository.removeMemberFromGroup(groupId, email)
            } catch (e: Exception) {
                _error.value = "Failed to leave group: ${e.message}"
            }
        }
    }
} 