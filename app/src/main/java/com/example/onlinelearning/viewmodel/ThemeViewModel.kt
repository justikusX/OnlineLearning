package com.example.onlinelearning.viewmodel

import androidx.lifecycle.ViewModel
import com.example.onlinelearning.model.repository.ThemeMode
import com.example.onlinelearning.model.repository.ThemePreferences
import kotlinx.coroutines.flow.StateFlow

/** Light / dark / system theme switch, persisted in [ThemePreferences]. */
class ThemeViewModel(private val prefs: ThemePreferences) : ViewModel() {
    val mode: StateFlow<ThemeMode> = prefs.mode

    fun set(mode: ThemeMode) = prefs.set(mode)

    /** Quick toggle: flips whatever is on screen now to the opposite theme. */
    fun toggle(currentlyDark: Boolean) = prefs.set(if (currentlyDark) ThemeMode.LIGHT else ThemeMode.DARK)
}
