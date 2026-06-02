package com.example.a211198_hasif_drnelson_Project2.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.a211198_hasif_drnelson_Project2.data.dao.ActivityDao
import com.example.a211198_hasif_drnelson_Project2.data.dao.MessageDao
import com.example.a211198_hasif_drnelson_Project2.data.dao.UserDao
import com.example.a211198_hasif_drnelson_Project2.data.entities.ActivityRecordEntity
import com.example.a211198_hasif_drnelson_Project2.data.entities.ConversationEntity
import com.example.a211198_hasif_drnelson_Project2.data.entities.FollowEntity
import com.example.a211198_hasif_drnelson_Project2.data.entities.MediaEntity
import com.example.a211198_hasif_drnelson_Project2.data.entities.MessageEntity
import com.example.a211198_hasif_drnelson_Project2.data.entities.SavedRouteEntity
import com.example.a211198_hasif_drnelson_Project2.data.entities.UserDirectoryEntity
import com.example.a211198_hasif_drnelson_Project2.data.entities.UserEntity

@Database(
    entities = [
        UserEntity::class,
        SavedRouteEntity::class,
        FollowEntity::class,
        ActivityRecordEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        MediaEntity::class,
        UserDirectoryEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun messageDao(): MessageDao
    abstract fun activityDao(): ActivityDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        // v3 → v4 (Phase 5.2): add the Firestore-mapping columns. All three are
        // additive, nullable ADD COLUMNs, so every existing row is preserved —
        // no data is dropped on upgrade.
        //  - users.firebaseUid       → maps a local user row to its Firestore doc
        //  - conversations.conversationId / messages.conversationId
        //                            → shared top-level conversation id (uid-based)
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN firebaseUid TEXT")
                // Index name must match Room's default (index_<table>_<column>)
                // or runtime schema validation fails.
                db.execSQL("CREATE INDEX IF NOT EXISTS index_users_firebaseUid ON users (firebaseUid)")
                db.execSQL("ALTER TABLE conversations ADD COLUMN conversationId TEXT")
                db.execSQL("ALTER TABLE messages ADD COLUMN conversationId TEXT")
            }
        }

        // v4 → v5 (cross-device discovery): add the public user directory table,
        // mirrored from Firestore `publicProfiles/{uid}`. Additive CREATE TABLE —
        // existing data is preserved.
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS user_directory (" +
                        "uid TEXT NOT NULL PRIMARY KEY, " +
                        "runnerName TEXT NOT NULL, " +
                        "location TEXT NOT NULL, " +
                        "fitnessLevel TEXT NOT NULL, " +
                        "photoUri TEXT)"
                )
            }
        }

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "runtrack.db"
                )
                    // Real migrations preserve data; destructive fallback only
                    // kicks in for version paths we haven't written a migration for.
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
        }
    }
}
