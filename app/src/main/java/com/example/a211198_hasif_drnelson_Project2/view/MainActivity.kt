// Package declaration for the app
package com.example.a211198_hasif_drnelson_Project2.view

// Imports for Android and Compose components
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
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
import com.example.a211198_hasif_drnelson_Project2.view_model.MessageViewModel
import com.example.a211198_hasif_drnelson_Project2.view_model.UserViewModel
import com.example.a211198_hasif_drnelson_Project2.ui.theme.RunTrackTheme
import com.example.a211198_hasif_drnelson_Project2.view.screen.ChatScreen
import com.example.a211198_hasif_drnelson_Project2.view.screen.EditProfileScreen
import com.example.a211198_hasif_drnelson_Project2.view.screen.GalleryScreen
import com.example.a211198_hasif_drnelson_Project2.view.screen.HomeScreen
import com.example.a211198_hasif_drnelson_Project2.view.screen.LoginScreen
import com.example.a211198_hasif_drnelson_Project2.view.screen.MessageScreen
import com.example.a211198_hasif_drnelson_Project2.view.screen.ProfileScreen
import com.example.a211198_hasif_drnelson_Project2.view.screen.RecordScreen
import com.example.a211198_hasif_drnelson_Project2.view.screen.SearchScreen
import com.example.a211198_hasif_drnelson_Project2.view.screen.SignupScreen

// Main activity class that extends ComponentActivity
class MainActivity : ComponentActivity() {
    // Override onCreate to set up the app
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Makes the app content flow behind the status bar and navigation bar (full-screen look)
        enableEdgeToEdge()
        // Set the content to use Compose with the theme
        setContent {
            RunTrackTheme {
                MainApp()  //Calls main UI function to start showing content.
            }
        }
    }
}

// Composable function for the main screen with navigation
@Composable
fun MainApp() {
    // Remember the navigation controller
    val navController = rememberNavController()

    // ViewModel Integration: instances shared across screens
    val userViewModel: UserViewModel = viewModel()
    val messageViewModel: MessageViewModel = viewModel()

    // Get current back stack entry and destination
    val navBackStackEntry by navController.currentBackStackEntryAsState() //Keeps track of which screen the user is currently looking at
    val currentDestination = navBackStackEntry?.destination //to get the name of the current screen.

    // Hide the bottom bar on the auth screens (Login + Signup).
    val authRoutes = listOf(Screen.Login.route, Screen.Signup.route)
    val shouldShowBottomBar = currentDestination?.route !in authRoutes

    // Scaffold with bottom navigation bar
    Scaffold( // pre-made layout helper to avoid overlap
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (shouldShowBottomBar) {
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
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        // Navigation host for handling screen navigation with animations
        NavHost(
            navController = navController,
            startDestination = Screen.Login.route,
            modifier = Modifier.padding(if (shouldShowBottomBar) innerPadding else PaddingValues(0.dp)),
            enterTransition = {  // slide from left + fade in
                fadeIn(animationSpec = tween(300)) + slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300)
                )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(300)) + slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300) // slide out to the left with speed 0.3s
                )
            },
            popEnterTransition = { //click back button - remove screen previous open (Stack implimentation)
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
            // Login Screen Route (launch screen). Sign Up button reaches Signup;
            // Google button signs in and goes straight to Home.
            composable(Screen.Login.route) {
                LoginScreen(
                    onSignUpClick = { navController.navigate(Screen.Signup.route) },
                    onGoogleSignIn = {
                        // Sign in with Google, then go to Home.
                        userViewModel.loginWithGoogle()
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onContinueClick = { name: String, email: String ->
                        // Attempt to login - validates that credentials match signup data
                        val loginSuccess = userViewModel.loginUser(email)

                        if (loginSuccess) {
                            // If login is successful, navigate to Home
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        }
                        // Login failed -> user stays on LoginScreen.
                    }
                )
            }

            // Signup Screen Route
            composable(Screen.Signup.route) {
                SignupScreen(
                    onBackClick = { navController.popBackStack() },
                    onSignupComplete = { name: String, email: String ->
                        // Register user with both name and email
                        userViewModel.registerUser(name, email)
                        // Navigate back to Login so the user can sign in.
                        navController.popBackStack()
                    }
                )
            }

            // Define composable routes for each screen, passing userViewModel where needed
            composable(Screen.Home.route) {
                HomeScreen(
                    navController = navController,
                    userViewModel = userViewModel
                )
            }
            composable(Screen.Search.route) {
                SearchScreen(
                    navController = navController,
                    userViewModel = userViewModel,
                    messageViewModel = messageViewModel
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    navController = navController,
                    userViewModel = userViewModel
                )
            }
            composable(Screen.EditProfile.route) {
                EditProfileScreen(
                    navController = navController,
                    userViewModel = userViewModel
                )
            }
            // Record + Maps combined screen.
            composable(Screen.Record.route) { RecordScreen(navController = navController) }
            // Gallery — reels feed (own + friends').
            composable(Screen.Gallery.route) {
                GalleryScreen(
                    navController = navController,
                    userViewModel = userViewModel,
                    messageViewModel = messageViewModel
                )
            }
            composable(Screen.Messages.route) {
                MessageScreen(
                    navController = navController,
                    messageViewModel = messageViewModel
                )
            }
            composable(
                route = Screen.Chat.route,
                arguments = listOf(androidx.navigation.navArgument("friendName") { type = androidx.navigation.NavType.StringType })
            ) { backStackEntry ->
                val raw = backStackEntry.arguments?.getString("friendName") ?: ""
                val friendName = java.net.URLDecoder.decode(raw, "UTF-8")
                ChatScreen(
                    navController = navController,
                    friendName = friendName,
                    messageViewModel = messageViewModel
                )
            }
        }
    }
}
