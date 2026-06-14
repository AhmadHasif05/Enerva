package com.example.a211198_hasif_drnelson_Project2.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OverpassMapperTest {

    @Test fun formatDistance_under_1km_shows_meters() {
        assertEquals("800 m away", formatDistance(800))
    }

    @Test fun formatDistance_at_or_over_1km_shows_one_decimal_km() {
        assertEquals("1.2 km away", formatDistance(1200))
        assertEquals("5.0 km away", formatDistance(5000))
    }

    @Test fun formatDistance_exactly_1km_shows_km() {
        assertEquals("1.0 km away", formatDistance(1000))
    }

    @Test fun haversine_same_point_is_zero() {
        assertEquals(0, haversineMeters(3.1390, 101.6869, 3.1390, 101.6869))
    }

    @Test fun haversine_roughly_one_km() {
        // ~0.009 degrees of latitude ≈ 1 km; allow generous tolerance.
        val meters = haversineMeters(3.1390, 101.6869, 3.1480, 101.6869)
        assertTrue("expected ~1000m, got $meters", meters in 900..1100)
    }

    @Test fun latLon_uses_node_coordinates() {
        val el = OverpassElement(type = "node", lat = 3.1, lon = 101.6)
        assertEquals(3.1 to 101.6, el.latLon())
    }

    @Test fun latLon_falls_back_to_center_for_ways() {
        val el = OverpassElement(type = "way", center = OverpassCenter(3.2, 101.7))
        assertEquals(3.2 to 101.7, el.latLon())
    }

    @Test fun latLon_null_when_no_coordinates() {
        assertNull(OverpassElement(type = "relation").latLon())
    }

    @Test fun categoryLabel_humanizes_leisure_tag() {
        val el = OverpassElement(tags = mapOf("leisure" to "nature_reserve"))
        assertEquals("Nature reserve", el.categoryLabel())
    }

    @Test fun categoryLabel_falls_back_when_untagged() {
        assertEquals("Run spot", OverpassElement().categoryLabel())
    }

    @Test fun directPhotoUrl_accepts_direct_image_file() {
        val el = OverpassElement(tags = mapOf("image" to "https://x.com/park.jpg"))
        assertEquals("https://x.com/park.jpg", el.directPhotoUrl())
    }

    @Test fun directPhotoUrl_rejects_google_photos_share_link() {
        val el = OverpassElement(tags = mapOf("image" to "https://photos.app.goo.gl/abc"))
        assertNull(el.directPhotoUrl())
    }

    @Test fun directPhotoUrl_builds_commons_url_from_file_tag() {
        val el = OverpassElement(tags = mapOf("wikimedia_commons" to "File:My Park.jpg"))
        assertEquals(
            "https://commons.wikimedia.org/wiki/Special:FilePath/My%20Park.jpg?width=400",
            el.directPhotoUrl()
        )
    }

    @Test fun directPhotoUrl_null_for_commons_category() {
        val el = OverpassElement(tags = mapOf("wikimedia_commons" to "Category:KLCC Park"))
        assertNull(el.directPhotoUrl())
    }

    @Test fun wikidataId_reads_q_id() {
        assertEquals("Q6332465", OverpassElement(tags = mapOf("wikidata" to "Q6332465")).wikidataId())
        assertNull(OverpassElement(tags = mapOf("wikidata" to "bogus")).wikidataId())
    }

    @Test fun imageFileName_reads_p18_value() {
        val entity = WikidataEntity(
            claims = mapOf(
                "P18" to listOf(WikidataClaim(WikidataSnak(WikidataDatavalue("KLCC fountain.jpg"))))
            )
        )
        assertEquals("KLCC fountain.jpg", entity.imageFileName())
    }

    @Test fun imageFileName_null_without_p18() {
        assertNull(WikidataEntity().imageFileName())
    }
}
