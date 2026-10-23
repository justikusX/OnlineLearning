package com.example.onlinelearning.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onlinelearning.model.entity.Course
import com.example.onlinelearning.model.repository.CatalogState
import com.example.onlinelearning.model.repository.CourseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

/** Course and Search tabs: the catalog with All / Popular / New tabs, text search and the filter sheet. */
class CoursesViewModel(private val repository: CourseRepository) : ViewModel() {

    val state: StateFlow<CatalogState> = repository.catalog

    val query = MutableStateFlow("")
    val filter = MutableStateFlow(CourseFilter())
    val tab = MutableStateFlow(CourseTab.ALL)

    /** Categories for the category cards and the filter sheet, in catalog order. */
    val categories: StateFlow<List<String>> = state.map { s -> s.courses.map { it.category }.distinct() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Cover of the first course in each category, shown on the category cards. */
    val categoryCovers: StateFlow<Map<String, String>> = state.map { s -> s.courses.reversed().associate { it.category to it.imageUrl } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    /** Upper end of the price slider. */
    val maxPrice: StateFlow<Float> = state.map { s -> s.courses.maxOfOrNull { it.price }?.toFloat() ?: 0f }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0f)

    /** Quick search chips under the search field. */
    val suggestions: StateFlow<List<String>> = categories.map { (it + listOf("Kotlin", "Java", "Color", "History")).distinct() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Course tab: filter + All/Popular/New. */
    val catalog: StateFlow<List<Course>> = combine(state, filter, tab) { state, f, t ->
        state.courses.filter { course ->
            f.matches(course) && when (t) {
                CourseTab.ALL -> true
                CourseTab.POPULAR -> course.isPopular
                CourseTab.NEW -> course.isNew
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Search tab: text query + filter. */
    val searchResults: StateFlow<List<Course>> = combine(state, query, filter) { state, q, f ->
        state.courses.filter { course ->
            f.matches(course) && (q.isBlank() || course.title.contains(q.trim(), ignoreCase = true) ||
                course.author.contains(q.trim(), ignoreCase = true) || course.category.contains(q.trim(), ignoreCase = true))
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch { repository.load() }
    }

    fun refresh(force: Boolean = false) {
        viewModelScope.launch { repository.refresh(force) }
    }

    fun selectTab(value: CourseTab) {
        tab.value = value
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun applyFilter(value: CourseFilter) {
        filter.value = value
    }

    /** Category card on the Course tab: selects only this category, or clears it when tapped again. */
    fun toggleCategory(category: String) = filter.update {
        it.copy(categories = if (it.categories == setOf(category)) emptySet() else setOf(category))
    }

    fun clearSearch() {
        query.value = ""
        filter.value = CourseFilter()
    }

    /** "Show all" on the empty Course tab. */
    fun showAll() {
        clearSearch()
        tab.value = CourseTab.ALL
    }
}
