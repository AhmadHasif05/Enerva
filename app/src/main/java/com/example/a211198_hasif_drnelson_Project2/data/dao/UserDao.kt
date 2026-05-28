package com.example.a211198_hasif_drnelson_Project2.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.a211198_hasif_drnelson_Project2.data.entities.FollowEntity
import com.example.a211198_hasif_drnelson_Project2.data.entities.SavedRouteEntity
import com.example.a211198_hasif_drnelson_Project2.data.entities.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    fun observeByEmail(email: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE email != :exceptEmail ORDER BY runnerName ASC")
    fun observeAllExcept(exceptEmail: String): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE runnerName = :name LIMIT 1")
    suspend fun findByName(name: String): UserEntity?

    // ---- saved routes ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRoute(route: SavedRouteEntity)

    @Query("DELETE FROM saved_routes WHERE ownerEmail = :ownerEmail AND title = :title")
    suspend fun unsaveRoute(ownerEmail: String, title: String)

    @Query("SELECT * FROM saved_routes WHERE ownerEmail = :ownerEmail")
    fun observeSavedRoutes(ownerEmail: String): Flow<List<SavedRouteEntity>>

    // ---- follows ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFollow(follow: FollowEntity)

    @Query("DELETE FROM follows WHERE ownerEmail = :ownerEmail AND friendName = :friendName")
    suspend fun removeFollow(ownerEmail: String, friendName: String)

    @Query("SELECT friendName FROM follows WHERE ownerEmail = :ownerEmail")
    fun observeFollowing(ownerEmail: String): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM follows WHERE friendName = :name")
    suspend fun countFollowersOf(name: String): Int
}
