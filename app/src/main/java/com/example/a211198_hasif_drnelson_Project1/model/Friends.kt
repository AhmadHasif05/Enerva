package com.example.a211198_hasif_drnelson_Project1.model

import java.util.UUID

// A friend / runner the user might follow. Used by SearchScreen's
// "People you may know" list and for seeding sample data.
data class Friend(
    val id: String = UUID.randomUUID().toString(), // Unique id (used as LazyColumn key)
    val name: String,                              // Display name shown on the row
    val location: String,                          // City / area shown under the name
    val subtitle: String = "",                     // Optional tag like "Local Legend near you"
    val isFollowing: Boolean = false               // Whether the current user follows them
)

// Demo runners shown on SearchScreen by default.
val sampleFriends = listOf(
    Friend(name = "Anette Visser", location = "Hantum, Friesland", subtitle = "Fan favorite on Strava"),
    Friend(name = "Liyana Rahman", location = "Jerantut, Pahang", subtitle = "Local Legend near you"),
    Friend(name = "Mohd Khairol Azani", location = "Local Legend near you"),
    Friend(name = "Helly M", location = "Marin, CA", subtitle = "Fan favorite on Strava"),
    Friend(name = "boy ezwan", location = "Local Legend near you")
)