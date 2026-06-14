package com.example.a211198_hasif_drnelson_Project2.data.remote

import com.example.a211198_hasif_drnelson_Project2.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

// Builds the keyless map-data clients used by the Weekend Run section:
//  - Overpass (OpenStreetMap) for nearby places
//  - Wikidata for each place's photo (P18 -> Wikimedia Commons)
// Both are free and need no auth — just a polite User-Agent (their etiquette) and
// generous timeouts because the public endpoints can be slow under load.
object NetworkModule {
    private const val OVERPASS_URL = "https://overpass-api.de/"
    private const val WIKIDATA_URL = "https://www.wikidata.org/"
    private const val USER_AGENT = "Enerva-Android/1.0 (weekend-run-spots)"

    private val client: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
            else HttpLoggingInterceptor.Level.NONE
        }
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .addHeader("User-Agent", USER_AGENT)
                    .addHeader("Accept", "application/json")
                    .build()
                chain.proceed(req)
            }
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val moshi: Moshi by lazy { Moshi.Builder().add(KotlinJsonAdapterFactory()).build() }

    private fun retrofit(baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val overpassApi: OverpassApi by lazy { retrofit(OVERPASS_URL).create(OverpassApi::class.java) }
    val wikidataApi: WikidataApi by lazy { retrofit(WIKIDATA_URL).create(WikidataApi::class.java) }
}
