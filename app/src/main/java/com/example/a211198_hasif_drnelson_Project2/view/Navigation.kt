// Package declaration for the navigation-related classes
package com.example.a211198_hasif_drnelson_Project2.view

// Imports for Compose icons used in navigation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.Search
import androidx.compose.ui.graphics.vector.ImageVector

// Sealed class to define different screens in the app.
// Each screen has a route (for navigation), label (for display), and icon (for UI).
// The app is trimmed to exactly 10 screens (see plan.md).
sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    // 1. Login — the launch screen.
    object Login : Screen("login", "Login", Icons.Rounded.Lock)
    // 2. Sign up.
    object Signup : Screen("signup", "Sign Up", Icons.Rounded.PersonAdd)
    // 3. Home feed.
    object Home : Screen("home", "Home", Icons.Rounded.Home)
    // 4. Profile (also hosts Log out).
    object Profile : Screen("profile", "Profile", Icons.Rounded.Person)
    // 5. Messages inbox (also hosts group creation).
    object Messages : Screen("messages", "Messages", Icons.Rounded.Email)
    // 6. Edit profile.
    object EditProfile : Screen("edit_profile", "Edit Profile", Icons.Rounded.Person)
    // 7. Record + Maps combined into one screen.
    object Record : Screen("record", "Record", Icons.Rounded.RadioButtonChecked)
    // 8. Gallery — reels feed (own + friends').
    object Gallery : Screen("gallery", "Gallery", Icons.Rounded.Movie)
    // 9. Search.
    object Search : Screen("search", "Search", Icons.Rounded.Search)
    // 10. Chat with a specific friend. Use chatRoute(name) to build the destination.
    object Chat : Screen("chat/{friendName}", "Chat", Icons.Rounded.Email)
    // Viewing another user's gallery. Use userGalleryRoute(name) to build it.
    object UserGallery : Screen("gallery/user/{authorName}", "User Gallery", Icons.Rounded.Movie)
    // Viewing another user's profile. Use userProfileRoute(name) to build it.
    object UserProfile : Screen("profile/user/{authorName}", "User Profile", Icons.Rounded.Person)
}

// Build a concrete chat route for a given friend.
fun chatRoute(friendName: String): String = "chat/${java.net.URLEncoder.encode(friendName, "UTF-8")}"

// Build a route to view a specific user's personal gallery.
fun userGalleryRoute(authorName: String): String =
    "gallery/user/${java.net.URLEncoder.encode(authorName, "UTF-8")}"

// Build a route to view a specific user's profile (Follow-only, read-only gallery).
fun userProfileRoute(authorName: String): String =
    "profile/user/${java.net.URLEncoder.encode(authorName, "UTF-8")}"

// List of screens shown in the bottom navigation bar (5 tabs).
val bottomNavItems = listOf(
    Screen.Home,
    Screen.Search,
    Screen.Record,
    Screen.Gallery,
    Screen.Profile
)
