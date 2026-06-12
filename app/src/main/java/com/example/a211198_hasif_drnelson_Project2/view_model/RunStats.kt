package com.example.a211198_hasif_drnelson_Project2.view_model

import com.example.a211198_hasif_drnelson_Project2.R
import com.example.a211198_hasif_drnelson_Project2.data.entities.ActivityRecordEntity
import com.example.a211198_hasif_drnelson_Project2.data.entities.MediaEntity

// Below this distance the pace number is noise, so we show a placeholder.
private const val MIN_PACE_DISTANCE_KM = 0.01

// Average pace as "m:ss" per km (e.g. "6:12"). The UI appends " /km".
fun formatPace(elapsedSeconds: Long, distanceKm: Double): String {
    if (distanceKm < MIN_PACE_DISTANCE_KM || elapsedSeconds <= 0) return "--:--"
    val secPerKm = elapsedSeconds / distanceKm
    val m = (secPerKm / 60).toInt()
    val s = (secPerKm % 60).toInt()
    return "%d:%02d".format(m, s)
}

// A run is worth saving if it captured anything at all: elapsed time, distance,
// or an image the user chose to post. The image clause is what lets a no-movement
// photo post (0 time, 0 distance) survive — without it such posts were silently
// dropped before reaching Room, so they never showed in the gallery.
fun shouldSaveRun(elapsedSeconds: Long, distanceKm: Double, hasImage: Boolean): Boolean =
    elapsedSeconds > 0L || distanceKm > 0.0 || hasImage

// Build the gallery reel for a finished run. imageUri is the route snapshot path
// (or null for a stats-only post, which falls back to the drawable in the UI).
fun buildRunMedia(
    id: String,
    ownerEmail: String,
    author: String,
    caption: String,
    type: String,
    distanceKm: Double,
    imageUri: String?,
    createdAtMs: Long,
    isCard: Boolean = false,
): MediaEntity = MediaEntity(
    id = id,
    ownerEmail = ownerEmail,
    author = author,
    caption = caption,
    activity = type,
    distanceKm = "%.1f".format(distanceKm),
    tint = 0xFF1E3A5F,
    imageRes = R.drawable.lakesidetrail,
    imageUri = imageUri,
    likes = 0,
    createdAtMs = createdAtMs,
    isCard = isCard,
)

// Build the activity-history record for a finished run.
fun buildRunRecord(
    id: String,
    ownerEmail: String,
    caption: String,
    type: String,
    distanceKm: Double,
    elapsedSeconds: Long,
    dateStr: String,
): ActivityRecordEntity = ActivityRecordEntity(
    id = id,
    ownerEmail = ownerEmail,
    type = type,
    title = caption,
    date = dateStr,
    distanceKm = distanceKm,
    durationMinutes = (elapsedSeconds / 60).toInt(),
    elevationM = 0,
    avgPace = formatPace(elapsedSeconds, distanceKm),
)
