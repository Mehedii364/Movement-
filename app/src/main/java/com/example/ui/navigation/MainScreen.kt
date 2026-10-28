package com.example.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.MovementApp
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.dashboard.DashboardViewModel
import com.example.ui.movement.MovementEntryScreen
import com.example.ui.movement.MovementEntryViewModel
import com.example.ui.movement.MovementHistoryScreen
import com.example.ui.movement.MovementHistoryViewModel
import com.example.ui.persons.PersonsScreen
import com.example.ui.persons.PersonsViewModel
import com.example.ui.reports.ReportsScreen
import com.example.ui.reports.ReportsViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as MovementApp
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Dashboard.route

    val userSession by app.appPreferences.currentUserFlow.collectAsStateWithLifecycle(null)

    // ViewModels with factories
    val dashboardViewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.Factory(app.movementRepository)
    )
    val movementEntryViewModel: MovementEntryViewModel = viewModel(
        factory = MovementEntryViewModel.Factory(app.movementRepository, app.appPreferences)
    )
    val movementHistoryViewModel: MovementHistoryViewModel = viewModel(
        factory = MovementHistoryViewModel.Factory(app.movementRepository, app.appPreferences)
    )
    val personsViewModel: PersonsViewModel = viewModel(
        factory = PersonsViewModel.Factory(app.movementRepository, context)
    )
    val reportsViewModel: ReportsViewModel = viewModel(
        factory = ReportsViewModel.Factory(
            app.movementRepository,
            app.pdfExportManager,
            app.excelExportManager,
            app.imageExportManager
        )
    )
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(app.appPreferences, app.userRepository, app.syncRepository)
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "M",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Movement",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Text(
                                text = "Digital Movement Register",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    userSession?.let { session ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = session.role,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            if (currentRoute != Screen.Settings.route) {
                                navController.navigate(Screen.Settings.route)
                            }
                        },
                        modifier = Modifier.testTag("nav_settings_button")
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = if (currentRoute == Screen.Settings.route) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("main_bottom_nav")
            ) {
                bottomNavItems.forEach { screen ->
                    val selected = currentRoute == screen.route
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        selected = selected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onNavigateToOutEntry = {
                        movementEntryViewModel.setMovementType("OUT")
                        navController.navigate(Screen.Entry.route)
                    },
                    onNavigateToInEntry = {
                        movementEntryViewModel.setMovementType("IN")
                        navController.navigate(Screen.Entry.route)
                    },
                    onNavigateToAddPerson = {
                        navController.navigate(Screen.Persons.route)
                    },
                    onNavigateToReports = {
                        navController.navigate(Screen.Reports.route)
                    },
                    onNavigateToPersonDetail = { personId ->
                        val person = personsViewModel.filteredPersons.value.find { it.id == personId }
                        if (person != null) {
                            personsViewModel.selectPersonForDetail(person)
                            navController.navigate(Screen.Persons.route)
                        }
                    }
                )
            }

            composable(Screen.Entry.route) {
                MovementEntryScreen(viewModel = movementEntryViewModel)
            }

            composable(Screen.History.route) {
                MovementHistoryScreen(
                    viewModel = movementHistoryViewModel,
                    onNavigateToPersonDetail = { personId ->
                        val person = personsViewModel.filteredPersons.value.find { it.id == personId }
                        if (person != null) {
                            personsViewModel.selectPersonForDetail(person)
                            navController.navigate(Screen.Persons.route)
                        }
                    }
                )
            }

            composable(Screen.Persons.route) {
                PersonsScreen(
                    viewModel = personsViewModel,
                    onNavigateToRecordMovementForPerson = { personId ->
                        val person = personsViewModel.filteredPersons.value.find { it.id == personId }
                        if (person != null) {
                            movementEntryViewModel.selectPerson(person)
                            navController.navigate(Screen.Entry.route)
                        }
                    }
                )
            }

            composable(Screen.Reports.route) {
                ReportsScreen(viewModel = reportsViewModel)
            }

            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = settingsViewModel)
            }
        }
    }
}
