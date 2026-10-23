package com.example.onlinelearning.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onlinelearning.model.entity.Course
import com.example.onlinelearning.model.repository.CourseRepository
import com.example.onlinelearning.model.repository.SessionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** A course the user owns or has started, with the number of finished lessons. */
data class CourseProgress(val course: Course, val done: Int) {
    val total: Int get() = course.lessons.size
}

data class HomeUiState(
    val loading: Boolean = true,
    val name: String = "Guest",
    val avatar: String = "",
    val todayMinutes: Int = 0,
    val goalMinutes: Int = DAILY_GOAL_MINUTES,
    /** "Learning Plan" on Home and the grid on My courses. */
    val myCourses: List<CourseProgress> = emptyList(),
    val popular: List<Course> = emptyList(),
    val favourites: List<Course> = emptyList(),
)

const val DAILY_GOAL_MINUTES = 60

/** Home, My courses and Favourite screens: everything is built from the catalog and the user's profile. */
class HomeViewModel(
    private val courses: CourseRepository,
    session: SessionRepository,
) : ViewModel() {

    val state: StateFlow<HomeUiState> = combine(courses.catalog, session.user, session.profile) { catalog, user, profile ->
        val all = catalog.courses
        HomeUiState(
            loading = catalog.loading,
            name = SessionRepository.displayName(profile, user),
            avatar = profile?.avatar.orEmpty(),
            todayMinutes = profile?.minutesOn(LocalDate.now()) ?: 0,
            // Purchased courses plus any course with finished lessons.
            myCourses = all
                .filter { profile?.purchased?.contains(it.id) == true || !profile?.progress?.get(it.id).isNullOrEmpty() }
                .map { CourseProgress(it, profile?.progress?.get(it.id)?.size ?: 0) },
            popular = all.filter { it.isPopular }.take(4),
            favourites = all.filter { profile?.favorites?.contains(it.id) == true },
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState())

    init {
        viewModelScope.launch { courses.load() }
    }
}
