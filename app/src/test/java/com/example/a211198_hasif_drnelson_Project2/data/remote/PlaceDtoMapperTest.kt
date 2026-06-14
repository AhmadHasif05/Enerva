package com.example.a211198_hasif_drnelson_Project2.data.remote

import com.example.a211198_hasif_drnelson_Project2.data.repository.toRunRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaceDtoMapperTest {

    @Test fun photoUrl_concatenates_prefix_size_suffix() {
        assertEquals(
            "https://fastly.4sqi.net/img/general/400x300/abc.jpg",
            photoUrl("https://fastly.4sqi.net/img/general/", "/abc.jpg", "400x300")
        )
    }

    @Test fun formatDistance_under_1km_shows_meters() {
        assertEquals("800 m away", formatDistance(800))
    }

    @Test fun formatDistance_at_or_over_1km_shows_one_decimal_km() {
        assertEquals("1.2 km away", formatDistance(1200))
        assertEquals("5.0 km away", formatDistance(5000))
    }

    @Test fun firstPhotoUrl_null_when_no_photos() {
        val dto = PlaceDto(name = "X", distance = 100, categories = emptyList(), photos = emptyList())
        assertNull(dto.firstPhotoUrl())
    }

    @Test fun firstPhotoUrl_builds_url_from_first_photo() {
        val dto = PlaceDto(
            name = "Cafe",
            distance = 500,
            categories = emptyList(),
            photos = listOf(PhotoDto("https://prefix/", "/photo.jpg"))
        )
        assertEquals("https://prefix/400x300/photo.jpg", dto.firstPhotoUrl())
    }

    @Test fun formatDistance_exactly_1km_shows_km() {
        assertEquals("1.0 km away", formatDistance(1000))
    }

    @Test fun toRunRoute_maps_name_distance_category_photo() {
        val dto = PlaceDto(
            name = "Lake Gardens",
            distance = 1200,
            categories = listOf(CategoryDto("Park")),
            photos = listOf(PhotoDto("https://x/", "/p.jpg"))
        )
        val route = dto.toRunRoute()
        assertEquals("Lake Gardens", route.title)
        assertEquals("1.2 km away", route.distance)
        assertEquals("Park", route.difficulty)
        assertEquals("https://x/400x300/p.jpg", route.imageUrl)
        assertEquals("", route.time)
        assertEquals("", route.elevation)
        assertEquals(0, route.imageRes)
    }

    @Test fun toRunRoute_blank_category_falls_back_to_label() {
        val dto = PlaceDto(name = "Trailhead", distance = 300, categories = emptyList(), photos = emptyList())
        val route = dto.toRunRoute()
        assertEquals("Run spot", route.difficulty)
        assertNull(route.imageUrl)
    }
}
