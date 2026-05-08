package com.example.a211198_hasif_drnelson_Project1.view.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.a211198_hasif_drnelson_Project1.model.Challenge
import com.example.a211198_hasif_drnelson_Project1.model.Club
import com.example.a211198_hasif_drnelson_Project1.view.Screen
import com.example.a211198_hasif_drnelson_Project1.view_model.ChallengeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    navController: NavController,
    challengeViewModel: ChallengeViewModel = viewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Active", "Challenges", "Clubs")
    val colors = MaterialTheme.colorScheme

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Groups",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.onBackground
                        )
                    )
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Messages.route) }) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Messages", tint = colors.onBackground)
                    }
                    IconButton(onClick = { navController.navigate(Screen.Search.route) }) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = colors.onBackground)
                    }
                    Box(contentAlignment = Alignment.TopEnd) {
                        IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = colors.onBackground)
                        }
                        Box(
                            modifier = Modifier
                                .padding(top = 8.dp, end = 8.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(colors.error)
                        )
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
                0 -> ActiveTab(challengeViewModel)
                1 -> ChallengesTab(challengeViewModel)
                else -> ClubsTab(challengeViewModel)
            }
        }
    }
}

// --- Active tab: joined challenges with progress ---
@Composable
private fun ActiveTab(vm: ChallengeViewModel) {
    val colors = MaterialTheme.colorScheme
    val joined = vm.joinedChallenges

    if (joined.isEmpty()) {
        EmptyState(
            icon = Icons.AutoMirrored.Filled.DirectionsRun,
            title = "No active challenges",
            subtitle = "Join one from the Challenges tab to start tracking your progress."
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Text(
                "Your active challenges",
                color = colors.onBackground,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        items(joined, key = { it.id }) { challenge ->
            ActiveChallengeCard(
                challenge = challenge,
                progress = vm.progressFor(challenge.id),
                onLogRun = { vm.bumpProgress(challenge.id) },
                onLeave = { vm.toggleJoinChallenge(challenge.id) }
            )
        }
    }
}

// --- Challenges tab: full list with Join button ---
@Composable
private fun ChallengesTab(vm: ChallengeViewModel) {
    val colors = MaterialTheme.colorScheme

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Text(
                "Available challenges",
                color = colors.onBackground,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        items(vm.challenges, key = { it.id }) { challenge ->
            ChallengeRow(
                challenge = challenge,
                onToggleJoin = { vm.toggleJoinChallenge(challenge.id) }
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// --- Clubs tab ---
@Composable
private fun ClubsTab(vm: ChallengeViewModel) {
    val colors = MaterialTheme.colorScheme

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Text(
                "Clubs near you",
                color = colors.onBackground,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        items(vm.clubs, key = { it.id }) { club ->
            ClubRow(
                club = club,
                onToggleJoin = { vm.toggleJoinClub(club.id) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ChallengeRow(
    challenge: Challenge,
    onToggleJoin: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.primary.copy(alpha = 0.2f))
                .border(1.dp, colors.primary, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(challenge.badge, color = colors.onBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Icon(
                    Icons.AutoMirrored.Filled.DirectionsRun,
                    contentDescription = null,
                    tint = colors.onBackground,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(challenge.title, color = colors.onBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(challenge.description, color = colors.onSurfaceVariant, fontSize = 13.sp)
            Text(
                "${challenge.startDate} – ${challenge.endDate}",
                color = colors.onSurfaceVariant,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Button(
            onClick = onToggleJoin,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (challenge.isJoined) Color.Transparent else colors.primary,
                contentColor = if (challenge.isJoined) colors.primary else colors.onPrimary
            ),
            border = if (challenge.isJoined)
                androidx.compose.foundation.BorderStroke(1.dp, colors.primary)
            else null,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Icon(
                if (challenge.isJoined) Icons.Default.Check else Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(if (challenge.isJoined) "Joined" else "Join", fontSize = 13.sp)
        }
    }
}

@Composable
private fun ActiveChallengeCard(
    challenge: Challenge,
    progress: Float,
    onLogRun: () -> Unit,
    onLeave: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.primary.copy(alpha = 0.2f))
                        .border(1.dp, colors.primary, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(challenge.badge, color = colors.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(challenge.title, color = colors.onSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(challenge.description, color = colors.onSurfaceVariant, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                color = colors.primary,
                trackColor = colors.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "${(progress * 100).toInt()}% complete",
                color = colors.onSurfaceVariant,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onLogRun,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Log run", fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = onLeave,
                    modifier = Modifier.weight(1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.outline),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.onSurface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Leave", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun ClubRow(club: Club, onToggleJoin: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Group, contentDescription = null, tint = colors.primary)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(club.name, color = colors.onSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(club.description, color = colors.onSurfaceVariant, fontSize = 12.sp)
                Text(
                    "${club.location} • ${club.memberCount} members",
                    color = colors.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
            Button(
                onClick = onToggleJoin,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (club.isJoined) Color.Transparent else colors.primary,
                    contentColor = if (club.isJoined) colors.primary else colors.onPrimary
                ),
                border = if (club.isJoined)
                    androidx.compose.foundation.BorderStroke(1.dp, colors.primary)
                else null,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(if (club.isJoined) "Joined" else "Join", fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun EmptyState(icon: ImageVector, title: String, subtitle: String) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(colors.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(48.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(title, color = colors.onBackground, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(subtitle, color = colors.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp))
    }
}

@Preview
@Composable
fun GroupsScreenPreview() {
    GroupsScreen(rememberNavController())
}
