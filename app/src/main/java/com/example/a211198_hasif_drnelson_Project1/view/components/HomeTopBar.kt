// Top bar shown above the Home feed. Doesn't use Material3 TopAppBar because
// we want a custom layout: title left, profile avatar centred, action icons right.
package com.example.a211198_hasif_drnelson_Project1.view.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.a211198_hasif_drnelson_Project1.R
import com.example.a211198_hasif_drnelson_Project1.view.Screen

// `navController` is passed in so each tap can route the user to the
// appropriate screen (Profile / Messages / Search / Notifications).
@Composable
fun HomeTopBar(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // "Home" title — pinned to the left edge using Box alignment.
        Text(
            text = "Home",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            ),
            modifier = Modifier.align(Alignment.CenterStart)
        )

        // Profile avatar — centred horizontally. Tapping opens the profile.
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                .align(Alignment.Center)
                .clickable { navController.navigate(Screen.Profile.route) }
        ) {
            AsyncImage(
                model = R.drawable.hasif_profile,
                contentDescription = "Profile Picture",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Right-side actions: chat, search, notifications.
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.navigate(Screen.Messages.route) },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.Chat, contentDescription = "Chat", tint = MaterialTheme.colorScheme.onBackground)
            }
            IconButton(
                onClick = { navController.navigate(Screen.Search.route) },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Rounded.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onBackground)
            }
            IconButton(
                onClick = { navController.navigate(Screen.Notifications.route) },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Rounded.Notifications, contentDescription = "Notifications", tint = MaterialTheme.colorScheme.onBackground)
            }
        }
    }
}