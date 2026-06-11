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
import com.example.a211198_hasif_drnelson_Project2.R
import com.example.a211198_hasif_drnelson_Project2.RunTrackApplication
import com.example.a211198_hasif_drnelson_Project2.data.AppDatabase
import com.example.a211198_hasif_drnelson_Project2.data.entities.ActivityRecordEntity
import com.example.a211198_hasif_drnelson_Project2.data.entities.MediaEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// Backs RecordScreen. Owns timer + GPS-derived stats + breadcrumb path.
class RecordViewModel(application: Application) : AndroidViewModel(application) {

    private val activityDao = AppDatabase.get(application).activityDao()
    private val prefs = application.getSharedPreferences("runtrack", Context.MODE_PRIVATE)

    // True while the play button is engaged. Pause flips it to false.
    var isRecording by mutableStateOf(false)
        private set

    // How many seconds of recording have accumulated so far.
    var elapsedSeconds by mutableStateOf(0L)
        private set

    // Total distance in kilometres (sum of segment lengths between fixes).
    var distanceKm by mutableStateOf(0.0)
        private set

    // Most recent instantaneous speed in km/h.
    var currentSpeedKmh by mutableStateOf(0.0)
        private set

    // Compose-observable list of breadcrumb points — TrailCanvas reads it.
    val path: MutableList<TrackPoint> = mutableStateListOf()

    // Internal timing bookkeeping for the start/pause/resume cycle.
    private var startElapsedMs: Long = 0L   // SystemClock.elapsedRealtime() at last start()
    private var accumulatedMs: Long = 0L    // Total ms accumulated across previous segments
    private var timerJob: Job? = null       // The coroutine that ticks elapsedSeconds

    // Begin / resume recording. Idempotent if already recording.
    fun start() {
        if (isRecording) return
        isRecording = true
        startElapsedMs = SystemClock.elapsedRealtime()
        // Launch a coroutine that ticks elapsedSeconds while we're recording.
        // viewModelScope auto-cancels if the ViewModel is cleared.
        timerJob = viewModelScope.launch {
            while (isRecording) {
                val live = SystemClock.elapsedRealtime() - startElapsedMs
                elapsedSeconds = (accumulatedMs + live) / 1000
                delay(200) // 5 Hz refresh — smooth enough for the eye, easy on the CPU
            }
        }
    }

    // Stop ticking but keep the elapsed time and path so resume continues from here.
    fun pause() {
        if (!isRecording) return
        isRecording = false
        timerJob?.cancel()
        timerJob = null
        accumulatedMs += SystemClock.elapsedRealtime() - startElapsedMs
    }

    // Wipe everything — used by the Stop and Refresh buttons.
    fun reset() {
        isRecording = false
        timerJob?.cancel()
        timerJob = null
        accumulatedMs = 0L
        elapsedSeconds = 0L
        distanceKm = 0.0
        currentSpeedKmh = 0.0
        path.clear()
    }

    // Feed a new GPS sample. Speed is in m/s if known (else null, and we compute from positions).
    fun onLocation(lat: Double, lng: Double, speedMps: Float?, timeMs: Long) {
        // Ignore samples received while paused so unwanted distance isn't logged.
        if (!isRecording) return
        val previous = path.lastOrNull()
        path.add(TrackPoint(lat, lng, timeMs))

        if (previous != null) {
            val segmentKm = haversineKm(previous.lat, previous.lng, lat, lng)
            distanceKm += segmentKm

            currentSpeedKmh = if (speedMps != null && speedMps > 0f) {
                // Device reported speed — convert m/s -> km/h.
                speedMps * 3.6
            } else {
                // Fall back to dividing distance by time between samples.
                val dtSec = (timeMs - previous.timeMs).coerceAtLeast(1L) / 1000.0
                if (dtSec > 0) (segmentKm / dtSec) * 3600.0 else 0.0
            }
        }
    }

    // Persist the current run to Room. Inserts both an ActivityRecord and a
    // gallery MediaEntity so the activity shows up in the reels feed. Safe to
    // call from a button — does nothing if there's no recorded distance yet.
    fun saveActivity(type: String = "Run", caption: String = "New run") {
        if (elapsedSeconds == 0L && distanceKm == 0.0) return
        val email = prefs.getString("activeEmail", null).orEmpty()
        if (email.isBlank()) return
        val now = System.currentTimeMillis()
        val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(now))
        val record = ActivityRecordEntity(
            id = UUID.randomUUID().toString(),
            ownerEmail = email,
            type = type,
            title = caption,
            date = dateStr,
            distanceKm = distanceKm,
            durationMinutes = (elapsedSeconds / 60).toInt(),
            elevationM = 0,
            avgPace = formatElapsed(elapsedSeconds)
        )
        val media = MediaEntity(
            id = UUID.randomUUID().toString(),
            ownerEmail = email,
            author = "You",
            caption = caption,
            activity = type,
            distanceKm = "%.1f".format(distanceKm),
            tint = 0xFF1E3A5F,
            imageRes = R.drawable.lakesidetrail,
            imageUri = null,
            likes = 0,
            createdAtMs = now
        )
        viewModelScope.launch {
            activityDao.insertActivity(record)
            activityDao.insertMedia(media)
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