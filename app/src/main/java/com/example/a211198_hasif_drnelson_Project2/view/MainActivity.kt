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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.a211198_hasif_drnelson_Project2.BuildConfig
import com.example.a211198_hasif_drnelson_Project2.data.repository.GoogleSignInHelper
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.a211198_hasif_drnelson_Project2.RunTrackApplication
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

    // ViewModel Integration: instances shared across screens, backed by Room.
    val userViewModel: UserViewModel = viewModel(factory = UserViewModel.Factory)
    val messageViewModel: MessageViewModel = viewModel(factory = MessageViewModel.Factory)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Credential Manager helper for "Continue with Google". Web client ID is
    // injected from local.properties via BuildConfig; blank → not configured.
    val googleSignInHelper = remember { GoogleSignInHelper(BuildConfig.GOOGLE_WEB_CLIENT_ID) }

    // Start on Home if Firebase already has a signed-in user (e.g. after a restart).
    val app = context.applicationContext as RunTrackApplication
    val startRoute = if (app.authRepository.currentUser != null) Screen.Home.route
                     else Screen.Login.route

    // Get current back stack entry and destination
    val navBackStackEntry by navController.currentBackStackEntryAsState() //Keeps track of which screen the user is currently looking at
    val currentDestination = navBackStackEntry?.destination //to get the name of the current screen.

    // Hide the bottom bar on the auth screens (Login + Signup).
    val authRoutes = listOf(Screen.Login.route, Screen.Signup.route)
    val shouldShowBottomBar = currentDestination?.route !in authRoutes

    // Wrap Scaffold in a Box so the orange + FAB can render *over* the bottom
    // nav bar (specifically on the Record tab). Inside Scaffold's content slot
    // we'd be constrained above the nav bar.
    Box(modifier = Modifier.fillMaxSize()) {

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
                                    // Pop up to Home — the stable base of the logged-in
                                    // experience. We can't use the graph's start
                                    // destination here: it may be the Login route, which
                                    // is popped inclusive after sign-in and so no longer
                                    // exists on the back stack (popUpTo would match
                                    // nothing and the stack would grow unbounded).
                                    popUpTo(Screen.Home.route) {
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
            startDestination = startRoute,
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
                        scope.launch {
                            try {
                                val idToken = googleSignInHelper.getIdToken(context)
                                userViewModel.loginWithGoogle(idToken) { success, error ->
                                    if (success) {
                                        messageViewModel.setActiveUser(userViewModel.userProfile.email)
                                        navController.navigate(Screen.Home.route) {
                                            popUpTo(Screen.Login.route) { inclusive = true }
                                        }
                                    } else {
                                        android.util.Log.e("GoogleSignIn", "Firebase sign-in failed: $error")
                                        Toast.makeText(context, error ?: "Google sign-in failed", Toast.LENGTH_LONG).show()
                                    }
                                }
                            } catch (e: GetCredentialCancellationException) {
                                // User dismissed the account picker — no-op, no toast spam.
                            } catch (e: NoCredentialException) {
                                Toast.makeText(context, "No Google account found on this device", Toast.LENGTH_SHORT).show()
                            } catch (e: GoogleSignInHelper.NotConfiguredException) {
                                Toast.makeText(context, "Google Sign-In not configured (missing Web client ID)", Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                android.util.Log.e("GoogleSignIn", "Credential flow failed", e)
                                Toast.makeText(context, e.message ?: "Google sign-in failed", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    onForgotPassword = { email, cb ->
                        userViewModel.sendPasswordReset(email, cb)
                    },
                    onContinueClick = { email: String, password: String ->
                        // Sign in against Firebase Auth. Async — navigate from the callback.
                        userViewModel.loginUser(email, password) { success, error ->
                            if (success) {
                                messageViewModel.setActiveUser(email)
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            } else {
                                Toast.makeText(context, error ?: "Login failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }

            // Signup Screen Route — creates a Firebase Auth user, seeds the local row, navigates Home.
            composable(Screen.Signup.route) {
                SignupScreen(
                    onBackClick = { navController.popBackStack() },
                    onSignupComplete = { name: String, email: String, password: String ->
                        userViewModel.registerUser(name, email, password) { ok, error ->
                            if (ok) {
                                // Firebase auto-signs the new user in. Sign them out so they
                                // have to log in explicitly on the Login screen.
                                app.authRepository.signOut()
                                Toast.makeText(context, "Account created. Please log in.", Toast.LENGTH_SHORT).show()
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            } else {
                                Toast.makeText(context, error ?: "Signup failed", Toast.LENGTH_SHORT).show()
                            }
                        }
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
                    userViewModel = userViewModel,
                    onLogout = {
                        // Clear BOTH activity-scoped ViewModels' active user so the
                        // next account starts clean (per-screen Gallery VMs are
                        // disposed by the popUpTo(0) below).
                        userViewModel.logout()
                        messageViewModel.clearActiveUser()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
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
                route = Screen.UserGallery.route,
                arguments = listOf(androidx.navigation.navArgument("authorName") { type = androidx.navigation.NavType.StringType })
            ) { backStackEntry ->
                val raw = backStackEntry.arguments?.getString("authorName") ?: ""
                val authorName = java.net.URLDecoder.decode(raw, "UTF-8")
                GalleryScreen(
                    navController = navController,
                    userViewModel = userViewModel,
                    messageViewModel = messageViewModel,
                    authorName = authorName
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

        // Orange + FAB overlay — sits on top of the bottom nav bar over the
        // Record tab. Visible only when the bottom nav is showing.
        if (shouldShowBottomBar) {
            FloatingActionButton(
                onClick = {
                    navController.navigate(Screen.Record.route) {
                        launchSingleTop = true
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    // Nav bar is ~80dp tall; FAB is 80dp. Bottom padding of 40dp
                    // places the FAB's vertical center on the nav bar's top
                    // edge — half above, half below — and the larger size
                    // visually covers the Record tab.
                    .padding(bottom = 40.dp)
                    .size(80.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(Icons.Rounded.RadioButtonChecked, contentDescription = "Record", modifier = Modifier.size(40.dp))
            }
        }
    }
}
