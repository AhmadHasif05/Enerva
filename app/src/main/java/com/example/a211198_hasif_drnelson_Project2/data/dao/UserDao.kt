package com.example.a211198_hasif_drnelson_Project2.data.dao

import androidx.room.Dao                        // marks this interface as a Room Data Access Object
import androidx.room.Insert                     // generates an INSERT implementation
import androidx.room.OnConflictStrategy         // what to do on a primary-key clash (REPLACE = upsert)
import androidx.room.Query                      // attach a raw SQL statement to a method
import com.example.a211198_hasif_drnelson_Project2.data.entities.FollowEntity         // a "I follow X" row
import com.example.a211198_hasif_drnelson_Project2.data.entities.SavedRouteEntity     // a bookmarked route row
import com.example.a211198_hasif_drnelson_Project2.data.entities.UserDirectoryEntity  // a cross-device user row
import com.example.a211198_hasif_drnelson_Project2.data.entities.UserEntity           // the local user row
import kotlinx.coroutines.flow.Flow             // a stream Room re-emits whenever the queried rows change

// UserDao — all reads/writes for the user domain (profile, directory, saved
// routes, follows). Room generates the implementation at build time. `suspend`
// methods run once off the main thread; `Flow` methods emit continuously.
@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)          // insert or overwrite by primary key (email)
    suspend fun upsertUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1") // one-shot lookup by email
    suspend fun findByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1") // same lookup, but as a live stream
    fun observeByEmail(email: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE email != :exceptEmail ORDER BY runnerName ASC") // everyone but me, A→Z
    fun observeAllExcept(exceptEmail: String): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE runnerName = :name LIMIT 1") // look up a user by display name
    suspend fun findByName(name: String): UserEntity?

    // ---- firebase uid mapping (5.3 backfills this on cloud sign-in) ----
    @Query("SELECT * FROM users WHERE firebaseUid = :uid LIMIT 1") // find the local row for a Firebase uid
    suspend fun findByFirebaseUid(uid: String): UserEntity?

    @Query("UPDATE users SET firebaseUid = :uid WHERE email = :email") // stamp the uid onto an existing row
    suspend fun setFirebaseUid(email: String, uid: String)

    // ---- public user directory (cross-device discovery) ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)          // mirror Firestore publicProfiles into Room
    suspend fun upsertDirectoryUsers(users: List<UserDirectoryEntity>)

    @Query("DELETE FROM user_directory WHERE uid = :uid")     // drop a directory entry (user removed remotely)
    suspend fun deleteDirectoryUser(uid: String)

    @Query("SELECT * FROM user_directory WHERE uid != :myUid ORDER BY runnerName ASC") // other users for Search
    fun observeDirectoryExcept(myUid: String): Flow<List<UserDirectoryEntity>>

    @Query("SELECT * FROM user_directory WHERE runnerName = :name LIMIT 1") // directory lookup by name
    suspend fun findDirectoryByName(name: String): UserDirectoryEntity?

    // ---- saved routes ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)          // bookmark a route (replace if already saved)
    suspend fun saveRoute(route: SavedRouteEntity)

    @Query("DELETE FROM saved_routes WHERE ownerEmail = :ownerEmail AND title = :title") // remove a bookmark
    suspend fun unsaveRoute(ownerEmail: String, title: String)

    @Query("SELECT * FROM saved_routes WHERE ownerEmail = :ownerEmail") // live list of my saved routes
    fun observeSavedRoutes(ownerEmail: String): Flow<List<SavedRouteEntity>>

    // ---- follows ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)          // follow someone (idempotent)
    suspend fun addFollow(follow: FollowEntity)

    @Query("DELETE FROM follows WHERE ownerEmail = :ownerEmail AND friendName = :friendName") // unfollow
    suspend fun removeFollow(ownerEmail: String, friendName: String)

    @Query("SELECT friendName FROM follows WHERE ownerEmail = :ownerEmail") // names I currently follow (live)
    fun observeFollowing(ownerEmail: String): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM follows WHERE friendName = :name") // how many people follow this user
    suspend fun countFollowersOf(name: String): Int

    // Keep follow rows pointing at the renamed user by their new display name.
    @Query("UPDATE follows SET friendName = :newName WHERE friendName = :oldName")
    suspend fun renameFollowFriend(oldName: String, newName: String)
}
