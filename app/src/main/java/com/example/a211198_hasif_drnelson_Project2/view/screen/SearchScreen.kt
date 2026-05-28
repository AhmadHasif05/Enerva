package com.example.a211198_hasif_drnelson_Project2.view.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.a211198_hasif_drnelson_Project2.view.Screen
import com.example.a211198_hasif_drnelson_Project2.view.chatRoute
import com.example.a211198_hasif_drnelson_Project2.view_model.MessageViewModel
import com.example.a211198_hasif_drnelson_Project2.view_model.UserViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    userViewModel: UserViewModel = viewModel(),
    messageViewModel: MessageViewModel = viewModel()
) {
    val tabTitles = listOf("Friends", "Group")
    var selectedTab by remember { mutableStateOf(0) }
    var searchText by remember { mutableStateOf("") }
    val colors = MaterialTheme.colorScheme
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // "New Group" dialog — shared with MessageScreen's flow via createGroup().
    var showGroupDialog by remember { mutableStateOf(false) }
    if (showGroupDialog) {
        // Only direct-chat friends can be added as group members.
        val candidateFriends = messageViewModel.conversationList
            .filter { !it.isGroup }
            .map { it.friendName }
        NewGroupDialog(
            friendNames = candidateFriends,
            onDismiss = { showGroupDialog = false },
            onCreate = { name, members ->
                messageViewModel.createGroup(name, members)
                showGroupDialog = false
                // Jump straight into the newly-created group chat.
                navController.navigate(chatRoute(name))
            }
        )
    }

    val people = listOf(
        Triple("Anette Visser", "Hantum, Friesland", "Fan favorite on Strava"),
        Triple("Liyana Rahman", "Jerantut, Pahang", "Local Legend near you"),
        Triple("Mohd Khairol Azani", "Local Legend near you", ""),
        Triple("Helly M", "Marin, CA", "Fan favorite on Strava"),
        Triple("boy ezwan", "Local Legend near you", "")
    )

    // Suggested groups the user can one-tap join. Each entry carries default
    // members so the resulting Conversation has a member list to display.
    val suggestedGroups = listOf(
        SuggestedGroup("KL Morning Runners", "Daily 6 AM loops around KLCC", listOf("Anette Visser", "Liyana Rahman", "boy ezwan")),
        SuggestedGroup("Trail Junkies MY", "Weekend trails — Gasing, Broga, Bukit Tabur", listOf("Helly M", "Mohd Khairol Azani")),
        SuggestedGroup("Sub-30 5K Club", "Speedwork & tempo sessions", listOf("Sarah Tan", "Daniel Lee", "Liyana Rahman")),
        SuggestedGroup("Sunset Cyclists", "Easy spins around the city", listOf("Daniel Lee", "Aisha Rahman"))
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
                // Friends tab → jump to Messages. Group tab → open the create-group dialog.
                Button(
                    onClick = {
                        if (selectedTab == 0) navController.navigate(Screen.Messages.route)
                        else showGroupDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary
                    )
                ) {
                    Text(if (selectedTab == 0) "Invite Friends" else "Create New Group")
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
                    placeholder = {
                        Text(
                            if (selectedTab == 0) "Search on Enerva" else "Search groups",
                            color = colors.onSurfaceVariant
                        )
                    },
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

            if (selectedTab == 0) {
                FriendsContent(
                    people = people,
                    searchText = searchText,
                    userViewModel = userViewModel,
                    onFollow = { name ->
                        val wasFollowing = userViewModel.isFollowing(name)
                        userViewModel.toggleFollow(name)
                        if (!wasFollowing) {
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
                            messageViewModel.removeConversation(name)
                        }
                    }
                )
            } else {
                GroupsContent(
                    groups = messageViewModel.groupConversations,
                    suggestedGroups = suggestedGroups,
                    searchText = searchText,
                    onCreate = { showGroupDialog = true },
                    onOpenGroup = { groupName -> navController.navigate(chatRoute(groupName)) },
                    onJoin = { name, members ->
                        messageViewModel.createGroup(name, members)
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Joined \"$name\"",
                                actionLabel = "Open",
                                duration = SnackbarDuration.Short
                            ).let { result ->
                                if (result == SnackbarResult.ActionPerformed) {
                                    navController.navigate(chatRoute(name))
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.FriendsContent(
    people: List<Triple<String, String, String>>,
    searchText: String,
    userViewModel: UserViewModel,
    onFollow: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
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
                    Text("No results found for \"$searchText\"", color = colors.onSurfaceVariant, fontSize = 16.sp)
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
                            onClick = { onFollow(name) },
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

// Lightweight model for a "suggested group" card on the Group tab.
data class SuggestedGroup(
    val name: String,
    val description: String,
    val members: List<String>
)

@Composable
private fun ColumnScope.GroupsContent(
    groups: List<com.example.a211198_hasif_drnelson_Project2.model.Conversation>,
    suggestedGroups: List<SuggestedGroup>,
    searchText: String,
    onCreate: () -> Unit,
    onOpenGroup: (String) -> Unit,
    onJoin: (String, List<String>) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val joinedNames = groups.map { it.friendName }.toSet()

    // Create-new-group card sits at the top of the list.
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onCreate() },
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.GroupAdd, contentDescription = null, tint = colors.primary)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Create New Group", color = colors.onSurface, fontWeight = FontWeight.Bold)
                Text("Build a group chat from friends you follow", color = colors.onSurfaceVariant, fontSize = 13.sp)
            }
            OutlinedButton(
                onClick = onCreate,
                border = BorderStroke(1.dp, colors.primary),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
            ) {
                Text("New")
            }
        }
    }

    val filteredYourGroups = groups.filter { it.friendName.contains(searchText, ignoreCase = true) }
    val filteredSuggested = suggestedGroups.filter {
        it.name.contains(searchText, ignoreCase = true) ||
            it.description.contains(searchText, ignoreCase = true)
    }

    LazyColumn(
        Modifier
            .weight(1f)
            .padding(horizontal = 8.dp)
    ) {
        // ─── SUGGESTED GROUPS ────────────────────────────────────────────
        item {
            Spacer(Modifier.height(16.dp))
            Text(
                "SUGGESTED GROUPS",
                color = colors.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(Modifier.height(8.dp))
        }
        if (filteredSuggested.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No matching suggestions", color = colors.onSurfaceVariant, fontSize = 14.sp)
                }
            }
        } else {
            items(filteredSuggested, key = { "suggested:" + it.name }) { suggestion ->
                val joined = suggestion.name in joinedNames
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            if (joined) onOpenGroup(suggestion.name)
                            else onJoin(suggestion.name, suggestion.members)
                        },
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(colors.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Group, contentDescription = null, tint = colors.primary)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(suggestion.name, color = colors.onSurface, fontWeight = FontWeight.Bold)
                            Text(
                                suggestion.description,
                                color = colors.onSurfaceVariant,
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                            Text(
                                "${suggestion.members.size + 1} members",
                                color = colors.primary,
                                fontSize = 12.sp
                            )
                        }
                        Button(
                            onClick = {
                                if (joined) onOpenGroup(suggestion.name)
                                else onJoin(suggestion.name, suggestion.members)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (joined) colors.primary else Color.Transparent,
                                contentColor = if (joined) colors.onPrimary else colors.primary
                            ),
                            border = if (joined) null else BorderStroke(1.dp, colors.primary),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                        ) {
                            Text(if (joined) "Joined" else "Join")
                        }
                    }
                }
            }
        }

        // ─── YOUR GROUPS ─────────────────────────────────────────────────
        item {
            Spacer(Modifier.height(20.dp))
            Text(
                "YOUR GROUPS",
                color = colors.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(Modifier.height(8.dp))
        }
        if (filteredYourGroups.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (groups.isEmpty()) "No groups yet. Join a suggested group or tap \"Create New Group\"."
                        else "No groups match \"$searchText\"",
                        color = colors.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            items(filteredYourGroups, key = { "joined:" + it.friendName }) { group ->
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onOpenGroup(group.friendName) },
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Group, contentDescription = null, tint = colors.onSurface)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(group.friendName, color = colors.onSurface, fontWeight = FontWeight.Bold)
                            val subtitle = if (group.members.isNotEmpty()) {
                                "${group.members.size} members · ${group.members.take(2).joinToString(", ")}" +
                                    if (group.members.size > 2) ", …" else ""
                            } else {
                                group.lastMessage?.text ?: "Group chat"
                            }
                            Text(subtitle, color = colors.onSurfaceVariant, fontSize = 13.sp, maxLines = 1)
                        }
                        Text("Open", color = colors.primary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// Dialog mirrors the one in MessageScreen so users can create groups from
// the Search screen too — the underlying createGroup() is shared.
@Composable
private fun NewGroupDialog(
    friendNames: List<String>,
    onDismiss: () -> Unit,
    onCreate: (groupName: String, members: List<String>) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var groupName by remember { mutableStateOf("") }
    val selected = remember { mutableListOf<String>().toMutableStateList() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Group") },
        text = {
            Column {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Group name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                if (friendNames.isEmpty()) {
                    Text(
                        "Follow some runners first — they'll appear here to add to a group.",
                        color = colors.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                } else {
                    Text("Add friends", color = colors.onSurfaceVariant, fontSize = 13.sp)
                    friendNames.forEach { name ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (selected.contains(name)) selected.remove(name) else selected.add(name)
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selected.contains(name),
                                onCheckedChange = {
                                    if (it) selected.add(name) else selected.remove(name)
                                }
                            )
                            Text(name, color = colors.onBackground)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(groupName, selected.toList()) },
                enabled = groupName.isNotBlank() && selected.isNotEmpty()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
