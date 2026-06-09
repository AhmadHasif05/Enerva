# Phase 4 — Real Google Map in Record Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the hand-drawn `TrailCanvas` map stand-in in the Record screen with a live `GoogleMap` that draws the GPS trail as a `Polyline`, shows the user's location, follows the trail, and has three working map-control buttons.

**Architecture:** View-layer + build-config change only. `RecordViewModel` is untouched — the map and its controls (map type, tilt) are view-local `remember` state inside `RecordScreen`. The Maps API key follows the existing `local.properties` → `Properties()` → injected-value pattern already used for `GOOGLE_WEB_CLIENT_ID` (no `secrets-gradle-plugin`).

**Tech Stack:** Jetpack Compose, `maps-compose` (~6.4.x), `play-services-maps` (~19.x), Accompanist Permissions (existing), FusedLocationProviderClient (existing).

---

## Methodology note (read before starting)

This phase produces **no JVM-unit-testable logic**: the changes are Compose UI, a manifest/Gradle wiring, and Markdown docs. The Maps `LatLng`/`CameraPosition` types are Android-only and not available to the existing `testDebugUnitTest` JVM suite without an instrumented/Robolectric harness, which is out of scope. So instead of per-step failing tests, **each code task is verified by a Kotlin compile**, and the phase exits on a build gate plus an on-device checkpoint (Task 6). The existing unit + DAO suites must stay green (they don't touch any changed code).

### Build commands (this machine)

All builds run through the **Bash tool (git-bash)** with `JAVA_HOME` set to Android Studio's bundled JBR. `java` is not on PATH and PowerShell is unavailable for builds.

```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
cd "C:/Users/hasif/AndroidStudioProjects/a211198_Hasif_DrNelson_Project2"
./gradlew compileDebugKotlin      # quick verify
./gradlew assembleDebug           # full build gate
```

---

## File Structure

| File | Action | Responsibility |
|------|--------|----------------|
| `gradle/libs.versions.toml` | Modify | Declare `maps-compose` + `play-services-maps` versions & library aliases. |
| `app/build.gradle.kts` | Modify | Read `MAPS_API_KEY` from `local.properties`; inject as manifest placeholder; add the two deps. |
| `app/src/main/AndroidManifest.xml` | Modify | `com.google.android.geo.API_KEY` `<meta-data>` using the `${MAPS_API_KEY}` placeholder. |
| `local.properties` | Modify (local only, git-ignored) | Hold the developer's `MAPS_API_KEY`. |
| `app/src/main/java/.../view/screen/RecordScreen.kt` | Modify | Swap `TrailCanvas`→`GoogleMap`; polyline + start marker + location layer + camera follow; wire the 3 buttons; remove the search/filter bar; delete `TrailCanvas`. |
| `docs/setupmaps.md` | Create | Google Cloud Console guide to obtain + restrict the key. |
| `plan.md` | Modify | Mark Phase 4 done; refresh the "placeholder map" notes. |
| `README.md` | Modify | Update any reference to the placeholder map (if present). |

---

## Task 1: Maps dependencies + API-key plumbing

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `local.properties` (local only)

- [ ] **Step 1: Add versions + library aliases to the version catalog**

In `gradle/libs.versions.toml`, add to the `[versions]` block (after `googleid = "1.1.1"`):

```toml
mapsCompose = "6.4.1"
playServicesMaps = "19.2.0"
```

Add to the `[libraries]` block (after the `googleid` line):

```toml
maps-compose = { group = "com.google.maps.android", name = "maps-compose", version.ref = "mapsCompose" }
play-services-maps = { group = "com.google.android.gms", name = "play-services-maps", version.ref = "playServicesMaps" }
```

> If Gradle reports a newer stable exists, bump `mapsCompose`/`playServicesMaps` to the latest stable — these are not managed by any BoM.

- [ ] **Step 2: Read `MAPS_API_KEY` and inject it as a manifest placeholder**

In `app/build.gradle.kts`, replace the existing single-property read:

```kotlin
// Google Sign-In needs the Firebase "Web client ID" as the server client id.
// Kept out of version control in local.properties; falls back to "" so the
// build still works before it's filled in (runtime shows a "not configured" toast).
val googleWebClientId: String = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}.getProperty("GOOGLE_WEB_CLIENT_ID", "")
```

with a single shared load that also reads the Maps key:

```kotlin
// Secrets kept out of version control in local.properties; each falls back to "" so the
// build still works before they're filled in (Google Sign-In shows a "not configured"
// toast; the map renders blank tiles until MAPS_API_KEY is set).
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val googleWebClientId: String = localProps.getProperty("GOOGLE_WEB_CLIENT_ID", "")
val mapsApiKey: String = localProps.getProperty("MAPS_API_KEY", "")
```

- [ ] **Step 3: Wire the placeholder and the dependencies**

In `app/build.gradle.kts`, inside `defaultConfig { ... }`, after the existing `buildConfigField(...)` line, add:

```kotlin
        // Surfaced to AndroidManifest as ${MAPS_API_KEY} for the Maps SDK meta-data.
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
```

In the `dependencies { ... }` block, after `implementation(libs.play.services.location)`, add:

```kotlin
    implementation(libs.maps.compose)
    implementation(libs.play.services.maps)
```

- [ ] **Step 4: Add the Maps API-key meta-data to the manifest**

In `app/src/main/AndroidManifest.xml`, inside `<application>` (e.g. immediately after the opening `<application ...>` tag's attributes, before `<activity>`), add:

```xml
        <meta-data
            android:name="com.google.android.geo.API_KEY"
            android:value="${MAPS_API_KEY}" />
```

- [ ] **Step 5: Add the key placeholder to `local.properties`** (local only — git-ignored, never committed)

Append to `local.properties`:

```properties
# Google Maps SDK for Android key — see docs/setupmaps.md. Leave blank to build keyless (map renders grey).
MAPS_API_KEY=
```

> The developer fills in the value after following `docs/setupmaps.md` (Task 5). An empty value is fine for compiling.

- [ ] **Step 6: Verify the project still compiles (keyless is OK)**

Run:
```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
cd "C:/Users/hasif/AndroidStudioProjects/a211198_Hasif_DrNelson_Project2"
./gradlew compileDebugKotlin
```
Expected: `BUILD SUCCESSFUL`. (Dependencies resolve; no code uses them yet.)

- [ ] **Step 7: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts app/src/main/AndroidManifest.xml
git commit -m "P4: add Maps Compose deps + MAPS_API_KEY plumbing"
```
> Do **not** `git add local.properties` — it is git-ignored and holds the secret.

---

## Task 2: Replace the canvas map with a real `GoogleMap`

**Files:**
- Modify: `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/RecordScreen.kt`

This task rewrites the map portion of `RecordScreen`, wires the three buttons, removes the search/filter bar, and deletes `TrailCanvas`. Because the change is large and interleaved, replace the whole file with the content below.

- [ ] **Step 1: Replace `RecordScreen.kt` with the map-based implementation**

Write the entire file `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/RecordScreen.kt` as:

```kotlin
package com.example.a211198_hasif_drnelson_Project2.view.screen

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.a211198_hasif_drnelson_Project2.view_model.RecordViewModel
import com.example.a211198_hasif_drnelson_Project2.view_model.RecordViewModelFactory
import com.example.a211198_hasif_drnelson_Project2.view_model.TrackPoint
import com.example.a211198_hasif_drnelson_Project2.view_model.formatElapsed
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(
    navController: NavController? = null,
    recordViewModel: RecordViewModel = viewModel(factory = RecordViewModelFactory)
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val permission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    LaunchedEffect(Unit) {
        if (!permission.status.isGranted) permission.launchPermissionRequest()
    }

    // Wire FusedLocationProviderClient to the ViewModel while recording.
    LocationUpdatesEffect(
        enabled = recordViewModel.isRecording && permission.status.isGranted,
        onLocation = { lat, lng, speed, time ->
            recordViewModel.onLocation(lat, lng, speed, time)
        }
    )

    // --- Map view-state (no ViewModel involvement) ---
    var mapType by remember { mutableStateOf(MapType.NORMAL) }
    var tilted by remember { mutableStateOf(false) }
    val cameraPositionState = rememberCameraPositionState()

    // TrackPoints -> LatLng for the polyline + start marker.
    val trail = recordViewModel.path.map { LatLng(it.lat, it.lng) }

    // Keep a start marker pinned to the first recorded point.
    val startMarker = rememberMarkerState()
    LaunchedEffect(trail.firstOrNull()) {
        trail.firstOrNull()?.let { startMarker.position = it }
    }

    // Follow the newest point while recording.
    LaunchedEffect(recordViewModel.path.lastOrNull()) {
        val last = recordViewModel.path.lastOrNull()
        if (last != null && recordViewModel.isRecording) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(last.lat, last.lng), 17f)
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        // --- Real Google map (replaces the Canvas breadcrumb backdrop) ---
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = permission.status.isGranted,
                mapType = mapType,
                isBuildingEnabled = tilted
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                mapToolbarEnabled = false,
                compassEnabled = false
            )
        ) {
            if (trail.size >= 2) {
                Polyline(points = trail, color = colors.primary, width = 14f)
            }
            if (trail.isNotEmpty()) {
                Marker(state = startMarker, title = "Start")
            }
        }

        if (recordViewModel.path.isEmpty()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = colors.outline
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    if (!permission.status.isGranted) "Location permission required"
                    else if (!recordViewModel.isRecording) "Press play to start tracking"
                    else "Waiting for GPS fix...",
                    color = colors.onSurfaceVariant
                )
            }
        }

        // --- Top-left back / Activities button (search + filter bar removed in Phase 4) ---
        IconButton(
            onClick = { navController?.popBackStack() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 16.dp, start = 16.dp)
                .background(colors.surface, CircleShape)
                .size(48.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = "Activities", tint = colors.primary)
        }

        // --- Right-side map action buttons (now functional) ---
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Layers: cycle Normal -> Satellite -> Terrain -> Normal.
            RecordMapActionButton(Icons.Outlined.Layers) {
                mapType = when (mapType) {
                    MapType.NORMAL -> MapType.SATELLITE
                    MapType.SATELLITE -> MapType.TERRAIN
                    else -> MapType.NORMAL
                }
            }
            // 3D: toggle camera tilt + buildings.
            RecordMapActionButton(Icons.Default.ViewInAr) {
                tilted = !tilted
                scope.launch {
                    val pos = cameraPositionState.position
                    cameraPositionState.animate(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.builder(pos).tilt(if (tilted) 45f else 0f).build()
                        )
                    )
                }
            }
            // Recenter on the user's current position.
            RecordMapActionButton(Icons.Default.MyLocation) {
                recenterOnUser(
                    context = context,
                    granted = permission.status.isGranted,
                    lastPoint = recordViewModel.path.lastOrNull(),
                    scope = scope,
                    cameraPositionState = cameraPositionState
                )
            }
        }

        IconButton(
            onClick = { recordViewModel.reset() },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 280.dp)
                .background(colors.surface, CircleShape)
                .size(36.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = colors.onSurface, modifier = Modifier.size(20.dp))
        }

        // --- Live stats card ---
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
                    Icon(Icons.Default.OpenInFull, contentDescription = null, tint = colors.onSurface, modifier = Modifier.size(16.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    RecordStatItem(formatElapsed(recordViewModel.elapsedSeconds), "Time")
                    RecordStatItem("%.2f".format(recordViewModel.distanceKm), "Distance (km)")
                    RecordStatItem("%.1f".format(recordViewModel.currentSpeedKmh), "Speed (km/h)")
                }
            }
        }

        // --- Bottom control bar ---
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(160.dp),
            color = colors.background,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.outline)
                )

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = colors.primary.copy(alpha = 0.2f),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = null, tint = colors.primary)
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .align(Alignment.TopEnd)
                                        .padding(2.dp)
                                        .clip(CircleShape)
                                        .background(colors.primary)
                                )
                            }
                        }
                        Text("Walk", color = colors.onSurface, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                    }

                    // Start / Pause toggle
                    Surface(
                        shape = CircleShape,
                        color = colors.primary,
                        modifier = Modifier.size(80.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (!permission.status.isGranted) {
                                    permission.launchPermissionRequest()
                                } else if (recordViewModel.isRecording) {
                                    recordViewModel.pause()
                                } else {
                                    recordViewModel.start()
                                }
                            }
                        ) {
                            Icon(
                                if (recordViewModel.isRecording) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (recordViewModel.isRecording) "Pause" else "Start",
                                tint = colors.onPrimary,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = colors.surfaceVariant,
                            modifier = Modifier.size(56.dp)
                        ) {
                            IconButton(onClick = { recordViewModel.reset() }) {
                                Icon(Icons.Default.Stop, contentDescription = "Stop", tint = colors.onSurface)
                            }
                        }
                        Text("Stop", color = colors.onSurface, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
    }
}

// Recenter the camera on the user: prefer the latest recorded point, else the last
// known device location (guarded by the location permission).
@SuppressLint("MissingPermission")
private fun recenterOnUser(
    context: Context,
    granted: Boolean,
    lastPoint: TrackPoint?,
    scope: CoroutineScope,
    cameraPositionState: CameraPositionState
) {
    if (lastPoint != null) {
        scope.launch {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(lastPoint.lat, lastPoint.lng), 17f)
            )
        }
        return
    }
    if (!granted) return
    LocationServices.getFusedLocationProviderClient(context).lastLocation
        .addOnSuccessListener { loc ->
            if (loc != null) {
                scope.launch {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 17f)
                    )
                }
            }
        }
}

@SuppressLint("MissingPermission")
@Composable
private fun LocationUpdatesEffect(
    enabled: Boolean,
    onLocation: (lat: Double, lng: Double, speed: Float?, time: Long) -> Unit
) {
    val context = LocalContext.current

    DisposableEffect(enabled) {
        if (!enabled) return@DisposableEffect onDispose { }

        val client = LocationServices.getFusedLocationProviderClient(context)
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(500L)
            .setMinUpdateDistanceMeters(1f)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    val speed: Float? = if (location.hasSpeed()) location.speed else null
                    onLocation(location.latitude, location.longitude, speed, location.time)
                }
            }
        }

        client.requestLocationUpdates(request, callback, Looper.getMainLooper())

        onDispose {
            client.removeLocationUpdates(callback)
        }
    }
}

@Composable
fun RecordStatItem(value: String, label: String) {
    val colors = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = colors.onSurface, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(label, color = colors.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun RecordMapActionButton(icon: ImageVector, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = CircleShape,
        color = colors.surface,
        modifier = Modifier.size(44.dp),
        shadowElevation = 4.dp
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = null, tint = colors.onSurface, modifier = Modifier.size(20.dp))
        }
    }
}

@Preview
@Composable
fun RecordScreenPreview() {
    RecordScreen(rememberNavController())
}
```

> Notes on what changed vs. the old file: `TrailCanvas` and its `Canvas`/`Path`/`Offset`/`Stroke`/`Color`/geometry imports are gone; the `LazyRow` filter chips + the "Search locations" `Surface` (and their `BookmarkBorder`/`KeyboardArrowDown`/`VerticalDivider`/`LazyRow`/`items` imports) are gone; `RecordMapActionButton` gained an `onClick` param; a `recenterOnUser` helper was added. The stats card and bottom control bar are unchanged. The `@Preview` will compile but the Maps surface may not render inside the IDE preview pane — that is expected.

- [ ] **Step 2: Verify it compiles**

Run:
```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
cd "C:/Users/hasif/AndroidStudioProjects/a211198_Hasif_DrNelson_Project2"
./gradlew compileDebugKotlin
```
Expected: `BUILD SUCCESSFUL`. If an unresolved-reference error mentions a `maps.android.compose` symbol, confirm Task 1 Step 1 versions resolved (try the latest stable `maps-compose`).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/RecordScreen.kt
git commit -m "P4: replace canvas backdrop with live GoogleMap + wire map buttons"
```

---

## Task 3: Maps API-key setup guide

**Files:**
- Create: `docs/setupmaps.md`

- [ ] **Step 1: Write the setup guide**

Create `docs/setupmaps.md` with:

```markdown
# Google Maps Setup Guide — Enerva

The Record screen uses the **Maps SDK for Android**, which needs an API key. You only
need to do this **once**. The key is read from `local.properties` and is **never committed**.

---

## Step 1 — Create / pick a Google Cloud project

1. Go to **https://console.cloud.google.com** and sign in.
2. In the top project picker, click **New Project** (or reuse the one backing your Firebase project — Firebase projects are also Google Cloud projects).
3. Name it (e.g. `Enerva`) → **Create**, then make sure it's the **selected** project.

---

## Step 2 — Enable the Maps SDK for Android

1. Left menu → **APIs & Services → Library**.
2. Search **"Maps SDK for Android"** → open it → **Enable**.
3. (Billing: Google requires a billing account enabled on the project. The Maps SDK for Android has a generous free tier; you will not be charged for normal development use, but a billing account must exist.)

---

## Step 3 — Get your debug signing SHA-1

You restrict the key to your app's package **and** signing certificate. Get the debug SHA-1:

```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
cd "C:/Users/hasif/AndroidStudioProjects/a211198_Hasif_DrNelson_Project2"
./gradlew signingReport
```

Copy the **SHA1** under the `debug` variant (`Variant: debug`, `Config: debug`). It looks like
`AA:BB:CC:...:FF`.

---

## Step 4 — Create + restrict the API key

1. Left menu → **APIs & Services → Credentials**.
2. **Create credentials → API key**. Copy the generated key.
3. Click the key to edit it:
   - **Application restrictions** → **Android apps** → **Add an item**:
     - **Package name:** `com.example.a211198_hasif_drnelson_Project2`
     - **SHA-1 certificate fingerprint:** the debug SHA-1 from Step 3.
   - **API restrictions** → **Restrict key** → check **Maps SDK for Android** only.
4. **Save**. (Restriction changes can take a few minutes to propagate.)

---

## Step 5 — Put the key in local.properties

Open `local.properties` (project root, git-ignored) and set:

```properties
MAPS_API_KEY=AIza...your-key...
```

Then rebuild. The map should render on the Record screen.

---

## Troubleshooting

- **Grey/blank map tiles + an `Authorization failure` line in Logcat** → the key, package
  name, or SHA-1 don't match. Re-check Step 4 against the **debug** SHA-1 from Step 3.
- **Map still blank after fixing** → restriction changes take a few minutes; also do a clean
  rebuild so the new `${MAPS_API_KEY}` manifest value is baked in.
- **`local.properties` got overwritten by Android Studio** → re-add the `MAPS_API_KEY` line;
  Studio only manages `sdk.dir`.

---

## What you should have at the end

- ✅ Maps SDK for Android enabled on a Google Cloud project (with billing account attached)
- ✅ An API key restricted to package `com.example.a211198_hasif_drnelson_Project2` + your debug SHA-1
- ✅ `MAPS_API_KEY=...` set in `local.properties`
- ✅ A real map rendering on the Record screen
```

- [ ] **Step 2: Commit**

```bash
git add docs/setupmaps.md
git commit -m "P4: add Google Maps API key setup guide"
```

---

## Task 4: Update project docs

**Files:**
- Modify: `plan.md`
- Modify: `README.md`

- [ ] **Step 1: Mark Phase 4 done in the roadmap table**

In `plan.md` §8, in the roadmap table, change the Phase 4 row from:

```markdown
| 4 | Google Maps API (real map) | 🔜 Next |
```

to:

```markdown
| 4 | Google Maps API (real map) | ✅ Done (needs `MAPS_API_KEY` in local.properties — see docs/setupmaps.md) |
```

And change the Phase 5 row's status from `🔜 Upcoming` to `🔜 Next`.

- [ ] **Step 2: Refresh the "placeholder map" notes in `plan.md`**

In `plan.md` §10.1, update the **Record screen honesty** row from:

```markdown
| **Record screen honesty** | The map backdrop is a placeholder grid and the action buttons/filters are inert. Either ship Phase 4's real map or visibly mark these as "coming soon" so they don't feel broken. |
```

to:

```markdown
| **Record screen honesty** | ✅ Phase 4 shipped a real Google map with working Layers/3D/recenter buttons; the inert search/filter bar was removed. |
```

In §2 (Tech Stack), update the CameraX row note and the Retrofit note if they reference Phase 4 for Maps — specifically change the trailing note:

```markdown
> **Note:** Retrofit/Moshi/OkHttp are wired but not yet exercised by a live API — reserved for Maps/places work in Phase 4.
```

to:

```markdown
> **Note:** Retrofit/Moshi/OkHttp are wired but not yet exercised by a live API — reserved for future Maps *places* work (route discovery, §10.2).
```

- [ ] **Step 3: Update the Phase 4 detail section in `plan.md` §8**

Under `### Phase 4 — Google Maps (real map in Record)`, replace the numbered checklist with a done-state summary (keep it brief):

```markdown
### Phase 4 — Google Maps (real map in Record) ✅
- `maps-compose` + `play-services-maps` added; key read from `local.properties` (`MAPS_API_KEY`) and injected as a manifest placeholder (same pattern as `GOOGLE_WEB_CLIENT_ID` — no secrets plugin).
- `TrailCanvas` replaced by a `GoogleMap`; live trail drawn as a `Polyline`; current position via the SDK location layer; camera follows while recording.
- Layers (Normal/Satellite/Terrain), 3D (tilt + buildings), and recenter buttons wired. The inert search/filter bar was removed.
- Setup: see `docs/setupmaps.md`. **Checkpoint:** live GPS path on a real map (on-device).
```

- [ ] **Step 4: Update `README.md` if it mentions the placeholder map**

Search `README.md` for any mention of the Record map being a placeholder / canvas / "coming soon", or a feature list that says the map isn't real. If found, update it to state the Record screen now shows a real Google map (note the `MAPS_API_KEY` requirement, pointing to `docs/setupmaps.md`). If `README.md` has no such reference, make no change.

- [ ] **Step 5: Commit**

```bash
git add plan.md README.md
git commit -m "P4: docs — mark Phase 4 done, refresh Record-screen notes"
```

---

## Task 5: Build gate + regression check

**Files:** none (verification only)

- [ ] **Step 1: Full debug build (keyless still builds)**

Run:
```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
cd "C:/Users/hasif/AndroidStudioProjects/a211198_Hasif_DrNelson_Project2"
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Run the existing JVM unit suite (must stay green)**

Run:
```bash
./gradlew testDebugUnitTest
```
Expected: `BUILD SUCCESSFUL` — the 13 existing unit tests pass; nothing they cover was touched.

> The DAO instrumented suite (`connectedDebugAndroidTest`) needs a device/emulator; run it in Task 6 if a device is attached, otherwise note it as deferred.

---

## Task 6: On-device checkpoint (requires `MAPS_API_KEY`)

**Files:** none (manual verification)

This is the Phase-4 exit checkpoint and **requires a real key** (Task 3) in `local.properties` plus a device/emulator with Google Play services.

- [ ] **Step 1: Confirm the key is set**

Ensure `local.properties` has a non-empty `MAPS_API_KEY` (per `docs/setupmaps.md`). Without it the map renders grey and the checkpoint cannot pass.

- [ ] **Step 2: Install and launch**

Run (with a device/emulator connected):
```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
cd "C:/Users/hasif/AndroidStudioProjects/a211198_Hasif_DrNelson_Project2"
./gradlew installDebug
```
Then open the app and navigate to the **Record** tab.

- [ ] **Step 3: Verify the checkpoint behaviours**

Grant the location permission, press **play**, and move (walk, or use the emulator's location/route simulation). Confirm:
- [ ] A real map renders (streets/tiles, not a grey blank).
- [ ] The live GPS trail draws as an orange **polyline** that grows as you move.
- [ ] The blue **location dot** appears.
- [ ] The **camera follows** the newest point while recording.
- [ ] **Layers** cycles Normal → Satellite → Terrain.
- [ ] **3D** tilts the camera (and shows buildings where available).
- [ ] **MyLocation** recenters the camera on the current position.

- [ ] **Step 4 (optional, if a device is attached): run the DAO instrumented suite**

```bash
./gradlew connectedDebugAndroidTest
```
Expected: `BUILD SUCCESSFUL` — the 19 DAO tests still pass (unaffected by Phase 4).

- [ ] **Step 5: Finishing the branch**

Once the checkpoint passes, use the `superpowers:finishing-a-development-branch` skill to merge `phase4-google-maps` into `main` (or open a PR), per the user's preference.

---

## Self-Review (completed during authoring)

- **Spec coverage:** §2 key plumbing → Task 1. §3 map/polyline/location/camera → Task 2. §4 three buttons → Task 2. §5 remove filter bar → Task 2. §6 setup guide → Task 3, docs → Task 4. §7 testing/build gate → Tasks 5–6. All spec sections map to a task.
- **Placeholder scan:** every code step contains complete content; no TBD/TODO. The only intentional blank is the empty `MAPS_API_KEY=` value (the secret the user fills in) and the conditional README edit (explicitly "make no change if absent").
- **Type consistency:** `RecordMapActionButton(icon, onClick)`, `recenterOnUser(context, granted, lastPoint, scope, cameraPositionState)`, `MapType.NORMAL/SATELLITE/TERRAIN`, `cameraPositionState.position`/`.animate(...)`, `startMarker.position` are used identically wherever referenced.
```

