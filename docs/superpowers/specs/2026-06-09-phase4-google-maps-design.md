# Phase 4 — Real Map in the Record Screen (MapLibre + MapTiler/OpenFreeMap)

> Replace the hand-drawn `TrailCanvas` map stand-in with a live vector map, draw the
> GPS trail as a `Polyline`, show the current position via the map's built-in location
> layer, and wire the three previously-inert map action buttons. Remove the inert
> search/filter bar.

- **Phase:** 4 of the Enerva roadmap (`plan.md` §8).
- **Status going in:** Record screen renders a fake grid + breadcrumb (`TrailCanvas`); the
  right-side buttons (Layers / 3D / MyLocation) and the top filter chips are all `onClick = { }`.
- **Checkpoint to exit:** live GPS path drawn on a real map on-device.

---

## 0. Provider decision (changed from the original Google Maps draft)

The original draft of this spec targeted **Google Maps SDK for Android**. That was changed
because Google Maps Platform **requires a billing account with a credit card** to mint a working
API key, even though the Android SDK itself is free. The project owner does not want to attach a
card.

**New decision: [MapLibre Native](https://maplibre.org/) (open-source renderer) with
[MapTiler](https://www.maptiler.com/) tiles.**

| Requirement | How this stack satisfies it |
|-------------|------------------------------|
| **No credit card** | MapTiler's free tier (100k loads/month) needs no card. **OpenFreeMap** needs no account at all. |
| **API key acceptable** | MapTiler uses a simple API key appended to the style URL (`?key=...`). |
| **Up-to-date maps** | Both MapTiler and OpenFreeMap render **OpenStreetMap** data, continuously updated. |
| **Free & open** | MapLibre Native is BSD/MPL open-source; no per-load billing. |

**Compose integration:** MapLibre Native is a View-based SDK. We use
[`ramani-maps`](https://github.com/ramani-maps/ramani-maps) (`org.ramani-maps:ramani-maplibre`),
a Jetpack-Compose wrapper providing a `MapLibre` composable plus `Polyline`, `Circle`, `Symbol`,
a `CameraPositionState`, and a built-in location layer.

> The committed spec/plan filenames keep `google-maps` in their names to preserve the existing
> cross-links from `plan.md`; the content below is the source of truth.

---

## 1. Goals & Non-Goals

### Goals
1. A real vector-map backdrop on the Record screen.
2. The live GPS trail drawn as a `Polyline` from `RecordViewModel.path`.
3. The current location shown via MapLibre's built-in location layer (blue dot).
4. The camera follows the trail while recording.
5. The three right-side action buttons do real work: Layers (style), 3D (tilt), MyLocation (recenter).
6. A map key supplied out-of-band (never committed) — **optional**, because OpenFreeMap works keyless.

### Non-Goals (explicitly out of scope)
- Changes to GPS tracking logic, distance/speed math, or `saveActivity` (untouched).
- Route data model, route discovery, or plotting saved routes on the map.
- Phase 5 camera capture.
- Any new `RecordViewModel` state — the map controls are view-local concerns.

---

## 2. Dependencies & key plumbing

**Decision:** reuse the existing `local.properties` → `Properties()` → injected-value pattern already
used for `GOOGLE_WEB_CLIENT_ID` in `app/build.gradle.kts`. MapLibre constructs its own tile-request
URLs from a **style URL** string, so — unlike Google Maps — there is **no manifest meta-data key**.
The key is just a string we splice into the MapTiler style URL, so it is surfaced via a
**`buildConfigField`** (exactly like `GOOGLE_WEB_CLIENT_ID`), not a `manifestPlaceholder`.

Changes:
- `gradle/libs.versions.toml`: add `ramani-maplibre` version + library entry (pulls MapLibre Native transitively).
- `app/build.gradle.kts`:
  - Read `MAPTILER_API_KEY` from `local.properties` (shared `Properties()` block), defaulting to `""`.
  - Expose it via `buildConfigField("String", "MAPTILER_API_KEY", "\"$mapTilerApiKey\"")`.
  - Add the `implementation(libs.ramani.maplibre)` dep.
- `local.properties`: developer adds `MAPTILER_API_KEY=...` (git-ignored; never committed). **Optional.**
- `AndroidManifest.xml`: **no change** (MapLibre needs no API-key meta-data; INTERNET + location perms already present).

**Keyless fallback (better than Google's blank-tile):** with an empty key the project compiles, runs,
and shows a **real map** because the default style is **OpenFreeMap** (`https://tiles.openfreemap.org/styles/liberty`),
which needs no key. A MapTiler key only unlocks the extra MapTiler styles (satellite/outdoor) in the
Layers cycle.

---

## 3. The map (replaces `TrailCanvas`)

In `RecordScreen.kt`:
- Replace the `TrailCanvas(...)` call with a `MapLibre(...)` composable filling the existing root `Box`.
- **Style:** `MapStyle.Uri(currentStyleUrl)` where `currentStyleUrl` is view-local `remember` state,
  initialised to OpenFreeMap Liberty.
- **Trail:** map `recordViewModel.path` (`List<TrackPoint>`) → `List<LatLng>` (`org.maplibre.android.geometry.LatLng`)
  and render a `Polyline(points, color = "#<primary hex>", lineWidth = 5f)`. A start `Symbol`/`Circle` marks `path.first()`.
- **Current position + follow:** pass `locationRequestProperties = LocationRequestProperties()`,
  `userLocation = remember { mutableStateOf(Location(null)) }`, `renderMode = RenderMode.COMPASS`, and
  `cameraMode` = `CameraMode.TRACKING` while recording, `CameraMode.NONE` otherwise. MapLibre draws the
  blue dot and auto-follows — no custom marker or camera `LaunchedEffect` needed for following.
- **Feed the ViewModel:** a `LaunchedEffect(userLocation.value)` forwards valid fixes to
  `recordViewModel.onLocation(lat, lng, speed, time)`. This **replaces** the manual
  `FusedLocationProviderClient`/`LocationUpdatesEffect` wiring (deleted).
- **Empty-state overlay preserved:** the centered overlay ("Location permission required" /
  "Press play to start tracking" / "Waiting for GPS fix...") stays, drawn on top while `path` is empty.

`TrailCanvas`, its grid-drawing code, and the `Canvas`/`Path`/`Offset`/`Stroke` imports it needed are removed.

---

## 4. The three right-side action buttons

Pure view/map concerns — held in `remember` state inside `RecordScreen`, **no `RecordViewModel` changes**.
`RecordMapActionButton` gains an `onClick` parameter (currently hardcoded `{ }`).

| Button | Icon | Behaviour |
|--------|------|-----------|
| **Layers** | `Outlined.Layers` | Cycle through the available style URLs (OpenFreeMap Liberty/Bright, plus MapTiler Streets/Satellite/Outdoor when a key is set). |
| **3D** | `Default.ViewInAr` | Toggle camera tilt `0.0° ↔ 45.0°` via `cameraPositionState.position.copy(tilt = …)`. |
| **MyLocation** | `Default.MyLocation` | Re-engage follow: set `cameraMode.intValue = CameraMode.TRACKING` (and/or set `position` target to `userLocation`). |

---

## 5. Remove the search/filter top bar

Per the approved scope ("map buttons only"), the top `Surface` "Search locations" bar and the
`LazyRow` of filter chips (`Routes / Length / Difficulty / Elevation`) are **removed** from
`RecordScreen` — carried over from the deleted Maps screen, they imply an unbuilt route-discovery
dataset. The top-left back/Activities `IconButton` is retained.

---

## 6. Deliverables

1. The code changes in §2–§5.
2. `docs/setupmaps.md` — a step-by-step **MapTiler** guide: create a free MapTiler Cloud account
   (no card), copy the default API key, place it in `local.properties` as `MAPTILER_API_KEY`. Notes
   the OpenFreeMap keyless default.
3. Doc updates: mark Phase 4 done in `plan.md` §8; refresh the Record-screen note in §10
   ("map backdrop is a placeholder") and the Tech-Stack rows (Maps SDK → MapLibre); update `README.md`
   if it references the placeholder map.

---

## 7. Testing & verification

- **No new unit-testable logic.** The map is view-layer; `TrackPoint → LatLng` is a trivial mapping.
  Existing unit tests (`testDebugUnitTest`) and DAO instrumented tests (`connectedDebugAndroidTest`)
  must remain green.
- **Build gate:** `./gradlew assembleDebug` succeeds both with and without a key present.
- **On-device checkpoint:** build + run, grant location, press play, walk/simulate movement → confirm
  (a) a real map renders (OpenFreeMap even with no key), (b) the live GPS polyline draws, (c) the blue
  location dot appears, (d) the camera follows, (e) Layers cycles styles, (f) 3D tilts, (g) MyLocation recenters.

---

## 8. Risks & notes

- **ramani-maps version drift.** Pin `ramani-maplibre` to current stable (`0.10.0` at writing). The
  Compose API (`MapLibre`, `MapStyle.Uri`, `Polyline`, `CameraPosition(target, zoom, tilt, bearing)`,
  `rememberCameraPositionState`) was confirmed against the repo source; the compile gate catches any skew.
- **MapTiler key is optional.** Code completes and the map renders via OpenFreeMap before any key exists;
  a key only enriches the Layers cycle. So the on-device checkpoint is **not** blocked on a key.
- **OpenFreeMap is a community service.** Fine for an educational/portfolio app; MapTiler is the more
  robust path for heavier use, hence the optional key.
- **MPL license.** MapLibre Native and ramani-maps are MPL-2.0 — fine for this educational project.
