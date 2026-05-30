package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ChallengeViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: ChallengeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeState.collectAsState()

            MyApplicationTheme(themeMode = themeMode) {
                AppMainShell(viewModel = viewModel)
            }
        }
    }
}

sealed class NavigationTab(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    object Home : NavigationTab("home", "Habits", Icons.Default.Home, Icons.Outlined.Home, "nav_tab_home")
    object Analytics : NavigationTab("analytics", "Analytics", Icons.Default.Analytics, Icons.Outlined.Analytics, "nav_tab_analytics")
    object Achievements : NavigationTab("achievements", "Medals", Icons.Default.EmojiEvents, Icons.Outlined.EmojiEvents, "nav_tab_achievements")
    object Settings : NavigationTab("settings", "Rules", Icons.Default.Settings, Icons.Outlined.Settings, "nav_tab_settings")
}

@Composable
fun AppMainShell(viewModel: ChallengeViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val navigationTabs = listOf(
        NavigationTab.Home,
        NavigationTab.Analytics,
        NavigationTab.Achievements,
        NavigationTab.Settings
    )

    // Only display bottom navigation bar for the 4 primary tabs
    val displayBottomBar = currentRoute in navigationTabs.map { it.route }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (displayBottomBar) {
                NavigationBar(
                    containerColor = if (MaterialTheme.colorScheme.background.red < 0.1f) Color(0xFF050505) else MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    modifier = Modifier.testTag("app_bottom_nav_bar")
                ) {
                    navigationTabs.forEach { tab ->
                        val isSelected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = isSelected,
                            label = {
                                Text(
                                    text = tab.title,
                                    fontSize = 11.sp
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            onClick = {
                                navController.navigate(tab.route) {
                                    // Pop up to the start destination of the graph to
                                    // avoid building up a large stack of destinations
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    // Avoid multiple copies of the same destination when
                                    // reselecting the same item
                                    launchSingleTop = true
                                    // Restore state when reselecting a previously selected item
                                    restoreState = true
                                }
                            },
                            modifier = Modifier.testTag(tab.tag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier.padding(innerPadding)
        ) {
            // Animated Splash Screen
            composable("splash") {
                SplashScreen(
                    onNavigateToHome = {
                        navController.navigate("home") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                )
            }

            // Dashboard
            composable("home") {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToCreate = {
                        navController.navigate("create_challenge")
                    },
                    onNavigateToDetails = { challengeId ->
                        navController.navigate("challenge_details/$challengeId")
                    }
                )
            }

            // Performance hub
            composable("analytics") {
                AnalyticsScreen(viewModel = viewModel)
            }

            // Badge Board
            composable("achievements") {
                AchievementsScreen(viewModel = viewModel)
            }

            // Settings Configurations
            composable("settings") {
                SettingsScreen(viewModel = viewModel)
            }

            // Create Challenge
            composable("create_challenge") {
                CreateChallengeScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // Challenge Details with argument mapping
            composable(
                route = "challenge_details/{challengeId}",
                arguments = listOf(navArgument("challengeId") { type = NavType.IntType })
            ) { backStackEntry ->
                val challengeId = backStackEntry.arguments?.getInt("challengeId") ?: -1
                DetailsScreen(
                    challengeId = challengeId,
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
