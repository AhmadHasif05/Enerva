package com.example.a211198_hasif_drnelson_Project2.view.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.a211198_hasif_drnelson_Project2.R
import com.example.a211198_hasif_drnelson_Project2.view.Screen
import com.example.a211198_hasif_drnelson_Project2.view.userGalleryRoute
import com.example.a211198_hasif_drnelson_Project2.view_model.GalleryViewModel
import com.example.a211198_hasif_drnelson_Project2.view_model.UserViewModel

// ProfileScreen composable: Matched with image and using Material Theme tokens
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userViewModel: UserViewModel,
    navController: NavController,
    onLogout: () -> Unit = {},
    galleryViewModel: GalleryViewModel = viewModel(factory = GalleryViewModel.Factory)
) {
    val userData = userViewModel.userProfile
    val primaryColor = MaterialTheme.colorScheme.primary
    val isFollowing = userViewModel.isFollowing(userData.runnerName)

    // Profile gallery = only this user's own posts.
    androidx.compose.runtime.LaunchedEffect(userData.email, userData.runnerName) {
        if (userData.email.isNotBlank()) {
            galleryViewModel.showMyPosts(userData.email, userData.runnerName.ifBlank { "You" })
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Gallery.route) }) {
                        Icon(Icons.Rounded.Share, contentDescription = "Gallery", tint = MaterialTheme.colorScheme.onBackground)
                    }
                    IconButton(onClick = {
                        // Log out: clears the active session across ViewModels and
                        // returns to Login (handled by MainActivity).
                        onLogout()
                    }) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = "Log out", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Header: Image and Name
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    AsyncImage(
                        model = userData.photoUri ?: R.drawable.hasif_profile,
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = userData.runnerName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = userData.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = userData.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                    if (userData.bio.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = userData.bio,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Stats row
            Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                Column {
                    Text("Following", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${userData.following}", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                }
                Spacer(modifier = Modifier.width(32.dp))
                Column {
                    Text("Followers", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${userData.followers}", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Primary action: Follow / Following
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                if (isFollowing) {
                    OutlinedButton(
                        onClick = { userViewModel.toggleFollow(userData.runnerName) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        border = BorderStroke(1.dp, primaryColor),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor)
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Following", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Button(
                        onClick = { userViewModel.toggleFollow(userData.runnerName) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Follow", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Secondary action: Edit profile
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                OutlinedButton(
                    onClick = { navController.navigate(Screen.EditProfile.route) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    border = BorderStroke(1.dp, primaryColor),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor)
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Profile", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Gallery section — Instagram-style 3-column image grid. Latest at the top.
            // Tapping any tile jumps to the Gallery reels screen.
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                Text(
                    "Gallery",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                val gallery = galleryViewModel.reels
                if (gallery.isEmpty()) {
                    Text(
                        "No posts yet. Tap + on Gallery to share your first run.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier.padding(horizontal = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        gallery.chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                row.forEach { item ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clickable {
                                                navController.navigate(userGalleryRoute(item.author))
                                            }
                                    ) {
                                        AsyncImage(
                                            model = item.imageUri ?: item.imageRes,
                                            contentDescription = item.caption,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                                repeat(3 - row.size) {
                                    Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Saved section — weekend-run places the user has bookmarked.
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                Text(
                    "Saved",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                val savedRoutes = userViewModel.savedRoutes
                if (savedRoutes.isEmpty()) {
                    Text(
                        "No saved places yet. Tap the bookmark on a route to save it here.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(savedRoutes) { route ->
                            RouteCard(
                                route = route,
                                saved = true,
                                onSaveToggle = { userViewModel.toggleRouteSave(route) }
                            )
                        }
                    }
                }
            }
        }
    }
}

