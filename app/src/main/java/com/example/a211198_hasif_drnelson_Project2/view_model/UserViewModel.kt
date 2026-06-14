package com.example.a211198_hasif_drnelson_Project2.view_model

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.a211198_hasif_drnelson_Project2.RunTrackApplication
import com.example.a211198_hasif_drnelson_Project2.data.entities.SavedRouteEntity
import com.example.a211198_hasif_drnelson_Project2.data.repository.AuthRepository
import com.example.a211198_hasif_drnelson_Project2.data.repository.RunSpotRepository
import com.example.a211198_hasif_drnelson_Project2.data.repository.UserRepository
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await
import com.example.a211198_hasif_drnelson_Project2.model.RunRoute
import com.example.a211198_hasif_drnelson_Project2.model.UserData
import com.example.a211198_hasif_drnelson_Project2.model.routeList
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// User-facing state holder. All persistence (Room cache + Firestore sync) lives in
// UserRepository; this ViewModel only mirrors repository flows into Compose-readable
// state and forwards user actions. Auth itself stays in AuthRepository.
//
// The mutableStateOf surfaces are kept identical to the original in-memory
// ViewModel so screens don't change.
class UserViewModel(
    application: Application,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val runSpotRepository: RunSpotRepository
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("runtrack", Context.MODE_PRIVATE)

    // Active Room/Firestore observers. Held so logout (and re-login) can cancel them —
    // otherwise a stale collector keeps writing the previous user's data into
    // userProfile and the old account "comes back".
    private val observerJobs = mutableListOf<Job>()

    // The user data captured during signup (the last one registered in this session).
    var registeredUser by mutableStateOf<UserData?>(null)
        private set

    // The currently logged-in user's profile.
    var userProfile by mutableStateOf(UserData())
        private set

    // Live "Plan Your Weekend Run" spots (OpenStreetMap). Starts Loading; resolves to
    // Success (live) or Fallback (samples) after the first fetch.
    var weekendRun by mutableStateOf<WeekendRunUiState>(WeekendRunUiState.Loading)
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
    suspend fun findUserByName(name: String): UserData? = userRepository.findByName(name)

    init {
        // Restore the active session from Firebase Auth if there is one.
        authRepository.currentUser?.let { fbUser ->
            viewModelScope.launch {
                val profile = userRepository.onSignIn(fbUser)
                applyActiveSession(profile)
            }
        }
        loadWeekendRunSpots()
    }

    // ---- registration / login ----

    fun registerUser(
        name: String,
        email: String,
        password: String,
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            authRepository.signUp(email, password).fold(
                onSuccess = { authUser ->
                    // Seed Room + the Firestore doc. No session is started here:
                    // MainActivity signs the user back out and routes them to Login.
                    registeredUser = userRepository.onSignUp(authUser, name)
                    onResult(true, null)
                },
                onFailure = { e -> onResult(false, e.message ?: "Signup failed") }
            )
        }
    }

    /**
     * Result is delivered via [onResult] on the main thread.
     * Signs in against Firebase Auth; on success, hydrates the local profile.
     */
    fun loginUser(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            authRepository.signIn(email, password).fold(
                onSuccess = { authUser ->
                    val profile = userRepository.onSignIn(authUser)
                    applyActiveSession(profile)
                    onResult(true, null)
                },
                onFailure = { e -> onResult(false, e.message ?: "Login failed") }
            )
        }
    }

    /**
     * Exchanges a Google ID token for a Firebase session, then hydrates the
     * local profile — mirroring [loginUser]. On first login the display name
     * comes from the Google account.
     */
    fun loginWithGoogle(idToken: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            authRepository.signInWithGoogle(idToken).fold(
                onSuccess = { authUser ->
                    // Guard hydration so onResult always fires — a Room/Firestore error
                    // after sign-in must not leave the UI stuck on Login with no feedback.
                    try {
                        val profile = userRepository.onSignIn(authUser)
                        applyActiveSession(profile)
                        onResult(true, null)
                    } catch (e: Exception) {
                        android.util.Log.e("GoogleSignIn", "Profile hydration failed", e)
                        onResult(false, e.message ?: "Failed to load profile after Google sign-in")
                    }
                },
                onFailure = { e -> onResult(false, e.message ?: "Google sign-in failed") }
            )
        }
    }

    fun sendPasswordReset(email: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            authRepository.sendPasswordReset(email).fold(
                onSuccess = { onResult(true, null) },
                onFailure = { e -> onResult(false, e.message ?: "Could not send reset email") }
            )
        }
    }

    fun logout() {
        // Tear down the previous user's observers + cloud listeners FIRST, otherwise
        // their collectors keep emitting and overwrite the cleared profile.
        stopObserving()
        userRepository.stopListeners()
        authRepository.signOut()
        prefs.edit().remove(KEY_ACTIVE_EMAIL).apply()
        userProfile = UserData()
        registeredUser = null
        otherUsers = emptyList()
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
        val oldName = userProfile.runnerName
        val updated = userProfile.copy(
            runnerName = name,
            email = email,
            location = location,
            fitnessLevel = fitnessLevel,
            personalGoal = personalGoal,
            bio = bio
        )
        userProfile = updated
        viewModelScope.launch { userRepository.saveProfileWithRename(updated, oldName) }
    }

    fun updateEmail(email: String) {
        val updated = userProfile.copy(email = email)
        userProfile = updated
        viewModelScope.launch { userRepository.saveProfile(updated) }
    }

    fun updatePhotoUri(photoUri: String?, avatarBytes: ByteArray? = null) {
        val updated = userProfile.copy(photoUri = photoUri)
        userProfile = updated
        viewModelScope.launch { userRepository.saveProfile(updated, avatarBytes) }
    }

    fun updateRunnerName(runnerName: String) {
        val oldName = userProfile.runnerName
        val updated = userProfile.copy(runnerName = runnerName)
        userProfile = updated
        viewModelScope.launch { userRepository.saveProfileWithRename(updated, oldName) }
    }

    // ---- follows ----

    fun isFollowing(name: String): Boolean = followedPeople[name] == true

    /**
     * Resolve a reel/profile author's avatar by display name: my own photo if it's
     * me, else the directory entry (cross-device), else null (icon fallback).
     */
    fun photoForAuthor(name: String): String? =
        if (name == userProfile.runnerName) userProfile.photoUri
        else otherUsers.firstOrNull { it.runnerName == name }?.photoUri

    fun toggleFollow(name: String) {
        val owner = userProfile.email
        if (owner.isBlank()) return
        viewModelScope.launch {
            userRepository.toggleFollow(owner, userProfile.firebaseUid, name)
        }
    }

    // ---- saved routes ----

    fun isRouteSaved(title: String): Boolean = savedRoutesByTitle.containsKey(title)

    fun toggleRouteSave(route: RunRoute) {
        val owner = userProfile.email
        if (owner.isBlank()) return
        val uid = userProfile.firebaseUid
        viewModelScope.launch {
            if (savedRoutesByTitle.containsKey(route.title)) {
                userRepository.unsaveRoute(owner, uid, route.title)
            } else {
                userRepository.saveRoute(owner, uid, route.toEntity(owner))
            }
        }
    }

    // ---- weekend run spots ----

    private var weekendRunJob: Job? = null

    fun loadWeekendRunSpots() {
        weekendRunJob?.cancel()
        weekendRunJob = viewModelScope.launch {
            val (lat, lng) = lastKnownLatLng()
            val result = runSpotRepository.nearbyRunSpots(lat, lng)
            weekendRun = if (result.isLive) WeekendRunUiState.Success(result.routes)
            else WeekendRunUiState.Fallback(result.routes)
        }
    }

    // Best-effort last-known location; falls back to a fixed city when permission
    // isn't granted or no fix is cached. Reuses the location permission already
    // requested by the Record screen — no new prompt here.
    @SuppressLint("MissingPermission")
    private suspend fun lastKnownLatLng(): Pair<Double, Double> = runCatching {
        val client = LocationServices.getFusedLocationProviderClient(getApplication<Application>())
        val loc = client.lastLocation.await()
        if (loc != null) loc.latitude to loc.longitude else DEFAULT_LAT to DEFAULT_LNG
    }.getOrElse { DEFAULT_LAT to DEFAULT_LNG }

    // ---- private helpers ----

    /** Wire up an authenticated session: mirror state, persist active email, start sync. */
    private fun applyActiveSession(profile: UserData) {
        userProfile = profile
        registeredUser = profile
        setActiveEmail(profile.email)
        startObserving(profile.email)
        profile.firebaseUid?.let { uid ->
            userRepository.startListeners(viewModelScope, uid, profile.email)
        }
    }

    private fun setActiveEmail(email: String) {
        prefs.edit().putString(KEY_ACTIVE_EMAIL, email).apply()
    }

    private fun startObserving(email: String) {
        // Cancel any observers from a previous session before starting fresh,
        // so we never end up with two collectors writing userProfile at once.
        stopObserving()
        observerJobs += viewModelScope.launch {
            userRepository.observeFollowing(email).collect { names ->
                followedPeople.clear()
                names.forEach { followedPeople[it] = true }
            }
        }
        observerJobs += viewModelScope.launch {
            userRepository.observeSavedRoutes(email).collect { rows ->
                savedRoutesByTitle.clear()
                rows.forEach { row -> row.toModel()?.let { savedRoutesByTitle[row.title] = it } }
            }
        }
        observerJobs += viewModelScope.launch {
            userRepository.observeProfile(email).collect { profile ->
                if (profile != null) userProfile = profile
            }
        }
        observerJobs += viewModelScope.launch {
            userRepository.observeOtherUsers(email, userProfile.firebaseUid).collect { users -> otherUsers = users }
        }
    }

    private fun stopObserving() {
        observerJobs.forEach { it.cancel() }
        observerJobs.clear()
    }

    companion object {
        private const val KEY_ACTIVE_EMAIL = "activeEmail"
        private const val DEFAULT_LAT = 3.1390
        private const val DEFAULT_LNG = 101.6869

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as RunTrackApplication
                UserViewModel(app, app.authRepository, app.userRepository, app.runSpotRepository)
            }
        }
    }
}

// ---- mappers kept in the VM: RunRoute is a UI/model concern, not persistence ----

private fun RunRoute.toEntity(ownerEmail: String) = SavedRouteEntity(
    ownerEmail = ownerEmail,
    title = title,
    distance = distance,
    time = time,
    elevation = elevation,
    difficulty = difficulty,
    imageRes = imageRes,
    imageUrl = imageUrl
)

// Saved routes are matched back to the in-memory `routeList` so screens reuse
// the same drawable + label formatting. If a stored route no longer matches
// (e.g. resource id moved across rebuilds), it is rebuilt from the stored fields.
private fun SavedRouteEntity.toModel(): RunRoute? =
    routeList.firstOrNull { it.title == title }
        ?: RunRoute(title, distance, time, elevation, difficulty, imageRes, imageUrl)
