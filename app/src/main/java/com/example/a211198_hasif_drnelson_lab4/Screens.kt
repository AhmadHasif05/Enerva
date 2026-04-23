// Package declaration for the screens
package com.example.a211198_hasif_drnelson_lab4

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
import androidx.compose.material.icons.automirrored.rounded.Notes
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.a211198_hasif_drnelson_lab4.ui.theme.RunTrackTheme

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
                InstantWorkoutsSection()
                Spacer(modifier = Modifier.height(32.dp))
                WeekendRunSection()
                Spacer(modifier = Modifier.height(100.dp))
            }
        }

        FloatingActionButton(
            onClick = { /* Start Record */ },
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

// HomeTopBar composable
@Composable
fun HomeTopBar(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Home",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            ),
            modifier = Modifier.align(Alignment.CenterStart)
        )

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

        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.AutoMirrored.Rounded.Chat, contentDescription = "Chat", tint = MaterialTheme.colorScheme.onBackground)
            }
            IconButton(
                onClick = { navController.navigate(Screen.Search.route) },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Rounded.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onBackground)
            }
            IconButton(onClick = { }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Rounded.Notifications, contentDescription = "Notifications", tint = MaterialTheme.colorScheme.onBackground)
            }
        }
    }
}

// ProfileScreen composable: Matched with image and using Material Theme tokens
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(userViewModel: UserViewModel, navController: NavController) {
    val userData = userViewModel.userProfile
    val primaryColor = MaterialTheme.colorScheme.primary

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
                    IconButton(onClick = { }) { Icon(Icons.Rounded.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onBackground) }
                    IconButton(onClick = { }) { Icon(Icons.Rounded.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.onBackground) }
                    IconButton(onClick = { }) { Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onBackground) }
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
                        model = R.drawable.hasif_profile,
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
                    Text(
                        text = userData.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
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

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, primaryColor),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor)
                ) {
                    Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share my QR code", fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = { },
                    modifier = Modifier.weight(0.5f),
                    border = BorderStroke(1.dp, primaryColor),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor)
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit", fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Activity Section
            Column(modifier = Modifier.padding(16.dp)) {
                Text("This week", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatItem(label = "Distance", value = "0 km")
                    StatItem(label = "Time", value = "0h")
                    StatItem(label = "Elevation Gain", value = "0 m")
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Chart Placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(bottom = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        repeat(10) {
                            Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.DarkGray))
                        }
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.background).align(Alignment.BottomCenter))
                    Row(
                        modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).offset(y = 18.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Text("FEB", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        Text("MAR", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        Text("APR", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    }
                    Text("0 km", color = MaterialTheme.colorScheme.onBackground, fontSize = 10.sp, modifier = Modifier.align(Alignment.TopEnd))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 8.dp)

            // List Items
            ProfileListItem(icon = Icons.Rounded.Timeline, title = "Activities", sub = "November 22, 2025")
            ProfileListItem(icon = Icons.Rounded.BarChart, title = "Statistics", sub = "—")
        }
    }
}

//Message Screen

@Composable
private fun StatItem(label: String, value: String) {
    Column {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        Text(value, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun ProfileListItem(icon: ImageVector, title: String, sub: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            Text(sub, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
    }
}

// InstantWorkoutsSection
@Composable
fun InstantWorkoutsSection() {
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
            TextButton(onClick = { }) {
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
    val imageUrl: String
)

val routeList = listOf(
    RunRoute("Teratai", "3.22 km", "0h 21m", "0 m", "Easy", "https://images.unsplash.com/photo-1502082553048-f009c37129b9?ixlib=rb-1.2.1&auto=format&fit=crop&w=800&q=60"),
    RunRoute("Lakeside Trail", "5.50 km", "0h 45m", "15 m", "Moderate", "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?ixlib=rb-1.2.1&auto=format&fit=crop&w=800&q=60")
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
                AsyncImage(model = route.imageUrl, contentDescription = route.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavController) {
    // State variables for tabs, search text, and followed people
    val tabTitles = listOf("Friends", "Clubs")
    var selectedTab by remember { mutableStateOf(0) }
    var searchText by remember { mutableStateOf("") }
    var followedPeople by remember { mutableStateOf(setOf<String>()) }
    // List of people data
    val people = listOf(
        Triple("Anette Visser", "Hantum, Friesland", "Fan favorite on Strava"),
        Triple("Liyana Rahman", "Jerantut, Pahang", "Local Legend near you"),
        Triple("Mohd Khairol Azani", "Local Legend near you", ""),
        Triple("Helly M", "Marin, CA", "Fan favorite on Strava"),
        Triple("boy ezwan", "Local Legend near you", "")
    )
    // Scaffold with top bar, bottom bar, and content
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search", color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate(Screen.Home.route) }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            // Bottom bar with invite friends button
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
            ) {
                Button(
                    onClick = { /* Invite Friends */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
                ) {
                    Text("Invite Friends", color = Color.White)
                }
            }
        }
    ) { innerPadding ->
        // Column for main content
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab row for Friends and Clubs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                color = if (selectedTab == index) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            // Search text field
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                TextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text("Search on Enerva", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        disabledContainerColor = MaterialTheme.colorScheme.surface,
                    )
                )
            }
            Spacer(Modifier.height(16.dp))
            // Row of icons for suggestions, Facebook, Contacts, QR Code
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val iconData = listOf(
                    Pair(Icons.Rounded.Group, "Suggest"),
                    Pair(Icons.Rounded.Facebook, "Facebook"),
                    Pair(Icons.Rounded.Contacts, "Contacts"),
                    Pair(Icons.Rounded.QrCode, "QR Code")
                )
                iconData.forEach { (icon, label) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(32.dp))
                        Text(label, color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp, fontWeight = if (label == "Suggest") FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            // Header for people you may know
            Text(
                "PEOPLE YOU MAY KNOW",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(8.dp))
            // LazyColumn for list of people
            LazyColumn(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                // Filter people based on search text
                val filteredPeople = people.filter { (name, _, _) ->
                    name.contains(searchText, ignoreCase = true)
                }

                // If no results, show message
                if (filteredPeople.isEmpty() && searchText.isNotEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No results found for \"$searchText\"",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 16.sp
                            )
                        }
                    }
                } else {
                    // Items for each person
                    items(filteredPeople) { (name, location, subtitle) ->
                        val isFollowed = followedPeople.contains(name)
                        // Card for each person
                        Card(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(40.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                    Text(location, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                                    if (subtitle.isNotEmpty()) Text(subtitle, color = Color(0xFFFF9800), fontSize = 12.sp)
                                }
                                // Follow button that toggles to Followed
                                Button(
                                    onClick = {
                                        followedPeople = if (isFollowed) {
                                            followedPeople - name
                                        } else {
                                            followedPeople + name
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isFollowed) Color(0xFFFF5722) else Color.Transparent
                                    ),
                                    border = if (isFollowed) null else BorderStroke(1.dp, Color(0xFFFF5722)),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                                ) {
                                    Text(
                                        if (isFollowed) "Followed" else "Follow",
                                        color = if (isFollowed) Color.White else Color(0xFFFF5722)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Map Screen
@Composable
fun MapsScreen() { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Maps Screen") } }

// Record Screen
@Composable
fun RecordScreen() { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Record Screen") } }

// Group Screen
@Composable
fun GroupsScreen() { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Groups Screen") } }

// YouScreen composable
@Composable
fun YouScreen(userViewModel: UserViewModel) {
    val userData = userViewModel.userProfile
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color(0xFFFF5722))
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Welcome, ${userData.runnerName}", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
            Text(text = "${userData.studentId} Dashboard", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}