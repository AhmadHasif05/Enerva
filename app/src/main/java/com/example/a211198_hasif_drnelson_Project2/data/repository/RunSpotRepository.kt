package com.example.a211198_hasif_drnelson_Project2.data.repository

import com.example.a211198_hasif_drnelson_Project2.R
import com.example.a211198_hasif_drnelson_Project2.data.remote.NetworkModule
import com.example.a211198_hasif_drnelson_Project2.data.remote.OverpassApi
import com.example.a211198_hasif_drnelson_Project2.data.remote.OverpassElement
import com.example.a211198_hasif_drnelson_Project2.data.remote.WikidataApi
import com.example.a211198_hasif_drnelson_Project2.data.remote.categoryLabel
import com.example.a211198_hasif_drnelson_Project2.data.remote.categoryStockUrl
import com.example.a211198_hasif_drnelson_Project2.data.remote.commonsFilePathUrl
import com.example.a211198_hasif_drnelson_Project2.data.remote.directPhotoUrl
import com.example.a211198_hasif_drnelson_Project2.data.remote.formatDistance
import com.example.a211198_hasif_drnelson_Project2.data.remote.haversineMeters
import com.example.a211198_hasif_drnelson_Project2.data.remote.imageFileName
import com.example.a211198_hasif_drnelson_Project2.data.remote.latLon
import com.example.a211198_hasif_drnelson_Project2.data.remote.wikidataId
import com.example.a211198_hasif_drnelson_Project2.model.RunRoute
import com.example.a211198_hasif_drnelson_Project2.model.routeList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Result of a run-spot fetch: live = from OpenStreetMap, fallback = hardcoded samples.
data class RunSpotResult(val routes: List<RunRoute>, val isLive: Boolean)

class RunSpotRepository(
    private val api: OverpassApi = NetworkModule.overpassApi,
    private val wikidata: WikidataApi = NetworkModule.wikidataApi
) {
    // Fetch nearby run-friendly places from OpenStreetMap (Overpass API) and a real
    // photo for each (Wikidata P18 -> Wikimedia Commons). Any failure (IO, parse,
    // empty) returns the hardcoded sample routes so the UI always has something to
    // show. Everything here is free and keyless — no API key required.
    suspend fun nearbyRunSpots(lat: Double, lng: Double): RunSpotResult =
        withContext(Dispatchers.IO) {
            runCatching {
                // Nearest named places, paired with their distance in metres.
                val nearest = api.query(overpassQuery(lat, lng)).elements
                    .mapNotNull { el ->
                        val name = el.tags["name"]?.takeIf { it.isNotBlank() }
                            ?: return@mapNotNull null
                        val (plat, plon) = el.latLon() ?: return@mapNotNull null
                        el to haversineMeters(lat, lng, plat, plon)
                    }
                    .sortedBy { (_, meters) -> meters }
                    .take(MAX_RESULTS)

                val wikidataPhotos = resolveWikidataPhotos(nearest.map { it.first })

                val routes = nearest.mapIndexed { index, (el, meters) ->
                    // Real photo of this place if we have one; else a category-appropriate
                    // stock photo so every card shows an image. Bundled drawable is only a
                    // last resort if even the stock URL fails to load.
                    val photo = el.directPhotoUrl()
                        ?: el.wikidataId()?.let { wikidataPhotos[it] }
                        ?: el.categoryStockUrl()
                    RunRoute(
                        title = el.tags["name"]!!,
                        distance = formatDistance(meters),
                        time = "",
                        elevation = "",
                        difficulty = el.categoryLabel(),
                        imageRes = PLACEHOLDERS[index % PLACEHOLDERS.size],
                        imageUrl = photo
                    )
                }
                if (routes.isEmpty()) fallback() else RunSpotResult(routes, isLive = true)
            }.getOrElse { e ->
                android.util.Log.w("RunSpotRepository", "nearbyRunSpots failed; using samples", e)
                fallback()
            }
        }

    // One batched Wikidata lookup: Q-id -> Commons photo URL, for places that have a
    // `wikidata` tag but no directly-usable photo tag. A failure here is non-fatal —
    // those cards just fall back to a bundled cover image.
    private suspend fun resolveWikidataPhotos(elements: List<OverpassElement>): Map<String, String> {
        val ids = elements
            .filter { it.directPhotoUrl() == null }
            .mapNotNull { it.wikidataId() }
            .distinct()
        if (ids.isEmpty()) return emptyMap()
        return runCatching {
            wikidata.getEntities(ids = ids.joinToString("|")).entities
                .mapNotNull { (qid, entity) ->
                    entity.imageFileName()?.let { qid to commonsFilePathUrl(it) }
                }
                .toMap()
        }.getOrElse { e ->
            android.util.Log.w("RunSpotRepository", "wikidata photo lookup failed", e)
            emptyMap()
        }
    }

    private fun fallback() = RunSpotResult(routeList, isLive = false)

    companion object {
        private const val MAX_RESULTS = 10
        private const val FETCH_LIMIT = 60   // over-fetch so "nearest 10" is meaningful
        private const val RADIUS_M = 8000

        // Rotating cover images for live spots that have no real photo.
        private val PLACEHOLDERS = intArrayOf(R.drawable.teratai, R.drawable.lakesidetrail)

        // Overpass QL: nearby run-friendly leisure areas (nodes, ways, relations).
        // `out center` gives ways/relations a single coordinate to measure from.
        private fun overpassQuery(lat: Double, lng: Double): String = """
            [out:json][timeout:25];
            (
              nwr["leisure"~"^(park|garden|nature_reserve|recreation_ground|track|sports_centre)$"](around:$RADIUS_M,$lat,$lng);
            );
            out center $FETCH_LIMIT;
        """.trimIndent()
    }
}
