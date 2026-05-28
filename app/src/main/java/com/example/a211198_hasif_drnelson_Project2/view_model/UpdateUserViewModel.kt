package com.example.a211198_hasif_drnelson_Project2.view_model

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.a211198_hasif_drnelson_Project2.RunTrackApplication
import com.example.a211198_hasif_drnelson_Project2.data.AppDatabase
import com.example.a211198_hasif_drnelson_Project2.data.entities.FollowEntity
import com.example.a211198_hasif_drnelson_Project2.data.entities.SavedRouteEntity
import com.example.a211198_hasif_drnelson_Project2.data.entities.UserEntity
import com.example.a211198_hasif_drnelson_Project2.model.RunRoute
import com.example.a211198_hasif_drnelson_Project2.model.UserData
import com.example.a211198_hasif_drnelson_Project2.model.routeList
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Persistence-backed user state. Compose-readable mutableStateOf surfaces are
// kept identical to the original in-memory ViewModel so screens don't change.
// Source of truth is Room; mutableStateOf values are mirrors refreshed from
// flows + suspend reads.
class UserViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.get(application)
    private val userDao = db.userDao()
    private val prefs = application.getSharedPreferences("runtrack", Context.MODE_PRIVATE)

    // The user data captured during signup (the last one registered in this session).
    var registeredUser by mutableStateOf<UserData?>(null)
        private set

    // The currently logged-in user's profile.
    var userProfile by mutableStateOf(UserData())
        private set

    // Friend follow state mirror, keyed by friend display name.
    private val followedPeople = mutableStateMapOf<String, Boolean>()

    // Saved weekend routes mirror, keyed by title.
    private val savedRoutesByTitle = mutableStateMapOf<String, RunRoute>()

    val savedRoutes: List<RunRoute>
        get() = savedRoutesByTitle.values.toList()

    // Other registered users — drives discovery on SearchScreen.
    var otherUsers by mutableStateOf<List<UserData>>(emptyList())
        private set

    // Lookup helper: returns the registered user with this display name, or null.
    suspend fun findUserByName(name: String): UserData? =
        userDao.findByName(name)?.toModel()

    init {
        // Restore the active session if there is one.
        prefs.getString(KEY_ACTIVE_EMAIL, null)?.let { email ->
            viewModelScope.launch {
                userDao.findByEmail(email)?.let { entity ->
                    userProfile = entity.toModel()
                    registeredUser = entity.toModel()
                    startObserving(email)
                }
            }
        }
    }

    // ---- registration / login ----

    fun registerUser(
        name: String,
        email: String,
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            if (userDao.findByEmail(email) != null) {
                onResult(false, "That email is already registered")
                return@launch
            }
            val seed = UserData(runnerName = name, email = email)
            userDao.upsertUser(seed.toEntity())
            registeredUser = seed
            onResult(true, null)
        }
    }

    /**
     * Result is delivered via [onResult] on the main thread.
     * true = user found (or seeded fallback) and is now logged in.
     */
    fun loginUser(email: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val existing = userDao.findByEmail(email)
            val resolved = when {
                existing != null -> existing
                email == "hasif@gmail.com" -> {
                    // Hard-coded fallback for testing; seed the row so we have a real user.
                    val seed = UserData(runnerName = "Hasif Azizan", email = email).toEntity()
                    userDao.upsertUser(seed)
                    seed
                }
                else -> null
            }
            if (resolved != null) {
                userProfile = resolved.toModel()
                registeredUser = resolved.toModel()
                setActiveEmail(email)
                startObserving(email)
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    fun loginWithGoogle() {
        viewModelScope.launch {
            val seed = registeredUser
                ?: UserData(runnerName = "Google User", email = "googleuser@gmail.com")
            userDao.upsertUser(seed.toEntity())
            userProfile = seed
            setActiveEmail(seed.email)
            startObserving(seed.email)
        }
    }

    fun logout() {
        prefs.edit().remove(KEY_ACTIVE_EMAIL).apply()
        userProfile = UserData()
        followedPeople.clear()
        savedRoutesByTitle.clear()
    }

    // ---- profile edits ----

    fun updateProfile(
        name: String,
        email: String,
        location: String,
        fitnessLevel: String,
        personalGoal: String,
        bio: String
    ) {
        val updated = userProfile.copy(
            runnerName = name,
            email = email,
            location = location,
            fitnessLevel = fitnessLevel,
            personalGoal = personalGoal,
            bio = bio
        )
        userProfile = updated
        viewModelScope.launch { userDao.upsertUser(updated.toEntity()) }
    }

    fun updateEmail(email: String) {
        val updated = userProfile.copy(email = email)
        userProfile = updated
        viewModelScope.launch { userDao.upsertUser(updated.toEntity()) }
    }

    fun updatePhotoUri(photoUri: String?) {
        val updated = userProfile.copy(photoUri = photoUri)
        userProfile = updated
        viewModelScope.launch { userDao.upsertUser(updated.toEntity()) }
    }

    fun updateRunnerName(runnerName: String) {
        val updated = userProfile.copy(runnerName = runnerName)
        userProfile = updated
        viewModelScope.launch { userDao.upsertUser(updated.toEntity()) }
    }

    // ---- follows ----

    fun isFollowing(name: String): Boolean = followedPeople[name] == true

    fun toggleFollow(name: String) {
        val owner = userProfile.email
        if (owner.isBlank()) return
        val currentlyFollowing = isFollowing(name)
        viewModelScope.launch {
            if (currentlyFollowing) {
                userDao.removeFollow(owner, name)
            } else {
                userDao.addFollow(FollowEntity(owner, name))
            }
            // Update my own following count.
            val followingCount = userDao.observeFollowing(owner).first().size
            val updatedMe = userProfile.copy(following = followingCount)
            userProfile = updatedMe
            userDao.upsertUser(updatedMe.toEntity())

            // Mirror to the followee: if they're a registered user, refresh
            // their followers count from the follows table so it shows up
            // next time they log in (or right away if they're observing).
            val followee = userDao.findByName(name)
            if (followee != null) {
                val followers = userDao.countFollowersOf(name)
                userDao.upsertUser(followee.copy(followers = followers))
            }
        }
    }

    // ---- saved routes ----

    fun isRouteSaved(title: String): Boolean = savedRoutesByTitle.containsKey(title)

    fun toggleRouteSave(route: RunRoute) {
        val owner = userProfile.email
        if (owner.isBlank()) return
        viewModelScope.launch {
            if (savedRoutesByTitle.containsKey(route.title)) {
                userDao.unsaveRoute(owner, route.title)
            } else {
                userDao.saveRoute(route.toEntity(owner))
            }
        }
    }

    // ---- private helpers ----

    private fun setActiveEmail(email: String) {
        prefs.edit().putString(KEY_ACTIVE_EMAIL, email).apply()
    }

    private fun startObserving(email: String) {
        viewModelScope.launch {
            userDao.observeFollowing(email).collect { names ->
                followedPeople.clear()
                names.forEach { followedPeople[it] = true }
            }
        }
        viewModelScope.launch {
            userDao.observeSavedRoutes(email).collect { rows ->
                savedRoutesByTitle.clear()
                rows.forEach { row -> row.toModel()?.let { savedRoutesByTitle[row.title] = it } }
            }
        }
        viewModelScope.launch {
            userDao.observeByEmail(email).collect { entity ->
                if (entity != null) userProfile = entity.toModel()
            }
        }
        viewModelScope.launch {
            userDao.observeAllExcept(email).collect { entities ->
                otherUsers = entities.map { it.toModel() }
            }
        }
    }

    companion object {
        private const val KEY_ACTIVE_EMAIL = "activeEmail"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as RunTrackApplication
                UserViewModel(app)
            }
        }
    }
}

// ---- mappers (file-private, kept next to the VM that owns them) ----

private fun UserData.toEntity() = UserEntity(
    email = email,
    runnerName = runnerName,
    location = location,
    fitnessLevel = fitnessLevel,
    personalGoal = personalGoal,
    bio = bio,
    following = following,
    followers = followers,
    photoUri = photoUri
)

private fun UserEntity.toModel() = UserData(
    runnerName = runnerName,
    email = email,
    location = location,
    fitnessLevel = fitnessLevel,
    personalGoal = personalGoal,
    bio = bio,
    following = following,
    followers = followers,
    photoUri = photoUri
)

private fun RunRoute.toEntity(ownerEmail: String) = SavedRouteEntity(
    ownerEmail = ownerEmail,
    title = title,
    distance = distance,
    time = time,
    elevation = elevation,
    difficulty = difficulty,
    imageRes = imageRes
)

// Saved routes are matched back to the in-memory `routeList` so screens reuse
// the same drawable + label formatting. If a stored route no longer matches
// (e.g. resource id moved across rebuilds), it is filtered out by the caller.
private fun SavedRouteEntity.toModel(): RunRoute? =
    routeList.firstOrNull { it.title == title }
        ?: RunRoute(title, distance, time, elevation, difficulty, imageRes)
