package com.example.onlinelearning.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.onlinelearning.LearningApp
import com.example.onlinelearning.data.AuthRepository
import com.example.onlinelearning.data.CourseRepository
import com.example.onlinelearning.data.UserRepository
import com.example.onlinelearning.data.model.Course
import com.example.onlinelearning.data.model.Lesson
import com.example.onlinelearning.data.model.UserProfile
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

object AppViewModelFactory {
    val Factory = viewModelFactory {
        initializer { SessionViewModel(container().auth, container().users) }
        initializer { AuthViewModel(container().auth, container().users) }
        initializer { CoursesViewModel(container().courses) }
    }

    private fun androidx.lifecycle.viewmodel.CreationExtras.container() =
        (this[APPLICATION_KEY] as LearningApp).container
}

/** Signed-in user and their Firestore profile; shared by all screens of the activity. */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModel(
    private val auth: AuthRepository,
    private val users: UserRepository,
) : ViewModel() {

    val firebaseConfigured: Boolean = auth.isConfigured

    val user: StateFlow<FirebaseUser?> =
        auth.userFlow.stateIn(viewModelScope, SharingStarted.Eagerly, auth.currentUser)

    val profile: StateFlow<UserProfile?> = user
        .flatMapLatest { users.profileFlow(it?.uid) }
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages = _messages.asSharedFlow()

    fun toggleFavorite(courseId: String) = withUser { uid ->
        val isFavorite = profile.value?.favorites?.contains(courseId) == true
        users.setFavorite(uid, courseId, !isFavorite)
    }

    fun purchase(courseId: String, onSuccess: () -> Unit) = withUser { uid ->
        users.purchase(uid, courseId)
        onSuccess()
    }

    fun completeLesson(course: Course, lesson: Lesson) = withUser { uid ->
        users.completeLesson(uid, course.id, lesson.id, lesson.durationMinutes)
    }

    fun signOut() = auth.signOut()

    private fun withUser(block: suspend (uid: String) -> Unit) {
        val uid = user.value?.uid
        if (uid == null) {
            _messages.tryEmit("Войдите в аккаунт, чтобы сохранять прогресс")
            return
        }
        viewModelScope.launch {
            runCatching { block(uid) }.onFailure { _messages.tryEmit(it.message ?: "Ошибка Firebase") }
        }
    }
}

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val info: String? = null,
)

class AuthViewModel(
    private val auth: AuthRepository,
    private val users: UserRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    val firebaseConfigured: Boolean = auth.isConfigured

    fun signIn(email: String, password: String, onSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            _state.value = AuthUiState(error = "Введите email и пароль")
            return
        }
        run(onSuccess) { auth.signIn(email.trim(), password) }
    }

    fun signUp(name: String, email: String, password: String, onSuccess: () -> Unit) {
        when {
            name.isBlank() || email.isBlank() -> _state.value = AuthUiState(error = "Заполните все поля")
            password.length < 6 -> _state.value = AuthUiState(error = "Пароль должен быть не короче 6 символов")
            else -> run(onSuccess) {
                val user = auth.signUp(name.trim(), email.trim(), password)
                users.createProfile(user.uid, name.trim(), email.trim())
            }
        }
    }

    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _state.value = AuthUiState(error = "Введите email, чтобы восстановить пароль")
            return
        }
        viewModelScope.launch {
            _state.value = AuthUiState(loading = true)
            _state.value = runCatching { auth.sendPasswordReset(email.trim()) }.fold(
                onSuccess = { AuthUiState(info = "Письмо для сброса пароля отправлено") },
                onFailure = { AuthUiState(error = it.message) },
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
                    onSuccess()
                }
                .onFailure { _state.value = AuthUiState(error = it.message ?: "Не удалось выполнить вход") }
        }
    }
}

data class CoursesUiState(
    val loading: Boolean = true,
    val courses: List<Course> = emptyList(),
    val offline: Boolean = false,
    val error: String? = null,
)

/** Course catalog loaded through Retrofit, with search and category filtering. */
class CoursesViewModel(private val repository: CourseRepository) : ViewModel() {

    private val _state = MutableStateFlow(CoursesUiState())
    val state: StateFlow<CoursesUiState> = _state.asStateFlow()

    val query = MutableStateFlow("")
    val category = MutableStateFlow<String?>(null)

    val filtered: StateFlow<List<Course>> = combine(_state, query, category) { state, q, cat ->
        state.courses.filter { course ->
            (cat == null || course.category == cat) &&
                (q.isBlank() || course.title.contains(q, ignoreCase = true) ||
                    course.author.contains(q, ignoreCase = true))
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        refresh()
    }

    fun refresh(force: Boolean = false) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            _state.value = runCatching { repository.getCourses(force) }.fold(
                onSuccess = { CoursesUiState(loading = false, courses = it, offline = repository.isOffline) },
                onFailure = { CoursesUiState(loading = false, error = it.message) },
            )
        }
    }

    fun course(id: String): Course? = _state.value.courses.firstOrNull { it.id == id }
}
