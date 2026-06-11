package com.example.a211198_hasif_drnelson_Project2.view.screen

import android.content.Context
import android.graphics.Bitmap
import com.example.a211198_hasif_drnelson_Project2.view_model.TrackPoint
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.Style
import org.maplibre.android.snapshotter.MapSnapshotter
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import java.io.File
import java.io.FileOutputStream

// Render the recorded route over the basemap into a Bitmap, off-screen.
// onResult is called on the main thread with the bitmap, or null on failure /
// too few points. colorHex is a "#RRGGBB" string (the trail colour).
fun captureRouteSnapshot(
    context: Context,
    points: List<TrackPoint>,
    styleUrl: String,
    colorHex: String,
    widthPx: Int = 1000,
    heightPx: Int = 1000,
    onResult: (Bitmap?) -> Unit,
) {
    if (points.size < 2) {
        onResult(null)
        return
    }

    val latLngs = points.map { LatLng(it.lat, it.lng) }
    val bounds = LatLngBounds.Builder().includes(latLngs).build()

    val line = LineString.fromLngLats(points.map { Point.fromLngLat(it.lng, it.lat) })
    val routeSource = GeoJsonSource("route-src", line)
    val routeLayer = LineLayer("route-layer", "route-src").withProperties(
        PropertyFactory.lineColor(colorHex),
        PropertyFactory.lineWidth(5f),
        PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
        PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
    )

    val options = MapSnapshotter.Options(widthPx, heightPx)
        .withRegion(bounds)
        .withStyleBuilder(
            Style.Builder()
                .fromUri(styleUrl)
                .withSource(routeSource)
                .withLayer(routeLayer)
        )

    val snapshotter = MapSnapshotter(context, options)
    snapshotter.start(
        { snapshot -> onResult(snapshot.bitmap) },
        { _ -> onResult(null) },
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
