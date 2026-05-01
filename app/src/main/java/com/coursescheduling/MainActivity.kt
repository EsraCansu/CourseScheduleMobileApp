package com.coursescheduling

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import com.coursescheduling.theme.CourseScheduleTheme
import com.coursescheduling.data.AuthManager
import com.coursescheduling.data.ThemeMode
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val authManager = remember { AuthManager.getInstance(context) }
            val themeMode by authManager.themeModeFlow.collectAsState(initial = ThemeMode.SYSTEM)

            val isDarkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            CourseScheduleTheme(darkTheme = isDarkTheme) {
                com.coursescheduling.navigation.CourseScheduleApp()
            }
        }
    }
}
