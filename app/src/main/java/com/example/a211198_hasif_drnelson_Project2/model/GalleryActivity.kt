// Package — model layer (pure data, no Android/Compose dependencies).
package com.example.a211198_hasif_drnelson_Project2.model

import com.example.a211198_hasif_drnelson_Project2.R

// GalleryActivity is one activity shown in the gallery/reels feed.
// It's also reused as a card on Home ("Your Progress") and in the
// Profile's gallery grid.
//
// `tint` is stored as a plain ARGB Long so this model stays free of any
// Compose dependency — the UI converts it to a Color when rendering.
// `imageRes` is the drawable shown as the tile image (Profile grid) and
// the reel background (Gallery reels).
data class GalleryActivity(
    val id: String,
    val author: String,
    val caption: String,
    val activity: String,   // Run / Cycle / Walk
    val distanceKm: String,
    val tint: Long,
    val imageRes: Int,
    val imageUri: String? = null, // user-picked photo (URI string); null falls back to imageRes
    val likes: Int = 0,
    // True when imageUri is a branded run-summary card (≈square) rather than a
    // full-bleed photo. The reel renderer fits cards (whole frame visible) and
    // crops photos (full-bleed). See docs/.../reel-card-fit.md.
    val isCard: Boolean = false
)

// Sample feed used across Home, Gallery and Profile. The first entry is the
// current user's activity (so it shows their name); the rest are friends'.
// Order = newest first (Instagram-style — latest at the top of the grid).
fun sampleGalleryActivities(myName: String): List<GalleryActivity> = listOf(
    GalleryActivity("a1", myName.ifBlank { "You" }, "Morning loop around the lake 🌅", "Run", "5.2", 0xFF1E3A5F, R.drawable.lakesidetrail, likes = 128),
    GalleryActivity("a2", "Sarah Tan", "New PB on the hill repeats!", "Run", "8.0", 0xFF4A2C5E, R.drawable.running, likes = 342),
    GalleryActivity("a3", "Daniel Lee", "Easy recovery spin 🚴", "Cycle", "18.4", 0xFF2C5E3A, R.drawable.lingkunganilmu, likes = 89),
    GalleryActivity("a4", "Aisha Rahman", "Sunset trail walk", "Walk", "3.1", 0xFF5E4A2C, R.drawable.teratai, likes = 56),
    GalleryActivity("a5", myName.ifBlank { "You" }, "Tempo intervals on the track", "Run", "6.4", 0xFF3A1E5F, R.drawable.running, likes = 210),
    GalleryActivity("a6", "Sarah Tan", "Park loop with the dog", "Walk", "2.8", 0xFF5F3A1E, R.drawable.lakesidetrail, likes = 74),
    GalleryActivity("a7", "Daniel Lee", "Long ride out to the coast", "Cycle", "42.1", 0xFF1E5F3A, R.drawable.lingkunganilmu, likes = 301),
    GalleryActivity("a8", "Aisha Rahman", "Hill repeats — brutal", "Run", "7.5", 0xFF5F1E3A, R.drawable.teratai, likes = 167),
    GalleryActivity("a9", myName.ifBlank { "You" }, "Easy shakeout", "Run", "3.0", 0xFF2C2C5E, R.drawable.lakesidetrail, likes = 41)
)
