package com.example.a211198_hasif_drnelson_Project2.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface WikidataApi {
    // Batch-fetch claims for several entities at once (ids joined by "|", max 50).
    // We only read the P18 "image" claim. Free/keyless, no auth.
    @GET("w/api.php")
    suspend fun getEntities(
        @Query("ids") ids: String,
        @Query("action") action: String = "wbgetentities",
        @Query("props") props: String = "claims",
        @Query("format") format: String = "json"
    ): WikidataResponse
}
