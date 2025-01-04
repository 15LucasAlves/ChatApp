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

class LoginViewModel : ViewModel() {
    private val repository = AuthRepository()
    private val userRepository = UserRepository()

    val auth = Firebase.auth

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // test function
    fun onlogins(email: String, password: String, onSuccess: () -> Unit, onFailure: () -> Unit){
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // sign in success
                    Log.d("LoginView", "signInWithEmail:success")
                    val user = auth.currentUser

                    // 1) After login, fetch token & store in Firestore
                    user?.email?.let { currentEmail ->
                        FirebaseMessaging.getInstance().token
                            .addOnCompleteListener { tokenTask ->
                                if (!tokenTask.isSuccessful) {
                                    Log.w("LoginView", "Fetching FCM registration token failed", tokenTask.exception)
                                    // Not critical if it fails, user is still logged in
                                } else {
                                    val token = tokenTask.result
                                    // 2) Update Firestore with user’s token
                                    viewModelScope.launch {
                                        userRepository.updateFcmToken(currentEmail, token)
                                    }
                                }
                            }
                    }

                    onSuccess()
                } else {
                    // if sign in fails
                    Log.w("LoginView", "signInWithEmail:failure", task.exception)
                    onFailure()
                }
            }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit, onFailure: () -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                Log.d("loginviewmodel","passed repo")
                when (val result = repository.login(email, password)) {
                    is Result.Success -> {
                        // After sign in, again do FCM token check
                        FirebaseMessaging.getInstance().token
                            .addOnCompleteListener { tokenTask ->
                                if (tokenTask.isSuccessful) {
                                    val token = tokenTask.result
                                    viewModelScope.launch {
                                        userRepository.updateFcmToken(email, token)
                                    }
                                }
                                onSuccess()
                            }
                    }
                    is Result.Error -> {
                        _error.value = result.exception.message
                        onFailure()
                    }
                    is Result.Loading -> Log.d("loginview", "loading")
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
}