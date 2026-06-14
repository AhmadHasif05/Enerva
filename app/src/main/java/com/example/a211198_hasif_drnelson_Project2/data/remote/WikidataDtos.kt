package com.example.a211198_hasif_drnelson_Project2.data.remote

import com.squareup.moshi.JsonClass

// Wikidata wbgetentities response: { "entities": { "Q123": { "claims": {...} } } }.
// Keyed by entity id. We only care about the P18 ("image") claim of each entity.
@JsonClass(generateAdapter = true)
data class WikidataResponse(
    val entities: Map<String, WikidataEntity> = emptyMap()
)

@JsonClass(generateAdapter = true)
data class WikidataEntity(
    // Keyed by property id (e.g. "P18"). `value` varies by property type, so it's
    // kept as Any? — for P18 (commonsMedia) it's the image file name string.
    val claims: Map<String, List<WikidataClaim>> = emptyMap()
)

@JsonClass(generateAdapter = true)
data class WikidataClaim(
    val mainsnak: WikidataSnak? = null
)

@JsonClass(generateAdapter = true)
data class WikidataSnak(
    val datavalue: WikidataDatavalue? = null
)

@JsonClass(generateAdapter = true)
data class WikidataDatavalue(
    val value: Any? = null
)

// The Commons image file name from this entity's P18 claim, or null. P18's value
// is a plain file-name string; other claim types are ignored.
fun WikidataEntity.imageFileName(): String? =
    claims["P18"]?.firstOrNull()?.mainsnak?.datavalue?.value as? String
