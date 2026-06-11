# Record Screen Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Polish the Record screen — clean GPS route, valid stats, average pace, a minimizable info bar, and an "End" button that opens a summary sheet to post the run (with a route picture + caption) to the gallery reels feed.

**Architecture:** Extract the GPS gating + distance math into a pure, JVM-testable `RouteAccumulator`, and pace/entity construction into pure top-level functions, leaving `RecordViewModel` as a thin Compose-observable wrapper. The End flow captures a static route image via MapLibre `MapSnapshotter` and shows a Material3 `ModalBottomSheet`; posting writes the bitmap to internal storage and stores its path in the existing `MediaEntity.imageUri`, so the gallery renders it with no gallery-side changes.

**Tech Stack:** Kotlin, Jetpack Compose, Material3, MapLibre Android SDK (via ramani-maplibre 0.12.0), Room, JUnit4.

**Spec:** `docs/superpowers/specs/2026-06-11-record-screen-polish-design.md`

**Status:** ✅ COMPLETE (2026-06-11). All 8 tasks implemented via subagent-driven development, each with spec + code-quality review. Unit tests green (`RouteAccumulatorTest` 7, `RunStatsTest` 6); `assembleDebug` succeeds. Follow-up fixes applied beyond the original steps: extra null-accuracy jitter test, 48dp touch target on the minimize button, `MapSnapshotter` cancellation handle, and posted runs now use the user's runner name as author (final-review fix). Remaining: the Task 8 manual on-device GPS/map check is the user's to run.

---

## File Structure

- **Create** `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view_model/RouteAccumulator.kt` — pure GPS gating + distance accumulation, owns `TrackPoint` and `haversineKm`.
- **Create** `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view_model/RunStats.kt` — pure `formatPace` + entity builders (`buildRunMedia`, `buildRunRecord`).
- **Create** `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/RouteSnapshot.kt` — MapLibre snapshot capture + bitmap-to-file helper.
- **Create** `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/RunSummarySheet.kt` — the End summary bottom sheet composable.
- **Modify** `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view_model/RecordViewModel.kt` — delegate to the pure units; replace speed with distance-only state; extend `saveActivity`.
- **Modify** `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/RecordScreen.kt` — forward accuracy; pace display; collapsible info bar; End button wiring.
- **Create tests** under `app/src/test/java/com/example/a211198_hasif_drnelson_Project2/view_model/`.

---

## Task 1: Pure RouteAccumulator (GPS gates + distance)

**Files:**
- Create: `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view_model/RouteAccumulator.kt`
- Test: `app/src/test/java/com/example/a211198_hasif_drnelson_Project2/view_model/RouteAccumulatorTest.kt`

- [x] **Step 1: Write the failing test**

Create `RouteAccumulatorTest.kt`:

```kotlin
package com.example.a211198_hasif_drnelson_Project2.view_model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteAccumulatorTest {

    @Test
    fun `first fix is always kept and adds no distance`() {
        val acc = RouteAccumulator()
        val kept = acc.addFix(1.0, 103.0, accuracyM = 5f, timeMs = 0L)
        assertTrue(kept)
        assertEquals(1, acc.points.size)
        assertEquals(0.0, acc.distanceKm, 1e-9)
    }

    @Test
    fun `a fix worse than the accuracy gate is rejected`() {
        val acc = RouteAccumulator(minAccuracyM = 25f)
        val kept = acc.addFix(1.0, 103.0, accuracyM = 40f, timeMs = 0L)
        assertFalse(kept)
        assertEquals(0, acc.points.size)
    }

    @Test
    fun `a sub-threshold move is rejected as jitter`() {
        val acc = RouteAccumulator(minMoveM = 4.0)
        acc.addFix(1.0, 103.0, accuracyM = 5f, timeMs = 0L)
        // ~0.1 m away — well under the 4 m jitter gate.
        val kept = acc.addFix(1.000001, 103.0, accuracyM = 5f, timeMs = 1000L)
        assertFalse(kept)
        assertEquals(1, acc.points.size)
        assertEquals(0.0, acc.distanceKm, 1e-9)
    }

    @Test
    fun `a real move is kept and accrues distance`() {
        val acc = RouteAccumulator()
        acc.addFix(1.0, 103.0, accuracyM = 5f, timeMs = 0L)
        // ~111 m north (0.001 deg latitude).
        val kept = acc.addFix(1.001, 103.0, accuracyM = 5f, timeMs = 1000L)
        assertTrue(kept)
        assertEquals(2, acc.points.size)
        assertEquals(0.111, acc.distanceKm, 0.002)
    }

    @Test
    fun `null accuracy passes the accuracy gate`() {
        val acc = RouteAccumulator()
        val kept = acc.addFix(1.0, 103.0, accuracyM = null, timeMs = 0L)
        assertTrue(kept)
    }

    @Test
    fun `reset clears points and distance`() {
        val acc = RouteAccumulator()
        acc.addFix(1.0, 103.0, accuracyM = 5f, timeMs = 0L)
        acc.addFix(1.001, 103.0, accuracyM = 5f, timeMs = 1000L)
        acc.reset()
        assertEquals(0, acc.points.size)
        assertEquals(0.0, acc.distanceKm, 1e-9)
    }
}
```

- [x] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*.RouteAccumulatorTest"`
Expected: FAIL — `RouteAccumulator` / `TrackPoint` unresolved (compile error).

- [x] **Step 3: Write minimal implementation**

Create `RouteAccumulator.kt`:

```kotlin
package com.example.a211198_hasif_drnelson_Project2.view_model

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// One sample on the user's GPS trail. timeMs lets us compute pace later.
data class TrackPoint(val lat: Double, val lng: Double, val timeMs: Long)

// Pure GPS gating + distance accumulation. No Android dependencies, so the
// jitter/accuracy rules are unit-testable on the JVM. RecordViewModel drives
// this and mirrors kept points into Compose-observable state.
class RouteAccumulator(
    private val minAccuracyM: Float = 25f,
    private val minMoveM: Double = 4.0,
) {
    private val _points = mutableListOf<TrackPoint>()
    val points: List<TrackPoint> get() = _points

    var distanceKm: Double = 0.0
        private set

    // Returns true if the fix passed both gates and was appended.
    fun addFix(lat: Double, lng: Double, accuracyM: Float?, timeMs: Long): Boolean {
        // Accuracy gate: a low-confidence fix is what makes the line zig-zag.
        if (accuracyM != null && accuracyM > minAccuracyM) return false

        val prev = _points.lastOrNull()
        if (prev != null) {
            // Jitter gate: standing still must not accrue distance.
            val moveM = haversineKm(prev.lat, prev.lng, lat, lng) * 1000.0
            if (moveM < minMoveM) return false
            distanceKm += moveM / 1000.0
        }
        _points.add(TrackPoint(lat, lng, timeMs))
        return true
    }

    fun reset() {
        _points.clear()
        distanceKm = 0.0
    }
}

// Great-circle distance between two lat/lng pairs in kilometres (Haversine).
fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val r = 6371.0 // Earth radius in km
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = sin(dLat / 2).let { it * it } +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
        sin(dLng / 2).let { it * it }
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}
```

- [x] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*.RouteAccumulatorTest"`
Expected: PASS (6 tests).

- [x] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view_model/RouteAccumulator.kt \
        app/src/test/java/com/example/a211198_hasif_drnelson_Project2/view_model/RouteAccumulatorTest.kt
git commit -m "P4.5: pure RouteAccumulator — GPS accuracy/jitter gates + distance"
```

---

## Task 2: Pure pace formatting + run entity builders

**Files:**
- Create: `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view_model/RunStats.kt`
- Test: `app/src/test/java/com/example/a211198_hasif_drnelson_Project2/view_model/RunStatsTest.kt`

- [x] **Step 1: Write the failing test**

Create `RunStatsTest.kt`:

```kotlin
package com.example.a211198_hasif_drnelson_Project2.view_model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RunStatsTest {

    @Test
    fun `pace formats minutes and seconds per km`() {
        // 6 min 12 s over 1 km => "6:12".
        assertEquals("6:12", formatPace(elapsedSeconds = 372, distanceKm = 1.0))
    }

    @Test
    fun `pace pads seconds`() {
        // 300 s / 1 km = 5:00.
        assertEquals("5:00", formatPace(elapsedSeconds = 300, distanceKm = 1.0))
    }

    @Test
    fun `pace is placeholder below the minimum distance`() {
        assertEquals("--:--", formatPace(elapsedSeconds = 30, distanceKm = 0.0))
        assertEquals("--:--", formatPace(elapsedSeconds = 30, distanceKm = 0.005))
    }

    @Test
    fun `buildRunMedia carries caption and image uri`() {
        val media = buildRunMedia(
            id = "m1",
            ownerEmail = "me@example.com",
            caption = "Evening loop",
            type = "Walk",
            distanceKm = 2.345,
            imageUri = "/data/run/snap.png",
            createdAtMs = 1_700_000_000_000L,
        )
        assertEquals("m1", media.id)
        assertEquals("me@example.com", media.ownerEmail)
        assertEquals("You", media.author)
        assertEquals("Evening loop", media.caption)
        assertEquals("Walk", media.activity)
        assertEquals("2.3", media.distanceKm)
        assertEquals("/data/run/snap.png", media.imageUri)
        assertEquals(0, media.likes)
        assertEquals(1_700_000_000_000L, media.createdAtMs)
    }

    @Test
    fun `buildRunMedia allows a null image uri for stats-only posts`() {
        val media = buildRunMedia(
            id = "m2",
            ownerEmail = "me@example.com",
            caption = "No photo",
            type = "Run",
            distanceKm = 1.0,
            imageUri = null,
            createdAtMs = 0L,
        )
        assertNull(media.imageUri)
    }

    @Test
    fun `buildRunRecord captures distance duration and pace`() {
        val record = buildRunRecord(
            id = "r1",
            ownerEmail = "me@example.com",
            caption = "Evening loop",
            type = "Walk",
            distanceKm = 2.0,
            elapsedSeconds = 600,
            dateStr = "Jun 11, 2026",
        )
        assertEquals("r1", record.id)
        assertEquals("Walk", record.type)
        assertEquals("Evening loop", record.title)
        assertEquals("Jun 11, 2026", record.date)
        assertEquals(2.0, record.distanceKm, 1e-9)
        assertEquals(10, record.durationMinutes)
        // avgPace stores the formatted pace string.
        assertEquals("5:00", record.avgPace)
    }
}
```

- [x] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*.RunStatsTest"`
Expected: FAIL — `formatPace` / `buildRunMedia` / `buildRunRecord` unresolved.

- [x] **Step 3: Write minimal implementation**

Create `RunStats.kt`:

```kotlin
package com.example.a211198_hasif_drnelson_Project2.view_model

import com.example.a211198_hasif_drnelson_Project2.R
import com.example.a211198_hasif_drnelson_Project2.data.entities.ActivityRecordEntity
import com.example.a211198_hasif_drnelson_Project2.data.entities.MediaEntity

// Below this distance the pace number is noise, so we show a placeholder.
private const val MIN_PACE_DISTANCE_KM = 0.01

// Average pace as "m:ss" per km (e.g. "6:12"). The UI appends " /km".
fun formatPace(elapsedSeconds: Long, distanceKm: Double): String {
    if (distanceKm < MIN_PACE_DISTANCE_KM || elapsedSeconds <= 0) return "--:--"
    val secPerKm = elapsedSeconds / distanceKm
    val m = (secPerKm / 60).toInt()
    val s = (secPerKm % 60).toInt()
    return "%d:%02d".format(m, s)
}

// Build the gallery reel for a finished run. imageUri is the route snapshot path
// (or null for a stats-only post, which falls back to the drawable in the UI).
fun buildRunMedia(
    id: String,
    ownerEmail: String,
    caption: String,
    type: String,
    distanceKm: Double,
    imageUri: String?,
    createdAtMs: Long,
): MediaEntity = MediaEntity(
    id = id,
    ownerEmail = ownerEmail,
    author = "You",
    caption = caption,
    activity = type,
    distanceKm = "%.1f".format(distanceKm),
    tint = 0xFF1E3A5F,
    imageRes = R.drawable.lakesidetrail,
    imageUri = imageUri,
    likes = 0,
    createdAtMs = createdAtMs,
)

// Build the activity-history record for a finished run.
fun buildRunRecord(
    id: String,
    ownerEmail: String,
    caption: String,
    type: String,
    distanceKm: Double,
    elapsedSeconds: Long,
    dateStr: String,
): ActivityRecordEntity = ActivityRecordEntity(
    id = id,
    ownerEmail = ownerEmail,
    type = type,
    title = caption,
    date = dateStr,
    distanceKm = distanceKm,
    durationMinutes = (elapsedSeconds / 60).toInt(),
    elevationM = 0,
    avgPace = formatPace(elapsedSeconds, distanceKm),
)
```

- [x] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*.RunStatsTest"`
Expected: PASS (6 tests).

- [x] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view_model/RunStats.kt \
        app/src/test/java/com/example/a211198_hasif_drnelson_Project2/view_model/RunStatsTest.kt
git commit -m "P4.5: pure pace formatter + run entity builders"
```

---

## Task 3: Refactor RecordViewModel onto the pure units

**Files:**
- Modify: `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view_model/RecordViewModel.kt`

This task removes the now-duplicated `TrackPoint`, `haversineKm`, and the speed
state from the ViewModel, delegates gating to `RouteAccumulator`, and extends
`saveActivity`. No new test — behaviour is covered by Tasks 1–2; we verify by
compiling and running the existing suite.

- [x] **Step 1: Replace the ViewModel body**

Replace the entire contents of `RecordViewModel.kt` with:

```kotlin
package com.example.a211198_hasif_drnelson_Project2.view_model

// SystemClock.elapsedRealtime() is monotonic — it can't go backwards even if
// the user changes the device clock. Best choice for measuring elapsed time.
import android.app.Application
import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.a211198_hasif_drnelson_Project2.RunTrackApplication
import com.example.a211198_hasif_drnelson_Project2.data.AppDatabase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// Backs RecordScreen. Owns the timer + breadcrumb state; delegates GPS gating
// and entity construction to the pure units (RouteAccumulator, RunStats).
class RecordViewModel(application: Application) : AndroidViewModel(application) {

    private val activityDao = AppDatabase.get(application).activityDao()
    private val prefs = application.getSharedPreferences("runtrack", Context.MODE_PRIVATE)

    // Pure GPS logic; the observable state below mirrors its kept points.
    private val route = RouteAccumulator()

    // True while the play button is engaged. Pause flips it to false.
    var isRecording by mutableStateOf(false)
        private set

    // How many seconds of recording have accumulated so far.
    var elapsedSeconds by mutableStateOf(0L)
        private set

    // Total distance in kilometres (mirrors RouteAccumulator.distanceKm).
    var distanceKm by mutableStateOf(0.0)
        private set

    // Compose-observable breadcrumb — RecordScreen draws this as a Polyline.
    val path: MutableList<TrackPoint> = mutableStateListOf()

    // Internal timing bookkeeping for the start/pause/resume cycle.
    private var startElapsedMs: Long = 0L
    private var accumulatedMs: Long = 0L
    private var timerJob: Job? = null

    fun start() {
        if (isRecording) return
        isRecording = true
        startElapsedMs = SystemClock.elapsedRealtime()
        timerJob = viewModelScope.launch {
            while (isRecording) {
                val live = SystemClock.elapsedRealtime() - startElapsedMs
                elapsedSeconds = (accumulatedMs + live) / 1000
                delay(200) // 5 Hz refresh
            }
        }
    }

    fun pause() {
        if (!isRecording) return
        isRecording = false
        timerJob?.cancel()
        timerJob = null
        accumulatedMs += SystemClock.elapsedRealtime() - startElapsedMs
    }

    fun reset() {
        isRecording = false
        timerJob?.cancel()
        timerJob = null
        accumulatedMs = 0L
        elapsedSeconds = 0L
        distanceKm = 0.0
        route.reset()
        path.clear()
    }

    // Feed a new GPS sample. accuracyM is the fix's horizontal accuracy in
    // metres (null if unknown). Only fixes that pass the gates are kept.
    fun onLocation(lat: Double, lng: Double, accuracyM: Float?, timeMs: Long) {
        if (!isRecording) return
        if (route.addFix(lat, lng, accuracyM, timeMs)) {
            path.add(route.points.last())
            distanceKm = route.distanceKm
        }
    }

    // Persist the current run to Room: an ActivityRecord plus a gallery reel.
    // imageUri is the route-snapshot path, or null for a stats-only post.
    fun saveActivity(type: String = "Run", caption: String = "New run", imageUri: String? = null) {
        if (elapsedSeconds == 0L && distanceKm == 0.0) return
        val email = prefs.getString("activeEmail", null).orEmpty()
        if (email.isBlank()) return
        val now = System.currentTimeMillis()
        val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(now))
        val record = buildRunRecord(
            id = UUID.randomUUID().toString(),
            ownerEmail = email,
            caption = caption,
            type = type,
            distanceKm = distanceKm,
            elapsedSeconds = elapsedSeconds,
            dateStr = dateStr,
        )
        val media = buildRunMedia(
            id = UUID.randomUUID().toString(),
            ownerEmail = email,
            caption = caption,
            type = type,
            distanceKm = distanceKm,
            imageUri = imageUri,
            createdAtMs = now,
        )
        viewModelScope.launch {
            activityDao.insertActivity(record)
            activityDao.insertMedia(media)
        }
    }
}

val RecordViewModelFactory: ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
            as RunTrackApplication
        RecordViewModel(app)
    }
}

// Format raw seconds into "mm:ss" or "h:mm:ss" depending on length.
fun formatElapsed(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
```

- [x] **Step 2: Compile and run the full unit suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS — all tests compile and pass. (`currentSpeedKmh`, the old
`TrackPoint`/`haversineKm` definitions, and the `R`/entity imports are gone from
this file; they now live in `RouteAccumulator.kt` / `RunStats.kt`.)

- [x] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view_model/RecordViewModel.kt
git commit -m "P4.5: RecordViewModel delegates to RouteAccumulator + RunStats; drop speed"
```

---

## Task 4: Pace display + accuracy forwarding in RecordScreen

**Files:**
- Modify: `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/RecordScreen.kt`

- [x] **Step 1: Forward GPS accuracy and drop speed**

In `RecordScreen.kt`, replace the location-forwarding `LaunchedEffect` (currently
the block reading `loc.hasSpeed()` and calling `onLocation(... speed ...)`):

```kotlin
    // Forward each valid fix to the ViewModel while recording.
    LaunchedEffect(userLocation.value) {
        val loc = userLocation.value
        if (recordViewModel.isRecording && (loc.latitude != 0.0 || loc.longitude != 0.0)) {
            val accuracy: Float? = if (loc.hasAccuracy()) loc.accuracy else null
            recordViewModel.onLocation(loc.latitude, loc.longitude, accuracy, loc.time)
        }
    }
```

- [x] **Step 2: Thicker route line**

In the `MapLibre { ... }` content, change the `Polyline` line width from `5f` to
`8f` so the trail reads clearly:

```kotlin
                Polyline(points = trail, color = trailColorHex, lineWidth = 8f)
```

- [x] **Step 3: Show average pace instead of speed**

In the live stats `Row` (the one with three `RecordStatItem`s), replace the speed
item with pace:

```kotlin
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    RecordStatItem(formatElapsed(recordViewModel.elapsedSeconds), "Time")
                    RecordStatItem("%.2f".format(recordViewModel.distanceKm), "Distance (km)")
                    RecordStatItem(
                        formatPace(recordViewModel.elapsedSeconds, recordViewModel.distanceKm),
                        "Pace (/km)"
                    )
                }
```

Add the import near the other `view_model` imports at the top of the file:

```kotlin
import com.example.a211198_hasif_drnelson_Project2.view_model.formatPace
```

- [x] **Step 4: Build to verify it compiles**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [x] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/RecordScreen.kt
git commit -m "P4.5: Record screen forwards GPS accuracy, shows pace, thicker route"
```

---

## Task 5: Minimizable info bar

**Files:**
- Modify: `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/RecordScreen.kt`

The stats `Card` overlaps the right-side map buttons. Add a collapsed state: a
compact time pill, so the buttons are reachable. The header icon toggles it.

- [x] **Step 1: Add the expanded/collapsed state**

Near the other `remember`/`rememberSaveable` declarations at the top of
`RecordScreen` (e.g. just after `var tilted by remember { mutableStateOf(false) }`),
add:

```kotlin
    var statsExpanded by rememberSaveable { mutableStateOf(true) }
```

- [x] **Step 2: Make the stats card collapsible**

Replace the entire live stats `Card(...) { ... }` block with the conditional
below. Expanded shows the full card with a toggle chevron in the header;
collapsed shows a small centered pill.

```kotlin
        // --- Live stats card (collapsible so it never hides the map buttons) ---
        if (statsExpanded) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 170.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(24.dp))
                        Text("Walk", color = colors.onSurface, fontWeight = FontWeight.Bold)
                        IconButton(
                            onClick = { statsExpanded = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.CloseFullscreen,
                                contentDescription = "Minimize stats",
                                tint = colors.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        RecordStatItem(formatElapsed(recordViewModel.elapsedSeconds), "Time")
                        RecordStatItem("%.2f".format(recordViewModel.distanceKm), "Distance (km)")
                        RecordStatItem(
                            formatPace(recordViewModel.elapsedSeconds, recordViewModel.distanceKm),
                            "Pace (/km)"
                        )
                    }
                }
            }
        } else {
            Surface(
                onClick = { statsExpanded = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 170.dp),
                shape = RoundedCornerShape(20.dp),
                color = colors.surface,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        formatElapsed(recordViewModel.elapsedSeconds),
                        color = colors.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        Icons.Default.OpenInFull,
                        contentDescription = "Expand stats",
                        tint = colors.onSurface,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
```

Note: `Icons.Default.OpenInFull` and `Icons.Default.CloseFullscreen` both come
from the existing `androidx.compose.material.icons.filled.*` wildcard import, so
no new import is needed.

- [x] **Step 3: Build to verify it compiles**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [x] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/RecordScreen.kt
git commit -m "P4.5: collapsible Record stats bar — frees the map buttons"
```

---

## Task 6: Route snapshot capture utility

**Files:**
- Create: `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/RouteSnapshot.kt`

A pure-Android utility (no Compose) that renders the recorded route over the
basemap into a `Bitmap` via MapLibre's off-screen `MapSnapshotter`, plus a helper
that writes a bitmap to internal storage and returns its absolute path. No unit
test — this is device/SDK integration, verified manually in Task 7.

- [x] **Step 1: Create the utility**

Create `RouteSnapshot.kt`:

```kotlin
package com.example.a211198_hasif_drnelson_Project2.view.screen

import android.content.Context
import android.graphics.Bitmap
import com.example.a211198_hasif_drnelson_Project2.view_model.TrackPoint
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.Style
import org.maplibre.android.snapshotter.MapSnapshotter
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import java.io.File
import java.io.FileOutputStream

// Render the recorded route over the basemap into a Bitmap, off-screen.
// onResult is called on the main thread with the bitmap, or null on failure /
// too few points. colorHex is a "#RRGGBB" string (the trail colour).
fun captureRouteSnapshot(
    context: Context,
    points: List<TrackPoint>,
    styleUrl: String,
    colorHex: String,
    widthPx: Int = 1000,
    heightPx: Int = 1000,
    onResult: (Bitmap?) -> Unit,
) {
    if (points.size < 2) {
        onResult(null)
        return
    }

    val latLngs = points.map { LatLng(it.lat, it.lng) }
    val bounds = LatLngBounds.Builder().includes(latLngs).build()

    val line = LineString.fromLngLats(points.map { Point.fromLngLat(it.lng, it.lat) })
    val routeSource = GeoJsonSource("route-src", line)
    val routeLayer = LineLayer("route-layer", "route-src").withProperties(
        PropertyFactory.lineColor(colorHex),
        PropertyFactory.lineWidth(5f),
        PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
        PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
    )

    val options = MapSnapshotter.Options(widthPx, heightPx)
        .withRegion(bounds)
        .withStyleBuilder(
            Style.Builder()
                .fromUri(styleUrl)
                .withSource(routeSource)
                .withLayer(routeLayer)
        )

    val snapshotter = MapSnapshotter(context, options)
    snapshotter.start(
        { snapshot -> onResult(snapshot.bitmap) },
        { _ -> onResult(null) },
    )
}

// Write a bitmap to internal storage and return its absolute path. Coil renders
// a plain file path, so this path can be stored directly in MediaEntity.imageUri.
fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap): String {
    val dir = File(context.filesDir, "runs").apply { mkdirs() }
    val file = File(dir, "run_${System.currentTimeMillis()}.png")
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    return file.absolutePath
}
```

- [x] **Step 2: Build to verify it compiles**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL. (If any MapLibre import path differs in this SDK
version, fix the import — the classes live under `org.maplibre.android.*` and
`org.maplibre.geojson.*`.)

- [x] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/RouteSnapshot.kt
git commit -m "P4.5: route snapshot util — MapSnapshotter bitmap + save to disk"
```

---

## Task 7: End button + summary bottom sheet

**Files:**
- Create: `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/RunSummarySheet.kt`
- Modify: `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/RecordScreen.kt`

- [x] **Step 1: Create the summary sheet composable**

Create `RunSummarySheet.kt`:

```kotlin
package com.example.a211198_hasif_drnelson_Project2.view.screen

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Bottom-sheet summary shown when the user taps End. Shows the route picture (or
// a spinner while it renders), the run stats, a caption field, an "include
// photo" toggle, and Post / Discard actions.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunSummarySheet(
    snapshot: Bitmap?,
    snapshotLoading: Boolean,
    timeText: String,
    distanceText: String,
    paceText: String,
    onPost: (caption: String, includePhoto: Boolean) -> Unit,
    onDiscard: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var caption by remember { mutableStateOf("New run") }
    var includePhoto by remember { mutableStateOf(true) }

    ModalBottomSheet(
        onDismissRequest = onDiscard,
        sheetState = sheetState,
        containerColor = colors.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Run complete", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = colors.onSurface)
            Spacer(Modifier.height(16.dp))

            // Route picture preview.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    snapshotLoading -> CircularProgressIndicator()
                    snapshot != null -> Image(
                        bitmap = snapshot.asImageBitmap(),
                        contentDescription = "Route map",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    else -> Text("No route image", color = colors.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                SummaryStat(timeText, "Time")
                SummaryStat(distanceText, "Distance (km)")
                SummaryStat(paceText, "Pace (/km)")
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it },
                label = { Text("Caption") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // "Include route photo" toggle — off posts stats only.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Include route photo", color = colors.onSurface)
                Switch(
                    checked = includePhoto,
                    onCheckedChange = { includePhoto = it },
                    enabled = snapshot != null
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = onDiscard, modifier = Modifier.weight(1f)) {
                    Text("Discard")
                }
                Button(
                    onClick = { onPost(caption, includePhoto && snapshot != null) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Post")
                }
            }
        }
    }
}

@Composable
private fun SummaryStat(value: String, label: String) {
    val colors = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = colors.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(label, color = colors.onSurfaceVariant, fontSize = 12.sp)
    }
}
```

- [x] **Step 2: Wire the End button + sheet into RecordScreen**

In `RecordScreen.kt`, add these imports near the other imports:

```kotlin
import android.graphics.Bitmap
import androidx.compose.ui.platform.LocalContext
import com.example.a211198_hasif_drnelson_Project2.view_model.formatPace
```

(If `formatPace` was already added in Task 4, do not duplicate it.)

Add state near the other `RecordScreen` state declarations (after `statsExpanded`):

```kotlin
    val context = LocalContext.current
    var showSummary by remember { mutableStateOf(false) }
    var snapshot by remember { mutableStateOf<Bitmap?>(null) }
    var snapshotLoading by remember { mutableStateOf(false) }
```

- [x] **Step 3: Change the Stop control to End**

Replace the third bottom-bar `Column` (the one whose `IconButton` calls
`recordViewModel.reset()` and is labelled "Stop") with:

```kotlin
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = colors.surfaceVariant,
                            modifier = Modifier.size(56.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    recordViewModel.pause()
                                    showSummary = true
                                    snapshotLoading = true
                                    snapshot = null
                                    captureRouteSnapshot(
                                        context = context,
                                        points = recordViewModel.path,
                                        styleUrl = styleUrls[styleIndex],
                                        colorHex = trailColorHex,
                                    ) { bmp ->
                                        snapshot = bmp
                                        snapshotLoading = false
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Flag, contentDescription = "End", tint = colors.onSurface)
                            }
                        }
                        Text("End", color = colors.onSurface, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                    }
```

`Icons.Default.Flag` resolves via the existing `material.icons.filled.*` import.

- [x] **Step 4: Show the sheet**

Just before the final closing brace of the outer `Box` in `RecordScreen` (after
the bottom control `Surface { ... }` block), add:

```kotlin
        if (showSummary) {
            RunSummarySheet(
                snapshot = snapshot,
                snapshotLoading = snapshotLoading,
                timeText = formatElapsed(recordViewModel.elapsedSeconds),
                distanceText = "%.2f".format(recordViewModel.distanceKm),
                paceText = formatPace(recordViewModel.elapsedSeconds, recordViewModel.distanceKm),
                onPost = { caption, includePhoto ->
                    val uri = if (includePhoto && snapshot != null) {
                        saveBitmapToInternalStorage(context, snapshot!!)
                    } else null
                    recordViewModel.saveActivity(type = "Walk", caption = caption, imageUri = uri)
                    showSummary = false
                    snapshot = null
                    recordViewModel.reset()
                },
                onDiscard = {
                    showSummary = false
                    snapshot = null
                    recordViewModel.reset()
                }
            )
        }
```

- [x] **Step 5: Build to verify it compiles**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [x] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/RunSummarySheet.kt \
        app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/RecordScreen.kt
git commit -m "P4.5: End button opens run summary sheet — post to gallery or discard"
```

---

## Task 8: Full verification

- [x] **Step 1: Run the whole unit suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS — including `RouteAccumulatorTest` (6) and `RunStatsTest` (6).

- [x] **Step 2: Build the debug APK**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [x] **Step 3: Manual on-device check**

Walk a short loop and confirm:
- The route line follows the path cleanly (no wild zig-zags).
- Distance looks sane; Pace shows `m:ss /km` and updates.
- Tapping the minimize chevron collapses the stats to a time pill, and all three
  right-side map buttons (Layers / 3D / Recenter) are now tappable; tapping the
  pill expands it again.
- Tapping **End** pauses and opens the summary sheet with a route picture,
  Time / Distance / Pace, a caption field, and an "Include route photo" toggle.
- **Post** makes the run appear in the Gallery reels feed with the route picture
  as its background (and the typed caption).
- Posting with the photo toggle **off** still creates the reel (drawable
  fallback), with no route picture.
- **Discard** returns to a clean Record screen with nothing saved.

- [x] **Step 4: Update the roadmap**

In `README.md`, under the Roadmap table, add a row after the map row:

```markdown
| Record screen polish (clean route, pace, End → post reel) | ✅ |
```

- [x] **Step 5: Commit**

```bash
git add README.md
git commit -m "P4.5: mark Record screen polish done in roadmap"
```

---

## Self-Review Notes

- **Spec coverage:** route smoothing (Task 1 gates + Task 4 thicker line), valid
  stats (Task 1), pace not speed (Task 2 + Task 4), minimizable info bar (Task 5),
  End → summary → post/discard with caption + include-photo toggle (Tasks 6–7),
  reels wiring via `imageUri` (Task 2 builder + Task 7 save). All covered.
- **Type consistency:** `addFix(lat, lng, accuracyM, timeMs)`, `formatPace(elapsedSeconds, distanceKm)`,
  `buildRunMedia(...)`, `buildRunRecord(...)`, `captureRouteSnapshot(...) { bmp -> }`,
  `saveBitmapToInternalStorage(context, bitmap)`, `saveActivity(type, caption, imageUri)`,
  and `RunSummarySheet(snapshot, snapshotLoading, timeText, distanceText, paceText, onPost, onDiscard)`
  are used identically wherever referenced.
- **Risk:** `MapSnapshotter` API/threading (flagged in the spec). If snapshotting
  proves unreliable on-device, the isolated fallback is to render `path` onto a
  `Canvas`-backed `Bitmap` inside `captureRouteSnapshot` without the basemap — no
  other task changes.
