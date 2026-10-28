package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Entry : Screen("entry", "Entry", Icons.Default.PostAdd)
    object History : Screen("history", "History", Icons.Default.History)
    object Persons : Screen("persons", "Persons", Icons.Default.People)
    object Reports : Screen("reports", "Reports", Icons.Default.Assessment)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Entry,
    Screen.History,
    Screen.Persons,
    Screen.Reports
)
