package com.example.onlinelearning

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.onlinelearning.ui.AppNavigation
import com.example.onlinelearning.ui.theme.OnlineLearningTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OnlineLearningTheme {
                AppNavigation()
            }
        }
    }
}
