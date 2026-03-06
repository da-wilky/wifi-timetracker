package com.wifitracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.wifitracker.R
import com.wifitracker.ui.detail.EventLogScreen
import com.wifitracker.ui.home.HomeScreen
import com.wifitracker.ui.trackers.TrackersScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Trackers : Screen("trackers")
    data object EventLog : Screen("eventLog/{trackerId}") {
        fun createRoute(trackerId: Long) = "eventLog/$trackerId"
    }
}

@Composable
fun BottomNavigationBar(
    navController: NavHostController
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text(stringResource(R.string.tab_home)) },
            selected = currentDestination?.hierarchy?.any { it.route == Screen.Home.route } == true,
            onClick = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(navController.graph.startDestinationId) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )

        NavigationBarItem(
            icon = { Icon(Icons.Default.List, contentDescription = null) },
            label = { Text(stringResource(R.string.tab_trackers)) },
            selected = currentDestination?.hierarchy?.any { it.route == Screen.Trackers.route } == true,
            onClick = {
                navController.navigate(Screen.Trackers.route) {
                    popUpTo(navController.graph.startDestinationId) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToTrackers = {
                    navController.navigate(Screen.Trackers.route)
                },
                onNavigateToEventLog = { trackerId ->
                    navController.navigate(Screen.EventLog.createRoute(trackerId))
                }
            )
        }

        composable(Screen.Trackers.route) {
            TrackersScreen(
                onNavigateToEventLog = { trackerId ->
                    navController.navigate(Screen.EventLog.createRoute(trackerId))
                }
            )
        }

        composable(
            route = Screen.EventLog.route,
            arguments = listOf(
                navArgument("trackerId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val trackerId = backStackEntry.arguments?.getLong("trackerId") ?: 0L
            EventLogScreen(
                trackerId = trackerId,
                onNavigateBack = { navController.navigateUp() }
            )
        }
    }
}
