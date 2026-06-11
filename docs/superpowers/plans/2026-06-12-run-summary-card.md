# Branded Run-Summary Card Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the plain End-of-run summary with a branded "ENERVA" card — pace-coloured route map + START/FINISH markers + icon stats footer — that is shown on screen AND rendered to the bitmap posted to the gallery feed; and make the decorative Walk chip a working Walk↔Run toggle.

**Architecture:** One Compose `RunSummaryCard` composable is the single source of truth: the sheet displays it and Post captures it via `GraphicsLayer.toImageBitmap()`. MapLibre's snapshotter renders only the map portion, using a `lineGradient` driven by a pure, unit-tested pace→colour function; Compose owns the header/footer chrome and the final capture.

**Tech Stack:** Kotlin, Jetpack Compose (BOM 2024.09 / Compose 1.7), Material 3, MapLibre Native (`org.maplibre.android`), Room (existing `saveActivity` flow), JUnit4 (JVM unit tests).

---

## File Structure

- **Create** `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view_model/PaceColors.kt` — pure pace→colour ramp (no Android deps; JVM-testable).
- **Create** `app/src/test/java/com/example/a211198_hasif_drnelson_Project2/view_model/PaceColorsTest.kt` — unit tests for the ramp.
- **Create** `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/RunSummaryCard.kt` — branded card composable.
- **Modify** `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/RouteSnapshot.kt` — pace gradient + START/FINISH markers.
- **Modify** `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/RunSummarySheet.kt` — use the card; capture-to-bitmap on Post.
- **Modify** `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view_model/RecordViewModel.kt` — `activityType` state + toggle.
- **Modify** `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/RecordScreen.kt` — wire the toggle, the type label, and pass pace colours into the snapshot call.

## Build / Test environment (run before every gradle command)

```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
cd "C:/Users/hasif/AndroidStudioProjects/a211198_Hasif_DrNelson_Project2"
```

- Unit tests: `./gradlew testDebugUnitTest --tests "<pattern>"`
- Compile check: `./gradlew compileDebugKotlin`

---

### Task 1: Pure pace → colour ramp

**Files:**
- Create: `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view_model/PaceColors.kt`
- Test: `app/src/test/java/com/example/a211198_hasif_drnelson_Project2/view_model/PaceColorsTest.kt`

- [ ] **Step 1: Write the failing test**

Create `PaceColorsTest.kt`:

```kotlin
package com.example.a211198_hasif_drnelson_Project2.view_model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaceColorsTest {

    @Test
    fun `ramp endpoints are green at fastest and red at slowest`() {
        assertEquals("#4CAF50", paceColorAt(0.0))
        assertEquals("#FC4C02", paceColorAt(1.0))
    }

    @Test
    fun `ramp clamps out-of-range input`() {
        assertEquals("#4CAF50", paceColorAt(-1.0))
        assertEquals("#FC4C02", paceColorAt(2.0))
    }

    @Test
    fun `fewer than two points yields no segment colours`() {
        assertTrue(paceSegmentColors(emptyList()).isEmpty())
        assertTrue(paceSegmentColors(listOf(TrackPoint(1.0, 103.0, 0L))).isEmpty())
    }

    @Test
    fun `faster segment is green, slower segment is red`() {
        // Same spatial step (~11 m at this latitude) but seg0 takes 1s, seg1 takes 10s.
        val points = listOf(
            TrackPoint(1.0000, 103.0, 0L),
            TrackPoint(1.0001, 103.0, 1_000L),
            TrackPoint(1.0002, 103.0, 11_000L),
        )
        val colors = paceSegmentColors(points)
        assertEquals(2, colors.size)
        assertEquals("#4CAF50", colors[0]) // fastest -> green
        assertEquals("#FC4C02", colors[1]) // slowest -> red
    }

    @Test
    fun `constant speed falls back to the brand colour`() {
        val points = listOf(
            TrackPoint(1.0000, 103.0, 0L),
            TrackPoint(1.0001, 103.0, 1_000L),
            TrackPoint(1.0002, 103.0, 2_000L),
        )
        val colors = paceSegmentColors(points)
        assertEquals(2, colors.size)
        assertTrue(colors.all { it == "#FC4C02" })
    }

    @Test
    fun `non-increasing timestamps do not crash`() {
        val points = listOf(
            TrackPoint(1.0000, 103.0, 5_000L),
            TrackPoint(1.0001, 103.0, 5_000L), // zero dt
            TrackPoint(1.0002, 103.0, 4_000L), // negative dt
        )
        val colors = paceSegmentColors(points)
        assertEquals(2, colors.size) // produced a colour per segment, no exception
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.example.a211198_hasif_drnelson_Project2.view_model.PaceColorsTest"`
Expected: FAIL — unresolved reference `paceColorAt` / `paceSegmentColors`.

- [ ] **Step 3: Write minimal implementation**

Create `PaceColors.kt`:

```kotlin
package com.example.a211198_hasif_drnelson_Project2.view_model

import kotlin.math.min

// Pure pace → colour mapping for the run-summary route. No Android dependencies,
// so it is unit-testable on the JVM (like RouteAccumulator/RunStats). Colours are
// returned as "#RRGGBB" strings so both MapLibre (lineGradient) and the snapshot
// fallback can consume them directly.

// The fast→slow ramp: green → amber → orange → Strava red.
private val PACE_RAMP = listOf(
    Triple(0x4C, 0xAF, 0x50), // green  (fastest)
    Triple(0xFF, 0xC1, 0x07), // amber
    Triple(0xFF, 0x70, 0x43), // orange
    Triple(0xFC, 0x4C, 0x02), // red    (slowest) — matches StravaOrange
)

// Shown when a run has no usable pace variation (single segment / constant speed).
private const val BRAND_HEX = "#FC4C02"

// Map t in [0,1] (0 = fastest → green, 1 = slowest → red) to a ramp colour.
fun paceColorAt(t: Double): String {
    val tc = t.coerceIn(0.0, 1.0)
    val scaled = tc * (PACE_RAMP.size - 1) // 0..3
    val i = min(scaled.toInt(), PACE_RAMP.size - 2)
    val localT = scaled - i
    val (r1, g1, b1) = PACE_RAMP[i]
    val (r2, g2, b2) = PACE_RAMP[i + 1]
    val r = (r1 + (r2 - r1) * localT).toInt()
    val g = (g1 + (g2 - g1) * localT).toInt()
    val b = (b1 + (b2 - b1) * localT).toInt()
    return "#%02X%02X%02X".format(r, g, b)
}

// One colour per segment (size = points.size - 1), coloured by each segment's
// speed relative to the run's own min/max. Empty when there is no segment.
fun paceSegmentColors(points: List<TrackPoint>): List<String> {
    if (points.size < 2) return emptyList()

    // speed = distanceKm / dtMs; null where dt is non-positive (clock glitch).
    val speeds = (0 until points.size - 1).map { i ->
        val a = points[i]
        val b = points[i + 1]
        val dtMs = b.timeMs - a.timeMs
        if (dtMs <= 0L) null
        else haversineKm(a.lat, a.lng, b.lat, b.lng) / dtMs
    }

    val valid = speeds.filterNotNull()
    val minS = valid.minOrNull()
    val maxS = valid.maxOrNull()

    // No usable variation → a single recognisable brand-coloured trail.
    if (minS == null || maxS == null || maxS <= minS) {
        return List(speeds.size) { BRAND_HEX }
    }

    return speeds.map { s ->
        if (s == null) paceColorAt(0.5)
        else paceColorAt((maxS - s) / (maxS - minS)) // fastest→0(green), slowest→1(red)
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.example.a211198_hasif_drnelson_Project2.view_model.PaceColorsTest"`
Expected: PASS (6 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view_model/PaceColors.kt \
        app/src/test/java/com/example/a211198_hasif_drnelson_Project2/view_model/PaceColorsTest.kt
git commit -m "P5: pure pace->colour ramp for the run-summary route"
```

---

### Task 2: Pace-coloured snapshot + START/FINISH markers

**Files:**
- Modify: `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/RouteSnapshot.kt`

Goal: `captureRouteSnapshot` accepts a per-segment colour list. When present, the route is drawn as a `lineGradient`; when empty, it falls back to today's single-colour line. Two endpoint markers (green START, red FINISH) are added in both cases.

- [ ] **Step 1: Add the colours parameter and gradient/marker rendering**

Replace the body of `captureRouteSnapshot` in `RouteSnapshot.kt`. The new signature adds `segmentColors: List<String> = emptyList()` (defaulted, so existing callers still compile) and new imports. Full updated file region:

```kotlin
package com.example.a211198_hasif_drnelson_Project2.view.screen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import com.example.a211198_hasif_drnelson_Project2.view_model.TrackPoint
import com.example.a211198_hasif_drnelson_Project2.view_model.haversineKm
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.Style
import org.maplibre.android.snapshotter.MapSnapshotter
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonOptions
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import java.io.File
import java.io.FileOutputStream

// Render the recorded route over the basemap into a Bitmap, off-screen.
// onResult is called on the main thread with the bitmap, or null on failure /
// too few points. colorHex is the fallback single-colour trail ("#RRGGBB").
// segmentColors (size = points.size - 1, from paceSegmentColors) paints the line
// by pace when provided; empty falls back to the single colour.
// Returns the MapSnapshotter so the caller can .cancel() if the screen is
// dismissed before the callback fires; the caller owns the snapshotter lifecycle.
fun captureRouteSnapshot(
    context: Context,
    points: List<TrackPoint>,
    styleUrl: String,
    colorHex: String,
    segmentColors: List<String> = emptyList(),
    widthPx: Int = 1000,
    heightPx: Int = 1000,
    onResult: (Bitmap?) -> Unit,
): MapSnapshotter? {
    if (points.size < 2) {
        onResult(null)
        return null
    }

    val latLngs = points.map { LatLng(it.lat, it.lng) }
    val bounds = LatLngBounds.Builder().includes(latLngs).build()

    val line = LineString.fromLngLats(points.map { Point.fromLngLat(it.lng, it.lat) })
    // lineGradient requires line-distance metrics on the source.
    val routeSource = GeoJsonSource("route-src", line, GeoJsonOptions().withLineMetrics(true))

    val useGradient = segmentColors.size == points.size - 1
    val routeLayer = LineLayer("route-layer", "route-src").withProperties(
        if (useGradient)
            PropertyFactory.lineGradient(buildPaceGradient(points, segmentColors))
        else
            PropertyFactory.lineColor(colorHex),
        PropertyFactory.lineWidth(6f),
        PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
        PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
    )

    // START (green) + FINISH (red) endpoint dots.
    val first = points.first()
    val last = points.last()
    val endpoints = FeatureCollection.fromFeatures(
        listOf(
            Feature.fromGeometry(Point.fromLngLat(first.lng, first.lat)).apply {
                addStringProperty("role", "start")
            },
            Feature.fromGeometry(Point.fromLngLat(last.lng, last.lat)).apply {
                addStringProperty("role", "finish")
            },
        )
    )
    val endpointSource = GeoJsonSource("endpoints-src", endpoints)
    val endpointLayer = CircleLayer("endpoints-layer", "endpoints-src").withProperties(
        PropertyFactory.circleRadius(7f),
        PropertyFactory.circleColor(
            Expression.match(
                Expression.get("role"),
                Expression.literal("start"), Expression.color(AndroidColor.parseColor("#2E7D32")),
                Expression.literal("finish"), Expression.color(AndroidColor.parseColor("#C62828")),
                Expression.color(AndroidColor.parseColor(colorHex)),
            )
        ),
        PropertyFactory.circleStrokeColor("#FFFFFF"),
        PropertyFactory.circleStrokeWidth(2.5f),
    )

    val options = MapSnapshotter.Options(widthPx, heightPx)
        .withRegion(bounds)
        .withStyleBuilder(
            Style.Builder()
                .fromUri(styleUrl)
                .withSource(routeSource)
                .withLayer(routeLayer)
                .withSource(endpointSource)
                .withLayer(endpointLayer)
        )

    val snapshotter = MapSnapshotter(context, options)
    snapshotter.start(
        { snapshot -> onResult(snapshot.bitmap) },
        { error -> android.util.Log.w("RouteSnapshot", "snapshot failed: $error"); onResult(null) },
    )
    return snapshotter
}

// Build a lineGradient expression: at each vertex's cumulative fraction along the
// line, use that segment's pace colour. Stops are strictly increasing 0.0 → 1.0.
private fun buildPaceGradient(points: List<TrackPoint>, segmentColors: List<String>): Expression {
    val cum = DoubleArray(points.size)
    for (i in 1 until points.size) {
        cum[i] = cum[i - 1] + haversineKm(
            points[i - 1].lat, points[i - 1].lng, points[i].lat, points[i].lng
        )
    }
    val total = cum.last().takeIf { it > 0.0 } ?: 1.0

    val stops = ArrayList<Expression.Stop>()
    var lastFrac = -1.0
    // vertex i (0..n-2) starts segment i → colour segmentColors[i]
    for (i in 0 until points.size - 1) {
        val frac = (cum[i] / total).coerceIn(0.0, 1.0)
        if (frac > lastFrac) {
            stops.add(Expression.stop(frac, Expression.color(AndroidColor.parseColor(segmentColors[i]))))
            lastFrac = frac
        }
    }
    // final vertex at 1.0 keeps the last segment colour
    if (1.0 > lastFrac) {
        stops.add(Expression.stop(1.0, Expression.color(AndroidColor.parseColor(segmentColors.last()))))
    }

    return Expression.interpolate(
        Expression.linear(),
        Expression.lineProgress(),
        *stops.toTypedArray()
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

- [ ] **Step 2: Compile**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL. (If `GeoJsonSource(name, geometry, options)` constructor resolution fails, use `GeoJsonSource("route-src", line).also { }` is NOT valid — instead keep the 3-arg form which exists in MapLibre Android 10+. If the `Expression.Stop` vararg overload is unresolved, the alternative is `Expression.interpolate(Expression.linear(), Expression.lineProgress(), *stops.toTypedArray())` already used here.)

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/RouteSnapshot.kt
git commit -m "P5: snapshot paints route by pace + START/FINISH markers"
```

---

### Task 3: `RunSummaryCard` composable

**Files:**
- Create: `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/RunSummaryCard.kt`

The card = ENERVA header (solid StravaOrange) + map image (with FAST→SLOW legend) + dark icon stats footer. It takes an optional `GraphicsLayer` so the sheet can capture it.

- [ ] **Step 1: Write the composable**

Create `RunSummaryCard.kt`:

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

// The branded run-summary card: ENERVA header + pace-coloured route map + stats
// footer. Used both on screen (in RunSummarySheet) and, via `captureLayer`,
// rendered to the bitmap posted to the gallery. Pure presentation — no logic.
@Composable
fun RunSummaryCard(
    snapshot: Bitmap?,
    snapshotLoading: Boolean,
    timeText: String,
    distanceText: String,
    paceText: String,
    modifier: Modifier = Modifier,
    captureLayer: GraphicsLayer? = null,
) {
    val colors = MaterialTheme.colorScheme

    // When a capture layer is supplied, record this card's drawing into it so the
    // sheet can export the exact on-screen card as a bitmap.
    val captureModifier = if (captureLayer != null) {
        Modifier.drawWithContent {
            captureLayer.record { this@drawWithContent.drawContent() }
            drawLayer(captureLayer)
        }
    } else Modifier

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(colors.background)
            .then(captureModifier)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.primary)
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "ENERVA",
                color = colors.onPrimary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 26.sp,
                letterSpacing = 3.sp
            )
        }

        // Map
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
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
            if (snapshot != null) {
                PaceLegend(modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp))
            }
        }

        // Stats footer
        Row(modifier = Modifier.fillMaxWidth().background(colors.surface)) {
            SummaryStatTile(Icons.Filled.Timer, "DURATION", timeText, Modifier.weight(1f))
            FooterDivider()
            SummaryStatTile(Icons.Filled.Place, "DISTANCE", distanceText, Modifier.weight(1f))
            FooterDivider()
            SummaryStatTile(Icons.Filled.Speed, "PACE", paceText, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryStatTile(icon: ImageVector, label: String, value: String, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier.padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = colors.onSurface, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, color = colors.onSurfaceVariant, fontSize = 9.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(2.dp))
        Text(value, color = colors.onSurface, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun FooterDivider() {
    Box(
        modifier = Modifier
            .padding(vertical = 12.dp)
            .width(1.dp)
            .height(40.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
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
                    Brush.horizontalGradient(
                        listOf(Color(0xFF4CAF50), Color(0xFFFFC107), Color(0xFFFC4C02))
                    )
                )
        )
        Text("SLOW", color = colors.onSurfaceVariant, fontSize = 8.sp)
    }
}
```

- [ ] **Step 2: Compile**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL. (`drawLayer`, `record`, `GraphicsLayer` are in `androidx.compose.ui.graphics.layer` — available in Compose 1.7 / BOM 2024.09.)

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/RunSummaryCard.kt
git commit -m "P5: branded RunSummaryCard composable (header + map + stats footer)"
```

---

### Task 4: Restyle `RunSummarySheet` + capture the card to a bitmap

**Files:**
- Modify: `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/RunSummarySheet.kt`

The sheet swaps its inline map/stats for `RunSummaryCard`, keeps caption/toggle/buttons, and on Post captures the card via the shared `GraphicsLayer` (so the posted image is the branded card, not the bare map). The `onPost` contract changes from `(caption, includePhoto)` to `(caption, cardBitmap)` so the sheet — which owns the layer — does the capture.

- [ ] **Step 1: Rewrite the sheet**

Replace the whole contents of `RunSummarySheet.kt`:

```kotlin
package com.example.a211198_hasif_drnelson_Project2.view.screen

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// Bottom-sheet summary shown when the user taps End. Shows the branded
// RunSummaryCard (the same composition that gets posted), a caption field, an
// "include photo" toggle, and Post / Discard. On Post, the card is captured to a
// Bitmap and handed back so the gallery reel shows the branded card.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunSummarySheet(
    snapshot: Bitmap?,
    snapshotLoading: Boolean,
    timeText: String,
    distanceText: String,
    paceText: String,
    onPost: (caption: String, cardBitmap: Bitmap?) -> Unit,
    onDiscard: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val captureLayer: GraphicsLayer = rememberGraphicsLayer()
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
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RunSummaryCard(
                snapshot = snapshot,
                snapshotLoading = snapshotLoading,
                timeText = timeText,
                distanceText = distanceText,
                paceText = paceText,
                modifier = Modifier.fillMaxWidth(),
                captureLayer = captureLayer,
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it },
                label = { Text("Caption") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
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
                    onClick = {
                        val wantPhoto = includePhoto && snapshot != null
                        if (wantPhoto) {
                            scope.launch {
                                val bmp = captureLayer.toImageBitmap().asAndroidBitmap()
                                onPost(caption, bmp)
                            }
                        } else {
                            onPost(caption, null)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Post")
                }
            }
        }
    }
}
```

- [ ] **Step 2: Compile (expected to fail at the call site)**

Run: `./gradlew compileDebugKotlin`
Expected: FAIL in `RecordScreen.kt` — `onPost` lambda signature no longer matches (it still references `includePhoto`). Fixed in Task 6. (If you are running tasks in order, you may defer this compile to Task 6; it is listed here so the change set is explicit.)

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/RunSummarySheet.kt
git commit -m "P5: RunSummarySheet uses RunSummaryCard; Post captures the card bitmap"
```

---

### Task 5: Activity-type toggle in the ViewModel

**Files:**
- Modify: `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view_model/RecordViewModel.kt`

- [ ] **Step 1: Add `activityType` state + toggle**

In `RecordViewModel.kt`, add the state right after the `distanceKm` declaration (after line 48, before `val path`):

```kotlin
    // Selected activity type for this run ("Run" or "Walk"); drives the chip,
    // the stats header, and the type stored on save. "Jog" is treated as "Run".
    var activityType by mutableStateOf("Run")
        private set

    fun toggleActivityType() {
        activityType = if (activityType == "Run") "Walk" else "Run"
    }
```

Then reset it in `reset()` — add this line inside `reset()` (e.g. after `path.clear()`):

```kotlin
        activityType = "Run"
```

- [ ] **Step 2: Compile**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL (the new state is not yet consumed; that is Task 6).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view_model/RecordViewModel.kt
git commit -m "P5: RecordViewModel holds toggleable activityType (Run/Walk)"
```

---

### Task 6: Wire RecordScreen — toggle, type label, pace colours, new onPost

**Files:**
- Modify: `app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/RecordScreen.kt`

- [ ] **Step 1: Import the pace-colour helper**

Add to the imports block (near the other `view_model` imports around line 33-36):

```kotlin
import com.example.a211198_hasif_drnelson_Project2.view_model.paceSegmentColors
```

- [ ] **Step 2: Make the stats-header label reflect the type**

Replace line 242:

```kotlin
                        Text("Walk", color = colors.onSurface, fontWeight = FontWeight.Bold)
```

with:

```kotlin
                        Text(recordViewModel.activityType, color = colors.onSurface, fontWeight = FontWeight.Bold)
```

- [ ] **Step 3: Make the bottom chip a working toggle**

Replace the activity chip `Column` (lines 352-371, the `Column` that contains the `DirectionsWalk` `Surface` and the `Text("Walk", …)`) with:

```kotlin
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = colors.primary.copy(alpha = 0.2f),
                            modifier = Modifier.size(56.dp),
                            onClick = { recordViewModel.toggleActivityType() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    if (recordViewModel.activityType == "Run")
                                        Icons.AutoMirrored.Filled.DirectionsRun
                                    else
                                        Icons.AutoMirrored.Filled.DirectionsWalk,
                                    contentDescription = "Activity type: ${recordViewModel.activityType}",
                                    tint = colors.primary
                                )
                            }
                        }
                        Text(
                            recordViewModel.activityType,
                            color = colors.onSurface,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
```

- [ ] **Step 4: Pass pace colours into the snapshot call**

In the End button `onClick` (lines 411-419), update the `captureRouteSnapshot(...)` call to pass `segmentColors`:

```kotlin
                                    snapshotter = captureRouteSnapshot(
                                        context = context,
                                        points = recordViewModel.path,
                                        styleUrl = styleUrls[styleIndex],
                                        colorHex = trailColorHex,
                                        segmentColors = paceSegmentColors(recordViewModel.path),
                                    ) { bmp ->
                                        snapshot = bmp
                                        snapshotLoading = false
                                    }
```

- [ ] **Step 5: Update the `RunSummarySheet` call to the new `onPost` contract**

Replace the `onPost` lambda (lines 305-315) with:

```kotlin
                onPost = { caption, cardBitmap ->
                    val uri = cardBitmap?.let { saveBitmapToInternalStorage(context, it) }
                    recordViewModel.saveActivity(
                        type = recordViewModel.activityType,
                        caption = caption,
                        imageUri = uri,
                    )
                    snapshotter?.cancel()
                    snapshotter = null
                    showSummary = false
                    snapshot = null
                    recordViewModel.reset()
                },
```

- [ ] **Step 6: Compile the whole app**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Run the full unit-test suite**

Run: `./gradlew testDebugUnitTest`
Expected: PASS (existing suite + the 6 new `PaceColorsTest` cases).

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/example/a211198_hasif_drnelson_Project2/view/screen/RecordScreen.kt
git commit -m "P5: wire Walk/Run toggle, type label, and pace-coloured snapshot"
```

---

### Task 7: Build the APK + update the roadmap

**Files:**
- Modify: `plan.md` (roadmap)

- [ ] **Step 1: Full debug build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Manual on-device verification (real phone — emulators give a fixed GPS point)**

Walk a short loop with a deliberately fast stretch and a slow stretch, then tap End. Confirm:
- The summary card shows the **ENERVA** header, the route coloured green↔red along the fast/slow stretches, green START + red FINISH dots, the FAST→SLOW legend, and correct Duration / Distance / Pace.
- Before End, the bottom **chip toggles Walk↔Run** (icon + label change), and the live stats-card header shows the selected type.
- **Post** → the run appears in Gallery reels showing the **branded card image** (not a bare map).
- Toggle "Include route photo" **OFF** → Post still works, reel uses the drawable fallback.
- **Discard** → nothing is saved.

- [ ] **Step 3: Update the roadmap**

In `plan.md`, under the **Phase 5 — Camera in Record** section, add a line recording this polish (the card + pace route + Walk/Run toggle shipped ahead of the camera work). Example addition after the Phase 5 list:

```markdown
> **P5 polish (shipped first):** End → branded ENERVA run-summary card — pace-coloured
> route (green=fast … red=slow) + START/FINISH markers + icon stats footer, captured to
> the posted reel image; Record activity-type chip is now a working Walk↔Run toggle.
> Spec: docs/superpowers/specs/2026-06-12-run-summary-card-design.md ·
> Plan: docs/superpowers/plans/2026-06-12-run-summary-card.md
```

- [ ] **Step 4: Commit**

```bash
git add plan.md
git commit -m "P5: record run-summary-card polish in the roadmap"
```

---

## Self-Review

**Spec coverage:**
- Branded card (header + pace map + stats footer) → Task 3. ✓
- Pace-coloured route from `timeMs` → Task 1 (logic) + Task 2 (render) + Task 6 (wiring). ✓
- Same card shown on screen AND posted (GraphicsLayer capture) → Task 3 (`captureLayer`) + Task 4 (capture on Post) + Task 6 (save). ✓
- Keep caption / include-photo toggle / Post / Discard + existing `saveActivity` → Task 4 + Task 6. ✓
- START/FINISH markers + FAST→SLOW legend → Task 2 (markers) + Task 3 (legend). ✓
- Walk↔Run/Jog toggle (chip onClick, header label, `saveActivity(type)`) → Task 5 + Task 6. ✓
- No new deps / no DB migration / no gallery changes → confirmed (uses existing `MediaEntity.imageUri` + Coil path render). ✓

**Placeholder scan:** No TBD/TODO; every code step shows complete code.

**Type consistency:** `paceSegmentColors(List<TrackPoint>): List<String>` (Task 1) is consumed in Task 6 and passed to `captureRouteSnapshot(..., segmentColors: List<String>)` (Task 2). `onPost: (String, Bitmap?)` defined in Task 4 matches the call site rewritten in Task 6. `activityType` / `toggleActivityType()` defined in Task 5 used in Task 6. Card param names (`snapshot`, `snapshotLoading`, `timeText`, `distanceText`, `paceText`, `captureLayer`) match between Task 3 and Task 4.
