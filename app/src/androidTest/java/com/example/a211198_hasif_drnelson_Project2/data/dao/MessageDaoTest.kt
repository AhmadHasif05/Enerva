package com.example.a211198_hasif_drnelson_Project2.data.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.a211198_hasif_drnelson_Project2.data.entities.ConversationEntity
import com.example.a211198_hasif_drnelson_Project2.data.entities.MessageEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MessageDaoTest : RoomDaoTest() {

    private lateinit var dao: MessageDao

    @Before
    fun initDao() {
        dao = db.messageDao()
    }

    private fun msg(id: String, friend: String, text: String, ts: Long, owner: String = "me@example.com") =
        MessageEntity(
            id = id,
            ownerEmail = owner,
            friendName = friend,
            fromMe = true,
            text = text,
            timestampMs = ts,
            conversationId = "cid-$friend"
        )

    @Test
    fun upsertConversation_then_findConversation_returns_it() = runTest {
        dao.upsertConversation(ConversationEntity("me@example.com", "Amy", isGroup = false, membersCsv = "", conversationId = "cid-1"))
        assertEquals("cid-1", dao.findConversation("me@example.com", "Amy")?.conversationId)
        assertNull(dao.findConversation("me@example.com", "Nobody"))
    }

    @Test
    fun findConversationByCloudId_maps_shared_id_back_to_local_row() = runTest {
        dao.upsertConversation(ConversationEntity("me@example.com", "Amy", false, "", "shared-cid"))
        assertEquals("Amy", dao.findConversationByCloudId("shared-cid")?.friendName)
    }

    @Test
    fun messages_are_observed_in_timestamp_order() = runTest {
        dao.insertMessage(msg("m2", "Amy", "second", ts = 200))
        dao.insertMessage(msg("m1", "Amy", "first", ts = 100))
        dao.insertMessage(msg("m3", "Amy", "third", ts = 300))

        val texts = dao.observeMessages("me@example.com", "Amy").first().map { it.text }
        assertEquals(listOf("first", "second", "third"), texts)
    }

    @Test
    fun inserting_same_id_replaces_rather_than_duplicates() = runTest {
        // The sync layer relies on this: a Firestore echo of an already-stored
        // message (same id) must be an idempotent upsert, not a duplicate.
        dao.insertMessage(msg("m1", "Amy", "original", ts = 100))
        dao.insertMessage(msg("m1", "Amy", "echoed", ts = 100))

        val all = dao.observeMessages("me@example.com", "Amy").first()
        assertEquals(1, all.size)
        assertEquals("echoed", all.first().text)
    }

    @Test
    fun deleting_a_conversation_drops_its_messages_for_that_owner() = runTest {
        dao.upsertConversation(ConversationEntity("me@example.com", "Amy", false, "", "cid-Amy"))
        dao.insertMessage(msg("m1", "Amy", "hi", ts = 100))

        dao.deleteMessagesFor("me@example.com", "Amy")
        dao.deleteConversation("me@example.com", "Amy")

        assertTrue(dao.observeMessages("me@example.com", "Amy").first().isEmpty())
        assertNull(dao.findConversation("me@example.com", "Amy"))
    }

    @Test
    fun rename_propagates_to_conversation_and_message_rows() = runTest {
        dao.upsertConversation(ConversationEntity("me@example.com", "Old", false, "", "cid-old"))
        dao.insertMessage(msg("m1", "Old", "hi", ts = 100))

        dao.renameConversationFriend("Old", "New")
        dao.renameMessageFriend("Old", "New")

        assertEquals("New", dao.observeConversations("me@example.com").first().single().friendName)
        assertEquals("New", dao.observeMessages("me@example.com", "New").first().single().friendName)
    }
}
