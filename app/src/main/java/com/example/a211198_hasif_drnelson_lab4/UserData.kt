package com.example.a211198_hasif_drnelson_lab4

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

// Data Class: Holds user-related information
data class UserData(
    val runnerName: String = "Hasif Azizan",
    val studentId: String = "A211198",
    val location: String = "Kuala Lumpur, Federal Territory of Kuala Lumpur, Malaysia",
    val fitnessLevel: String = "Intermediate",
    val personalGoal: String = "25.0 km / week",
    val following: Int = 0,
    val followers: Int = 0
)

// ViewModel Integration: Holds the instance of UserData to provide to multiple screens
class UserViewModel : ViewModel() {
    // State to hold the user data
    var userProfile by mutableStateOf(UserData())
        private set

    // Function to update data if needed
    fun updateProfile(name: String, level: String, goal: String) {
        userProfile = userProfile.copy(runnerName = name, fitnessLevel = level, personalGoal = goal)
    }
}
