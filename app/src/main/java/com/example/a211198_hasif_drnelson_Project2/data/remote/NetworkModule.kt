package com.example.a211198_hasif_drnelson_Project2.data.remote

import com.example.a211198_hasif_drnelson_Project2.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

// Builds the Foursquare Retrofit client. The API key + version are injected by an
// interceptor from BuildConfig (blank key is fine — the repository falls back to
// sample routes before any call is made).
object NetworkModule {
    private const val BASE_URL = "https://places-api.foursquare.com/"
    private const val API_VERSION = "2025-06-17"

    val foursquareApi: FoursquareApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
            else HttpLoggingInterceptor.Level.NONE
        }
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer ${BuildConfig.FOURSQUARE_API_KEY}")
                    .addHeader("X-Places-Api-Version", API_VERSION)
                    .addHeader("Accept", "application/json")
                    .build()
                chain.proceed(req)
            }
            .addInterceptor(logging)
            .build()
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(FoursquareApi::class.java)
    }
}
