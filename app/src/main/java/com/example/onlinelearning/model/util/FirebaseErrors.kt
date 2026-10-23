package com.example.onlinelearning.model.util

import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.onlinelearning.model.repository.FirebaseNotConfiguredException
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
fun Throwable.toUserMessage(): String = when {
    message.orEmpty().contains("BILLING_NOT_ENABLED") ->
        "Firebase отправляет SMS только на тарифе Blaze. Для проверки добавьте тестовый номер: " +
            "Authentication → Sign-in method → Phone → Phone numbers for testing"
    message.orEmpty().contains("28444") || message.orEmpty().contains("Developer console is not set up correctly") ->
        "Google: приложение не зарегистрировано. Добавьте SHA-1 этой сборки в Firebase (Project settings → Your apps), " +
            "затем скачайте google-services.json заново"
    else -> toUserMessageByType()
}

private fun Throwable.toUserMessageByType(): String = when (this) {
    is FirebaseNotConfiguredException -> message.orEmpty()
    is FirebaseNetworkException -> "Нет подключения к интернету"
    is FirebaseTooManyRequestsException -> "Слишком много попыток. Попробуйте позже"
    // Credential Manager also reports "no credentials" when the build's SHA-1 is not registered in Firebase.
    is NoCredentialException ->
        "Google не предложил аккаунт: проверьте, что на телефоне есть аккаунт Google и что SHA-1 этой сборки " +
            "добавлен в Firebase (Project settings → Your apps)"
    is GetCredentialException -> "Не удалось войти через Google ($type): ${message.orEmpty()}"
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
