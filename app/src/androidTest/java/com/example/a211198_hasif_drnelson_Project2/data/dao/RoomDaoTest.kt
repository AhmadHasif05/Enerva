package com.example.a211198_hasif_drnelson_Project2.data.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.a211198_hasif_drnelson_Project2.data.AppDatabase
import org.junit.After
import org.junit.Before

/**
 * Shared in-memory Room setup for DAO instrumented tests. Each test gets a fresh
 * database that never touches disk and is torn down afterwards, so tests are
 * isolated and order-independent.
 */
abstract class RoomDaoTest {

    protected lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }
}
