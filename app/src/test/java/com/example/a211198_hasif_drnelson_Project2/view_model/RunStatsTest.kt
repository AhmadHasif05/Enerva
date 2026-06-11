package com.example.a211198_hasif_drnelson_Project2.view_model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RunStatsTest {

    @Test
    fun `pace formats minutes and seconds per km`() {
        assertEquals("6:12", formatPace(elapsedSeconds = 372, distanceKm = 1.0))
    }

    @Test
    fun `pace pads seconds`() {
        assertEquals("5:00", formatPace(elapsedSeconds = 300, distanceKm = 1.0))
    }

    @Test
    fun `pace is placeholder below the minimum distance`() {
        assertEquals("--:--", formatPace(elapsedSeconds = 30, distanceKm = 0.0))
        assertEquals("--:--", formatPace(elapsedSeconds = 30, distanceKm = 0.005))
    }

    @Test
    fun `buildRunMedia carries caption and image uri`() {
        val media = buildRunMedia(
            id = "m1",
            ownerEmail = "me@example.com",
            caption = "Evening loop",
            type = "Walk",
            distanceKm = 2.345,
            imageUri = "/data/run/snap.png",
            createdAtMs = 1_700_000_000_000L,
        )
        assertEquals("m1", media.id)
        assertEquals("me@example.com", media.ownerEmail)
        assertEquals("You", media.author)
        assertEquals("Evening loop", media.caption)
        assertEquals("Walk", media.activity)
        assertEquals("2.3", media.distanceKm)
        assertEquals("/data/run/snap.png", media.imageUri)
        assertEquals(0, media.likes)
        assertEquals(1_700_000_000_000L, media.createdAtMs)
    }

    @Test
    fun `buildRunMedia allows a null image uri for stats-only posts`() {
        val media = buildRunMedia(
            id = "m2",
            ownerEmail = "me@example.com",
            caption = "No photo",
            type = "Run",
            distanceKm = 1.0,
            imageUri = null,
            createdAtMs = 0L,
        )
        assertNull(media.imageUri)
    }

    @Test
    fun `buildRunRecord captures distance duration and pace`() {
        val record = buildRunRecord(
            id = "r1",
            ownerEmail = "me@example.com",
            caption = "Evening loop",
            type = "Walk",
            distanceKm = 2.0,
            elapsedSeconds = 600,
            dateStr = "Jun 11, 2026",
        )
        assertEquals("r1", record.id)
        assertEquals("Walk", record.type)
        assertEquals("Evening loop", record.title)
        assertEquals("Jun 11, 2026", record.date)
        assertEquals(2.0, record.distanceKm, 1e-9)
        assertEquals(10, record.durationMinutes)
        assertEquals("5:00", record.avgPace)
    }
}
