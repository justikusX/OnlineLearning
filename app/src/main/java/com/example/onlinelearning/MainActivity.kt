package com.example.onlinelearning

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.onlinelearning.view.AppNavigation
import com.example.onlinelearning.view.theme.OnlineLearningTheme
import com.example.onlinelearning.view.theme.isDark
import com.example.onlinelearning.viewmodel.AppViewModelFactory
import com.example.onlinelearning.viewmodel.ThemeViewModel

class MainActivity : ComponentActivity() {

    // Same instance as viewModel<ThemeViewModel>() inside AppNavigation: both use the activity's ViewModelStore.
    private val themeViewModel: ThemeViewModel by viewModels { AppViewModelFactory.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val mode by themeViewModel.mode.collectAsStateWithLifecycle()
            val dark = mode.isDark()
            // Status/navigation bar icons follow the app theme, not only the system setting.
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { dark },
                )
                onDispose {}
            }
            OnlineLearningTheme(darkTheme = dark) {
                AppNavigation()
            }
        }
    }

    private companion object {
        // Same scrims as the androidx.activity defaults.
        val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
