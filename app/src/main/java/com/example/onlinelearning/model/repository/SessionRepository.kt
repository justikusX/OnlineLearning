package com.example.onlinelearning.model.repository

import com.example.onlinelearning.model.entity.UserProfile
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The signed-in user and their Firestore profile (users/{uid}), shared by all ViewModels,
 * so the app keeps a single listener on the profile document.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionRepository(
    auth: AuthRepository,
    private val users: UserRepository,
    scope: CoroutineScope,
) {
    val user: StateFlow<FirebaseUser?> =
        auth.userFlow.stateIn(scope, SharingStarted.Eagerly, auth.currentUser)

    private val _errors = MutableSharedFlow<Throwable>(extraBufferCapacity = 4)

    /** Failures of loading or creating the profile; SessionViewModel shows them to the user. */
    val errors: SharedFlow<Throwable> = _errors.asSharedFlow()

    /** Profile of the current user; null while signed out or loading. Re-subscribes when the user changes. */
    val profile: StateFlow<UserProfile?> = user
        .flatMapLatest { u ->
            users.profileFlow(u?.uid).catch {
                _errors.tryEmit(it)
                emit(null)
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, null)

    val uid: String? get() = user.value?.uid

    init {
        // Every signed-in user gets a users/{uid} document, even if it failed to be created at sign-up.
        scope.launch {
            user.collect { u ->
                if (u != null) {
                    runCatching { users.ensureProfile(u.uid, u.displayName, u.email) }
                        .onFailure { _errors.tryEmit(it) }
                }
            }
        }
    }

    companion object {
        /** Name shown in greetings: profile name, then the Firebase display name, phone number or "Guest". */
        fun displayName(profile: UserProfile?, user: FirebaseUser?): String =
            profile?.name?.takeIf { it.isNotBlank() }
                ?: user?.displayName?.takeIf { it.isNotBlank() }
                ?: user?.phoneNumber?.takeIf { it.isNotBlank() }
                ?: "Guest"
    }
}
