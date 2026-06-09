# Phase 4 — Real Google Map in the Record Screen

> Replace the hand-drawn `TrailCanvas` map stand-in with a live `GoogleMap`, draw the
> GPS trail as a `Polyline`, show the current position via the SDK location layer, and
> wire the three previously-inert map action buttons. Remove the inert search/filter bar.

- **Phase:** 4 of the Enerva roadmap (`plan.md` §8).
- **Status going in:** Record screen renders a fake grid + breadcrumb (`TrailCanvas`); the
  right-side buttons (Layers / 3D / MyLocation) and the top filter chips are all `onClick = { }`.
- **Checkpoint to exit:** live GPS path drawn on a real map on-device.

---

## 1. Goals & Non-Goals

### Goals
1. A real `GoogleMap` backdrop on the Record screen, themed to the app.
2. The live GPS trail drawn as a `Polyline` from `RecordViewModel.path`.
3. The current location shown via the Maps SDK built-in location layer (blue dot).
4. The camera follows the trail while recording.
5. The three right-side action buttons do real work: Layers (map type), 3D (tilt), MyLocation (recenter).
6. Maps API key supplied out-of-band (never committed); a setup guide for obtaining it.

### Non-Goals (explicitly out of scope)
- Changes to GPS tracking logic, distance/speed math, or `saveActivity` (untouched).
- Route data model, route discovery, or plotting saved routes on the map.
- Phase 5 camera capture.
- Any new `RecordViewModel` state — the map controls are view-local concerns.

---

## 2. Dependencies & API-key plumbing

**Decision:** reuse the existing `local.properties` → `Properties()` → injected-value pattern already
used for `GOOGLE_WEB_CLIENT_ID` in `app/build.gradle.kts`. Do **not** add the `secrets-gradle-plugin`
— it would be a second, redundant mechanism for the same job.

Changes:
- `gradle/libs.versions.toml`: add `maps-compose` (~6.4.x) and `play-services-maps` (~19.x)
  versions + library entries. (Pin to the latest stable at implementation time.)
- `app/build.gradle.kts`:
  - Read `MAPS_API_KEY` from `local.properties` (same `Properties()` block already present),
    defaulting to `""`.
  - Expose it via `defaultConfig { manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey }`.
  - Add the two new `implementation(...)` deps.
- `app/src/main/AndroidManifest.xml`: inside `<application>`, add
  `<meta-data android:name="com.google.android.geo.API_KEY" android:value="${MAPS_API_KEY}" />`.
- `local.properties`: developer adds `MAPS_API_KEY=...` (git-ignored; never committed).

**Keyless fallback:** with an empty key the project still compiles and runs; the map tiles render
blank/grey. This keeps the build green for anyone who clones without a key.

---

## 3. The map (replaces `TrailCanvas`)

In `RecordScreen.kt`:
- Replace the `TrailCanvas(...)` call with a `GoogleMap` composable filling the existing root `Box`.
- **Trail:** map `recordViewModel.path` (`List<TrackPoint>`) → `List<LatLng>` and render a `Polyline`
  in `MaterialTheme.colorScheme.primary`. A `Marker` (or small overlay) at `path.first()` marks the start.
- **Current position:** set `MapProperties(isMyLocationEnabled = permission.status.isGranted)` so the
  SDK draws the blue location dot — no custom "current" marker needed.
- **Camera:** a `rememberCameraPositionState()`. A `LaunchedEffect` keyed on `path.lastOrNull()`
  animates/moves the camera to the newest point while `isRecording` is true.
- **Empty-state overlay preserved:** the existing centered overlay ("Location permission required" /
  "Press play to start tracking" / "Waiting for GPS fix...") stays, drawn on top of the map while
  `path` is empty.
- **Map UI defaults:** disable the SDK's own zoom/mylocation chrome (`MapUiSettings`) so the app's
  custom buttons are the only controls; keep the look consistent with the dark theme via map styling
  if straightforward (optional, not required for the checkpoint).

`TrailCanvas`, its grid-drawing code, and the `Canvas`/`Path`/`Offset`/`Stroke` imports it needed are
removed once unused.

---

## 4. The three right-side action buttons

These are pure view/map concerns — held in `remember` state inside `RecordScreen`, **no `RecordViewModel`
changes**. `RecordMapActionButton` gains an `onClick` parameter (currently hardcoded `{ }`).

| Button | Icon | Behaviour |
|--------|------|-----------|
| **Layers** | `Outlined.Layers` | Cycle `MapType`: Normal → Satellite → Terrain → (wrap). |
| **3D** | `Default.ViewInAr` | Toggle camera tilt 0° ↔ ~45°; enable `isBuildingEnabled`. |
| **MyLocation** | `Default.MyLocation` | Animate camera to the user's current location (latest path point, or last known location if not yet recording). |

---

## 5. Remove the search/filter top bar

Per the approved scope ("map buttons only"), the top `Surface` "Search locations" bar and the
`LazyRow` of filter chips (`Routes / Length / Difficulty / Elevation`) are **removed** from
`RecordScreen` — they were carried over from the deleted Maps screen and imply an unbuilt
route-discovery dataset. Removing them (rather than leaving inert chips) yields an honest, focused
tracking screen. The top-left back/Activities `IconButton` is retained.

---

## 6. Deliverables

1. The code changes in §2–§5.
2. `docs/setupmaps.md` — a step-by-step Google Cloud Console guide (mirroring `setupfirebase.md`):
   create/select a project, enable **Maps SDK for Android**, create an API key, restrict it to the
   app package `com.example.a211198_hasif_drnelson_Project2` + debug SHA-1, and place it in
   `local.properties` as `MAPS_API_KEY`.
3. Doc updates: mark Phase 4 done in `plan.md` §8 and refresh the Record-screen note in §10
   ("map backdrop is a placeholder") and the Tech-Stack note; update `README.md` if it references the
   placeholder map.

---

## 7. Testing & verification

- **No new unit-testable logic.** The map is view-layer; `TrackPoint → LatLng` is a trivial mapping.
  Existing unit tests (`testDebugUnitTest`) and DAO instrumented tests (`connectedDebugAndroidTest`)
  must remain green — the changes don't touch their subjects.
- **Build gate:** `./gradlew assembleDebug` succeeds both with and without a key present.
- **On-device checkpoint (requires the key):** build + run, grant location, press play, walk/simulate
  movement → confirm (a) the live GPS polyline draws on a real map, (b) the blue location dot appears,
  (c) the camera follows, (d) Layers cycles map types, (e) 3D tilts, (f) MyLocation recenters.

---

## 8. Risks & notes

- **Key required for visual verification.** Code can be completed before the key exists; the map just
  renders blank until `MAPS_API_KEY` is filled in. The on-device checkpoint is blocked on the key.
- **Network/key restrictions.** A key restricted to the wrong SHA-1/package shows a blank map with a
  `Authorization failure` log line — the setup guide must call out matching the **debug** SHA-1.
- **Compose Maps version drift.** Pin `maps-compose`/`play-services-maps` to current stable at
  implementation time; the BoM doesn't manage these.
