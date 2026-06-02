package com.example.a211198_hasif_drnelson_Project2.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.a211198_hasif_drnelson_Project2.data.entities.ActivityRecordEntity
import com.example.a211198_hasif_drnelson_Project2.data.entities.MediaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityRecordEntity)

    @Query("SELECT * FROM activities WHERE ownerEmail = :ownerEmail ORDER BY id DESC")
    fun observeActivities(ownerEmail: String): Flow<List<ActivityRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: MediaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMedia(media: List<MediaEntity>)

    @Query("SELECT COUNT(*) FROM media WHERE ownerEmail = :ownerEmail")
    suspend fun countMedia(ownerEmail: String): Int

    @Query("SELECT * FROM media WHERE ownerEmail = :ownerEmail ORDER BY createdAtMs DESC")
    fun observeMedia(ownerEmail: String): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media WHERE author = :authorName ORDER BY createdAtMs DESC")
    fun observeMediaByAuthor(authorName: String): Flow<List<MediaEntity>>

    @Query("SELECT COUNT(*) FROM media WHERE author = :authorName")
    suspend fun countMediaByAuthor(authorName: String): Int

    @Query("SELECT * FROM media ORDER BY createdAtMs DESC")
    fun observeAllMedia(): Flow<List<MediaEntity>>

    // Propagate a display-name change to existing reels so the gallery stops
    // showing the old author name after the user renames themselves.
    @Query("UPDATE media SET author = :newName WHERE author = :oldName")
    suspend fun renameAuthor(oldName: String, newName: String)
}
