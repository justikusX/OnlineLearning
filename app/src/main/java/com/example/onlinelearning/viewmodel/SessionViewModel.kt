package com.example.onlinelearning.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onlinelearning.model.entity.LearningStats
import com.example.onlinelearning.model.entity.UserProfile
import com.example.onlinelearning.model.entity.learningStats
import com.example.onlinelearning.model.repository.AuthRepository
import com.example.onlinelearning.model.repository.SessionRepository
import com.example.onlinelearning.model.util.toUserMessage
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn

/**
 * App-wide state shared by all screens of the activity: who is signed in, their name and photo,
 * the "Clocking in!" statistics and messages for the snackbar.
 */
class SessionViewModel(
    private val session: SessionRepository,
    private val auth: AuthRepository,
) : ViewModel() {

    val firebaseConfigured: Boolean = auth.isConfigured

    val user: StateFlow<FirebaseUser?> = session.user
    val profile: StateFlow<UserProfile?> = session.profile

    /** Name for greetings and the avatar letter. */
    val name: StateFlow<String> = combine(session.profile, session.user) { p, u -> SessionRepository.displayName(p, u) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SessionRepository.displayName(profile.value, user.value))

    val stats: StateFlow<LearningStats> = session.profile.map { it.learningStats() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, LearningStats())

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)

    /** Snackbar texts: profile errors from Firestore plus anything sent with [showMessage]. */
    val messages: Flow<String> = merge(_messages, session.errors.map { it.toUserMessage() })

    fun showMessage(text: String) {
        _messages.tryEmit(text)
    }
}
