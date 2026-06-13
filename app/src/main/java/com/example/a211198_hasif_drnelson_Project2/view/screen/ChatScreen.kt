// Package — this file lives in the UI "screen" layer (one composable per screen).
package com.example.a211198_hasif_drnelson_Project2.view.screen

// ---- Compose & Android imports ----
import androidx.compose.foundation.background                       // paints a background colour behind a composable
import androidx.compose.foundation.layout.*                         // Row, Column, Box, Spacer, padding, size, etc.
import androidx.compose.foundation.rememberScrollState              // remembers scroll position for verticalScroll
import androidx.compose.foundation.verticalScroll                   // makes a column scrollable (used for the empty state)
import androidx.compose.foundation.lazy.LazyColumn                  // recycler-style vertical list (only renders visible rows)
import androidx.compose.foundation.lazy.items                       // DSL helper to emit a list of items into a LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState       // remembers/controls the LazyColumn scroll state
import androidx.compose.foundation.shape.CircleShape                // perfectly round shape (the avatar circle)
import androidx.compose.foundation.shape.RoundedCornerShape         // rounded-corner shape (chat bubbles, text field)
import androidx.compose.material.icons.Icons                        // entry point to the Material icon set
import androidx.compose.material.icons.automirrored.filled.ArrowBack // back arrow that flips for right-to-left locales
import androidx.compose.material.icons.automirrored.filled.Send     // send (paper-plane) icon, RTL-aware
import androidx.compose.material.icons.filled.Refresh               // refresh icon for the top-bar action
import androidx.compose.material.icons.rounded.Person               // person silhouette used as the avatar placeholder
import androidx.compose.material3.*                                 // Scaffold, TopAppBar, Surface, Text, OutlinedTextField, etc.
import androidx.compose.material3.pulltorefresh.PullToRefreshBox    // swipe-down-to-refresh container
import androidx.compose.runtime.*                                   // remember, mutableStateOf, LaunchedEffect, Composable, etc.
import androidx.compose.ui.Alignment                                // alignment constants (Center, Start, End)
import androidx.compose.ui.Modifier                                 // modifier chain used to size/position/style composables
import androidx.compose.ui.draw.clip                                // clips a composable to a shape (round the avatar)
import androidx.compose.ui.text.font.FontWeight                     // bold/normal text weights
import androidx.compose.ui.tooling.preview.Preview                  // enables the Android Studio design preview
import androidx.compose.ui.unit.dp                                  // density-independent pixels (sizes/padding)
import androidx.compose.ui.unit.sp                                  // scale-independent pixels (font sizes)
import androidx.lifecycle.viewmodel.compose.viewModel               // obtains a ViewModel scoped to this composable
import androidx.navigation.NavController                            // drives navigation between screens
import androidx.navigation.compose.rememberNavController            // fake NavController for the @Preview
import com.example.a211198_hasif_drnelson_Project2.model.Message    // the chat-message data model
import com.example.a211198_hasif_drnelson_Project2.view_model.MessageViewModel // holds conversations + send/refresh logic
import kotlinx.coroutines.delay                                     // suspend-pause used to hold the refresh spinner
import kotlinx.coroutines.launch                                    // starts a coroutine (for the delayed spinner reset)
import java.text.SimpleDateFormat                                   // formats the message timestamp ("HH:mm")
import java.util.Date                                               // wraps the epoch-millis timestamp for formatting
import java.util.Locale                                             // device locale for the time format

// ChatScreen — the 1-to-1 conversation view with a single friend. Shows a header
// (avatar + name + back + refresh), the scrolling message history, and a bottom
// composer to type and send. All chat state lives in MessageViewModel.
@OptIn(ExperimentalMaterial3Api::class) // TopAppBar / PullToRefreshBox are still experimental APIs
@Composable
fun ChatScreen(
    navController: NavController,                              // used to go back when the user taps the arrow
    friendName: String,                                       // who we're chatting with (also the conversation key)
    messageViewModel: MessageViewModel = viewModel()          // source of truth for messages; defaulted so @Preview works
) {
    val colors = MaterialTheme.colorScheme                    // current theme palette (so colours follow light/dark)
    val conversation = messageViewModel.getConversation(friendName) // the Conversation object for this friend
    var draft by remember { mutableStateOf("") }              // the text currently typed in the composer
    val listState = rememberLazyListState()                   // lets us programmatically scroll the message list

    // Auto-scroll to the newest message whenever a message is added/removed.
    LaunchedEffect(conversation.messages.size) {              // re-runs every time the message count changes
        if (conversation.messages.isNotEmpty()) {             // nothing to scroll to on an empty chat
            listState.animateScrollToItem(conversation.messages.lastIndex) // smooth-scroll to the last bubble
        }
    }

    Scaffold(                                                 // standard screen skeleton: top bar + content + bottom bar
        topBar = {
            TopAppBar(
                title = {                                     // title area = avatar + friend's name
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(                                  // round avatar placeholder
                            modifier = Modifier
                                .size(32.dp)                  // 32dp circle
                                .clip(CircleShape)            // clip the box into a circle
                                .background(colors.surfaceVariant), // subtle grey fill
                            contentAlignment = Alignment.Center // centre the person icon inside
                        ) {
                            Icon(
                                Icons.Rounded.Person,         // person silhouette
                                contentDescription = null,    // decorative — no a11y label needed
                                tint = colors.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp)) // gap between avatar and name
                        Text(
                            friendName,                       // the friend's display name
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = colors.onBackground
                            )
                        )
                    }
                },
                navigationIcon = {                            // left side: back button
                    IconButton(onClick = { navController.popBackStack() }) { // pop this screen off the back stack
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.onBackground
                        )
                    }
                },
                actions = {                                   // right side: manual refresh button
                    IconButton(onClick = { messageViewModel.refresh() }) { // re-attach Firestore listeners / re-pull
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh messages",
                            tint = colors.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background) // match screen bg
            )
        },
        containerColor = colors.background,                   // screen background colour
        bottomBar = {                                         // bottom bar = the message composer
            ChatComposer(
                value = draft,
                onValueChange = { draft = it },               // keep the draft state in sync with typing
                onSend = {
                    if (draft.isNotBlank()) {                 // ignore empty/whitespace-only sends
                        messageViewModel.sendMessage(friendName, draft) // persist + push the message
                        draft = ""                            // clear the input after sending
                    }
                }
            )
        }
    ) { innerPadding ->                                       // innerPadding = space reserved for the top/bottom bars
        var isRefreshing by remember { mutableStateOf(false) } // drives the pull-to-refresh spinner
        val scope = rememberCoroutineScope()                  // coroutine scope tied to this composable's lifetime
        PullToRefreshBox(                                     // wraps the list so a downward swipe triggers refresh
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true                           // show the spinner immediately
                messageViewModel.refresh()                    // kick off the re-sync
                // The re-attached Firestore listeners deliver asynchronously; hold the
                // spinner briefly so the gesture reads as "pulling new messages".
                scope.launch {
                    delay(800)                                // wait ~0.8s for data to arrive
                    isRefreshing = false                      // then hide the spinner
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)                        // respect the scaffold's bar insets
        ) {
            if (conversation.messages.isEmpty()) {            // no messages yet → friendly empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()), // scrollable so pull-to-refresh still works when empty
                    contentAlignment = Alignment.Center
                ) {
                    Text("Say hi to $friendName!", color = colors.onSurfaceVariant)
                }
            } else {                                          // otherwise render the message history
                LazyColumn(
                    state = listState,                        // bound to listState for the auto-scroll above
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp), // 8dp gap between bubbles
                    contentPadding = PaddingValues(vertical = 12.dp)  // breathing room at top/bottom of the list
                ) {
                    items(conversation.messages, key = { it.id }) { message -> // stable key = message id
                        MessageBubble(message)                // render one bubble per message
                    }
                }
            }
        }
    }
}

// One chat bubble. Mine = right-aligned orange; theirs = left-aligned surface grey.
@Composable
private fun MessageBubble(message: Message) {
    val colors = MaterialTheme.colorScheme
    val bubbleShape = if (message.fromMe) {                   // tail corner differs by sender for a "chat" look
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp) // mine: sharp bottom-right
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp) // theirs: sharp bottom-left
    }
    val bubbleColor = if (message.fromMe) colors.primary else colors.surface   // my bubble = brand colour
    val textColor = if (message.fromMe) colors.onPrimary else colors.onSurface // contrasting text colour
    val alignment = if (message.fromMe) Alignment.End else Alignment.Start     // push the bubble to the correct side

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment                      // aligns the bubble + timestamp left or right
    ) {
        Surface(                                             // the coloured bubble container
            shape = bubbleShape,
            color = bubbleColor,
            modifier = Modifier.widthIn(max = 280.dp)        // cap bubble width so long text wraps
        ) {
            Text(
                text = message.text,                         // the message body
                color = textColor,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), // inner padding
                fontSize = 15.sp
            )
        }
        Text(
            text = formatTime(message.timestampMs),          // small "HH:mm" timestamp under the bubble
            color = colors.onSurfaceVariant,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

// The bottom composer: a rounded text field plus a circular send button.
@Composable
private fun ChatComposer(
    value: String,                                           // current draft text (hoisted state)
    onValueChange: (String) -> Unit,                         // called on every keystroke
    onSend: () -> Unit                                       // called when the send button is tapped
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = colors.background,
        shadowElevation = 2.dp,                              // slight lift so it separates from the message list
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),              // take all width except the send button
                placeholder = { Text("Message", color = colors.onSurfaceVariant) },
                shape = RoundedCornerShape(24.dp),           // pill-shaped input
                singleLine = false,                          // allow multi-line messages
                maxLines = 4,                                // but cap height at 4 lines
                colors = OutlinedTextFieldDefaults.colors(   // theme the field's text/border/cursor
                    focusedTextColor = colors.onBackground,
                    unfocusedTextColor = colors.onBackground,
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.outline,
                    cursorColor = colors.primary
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilledIconButton(
                onClick = onSend,
                enabled = value.isNotBlank(),                // disabled until there's something to send
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                    disabledContainerColor = colors.surfaceVariant, // greyed-out look when empty
                    disabledContentColor = colors.onSurfaceVariant
                ),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
        }
    }
}

// Format an epoch-millis timestamp as a 24-hour "HH:mm" string in the device locale.
private fun formatTime(timestampMs: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestampMs))

// Design-time preview so the screen renders in Android Studio without running the app.
@Preview
@Composable
fun ChatScreenPreview() {
    ChatScreen(rememberNavController(), "Liyana Rahman")
}
