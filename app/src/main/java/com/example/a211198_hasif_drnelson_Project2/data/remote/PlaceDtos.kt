package com.example.a211198_hasif_drnelson_Project2.data.remote

import com.squareup.moshi.JsonClass

// Top-level Foursquare /places/search response: { "results": [ ... ] }
@JsonClass(generateAdapter = true)
data class PlaceSearchResponse(
    val results: List<PlaceDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class PlaceDto(
    val name: String = "",
    val distance: Int = 0,                       // meters from the query point
    val categories: List<CategoryDto> = emptyList(),
    val photos: List<PhotoDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class CategoryDto(
    val name: String = ""
)

@JsonClass(generateAdapter = true)
data class PhotoDto(
    val prefix: String = "",
    val suffix: String = ""
)

// Build a usable Foursquare photo URL: prefix + size + suffix.
fun photoUrl(prefix: String, suffix: String, size: String = "400x300"): String =
    "$prefix$size$suffix"

// First photo of a place as a ready URL, or null when the place has no photo.
fun PlaceDto.firstPhotoUrl(): String? =
    photos.firstOrNull()?.let { photoUrl(it.prefix, it.suffix) }

// Human-friendly distance: "800 m away" under 1 km, else "1.2 km away".
fun formatDistance(meters: Int): String =
    if (meters >= 1000) String.format("%.1f km away", meters / 1000.0)
    else "$meters m away"
