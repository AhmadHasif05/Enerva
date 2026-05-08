// Package declaration for the screens
package com.example.a211198_hasif_drnelson_Project1.view.screen

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
import com.example.a211198_hasif_drnelson_Project1.R
import com.example.a211198_hasif_drnelson_Project1.view.Screen
import com.example.a211198_hasif_drnelson_Project1.view_model.UserViewModel
import com.example.a211198_hasif_drnelson_Project1.view.components.HomeTopBar
import com.example.a211198_hasif_drnelson_Project1.ui.theme.RunTrackTheme

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
                HomeTopBar(navController = navController)
                Spacer(modifier = Modifier.height(16.dp))
                InstantWorkoutsSection(onSeeAll = { navController.navigate(Screen.Activity.route) })
                Spacer(modifier = Modifier.height(32.dp))
                WeekendRunSection()
                Spacer(modifier = Modifier.height(100.dp))
            }
        }

        FloatingActionButton(
            onClick = { navController.navigate(Screen.Record.route) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 24.dp, end = 16.dp)
                .size(64.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(8.dp)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Add Workout", modifier = Modifier.size(32.dp))
        }
    }
}



// InstantWorkoutsSection
@Composable
fun InstantWorkoutsSection(onSeeAll: () -> Unit = {}) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Whatshot, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Instant Workouts-(A211198)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                )
            }
            TextButton(onClick = onSeeAll) {
                Text(text = "See all", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(workoutList) { workout -> WorkoutCard(workout) }
        }
    }
}

// WeekendRunSection
@Composable
fun WeekendRunSection() {
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
            items(routeList) { route -> WorkoutCardVisible(route) }
        }
    }
}

@Composable
fun WorkoutCardVisible(route: RunRoute) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(visible = visible, enter = fadeIn() + slideInVertically(initialOffsetY = { it }), exit = fadeOut()) {
        RouteCard(route)
    }
}

// Data class for RunRoute
data class RunRoute(
    val title: String,
    val distance: String,
    val time: String,
    val elevation: String,
    val difficulty: String,
    @androidx.annotation.DrawableRes val imageRes: Int
)

val routeList = listOf(
    RunRoute("Teratai", "3.22 km", "0h 21m", "0 m", "Easy", R.drawable.teratai),
    RunRoute("Lakeside Trail", "5.50 km", "0h 45m", "15 m", "Moderate", R.drawable.lakesidetrail)
)

@Composable
fun RouteCard(route: RunRoute) {
    Card(
        modifier = Modifier.width(320.dp).height(380.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().weight(0.72f)) {
                AsyncImage(model = route.imageRes, contentDescription = route.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                IconButton(
                    onClick = { },
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).size(40.dp).background(Color.Black.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(Icons.Rounded.BookmarkBorder, contentDescription = "Save", tint = Color.White, modifier = Modifier.size(24.dp))
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

// Data class for Workout
data class Workout(
    val title: String,
    val description: String,
    val duration: String,
    val icon: ImageVector,
    val color: Color
)

val workoutList = listOf(
    Workout("Brisk Walk", "Keep your body moving with a brisk walk.", "30m", Icons.AutoMirrored.Rounded.DirectionsWalk, Color(0xFF4A148C)),
    Workout("Easy Jog", "A light jog to get your heart rate up.", "20m", Icons.AutoMirrored.Rounded.DirectionsRun, Color(0xFF0D47A1))
)

@Composable
fun WorkoutCard(workout: Workout) {
    Card(
        modifier = Modifier.width(320.dp).height(130.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(90.dp).clip(RoundedCornerShape(16.dp)).background(Brush.verticalGradient(colors = listOf(workout.color.lighten(0.3f), workout.color))),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(workout.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = workout.duration, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = workout.title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp))
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = workout.description, style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant), maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp))
        }
    }
}

fun Color.lighten(factor: Float): Color = Color(red = (red + (1f - red) * factor), green = (green + (1f - green) * factor), blue = (blue + (1f - blue) * factor), alpha = alpha)

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun HomeScreenPreview() {
    RunTrackTheme(darkTheme = false) {
        HomeScreen(navController = rememberNavController(), userViewModel = UserViewModel())
    }
}






