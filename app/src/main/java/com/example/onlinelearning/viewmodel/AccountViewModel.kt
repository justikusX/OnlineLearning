package com.example.onlinelearning.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onlinelearning.model.repository.AuthRepository
import com.example.onlinelearning.model.repository.SessionRepository
import com.example.onlinelearning.model.repository.UserRepository
import com.example.onlinelearning.model.util.AvatarCodec
import com.example.onlinelearning.model.util.toUserMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AccountUiState(
    val signedIn: Boolean = false,
    val name: String = "Guest",
    /** Email, or the phone number for phone accounts. */
    val contact: String = "",
    val contactIsEmail: Boolean = true,
    val avatar: String = "",
    val savingPhoto: Boolean = false,
)

/** Account and Edit Account screens: profile photo, name and signing out. */
class AccountViewModel(
    private val app: Application,
    private val session: SessionRepository,
    private val users: UserRepository,
    private val auth: AuthRepository,
) : ViewModel() {

    private val savingPhoto = MutableStateFlow(false)

    val state: StateFlow<AccountUiState> = combine(session.user, session.profile, savingPhoto) { user, profile, saving ->
        AccountUiState(
            signedIn = user != null,
            name = SessionRepository.displayName(profile, user),
            contact = user?.email ?: user?.phoneNumber.orEmpty(),
            contactIsEmail = user?.email != null,
            avatar = profile?.avatar.orEmpty(),
            savingPhoto = saving,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AccountUiState())

    private val _editName = MutableStateFlow("")

    /** Text of the name field on Edit Account. */
    val editName: StateFlow<String> = _editName.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    /** Called when the photo is tapped; the screen opens the system photo picker when this returns true. */
    fun canPickPhoto(): Boolean {
        if (session.uid == null) _messages.tryEmit("Войдите, чтобы выбрать фото профиля")
        return session.uid != null
    }

    /** Photo chosen in the picker: squared to 256x256 JPEG and saved to the profile as Base64. */
    fun onPhotoPicked(uri: Uri) {
        val uid = session.uid ?: return
        viewModelScope.launch {
            savingPhoto.value = true
            runCatching { AvatarCodec.encode(app, uri) }
                .onSuccess { encoded ->
                    runCatching { users.setAvatar(uid, encoded) }.onFailure { _messages.tryEmit(it.toUserMessage()) }
                }
                .onFailure { _messages.tryEmit("Не удалось открыть фото") }
            savingPhoto.value = false
        }
    }

    fun removePhoto() {
        val uid = session.uid ?: return
        viewModelScope.launch {
            runCatching { users.setAvatar(uid, null) }.onFailure { _messages.tryEmit(it.toUserMessage()) }
        }
    }

    /** Fills the name field when Edit Account opens. */
    fun startEditing() {
        _editName.value = session.profile.value?.name ?: session.user.value?.displayName.orEmpty()
    }

    fun onNameChange(value: String) {
        _editName.value = value
    }

    fun saveName(onDone: () -> Unit) {
        val name = _editName.value.trim()
        val uid = session.uid ?: return
        if (name.isEmpty()) {
            _messages.tryEmit("Введите имя")
            return
        }
        viewModelScope.launch {
            runCatching {
                users.updateName(uid, name)
                runCatching { auth.updateDisplayName(name) }
            }
                .onSuccess {
                    _messages.tryEmit("Сохранено")
                    onDone()
                }
                .onFailure { _messages.tryEmit(it.toUserMessage()) }
        }
    }

    fun signOut() = auth.signOut()
}
