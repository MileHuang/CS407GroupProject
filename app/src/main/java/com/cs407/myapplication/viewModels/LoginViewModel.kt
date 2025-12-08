package com.cs407.myapplication.viewModels

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val userId: String? = null
)

class LoginViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    fun updateEmail(newEmail: String) {
        _uiState.value = _uiState.value.copy(email = newEmail)
    }

    fun updatePassword(newPass: String) {
        _uiState.value = _uiState.value.copy(password = newPass)
    }

    fun loginUser(onSuccess: (String) -> Unit) {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(error = "Email and password cannot be empty")
            return
        }

        _uiState.value = state.copy(isLoading = true, error = null)

        auth.signInWithEmailAndPassword(state.email, state.password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onSuccess(auth.currentUser!!.uid)
                    _uiState.value = _uiState.value.copy(isLoading = false)
                } else {
                    registerUser(onSuccess)
                }
            }
    }

    private fun registerUser(onSuccess: (String) -> Unit) {
        val state = _uiState.value

        auth.createUserWithEmailAndPassword(state.email, state.password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onSuccess(auth.currentUser!!.uid)
                    _uiState.value = _uiState.value.copy(isLoading = false)
                } else {
                    _uiState.value = state.copy(
                        isLoading = false,
                        error = task.exception?.message
                    )
                }
            }
    }
}
