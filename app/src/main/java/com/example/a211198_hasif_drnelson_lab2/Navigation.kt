// Package declaration for the navigation-related classes
package com.example.a211198_hasif_drnelson_lab2

// Imports for Compose icons used in navigation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.Search
import androidx.compose.ui.graphics.vector.ImageVector

// Sealed class to define different screens in the app
// Each screen has a route (for navigation), label (for display), and icon (for UI)
sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    // Home screen object
    object Home : Screen("home", "Home", Icons.Rounded.Home)
    // Maps screen object
    object Maps : Screen("maps", "Maps", Icons.Rounded.Map)
    // Record screen object
    object Record : Screen("record", "Record", Icons.Rounded.RadioButtonChecked)
    // Groups screen object
    object Groups : Screen("groups", "Groups", Icons.Rounded.Group)
    // You screen object
    object You : Screen("you", "You", Icons.Rounded.Person)
    // Search screen object
    object Search : Screen("search", "Search", Icons.Rounded.Search)
}

// List of screens to display in the bottom navigation bar
// Note: Search is defined but not included in bottom nav, as per the code
val bottomNavItems = listOf(
    Screen.Home,
    Screen.Maps,
    Screen.Record,
    Screen.Groups,
    Screen.You
)
