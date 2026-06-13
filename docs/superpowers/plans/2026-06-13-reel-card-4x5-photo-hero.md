# 4:5 Gallery Sizing + Photo-Hero Run-Summary Card — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign the run-summary card so the captured photo is the hero image (4:5) with ENERVA + a route thumbnail + stats in a bottom-right info chip, and make the profile gallery tiles 4:5 so a posted card looks the same in the grid and the reel feed.

**Architecture:** `RunSummaryCard` is rewritten from a stacked `Column` into a 4:5 `Box`: hero image (photo if taken, else route map) with overlays — ENERVA wordmark top-left, a translucent info chip bottom-right, and the pace legend bottom-left only when the route map is the hero. The card's signature takes the photo and the route snapshot separately. Because `RunSummarySheet` captures the card via a `GraphicsLayer`, a 4:5 card automatically exports a 4:5 bitmap; the profile grid tiles are switched from square to 4:5 to match. No data/model changes.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3.

**Testing note:** This module has no unit/instrumentation test harness; the change is Compose UI. Each task is verified by a clean Gradle compile (`./gradlew assembleDebug`), matching the project's established practice. Task 3 is a manual on-device check.

**Build/verify command** (git-bash, from project root — see `memory/build-env.md`):
```bash
export JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"   # adjust to your Android Studio JBR
./gradlew assembleDebug
```
Expected on success: `BUILD SUCCESSFUL`.

**Spec:** `docs/superpowers/specs/2026-06-13-reel-card-4x5-photo-hero-design.md`

---

## File Structure

- `view/screen/RunSummaryCard.kt` — **rewrite.** 4:5 `Box`; hero image; ENERVA overlay; `InfoChip` (ENERVA + optional route thumbnail + DURATION/DISTANCE/PACE); `ChipStat` helper; keep `PaceLegend`; drop the old `SummaryStatTile`/`FooterDivider`. New signature: `photo` + `routeSnapshot` instead of `snapshot` + `isPhoto`.
- `view/screen/RunSummarySheet.kt` — **modify.** Pass `photo` + `routeSnapshot` to the card; recompute `hasImage`.
- `view/screen/ProfileScreen.kt` — **modify.** Gallery grid tiles + filler boxes `aspectRatio(1f)` → `aspectRatio(4f / 5f)`.

---

## Task 1: Rewrite RunSummaryCard as a 4:5 photo-hero card

**Files:**
- Modify (full rewrite): `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/RunSummaryCard.kt`

- [x] **Step 1: Replace the entire contents of `RunSummaryCard.kt`**

Replace the whole file with:

```kotlin
package com.example.a211198_hasif_drnelson_Project2.view.screen

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// The branded run-summary card: a 4:5 hero image (the user's photo if taken, else
// the pace-coloured route map) with overlays — ENERVA wordmark top-left, a stats
// info chip bottom-right, and (only when the route map is the hero) the pace legend
// bottom-left. Used on screen in RunSummarySheet and, via `captureLayer`, exported
// to the 4:5 bitmap posted to the gallery. Pure presentation — no logic.
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
) {
    val colors = MaterialTheme.colorScheme

    val heroIsPhoto = photo != null
    val hero = photo ?: routeSnapshot
    // Show the route thumbnail in the chip only when a photo took the hero slot, so
    // the route stays visible. When the route map is already the hero, the thumbnail
    // would be redundant.
    val showRouteThumb = photo != null && routeSnapshot != null
    // The pace legend is meaningful only over the route map, never over a photo.
    val showPaceLegend = !heroIsPhoto && routeSnapshot != null

    // When a capture layer is supplied, record this card's drawing into it so the
    // sheet can export the exact on-screen card as a bitmap.
    val captureModifier = if (captureLayer != null) {
        Modifier.drawWithContent {
            captureLayer.record { this@drawWithContent.drawContent() }
            drawLayer(captureLayer)
        }
    } else Modifier

    Box(
        modifier = modifier
            .aspectRatio(4f / 5f)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.background)
            .then(captureModifier)
    ) {
        // Hero image (or loading / empty state).
        when {
            snapshotLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            hero != null -> Image(
                bitmap = hero.asImageBitmap(),
                contentDescription = if (heroIsPhoto) "Run photo" else "Route map",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No route image", color = colors.onSurfaceVariant)
            }
        }

        // Top + bottom scrim so the white wordmark and the chip stay legible over
        // any image.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.25f),
                        0.35f to Color.Transparent,
                        0.7f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.45f)
                    )
                )
        )

        // ENERVA wordmark, top-left.
        Text(
            "ENERVA",
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp,
            letterSpacing = 3.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 14.dp, top = 12.dp)
        )

        // Pace legend, bottom-left — only when the route map is the hero.
        if (showPaceLegend) {
            PaceLegend(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 12.dp)
            )
        }

        // Info chip, bottom-right: ENERVA + (route thumbnail when hero is a photo) +
        // the three stats.
        InfoChip(
            timeText = timeText,
            distanceText = distanceText,
            paceText = paceText,
            routeThumb = if (showRouteThumb) routeSnapshot else null,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 12.dp)
        )
    }
}

@Composable
private fun InfoChip(
    timeText: String,
    distanceText: String,
    paceText: String,
    routeThumb: Bitmap?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .widthIn(max = 180.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.72f))
            .padding(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (routeThumb != null) {
                Image(
                    bitmap = routeThumb.asImageBitmap(),
                    contentDescription = "Route map",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                "ENERVA",
                color = Color(0xFFFFB38A),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            ChipStat(Icons.Filled.Timer, "DURATION", timeText, Modifier.weight(1f))
            ChipStat(Icons.Filled.Place, "DISTANCE", distanceText, Modifier.weight(1f))
            ChipStat(Icons.Filled.Speed, "PACE", paceText, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ChipStat(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 7.sp,
            letterSpacing = 0.5.sp,
            fontWeight = FontWeight.Medium
        )
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun PaceLegend(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(colors.background.copy(alpha = 0.7f))
            .padding(horizontal = 7.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text("FAST", color = colors.onSurfaceVariant, fontSize = 8.sp)
        Box(
            modifier = Modifier
                .width(46.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(
                    // Mirrors the pace ramp stops in PaceColors.kt (fast→slow).
                    Brush.horizontalGradient(
                        listOf(Color(0xFF4CAF50), Color(0xFFFFC107), Color(0xFFFF7043), Color(0xFFFC4C02))
                    )
                )
        )
        Text("SLOW", color = colors.onSurfaceVariant, fontSize = 8.sp)
    }
}
```

- [x] **Step 2: Compile**

Run:
```bash
export JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
./gradlew assembleDebug
```
Expected: this **fails to compile** — `RunSummarySheet.kt` still calls `RunSummaryCard(snapshot = …, isPhoto = …)` with the old signature (errors like "No value passed for parameter 'photo'" / "Cannot find a parameter with this name: snapshot"). That's expected; Task 2 updates the call site. (If you prefer a green build at every step, do Task 1 Step 1 and Task 2 Step 1 together, then compile once.)

- [x] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/RunSummaryCard.kt
git commit -m "feat: 4:5 photo-hero run-summary card with bottom-right info chip"
```

---

## Task 2: Update RunSummarySheet to the new card signature

**Files:**
- Modify: `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/RunSummarySheet.kt`

- [x] **Step 1: Recompute `hasImage` (drop the merged `cardImage`)**

In `RunSummarySheet.kt`, find:

```kotlin
    // What the card shows: a captured photo wins over the route snapshot.
    val cardImage = photo ?: snapshot
    val hasImage = cardImage != null
```

Replace with:

```kotlin
    // True when there is anything to render/post — a photo or the route snapshot.
    val hasImage = photo != null || snapshot != null
```

- [x] **Step 2: Pass `photo` + `routeSnapshot` to the card**

In `RunSummarySheet.kt`, find the `RunSummaryCard(...)` call:

```kotlin
            RunSummaryCard(
                snapshot = cardImage,
                snapshotLoading = snapshotLoading && photo == null,
                timeText = timeText,
                distanceText = distanceText,
                paceText = paceText,
                modifier = Modifier.fillMaxWidth(),
                captureLayer = captureLayer,
                isPhoto = photo != null,
            )
```

Replace with:

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

No other changes: the camera launcher, `photo`/`pendingPhotoFile` state, the Take photo / Retake / "Use route map" buttons, the `includePhoto` switch, the caption field, and the Post path (`captureLayer.toImageBitmap().asAndroidBitmap()`) are unchanged. The "Use route map" button still gates on `photo != null && snapshot != null`, and the `includePhoto` switch still gates on `hasImage`.

- [x] **Step 3: Compile**

Run:
```bash
export JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`.

- [x] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/RunSummarySheet.kt
git commit -m "feat: pass photo + route snapshot separately to RunSummaryCard"
```

---

## Task 3: Make the profile gallery tiles 4:5

**Files:**
- Modify: `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/ProfileScreen.kt`

- [x] **Step 1: Switch the grid tiles from square to 4:5**

In `ProfileScreen.kt`, the gallery grid uses `Modifier.aspectRatio(1f)` in two places inside the `gallery.chunked(3).forEach { row -> ... }` block: the image tile `Box` and the trailing filler `Box`:

```kotlin
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .combinedClickable(
```

and

```kotlin
                                repeat(3 - row.size) {
                                    Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                                }
```

Change **both** occurrences of `.aspectRatio(1f)` to `.aspectRatio(4f / 5f)`. (There are exactly two `aspectRatio(1f)` uses in this file, both in this grid; replacing all occurrences is safe.)

Resulting tile:

```kotlin
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(4f / 5f)
                                            .combinedClickable(
```

Resulting filler:

```kotlin
                                repeat(3 - row.size) {
                                    Box(modifier = Modifier.weight(1f).aspectRatio(4f / 5f))
                                }
```

- [x] **Step 2: Compile**

Run:
```bash
export JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`.

- [x] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/ProfileScreen.kt
git commit -m "feat: profile gallery tiles 4:5 to match the reel card"
```

---

## Task 4: Manual on-device verification

No code changes — install the debug build and confirm the behaviour.

- [ ] **Step 1: Install and launch**

Run:
```bash
export JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
./gradlew installDebug
```
Expected: `BUILD SUCCESSFUL`, app installs.

- [ ] **Step 2: Route-only summary card**

Record a run, tap End. The summary card is a 4:5 frame with the **route map as the hero**, ENERVA top-left, the stats chip bottom-right (no route thumbnail inside it), and the pace legend bottom-left.

- [ ] **Step 3: Photo-hero summary card**

Tap **Take photo** and capture one. The **photo becomes the hero**; the bottom-right chip now shows the **mini route-map thumbnail** beside ENERVA; the standalone pace legend is gone. Tap **Use route map** to confirm it reverts to Step 2's look.

- [ ] **Step 4: Post + feed consistency**

Post the run. Open the Gallery reel feed: the whole 4:5 card is visible with **no side clipping** (DURATION/DISTANCE/PACE all readable).

- [ ] **Step 5: Profile grid consistency**

Open your Profile. The gallery tiles are **4:5** (taller than before) and the posted card shows whole, matching the feed. Long-press still starts multi-select; the check overlay still sits top-right on each tile.

- [ ] **Step 6: Final commit (if any verification fixes were needed)**

If Steps 2–5 surfaced fixes, commit them with a clear message. Otherwise nothing to commit.

---

## Self-review notes (for the executor)

- Tasks 1 and 2 are a matched pair: after Task 1 alone the build is intentionally red (old call site), and Task 2 makes it green. If you want every commit to build, apply both Step 1 edits before compiling.
- The route snapshot stays square (1000×1000); it is cropped into the 4:5 hero and into the 40dp chip thumbnail — no change to `captureRouteSnapshot`.
- `PaceLegend` is carried over unchanged; `SummaryStatTile` and `FooterDivider` from the old card are intentionally removed (no remaining callers).
- No DB/Firestore/model changes; run posts keep `isCard = true`, so the reel feed continues to render cards with `ContentScale.Fit`.
