package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.navigation.MainScreen
import com.example.ui.theme.MovementTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = applicationContext as MovementApp

        setContent {
            val themeSetting by app.appPreferences.themeFlow.collectAsStateWithLifecycle("SYSTEM")
            val isDark = when (themeSetting) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            MovementTheme(darkTheme = isDark) {
                MainScreen()
            }
        }
    }
}
