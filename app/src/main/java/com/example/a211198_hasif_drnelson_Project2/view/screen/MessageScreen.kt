package com.example.a211198_hasif_drnelson_Project2.view.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.a211198_hasif_drnelson_Project2.model.Conversation
import com.example.a211198_hasif_drnelson_Project2.view.Screen
import com.example.a211198_hasif_drnelson_Project2.view.chatRoute
import com.example.a211198_hasif_drnelson_Project2.view_model.MessageViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageScreen(
    navController: NavController,
    messageViewModel: MessageViewModel = viewModel()
) {
    val colors = MaterialTheme.colorScheme
    val conversations = messageViewModel.conversationList

    // Controls the "New Group" dialog.
    var showGroupDialog by remember { mutableStateOf(false) }

    if (showGroupDialog) {
        NewGroupDialog(
            friendNames = conversations.filter { !it.isGroup }.map { it.friendName },
            onDismiss = { showGroupDialog = false },
            onCreate = { groupName, members ->
                messageViewModel.createGroup(groupName, members)
                showGroupDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Messages",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.onBackground
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.onBackground
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Search.route) }) {
                        Icon(Icons.Default.EditNote, contentDescription = "New Message", tint = colors.onBackground)
                    }
                    IconButton(onClick = { showGroupDialog = true }) {
                        Icon(Icons.Default.GroupAdd, contentDescription = "New Group", tint = colors.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        },
        containerColor = colors.background
    ) { paddingValues ->
        if (conversations.isEmpty()) {
            EmptyMessages(modifier = Modifier.padding(paddingValues))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(conversations, key = { it.friendName }) { conversation ->
                    ConversationRow(
                        conversation = conversation,
                        onClick = { navController.navigate(chatRoute(conversation.friendName)) }
                    )
                    HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(
    conversation: Conversation,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val last = conversation.lastMessage

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(colors.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Person, contentDescription = null, tint = colors.onSurface)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                conversation.friendName,
                color = colors.onBackground,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = last?.let {
                    if (it.fromMe) "You: ${it.text}" else it.text
                } ?: "Started following you",
                color = colors.onSurfaceVariant,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (last != null) {
            Text(
                text = formatTimestamp(last.timestampMs),
                color = colors.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun EmptyMessages(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(colors.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.EditNote,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(56.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Start Messaging!",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = colors.onBackground
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Follow a runner from Search and your chats will appear here.",
            style = MaterialTheme.typography.bodyMedium.copy(color = colors.onSurfaceVariant),
            textAlign = TextAlign.Center
        )
    }
}

// Dialog to create a group chat from the friends you already message.
// Pick a name + at least one member, then Create.
@Composable
private fun NewGroupDialog(
    friendNames: List<String>,
    onDismiss: () -> Unit,
    onCreate: (groupName: String, members: List<String>) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var groupName by remember { mutableStateOf("") }
    // Tracks which friends are checked. A SnapshotStateList so the dialog recomposes on toggle.
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
                Spacer(modifier = Modifier.height(12.dp))
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

private fun formatTimestamp(ms: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ms))

@Preview
@Composable
fun MessageScreenPreview() {
    MessageScreen(rememberNavController())
}