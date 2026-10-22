package com.example.onlinelearning.data

import android.content.Context
import android.util.Log
import com.example.onlinelearning.data.model.Course
import com.example.onlinelearning.data.remote.CourseApi
import com.example.onlinelearning.data.remote.CoursesResponse
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Loads the course catalog through Retrofit. If the API is unreachable (no network,
 * base URL not configured yet) it falls back to the copy bundled in assets/courses.json,
 * so the app is usable before the backend is deployed.
 */
class CourseRepository(
    private val context: Context,
    private val api: CourseApi,
) {
    private val mutex = Mutex()
    private var cache: List<Course>? = null

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

    suspend fun getCourse(id: String): Course? = getCourses().firstOrNull { it.id == id }

    private suspend fun loadBundled(): List<Course> = withContext(Dispatchers.IO) {
        context.assets.open("courses.json").bufferedReader().use {
            Gson().fromJson(it, CoursesResponse::class.java).courses
        }
    }

    private companion object {
        const val TAG = "CourseRepository"
    }
}
