package com.example.onlinelearning.viewmodel

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onlinelearning.model.entity.Course
import com.example.onlinelearning.model.entity.Lesson
import com.example.onlinelearning.model.entity.LessonKind
import com.example.onlinelearning.model.player.LessonPlayback
import com.example.onlinelearning.model.repository.CourseRepository
import com.example.onlinelearning.model.repository.SessionRepository
import com.example.onlinelearning.model.repository.UserRepository
import com.example.onlinelearning.model.util.toUserMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class CourseDetailUiState(
    val loading: Boolean = true,
    val course: Course? = null,
    val purchased: Boolean = false,
    val favorite: Boolean = false,
    /** Ids of finished lessons. */
    val completed: Set<String> = emptySet(),
    /** Lesson open in the player, or null. */
    val playing: Lesson? = null,
) {
    /** Free courses, bought courses and free lessons can be played. */
    fun isUnlocked(lesson: Lesson): Boolean = purchased || lesson.free

    val nextLesson: Lesson?
        get() = playing?.let { current -> course?.lessons?.dropWhile { it.id != current.id }?.drop(1)?.firstOrNull() }
}

enum class PayStep { METHOD, PASSWORD, SUCCESS }

/** Demo checkout: "Payment Method" → "Payment Password" → "Successful purchase!". */
data class PaymentState(
    val step: PayStep = PayStep.METHOD,
    val pin: String = "",
    val processing: Boolean = false,
)

/**
 * Course screen: course info, lessons, the lesson player, favourites and the demo purchase.
 * The ViewModel owns the player, so playback survives screen rotation and stops when the user leaves the course.
 */
class CourseDetailViewModel(
    savedState: SavedStateHandle,
    private val app: Application,
    private val courses: CourseRepository,
    private val session: SessionRepository,
    private val users: UserRepository,
) : ViewModel() {

    private val courseId: String = savedState.get<String>("id").orEmpty()

    private val playing = MutableStateFlow<Lesson?>(null)

    val state: StateFlow<CourseDetailUiState> =
        combine(courses.catalog, session.profile, playing) { catalog, profile, lesson ->
            val course = catalog.courses.firstOrNull { it.id == courseId }
            CourseDetailUiState(
                loading = catalog.loading,
                course = course,
                purchased = course != null && (course.price == 0.0 || profile?.purchased?.contains(course.id) == true),
                favorite = profile?.favorites?.contains(courseId) == true,
                completed = profile?.progress?.get(courseId).orEmpty().toSet(),
                playing = lesson,
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, CourseDetailUiState())

    private val _playback = MutableStateFlow<LessonPlayback?>(null)

    /** Player of the [CourseDetailUiState.playing] lesson; its fields are observable by Compose. */
    val playback: StateFlow<LessonPlayback?> = _playback.asStateFlow()

    private val _speed = MutableStateFlow(SPEEDS.first())
    val speed: StateFlow<Float> = _speed.asStateFlow()

    private val _payment = MutableStateFlow<PaymentState?>(null)

    /** Open checkout, or null when it is closed. */
    val payment: StateFlow<PaymentState?> = _payment.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val _clockIn = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Fires after the first lesson of the day is finished, to show the "Clocking in!" card. */
    val clockIn: SharedFlow<Unit> = _clockIn.asSharedFlow()

    private var progressJob: Job? = null

    init {
        viewModelScope.launch { courses.load() }
    }

    /** Tap on a lesson row: pauses or resumes the current lesson, otherwise starts the tapped one. */
    fun onLessonClick(lesson: Lesson) {
        val pb = _playback.value
        if (playing.value?.id == lesson.id && pb != null) {
            if (pb.isPlaying) pb.pause() else pb.play()
        } else {
            play(lesson)
        }
    }

    fun play(lesson: Lesson) {
        val s = state.value
        when {
            !s.isUnlocked(lesson) -> _messages.tryEmit("Купите курс, чтобы открыть этот урок")
            lesson.kind == LessonKind.NONE -> _messages.tryEmit("У этого урока пока нет записи")
            else -> start(lesson)
        }
    }

    /** "Start learning": the first lesson that is not finished yet. */
    fun startLearning() {
        val s = state.value
        s.course?.lessons?.firstOrNull { it.id !in s.completed }?.let(::play)
    }

    fun playNext() {
        state.value.nextLesson?.let(::play)
    }

    fun stopPlayback() {
        progressJob?.cancel()
        _playback.value?.release()
        _playback.value = null
        playing.value = null
    }

    /** Speed button in the player: 1x → 1.25x → 1.5x → 2x → 1x. */
    fun cycleSpeed() {
        val next = SPEEDS[(SPEEDS.indexOf(_speed.value) + 1) % SPEEDS.size]
        _speed.value = next
        _playback.value?.setSpeed(next)
    }

    private fun start(lesson: Lesson) {
        stopPlayback()
        val course = state.value.course ?: return
        val pb = LessonPlayback.create(app, lesson).also { it.setSpeed(_speed.value) }
        _playback.value = pb
        playing.value = lesson
        // Refresh the position twice a second and record the lesson once 90% of it has been played.
        progressJob = viewModelScope.launch {
            var reported = false
            while (isActive) {
                pb.tick()
                val d = pb.durationMs
                if (!reported && (pb.ended || (d > 0 && pb.positionMs >= d * COMPLETE_AT))) {
                    reported = true
                    recordLesson(course, lesson)
                }
                delay(500)
            }
        }
    }

    private suspend fun recordLesson(course: Course, lesson: Lesson) {
        val uid = session.uid
        if (uid == null) {
            _messages.tryEmit("Войдите в аккаунт, чтобы сохранять прогресс")
            return
        }
        runCatching { users.recordLesson(uid, course, lesson, session.profile.value) }
            .onSuccess { firstToday -> if (firstToday) _clockIn.tryEmit(Unit) }
            .onFailure { _messages.tryEmit(it.toUserMessage()) }
    }

    fun toggleFavorite() {
        val uid = session.uid
        if (uid == null) {
            _messages.tryEmit("Войдите в аккаунт, чтобы добавлять курсы в избранное")
            return
        }
        val favorite = state.value.favorite
        viewModelScope.launch {
            runCatching { users.setFavorite(uid, courseId, !favorite) }
                .onFailure { _messages.tryEmit(it.toUserMessage()) }
        }
    }

    // --- Demo checkout ---

    fun buy() {
        if (session.uid == null) _messages.tryEmit("Войдите в аккаунт, чтобы купить курс")
        else _payment.value = PaymentState()
    }

    fun payNow() = _payment.update { it?.copy(step = PayStep.PASSWORD) }

    fun onPinDigit(digit: Char) {
        val p = _payment.value ?: return
        if (p.processing || p.pin.length >= PIN_LENGTH) return
        val pin = p.pin + digit
        _payment.value = p.copy(pin = pin, processing = pin.length == PIN_LENGTH)
        if (pin.length == PIN_LENGTH) confirmPurchase()
    }

    fun onPinBackspace() = _payment.update { p ->
        if (p == null || p.processing) p else p.copy(pin = p.pin.dropLast(1))
    }

    fun closePayment() {
        _payment.value = null
    }

    /** "Start learning" on the success screen. */
    fun finishPurchase() {
        _payment.value = null
        // The course is paid for, so start right away even if the profile update has not arrived yet.
        val s = state.value
        s.course?.lessons?.firstOrNull { it.id !in s.completed && it.kind != LessonKind.NONE }?.let(::start)
    }

    private fun confirmPurchase() {
        val uid = session.uid
        val course = state.value.course
        if (uid == null || course == null) {
            _payment.value = null
            return
        }
        viewModelScope.launch {
            runCatching { users.purchase(uid, course.id, course.title) }
                .onSuccess { _payment.update { it?.copy(step = PayStep.SUCCESS, processing = false) } }
                .onFailure { e ->
                    _payment.update { it?.copy(pin = "", processing = false) }
                    _messages.tryEmit(e.toUserMessage())
                }
        }
    }

    override fun onCleared() {
        stopPlayback()
    }

    companion object {
        /** A lesson counts as done once this share of it has been played. */
        const val COMPLETE_AT = 0.9f
        const val PIN_LENGTH = 6
        val SPEEDS = listOf(1f, 1.25f, 1.5f, 2f)
    }
}
