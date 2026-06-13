package com.example.a211198_hasif_drnelson_Project2.data.dao

import androidx.room.Dao                        // marks this as a Room DAO
import androidx.room.Insert                     // generates INSERT code
import androidx.room.OnConflictStrategy         // REPLACE = upsert on key clash
import androidx.room.Query                      // raw SQL on a method
import com.example.a211198_hasif_drnelson_Project2.data.entities.ConversationEntity // a chat-thread row
import com.example.a211198_hasif_drnelson_Project2.data.entities.MessageEntity      // a single message row
import kotlinx.coroutines.flow.Flow             // live stream of query results

// MessageDao — local persistence for conversations and their messages. Rows are
// keyed by (ownerEmail, friendName) so each signed-in user has their own copy.
@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)          // create/update a conversation thread
    suspend fun upsertConversation(conversation: ConversationEntity)

    @Query("DELETE FROM conversations WHERE ownerEmail = :ownerEmail AND friendName = :friendName") // remove a thread
    suspend fun deleteConversation(ownerEmail: String, friendName: String)

    @Query("DELETE FROM messages WHERE ownerEmail = :ownerEmail AND friendName = :friendName") // and its messages
    suspend fun deleteMessagesFor(ownerEmail: String, friendName: String)

    @Query("SELECT * FROM conversations WHERE ownerEmail = :ownerEmail") // live inbox list for this user
    fun observeConversations(ownerEmail: String): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE ownerEmail = :ownerEmail AND friendName = :friendName LIMIT 1") // one thread
    suspend fun findConversation(ownerEmail: String, friendName: String): ConversationEntity?

    // Used by the Firestore sync layer to map a shared cloud conversation id back
    // to this user's local conversation row (regardless of the friendName key).
    @Query("SELECT * FROM conversations WHERE conversationId = :conversationId LIMIT 1")
    suspend fun findConversationByCloudId(conversationId: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)          // add a message (idempotent by message id)
    suspend fun insertMessage(message: MessageEntity)

    @Query("SELECT * FROM messages WHERE ownerEmail = :ownerEmail AND friendName = :friendName ORDER BY timestampMs ASC") // one chat, oldest→newest
    fun observeMessages(ownerEmail: String, friendName: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE ownerEmail = :ownerEmail ORDER BY timestampMs ASC") // all of my messages (for previews)
    fun observeAllMessages(ownerEmail: String): Flow<List<MessageEntity>>

    // Display-name propagation: when a user renames themselves, update the rows
    // where OTHER users reference them by name (their 1:1 conversation key).
    @Query("UPDATE conversations SET friendName = :newName WHERE friendName = :oldName")
    suspend fun renameConversationFriend(oldName: String, newName: String)

    @Query("UPDATE messages SET friendName = :newName WHERE friendName = :oldName") // same rename on message rows
    suspend fun renameMessageFriend(oldName: String, newName: String)
}
