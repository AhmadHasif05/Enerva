// Package — UI "screen" layer.
package com.example.a211198_hasif_drnelson_Project2.view.screen

// ---- Compose & Android imports ----
import androidx.compose.foundation.background                       // background colour fill
import androidx.compose.foundation.clickable                        // makes a row/box tappable
import androidx.compose.foundation.layout.*                         // Row, Column, Box, Spacer, padding, size…
import androidx.compose.foundation.lazy.LazyColumn                  // efficient scrolling list
import androidx.compose.foundation.lazy.items                       // emits a list into a LazyColumn
import androidx.compose.foundation.shape.CircleShape                // round avatar shape
import androidx.compose.material.icons.Icons                        // Material icon set entry point
import androidx.compose.material.icons.automirrored.filled.ArrowBack // RTL-aware back arrow
import androidx.compose.material.icons.filled.EditNote              // "new message" / empty-state icon
import androidx.compose.material.icons.filled.GroupAdd              // "new group" icon
import androidx.compose.material.icons.rounded.Person               // avatar placeholder icon
import androidx.compose.material3.*                                 // Scaffold, TopAppBar, Text, AlertDialog…
import androidx.compose.runtime.Composable                         // marks a function as composable UI
import androidx.compose.runtime.getValue                           // property-delegate read for state (by)
import androidx.compose.runtime.mutableStateOf                     // creates observable state
import androidx.compose.runtime.remember                           // remembers state across recompositions
import androidx.compose.runtime.setValue                           // property-delegate write for state (by)
import androidx.compose.runtime.toMutableStateList                 // turns a list into observable SnapshotStateList
import androidx.compose.ui.Alignment                               // Center/Start/End alignment
import androidx.compose.ui.Modifier                                // styling/layout modifier chain
import androidx.compose.ui.draw.clip                               // clip composable to a shape
import androidx.compose.ui.text.font.FontWeight                    // bold/normal weights
import androidx.compose.ui.text.style.TextAlign                    // text alignment (centre the empty state)
import androidx.compose.ui.text.style.TextOverflow                 // ellipsis for overflowing preview text
import androidx.compose.ui.tooling.preview.Preview                 // Studio design preview
import androidx.compose.ui.unit.dp                                 // dp sizes
import androidx.compose.ui.unit.sp                                 // sp font sizes
import androidx.lifecycle.viewmodel.compose.viewModel              // obtain the screen's ViewModel
import androidx.navigation.NavController                           // navigate to chat / search
import androidx.navigation.compose.rememberNavController           // fake controller for @Preview
import com.example.a211198_hasif_drnelson_Project2.model.Conversation        // a chat thread model
import com.example.a211198_hasif_drnelson_Project2.view.Screen                // route constants
import com.example.a211198_hasif_drnelson_Project2.view.chatRoute             // builds the "chat/{name}" route
import com.example.a211198_hasif_drnelson_Project2.view_model.MessageViewModel // holds the conversation list
import java.text.SimpleDateFormat                                  // formats the last-message time
import java.util.Date                                             // wraps epoch millis
import java.util.Locale                                           // device locale for time formatting

// MessageScreen — the inbox: a list of conversations (direct + group), with
// actions to start a new message (go to Search) or create a group chat.
@OptIn(ExperimentalMaterial3Api::class) // TopAppBar is an experimental Material3 API
@Composable
fun MessageScreen(
    navController: NavController,                              // for navigating to a chat / search
    messageViewModel: MessageViewModel = viewModel()          // supplies the conversation list + group creation
) {
    val colors = MaterialTheme.colorScheme                    // theme palette
    val conversations = messageViewModel.conversationList     // all threads, newest-active first

    // Controls the "New Group" dialog.
    var showGroupDialog by remember { mutableStateOf(false) } // false = hidden, true = shown

    if (showGroupDialog) {                                    // only compose the dialog when requested
        NewGroupDialog(
            friendNames = conversations.filter { !it.isGroup }.map { it.friendName }, // only direct chats can join a group
            onDismiss = { showGroupDialog = false },          // close without creating
            onCreate = { groupName, members ->
                messageViewModel.createGroup(groupName, members) // create the group thread
                showGroupDialog = false                       // then close the dialog
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
                navigationIcon = {                            // back button
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.onBackground
                        )
                    }
                },
                actions = {                                   // right-side actions
                    IconButton(onClick = { navController.navigate(Screen.Search.route) }) { // start a new 1:1 chat
                        Icon(Icons.Default.EditNote, contentDescription = "New Message", tint = colors.onBackground)
                    }
                    IconButton(onClick = { showGroupDialog = true }) { // open the group-create dialog
                        Icon(Icons.Default.GroupAdd, contentDescription = "New Group", tint = colors.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        },
        containerColor = colors.background
    ) { paddingValues ->                                      // padding reserved for the top bar
        if (conversations.isEmpty()) {                        // nothing yet → show the empty state
            EmptyMessages(modifier = Modifier.padding(paddingValues))
        } else {                                              // otherwise list the conversations
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(conversations, key = { it.friendName }) { conversation -> // key = friend/group name
                    ConversationRow(
                        conversation = conversation,
                        onClick = { navController.navigate(chatRoute(conversation.friendName)) } // open that chat
                    )
                    HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f)) // thin separator between rows
                }
            }
        }
    }
}

// One inbox row: avatar + name + last-message preview + time.
@Composable
private fun ConversationRow(
    conversation: Conversation,
    onClick: () -> Unit                                       // tap → open the chat
) {
    val colors = MaterialTheme.colorScheme
    val last = conversation.lastMessage                       // most recent message (null if none yet)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)                     // whole row is tappable
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(                                                  // round avatar placeholder
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(colors.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Person, contentDescription = null, tint = colors.onSurface)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {              // name + preview take the remaining width
            Text(
                conversation.friendName,                      // friend or group name
                color = colors.onBackground,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = last?.let {                            // build the preview line:
                    if (it.fromMe) "You: ${it.text}" else it.text // prefix "You:" for my own last message
                } ?: "Started following you",                 // fallback when there are no messages yet
                color = colors.onSurfaceVariant,
                fontSize = 13.sp,
                maxLines = 1,                                  // keep the preview to a single line
                overflow = TextOverflow.Ellipsis              // … when it's too long
            )
        }
        if (last != null) {                                   // only show a time when a message exists
            Text(
                text = formatTimestamp(last.timestampMs),
                color = colors.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

// Friendly empty state shown when the user has no conversations yet.
@Composable
private fun EmptyMessages(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,   // centre everything
        verticalArrangement = Arrangement.Center
    ) {
        Box(                                                  // large tinted icon badge
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(colors.primary.copy(alpha = 0.15f)), // faint brand-tinted circle
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
    friendNames: List<String>,                               // candidate members (your direct-chat friends)
    onDismiss: () -> Unit,                                   // cancel / outside-tap
    onCreate: (groupName: String, members: List<String>) -> Unit // confirm with the chosen name + members
) {
    val colors = MaterialTheme.colorScheme
    var groupName by remember { mutableStateOf("") }          // the typed group name
    // Tracks which friends are checked. A SnapshotStateList so the dialog recomposes on toggle.
    val selected = remember { mutableListOf<String>().toMutableStateList() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Group") },
        text = {
            Column {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },       // keep the name in sync with typing
                    label = { Text("Group name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (friendNames.isEmpty()) {                  // no friends to add yet → hint instead of a list
                    Text(
                        "Follow some runners first — they'll appear here to add to a group.",
                        color = colors.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                } else {
                    Text("Add friends", color = colors.onSurfaceVariant, fontSize = 13.sp)
                    friendNames.forEach { name ->             // one checkable row per friend
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {                  // tapping the row toggles selection too
                                    if (selected.contains(name)) selected.remove(name) else selected.add(name)
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selected.contains(name), // reflects current selection
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
                onClick = { onCreate(groupName, selected.toList()) }, // hand back a snapshot copy of the members
                enabled = groupName.isNotBlank() && selected.isNotEmpty() // need a name and ≥1 member
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// Format the last-message time as "HH:mm" in the device locale.
private fun formatTimestamp(ms: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ms))

// Design-time preview for Android Studio.
@Preview
@Composable
fun MessageScreenPreview() {
    MessageScreen(rememberNavController())
}
