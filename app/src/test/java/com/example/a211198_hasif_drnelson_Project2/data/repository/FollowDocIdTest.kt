package com.example.a211198_hasif_drnelson_Project2.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Display names and route titles are used directly as Firestore document ids
 * (owner-scoped sub-collections). Firestore forbids '/' in a document id and
 * rejects an empty id, so [toFollowDocId] sanitises both cases.
 */
class FollowDocIdTest {

    @Test
    fun `leaves a plain name unchanged`() {
        assertEquals("Liyana Rahman", "Liyana Rahman".toFollowDocId())
    }

    @Test
    fun `replaces forward slashes with underscores`() {
        assertEquals("trail_loop", "trail/loop".toFollowDocId())
    }

    @Test
    fun `replaces every forward slash`() {
        assertEquals("a_b_c", "a/b/c".toFollowDocId())
    }

    @Test
    fun `falls back to underscore for a blank id`() {
        assertEquals("_", "".toFollowDocId())
    }
}
