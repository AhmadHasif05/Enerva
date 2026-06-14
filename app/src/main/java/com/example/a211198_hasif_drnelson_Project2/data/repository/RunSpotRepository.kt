package com.example.a211198_hasif_drnelson_Project2.data.repository

import com.example.a211198_hasif_drnelson_Project2.data.remote.PlaceDto
import com.example.a211198_hasif_drnelson_Project2.data.remote.firstPhotoUrl
import com.example.a211198_hasif_drnelson_Project2.data.remote.formatDistance
import com.example.a211198_hasif_drnelson_Project2.model.RunRoute

// Map a Foursquare place to the app's RunRoute model. imageRes = 0 is a sentinel:
// live spots render from imageUrl; the UI supplies a placeholder painter when the
// photo is missing. time/elevation are blank (unknown for a discovered place) and
// the card omits them when blank.
fun PlaceDto.toRunRoute(): RunRoute = RunRoute(
    title = name,
    distance = formatDistance(distance),
    time = "",
    elevation = "",
    difficulty = categories.firstOrNull()?.name?.ifBlank { null } ?: "Run spot",
    imageRes = 0,
    imageUrl = firstPhotoUrl()
)
