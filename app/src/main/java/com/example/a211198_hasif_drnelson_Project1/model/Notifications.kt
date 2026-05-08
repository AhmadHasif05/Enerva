package com.example.a211198_hasif_drnelson_Project1.model

// One in-app notification, e.g. "Welcome to Enerva Challenges".
// Used by NotifyScreen.
data class Notification(
    val id: String = java.util.UUID.randomUUID().toString(), // Unique id (good for LazyColumn keys)
    val title: String,                                       // Bold heading line
    val description: String,                                 // Short body shown below the title
    val timestamp: String,                                   // Pre-formatted "14 days ago" style label
    val isRead: Boolean = false                              // True once the user opens / dismisses it
)