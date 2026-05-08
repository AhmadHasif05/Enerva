package com.example.a211198_hasif_drnelson_Project1.view.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.a211198_hasif_drnelson_Project1.R
import com.example.a211198_hasif_drnelson_Project1.model.ActivityRecord
import com.example.a211198_hasif_drnelson_Project1.model.sampleActivityRecords
import com.example.a211198_hasif_drnelson_Project1.view.Screen
import com.example.a211198_hasif_drnelson_Project1.view_model.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouScreen(navController: NavController, userViewModel: UserViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Progress", "Workouts", "Activities")
    val colors = MaterialTheme.colorScheme

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "You",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.onBackground
                        )
                    )
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceVariant)
                            .clickable { navController.navigate(Screen.Profile.route) }
                    ) {
                        AsyncImage(
                            model = R.drawable.hasif_profile,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    IconButton(onClick = { navController.navigate(Screen.Search.route) }) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = colors.onBackground)
                    }
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = colors.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        },
        containerColor = colors.background
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = colors.background,
                contentColor = colors.primary,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = colors.primary
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                color = if (selectedTab == index) colors.onBackground else colors.onSurfaceVariant,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> ProgressTab(records = sampleActivityRecords)
                1 -> WorkoutsTab(onStart = { navController.navigate(Screen.Record.route) })
                else -> ActivitiesTab(
                    records = sampleActivityRecords,
                    onSeeAll = { navController.navigate(Screen.Activity.route) }
                )
            }
        }
    }
}

// ---------- Progress tab ----------
@Composable
private fun ProgressTab(records: List<ActivityRecord>) {
    val colors = MaterialTheme.colorScheme

    val totalKm = records.sumOf { it.distanceKm }
    val totalMin = records.sumOf { it.durationMinutes }
    val totalElev = records.sumOf { it.elevationM }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, colors.primary),
                color = Color.Transparent,
                modifier = Modifier.wrapContentSize()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.DirectionsRun,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Run", color = colors.onBackground, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "This week",
                color = colors.onBackground,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                WeeklyStatItem("Distance", "%.1f km".format(totalKm), Modifier.weight(1f))
                WeeklyStatItem("Time", "${totalMin}m", Modifier.weight(1f))
                WeeklyStatItem("Elev Gain", "$totalElev m", Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text("Past 7 days", color = colors.onSurfaceVariant, fontSize = 14.sp)

            Spacer(modifier = Modifier.height(8.dp))

            // Bar chart of distance per day (uses up to 7 records)
            BarChart(
                values = records.take(7).map { it.distanceKm.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                    Text(day, color = colors.onSurfaceVariant, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = colors.surfaceVariant, thickness = 8.dp)
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "April 2026",
                color = colors.onBackground,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                WeeklyStatItem("Your Streak", "${records.size} Days", Modifier.weight(1f))
                WeeklyStatItem("Activities", "${records.size}", Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun BarChart(values: List<Float>, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val max = (values.maxOrNull() ?: 1f).coerceAtLeast(1f)

    Row(
        modifier = modifier.padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        repeat(7) { i ->
            val v = values.getOrNull(i) ?: 0f
            val pct = (v / max).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .width(20.dp)
                    .fillMaxHeight(if (pct > 0f) pct else 0.05f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (v > 0f) colors.primary else colors.surfaceVariant)
            )
        }
    }
}

// ---------- Workouts tab ----------
private data class QuickWorkout(
    val title: String,
    val description: String,
    val duration: String,
    val icon: ImageVector
)

private val quickWorkouts = listOf(
    QuickWorkout("Brisk Walk", "Keep moving with a brisk walk.", "30m", Icons.AutoMirrored.Filled.DirectionsWalk),
    QuickWorkout("Easy Jog", "A light jog to get your heart rate up.", "20m", Icons.AutoMirrored.Filled.DirectionsRun),
    QuickWorkout("Hill Repeats", "Short bursts on rolling hills.", "35m", Icons.Rounded.Whatshot),
    QuickWorkout("Cycle Cruise", "A steady-state ride to build endurance.", "45m", Icons.AutoMirrored.Filled.DirectionsBike)
)

@Composable
private fun WorkoutsTab(onStart: () -> Unit) {
    val colors = MaterialTheme.colorScheme

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Text(
                "Quick workouts",
                color = colors.onBackground,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Pick one and start tracking right away.",
                color = colors.onSurfaceVariant,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        items(quickWorkouts) { workout ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(colors.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(workout.icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(workout.title, color = colors.onSurface, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(workout.description, color = colors.onSurfaceVariant, fontSize = 13.sp)
                        Text(workout.duration, color = colors.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    FilledIconButton(
                        onClick = onStart,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = colors.primary,
                            contentColor = colors.onPrimary
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Start")
                    }
                }
            }
        }
    }
}

// ---------- Activities tab ----------
@Composable
private fun ActivitiesTab(records: List<ActivityRecord>, onSeeAll: () -> Unit) {
    val colors = MaterialTheme.colorScheme

    if (records.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No activities yet.", color = colors.onSurfaceVariant)
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Recent activities",
                    color = colors.onBackground,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onSeeAll) {
                    Text("See all", color = colors.primary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        items(records, key = { it.id }) { record ->
            YouActivityCard(record)
        }
    }
}

@Composable
private fun YouActivityCard(record: ActivityRecord) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    iconForActivityType(record.type),
                    contentDescription = record.type,
                    tint = colors.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(record.title, color = colors.onSurface, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(record.date, color = colors.onSurfaceVariant, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row {
                    Text("${record.distanceKm} km", color = colors.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("${record.durationMinutes}m", color = colors.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(record.avgPace, color = colors.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Icon(Icons.Default.Timeline, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}

private fun iconForActivityType(type: String): ImageVector = when (type.lowercase()) {
    "ride", "bike" -> Icons.AutoMirrored.Filled.DirectionsBike
    "walk" -> Icons.AutoMirrored.Filled.DirectionsWalk
    else -> Icons.AutoMirrored.Filled.DirectionsRun
}

@Composable
private fun WeeklyStatItem(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier) {
        Text(label, color = colors.onSurfaceVariant, fontSize = 12.sp)
        Text(value, color = colors.onBackground, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}