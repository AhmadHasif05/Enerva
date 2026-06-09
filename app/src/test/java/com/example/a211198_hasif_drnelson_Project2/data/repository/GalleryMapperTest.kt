package com.example.a211198_hasif_drnelson_Project2.data.repository

import com.example.a211198_hasif_drnelson_Project2.data.entities.MediaEntity
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Reel mappers bridge the Room entity and the two Firestore shapes (the private
 * `users/{uid}/media` copy and the public `publicReels` copy). The behaviours that
 * matter for cross-device sync (plan.md §3.5, §5.2): stats/text survive the trip,
 * the public copy carries the owner uid, and remote reels land under a synthetic
 * `uid:<ownerUid>` email so they never collide with my own email-keyed posts.
 */
class GalleryMapperTest {

    private fun sampleEntity(
        id: String = "reel-1",
        ownerEmail: String = "me@example.com"
    ) = MediaEntity(
        id = id,
        ownerEmail = ownerEmail,
        author = "Hasif",
        caption = "Morning 5K",
        activity = "Run",
        distanceKm = "5.0",
        tint = 0xFF1E3A5F,
        imageRes = 42,
        imageUri = "content://media/1",
        likes = 3,
        createdAtMs = 1_700_000_000_000L
    )

    @Test
    fun `toDoc carries the reel content`() {
        val doc = sampleEntity().toDoc()
        assertEquals("reel-1", doc.id)
        assertEquals("Hasif", doc.author)
        assertEquals("Morning 5K", doc.caption)
        assertEquals("Run", doc.activity)
        assertEquals("5.0", doc.distanceKm)
        assertEquals(0xFF1E3A5F, doc.tint)
        assertEquals(42, doc.imageRes)
        assertEquals("content://media/1", doc.imageUri)
        assertEquals(3, doc.likes)
        assertEquals(1_700_000_000_000L, doc.createdAtMs)
    }

    @Test
    fun `toPublicReel stamps the owner uid`() {
        val reel = sampleEntity().toPublicReel(ownerUid = "uid-abc")
        assertEquals("uid-abc", reel.ownerUid)
        assertEquals("reel-1", reel.id)
        assertEquals("Morning 5K", reel.caption)
    }

    @Test
    fun `remote public reel lands under a synthetic uid email`() {
        val entity = sampleEntity().toPublicReel(ownerUid = "uid-abc").toEntity()
        assertEquals("uid:uid-abc", entity.ownerEmail)
        assertEquals("reel-1", entity.id)
    }

    @Test
    fun `media doc maps back under the given owner email`() {
        val entity = sampleEntity().toDoc().toEntity(ownerEmail = "me@example.com")
        assertEquals("me@example.com", entity.ownerEmail)
        assertEquals("reel-1", entity.id)
    }

    @Test
    fun `public reel round-trip preserves stats and text`() {
        val original = sampleEntity()
        val restored = original.toPublicReel(ownerUid = "uid-abc").toEntity()
        assertEquals(original.id, restored.id)
        assertEquals(original.author, restored.author)
        assertEquals(original.caption, restored.caption)
        assertEquals(original.activity, restored.activity)
        assertEquals(original.distanceKm, restored.distanceKm)
        assertEquals(original.tint, restored.tint)
        assertEquals(original.likes, restored.likes)
        assertEquals(original.createdAtMs, restored.createdAtMs)
    }
}
