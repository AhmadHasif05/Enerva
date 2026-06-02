package com.example.a211198_hasif_drnelson_Project2.view_model

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.a211198_hasif_drnelson_Project2.R
import com.example.a211198_hasif_drnelson_Project2.RunTrackApplication
import com.example.a211198_hasif_drnelson_Project2.data.entities.MediaEntity
import com.example.a211198_hasif_drnelson_Project2.data.repository.GalleryRepository
import com.example.a211198_hasif_drnelson_Project2.model.GalleryActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.UUID

// Backs both the bottom-nav Gallery (a cross-user feed) and the per-profile
// gallery on ProfileScreen (this user's own posts only).
//
// Persistence + own-media cloud sync live in GalleryRepository; this ViewModel
// holds the display mode, the demo seeding scaffolding (local-only), and mirrors
// the chosen repository flow into Compose state.
//
// Three modes via the show* methods:
//   - showFeed: every reel from every user (the local social feed)
//   - showMyPosts: only the active user's own reels (their profile gallery)
//   - showAuthorGallery(name): another user's gallery (visiting a profile)
class GalleryViewModel(
    application: Application,
    private val repository: GalleryRepository
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("runtrack", Context.MODE_PRIVATE)

    var reels by mutableStateOf<List<GalleryActivity>>(emptyList())
        private set

    private var activeEmail: String = prefs.getString("activeEmail", null).orEmpty()
    private var observeJob: Job? = null

    init {
        // Seed-only on init. Caller picks the mode via show* — this avoids a
        // race where Profile briefly shows feed data before switching to mine.
        if (activeEmail.isNotBlank()) {
            viewModelScope.launch { seedIfNeeded(activeEmail, "You") }
        }
    }

    /** Cross-user feed. Used by the bottom-nav Gallery tab. */
    fun showFeed(email: String, displayName: String) {
        if (email.isBlank()) return
        activeEmail = email
        viewModelScope.launch {
            seedIfNeeded(email, displayName)
            seedDemoAuthorsIfNeeded()
            repository.startMineSync(viewModelScope, email)
            observe(repository.observeFeed())
        }
    }

    /** Only this user's own posts. Used by ProfileScreen. */
    fun showMyPosts(email: String, displayName: String) {
        if (email.isBlank()) return
        activeEmail = email
        viewModelScope.launch {
            seedIfNeeded(email, displayName)
            repository.startMineSync(viewModelScope, email)
            observe(repository.observeMine(email))
        }
    }

    /** Another user's profile gallery. */
    fun showAuthorGallery(authorName: String) {
        observe(repository.observeByAuthor(authorName))
    }

    /**
     * Create a new post for the active user. imageUri may be null if the
     * caller wants to fall back to a default drawable.
     */
    fun createPost(
        caption: String,
        activity: String,
        distanceKm: String,
        imageUri: String?
    ) {
        val email = activeEmail.ifBlank { return }
        viewModelScope.launch {
            repository.createPost(email, caption, activity, distanceKm, imageUri, R.drawable.lakesidetrail)
        }
    }

    fun clearActiveUser() {
        observeJob?.cancel()
        repository.stopSync()
        activeEmail = ""
        reels = emptyList()
    }

    // ---- private ----

    private fun observe(flow: kotlinx.coroutines.flow.Flow<List<MediaEntity>>) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            flow.collect { rows -> reels = rows.map { it.toModel() } }
        }
    }

    private suspend fun seedIfNeeded(email: String, displayName: String) {
        if (repository.countMedia(email) == 0) {
            repository.insertAllMedia(seedReelsFor(email, displayName))
        }
    }

    /**
     * Seed a few demo authors so the cross-user Gallery feed has variety even
     * before other accounts sign up. Gated by a one-time prefs flag so it
     * doesn't duplicate on every showFeed call. Local-only (demo runners have no
     * cloud identity).
     */
    private suspend fun seedDemoAuthorsIfNeeded() {
        if (prefs.getBoolean(KEY_DEMO_SEED, false)) return
        val now = System.currentTimeMillis()
        val demos = listOf(
            DemoPost("sarah@demo.app", "Sarah Tan", "New PB on the hill repeats!", "Run", "8.0", R.drawable.running),
            DemoPost("daniel@demo.app", "Daniel Lee", "Easy recovery spin 🚴", "Cycle", "18.4", R.drawable.lingkunganilmu),
            DemoPost("aisha@demo.app", "Aisha Rahman", "Sunset trail walk", "Walk", "3.1", R.drawable.teratai),
            DemoPost("sarah@demo.app", "Sarah Tan", "Park loop with the dog", "Walk", "2.8", R.drawable.lakesidetrail),
            DemoPost("daniel@demo.app", "Daniel Lee", "Long ride out to the coast", "Cycle", "42.1", R.drawable.lingkunganilmu),
            DemoPost("aisha@demo.app", "Aisha Rahman", "Hill repeats — brutal", "Run", "7.5", R.drawable.teratai),
        )
        repository.insertAllMedia(demos.mapIndexed { i, d ->
            MediaEntity(
                id = UUID.randomUUID().toString(),
                ownerEmail = d.ownerEmail,
                author = d.author,
                caption = d.caption,
                activity = d.activity,
                distanceKm = d.distanceKm,
                tint = 0xFF1E3A5F,
                imageRes = d.imageRes,
                imageUri = null,
                likes = 50 + i * 20,
                createdAtMs = now - (i + 1) * 60_000L
            )
        })
        prefs.edit().putBoolean(KEY_DEMO_SEED, true).apply()
    }

    private data class DemoPost(
        val ownerEmail: String,
        val author: String,
        val caption: String,
        val activity: String,
        val distanceKm: String,
        val imageRes: Int
    )

    private fun seedReelsFor(email: String, displayName: String): List<MediaEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            Triple("Morning loop around the lake 🌅", "Run", "5.2") to R.drawable.lakesidetrail,
            Triple("Tempo intervals on the track", "Run", "6.4") to R.drawable.running,
            Triple("Easy shakeout", "Run", "3.0") to R.drawable.teratai
        ).mapIndexed { index, (info, image) ->
            val (caption, activity, distance) = info
            MediaEntity(
                id = UUID.randomUUID().toString(),
                ownerEmail = email,
                author = displayName,
                caption = caption,
                activity = activity,
                distanceKm = distance,
                tint = 0xFF1E3A5F,
                imageRes = image,
                imageUri = null,
                likes = 0,
                createdAtMs = now - index * 1000L
            )
        }
    }

    companion object {
        private const val KEY_DEMO_SEED = "galleryDemoSeeded"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as RunTrackApplication
                GalleryViewModel(app, app.galleryRepository)
            }
        }
    }
}

private fun MediaEntity.toModel() = GalleryActivity(
    id = id,
    author = author,
    caption = caption,
    activity = activity,
    distanceKm = distanceKm,
    tint = tint,
    imageRes = imageRes,
    imageUri = imageUri,
    likes = likes
)
