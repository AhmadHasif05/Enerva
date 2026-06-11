# Phase 4 — Real Map in Record Implementation Plan (MapLibre + MapTiler/OpenFreeMap)

> **Provider note:** originally drafted for Google Maps, switched to **MapLibre Native** +
> **MapTiler/OpenFreeMap** because Google Maps Platform requires a billing account with a credit
> card to mint a key. See the spec: [`../specs/2026-06-09-phase4-google-maps-design.md`](../specs/2026-06-09-phase4-google-maps-design.md).
> The filename keeps `google-maps` to preserve cross-links from `plan.md`.

**Goal:** Replace the hand-drawn `TrailCanvas` map stand-in in the Record screen with a live
MapLibre vector map that draws the GPS trail as a `Polyline`, shows the user's location, follows the
trail, and has three working map-control buttons. Free, no credit card.

**Architecture:** View-layer + build-config change only. `RecordViewModel` is untouched — the map and
its controls are view-local `remember` state inside `RecordScreen`. The MapTiler key follows the
existing `local.properties` → `Properties()` → `buildConfigField` pattern already used for
`GOOGLE_WEB_CLIENT_ID` (no `secrets-gradle-plugin`, no manifest meta-data).

**Tech Stack:** Jetpack Compose, `org.ramani-maps:ramani-maplibre` (Compose wrapper over MapLibre
Native), Accompanist Permissions (existing). The manual `FusedLocationProviderClient` wiring is
replaced by MapLibre's built-in location layer.

---

## Methodology note

This phase produces **no JVM-unit-testable logic**: the changes are Compose UI, a Gradle/build-config
wiring, and Markdown docs. Each code task is verified by a **Kotlin compile**; the phase exits on a
full build gate plus an on-device checkpoint. The existing unit + DAO suites must stay green.

### Build commands (this machine)

Builds run through the **Bash tool (git-bash)** with `JAVA_HOME` set to Android Studio's bundled JBR.

```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
cd "C:/Users/hasif/AndroidStudioProjects/a211198_Hasif_DrNelson_Project2"
./gradlew compileDebugKotlin      # quick verify
./gradlew assembleDebug           # full build gate
```

---

## Implementation gotchas discovered during execution

- **ramani-maplibre version:** `0.10.0` ships an **older API** (no `MapStyle`/`CameraPositionState`/
  `CenterState`). Use **`0.12.0`** (latest), whose Compose API matches: `MapLibre(style = MapStyle.Uri(...),
  cameraPositionState = ...)`, `Polyline(points, color, lineWidth)`, `Circle(centerState, ...)`,
  `CameraPosition(target, zoom, tilt, bearing)`.
- **minSdk:** ramani-maplibre `0.12.0` requires **minSdk 25** (app was 24). Bumped `minSdk` to 25
  (Android 7.0 → 7.1; negligible coverage impact).
- **No manifest change:** MapLibre builds its own tile URLs from a style URL string, so there is **no
  `com.google.android.geo.API_KEY` meta-data**. The key is surfaced via `buildConfigField` only.

---

## File Structure

| File | Action | Responsibility |
|------|--------|----------------|
| `gradle/libs.versions.toml` | Modify | Declare `ramani-maplibre` version (`0.12.0`) + library alias. |
| `app/build.gradle.kts` | Modify | Shared `local.properties` read; `buildConfigField("String", "MAPTILER_API_KEY", …)`; add the dep; bump `minSdk` 24 → 25. |
| `local.properties` | Modify (local only, git-ignored) | Hold the optional `MAPTILER_API_KEY`. |
| `.../view/screen/RecordScreen.kt` | Modify | Swap `TrailCanvas`→`MapLibre`; polyline + start `Circle` + built-in location layer feeding the ViewModel; wire the 3 buttons; remove the search/filter bar; delete `TrailCanvas` + `LocationUpdatesEffect`. |
| `docs/setupmaps.md` | Create | MapTiler (free, no card) + keyless-OpenFreeMap guide. |
| `plan.md`, `README.md` | Modify | Mark Phase 4 done; refresh map notes, tech stack, minSdk. |

---

## Task 1: Dependency + MAPTILER_API_KEY plumbing ✅

**Files:** `gradle/libs.versions.toml`, `app/build.gradle.kts`, `local.properties`

- [x] **Version catalog:** add to `[versions]`: `ramaniMaplibre = "0.12.0"`; add to `[libraries]`:
  `ramani-maplibre = { group = "org.ramani-maps", name = "ramani-maplibre", version.ref = "ramaniMaplibre" }`.
- [x] **Shared local.properties read:** replace the single `googleWebClientId` `Properties()` read with a
  shared `localProps` block reading both `GOOGLE_WEB_CLIENT_ID` and `MAPTILER_API_KEY` (default `""`).
- [x] **Expose the key:** in `defaultConfig`, after the existing `buildConfigField(...)`, add
  `buildConfigField("String", "MAPTILER_API_KEY", "\"$mapTilerApiKey\"")`.
- [x] **minSdk:** change `minSdk = 24` → `minSdk = 25`.
- [x] **Dependency:** add `implementation(libs.ramani.maplibre)`.
- [x] **local.properties:** append `MAPTILER_API_KEY=` (blank — optional; git-ignored, never committed).
- [x] **Verify:** `./gradlew compileDebugKotlin` → `BUILD SUCCESSFUL`.

---

## Task 2: Replace the canvas map with a real `MapLibre` map ✅

**Files:** `.../view/screen/RecordScreen.kt`

Rewrote the map portion of `RecordScreen` (see the file for the full, compiling implementation).
Key points:

- [x] **Map:** `MapLibre(style = MapStyle.Uri(styleUrls[styleIndex]), cameraPositionState, userLocation,
  cameraMode, renderMode = RenderMode.COMPASS, locationRequestProperties = LocationRequestProperties(),
  locationStyling = LocationStyling(enablePulse = true))`.
- [x] **Styles:** view-local `styleUrls` list — OpenFreeMap Liberty + Bright (keyless), plus MapTiler
  Streets/Satellite/Outdoor appended when `BuildConfig.MAPTILER_API_KEY` is non-blank.
- [x] **Trail:** `recordViewModel.path.map { LatLng(it.lat, it.lng) }` → `Polyline(points, color =
  <primary as #RRGGBB>, lineWidth = 5f)`. Start `Circle` pinned to `path.first()` via a `CenterState`.
- [x] **Location + follow:** MapLibre's location layer fills `userLocation`; `cameraMode` = `TRACKING`
  while recording, `NONE` otherwise. A `LaunchedEffect(userLocation.value)` forwards valid fixes to
  `recordViewModel.onLocation(lat, lng, speed, time)` — **replacing** the deleted
  `FusedLocationProviderClient`/`LocationUpdatesEffect`.
- [x] **Buttons** (`RecordMapActionButton` gained an `onClick` param): **Layers** cycles `styleIndex`;
  **3D** toggles `cameraPositionState.position.copy(tilt = 0.0 / 45.0)`; **Recenter** sets the camera
  target to `userLocation`.
- [x] **Removed:** the search `Surface` + filter `LazyRow`, `TrailCanvas`, `LocationUpdatesEffect`, and
  their now-unused imports. Empty-state overlay + stats card + bottom control bar unchanged.
- [x] **Verify:** `./gradlew compileDebugKotlin` → `BUILD SUCCESSFUL`.

---

## Task 3: Setup guide + doc updates ✅

- [x] `docs/setupmaps.md` — keyless OpenFreeMap (Option A) + free MapTiler key, no credit card (Option B).
- [x] `plan.md` — minSdk 24 → 25; Tech Stack Maps row; roadmap table + Phase 4 detail rewritten;
  §10.1 Record-screen honesty row marked done.
- [x] `README.md` — Maps tech-stack line; optional `MAPTILER_API_KEY` setup step; roadmap row.

---

## Task 4: Build gate + regression check ✅

- [x] `./gradlew assembleDebug` → `BUILD SUCCESSFUL` (keyless build).
- [x] `./gradlew testDebugUnitTest` → `BUILD SUCCESSFUL` (existing unit tests unaffected).
- [ ] `./gradlew connectedDebugAndroidTest` — DAO instrumented suite; run if a device is attached.

---

## Task 5: On-device checkpoint (owner-run)

Build + run on a device/emulator with internet, grant location, press **play**, walk/simulate movement.
Confirm:
- [ ] A real map renders (OpenFreeMap even with no key).
- [ ] The live GPS trail draws as a polyline that grows as you move.
- [ ] The location dot appears and the camera follows while recording.
- [ ] **Layers** cycles styles (and MapTiler Satellite/Outdoor if a key is set).
- [ ] **3D** tilts the camera; **Recenter** re-centres on the current position.

Then use `superpowers:finishing-a-development-branch` to merge `phase4-google-maps` into `main`.
