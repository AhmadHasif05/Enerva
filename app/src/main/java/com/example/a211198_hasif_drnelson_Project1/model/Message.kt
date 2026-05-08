package com.example.a211198_hasif_drnelson_Project1.model

import java.util.UUID

// A single chat message between the user and a friend.
//   - fromMe = true  → right-aligned orange bubble in ChatScreen
//   - fromMe = false → left-aligned surface bubble (the friend's reply)
data class Message(
    val id: String = UUID.randomUUID().toString(),    // Unique id (used as LazyColumn key)
    val fromMe: Boolean,                              // Who sent the message
    val text: String,                                 // The message body
    val timestampMs: Long = System.currentTimeMillis() // When it was sent (epoch ms)
)

// A whole conversation with one friend. The MessageViewModel keeps a map of
// friendName -> Conversation. The `lastMessage` getter is computed on demand
// for the conversation list preview on MessageScreen.
data class Conversation(
    val friendName: String,                  // Other party's display name (also the map key)
    val messages: List<Message> = emptyList() // Full chat history, ordered oldest -> newest
) {
    // Most recent message — null when no messages have been exchanged yet.
    val lastMessage: Message? get() = messages.lastOrNull()
}