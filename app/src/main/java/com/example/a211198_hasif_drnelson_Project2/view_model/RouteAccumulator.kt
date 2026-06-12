package com.example.a211198_hasif_drnelson_Project2.view_model

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// One sample on the user's GPS trail. timeMs lets us compute pace later.
data class TrackPoint(val lat: Double, val lng: Double, val timeMs: Long)

// Pure GPS gating + distance accumulation. No Android dependencies, so the
// jitter/accuracy rules are unit-testable on the JVM. RecordViewModel drives
// this and mirrors kept points into Compose-observable state.
class RouteAccumulator(
    // 50 m, not 25 m: real phones (especially indoors / first fixes) routinely
    // report 30–60 m accuracy. A 25 m gate dropped every fix, leaving the screen
    // stuck on "Waiting for GPS fix...". 50 m still rejects genuinely wild fixes.
    private val minAccuracyM: Float = 50f,
    private val minMoveM: Double = 4.0,
) {
    private val _points = mutableListOf<TrackPoint>()
    val points: List<TrackPoint> get() = _points

    var distanceKm: Double = 0.0
        private set

    // Returns true if the fix passed the gates and was appended.
    fun addFix(lat: Double, lng: Double, accuracyM: Float?, timeMs: Long): Boolean {
        val prev = _points.lastOrNull()
        if (prev == null) {
            // First fix always anchors the trail, even on a poor reading — this is
            // what clears "Waiting for GPS fix..." and gives the run a start point.
            _points.add(TrackPoint(lat, lng, timeMs))
            return true
        }
        // Accuracy gate (subsequent fixes only): a low-confidence fix is what
        // makes the line zig-zag, so drop it.
        if (accuracyM != null && accuracyM > minAccuracyM) return false
        // Jitter gate: standing still must not accrue distance.
        val moveM = haversineKm(prev.lat, prev.lng, lat, lng) * 1000.0
        if (moveM < minMoveM) return false
        distanceKm += moveM / 1000.0
        _points.add(TrackPoint(lat, lng, timeMs))
        return true
    }

    fun reset() {
        _points.clear()
        distanceKm = 0.0
    }
}

// Great-circle distance between two lat/lng pairs in kilometres (Haversine).
fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val r = 6371.0 // Earth radius in km
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = sin(dLat / 2).let { it * it } +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
        sin(dLng / 2).let { it * it }
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}
