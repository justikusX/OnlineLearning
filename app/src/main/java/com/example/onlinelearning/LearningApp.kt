package com.example.onlinelearning

import android.app.Application
import com.example.onlinelearning.data.AuthRepository
import com.example.onlinelearning.data.CourseRepository
import com.example.onlinelearning.data.UserRepository
import com.example.onlinelearning.data.remote.NetworkModule
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LearningApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // initializeApp returns null when google-services.json was not added to the build.
        val firebase = FirebaseApp.initializeApp(this)
        container = AppContainer(
            courses = CourseRepository(this, NetworkModule.courseApi),
            auth = AuthRepository(firebase?.let { FirebaseAuth.getInstance(it) }),
            users = UserRepository(firebase?.let { FirebaseFirestore.getInstance(it) }),
        )
    }
}

/** Manual dependency container; small enough that a DI framework is not worth it. */
class AppContainer(
    val courses: CourseRepository,
    val auth: AuthRepository,
    val users: UserRepository,
)
