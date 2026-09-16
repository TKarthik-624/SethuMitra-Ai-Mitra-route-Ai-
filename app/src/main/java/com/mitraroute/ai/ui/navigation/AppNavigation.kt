package com.mitraroute.ai.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.mitraroute.ai.R
import com.mitraroute.ai.ui.screens.ai.AIScreen
import com.mitraroute.ai.ui.screens.command.CommandDashboardScreen
import com.mitraroute.ai.ui.screens.dashboard.DashboardScreen
import com.mitraroute.ai.ui.screens.map.MapScreen
import com.mitraroute.ai.ui.screens.planner.RoutePlannerScreen
import com.mitraroute.ai.ui.screens.report.ReportScreen
import com.mitraroute.ai.ui.screens.settings.SettingsScreen
import com.mitraroute.ai.ui.screens.weather.WeatherScreen
import com.mitraroute.ai.ui.screens.hospitals.HospitalsScreen

sealed class Screen(val route: String, val titleRes: Int, val icon: ImageVector) {
    data object CommandDashboard : Screen("command", R.string.nav_command_dashboard, Icons.Default.Home)
    data object Planner : Screen("planner", R.string.nav_planner, Icons.AutoMirrored.Filled.AltRoute)
    data object Map : Screen("map", R.string.nav_map, Icons.Default.Map)
    data object Weather : Screen("weather", R.string.nav_weather, Icons.Default.Cloud)
    data object Hospitals : Screen("hospitals", R.string.nav_hospitals, Icons.Default.LocalHospital)
    data object Report : Screen("report", R.string.nav_report, Icons.Default.Warning)
    data object Dashboard : Screen("dashboard", R.string.nav_dashboard, Icons.Default.BarChart)
    data object AIIntelligence : Screen("ai", R.string.nav_ai, Icons.Default.AutoAwesome)
    data object Settings : Screen("settings", R.string.nav_settings, Icons.Default.Settings)
}

val bottomNavItems = listOf(
    Screen.CommandDashboard,
    Screen.Planner,
    Screen.Weather,
    Screen.Report,
    Screen.Settings
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, null) },
                        label = { Text(stringResource(screen.titleRes)) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            if (currentDestination?.route != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.CommandDashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.CommandDashboard.route) { 
                CommandDashboardScreen(onNavigate = { route ->
                    navController.navigate(route)
                }) 
            }
            composable(Screen.Planner.route) { 
                RoutePlannerScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToAI = { navController.navigate(Screen.AIIntelligence.route) }
                ) 
            }
            composable(Screen.Map.route) { 
                MapScreen(onNavigateToPlanner = {
                    navController.navigate(Screen.Planner.route)
                }) 
            }
            composable(Screen.Report.route) { 
                ReportScreen(onBack = { navController.popBackStack() }) 
            }
            composable(Screen.Weather.route) { 
                WeatherScreen(onBack = { navController.popBackStack() }) 
            }
            composable(Screen.Hospitals.route) {
                HospitalsScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Dashboard.route) { DashboardScreen() }
            composable(Screen.AIIntelligence.route) { 
                AIScreen(onBack = { navController.popBackStack() }) 
            }
            composable(Screen.Settings.route) { 
                SettingsScreen() 
            }
        }
    }
}
