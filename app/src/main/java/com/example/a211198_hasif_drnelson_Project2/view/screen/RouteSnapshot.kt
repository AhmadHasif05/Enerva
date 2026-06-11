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
