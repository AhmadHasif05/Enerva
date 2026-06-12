package com.example.a211198_hasif_drnelson_Project2.view_model

// SystemClock.elapsedRealtime() is monotonic — it can't go backwards even if
// the user changes the device clock. Best choice for measuring elapsed time.
import android.app.Application
import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.a211198_hasif_drnelson_Project2.RunTrackApplication
import com.example.a211198_hasif_drnelson_Project2.data.AppDatabase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// Backs RecordScreen. Owns the timer + breadcrumb state; delegates GPS gating
// and entity construction to the pure units (RouteAccumulator, RunStats).
class RecordViewModel(application: Application) : AndroidViewModel(application) {

    private val activityDao = AppDatabase.get(application).activityDao()
    private val userDao = AppDatabase.get(application).userDao()
    private val galleryRepository =
        (application as RunTrackApplication).galleryRepository
    private val prefs = application.getSharedPreferences("runtrack", Context.MODE_PRIVATE)

    // Pure GPS logic; the observable state below mirrors its kept points.
    private val route = RouteAccumulator()

    // True while the play button is engaged. Pause flips it to false.
    var isRecording by mutableStateOf(false)
        private set

    // How many seconds of recording have accumulated so far.
    var elapsedSeconds by mutableStateOf(0L)
        private set

    // Total distance in kilometres (mirrors RouteAccumulator.distanceKm).
    var distanceKm by mutableStateOf(0.0)
        private set

    // Selected activity type for this run ("Run" or "Walk"); drives the chip,
    // the stats header, and the type stored on save. "Jog" is treated as "Run".
    var activityType by mutableStateOf("Run")
        private set

    fun toggleActivityType() {
        activityType = if (activityType == "Run") "Walk" else "Run"
    }

    // True once a run is underway (timer running or any GPS point captured). Used
    // to lock the activity-type choice after recording starts.
    val hasStarted: Boolean get() = elapsedSeconds > 0L || path.isNotEmpty()

    // Compose-observable breadcrumb — RecordScreen draws this as a Polyline.
    val path: MutableList<TrackPoint> = mutableStateListOf()

    // Internal timing bookkeeping for the start/pause/resume cycle.
    private var startElapsedMs: Long = 0L
    private var accumulatedMs: Long = 0L
    private var timerJob: Job? = null

    fun start() {
        if (isRecording) return
        isRecording = true
        startElapsedMs = SystemClock.elapsedRealtime()
        timerJob = viewModelScope.launch {
            while (isRecording) {
                val live = SystemClock.elapsedRealtime() - startElapsedMs
                elapsedSeconds = (accumulatedMs + live) / 1000
                delay(200) // 5 Hz refresh
            }
        }
    }

    fun pause() {
        if (!isRecording) return
        isRecording = false
        timerJob?.cancel()
        timerJob = null
        accumulatedMs += SystemClock.elapsedRealtime() - startElapsedMs
    }

    fun reset() {
        isRecording = false
        timerJob?.cancel()
        timerJob = null
        accumulatedMs = 0L
        elapsedSeconds = 0L
        distanceKm = 0.0
        route.reset()
        path.clear()
        activityType = "Run"
    }

    // Feed a new GPS sample. accuracyM is the fix's horizontal accuracy in
    // metres (null if unknown). Only fixes that pass the gates are kept.
    fun onLocation(lat: Double, lng: Double, accuracyM: Float?, timeMs: Long) {
        if (!isRecording) return
        if (route.addFix(lat, lng, accuracyM, timeMs)) {
            path.add(route.points.last())
            distanceKm = route.distanceKm
        }
    }

    // Persist the current run to Room: an ActivityRecord plus a gallery reel.
    // imageUri is the route-snapshot path, or null for a stats-only post.
    fun saveActivity(
        type: String = "Run",
        caption: String = "New run",
        imageUri: String? = null,
        isCard: Boolean = false,
        imageBytes: ByteArray? = null,
    ) {
        if (!shouldSaveRun(elapsedSeconds, distanceKm, imageUri != null)) return
        val email = prefs.getString("activeEmail", null).orEmpty()
        if (email.isBlank()) return
        val now = System.currentTimeMillis()
        val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(now))
        val record = buildRunRecord(
            id = UUID.randomUUID().toString(),
            ownerEmail = email,
            caption = caption,
            type = type,
            distanceKm = distanceKm,
            elapsedSeconds = elapsedSeconds,
            dateStr = dateStr,
        )
        viewModelScope.launch {
            val author = userDao.findByEmail(email)?.runnerName ?: "You"
            val media = buildRunMedia(
                id = UUID.randomUUID().toString(),
                ownerEmail = email,
                author = author,
                caption = caption,
                type = type,
                distanceKm = distanceKm,
                imageUri = imageUri,
                createdAtMs = now,
                isCard = isCard,
            )
            activityDao.insertActivity(record)
            activityDao.insertMedia(media)
            galleryRepository.pushMediaToCloud(media, imageBytes)
        }
    }
}

val RecordViewModelFactory: ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
            as RunTrackApplication
        RecordViewModel(app)
    }
}

// Format raw seconds into "mm:ss" or "h:mm:ss" depending on length.
fun formatElapsed(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
