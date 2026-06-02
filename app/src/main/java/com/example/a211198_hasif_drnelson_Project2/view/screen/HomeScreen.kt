// Package declaration for the screens
package com.example.a211198_hasif_drnelson_Project2.view.screen

// Imports for Compose UI components, icons, navigation, and other utilities
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.a211198_hasif_drnelson_Project2.model.GalleryActivity
import com.example.a211198_hasif_drnelson_Project2.model.RunRoute
import com.example.a211198_hasif_drnelson_Project2.model.routeList
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.a211198_hasif_drnelson_Project2.view.Screen
import com.example.a211198_hasif_drnelson_Project2.view_model.GalleryViewModel
import com.example.a211198_hasif_drnelson_Project2.view_model.UserViewModel
import com.example.a211198_hasif_drnelson_Project2.view.components.HomeTopBar
import com.example.a211198_hasif_drnelson_Project2.ui.theme.RunTrackTheme

// HomeScreen composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, userViewModel: UserViewModel) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
            ) {
                HomeTopBar(
                    navController = navController,
                    photoUri = userViewModel.userProfile.photoUri
                )
                Spacer(modifier = Modifier.height(16.dp))
                GallerySection(
                    userViewModel = userViewModel,
                    onSeeAll = { navController.navigate(Screen.Profile.route) }
                )
                Spacer(modifier = Modifier.height(32.dp))
                WeekendRunSection(userViewModel = userViewModel)
                Spacer(modifier = Modifier.height(100.dp))
            }
        }

        // The orange + FAB is rendered globally in MainActivity so it can
        // overlap the bottom nav bar at the Record tab. Kept out of this
        // screen's Box on purpose.
    }
}



// GallerySection — styled to match WeekendRunSection (big headline + subtitle),
// but shows gallery image tiles. "See all" jumps to Profile where the full
// gallery grid lives.
@Composable
fun GallerySection(
    userViewModel: UserViewModel,
    onSeeAll: () -> Unit = {},
    galleryViewModel: GalleryViewModel = viewModel(factory = GalleryViewModel.Factory)
) {
    val myEmail = userViewModel.userProfile.email
    val myName = userViewModel.userProfile.runnerName.ifBlank { "You" }
    LaunchedEffect(myEmail, myName) {
        if (myEmail.isNotBlank()) galleryViewModel.showMyPosts(myEmail, myName)
    }
    val activities = galleryViewModel.reels
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Your Progress - (A211198)",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 24.sp
                    )
                )
                Text(
                    text = "Latest captures from your runs",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )
                )
            }
            TextButton(onClick = onSeeAll) {
                Text(text = "See all", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        if (activities.isEmpty()) {
            Text(
                "No posts yet. Capture a run on the Gallery tab.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(activities) { activity ->
                    GalleryImageTile(activity = activity, onClick = onSeeAll)
                }
            }
        }
    }
}

// Image-only tile (Instagram-vibes) used by the Home gallery row.
@Composable
fun GalleryImageTile(activity: GalleryActivity, onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .size(160.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = activity.imageUri ?: activity.imageRes,
            contentDescription = activity.caption,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // Bottom gradient + caption so the tile still reads as content, not just a swatch.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.6f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.7f)
                    )
                )
        )
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)
        ) {
            Text(
                text = activity.author,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${activity.activity} · ${activity.distanceKm} km",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}

// WeekendRunSection
@Composable
fun WeekendRunSection(userViewModel: UserViewModel) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Plan Your Weekend Run",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, fontSize = 24.sp)
            )
            Text(
                text = "Explore these popular routes near you",
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.5.sp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(routeList) { route ->
                WorkoutCardVisible(
                    route = route,
                    saved = userViewModel.isRouteSaved(route.title),
                    onSaveToggle = { userViewModel.toggleRouteSave(route) }
                )
            }
        }
    }
}

@Composable
fun WorkoutCardVisible(route: RunRoute, saved: Boolean, onSaveToggle: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(visible = visible, enter = fadeIn() + slideInVertically(initialOffsetY = { it }), exit = fadeOut()) {
        RouteCard(route = route, saved = saved, onSaveToggle = onSaveToggle)
    }
}

@Composable
fun RouteCard(route: RunRoute, saved: Boolean = false, onSaveToggle: () -> Unit = {}) {
    Card(
        modifier = Modifier.width(320.dp).height(380.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().weight(0.72f)) {
                AsyncImage(model = route.imageRes, contentDescription = route.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                IconButton(
                    onClick = onSaveToggle,
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).size(40.dp).background(Color.Black.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        if (saved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = if (saved) "Saved" else "Save",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Column(modifier = Modifier.fillMaxWidth().weight(0.28f).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.Center) {
                Text(text = route.title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 22.sp))
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Color(0xFF4CAF50).copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                        Text(text = route.difficulty, color = Color(0xFF81C784), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "${route.distance} (${route.time}) • ${route.elevation}", style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium))
                }
            }
        }
    }
}

fun Color.lighten(factor: Float): Color = Color(red = (red + (1f - red) * factor), green = (green + (1f - green) * factor), blue = (blue + (1f - blue) * factor), alpha = alpha)

// Preview removed: UserViewModel is now an AndroidViewModel and can't be
// instantiated without an Application context. Use device run for layout checks.






