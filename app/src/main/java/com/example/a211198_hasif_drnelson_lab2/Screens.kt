package com.example.a211198_hasif_drnelson_lab2

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
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
            Spacer(modifier = Modifier.height(100.dp)) // Padding for FAB and Bottom Nav
        }

        // Orange FAB positioned at bottom right (BottomEnd)
        FloatingActionButton(
            onClick = { /* Start Record */ },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 24.dp, end = 16.dp)
                .size(64.dp),
            containerColor = Color(0xFFFF5722),
            contentColor = Color.White,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(8.dp)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Add Workout", modifier = Modifier.size(32.dp))
        }
    }
}

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
                color = Color.White
            ),
            modifier = Modifier.align(Alignment.CenterStart)
        )

        // Profile Picture centrally positioned
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(1.dp, Color.Gray, CircleShape)
                .align(Alignment.Center)
        ) {
            AsyncImage(
                model = "https://randomuser.me/api/portraits/men/32.jpg",
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
                Icon(Icons.AutoMirrored.Rounded.Chat, contentDescription = "Chat", tint = Color.White)
            }
            IconButton(
                onClick = { navController.navigate(Screen.Search.route) },
                modifier = Modifier.size(40.dp)

            ) {
                Icon(Icons.Rounded.Search, contentDescription = "Search", tint = Color.White)
            }
            IconButton(onClick = { }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Rounded.Notifications, contentDescription = "Notifications", tint = Color.White)
            }
        }
    }
}

@Composable
fun InstantWorkoutsSection() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFFF5722).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Whatshot,
                        contentDescription = null,
                        tint = Color(0xFFFF5722),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Instant Workouts - Hasif(A211198)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
            TextButton(onClick = { }) {
                Text(text = "See all", color = Color(0xFFFF5722), fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(workoutList) { workout ->
                WorkoutCard(workout)
            }
        }
    }
}

@Composable
fun WeekendRunSection() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Plan Your Weekend Run",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 24.sp
                )
            )
            Text(
                text = "Explore these popular routes near you",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.LightGray,
                    letterSpacing = 0.5.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(routeList) { route ->
                RouteCard(route)
            }
        }
    }
}

data class Route(
    val title: String,
    val distance: String,
    val time: String,
    val elevation: String,
    val difficulty: String,
    val imageUrl: String
)

val routeList = listOf(
    Route(
        "Teratai",
        "3.22 km",
        "0h 21m",
        "0 m",
        "Easy",
        "https://images.unsplash.com/photo-1502082553048-f009c37129b9?ixlib=rb-1.2.1&auto=format&fit=crop&w=800&q=60"
    ),
    Route(
        "Lakeside Trail",
        "5.50 km",
        "0h 45m",
        "15 m",
        "Moderate",
        "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?ixlib=rb-1.2.1&auto=format&fit=crop&w=800&q=60"
    )
)

@Composable
fun RouteCard(route: Route) {
    Card(
        modifier = Modifier
            .width(320.dp)
            .height(380.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.72f)
            ) {
                AsyncImage(
                    model = route.imageUrl,
                    contentDescription = route.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // Bookmark Icon in top right
                IconButton(
                    onClick = { },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        Icons.Rounded.BookmarkBorder,
                        contentDescription = "Save",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.28f)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = route.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 22.sp
                    )
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Difficulty Badge
                    Surface(
                        color = Color(0xFF4CAF50).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = route.difficulty,
                            color = Color(0xFF81C784),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Text(
                        text = "${route.distance} (${route.time}) • ${route.elevation}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

data class Workout(
    val title: String,
    val description: String,
    val duration: String,
    val icon: ImageVector,
    val color: Color
)

val workoutList = listOf(
    Workout(
        "Brisk Walk",
        "Keep your body moving with a brisk walk. Maintain activity levels and e...",
        "30m",
        Icons.AutoMirrored.Rounded.DirectionsWalk,
        Color(0xFF4A148C)
    ),
    Workout(
        "Easy Jog",
        "A light jog to get your heart rate up. Perfect for beginners...",
        "20m",
        Icons.AutoMirrored.Rounded.DirectionsRun,
        Color(0xFF0D47A1)
    )
)

@Composable
fun WorkoutCard(workout: Workout) {
    Card(
        modifier = Modifier
            .width(320.dp)
            .height(130.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(workout.color.lighten(0.3f), workout.color)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        workout.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = workout.duration,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = workout.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 20.sp
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = workout.description,
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
            }

            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

fun Color.lighten(factor: Float): Color {
    return Color(
        red = (red + (1f - red) * factor),
        green = (green + (1f - green) * factor),
        blue = (blue + (1f - blue) * factor),
        alpha = alpha
    )
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp", backgroundColor = 0xFF000000)
@Composable
fun HomeScreenPreview() {
    HomeScreen(navController = rememberNavController())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavController) {
    val tabTitles = listOf("Friends", "Clubs")
    var selectedTab by remember { mutableStateOf(0) }
    var searchText by remember { mutableStateOf("") }
    var followedPeople by remember { mutableStateOf(setOf<String>()) }
    val people = listOf(
        Triple("Anette Visser", "Hantum, Friesland", "Fan favorite on Strava"),
        Triple("Liyana Rahman", "Jerantut, Pahang", "Local Legend near you"),
        Triple("Mohd Khairol Azani", "Local Legend near you", ""),
        Triple("Helly M", "Marin, CA", "Fan favorite on Strava"),
        Triple("boy ezwan", "Local Legend near you", "")
    )
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate(Screen.Home.route) }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF181818))
            )
        },
        containerColor = Color.Black,
        bottomBar = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
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
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Black,
                contentColor = Color.White
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                color = if (selectedTab == index) Color.White else Color(0xFFBDBDBD),
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        selectedContentColor = Color.White,
                        unselectedContentColor = Color(0xFFBDBDBD)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                TextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text("Search on Enerva", color = Color(0xFFBDBDBD)) },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null, tint = Color(0xFFBDBDBD))
                    },
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(16.dp))
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
                        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(32.dp))
                        Text(label, color = Color.White, fontSize = 14.sp, fontWeight = if (label == "Suggest") FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "PEOPLE YOU MAY KNOW",
                color = Color(0xFFBDBDBD),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                val filteredPeople = people.filter { (name, _, _) ->
                    name.contains(searchText, ignoreCase = true)
                }
                
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
                                color = Color(0xFFBDBDBD),
                                fontSize = 16.sp
                            )
                        }
                    }
                } else {
                    items(filteredPeople) { (name, location, subtitle) ->
                        val isFollowed = followedPeople.contains(name)
                        Card(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF181818)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(name, color = Color.White, fontWeight = FontWeight.Bold)
                                    Text(location, color = Color(0xFFBDBDBD), fontSize = 13.sp)
                                    if (subtitle.isNotEmpty()) Text(subtitle, color = Color(0xFFFF9800), fontSize = 12.sp)
                                }
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
                                        color = Color.White
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
@Composable
fun MapsScreen() {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Text(text = "Maps Screen", style = MaterialTheme.typography.headlineMedium, color = Color.White)
    }
}

@Composable
fun RecordScreen() {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Text(text = "Record Screen", style = MaterialTheme.typography.headlineMedium, color = Color.White)
    }
}

@Composable
fun GroupsScreen() {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Text(text = "Groups Screen", style = MaterialTheme.typography.headlineMedium, color = Color.White)
    }
}

@Composable
fun YouScreen() {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Text(text = "You Screen", style = MaterialTheme.typography.headlineMedium, color = Color.White)
    }
}
