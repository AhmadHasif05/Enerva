// Package declaration for the app
package com.example.a211198_hasif_drnelson_lab4

// Imports for Android and Compose components
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.a211198_hasif_drnelson_lab4.ui.theme.RunTrackTheme

// Main activity class that extends ComponentActivity
class MainActivity : ComponentActivity() {
    // Override onCreate to set up the app
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable edge-to-edge display
        enableEdgeToEdge()
        // Set the content to use Compose with the theme
        setContent {
            RunTrackTheme {
                MainScreen()
            }
        }
    }
}

// Composable function for the main screen with navigation
@Composable
fun MainScreen() {
    // Remember the navigation controller
    val navController = rememberNavController()
    
    // ViewModel Integration: Instance of UserViewModel to be shared
    val userViewModel: UserViewModel = viewModel()
    
    // Scaffold with bottom navigation bar
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Get current back stack entry and destination
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            
            // Navigation bar using theme colors
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 8.dp
            ) {
                // Iterate over bottom navigation items
                bottomNavItems.forEach { screen ->
                    // Check if the current destination matches the screen route
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    // Navigation bar item for each screen
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = selected,
                        onClick = {
                            // Navigate to the selected screen
                            navController.navigate(screen.route) {
                                // Pop up to the start destination, saving state
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                // Launch single top and restore state
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        // Colors for selected and unselected states
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFFFF5722),
                            selectedTextColor = Color(0xFFFF5722),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        // Navigation host for handling screen navigation with animations
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                fadeIn(animationSpec = tween(300)) + slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300)
                )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(300)) + slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300)
                )
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(300)) + slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300)
                )
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(300)) + slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300)
                )
            }
        ) {
            // Define composable routes for each screen, passing userViewModel where needed
            composable(Screen.Home.route) { HomeScreen(navController = navController, userViewModel = userViewModel) }
            composable(Screen.Search.route) { SearchScreen(navController = navController) }
            composable(Screen.Profile.route) { ProfileScreen(navController = navController, userViewModel = userViewModel) }
            composable(Screen.Maps.route) { MapsScreen() }
            composable(Screen.Record.route) { RecordScreen() }
            composable(Screen.Groups.route) { GroupsScreen() }
            composable(Screen.You.route) { YouScreen(userViewModel = userViewModel) }
        }
    }
}
