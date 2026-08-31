package com.example.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.MainViewModel
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.AuthUiState
import com.example.ui.auth.AuthViewModel
import com.example.ui.automation.AutomationHubScreen
import com.example.ui.automation.ColabHubScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.tasks.TasksScreen
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.CyberSurfaceDark
import com.example.ui.theme.EmeraldSuccess

const val ROUTE_AUTH = "auth"
const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_TASKS = "tasks"
const val ROUTE_AUTOMATION = "automation"
const val ROUTE_COLAB = "colab"

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Dashboard : Screen(ROUTE_DASHBOARD, "الرئيسية", Icons.Filled.Dashboard, Icons.Outlined.Dashboard)
    object Tasks : Screen(ROUTE_TASKS, "المهام", Icons.Filled.CheckCircleOutline, Icons.Outlined.CheckCircleOutline)
    object Automation : Screen(ROUTE_AUTOMATION, "الأتمتة", Icons.Filled.Bolt, Icons.Outlined.Bolt)
    object Colab : Screen(ROUTE_COLAB, "كولاب", Icons.Filled.Code, Icons.Outlined.Code)
}

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    mainViewModel: MainViewModel,
    navController: NavHostController = rememberNavController()
) {
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val todayTasks by mainViewModel.todayTasks.collectAsStateWithLifecycle()
    val activeScripts by mainViewModel.allScripts.collectAsStateWithLifecycle()

    val pendingTodayTasksCount = todayTasks.count { !it.isCompleted }
    val activeScriptsCount = activeScripts.count { it.isActive }

    val bottomNavItems = listOf(
        Screen.Dashboard,
        Screen.Tasks,
        Screen.Automation,
        Screen.Colab
    )

    val startDestination = if (authState is AuthUiState.Authenticated) {
        ROUTE_DASHBOARD
    } else {
        ROUTE_AUTH
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentRoute != ROUTE_AUTH && authState is AuthUiState.Authenticated) {
                NavigationBar(
                    containerColor = CyberSurfaceDark,
                    contentColor = Color.White,
                    tonalElevation = 8.dp,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    bottomNavItems.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = isSelected,
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
                            icon = {
                                if (screen == Screen.Tasks && pendingTodayTasksCount > 0) {
                                    BadgedBox(
                                        badge = {
                                            Badge(
                                                containerColor = CyanPrimary,
                                                contentColor = Color.Black
                                            ) {
                                                Text("$pendingTodayTasksCount")
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                            contentDescription = screen.title
                                        )
                                    }
                                } else if (screen == Screen.Automation && activeScriptsCount > 0) {
                                    BadgedBox(
                                        badge = {
                                            Badge(
                                                containerColor = EmeraldSuccess,
                                                contentColor = Color.Black
                                            ) {
                                                Text("$activeScriptsCount")
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                            contentDescription = screen.title
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                        contentDescription = screen.title
                                    )
                                }
                            },
                            label = { Text(screen.title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                selectedTextColor = CyanPrimary,
                                indicatorColor = CyanPrimary,
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray
                            ),
                            modifier = Modifier.testTag("nav_tab_${screen.route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(ROUTE_AUTH) {
                AuthScreen(
                    authViewModel = authViewModel,
                    onAuthSuccess = {
                        navController.navigate(ROUTE_DASHBOARD) {
                            popUpTo(ROUTE_AUTH) { inclusive = true }
                        }
                    }
                )
            }

            composable(ROUTE_DASHBOARD) {
                DashboardScreen(
                    mainViewModel = mainViewModel,
                    onNavigateToTasks = {
                        navController.navigate(ROUTE_TASKS)
                    },
                    onNavigateToAutomation = {
                        navController.navigate(ROUTE_AUTOMATION)
                    },
                    onSignOut = {
                        authViewModel.signOut()
                        navController.navigate(ROUTE_AUTH) {
                            popUpTo(ROUTE_DASHBOARD) { inclusive = true }
                        }
                    }
                )
            }

            composable(ROUTE_TASKS) {
                TasksScreen(mainViewModel = mainViewModel)
            }

            composable(ROUTE_COLAB) {
                ColabHubScreen(navController = navController)
            }

            composable(ROUTE_AUTOMATION) {
                AutomationHubScreen(mainViewModel = mainViewModel)
            }
        }
    }
}
