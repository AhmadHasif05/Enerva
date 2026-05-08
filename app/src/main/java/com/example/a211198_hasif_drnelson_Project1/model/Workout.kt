package com.example.a211198_hasif_drnelson_Project1.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

// A workout template — the kind of thing that appears in "Instant Workouts".
//
// We keep the icon as a String name and the colour as a hex String so this
// model stays free of UI types. The UI layer can map iconName -> ImageVector
// and colorHex -> Color when rendering. (HomeScreen currently keeps its own
// version with ImageVector + Color baked in for convenience.)
data class Workout(
    val id: String = java.util.UUID.randomUUID().toString(), // Unique id
    val title: String,                                       // Workout name e.g. "Brisk Walk"
    val description: String,                                 // Short description shown under the title
    val duration: String,                                    // Display duration, e.g. "30m"
    val iconName: String,                                    // Name of the icon (mapped in the UI layer)
    val colorHex: String                                     // Hex colour for the workout card accent
)