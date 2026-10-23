package com.example.onlinelearning.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onlinelearning.model.repository.AuthRepository
import com.example.onlinelearning.model.repository.UserRepository
import com.example.onlinelearning.model.util.isUserCancellation
import com.example.onlinelearning.model.util.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the user typed on the Log In / Sign Up screens. */
data class AuthForm(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val agreed: Boolean = false,
)

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val info: String? = null,
)

/** Log In and Sign Up screens: email/password and Google sign-in. */
class AuthViewModel(
    private val auth: AuthRepository,
    private val users: UserRepository,
) : ViewModel() {

    private val _form = MutableStateFlow(AuthForm())
    val form: StateFlow<AuthForm> = _form.asStateFlow()

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    val firebaseConfigured: Boolean = auth.isConfigured

    fun onNameChange(value: String) = _form.update { it.copy(name = value) }
    fun onEmailChange(value: String) = _form.update { it.copy(email = value) }
    fun onPasswordChange(value: String) = _form.update { it.copy(password = value) }
    fun onAgreedChange(value: Boolean) = _form.update { it.copy(agreed = value) }

    fun signIn(onSuccess: () -> Unit) {
        val (_, email, password) = _form.value
        if (email.isBlank() || password.isBlank()) {
            _state.value = AuthUiState(error = "Введите email и пароль")
            return
        }
        run(onSuccess) { auth.signIn(email.trim(), password) }
    }

    fun signUp(onSuccess: () -> Unit) {
        val (name, email, password) = _form.value
        when {
            name.isBlank() || email.isBlank() -> _state.value = AuthUiState(error = "Заполните все поля")
            password.length < 6 -> _state.value = AuthUiState(error = "Пароль должен быть не короче 6 символов")
            else -> run(onSuccess) {
                val user = auth.signUp(name.trim(), email.trim(), password)
                // The account already exists at this point, so a Firestore failure must not keep the user
                // on the sign-up screen: SessionRepository retries the profile and reports the error.
                runCatching { users.createProfile(user.uid, name.trim(), email.trim()) }
            }
        }
    }

    fun signInWithGoogle(activity: Activity, onSuccess: () -> Unit) = run(onSuccess) { auth.signInWithGoogle(activity) }

    fun resetPassword() {
        val email = _form.value.email
        if (email.isBlank()) {
            _state.value = AuthUiState(error = "Введите email, чтобы восстановить пароль")
            return
        }
        viewModelScope.launch {
            _state.value = AuthUiState(loading = true)
            _state.value = runCatching { auth.sendPasswordReset(email.trim()) }.fold(
                onSuccess = { AuthUiState(info = "Письмо для сброса пароля отправлено") },
                onFailure = { AuthUiState(error = it.toUserMessage()) },
            )
        }
    }

    fun clearMessages() = _state.update { it.copy(error = null, info = null) }

    private fun run(onSuccess: () -> Unit, block: suspend () -> Unit) {
        viewModelScope.launch {
            _state.value = AuthUiState(loading = true)
            runCatching { block() }
                .onSuccess {
                    _state.value = AuthUiState()
                    _form.update { it.copy(password = "") }
                    onSuccess()
                }
                .onFailure {
                    _state.value = if (it.isUserCancellation()) AuthUiState() else AuthUiState(error = it.toUserMessage())
                }
        }
    }
}
