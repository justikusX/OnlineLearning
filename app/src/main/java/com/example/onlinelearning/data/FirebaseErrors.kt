package com.example.onlinelearning.data

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException

/** Human-readable (Russian) text for Firebase errors shown in the UI. */
fun Throwable.toUserMessage(): String = when (this) {
    is FirebaseNotConfiguredException -> message.orEmpty()
    is FirebaseNetworkException -> "Нет подключения к интернету"
    is FirebaseAuthWeakPasswordException -> "Слишком простой пароль: нужно не меньше 6 символов"
    is FirebaseAuthUserCollisionException -> "Этот email уже зарегистрирован — войдите через Log in"
    is FirebaseAuthInvalidUserException -> "Аккаунт с таким email не найден"
    is FirebaseAuthInvalidCredentialsException -> "Неверный email или пароль"
    is FirebaseFirestoreException -> when (code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "Firestore отклонил запрос: опубликуйте правила из firestore.rules " +
                "(Firebase Console → Firestore Database → Rules)"
        FirebaseFirestoreException.Code.UNAVAILABLE -> "Firestore недоступен — проверьте интернет"
        else -> "Ошибка Firestore: ${message.orEmpty()}"
    }
    else -> message ?: "Ошибка Firebase"
}
