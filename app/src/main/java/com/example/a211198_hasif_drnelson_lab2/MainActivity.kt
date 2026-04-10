// Package declaration for the app
package com.example.a211198_hasif_drnelson_lab2

// Imports for Android and Compose components
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.a211198_hasif_drnelson_lab2.ui.theme.RunTrackTheme

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
    
    // Scaffold with bottom navigation bar
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Get current back stack entry and destination
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            
            // Navigation bar with black background
            NavigationBar(
                containerColor = Color.Black,
                contentColor = Color.White,
                tonalElevation = 0.dp
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
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        // Navigation host for handling screen navigation
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Define composable routes for each screen
            composable(Screen.Home.route) { HomeScreen(navController = navController) }
            composable(Screen.Search.route) { SearchScreen(navController = navController,) }
            composable(Screen.Maps.route) { MapsScreen() }
            composable(Screen.Record.route) { RecordScreen() }
            composable(Screen.Groups.route) { GroupsScreen() }
            composable(Screen.You.route) { YouScreen() }
        }
    }
}
