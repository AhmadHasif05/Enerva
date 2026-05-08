package com.example.a211198_hasif_drnelson_Project1.view_model

// SystemClock.elapsedRealtime() is monotonic — it can't go backwards even if
// the user changes the device clock. Best choice for measuring elapsed time.
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Math helpers for the Haversine distance formula below.
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// One sample on the user's GPS trail. timeMs lets us compute speed when the
// device-reported speed isn't available.
data class TrackPoint(val lat: Double, val lng: Double, val timeMs: Long)

// Backs RecordScreen. Owns timer + GPS-derived stats + breadcrumb path.
class RecordViewModel : ViewModel() {

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

    // Great-circle distance between two lat/lng pairs in kilometres.
    // Standard Haversine formula — accurate enough for activity tracking.
    private fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).let { it * it } +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLng / 2).let { it * it }
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}

// Format raw seconds into "mm:ss" or "h:mm:ss" depending on length.
fun formatElapsed(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}