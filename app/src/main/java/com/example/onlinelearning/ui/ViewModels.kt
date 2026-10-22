package com.example.onlinelearning.ui

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.onlinelearning.LearningApp
import com.example.onlinelearning.data.AuthRepository
import com.example.onlinelearning.data.CourseRepository
import com.example.onlinelearning.data.PhoneVerification
import com.example.onlinelearning.data.ThemeMode
import com.example.onlinelearning.data.ThemePreferences
import com.example.onlinelearning.data.UserRepository
import com.example.onlinelearning.data.isUserCancellation
import com.example.onlinelearning.data.model.AppNotification
import com.example.onlinelearning.data.model.Course
import com.example.onlinelearning.data.model.Lesson
import com.example.onlinelearning.data.model.UserProfile
import com.example.onlinelearning.data.toUserMessage
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
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
import java.time.LocalDate

object AppViewModelFactory {
    val Factory = viewModelFactory {
        initializer { SessionViewModel(container().auth, container().users) }
        initializer { AuthViewModel(container().auth, container().users) }
        initializer { PhoneAuthViewModel(container().auth) }
        initializer { CoursesViewModel(container().courses) }
        initializer { ThemeViewModel(container().theme) }
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

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages = _messages.asSharedFlow()

    /** Fires after the first lesson of the day is finished, to show the "Clocking in!" card. */
    private val _clockIn = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val clockIn: SharedFlow<Unit> = _clockIn.asSharedFlow()

    val profile: StateFlow<UserProfile?> = user
        .flatMapLatest { users.profileFlow(it?.uid) }
        .catch {
            _messages.tryEmit(it.toUserMessage())
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        // Every signed-in user gets a users/{uid} document, even if it failed to be created at sign-up.
        viewModelScope.launch {
            user.collect { u ->
                if (u != null) {
                    runCatching { users.ensureProfile(u.uid, u.displayName, u.email) }
                        .onFailure { _messages.tryEmit(it.toUserMessage()) }
                }
            }
        }
    }

    /** Name shown in greetings: profile name, then the Firebase display name, phone number or "Guest". */
    fun displayName(profile: UserProfile?, user: FirebaseUser?): String =
        profile?.name?.takeIf { it.isNotBlank() }
            ?: user?.displayName?.takeIf { it.isNotBlank() }
            ?: user?.phoneNumber?.takeIf { it.isNotBlank() }
            ?: "Guest"

    fun toggleFavorite(courseId: String) = withUser { uid ->
        val isFavorite = profile.value?.favorites?.contains(courseId) == true
        users.setFavorite(uid, courseId, !isFavorite)
    }

    fun purchase(course: Course, onSuccess: () -> Unit) = withUser { uid ->
        users.purchase(uid, course.id, course.title)
        onSuccess()
    }

    fun completeLesson(course: Course, lesson: Lesson) = withUser { uid ->
        val current = profile.value
        val done = current?.progress?.get(course.id).orEmpty()
        val firstToday = (current?.minutesOn(LocalDate.now()) ?: 0) == 0
        val notes = buildList {
            if (lesson.id !in done) {
                add(
                    UserRepository.notification(
                        kind = AppNotification.KIND_NOTIFICATION,
                        type = "lesson",
                        title = "Congratulations on completing the lesson",
                        text = "«${lesson.title}» in ${course.title}",
                    )
                )
                if ((done + lesson.id).toSet().containsAll(course.lessons.map { it.id })) {
                    add(
                        UserRepository.notification(
                            kind = AppNotification.KIND_MESSAGE,
                            type = "course",
                            title = "Online Learning",
                            text = "Congratulations, you have completed «${course.title}»! Pick your next course on the Course tab.",
                        )
                    )
                }
            }
        }
        users.completeLesson(uid, course.id, lesson.id, lesson.durationMinutes, notes)
        if (firstToday) _clockIn.tryEmit(Unit)
    }

    fun setAvatar(avatar: String?) = withUser { uid -> users.setAvatar(uid, avatar) }

    fun updateName(name: String, onDone: () -> Unit) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            _messages.tryEmit("Введите имя")
            return
        }
        withUser { uid ->
            users.updateName(uid, trimmed)
            runCatching { auth.updateDisplayName(trimmed) }
            _messages.tryEmit("Сохранено")
            onDone()
        }
    }

    fun showMessage(text: String) {
        _messages.tryEmit(text)
    }

    fun signOut() = auth.signOut()

    private fun withUser(block: suspend (uid: String) -> Unit) {
        val uid = user.value?.uid
        if (uid == null) {
            _messages.tryEmit("Войдите в аккаунт, чтобы сохранять прогресс")
            return
        }
        viewModelScope.launch {
            runCatching { block(uid) }.onFailure { _messages.tryEmit(it.toUserMessage()) }
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
                // The account already exists at this point, so a Firestore failure must not keep the user
                // on the sign-up screen: SessionViewModel retries the profile and reports the error.
                runCatching { users.createProfile(user.uid, name.trim(), email.trim()) }
            }
        }
    }

    fun signInWithGoogle(activity: Activity, onSuccess: () -> Unit) = run(onSuccess) { auth.signInWithGoogle(activity) }

    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _state.value = AuthUiState(error = "Введите email, чтобы восстановить пароль")
            return
        }
        viewModelScope.launch {
            _state.value = AuthUiState(loading = true)
            _state.value = runCatching { auth.sendPasswordReset(email.trim()) }.fold(
                onSuccess = { AuthUiState(info = "Письмо для сброса пароля отправлено") },
                onFailure = { AuthUiState(error = it.toUserMessage()) },
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
                .onFailure {
                    _state.value = if (it.isUserCancellation()) AuthUiState() else AuthUiState(error = it.toUserMessage())
                }
        }
    }
}

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

data class CoursesUiState(
    val loading: Boolean = true,
    val courses: List<Course> = emptyList(),
    val offline: Boolean = false,
    val error: String? = null,
)

enum class CourseTab(val label: String) { ALL("All"), POPULAR("Popular"), NEW("New") }

enum class DurationRange(val label: String, val minMinutes: Int, val maxMinutes: Int) {
    UNDER_1("0-1 Hours", 0, 60),
    FROM_1_TO_3("1-3 Hours", 60, 180),
    FROM_3_TO_8("3-8 Hours", 180, 480),
    FROM_8_TO_16("8-16 Hours", 480, 960),
    OVER_16("16+ Hours", 960, Int.MAX_VALUE),
}

/** Search filter from the "Search Filter" sheet. [maxPrice] null means no upper limit. */
data class CourseFilter(
    val categories: Set<String> = emptySet(),
    val minPrice: Float = 0f,
    val maxPrice: Float? = null,
    val durations: Set<DurationRange> = emptySet(),
) {
    val isActive: Boolean
        get() = categories.isNotEmpty() || minPrice > 0f || maxPrice != null || durations.isNotEmpty()

    fun matches(course: Course): Boolean =
        (categories.isEmpty() || course.category in categories) &&
            course.price >= minPrice &&
            (maxPrice == null || course.price <= maxPrice) &&
            (durations.isEmpty() || durations.any { course.durationMinutes >= it.minMinutes && course.durationMinutes < it.maxMinutes })
}

/** Course catalog loaded through Retrofit, with tabs, search and the filter sheet. */
class CoursesViewModel(private val repository: CourseRepository) : ViewModel() {

    private val _state = MutableStateFlow(CoursesUiState())
    val state: StateFlow<CoursesUiState> = _state.asStateFlow()

    val query = MutableStateFlow("")
    val filter = MutableStateFlow(CourseFilter())
    val tab = MutableStateFlow(CourseTab.ALL)

    /** Course tab: filter + All/Popular/New. */
    val catalog: StateFlow<List<Course>> = combine(_state, filter, tab) { state, f, t ->
        state.courses.filter { course ->
            f.matches(course) && when (t) {
                CourseTab.ALL -> true
                CourseTab.POPULAR -> course.isPopular
                CourseTab.NEW -> course.isNew
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Search tab: text query + filter. */
    val searchResults: StateFlow<List<Course>> = combine(_state, query, filter) { state, q, f ->
        state.courses.filter { course ->
            f.matches(course) && (q.isBlank() || course.title.contains(q.trim(), ignoreCase = true) ||
                course.author.contains(q.trim(), ignoreCase = true) || course.category.contains(q.trim(), ignoreCase = true))
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

    /** Category card on the Course tab: selects only this category, or clears it when tapped again. */
    fun toggleCategory(category: String) = filter.update {
        it.copy(categories = if (it.categories == setOf(category)) emptySet() else setOf(category))
    }

    fun clearSearch() {
        query.value = ""
        filter.value = CourseFilter()
    }

    fun course(id: String): Course? = _state.value.courses.firstOrNull { it.id == id }
}

/** Light / dark / system theme switch, persisted in [ThemePreferences]. */
class ThemeViewModel(private val prefs: ThemePreferences) : ViewModel() {
    val mode: StateFlow<ThemeMode> = prefs.mode

    fun set(mode: ThemeMode) = prefs.set(mode)

    /** Quick toggle: flips whatever is on screen now to the opposite theme. */
    fun toggle(currentlyDark: Boolean) = prefs.set(if (currentlyDark) ThemeMode.LIGHT else ThemeMode.DARK)
}
