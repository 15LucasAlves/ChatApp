package com.example.chatapp.ui.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatapp.data.model.User
import com.example.chatapp.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UserSelectionViewModel : ViewModel() {
    private val repository = UserRepository()
    
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    private val _error = MutableStateFlow("")
    val error: StateFlow<String> = _error

    private var allUsers: List<User> = listOf()

    fun loadUsers(currentUserEmail: String) {
        viewModelScope.launch {
            try {
                repository.getAllUsers().collect { usersList ->
                    allUsers = usersList.filter { it.email != currentUserEmail }
                    _users.value = allUsers
                }
            } catch (e: Exception) {
                _error.value = "Failed to load users: ${e.message}"
            }
        }
    }

    fun searchUsers(query: String) {
        if (query.isEmpty()) {
            _users.value = allUsers
        } else {
            _users.value = allUsers.filter { user ->
                user.email.contains(query, ignoreCase = true) ||
                user.username?.contains(query, ignoreCase = true) == true
            }
        }
    }
} 