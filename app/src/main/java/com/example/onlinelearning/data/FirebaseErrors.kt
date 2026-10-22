package com.example.onlinelearning.data

import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException

/** True when the user simply closed a sign-in dialog; nothing needs to be shown. */
fun Throwable.isUserCancellation(): Boolean = this is GetCredentialCancellationException

/** Human-readable (Russian) text for Firebase and sign-in errors shown in the UI. */
fun Throwable.toUserMessage(): String = when (this) {
    is FirebaseNotConfiguredException -> message.orEmpty()
    is FirebaseNetworkException -> "Нет подключения к интернету"
    is FirebaseTooManyRequestsException -> "Слишком много попыток. Попробуйте позже"
    is NoCredentialException -> "На телефоне нет аккаунта Google — добавьте его в настройках"
    is GetCredentialException -> "Не удалось войти через Google: ${message.orEmpty()}"
    is FirebaseAuthWeakPasswordException -> "Слишком простой пароль: нужно не меньше 6 символов"
    is FirebaseAuthUserCollisionException -> "Этот email уже зарегистрирован — войдите через Log in"
    is FirebaseAuthInvalidUserException -> "Аккаунт не найден"
    is FirebaseAuthInvalidCredentialsException -> when (errorCode) {
        "ERROR_INVALID_VERIFICATION_CODE" -> "Неверный код из SMS"
        "ERROR_INVALID_PHONE_NUMBER" -> "Неверный номер телефона. Введите его с кодом страны, например +7…"
        "ERROR_SESSION_EXPIRED" -> "Код устарел — запросите новый"
        else -> "Неверный email или пароль"
    }
    is FirebaseAuthException -> when (errorCode) {
        "ERROR_OPERATION_NOT_ALLOWED" ->
            "Этот способ входа выключен: Firebase Console → Authentication → Sign-in method"
        "ERROR_APP_NOT_AUTHORIZED", "ERROR_INVALID_APP_CREDENTIAL", "ERROR_MISSING_CLIENT_IDENTIFIER" ->
            "Добавьте SHA-1 и SHA-256 приложения в Firebase Console (Project settings → Your apps)"
        else -> message ?: "Ошибка входа"
    }
    is FirebaseFirestoreException -> when (code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "Firestore отклонил запрос: опубликуйте правила из firestore.rules " +
                "(Firebase Console → Firestore Database → Rules)"
        FirebaseFirestoreException.Code.UNAVAILABLE -> "Firestore недоступен — проверьте интернет"
        else -> "Ошибка Firestore: ${message.orEmpty()}"
    }
    else -> message ?: "Ошибка Firebase"
}
