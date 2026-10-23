package com.example.onlinelearning.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onlinelearning.model.entity.AppNotification
import com.example.onlinelearning.model.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class MessagesUiState(
    val signedIn: Boolean = false,
    /** 0 = "message", 1 = "notification". */
    val tab: Int = 0,
    val messages: List<AppNotification> = emptyList(),
    val notifications: List<AppNotification> = emptyList(),
) {
    val shown: List<AppNotification> get() = if (tab == 0) messages else notifications
}

/** Message tab ("Notifications" in the design): entries from the user's profile, newest first. */
class MessagesViewModel(session: SessionRepository) : ViewModel() {

    private val tab = MutableStateFlow(0)

    val state: StateFlow<MessagesUiState> = combine(session.user, session.profile, tab) { user, profile, t ->
        val all = profile?.notifications.orEmpty().sortedByDescending { it.time }
        MessagesUiState(
            signedIn = user != null,
            tab = t,
            messages = all.filter { it.kind == AppNotification.KIND_MESSAGE },
            notifications = all.filter { it.kind != AppNotification.KIND_MESSAGE },
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MessagesUiState())

    fun selectTab(index: Int) {
        tab.value = index
    }
}
