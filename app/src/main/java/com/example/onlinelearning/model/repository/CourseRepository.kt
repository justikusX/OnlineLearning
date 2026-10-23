package com.example.onlinelearning.model.repository

import android.content.Context
import android.util.Log
import com.example.onlinelearning.model.entity.Course
import com.example.onlinelearning.model.remote.CourseApi
import com.example.onlinelearning.model.remote.CoursesResponse
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** The course catalog as every screen sees it. */
data class CatalogState(
    val loading: Boolean = true,
    val courses: List<Course> = emptyList(),
    /** True when the courses come from the copy bundled in the app because the API was unreachable. */
    val offline: Boolean = false,
    val error: String? = null,
)

/**
 * Loads the course catalog through Retrofit. If the API is unreachable (no network,
 * base URL not configured yet) it falls back to the copy bundled in assets/courses.json,
 * so the app is usable before the backend is deployed.
 *
 * The result is published in [catalog], so all ViewModels share one copy of the catalog.
 */
class CourseRepository(
    private val context: Context,
    private val api: CourseApi,
) {
    private val mutex = Mutex()
    private var cache: List<Course>? = null

    private val _catalog = MutableStateFlow(CatalogState())
    val catalog: StateFlow<CatalogState> = _catalog.asStateFlow()

    /** Loads the catalog once; later calls return immediately. */
    suspend fun load() {
        if (_catalog.value.courses.isEmpty()) refresh()
    }

    /** Reloads the catalog ([force] skips the in-memory cache) and publishes it in [catalog]. */
    suspend fun refresh(force: Boolean = false) {
        _catalog.update { it.copy(loading = true, error = null) }
        _catalog.value = runCatching { getCourses(force) }.fold(
            onSuccess = { CatalogState(loading = false, courses = it, offline = isOffline) },
            onFailure = { CatalogState(loading = false, error = it.message) },
        )
    }

    /** True when the last load came from the bundled assets instead of the API. */
    var isOffline: Boolean = false
        private set

    suspend fun getCourses(forceRefresh: Boolean = false): List<Course> = mutex.withLock {
        cache?.takeIf { !forceRefresh }?.let { return it }
        val courses = try {
            api.getCourses().courses.also { isOffline = false }
        } catch (e: Exception) {
            Log.w(TAG, "Course API unavailable, using bundled catalog", e)
            isOffline = true
            loadBundled()
        }
        cache = courses
        courses
    }

    private suspend fun loadBundled(): List<Course> = withContext(Dispatchers.IO) {
        context.assets.open("courses.json").bufferedReader().use {
            Gson().fromJson(it, CoursesResponse::class.java).courses
        }
    }

    private companion object {
        const val TAG = "CourseRepository"
    }
}
