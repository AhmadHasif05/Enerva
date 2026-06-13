# Weekend Run Spots (Foursquare Places API) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the Home "Plan Your Weekend Run" section show **real nearby places to run** (parks) fetched live from the Foursquare Places REST API — each card showing a photo + name + distance — and keep them bookmark-saveable (photo included), with a graceful fallback to the existing hardcoded routes when offline or unconfigured.

**Architecture:** Adds a thin Retrofit/Moshi networking layer (`data/remote/`) and a `RunSpotRepository` that fetches places, maps them to the **existing `RunRoute` model** (extended with a nullable `imageUrl`), and falls back to the hardcoded `routeList` on any error. `UserViewModel` exposes a `weekendSpots` state (Loading/Success/Fallback) resolved from device last-known location (fused) with a fixed-city fallback. `WeekendRunSection` consumes it and loads photos with Coil. Bookmarks persist the photo URL via a new nullable Room column (`MIGRATION_6_7`) and Firestore field.

**Tech Stack:** Kotlin, Retrofit + Moshi (codegen) + OkHttp logging-interceptor (all already declared), Coil, Room, Play Services Location, Jetpack Compose, JUnit.

---

## File Structure

| File | Responsibility |
|------|----------------|
| `app/build.gradle.kts` | Read `FOURSQUARE_API_KEY` from `local.properties` → `BuildConfig` |
| `app/src/main/AndroidManifest.xml` | Ensure `INTERNET` permission |
| `model/RunRoute.kt` | Add nullable `imageUrl` to `RunRoute` |
| `data/entities/Entities.kt` | Add nullable `imageUrl` to `SavedRouteEntity` |
| `data/AppDatabase.kt` | `MIGRATION_6_7` + bump `version` 6→7 |
| `data/cloud/FirestoreSchema.kt` | Add `imageUrl` to `SavedRouteDoc` |
| `data/repository/UserRepository.kt` | Thread `imageUrl` through `SavedRouteEntity.toDoc()` / doc→entity |
| `view_model/UserViewModel.kt` | Thread `imageUrl` through `RunRoute.toEntity` / `SavedRouteEntity.toModel` |
| `data/remote/PlaceDtos.kt` | Moshi DTOs + pure `photoUrl()` + `formatDistance()` helpers |
| `data/remote/FoursquareApi.kt` | Retrofit interface (`places/search`) |
| `data/remote/NetworkModule.kt` | Retrofit/OkHttp/Moshi factory + auth interceptor |
| `data/repository/RunSpotRepository.kt` | Fetch + map DTO→`RunRoute` + fallback; pure `toRunRoute()` mapper |
| `view_model/WeekendRunUiState.kt` | Sealed UI state |
| `view/screen/HomeScreen.kt` | `WeekendRunSection` consumes state; `RouteCard` photo + metadata tweak |
| `RunTrackApplication.kt` | Own `RunSpotRepository` singleton |
| `app/src/test/.../PlaceDtoMapperTest.kt` | Unit tests for helpers + mapper |
| `docs/setupfoursquare.md` | How to get + configure the API key |

---

## Task 1: Build config + manifest for the API key

**Files:**
- Modify: `app/build.gradle.kts` (key read block ~line 18; `buildConfigField` ~line 39)
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Read the key from local.properties**

In `app/build.gradle.kts`, after the `mapTilerApiKey` line (~line 19), add:

```kotlin
val foursquareApiKey: String = localProps.getProperty("FOURSQUARE_API_KEY", "")
```

- [ ] **Step 2: Expose it via BuildConfig**

In the `defaultConfig { ... }` block, after the `MAPTILER_API_KEY` line (~line 39), add:

```kotlin
buildConfigField("String", "FOURSQUARE_API_KEY", "\"$foursquareApiKey\"")
```

- [ ] **Step 3: Ensure INTERNET permission**

Open `app/src/main/AndroidManifest.xml`. If `<uses-permission android:name="android.permission.INTERNET" />` is not already present, add it as the first child of `<manifest>` (above `<application>`).

- [ ] **Step 4: Add your key to local.properties (local only — not committed)**

Append to `local.properties` at the repo root (leave blank for now if you don't have a key yet; the app falls back to sample routes):

```
FOURSQUARE_API_KEY=YOUR_KEY_HERE
```

- [ ] **Step 5: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (a new `BuildConfig.FOURSQUARE_API_KEY` field is generated).

- [ ] **Step 6: Commit**

```bash
git add app/build.gradle.kts app/src/main/AndroidManifest.xml
git commit -m "feat: wire FOURSQUARE_API_KEY buildConfig + INTERNET permission"
```

---

## Task 2: Add `imageUrl` to the route model + Room (migration 6→7)

This lets a saved spot keep its real photo. All changes are additive and nullable.

**Files:**
- Modify: `model/RunRoute.kt:9-16`
- Modify: `data/entities/Entities.kt:42-51`
- Modify: `data/AppDatabase.kt` (version + migrations)
- Modify: `data/cloud/FirestoreSchema.kt` (`SavedRouteDoc`)
- Modify: `data/repository/UserRepository.kt` (`SavedRouteEntity.toDoc()` + doc→entity)
- Modify: `view_model/UserViewModel.kt:305-321` (mappers)

- [ ] **Step 1: Add `imageUrl` to `RunRoute`**

In `model/RunRoute.kt`, change the `RunRoute` data class to:

```kotlin
data class RunRoute(
    val title: String,
    val distance: String,
    val time: String,
    val elevation: String,
    val difficulty: String,
    @DrawableRes val imageRes: Int,
    val imageUrl: String? = null   // remote photo (Foursquare); null → use imageRes
)
```

(The existing `routeList` entries keep working — `imageUrl` defaults to `null`.)

- [ ] **Step 2: Add `imageUrl` to `SavedRouteEntity`**

In `data/entities/Entities.kt`, change `SavedRouteEntity` to:

```kotlin
@Entity(tableName = "saved_routes", primaryKeys = ["ownerEmail", "title"])
data class SavedRouteEntity(
    val ownerEmail: String,
    val title: String,
    val distance: String,
    val time: String,
    val elevation: String,
    val difficulty: String,
    val imageRes: Int,
    val imageUrl: String? = null
)
```

- [ ] **Step 3: Add `MIGRATION_6_7` and bump the version**

In `data/AppDatabase.kt`: change `version = 6` to `version = 7`. Add this migration object after `MIGRATION_4_5` (inside the `companion object`):

```kotlin
// v6 → v7 (Weekend Run Spots): saved routes fetched from Foursquare carry a
// remote photo URL. Additive nullable ADD COLUMN — existing rows preserved.
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE saved_routes ADD COLUMN imageUrl TEXT")
    }
}
```

Then register it in the builder — change:

```kotlin
.addMigrations(MIGRATION_3_4, MIGRATION_4_5)
```
to:
```kotlin
.addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_6_7)
```

- [ ] **Step 4: Add `imageUrl` to `SavedRouteDoc`**

In `data/cloud/FirestoreSchema.kt`, find `SavedRouteDoc` and add a nullable `imageUrl` field (default `null`) alongside the existing fields, e.g.:

```kotlin
val imageUrl: String? = null
```

(Keep it consistent with the other fields' style; it must have a default so Firestore deserialization of older docs still works.)

- [ ] **Step 5: Thread `imageUrl` through the repository mappers**

In `data/repository/UserRepository.kt`, update `SavedRouteEntity.toDoc()` (~line 399) to include `imageUrl = imageUrl`. Then find the inverse mapping (where a `SavedRouteDoc` is turned back into a `SavedRouteEntity` from the Firestore listener) and add `imageUrl = <doc>.imageUrl` there too. (Search the file for `SavedRouteEntity(` to find the doc→entity construction.)

- [ ] **Step 6: Thread `imageUrl` through the ViewModel mappers**

In `view_model/UserViewModel.kt`, update both mappers at the bottom:

```kotlin
private fun RunRoute.toEntity(ownerEmail: String) = SavedRouteEntity(
    ownerEmail = ownerEmail,
    title = title,
    distance = distance,
    time = time,
    elevation = elevation,
    difficulty = difficulty,
    imageRes = imageRes,
    imageUrl = imageUrl
)

private fun SavedRouteEntity.toModel(): RunRoute? =
    routeList.firstOrNull { it.title == title }
        ?: RunRoute(title, distance, time, elevation, difficulty, imageRes, imageUrl)
```

- [ ] **Step 7: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 8: Run the existing unit tests (no regressions)**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL (existing tests still pass; mapper signatures changed but defaults keep callers valid).

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java
git commit -m "feat: persist route photo URL (SavedRoute.imageUrl + migration 6->7)"
```

---

## Task 3: Foursquare DTOs + pure helpers (TDD)

**Files:**
- Create: `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/data/remote/PlaceDtos.kt`
- Create: `app/src/test/java/com/example/a211198_hasif_drnelson_Project2/data/remote/PlaceDtoMapperTest.kt`

- [ ] **Step 1: Write the failing test for the pure helpers**

Create `PlaceDtoMapperTest.kt`:

```kotlin
package com.example.a211198_hasif_drnelson_Project2.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaceDtoMapperTest {

    @Test fun photoUrl_concatenates_prefix_size_suffix() {
        assertEquals(
            "https://fastly.4sqi.net/img/general/400x300/abc.jpg",
            photoUrl("https://fastly.4sqi.net/img/general/", "/abc.jpg", "400x300")
        )
    }

    @Test fun formatDistance_under_1km_shows_meters() {
        assertEquals("800 m away", formatDistance(800))
    }

    @Test fun formatDistance_at_or_over_1km_shows_one_decimal_km() {
        assertEquals("1.2 km away", formatDistance(1200))
        assertEquals("5.0 km away", formatDistance(5000))
    }

    @Test fun firstPhotoUrl_null_when_no_photos() {
        val dto = PlaceDto(name = "X", distance = 100, categories = emptyList(), photos = emptyList())
        assertNull(dto.firstPhotoUrl())
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*PlaceDtoMapperTest*"`
Expected: FAIL — unresolved references (`PlaceDto`, `photoUrl`, `formatDistance`).

- [ ] **Step 3: Implement the DTOs + helpers**

Create `PlaceDtos.kt`:

```kotlin
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
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*PlaceDtoMapperTest*"`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/a211198_hasif_drnelson_Project2/data/remote/PlaceDtos.kt app/src/test
git commit -m "feat: Foursquare place DTOs + photo-url/distance helpers (tested)"
```

---

## Task 4: DTO → `RunRoute` mapper (TDD)

The mapper stays R-free (no `imageRes` resource lookup) so it runs as a JVM unit test; `imageRes = 0` is a sentinel, and the UI supplies a placeholder painter for null photos.

**Files:**
- Create: `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/data/repository/RunSpotRepository.kt` (mapper portion this task)
- Modify: `app/src/test/java/com/example/a211198_hasif_drnelson_Project2/data/remote/PlaceDtoMapperTest.kt`

- [ ] **Step 1: Add the failing mapper test**

Append to `PlaceDtoMapperTest.kt` (add the import at the top:
`import com.example.a211198_hasif_drnelson_Project2.data.repository.toRunRoute`):

```kotlin
    @Test fun toRunRoute_maps_name_distance_category_photo() {
        val dto = PlaceDto(
            name = "Lake Gardens",
            distance = 1200,
            categories = listOf(CategoryDto("Park")),
            photos = listOf(PhotoDto("https://x/", "/p.jpg"))
        )
        val route = dto.toRunRoute()
        assertEquals("Lake Gardens", route.title)
        assertEquals("1.2 km away", route.distance)
        assertEquals("Park", route.difficulty)
        assertEquals("https://x/400x300/p.jpg", route.imageUrl)
        assertEquals("", route.time)
        assertEquals("", route.elevation)
        assertEquals(0, route.imageRes)
    }

    @Test fun toRunRoute_blank_category_falls_back_to_label() {
        val dto = PlaceDto(name = "Trailhead", distance = 300, categories = emptyList(), photos = emptyList())
        val route = dto.toRunRoute()
        assertEquals("Run spot", route.difficulty)
        assertNull(route.imageUrl)
    }
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*PlaceDtoMapperTest*"`
Expected: FAIL — unresolved reference `toRunRoute`.

- [ ] **Step 3: Implement the mapper (create the repository file with the mapper first)**

Create `RunSpotRepository.kt` with just the mapper for now (the fetch method is added in Task 6):

```kotlin
package com.example.a211198_hasif_drnelson_Project2.data.repository

import com.example.a211198_hasif_drnelson_Project2.data.remote.PlaceDto
import com.example.a211198_hasif_drnelson_Project2.data.remote.firstPhotoUrl
import com.example.a211198_hasif_drnelson_Project2.data.remote.formatDistance
import com.example.a211198_hasif_drnelson_Project2.model.RunRoute

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
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*PlaceDtoMapperTest*"`
Expected: PASS (6 tests total).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/a211198_hasif_drnelson_Project2/data/repository/RunSpotRepository.kt app/src/test
git commit -m "feat: map Foursquare place -> RunRoute (tested)"
```

---

## Task 5: Retrofit API + network module

> **Verification first:** Before wiring, confirm the live API shape. With a valid key run:
> ```
> curl "https://places-api.foursquare.com/places/search?ll=3.1390,101.6869&query=park&radius=8000&limit=3&fields=name,distance,categories,photos" \
>   -H "Authorization: Bearer YOUR_KEY" -H "X-Places-Api-Version: 2025-06-17" -H "Accept: application/json"
> ```
> Confirm the response has a top-level `results` array and each item has `name`, `distance`, `categories[].name`, `photos[].prefix/suffix`. If field names differ, adjust the DTOs in Task 3 before continuing. (No key yet? Skip the curl — the app's fallback path still lets everything build and run.)

**Files:**
- Create: `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/data/remote/FoursquareApi.kt`
- Create: `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/data/remote/NetworkModule.kt`

- [ ] **Step 1: Create the Retrofit interface**

```kotlin
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
```

- [ ] **Step 2: Create the network module**

```kotlin
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
```

> Note: `KotlinJsonAdapterFactory` is in `moshi-kotlin` (already a dep). The DTOs use `@JsonClass(generateAdapter = true)` (codegen) and will be used by the converter automatically; the reflective factory is a harmless safety net.

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/a211198_hasif_drnelson_Project2/data/remote
git commit -m "feat: Foursquare Retrofit interface + OkHttp/Moshi network module"
```

---

## Task 6: `RunSpotRepository.nearbyRunSpots()` + fallback

**Files:**
- Modify: `data/repository/RunSpotRepository.kt`

- [ ] **Step 1: Add the fetch method with fallback to the repository**

Edit `RunSpotRepository.kt` — add imports and a class above the existing `toRunRoute` mapper:

```kotlin
import com.example.a211198_hasif_drnelson_Project2.BuildConfig
import com.example.a211198_hasif_drnelson_Project2.data.remote.FoursquareApi
import com.example.a211198_hasif_drnelson_Project2.data.remote.NetworkModule
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
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Run unit tests (mapper still green)**

Run: `./gradlew :app:testDebugUnitTest --tests "*PlaceDtoMapperTest*"`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/a211198_hasif_drnelson_Project2/data/repository/RunSpotRepository.kt
git commit -m "feat: RunSpotRepository.nearbyRunSpots with offline fallback"
```

---

## Task 7: UI state + ViewModel wiring (state, location, repo)

**Files:**
- Create: `view_model/WeekendRunUiState.kt`
- Modify: `RunTrackApplication.kt`
- Modify: `view_model/UserViewModel.kt`

- [ ] **Step 1: Create the UI state**

Create `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view_model/WeekendRunUiState.kt`:

```kotlin
package com.example.a211198_hasif_drnelson_Project2.view_model

import com.example.a211198_hasif_drnelson_Project2.model.RunRoute

// State of the Home "Plan Your Weekend Run" section.
sealed interface WeekendRunUiState {
    data object Loading : WeekendRunUiState
    data class Success(val routes: List<RunRoute>) : WeekendRunUiState   // live Foursquare data
    data class Fallback(val routes: List<RunRoute>) : WeekendRunUiState  // offline/unconfigured samples
}
```

- [ ] **Step 2: Own the repository in the Application**

In `RunTrackApplication.kt`, add an import and a lazy singleton:

```kotlin
import com.example.a211198_hasif_drnelson_Project2.data.repository.RunSpotRepository
```
```kotlin
val runSpotRepository: RunSpotRepository by lazy { RunSpotRepository() }
```

- [ ] **Step 3: Add state + location + fetch to `UserViewModel`**

In `view_model/UserViewModel.kt`:

(a) Add imports (note `mutableStateOf`, `getValue`, `setValue` are already imported in this file):
```kotlin
import android.annotation.SuppressLint
import com.example.a211198_hasif_drnelson_Project2.data.repository.RunSpotRepository
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await
```

(b) Add a constructor parameter for the repo. Change the constructor to:
```kotlin
class UserViewModel(
    application: Application,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val runSpotRepository: RunSpotRepository
) : AndroidViewModel(application) {
```

(c) Update the `Factory` initializer to pass it:
```kotlin
UserViewModel(app, app.authRepository, app.userRepository, app.runSpotRepository)
```

(d) Add the observable state near the other `mutableStateOf` fields:
```kotlin
// Live "Plan Your Weekend Run" spots (Foursquare). Starts Loading; resolves to
// Success (live) or Fallback (samples) after the first fetch.
var weekendRun by mutableStateOf<WeekendRunUiState>(WeekendRunUiState.Loading)
    private set
```

(e) Kick off the fetch from `init {}` (add at the end of the existing `init` block):
```kotlin
loadWeekendRunSpots()
```

(f) Add the fetch + location helper methods (Kuala Lumpur is the fixed fallback location):
```kotlin
private val DEFAULT_LAT = 3.1390
private val DEFAULT_LNG = 101.6869

fun loadWeekendRunSpots() {
    viewModelScope.launch {
        val (lat, lng) = lastKnownLatLng()
        val result = runSpotRepository.nearbyRunSpots(lat, lng)
        weekendRun = if (result.isLive) WeekendRunUiState.Success(result.routes)
        else WeekendRunUiState.Fallback(result.routes)
    }
}

// Best-effort last-known location; falls back to a fixed city when permission
// isn't granted or no fix is cached. Reuses the location permission already
// requested by the Record screen — no new prompt here.
@SuppressLint("MissingPermission")
private suspend fun lastKnownLatLng(): Pair<Double, Double> = runCatching {
    val client = LocationServices.getFusedLocationProviderClient(getApplication<Application>())
    val loc = client.lastLocation.await()
    if (loc != null) loc.latitude to loc.longitude else DEFAULT_LAT to DEFAULT_LNG
}.getOrElse { DEFAULT_LAT to DEFAULT_LNG }
```

- [ ] **Step 4: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java
git commit -m "feat: UserViewModel weekendRun state + location-based fetch"
```

---

## Task 8: Render live spots in `WeekendRunSection`

**Files:**
- Modify: `view/screen/HomeScreen.kt` (`WeekendRunSection` ~line 211; `RouteCard` ~line 249)

- [ ] **Step 1: Consume the state in `WeekendRunSection`**

Replace the body of `WeekendRunSection` (the `LazyRow { items(routeList) {...} }` part) so it reads the ViewModel state. Update the function to:

```kotlin
@Composable
fun WeekendRunSection(userViewModel: UserViewModel) {
    val state = userViewModel.weekendRun
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Plan Your Weekend Run",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, fontSize = 24.sp)
            )
            Text(
                text = "Real places to run near you",
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.5.sp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        when (state) {
            is WeekendRunUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(380.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
            is WeekendRunUiState.Success -> RouteRow(state.routes, userViewModel)
            is WeekendRunUiState.Fallback -> RouteRow(state.routes, userViewModel)
        }
    }
}

@Composable
private fun RouteRow(routes: List<RunRoute>, userViewModel: UserViewModel) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(routes) { route ->
            WorkoutCardVisible(
                route = route,
                saved = userViewModel.isRouteSaved(route.title),
                onSaveToggle = { userViewModel.toggleRouteSave(route) }
            )
        }
    }
}
```

Ensure these imports exist at the top of `HomeScreen.kt` (add any that are missing):
```kotlin
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import com.example.a211198_hasif_drnelson_Project2.model.RunRoute
import com.example.a211198_hasif_drnelson_Project2.view_model.WeekendRunUiState
```

- [ ] **Step 2: Make `RouteCard` use the remote photo + omit blank metadata**

In `RouteCard`, change the `AsyncImage` model and the metadata `Text` so a discovered spot (remote photo, blank time/elevation) renders cleanly.

Change the image line:
```kotlin
AsyncImage(model = route.imageRes, contentDescription = route.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
```
to:
```kotlin
AsyncImage(
    model = route.imageUrl ?: route.imageRes,
    contentDescription = route.title,
    modifier = Modifier.fillMaxSize(),
    contentScale = ContentScale.Crop,
    fallback = painterResource(R.drawable.teratai),
    error = painterResource(R.drawable.teratai)
)
```

Change the metadata `Text` (currently `"${route.distance} (${route.time}) • ${route.elevation}"`) to:
```kotlin
Text(
    text = if (route.time.isBlank() && route.elevation.isBlank()) route.distance
           else "${route.distance} (${route.time}) • ${route.elevation}",
    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
)
```

Ensure these imports exist at the top of `HomeScreen.kt`:
```kotlin
import androidx.compose.ui.res.painterResource
import com.example.a211198_hasif_drnelson_Project2.R
```

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Build the debug APK**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/HomeScreen.kt
git commit -m "feat: render live Foursquare run spots in Weekend Run section"
```

---

## Task 9: Setup doc + plan status

**Files:**
- Create: `docs/setupfoursquare.md`
- Modify: `plan.md` (Phase 6 row status)

- [ ] **Step 1: Write the setup doc**

Create `docs/setupfoursquare.md`:

```markdown
# Foursquare Places API setup

The Home "Plan Your Weekend Run" section fetches real nearby places to run from
the Foursquare Places API. Without a key the app still works — it shows the
built-in sample routes.

## Get a key (free, no credit card)
1. Sign up at https://foursquare.com/developers/
2. Create a project / app and generate a **Places API** key (Service Key /
   Bearer token).
3. Add it to `local.properties` at the repo root (git-ignored):

   ```
   FOURSQUARE_API_KEY=YOUR_KEY_HERE
   ```
4. Rebuild. The section now shows live nearby parks with photos.

## Notes
- Location uses the device's last-known GPS fix (the same permission the Record
  screen requests); with no fix it defaults to Kuala Lumpur (3.1390, 101.6869).
- API: `GET https://places-api.foursquare.com/places/search`, headers
  `Authorization: Bearer <key>` and `X-Places-Api-Version: 2025-06-17`.
```

- [ ] **Step 2: Mark Phase 6 done in plan.md**

In `plan.md`, change the Phase 6 roadmap row status from `🔜 Designed` to `✅ Done` (keep the spec link) once on-device verification (next task) passes. (If verification is deferred, leave as designed.)

- [ ] **Step 3: Commit**

```bash
git add docs/setupfoursquare.md plan.md
git commit -m "docs: Foursquare key setup + mark Phase 6 status"
```

---

## Task 10: On-device verification

- [ ] **Step 1: Install on a physical device**

Run: `./gradlew :app:installDebug` (the debug build is `armeabi-v7a`-only per plan.md; use a matching device or add an ABI).

- [ ] **Step 2: Verify the live path (with key + network)**

Open the app → Home. Expected: the "Plan Your Weekend Run" row shows **real nearby parks** with photos and "x.x km away" distances. Tap a bookmark → it saves; reopen Profile/saved list to confirm it persists with its photo.

- [ ] **Step 3: Verify the fallback path**

Enable airplane mode (or leave `FOURSQUARE_API_KEY` blank) and relaunch. Expected: the section shows the two **sample routes** (Teratai, Lakeside Trail) with their drawable images — no crash, the spinner resolves.

- [ ] **Step 4: Final commit (if plan.md flipped to Done)**

```bash
git add plan.md
git commit -m "docs: Phase 6 verified on-device"
```

---

## Self-Review notes

- **Spec coverage:** networking layer (Tasks 3,5), repository + fallback (Tasks 4,6), VM state (Task 7), UI (Task 8), location with fixed fallback (Task 7), save with photo via migration (Task 2), mapper unit test (Tasks 3,4), build config + setup doc (Tasks 1,9). All spec §9 file-change rows are covered.
- **Deviation from spec (intentional, simpler):** reuse the existing `RunRoute` model + save pipeline (extended with nullable `imageUrl`) instead of a separate `RunSpot` model — fewer new surfaces, save "just works". The DB touch is `MIGRATION_6_7` (DB is already at v6), not v5→v6 as the spec guessed.
- **Types consistent:** `PlaceSearchResponse`/`PlaceDto`/`CategoryDto`/`PhotoDto`, `photoUrl`/`formatDistance`/`firstPhotoUrl`/`toRunRoute`, `RunSpotResult{routes,isLive}`, `WeekendRunUiState{Loading,Success,Fallback}`, `weekendRun` state, `nearbyRunSpots(lat,lng)` — used identically across tasks.
- **API risk:** Task 5 opens with a `curl` verification step to confirm the live field names before relying on them.
```
