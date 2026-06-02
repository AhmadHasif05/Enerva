package com.example.a211198_hasif_drnelson_Project2.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.a211198_hasif_drnelson_Project2.data.entities.ConversationEntity
import com.example.a211198_hasif_drnelson_Project2.data.entities.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConversation(conversation: ConversationEntity)

    @Query("DELETE FROM conversations WHERE ownerEmail = :ownerEmail AND friendName = :friendName")
    suspend fun deleteConversation(ownerEmail: String, friendName: String)

    @Query("DELETE FROM messages WHERE ownerEmail = :ownerEmail AND friendName = :friendName")
    suspend fun deleteMessagesFor(ownerEmail: String, friendName: String)

    @Query("SELECT * FROM conversations WHERE ownerEmail = :ownerEmail")
    fun observeConversations(ownerEmail: String): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE ownerEmail = :ownerEmail AND friendName = :friendName LIMIT 1")
    suspend fun findConversation(ownerEmail: String, friendName: String): ConversationEntity?

    // Used by the Firestore sync layer to map a shared cloud conversation id back
    // to this user's local conversation row (regardless of the friendName key).
    @Query("SELECT * FROM conversations WHERE conversationId = :conversationId LIMIT 1")
    suspend fun findConversationByCloudId(conversationId: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("SELECT * FROM messages WHERE ownerEmail = :ownerEmail AND friendName = :friendName ORDER BY timestampMs ASC")
    fun observeMessages(ownerEmail: String, friendName: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE ownerEmail = :ownerEmail")
    fun observeAllMessages(ownerEmail: String): Flow<List<MessageEntity>>

    // Display-name propagation: when a user renames themselves, update the rows
    // where OTHER users reference them by name (their 1:1 conversation key).
    @Query("UPDATE conversations SET friendName = :newName WHERE friendName = :oldName")
    suspend fun renameConversationFriend(oldName: String, newName: String)

    @Query("UPDATE messages SET friendName = :newName WHERE friendName = :oldName")
    suspend fun renameMessageFriend(oldName: String, newName: String)
}
