package com.example.a211198_hasif_drnelson_Project1.model

// java.util.UUID generates a random unique id for each new record.
import java.util.UUID

// One completed workout the user has logged. Used by ActivityScreen and the
// "Activities" tab on YouScreen.
data class ActivityRecord(
    val id: String = UUID.randomUUID().toString(), // Unique key — useful for LazyColumn `key` lookups
    val type: String,                              // "Run", "Walk", "Ride" — drives which icon to show
    val title: String,                             // Display title, e.g. "Morning Run"
    val date: String,                              // Human-readable date string
    val distanceKm: Double,                        // Distance covered in kilometres
    val durationMinutes: Int,                      // How long the activity took (minutes)
    val elevationM: Int,                           // Total climb in metres
    val avgPace: String                            // Pre-formatted pace string, e.g. "6'09\" /km"
)

// Hard-coded sample data — lets screens render something useful before we
// have a real database. Replace with a Room DAO when persistence is added.
val sampleActivityRecords = listOf(
    ActivityRecord(
        type = "Run",
        title = "Morning Run",
        date = "May 8, 2026",
        distanceKm = 5.2,
        durationMinutes = 32,
        elevationM = 14,
        avgPace = "6'09\" /km"
    ),
    ActivityRecord(
        type = "Walk",
        title = "Evening Walk",
        date = "May 6, 2026",
        distanceKm = 3.0,
        durationMinutes = 28,
        elevationM = 5,
        avgPace = "9'20\" /km"
    ),
    ActivityRecord(
        type = "Ride",
        title = "Sunday Ride",
        date = "May 4, 2026",
        distanceKm = 18.5,
        durationMinutes = 55,
        elevationM = 120,
        avgPace = "20.2 km/h"
    )
)