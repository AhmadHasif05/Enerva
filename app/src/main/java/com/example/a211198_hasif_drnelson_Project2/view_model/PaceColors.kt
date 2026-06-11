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
