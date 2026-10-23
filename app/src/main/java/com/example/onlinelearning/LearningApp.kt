package com.example.onlinelearning

import android.app.Application
import com.example.onlinelearning.model.remote.NetworkModule
import com.example.onlinelearning.model.repository.AuthRepository
import com.example.onlinelearning.model.repository.CourseRepository
import com.example.onlinelearning.model.repository.SessionRepository
import com.example.onlinelearning.model.repository.ThemePreferences
import com.example.onlinelearning.model.repository.UserRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class LearningApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // initializeApp returns null when google-services.json was not added to the build.
        val firebase = FirebaseApp.initializeApp(this)
        val auth = AuthRepository(firebase?.let { FirebaseAuth.getInstance(it) })
        val users = UserRepository(firebase?.let { FirebaseFirestore.getInstance(it) })
        container = AppContainer(
            courses = CourseRepository(this, NetworkModule.courseApi),
            auth = auth,
            users = users,
            session = SessionRepository(auth, users, CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)),
            theme = ThemePreferences(this),
        )
    }
}

/**
 * Model layer of the app (MVVM "M"): repositories created once per process.
 * ViewModels get them from here through AppViewModelFactory; a DI framework is not needed for a project this size.
 */
class AppContainer(
    val courses: CourseRepository,
    val auth: AuthRepository,
    val users: UserRepository,
    val session: SessionRepository,
    val theme: ThemePreferences,
)
