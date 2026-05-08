package com.example.a211198_hasif_drnelson_Project1.model

import java.util.UUID

// A community challenge the user can join (e.g. "Run 10 km in April").
// Lives entirely in memory for now — the ChallengeViewModel mutates it.
data class Challenge(
    val id: String = UUID.randomUUID().toString(), // Unique id — used as LazyColumn key
    val badge: String,                             // Short label shown in the badge box, e.g. "10K"
    val title: String,                             // Full challenge name
    val description: String,                       // What you have to do to complete it
    val startDate: String,                         // When the challenge opens
    val endDate: String,                           // Deadline
    val isJoined: Boolean = false                  // True once the user taps "Join"
)

// Sample challenges that populate GroupsScreen on first launch.
val sampleChallenges = listOf(
    Challenge(
        badge = "10K",
        title = "April 10K Challenge",
        description = "Complete a 10 km (6.2 mi) run.",
        startDate = "Apr 1, 2026",
        endDate = "Apr 30, 2026"
    ),
    Challenge(
        badge = "180'",
        title = "April 180 Minute Sweat Challenge",
        description = "Complete a single activity that is 180 minutes or longer.",
        startDate = "Apr 1, 2026",
        endDate = "Apr 30, 2026"
    ),
    Challenge(
        badge = "2000M",
        title = "April Elevation Challenge",
        description = "Climb a total of 2,000 m (6,561.7 ft).",
        startDate = "Apr 1, 2026",
        endDate = "Apr 30, 2026"
    )
)