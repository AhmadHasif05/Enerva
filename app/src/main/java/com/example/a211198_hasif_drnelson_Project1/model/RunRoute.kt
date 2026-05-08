package com.example.a211198_hasif_drnelson_Project1.model

import java.util.UUID

// A pre-baked route the user can browse on the Maps tab. The HomeScreen
// has its own `RunRoute` class with a drawable resource — this generic model
// uses imageUrl so it can come from a remote source later.
data class RunRouteModel(
    val id: String = UUID.randomUUID().toString(), // Unique id
    val title: String,                             // Route name, e.g. "Teratai"
    val distance: String,                          // Pre-formatted distance, "3.22 km"
    val time: String,                              // Estimated completion time, "0h 21m"
    val elevation: String,                         // Total climb, "0 m"
    val difficulty: String,                        // Easy / Moderate / Hard
    val imageUrl: String                           // Cover image URL (Unsplash here, swap for your CDN)
)

// Sample routes — used as fallback when no remote data is available.
val sampleRoutes = listOf(
    RunRouteModel(
        title = "Teratai",
        distance = "3.22 km",
        time = "0h 21m",
        elevation = "0 m",
        difficulty = "Easy",
        imageUrl = "https://images.unsplash.com/photo-1502082553048-f009c37129b9?ixlib=rb-1.2.1&auto=format&fit=crop&w=800&q=60"
    ),
    RunRouteModel(
        title = "Lakeside Trail",
        distance = "5.50 km",
        time = "0h 45m",
        elevation = "15 m",
        difficulty = "Moderate",
        imageUrl = "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?ixlib=rb-1.2.1&auto=format&fit=crop&w=800&q=60"
    )
)