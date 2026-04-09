package com.example.a211198_hasif_drnelson_lab2

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.Search
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Rounded.Home)
    object Maps : Screen("maps", "Maps", Icons.Rounded.Map)
    object Record : Screen("record", "Record", Icons.Rounded.RadioButtonChecked)
    object Groups : Screen("groups", "Groups", Icons.Rounded.Group)
    object You : Screen("you", "You", Icons.Rounded.Person)
    object Search : Screen("search", "Search", Icons.Rounded.Search)
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Maps,
    Screen.Record,
    Screen.Groups,
    Screen.You
)
