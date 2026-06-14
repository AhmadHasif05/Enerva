package com.example.a211198_hasif_drnelson_Project2.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface OverpassApi {
    // Runs a raw Overpass QL query. The query string is built by RunSpotRepository
    // and URL-encoded by Retrofit. No API key is required — Overpass is free/keyless.
    @GET("api/interpreter")
    suspend fun query(@Query("data") data: String): OverpassResponse
}
