package com.example.a211198_hasif_drnelson_Project1.view_model

// derivedStateOf gives us a memoised value that recomputes only when the
// underlying state actually changes — perfect for the sorted list below.
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import com.example.a211198_hasif_drnelson_Project1.model.Conversation
import com.example.a211198_hasif_drnelson_Project1.model.Message

// Owns every chat conversation. Shared with SearchScreen, MessageScreen,
// and ChatScreen via MainActivity so they all see the same data.
class MessageViewModel : ViewModel() {

    // Compose-observable map: friendName -> Conversation.
    private val conversations = mutableStateMapOf<String, Conversation>()

    // Public list view — sorted so the most recently messaged friend is on top.
    // `derivedStateOf` re-runs only when `conversations` changes.
    val conversationList: List<Conversation> by derivedStateOf {
        conversations.values
            .sortedByDescending { it.lastMessage?.timestampMs ?: 0L }
    }

    // Look up a conversation by friend name. Returns an empty conversation
    // (no messages) if there is no entry yet — easier than handling null.
    fun getConversation(friendName: String): Conversation =
        conversations[friendName] ?: Conversation(friendName)

    // Called from SearchScreen the moment the user follows someone new.
    // Seeds the chat with a friendly opening message from the friend so the
    // conversation list isn't empty.
    fun startConversationWith(friendName: String) {
        if (conversations.containsKey(friendName)) return
        val opening = Message(
            fromMe = false,
            text = "Hey! Thanks for the follow 👋 Let's go for a run sometime."
        )
        conversations[friendName] = Conversation(friendName, listOf(opening))
    }

    // Append a new message from the user to a conversation.
    // No-op for blank text so accidentally tapping send does nothing.
    fun sendMessage(friendName: String, text: String) {
        if (text.isBlank()) return
        val current = conversations[friendName] ?: Conversation(friendName)
        val updated = current.copy(
            messages = current.messages + Message(fromMe = true, text = text.trim())
        )
        conversations[friendName] = updated
    }

    // Drop the conversation entirely — used when the user unfollows someone.
    fun removeConversation(friendName: String) {
        conversations.remove(friendName)
    }
}