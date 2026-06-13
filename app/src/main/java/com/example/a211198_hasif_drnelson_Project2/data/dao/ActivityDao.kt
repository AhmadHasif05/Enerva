package com.example.a211198_hasif_drnelson_Project2.data.dao

import androidx.room.Dao                        // marks this as a Room DAO
import androidx.room.Insert                     // generates INSERT code
import androidx.room.OnConflictStrategy         // REPLACE = upsert on key clash
import androidx.room.Query                      // raw SQL on a method
import com.example.a211198_hasif_drnelson_Project2.data.entities.ActivityRecordEntity // a logged workout row
import com.example.a211198_hasif_drnelson_Project2.data.entities.MediaEntity          // a gallery reel/post row
import kotlinx.coroutines.flow.Flow             // live stream of query results

// ActivityDao — persistence for logged activities and gallery "media" (reels).
// The media table backs both the cross-user feed and each user's own gallery.
@Dao
interface ActivityDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)          // save a completed activity record
    suspend fun insertActivity(activity: ActivityRecordEntity)

    @Query("SELECT * FROM activities WHERE ownerEmail = :ownerEmail ORDER BY id DESC") // my activities, newest first
    fun observeActivities(ownerEmail: String): Flow<List<ActivityRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)          // insert/replace a single reel by id
    suspend fun insertMedia(media: MediaEntity)

    @Query("SELECT * FROM media WHERE id = :id LIMIT 1")      // fetch one reel by id (own-post guard during sync)
    suspend fun getMediaById(id: String): MediaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)          // bulk insert (used by demo/seed data)
    suspend fun insertAllMedia(media: List<MediaEntity>)

    @Query("SELECT COUNT(*) FROM media WHERE ownerEmail = :ownerEmail") // how many posts this user has (seed gate)
    suspend fun countMedia(ownerEmail: String): Int

    @Query("SELECT * FROM media WHERE ownerEmail = :ownerEmail ORDER BY createdAtMs DESC") // my gallery, newest first
    fun observeMedia(ownerEmail: String): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media WHERE author = :authorName ORDER BY createdAtMs DESC") // another user's gallery
    fun observeMediaByAuthor(authorName: String): Flow<List<MediaEntity>>

    @Query("SELECT COUNT(*) FROM media WHERE author = :authorName") // count a given author's reels
    suspend fun countMediaByAuthor(authorName: String): Int

    @Query("SELECT * FROM media ORDER BY createdAtMs DESC")   // the whole cross-user feed, newest first
    fun observeAllMedia(): Flow<List<MediaEntity>>

    // Propagate a display-name change to existing reels so the gallery stops
    // showing the old author name after the user renames themselves.
    @Query("UPDATE media SET author = :newName WHERE author = :oldName")
    suspend fun renameAuthor(oldName: String, newName: String)

    // Delete reels by id (used by the profile multi-select delete).
    @Query("DELETE FROM media WHERE id IN (:ids)")
    suspend fun deleteMediaByIds(ids: List<String>)
}
