package com.example.a211198_hasif_drnelson_Project2.data.repository

import com.example.a211198_hasif_drnelson_Project2.BuildConfig
import com.example.a211198_hasif_drnelson_Project2.data.remote.FoursquareApi
import com.example.a211198_hasif_drnelson_Project2.data.remote.NetworkModule
import com.example.a211198_hasif_drnelson_Project2.data.remote.PlaceDto
import com.example.a211198_hasif_drnelson_Project2.data.remote.firstPhotoUrl
import com.example.a211198_hasif_drnelson_Project2.data.remote.formatDistance
import com.example.a211198_hasif_drnelson_Project2.model.RunRoute
import com.example.a211198_hasif_drnelson_Project2.model.routeList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Result of a run-spot fetch: live = from Foursquare, fallback = hardcoded samples.
data class RunSpotResult(val routes: List<RunRoute>, val isLive: Boolean)

class RunSpotRepository(
    private val api: FoursquareApi = NetworkModule.foursquareApi
) {
    // Fetch nearby run-friendly places. Any failure (blank key, IO, parse, empty)
    // returns the hardcoded sample routes so the UI always has something to show.
    suspend fun nearbyRunSpots(lat: Double, lng: Double): RunSpotResult =
        withContext(Dispatchers.IO) {
            if (BuildConfig.FOURSQUARE_API_KEY.isBlank()) return@withContext fallback()
            runCatching {
                val results = api.searchPlaces(ll = "$lat,$lng").results
                    .map { it.toRunRoute() }
                if (results.isEmpty()) fallback() else RunSpotResult(results, isLive = true)
            }.getOrElse { fallback() }
        }

    private fun fallback() = RunSpotResult(routeList, isLive = false)
}

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
