package com.example.a211198_hasif_drnelson_Project2.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface FoursquareApi {
    // Auth + version headers are added by an OkHttp interceptor (NetworkModule).
    @GET("places/search")
    suspend fun searchPlaces(
        @Query("ll") ll: String,                 // "lat,lng"
        @Query("query") query: String = "park",
        @Query("radius") radius: Int = 8000,
        @Query("sort") sort: String = "DISTANCE",
        @Query("limit") limit: Int = 10,
        @Query("fields") fields: String = "name,distance,categories,photos"
    ): PlaceSearchResponse
}
