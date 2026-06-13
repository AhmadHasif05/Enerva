# Design: 4:5 gallery sizing + photo-hero run-summary card

**Date:** 2026-06-13
**Status:** Approved — ready for implementation plan

## Goal

Two independent UI improvements to the run/gallery experience:

1. **Consistent 4:5 sizing.** A posted run-summary card currently looks different in
   the profile grid (square crop) vs. the reel feed (whole card). Make a post present
   the same everywhere by standardizing on a **4:5** aspect ratio.
2. **Photo-hero run-summary card.** Redesign the branded run-summary card so the
   **captured photo is the hero image**, with ENERVA branding, a mini route map, and
   the run stats shown as a small **info chip in the bottom-right** corner.

## Background (current state)

- `view/screen/RunSummaryCard.kt` — the branded card is a vertical `Column`:
  ENERVA header bar → 220dp image (route map *or* photo) → DURATION/DISTANCE/PACE
  stats footer. Roughly **square**. Takes a single `snapshot: Bitmap?` plus an
  `isPhoto: Boolean` flag; shows `PaceLegend` over the image when it's the route map.
- `view/screen/RunSummarySheet.kt` — the End-of-run bottom sheet. Computes
  `cardImage = photo ?: snapshot` and passes that single bitmap to `RunSummaryCard`
  with `isPhoto = photo != null`. On Post it captures the on-screen card to a bitmap
  via a `GraphicsLayer` (`captureLayer`) and hands it back through `onPost`.
- `view/screen/ProfileScreen.kt` — the profile gallery grid renders 3-column tiles
  with `Modifier.aspectRatio(1f)` (square) on both image tiles and the empty filler
  boxes.
- `view/screen/GalleryScreen.kt` — the reel feed (`ReelPage`) renders full-screen and
  already uses `ContentScale.Fit` for `reel.isCard` posts (so the whole card shows,
  letterboxed on black) and `Crop` otherwise. **No change needed here.**
- `view/screen/RouteSnapshot.kt` — `captureRouteSnapshot(...)` renders the route map
  to a square (1000×1000) bitmap. **No change needed.**

## Design

### Part 1 — Consistent 4:5 sizing

**Profile grid tiles → 4:5.** In `ProfileScreen.kt`, change the gallery grid tiles
and the trailing empty filler boxes from `Modifier.aspectRatio(1f)` to
`Modifier.aspectRatio(4f / 5f)`. This affects only the self/other gallery grid; the
multi-select overlay, click/long-press behaviour, and Saved row are unchanged.

**Card exported at 4:5.** The card composable becomes a 4:5 `Box` (Part 2). Because
`RunSummarySheet` captures the card via `captureLayer.toImageBitmap()`, the exported
bitmap is automatically 4:5 — no separate export logic. The post therefore lines up
across the sheet preview, the profile grid (4:5 tiles, `Crop` = whole card since same
aspect), and the reel feed (`Fit`, whole card).

The reel feed is intentionally left full-screen; `isCard` already drives `Fit`.

### Part 2 — Photo-hero run-summary card

Rewrite `RunSummaryCard.kt` from the stacked `Column` into a **4:5 `Box`**
(`Modifier.aspectRatio(4f / 5f)`, clipped to `RoundedCornerShape(16.dp)`), composed
back-to-front:

1. **Hero image** — fills the frame with `ContentScale.Crop`.
   - Hero = the **photo** if one was taken, else the **route map**.
   - `snapshotLoading` still shows a centered `CircularProgressIndicator`; the
     "No route image" empty state is preserved when both are null.
2. **ENERVA wordmark** — overlaid `Alignment.TopStart`, white, bold, letter-spaced,
   with a subtle text shadow so it stays legible over any image.
3. **Bottom-right info chip** (`Alignment.BottomEnd`) — a rounded translucent panel
   (dark scrim, ~72% alpha) holding:
   - an "ENERVA" label,
   - a **mini route-map thumbnail** (~42dp rounded square, `Crop`), shown **only when
     the hero is a photo** (`photo != null && routeSnapshot != null`) so the route is
     still visible; omitted when the route map is already the hero,
   - a compact **DURATION / DISTANCE / PACE** row (label + value, three equal cells).
4. **Pace legend** — `Alignment.BottomStart`, shown **only when the route map is the
   hero** (no photo). The legend is meaningless over a photo, so it is hidden then.

**Signature change.** `RunSummaryCard` takes both bitmaps explicitly instead of one
`snapshot` + `isPhoto`:

```kotlin
@Composable
fun RunSummaryCard(
    photo: Bitmap?,
    routeSnapshot: Bitmap?,
    snapshotLoading: Boolean,
    timeText: String,
    distanceText: String,
    paceText: String,
    modifier: Modifier = Modifier,
    captureLayer: GraphicsLayer? = null,
)
```

Derived inside the card:
- `heroIsPhoto = photo != null`
- `hero = photo ?: routeSnapshot`
- `showRouteThumb = photo != null && routeSnapshot != null`
- `showPaceLegend = !heroIsPhoto && routeSnapshot != null`

**`RunSummarySheet.kt` changes.** Pass both bitmaps through instead of the merged
`cardImage`:

```kotlin
RunSummaryCard(
    photo = photo,
    routeSnapshot = snapshot,
    snapshotLoading = snapshotLoading && photo == null,
    timeText = timeText,
    distanceText = distanceText,
    paceText = paceText,
    modifier = Modifier.fillMaxWidth(),
    captureLayer = captureLayer,
)
```

Everything else in the sheet is unchanged: the camera launcher, `photo`/`pendingPhotoFile`
state, the Take photo / Retake / "Use route map" controls, the `includePhoto` switch,
caption field, and the Post path (`captureLayer.toImageBitmap().asAndroidBitmap()`).
`hasImage` becomes `photo != null || snapshot != null`.

### Out of scope / explicitly unchanged

- No changes to Room entities, Firestore docs, models, or `isCard`. Run posts stay
  `isCard = true` so the reel feed continues to use `Fit`.
- `captureRouteSnapshot` keeps producing a square bitmap; it is cropped into the 4:5
  hero or the small thumbnail.
- `GalleryScreen` / `ReelPage` unchanged.
- Cross-device behaviour unchanged (the card image already only syncs for the owner;
  remote copies fall back to the drawable and render `Crop`).

## Files touched

| File | Change |
|------|--------|
| `view/screen/RunSummaryCard.kt` | Rewrite: 4:5 `Box`, photo hero, ENERVA overlay, bottom-right info chip, conditional route thumb + pace legend. New signature. |
| `view/screen/RunSummarySheet.kt` | Pass `photo` + `routeSnapshot` separately; update `hasImage`. |
| `view/screen/ProfileScreen.kt` | Gallery grid tiles + filler boxes `aspectRatio(1f)` → `aspectRatio(4f / 5f)`. |

## Verification

This module has no UI test harness; verification matches the project's established
practice:

- **Compile:** `./gradlew assembleDebug` → `BUILD SUCCESSFUL`
  (git-bash, `JAVA_HOME` = Android Studio JBR — see `memory/build-env.md`).
- **Manual (on device):**
  1. Record a run, End → the summary card is 4:5 with the route map as hero, ENERVA
     top-left, stats chip bottom-right, pace legend bottom-left, **no** route thumb.
  2. Take a photo → photo becomes the hero; the chip now shows the mini route-map
     thumbnail; the standalone pace legend is gone.
  3. Post → the reel feed shows the whole 4:5 card (no side clipping).
  4. Open Profile → the gallery tiles are 4:5 and the posted card shows whole,
     matching the feed.

## Self-review notes

- The route snapshot stays square; cropping it into a 4:5 hero trims a little off the
  sides — acceptable, and the route is recentered on its bounds at capture time.
- The `includePhoto` switch keeps its current meaning ("attach the generated card
  bitmap"); the captured card always carries the overlay regardless of hero source.
