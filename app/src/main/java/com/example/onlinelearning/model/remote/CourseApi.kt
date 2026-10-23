package com.example.onlinelearning.model.remote

import com.example.onlinelearning.BuildConfig
import com.example.onlinelearning.model.entity.Course
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

interface CourseApi {
    @GET("courses.json")
    suspend fun getCourses(): CoursesResponse
}

data class CoursesResponse(val courses: List<Course> = emptyList())

object NetworkModule {
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG) {
                        HttpLoggingInterceptor.Level.BASIC
                    } else {
                        HttpLoggingInterceptor.Level.NONE
                    }
                }
            )
            .build()
    }

    val courseApi: CourseApi by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.COURSES_API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CourseApi::class.java)
    }
}
