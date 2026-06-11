# Record Screen Polish — Design (Phase 4.5)

**Date:** 2026-06-11
**Branch:** `phase4-google-maps`
**Status:** Approved, ready for implementation plan

## Context

Phase 4 put a real MapLibre map in the Record screen with live location and a
breadcrumb route (`RecordScreen.kt`, `RecordViewModel.kt`). On-device testing
surfaced five rough edges. This spec covers polishing the existing Record
screen only. It does **not** start the camera/reels phase (CameraX, Firebase
Storage, video) — that remains future work.

Current behaviour:
- `RecordViewModel.onLocation()` adds every GPS fix to `path` and sums Haversine
  segments into `distanceKm`; speed comes from the device or a distance/time
  fallback.
- `RecordScreen.kt` draws the path as a `Polyline`, shows a stats `Card` with
  Time / Distance / Speed, and a "Stop" button that calls `reset()`.
- The stats card overlaps the right-side Layers / 3D / Recenter map buttons.
- Gallery reels render `imageUri ?: imageRes` via Coil `AsyncImage`, so a saved
  file path in `imageUri` shows as the reel background with no gallery changes.

## Goals

1. Route line reads as a clean trail that follows the road (no API key, free).
2. Valid stats — distance/pace not inflated by GPS noise.
3. Show **average pace** (min/km) instead of speed.
4. Stats info bar is minimizable so it stops covering the 3 map buttons.
5. "Stop" becomes "End": opens a summary, optionally posts the run (with a route
   picture + caption) to the gallery reels feed, or discards it.

## Non-Goals

Snap-to-roads / map-matching API, CameraX capture, Firebase Storage upload,
video reels, gallery-side UI changes, DB schema migration, new dependencies.

## Design

### 1. Cleaner route + valid stats (shared root cause: GPS noise)

The jagged line and the inflated distance are the same problem. Fix in
`RecordViewModel.onLocation()`:

- **Accuracy gate** — `onLocation` gains an `accuracyM: Float?` parameter sourced
  from `Location.getAccuracy()`. Drop any fix with accuracy worse than ~25 m.
- **Jitter gate** — ignore a new fix whose Haversine distance from the last
  *kept* point is below ~4 m (standing still must not accrue distance).
- A fix that passes both gates is appended to `path` and its segment added to
  `distanceKm`.
- **Render** — draw the `Polyline` thicker with rounded line joins/caps so it
  reads as a smooth trail hugging the road.

Thresholds (`MIN_ACCURACY_M = 25f`, `MIN_MOVE_M = 4.0`) live as named constants
so they are easy to tune.

### 2. Average pace instead of speed

- Remove `currentSpeedKmh` and the speed branch from `onLocation`.
- Add a derived value `averagePaceSecPerKm = elapsedSeconds / distanceKm` (guard
  against zero distance).
- Format as `m:ss /km` (e.g. `6:12 /km`); show `--:-- /km` until there is enough
  distance to be meaningful (e.g. distance > ~0.01 km).
- Stat card's third tile becomes "Pace (/km)".

### 3. Minimizable info bar

- The stats `Card` gets an expanded/collapsed state via `rememberSaveable`.
- **Expanded** (default): full card — Time / Distance / Pace.
- **Collapsed**: a compact pill at bottom-center showing just elapsed time,
  clearing the right edge so all three map action buttons are tappable.
- The existing `OpenInFull` icon becomes the real toggle (swap to a
  collapse/expand chevron pair).

### 4. End button + summary sheet

- Rename the bottom-right "Stop" control to **"End"** and change its icon
  (e.g. `Icons.Default.Flag` / outlined stop) — distinct from Pause.
- Tapping End **pauses** recording (does not reset) and opens a summary
  presented as a Material3 `ModalBottomSheet` (no new navigation route). Lives in
  a new composable file to keep `RecordScreen` focused.
- Summary sheet contents:
  - **Route picture** — a static PNG of the route, produced by MapLibre's
    `MapSnapshotter` (or equivalent) over the recorded path, written to
    app-internal storage; its path is the candidate `imageUri`.
  - **Stats** — Time, Distance, Pace.
  - **Caption** text field (default "New run").
  - **"Include route photo"** toggle, ON by default.
  - **Post** and **Discard** buttons.
- **Post** → `saveActivity(type, caption, imageUri)` where `imageUri` is the
  snapshot path when the toggle is ON, else `null` (falls back to the existing
  drawable). Closes the sheet and resets the screen for the next run.
- **Discard** → `reset()` and close the sheet; nothing is saved.

### 5. Reaching the reels feed

- Extend `saveActivity()` to accept `caption` and a nullable `imageUri`, storing
  them on the inserted `MediaEntity` (it already has `imageUri: String?`).
- No gallery code changes: `ReelPage` already renders `imageUri ?: imageRes`.

## Files Touched

- `view_model/RecordViewModel.kt` — accuracy/jitter gates + `accuracyM` param;
  replace speed with average pace; extend `saveActivity(caption, imageUri)`.
- `view/screen/RecordScreen.kt` — forward accuracy; pace display; collapsible
  stats card; rename Stop→End; trigger summary sheet; route snapshot capture.
- New file (e.g. `view/screen/RunSummarySheet.kt`) — the summary bottom sheet.

No DB migration (uses existing `MediaEntity` columns), no new dependencies, no
gallery-side changes.

## Testing

- `RecordViewModel` unit tests (JVM): accuracy gate rejects low-accuracy fixes;
  jitter gate rejects sub-threshold moves; distance accumulates only over kept
  segments; average pace formats correctly and guards zero distance;
  `saveActivity` inserts a `MediaEntity` carrying the caption and `imageUri`
  (null when photo excluded).
- Manual on-device: walk a short loop — line follows the road, distance is
  sane, pace shows min/km, info bar collapses to reveal the 3 buttons, End opens
  the sheet with a route snapshot, Post makes the run appear in Gallery reels
  with the route picture, Discard leaves nothing behind.

## Risks / Open Questions

- `MapSnapshotter` styling/threading: snapshot generation is async and may need
  a brief loading state in the sheet. If it proves fragile, fallback is rendering
  the path onto a `Bitmap`/`Canvas` without the basemap.
- Accuracy/jitter thresholds are best-effort defaults; tune during on-device
  testing.
