package com.example.a211198_hasif_drnelson_Project2.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * The 1:1 conversation id is the contract that lets two devices agree on the same
 * Firestore conversation document without a round-trip (plan.md §5.3). It must be
 * deterministic and independent of which participant computes it.
 */
class ConversationIdTest {

    @Test
    fun `is independent of argument order`() {
        assertEquals(
            oneToOneConversationId("alice", "bob"),
            oneToOneConversationId("bob", "alice")
        )
    }

    @Test
    fun `sorts the two uids and joins them with an underscore`() {
        assertEquals("alice_bob", oneToOneConversationId("bob", "alice"))
    }

    @Test
    fun `is deterministic across repeated calls`() {
        val first = oneToOneConversationId("uid-123", "uid-456")
        val second = oneToOneConversationId("uid-456", "uid-123")
        assertEquals(first, second)
    }

    @Test
    fun `distinct uid pairs produce distinct ids`() {
        assertNotEquals(
            oneToOneConversationId("alice", "bob"),
            oneToOneConversationId("alice", "carol")
        )
    }
}
