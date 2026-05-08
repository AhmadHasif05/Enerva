package com.example.a211198_hasif_drnelson_Project1.view_model

// Compose state delegates — let us write `var name by mutableStateOf(...)`
// and have the UI recompose automatically when the value changes.
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.a211198_hasif_drnelson_Project1.model.UserData

// UserViewModel is the single source of truth for the active user.
// It survives configuration changes (rotation) because it lives in the
// ViewModel store — Compose just reads the current values via state.
class UserViewModel : ViewModel() {

    // The user data captured during signup. Null until registerUser() is called.
    // `private set` means only this class can write to it from the outside.
    var registeredUser by mutableStateOf<UserData?>(null)
        private set

    // The currently logged-in user's profile. Edited by EditProfileScreen.
    var userProfile by mutableStateOf(UserData())
        private set

    // Map of "friendName -> isFollowed". Compose-observable so SearchScreen
    // and ProfileScreen redraw when the follow state flips.
    private val followedPeople = mutableStateMapOf<String, Boolean>()

    // Called when the user submits the signup form.
    fun registerUser(name: String, email: String) {
        registeredUser = UserData(runnerName = name, email = email)
    }

    // Try to log in. Returns true on success so MainActivity knows whether
    // to navigate to Home or stay on the LoginScreen.
    fun loginUser(email: String): Boolean {
        return if (registeredUser?.email == email) {
            // Email matches what was used during signup → log in as that user.
            userProfile = registeredUser!!
            true
        } else if (email == "hasif@example.com") {
            // Hard-coded fallback for testing without going through signup first.
            userProfile = UserData(runnerName = "Hasif Azizan", email = email)
            true
        } else {
            false
        }
    }

    // Used by EditProfileScreen — overwrites every editable field at once.
    fun updateProfile(
        name: String,
        email: String,
        location: String,
        fitnessLevel: String,
        personalGoal: String
    ) {
        userProfile = userProfile.copy(
            runnerName = name,
            email = email,
            location = location,
            fitnessLevel = fitnessLevel,
            personalGoal = personalGoal
        )
    }

    // Helpers for partial updates (kept for backwards compatibility).
    fun updateEmail(email: String) {
        userProfile = userProfile.copy(email = email)
    }

    fun updateRunnerName(runnerName: String) {
        userProfile = userProfile.copy(runnerName = runnerName)
    }

    // Returns true if the current user is following the named person.
    fun isFollowing(name: String): Boolean = followedPeople[name] == true

    // Toggle follow state for `name` and bump the user's `following` counter.
    // The counter is reflected on ProfileScreen automatically because it reads
    // userProfile (which is mutableStateOf, so Compose tracks reads).
    fun toggleFollow(name: String) {
        val currentlyFollowing = isFollowing(name)
        if (currentlyFollowing) {
            followedPeople[name] = false
            userProfile = userProfile.copy(
                // coerceAtLeast keeps the count from going negative due to bugs.
                following = (userProfile.following - 1).coerceAtLeast(0)
            )
        } else {
            followedPeople[name] = true
            userProfile = userProfile.copy(following = userProfile.following + 1)
        }
    }
}