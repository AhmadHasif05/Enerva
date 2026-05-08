package com.example.a211198_hasif_drnelson_Project1.model

import java.util.UUID

// A running/cycling club shown on the Groups → Clubs tab.
data class Club(
    val id: String = UUID.randomUUID().toString(), // Unique id — used as LazyColumn key
    val name: String,                              // Club name displayed at the top of the row
    val description: String,                       // Short tagline / description
    val sport: String,                             // Primary sport (Run / Trail Run / Ride …)
    val location: String,                          // Where the club is based
    val memberCount: Int,                          // How many members it has
    val isJoined: Boolean = false                  // True after the user taps "Join"
)

// A few starter clubs so the Clubs tab isn't empty.
val sampleClubs = listOf(
    Club(
        name = "KL Runners",
        description = "Casual weekly runs around Kuala Lumpur.",
        sport = "Run",
        location = "Kuala Lumpur, MY",
        memberCount = 1284
    ),
    Club(
        name = "UUM Trail Pack",
        description = "Trail and hill workouts on campus and beyond.",
        sport = "Trail Run",
        location = "Sintok, Kedah",
        memberCount = 312
    ),
    Club(
        name = "Pahang Cyclists",
        description = "Group rides and weekend tours in Pahang.",
        sport = "Ride",
        location = "Jerantut, Pahang",
        memberCount = 540
    )
)