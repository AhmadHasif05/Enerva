# Branded Run-Summary Card — Design (Phase 5)

**Date:** 2026-06-12
**Branch:** `main`
**Status:** Approved, ready for implementation plan

## Context

Phase 4.5 added an End → summary `ModalBottomSheet` (`RunSummarySheet.kt`) that
shows a MapLibre route snapshot, Time/Distance/Pace stats, a caption field, an
"include route photo" toggle, and Post/Discard. Post writes a `MediaEntity` with
the snapshot path as `imageUri`, and the gallery renders it as the reel image.

The current sheet is functional but plain: a single-colour route line, stats as
bare text, no branding. The user wants the summary to look like the Enerva
run-summary card (a branded card: wordmark header, a map with a pace-coloured
route + START/FINISH markers, and an icon-driven stats footer) — and crucially,
for that **same card to be the image posted to the gallery feed**, not just an
in-app view.

This is a Phase 5 polish item layered on the existing Phase 4.5 summary flow. It
does **not** add CameraX/photo capture or Firebase Storage (still future work).

## Decisions (from brainstorming)

- **Where it lives:** Both — the on-screen summary sheet shows the card, and the
  same card design is rendered to the bitmap that becomes the posted reel image.
- **Route line:** Pace-coloured segments (green = fast … red = slow), computed
  from the run's own pace range. (Not a plain orange line, not a mere
  start→finish progress gradient.)
- **Header / wordmark:** "ENERVA" in SansSerif bold on a solid `StravaOrange`
  bar — fully consistent with the app's existing theme tokens.
- **Markers + legend:** Green START and red FINISH dots; a small FAST→SLOW
  colour legend overlaid on the map so the route colours are self-explanatory.
- **Basemap:** The existing MapLibre / OpenFreeMap snapshot (place labels such as
  park names come baked into the basemap — no extra work).

## Goals

1. A branded `RunSummaryCard` = ENERVA header + pace-coloured route map +
   icon stats footer, styled entirely from `MaterialTheme` tokens.
2. The route is coloured by per-segment speed using the `timeMs` already stored
   on each `TrackPoint` — no new data capture.
3. The exact card shown in the sheet is what gets posted to the gallery feed
   (one composable, rendered to a bitmap), so preview and feed match.
4. Keep the existing sheet controls (caption, include-photo toggle,
   Post/Discard) and the existing `saveActivity(caption, imageUri)` flow.

### 5. Activity-type toggle (Walk ↔ Run/Jog) — added scope

Today the activity-type chip in the Record bottom bar (`RecordScreen.kt`, the
`DirectionsWalk` circle + "Walk" label) is decorative: it has no `onClick`, the
stats-card header is a hardcoded "Walk", and Post calls
`saveActivity(type = "Walk", …)`. The type can never actually be chosen.

- Hold the selected type in `RecordViewModel` (e.g. `activityType: String` with
  values `"Walk"` / `"Run"`, default `"Run"` to match `saveActivity`'s default;
  "Jog" is treated as Run).
- Make the chip a real toggle: tapping it cycles Walk ↔ Run, swapping the icon
  (`DirectionsWalk` ↔ `DirectionsRun`) and the label.
- Drive the stats-card header label and the `saveActivity(type = …)` call from
  that state instead of the literal `"Walk"`. Optionally surface the type on the
  summary card.

## Non-Goals

CameraX capture, Firebase Storage upload, gallery-side UI changes, DB schema
migration, new dependencies, changing the GPS gating/pace logic itself.

## Design

### Architecture choice

**One shared `RunSummaryCard` composable, captured to a bitmap for posting.**
The card is authored once in Compose; the sheet renders it live, and Post
captures that same composable via `GraphicsLayer.toImageBitmap()` (available in
Compose 1.7 / BOM 2024.09, already in use). Single source of truth → the feed
image is exactly the previewed card, with no duplicated drawing code.

Rejected alternatives: (a) drawing the posted image separately with a raw
Android `Canvas` — duplicates layout and drifts from the on-screen card;
(b) letting MapLibre bake the whole card — it cannot render the header/footer
chrome. So MapLibre renders only the map portion; Compose owns the chrome and
the final capture.

### 1. Pace → colour (pure logic)

A pure function (in `RouteAccumulator.kt` or a small sibling file, JVM-testable
like the existing gating code) takes `List<TrackPoint>` and returns a per-vertex
colour list:

- For each segment between kept points, speed = segmentDistanceKm / (Δ`timeMs`).
- Map speed onto a green→yellow→orange→red ramp scaled to the run's own
  min/max segment speed (so colour is relative to that run).
- Guard degenerate cases: < 2 points, zero/negative Δtime, all-equal speeds
  (fall back to a single mid-ramp colour or the brand orange).

### 2. Map bitmap (`RouteSnapshot.kt`, extended)

- `captureRouteSnapshot` accepts the per-vertex colours and renders the pace
  gradient using MapLibre's `lineGradient` with `lineMetrics = true` on the
  `GeoJsonSource` (gradient interpolated along line-progress, stops derived from
  each vertex's cumulative fraction + its pace colour).
- Add two marker layers: a green START dot at the first point and a red FINISH
  dot at the last (`CircleLayer`, or small symbol icons).
- Signature stays callback-based and returns the `MapSnapshotter` handle for
  cancellation, as today. Fallback to a single-colour line if colours are absent.

### 3. `RunSummaryCard` composable (new file)

`view/screen/RunSummaryCard.kt` — a self-contained card:

- **Header:** full-width `StravaOrange` bar, "ENERVA" in SansSerif bold,
  letter-spaced, `OnPrimary` white.
- **Map:** the snapshot `Image` (or a spinner while it renders / a placeholder
  when absent), with the small FAST→SLOW legend pill overlaid bottom-right.
- **Footer:** dark (`surface`/`background`) row of three tiles —
  Duration / Distance / Pace — each an icon (`Icons.Default.Timer`, `Place`,
  `Speed`), an UPPERCASE `onSurfaceVariant` label, and a bold `onSurface` value;
  thin dividers between tiles.
- Inputs: snapshot `Bitmap?`, loading flag, the three stat strings. No business
  logic inside.

### 4. `RunSummarySheet` (restyle existing)

- Replace the inline map `Box` + `SummaryStat` row with `RunSummaryCard`.
- Keep the caption `OutlinedTextField`, the "Include route photo" `Switch`, and
  the Discard/Post buttons.
- **Post:** capture the `RunSummaryCard` to a bitmap via a `GraphicsLayer`
  (record the card's draw into the layer, `toImageBitmap()`), hand it to
  `saveBitmapToInternalStorage`, then call the existing
  `saveActivity(caption, imageUri)`. When "include route photo" is OFF, behave as
  today (`imageUri = null`, drawable fallback).
- The bitmap captured for posting is the whole card (header + map + footer), so
  the reel shows the branded card, not a bare map.

## Files Touched

- `view_model/RouteAccumulator.kt` (or new sibling) — pure pace→colour ramp.
- `view/screen/RouteSnapshot.kt` — `lineGradient` pace colouring + START/FINISH
  markers; per-vertex colour parameter.
- `view/screen/RunSummaryCard.kt` — **new** branded card composable.
- `view/screen/RunSummarySheet.kt` — use the card; capture-to-bitmap on Post.
- `view/screen/RecordScreen.kt` — pass per-vertex pace colours into the snapshot
  call (it already owns the points + style + snapshot trigger); wire the
  activity-type toggle (chip onClick, header label, `saveActivity(type = …)`).
- `view_model/RecordViewModel.kt` — hold `activityType` state for the toggle.

No DB migration, no new dependencies, no gallery-side changes.

## Testing

- **Unit (JVM):** pace→colour ramp — monotonic mapping (faster segment → greener
  end of the ramp), correct handling of < 2 points, zero/negative Δtime, and
  all-equal speeds (single fallback colour). Reuse the existing
  `RouteAccumulator` test style.
- **Manual on-device:** walk a loop with a deliberate fast stretch and a slow
  stretch → End → the card shows ENERVA header, a route whose colour shifts
  green↔red along those stretches, START/FINISH dots, the legend, and correct
  Duration/Distance/Pace. Post → the gallery reel shows the same branded card
  image. Toggle OFF → posts with the drawable fallback. Discard → nothing saved.

## Risks / Open Questions

- **`GraphicsLayer` capture timing:** the card must be laid out/drawn before
  capture. If capturing the live sheet composable proves fragile, fall back to
  composing the card off-screen into the layer on Post. Keep capture on the main
  thread; show a brief progress state if needed (the sheet already has a
  `snapshotLoading` pattern).
- **`lineGradient` constraints:** requires `lineMetrics` and is incompatible with
  some dash/cap combinations; verify rounded caps still render. If gradient
  proves unreliable in the snapshot, fall back to multiple per-segment
  `LineLayer` features each with a solid colour.
- **Pace-range scaling:** colouring relative to the run's own min/max can make a
  near-constant-pace run look falsely dramatic. Acceptable for a visual; could
  later switch to absolute pace bands.
