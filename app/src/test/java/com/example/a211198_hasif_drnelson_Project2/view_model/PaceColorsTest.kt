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
