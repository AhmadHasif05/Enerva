# Plan: Reel post-stat frame too big / clipped — ✅ Done (Option A)

> Shipped via Option A. `isCard` threaded through `GalleryActivity`, `MediaEntity`
> (DB 5→6, destructive migration — no migration code), `GalleryRepository.createPost`
> (default false), `buildRunMedia`/`RecordViewModel.saveActivity`, and the `toModel`
> mapper. `RecordScreen` sets `isCard = uri != null` (a stats-only run with no captured
> bitmap stays Crop). `ReelPage` renders `ContentScale.Fit` for cards, `Crop` otherwise.
> `isCard` is **device-local** (not added to the Firestore docs): the card image itself
> doesn't sync, so remote copies fall back to the drawable and correctly stay Crop.
> `compileDebugKotlin` + `testDebugUnitTest` green.


## Bug
On the Gallery reels feed, a user post (the branded **RunSummaryCard** bitmap)
is over-zoomed and its stats footer is clipped at the sides
(DURATION → "TION", PACE → "PA"). See `Downloads/photo_2026-06-12_02-20-32.jpg`.

## Root cause
- A user post's image is the captured `RunSummaryCard` (ENERVA header + 220dp
  image + stats footer) — roughly **square** aspect.
  - Captured in `RunSummarySheet.kt` via `captureLayer.toImageBitmap()`, saved
    to a file in `RecordScreen.kt:323` (`saveBitmapToInternalStorage`), stored
    as `imageUri`.
- `GalleryScreen.kt` → `ReelPage` (line ~193) renders it full-screen portrait
  with `ContentScale.Crop`. Crop scales the square card by height to cover the
  tall reel → width overflows → **left/right of the stats footer get clipped**,
  and the whole card looks oversized.
- `Crop` is correct for full-bleed demo photos (`imageRes`, e.g. lakesidetrail)
  and raw photos posted via `CreatePostDialog`, so we can't blanket-switch to Fit.

## Fix (next session)
Distinguish a **branded-card post** from a **full-bleed photo** and render cards
with `ContentScale.Fit` (centered on the black reel bg → whole frame visible,
nothing clipped; also stops the bottom author/caption overlay from colliding
with the stats footer).

Two options for the flag:

### Option A (recommended) — thread an `isCard` boolean through the model
1. `model/GalleryActivity.kt`: add `val isCard: Boolean = false`.
2. `data/entities/Entities.kt` `MediaEntity`: add `val isCard: Boolean = false`
   (Room column). Bump DB version + add a trivial migration **or** confirm the
   DB already uses `fallbackToDestructiveMigration` (check the `@Database`
   builder) — if so, no migration code needed.
3. `data/repository/GalleryRepository.kt`: carry `isCard` through `createPost`
   (line ~63) and the entity↔model mappers (lines ~164–208).
4. `view_model/RecordViewModel.saveActivity` (line ~116) + `RecordScreen.kt:324`
   onPost: pass `isCard = true` (the run-summary post is always a card). The
   `CreatePostDialog` path (`GalleryViewModel.createPost`) stays `isCard = false`.
5. `GalleryScreen.kt` `ReelPage` AsyncImage:
   `contentScale = if (reel.isCard) ContentScale.Fit else ContentScale.Crop`.

### Option B (no DB change, hacky) — infer at display time
Decode the bitmap bounds and pick `Fit` when aspect ≈ square / landscape,
`Crop` when portrait. Avoid — unreliable; prefer A.

## Verify
- Build with git-bash + JAVA_HOME=Android Studio jbr (see memory `build-env.md`).
- Run app, post a run summary, open Gallery reel: full ENERVA card visible,
  DURATION/DISTANCE/PACE all readable, no side clipping; demo reels still
  full-bleed (Crop).

## Files touched (summary)
- model/GalleryActivity.kt
- data/entities/Entities.kt (+ DB version/migration if not destructive)
- data/repository/GalleryRepository.kt
- view_model/RecordViewModel.kt
- view/screen/RecordScreen.kt
- view/screen/GalleryScreen.kt
