// Package — every Kotlin file in this folder belongs to the model layer.
package com.example.a211198_hasif_drnelson_Project1.model

// UserData holds everything we know about the current runner.
// It's a plain data class (no logic, no Android dependencies) — pure model.
// Any field with a default value is optional when constructing one.
data class UserData(
    val runnerName: String = "",                                                       // Display name shown across the app
    val studentId: String = "A211198",                                                 // Student ID — fixed for this project
    val email: String = "",                                                            // Login / contact email
    val location: String = "Kuala Lumpur, Federal Territory of Kuala Lumpur, Malaysia", // City label shown on profile
    val fitnessLevel: String = "Intermediate",                                         // Beginner / Intermediate / Advanced / Elite
    val personalGoal: String = "25.0 km / week",                                       // User-set weekly target
    val following: Int = 0,                                                            // How many runners the user follows
    val followers: Int = 67,                                                           // How many runners follow the user
)