package com.example.onlinelearning.viewmodel

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.onlinelearning.AppContainer
import com.example.onlinelearning.LearningApp

/**
 * Creates every ViewModel of the app and hands it the repositories (the Model) it needs.
 * Views get ViewModels with `viewModel(factory = AppViewModelFactory.Factory)`.
 */
object AppViewModelFactory {
    val Factory = viewModelFactory {
        initializer { SessionViewModel(container().session, container().auth) }
        initializer { AuthViewModel(container().auth, container().users) }
        initializer { PhoneAuthViewModel(container().auth) }
        initializer { CoursesViewModel(container().courses) }
        initializer { HomeViewModel(container().courses, container().session) }
        initializer {
            CourseDetailViewModel(createSavedStateHandle(), app(), container().courses, container().session, container().users)
        }
        initializer { MessagesViewModel(container().session) }
        initializer { AccountViewModel(app(), container().session, container().users, container().auth) }
        initializer { ThemeViewModel(container().theme) }
    }

    private fun CreationExtras.app(): LearningApp = this[APPLICATION_KEY] as LearningApp

    private fun CreationExtras.container(): AppContainer = app().container
}
