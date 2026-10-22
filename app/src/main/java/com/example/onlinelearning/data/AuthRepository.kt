package com.example.onlinelearning.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

class FirebaseNotConfiguredException : IllegalStateException(
    "Firebase не настроен: добавьте app/google-services.json и пересоберите проект"
)

/** Email/password auth via Firebase Auth. [auth] is null when google-services.json is missing. */
class AuthRepository(private val auth: FirebaseAuth?) {

    val isConfigured: Boolean get() = auth != null

    val currentUser: FirebaseUser? get() = auth?.currentUser

    val userFlow: Flow<FirebaseUser?> = auth?.let { firebaseAuth ->
        callbackFlow {
            val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
            firebaseAuth.addAuthStateListener(listener)
            awaitClose { firebaseAuth.removeAuthStateListener(listener) }
        }
    } ?: flowOf(null)

    suspend fun signIn(email: String, password: String): FirebaseUser {
        val result = requireAuth().signInWithEmailAndPassword(email, password).await()
        return checkNotNull(result.user)
    }

    suspend fun signUp(name: String, email: String, password: String): FirebaseUser {
        val result = requireAuth().createUserWithEmailAndPassword(email, password).await()
        val user = checkNotNull(result.user)
        user.updateProfile(userProfileChangeRequest { displayName = name }).await()
        return user
    }

    suspend fun sendPasswordReset(email: String) {
        requireAuth().sendPasswordResetEmail(email).await()
    }

    fun signOut() {
        auth?.signOut()
    }

    private fun requireAuth(): FirebaseAuth = auth ?: throw FirebaseNotConfiguredException()
}
