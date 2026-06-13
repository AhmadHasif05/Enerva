package com.example.a211198_hasif_drnelson_Project2.view.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.a211198_hasif_drnelson_Project2.model.UserData
import com.example.a211198_hasif_drnelson_Project2.view.Screen
import com.example.a211198_hasif_drnelson_Project2.view.userGalleryRoute
import com.example.a211198_hasif_drnelson_Project2.view_model.GalleryViewModel
import com.example.a211198_hasif_drnelson_Project2.view_model.MessageViewModel
import com.example.a211198_hasif_drnelson_Project2.view_model.UserViewModel

// ProfileScreen — dual mode.
//  - authorName == null  → the signed-in user's own profile (Edit Profile, Logout,
//    Saved section, and the multi-select gallery grid).
//  - authorName != null  → another user's profile: avatar + name + stats + a single
//    Follow/Following button, a read-only gallery, and no Edit/Logout/Saved.
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProfileScreen(
    userViewModel: UserViewModel,
    navController: NavController,
    messageViewModel: MessageViewModel = viewModel(factory = MessageViewModel.Factory),
    onLogout: () -> Unit = {},
    authorName: String? = null,
    galleryViewModel: GalleryViewModel = viewModel(factory = GalleryViewModel.Factory)
) {
    val isSelf = authorName == null
    val myProfile = userViewModel.userProfile

    // For another user's profile, resolve their data from the in-memory directory
    // list first, then fall back to a name lookup.
    var otherProfile by remember(authorName) { mutableStateOf<UserData?>(null) }
    if (!isSelf) {
        val fromList = userViewModel.otherUsers.firstOrNull { it.runnerName == authorName }
        LaunchedEffect(authorName, fromList) {
            otherProfile = fromList ?: userViewModel.findUserByName(authorName!!)
        }
    }
    val userData = if (isSelf) myProfile else (otherProfile ?: UserData(runnerName = authorName ?: ""))

    val primaryColor = MaterialTheme.colorScheme.primary
    val isFollowing = userViewModel.isFollowing(userData.runnerName)

    // Multi-select state for the own gallery grid (self mode only).
    val selectedIds = remember { mutableStateMapOf<String, Boolean>() }
    var selectionMode by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val selectedCount = selectedIds.count { it.value }

    // Pick the gallery mode: my own posts vs. the visited user's posts.
    LaunchedEffect(isSelf, userData.email, userData.runnerName) {
        if (isSelf) {
            if (userData.email.isNotBlank()) {
                galleryViewModel.showMyPosts(userData.email, userData.runnerName.ifBlank { "You" })
            }
        } else if (userData.runnerName.isNotBlank()) {
            galleryViewModel.showAuthorGallery(userData.runnerName)
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
                    // Gallery + Logout actions only belong on your own profile.
                    if (isSelf) {
                        IconButton(onClick = { navController.navigate(Screen.Gallery.route) }) {
                            Icon(Icons.Rounded.Share, contentDescription = "Gallery", tint = MaterialTheme.colorScheme.onBackground)
                        }
                        IconButton(onClick = { onLogout() }) {
                            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = "Log out", tint = MaterialTheme.colorScheme.onBackground)
                        }
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
                    // Email is private to other users — only show it on your own profile.
                    if (isSelf && userData.email.isNotBlank()) {
                        Text(
                            text = userData.email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
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

            if (isSelf) {
                // Own profile: Edit Profile only (no self-follow).
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
            } else {
                // Another user's profile: Follow / Following only.
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    if (isFollowing) {
                        OutlinedButton(
                            onClick = {
                                userViewModel.toggleFollow(userData.runnerName)
                                messageViewModel.removeConversation(userData.runnerName)
                            },
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
                            onClick = {
                                userViewModel.toggleFollow(userData.runnerName)
                                messageViewModel.startConversationWith(userData.runnerName)
                            },
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
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Gallery section — Instagram-style 3-column image grid. Latest at the top.
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                // Header doubles as a selection action bar when selecting (self only).
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelf && selectionMode) {
                        IconButton(onClick = {
                            selectionMode = false
                            selectedIds.clear()
                        }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Cancel selection", tint = MaterialTheme.colorScheme.onBackground)
                        }
                        Text(
                            "$selectedCount selected",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = { if (selectedCount > 0) showDeleteConfirm = true },
                            enabled = selectedCount > 0
                        ) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = "Delete selected",
                                tint = if (selectedCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            "Gallery",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                val gallery = galleryViewModel.reels
                if (gallery.isEmpty()) {
                    Text(
                        if (isSelf) "No posts yet. Tap + on Gallery to share your first run."
                        else "${userData.runnerName} hasn't posted any reels yet.",
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
                                    val isChecked = selectedIds[item.id] == true
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(4f / 5f)
                                            .combinedClickable(
                                                onClick = {
                                                    if (isSelf && selectionMode) {
                                                        if (isChecked) selectedIds.remove(item.id)
                                                        else selectedIds[item.id] = true
                                                        // Leaving selection mode when nothing is left selected.
                                                        if (selectedIds.none { it.value }) selectionMode = false
                                                    } else {
                                                        navController.navigate(userGalleryRoute(item.author))
                                                    }
                                                },
                                                onLongClick = {
                                                    if (isSelf) {
                                                        selectionMode = true
                                                        selectedIds[item.id] = true
                                                    }
                                                }
                                            )
                                    ) {
                                        AsyncImage(
                                            model = item.imageUri ?: item.imageRes,
                                            contentDescription = item.caption,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        if (isSelf && selectionMode) {
                                            // Dim + check overlay on every tile while selecting.
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = if (isChecked) 0.35f else 0.0f))
                                            )
                                            Icon(
                                                if (isChecked) Icons.Rounded.Check else Icons.Rounded.RadioButtonUnchecked,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(6.dp)
                                                    .size(20.dp)
                                            )
                                        }
                                    }
                                }
                                repeat(3 - row.size) {
                                    Box(modifier = Modifier.weight(1f).aspectRatio(4f / 5f))
                                }
                            }
                        }
                    }
                }
            }

            // Confirm dialog for deleting the selected posts.
            if (showDeleteConfirm) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirm = false },
                    title = { Text("Delete ${selectedCount} post(s)?") },
                    text = { Text("This removes them from your gallery on all your devices. This can't be undone.") },
                    confirmButton = {
                        TextButton(onClick = {
                            val ids = selectedIds.filter { it.value }.keys.toSet()
                            galleryViewModel.deletePosts(ids)
                            selectedIds.clear()
                            selectionMode = false
                            showDeleteConfirm = false
                        }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
                    }
                )
            }

            // Saved section — own profile only.
            if (isSelf) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
}
