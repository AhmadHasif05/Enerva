package com.example.a211198_hasif_drnelson_Project2.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.a211198_hasif_drnelson_Project2.data.dao.ActivityDao
import com.example.a211198_hasif_drnelson_Project2.data.dao.MessageDao
import com.example.a211198_hasif_drnelson_Project2.data.dao.UserDao
import com.example.a211198_hasif_drnelson_Project2.data.entities.ActivityRecordEntity
import com.example.a211198_hasif_drnelson_Project2.data.entities.ConversationEntity
import com.example.a211198_hasif_drnelson_Project2.data.entities.FollowEntity
import com.example.a211198_hasif_drnelson_Project2.data.entities.MediaEntity
import com.example.a211198_hasif_drnelson_Project2.data.entities.MessageEntity
import com.example.a211198_hasif_drnelson_Project2.data.entities.SavedRouteEntity
import com.example.a211198_hasif_drnelson_Project2.data.entities.UserEntity

@Database(
    entities = [
        UserEntity::class,
        SavedRouteEntity::class,
        FollowEntity::class,
        ActivityRecordEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        MediaEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun messageDao(): MessageDao
    abstract fun activityDao(): ActivityDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "runtrack.db"
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
        }
    }
}
