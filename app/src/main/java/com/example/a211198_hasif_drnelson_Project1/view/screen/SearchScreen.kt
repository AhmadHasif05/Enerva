package com.example.a211198_hasif_drnelson_Project1.view.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Facebook
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.a211198_hasif_drnelson_Project1.view.Screen
import com.example.a211198_hasif_drnelson_Project1.view.chatRoute
import com.example.a211198_hasif_drnelson_Project1.view_model.MessageViewModel
import com.example.a211198_hasif_drnelson_Project1.view_model.UserViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    userViewModel: UserViewModel = viewModel(),
    messageViewModel: MessageViewModel = viewModel()
) {
    val tabTitles = listOf("Friends", "Clubs")
    var selectedTab by remember { mutableStateOf(0) }
    var searchText by remember { mutableStateOf("") }
    val colors = MaterialTheme.colorScheme
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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
                title = { Text("Search", color = colors.onSurface) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate(Screen.Home.route) }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = colors.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface)
            )
        },
        containerColor = colors.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(colors.background)
                    .padding(16.dp)
            ) {
                Button(
                    onClick = { navController.navigate(Screen.Messages.route) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary
                    )
                ) {
                    Text("Invite Friends")
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
                containerColor = colors.background,
                contentColor = colors.primary
            ) {
                tabTitles.forEachIndexed { index, title ->
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
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                TextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text("Search on Enerva", color = colors.onSurfaceVariant) },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.onSurfaceVariant)
                    },
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = colors.surface,
                        unfocusedContainerColor = colors.surface,
                        disabledContainerColor = colors.surface,
                    )
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
                        Icon(icon, contentDescription = label, tint = colors.onBackground, modifier = Modifier.size(32.dp))
                        Text(label, color = colors.onBackground, fontSize = 14.sp, fontWeight = if (label == "Suggest") FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "PEOPLE YOU MAY KNOW",
                color = colors.onSurfaceVariant,
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
                                color = colors.onSurfaceVariant,
                                fontSize = 16.sp
                            )
                        }
                    }
                } else {
                    items(filteredPeople) { (name, location, subtitle) ->
                        val isFollowed = userViewModel.isFollowing(name)
                        Card(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = colors.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Person, contentDescription = null, tint = colors.onSurface, modifier = Modifier.size(40.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(name, color = colors.onSurface, fontWeight = FontWeight.Bold)
                                    Text(location, color = colors.onSurfaceVariant, fontSize = 13.sp)
                                    if (subtitle.isNotEmpty()) Text(subtitle, color = colors.primary, fontSize = 12.sp)
                                }
                                Button(
                                    onClick = {
                                        val wasFollowing = userViewModel.isFollowing(name)
                                        userViewModel.toggleFollow(name)
                                        if (!wasFollowing) {
                                            // Just followed → seed a conversation and prompt to chat.
                                            messageViewModel.startConversationWith(name)
                                            scope.launch {
                                                val result = snackbarHostState.showSnackbar(
                                                    message = "You are now following $name",
                                                    actionLabel = "Message",
                                                    duration = SnackbarDuration.Short
                                                )
                                                if (result == SnackbarResult.ActionPerformed) {
                                                    navController.navigate(chatRoute(name))
                                                }
                                            }
                                        } else {
                                            // Unfollowed → drop the conversation.
                                            messageViewModel.removeConversation(name)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isFollowed) colors.primary else Color.Transparent,
                                        contentColor = if (isFollowed) colors.onPrimary else colors.primary
                                    ),
                                    border = if (isFollowed) null else BorderStroke(1.dp, colors.primary),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                                ) {
                                    Text(if (isFollowed) "Followed" else "Follow")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}