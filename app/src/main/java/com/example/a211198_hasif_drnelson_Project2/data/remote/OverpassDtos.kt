package com.example.a211198_hasif_drnelson_Project2.data.remote

import com.squareup.moshi.JsonClass
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// OpenStreetMap Overpass API response: { "elements": [ ... ] }.
// We query run-friendly leisure areas (parks, gardens, etc.) near a point.
@JsonClass(generateAdapter = true)
data class OverpassResponse(
    val elements: List<OverpassElement> = emptyList()
)

// One OSM element. Nodes carry lat/lon directly; ways/relations carry a computed
// `center` (because we ask Overpass for "out center"). Tags hold name/category.
@JsonClass(generateAdapter = true)
data class OverpassElement(
    val type: String = "",
    val lat: Double? = null,
    val lon: Double? = null,
    val center: OverpassCenter? = null,
    val tags: Map<String, String> = emptyMap()
)

@JsonClass(generateAdapter = true)
data class OverpassCenter(
    val lat: Double = 0.0,
    val lon: Double = 0.0
)

// Best coordinate for an element: its own lat/lon, else its center, else null.
fun OverpassElement.latLon(): Pair<Double, Double>? {
    val la = lat ?: center?.lat
    val lo = lon ?: center?.lon
    return if (la != null && lo != null) la to lo else null
}

// Friendly category label from the `leisure` tag, e.g. "nature_reserve" ->
// "Nature reserve". Falls back to a generic label.
fun OverpassElement.categoryLabel(): String {
    val raw = tags["leisure"] ?: tags["landuse"] ?: return "Run spot"
    return raw.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

private val IMAGE_EXT = Regex("\\.(jpg|jpeg|png|webp|gif)$", RegexOption.IGNORE_CASE)

// A directly-usable photo URL from this element's own tags, or null. Only handles
// what we can render without a second request: a direct `image=` file URL, or a
// `wikimedia_commons=File:...` tag. (Commons "Category:" values and `wikidata`
// Q-ids need a follow-up lookup — see RunSpotRepository.) Google Photos share
// links (photos.app.goo.gl) are HTML pages, not images, so they're rejected here.
fun OverpassElement.directPhotoUrl(): String? {
    tags["image"]?.let { img ->
        if (img.startsWith("http") && IMAGE_EXT.containsMatchIn(img.substringBefore('?'))) return img
    }
    tags["wikimedia_commons"]?.let { c ->
        if (c.startsWith("File:")) return commonsFilePathUrl(c.removePrefix("File:"))
    }
    return null
}

// The Wikidata entity id (e.g. "Q6332465") tagged on this element, or null.
fun OverpassElement.wikidataId(): String? = tags["wikidata"]?.takeIf { it.startsWith("Q") }

// Build a Wikimedia Commons image URL from a file name. Special:FilePath redirects
// to the actual media and honours a width param for a sane thumbnail size.
fun commonsFilePathUrl(fileName: String, width: Int = 400): String {
    val encoded = java.net.URLEncoder.encode(fileName, "UTF-8").replace("+", "%20")
    return "https://commons.wikimedia.org/wiki/Special:FilePath/$encoded?width=$width"
}

// Curated, verified Wikimedia Commons photos per leisure category. Used as a stock
// cover when a place has no real photo of its own, so every card still shows a
// category-appropriate image. Keyless (loaded via commonsFilePathUrl).
private val CATEGORY_STOCK = mapOf(
    "park" to "Turtle Pond, Central Park, New York City, 20231004 1640 2116.jpg",
    "garden" to "Balchik Botanical Garden 2017 E6.jpg",
    "nature_reserve" to "Nature Trail, lake reserve forest, Sukhna Lake, Chandigarh 01.jpg",
    "track" to "Running track near Japan National Stadium 2.jpg",
    "recreation_ground" to "Playing field, Princess May Recreation Ground, Penzance, Cornwall - April 2024.jpg",
    "sports_centre" to "Greenock Sports Centre - geograph.org.uk - 563424.jpg"
)

// A category-appropriate stock photo URL for this element (falls back to the park
// image for unknown categories). Always returns a URL.
fun OverpassElement.categoryStockUrl(): String {
    val key = tags["leisure"]
    val file = CATEGORY_STOCK[key] ?: CATEGORY_STOCK.getValue("park")
    return commonsFilePathUrl(file)
}

// Great-circle distance in metres between two lat/lng points (Haversine).
fun haversineMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Int {
    val r = 6_371_000.0 // Earth radius in metres
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
        sin(dLng / 2) * sin(dLng / 2)
    return (r * 2 * atan2(sqrt(a), sqrt(1 - a))).toInt()
}

// Human-friendly distance: "800 m away" under 1 km, else "1.2 km away".
fun formatDistance(meters: Int): String =
    if (meters >= 1000) String.format("%.1f km away", meters / 1000.0)
    else "$meters m away"
