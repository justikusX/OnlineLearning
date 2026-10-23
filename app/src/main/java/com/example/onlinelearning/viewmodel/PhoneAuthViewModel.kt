package com.example.onlinelearning.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onlinelearning.model.repository.AuthRepository
import com.example.onlinelearning.model.repository.PhoneVerification
import com.example.onlinelearning.model.util.toUserMessage
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PhoneStep { PHONE, CODE, DONE }

data class PhoneAuthUiState(
    val step: PhoneStep = PhoneStep.PHONE,
    /** Digits of the number including the country code, without "+". */
    val phoneDigits: String = "",
    val code: String = "",
    val loading: Boolean = false,
    val error: String? = null,
) {
    val phone: String get() = "+$phoneDigits"
}

/** "Continue with Phone" → "Verify Phone" → "Success" flow on top of Firebase phone auth. */
class PhoneAuthViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(PhoneAuthUiState())
    val state: StateFlow<PhoneAuthUiState> = _state.asStateFlow()

    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    fun onDigit(digit: Char) = _state.update {
        when (it.step) {
            PhoneStep.PHONE -> if (it.phoneDigits.length < 15) it.copy(phoneDigits = it.phoneDigits + digit, error = null) else it
            PhoneStep.CODE -> if (it.code.length < CODE_LENGTH) it.copy(code = it.code + digit, error = null) else it
            PhoneStep.DONE -> it
        }
    }

    fun onBackspace() = _state.update {
        when (it.step) {
            PhoneStep.PHONE -> it.copy(phoneDigits = it.phoneDigits.dropLast(1), error = null)
            PhoneStep.CODE -> it.copy(code = it.code.dropLast(1), error = null)
            PhoneStep.DONE -> it
        }
    }

    fun sendCode(activity: Activity, resend: Boolean = false) {
        val s = _state.value
        if (s.phoneDigits.length < 8) {
            _state.update { it.copy(error = "Введите номер с кодом страны, например 7 999 123 45 67") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching { auth.sendPhoneCode(activity, s.phone, if (resend) resendToken else null) }
                .onSuccess { result ->
                    when (result) {
                        is PhoneVerification.CodeSent -> {
                            verificationId = result.verificationId
                            resendToken = result.resendToken
                            _state.update { it.copy(step = PhoneStep.CODE, code = "", loading = false) }
                        }
                        is PhoneVerification.AutoVerified -> finish { auth.signInWithCredential(result.credential) }
                    }
                }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.toUserMessage()) } }
        }
    }

    fun verify() {
        val id = verificationId ?: return
        val code = _state.value.code
        if (code.length < CODE_LENGTH) {
            _state.update { it.copy(error = "Введите код из SMS") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            finish { auth.signInWithPhoneCode(id, code) }
        }
    }

    private suspend fun finish(signIn: suspend () -> Unit) {
        runCatching { signIn() }
            .onSuccess { _state.update { it.copy(step = PhoneStep.DONE, loading = false) } }
            .onFailure { e -> _state.update { it.copy(loading = false, error = e.toUserMessage()) } }
    }

    /** Returns true when the back press was handled inside the flow. */
    fun back(): Boolean {
        if (_state.value.step == PhoneStep.CODE) {
            _state.update { it.copy(step = PhoneStep.PHONE, code = "", error = null) }
            return true
        }
        return false
    }

    fun reset() {
        verificationId = null
        resendToken = null
        _state.value = PhoneAuthUiState()
    }

    companion object {
        const val CODE_LENGTH = 6
    }
}
